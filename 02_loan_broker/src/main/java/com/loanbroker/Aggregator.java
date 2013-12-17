package com.loanbroker;

import com.loanbroker.handlers.HandlerThread;
import com.loanbroker.logging.Level;
import com.loanbroker.logging.Logger;
import com.loanbroker.models.BankDTO;
import com.loanbroker.models.CanonicalDTO;
import com.rabbitmq.client.*;
import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.core.Persister;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * @author Preuss
 */
public class Aggregator extends HandlerThread {

	private long timeOutInMilliseconds = 1 * 5 * 1000; // 2 minutes
	private Logger log = Logger.getLogger(Aggregator.class);
	private String peepQueueIn;
	private String queueIn;
	private String queueOut;
	private HashMap<String, CanonicalDTO> peekDtoMap = new HashMap();
	private HashMap<String, CanonicalDTO> incomingBanks = new HashMap();
	private HashMap<String, Date> timeouts = new HashMap();

	public Aggregator(String peepQueueIn, String queueIn, String queueOut) {
		this.peepQueueIn = peepQueueIn;
		this.queueIn = queueIn;
		this.queueOut = queueOut;
	}

	private CanonicalDTO receiveMessage(Connection connection, Channel channel, String queueName) throws IOException, ConsumerCancelledException, ShutdownSignalException, InterruptedException {
		CanonicalDTO dto = null;

		GetResponse response = channel.basicGet(queueName, true);
		String xmlStr = null;
		if (response != null) {
			if (response.getBody() != null) {
				xmlStr = new String(response.getBody());
			}
		}
		Serializer serializer = new Persister();
		try {
			if (xmlStr != null) {
				dto = serializer.read(CanonicalDTO.class, xmlStr);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return dto;
	}

	private void sendAllSendableMessages(Connection connection, Channel channel) throws IOException {
		List<String> sendable = new ArrayList();
		for (CanonicalDTO peekDto : peekDtoMap.values()) {
			String ssn = peekDto.getSsn();
			int numBanks = peekDto.getBanks().size();
			if (incomingBanks.containsKey(ssn)) {
				List<BankDTO> banks = incomingBanks.get(ssn).getBanks();
				if (banks.size() == numBanks) {
					sendable.add(ssn);
				}
			}
		}
		for (String ssn : sendable) {
			sendMessage(connection, channel, ssn);
		}
	}

	private void sendMessage(Connection connection, Channel outChannel, String ssn) throws IOException {
		CanonicalDTO peekDto = peekDtoMap.get(ssn);

		// Cleanup
		if (peekDtoMap.containsKey(ssn)) {
			peekDtoMap.remove(ssn);
		}
		if (timeouts.containsKey(ssn)) {
			timeouts.remove(ssn);
		}
		if (peekDto != null) {
			if (incomingBanks.containsKey(ssn)) {
				peekDto.setBanks(incomingBanks.get(ssn).getBanks());
				incomingBanks.remove(ssn);
			} else {
				log.debug("Timeout before any incoming banks");
				peekDto = null;
			}
		} else {
			if (incomingBanks.containsKey(ssn)) {
				peekDto = incomingBanks.get(ssn);
			} else {
				log.warning("We don't have any banks with ssn: " + ssn);
			}
		}
		if (peekDto == null) {
			log.error("Peek DTO is null.");
			return;
		}

		// Now sending.
		outChannel = createChannel(connection, queueOut);
		String xmlStr = convertDtoToString(peekDto);
		outChannel.basicPublish("", queueOut, null, xmlStr.getBytes());
	}

	private void addIncomingBank(CanonicalDTO newIncomingDto) {
		String ssn = newIncomingDto.getSsn();
		if (incomingBanks.containsKey(ssn)) {
			CanonicalDTO currentDto = incomingBanks.get(ssn);
			List<BankDTO> currentBanks = currentDto.getBanks();
			if (newIncomingDto.getBanks() != null) {
				for (BankDTO newBank : newIncomingDto.getBanks()) {
					if(!currentBanks.contains(newBank)){
						currentBanks.add(newBank);
					}
				}
			}
			currentDto.setBanks(currentBanks);
		} else {
			if (newIncomingDto.getBanks() != null) {
				incomingBanks.put(ssn, newIncomingDto);
			}
		}
	}

	private void cleanupMessages(Connection connection, Channel channel) throws IOException {
		List<String> sendNowAndRemoveSsn = new ArrayList<>();
		for (Map.Entry<String, Date> entry : timeouts.entrySet()) {
			String ssn = entry.getKey();
			Date timeoutDate = entry.getValue();
			if (new Date().getTime() > timeoutDate.getTime()) {
				log.debug("Timeout for SSN: " + ssn);
				sendNowAndRemoveSsn.add(ssn);
			}
		}
		for (String ssn : sendNowAndRemoveSsn) {
			timeouts.remove(ssn);
			this.peekDtoMap.remove(ssn);
			sendMessage(connection, channel, ssn);
		}
	}

	@Override
	protected void doRun() {
		Connection connection = null;
		Channel channel = null;
		while (!isPleaseStop()) {
			try {
				if (connection == null) {
					connection = getConnection();
				}
				if (channel == null) {
					channel = createChannel(queueIn);
					channel.queueDeclare(queueOut, false, false, false, null);
					channel.queueDeclare(peepQueueIn, false, false, false, null);
				}

				log.trace("allDto");
				CanonicalDTO allDto = receiveMessage(connection, channel, peepQueueIn);
				if (allDto != null) {
					addPeek(allDto);
					addTimeout(allDto);
				}

				log.trace("receiveDto");
				CanonicalDTO receiveDto = receiveMessage(connection, channel, queueIn);
				if (receiveDto != null) {
					log.debug("Received DTO: " + receiveDto);
					addIncomingBank(receiveDto);
					addPeek(receiveDto);
					addTimeout(receiveDto);
				}

				sendAllSendableMessages(connection, channel);
				cleanupMessages(connection, channel);
			} catch (ConsumerCancelledException | ShutdownSignalException | InterruptedException e) {
				log.log(Level.SEVERE, null, e);
				e.printStackTrace();
				closeChannel(channel);
				channel = null;
				closeConnection(connection);
				connection = null;
			} catch (Exception e) {
				log.log(Level.SEVERE, null, e);
				e.printStackTrace();
				pleaseStop();
			}
		}
	}

	private void addPeek(CanonicalDTO allDto) {
		peekDtoMap.put(allDto.getSsn(), allDto);
	}

	private void addTimeout(CanonicalDTO allDto) {
		Date timeoutDate = new Date();
		timeoutDate = new Date(timeoutDate.getTime() + timeOutInMilliseconds);
		if (!timeouts.containsKey(allDto.getSsn())) {
			timeouts.put(allDto.getSsn(), timeoutDate);
		}
	}
}
