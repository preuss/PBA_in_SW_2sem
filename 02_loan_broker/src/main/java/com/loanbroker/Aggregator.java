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
import java.util.HashMap;
import java.util.List;

/**
 * @author Preuss
 */
public class Aggregator extends HandlerThread {

	private Logger log = Logger.getLogger(Aggregator.class);
	private String peepQueueIn;
	private String queueIn;
	private String queueOut;
	private HashMap<String, CanonicalDTO> canons = new HashMap();
	private HashMap<String, List<BankDTO>> incomingBanks = new HashMap();
	private HashMap<String, Integer> timeouts = new HashMap();

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
		for (CanonicalDTO canon : canons.values()) {
			String ssn = canon.getSsn();
			int numBanks = canon.getBanks().size();
			if (incomingBanks.containsKey(ssn)) {
				List<BankDTO> banks = incomingBanks.get(ssn);
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
		CanonicalDTO canon = canons.get(ssn);

		// Cleanup
		canons.remove(ssn);
		if (timeouts.containsKey(ssn)) {
			timeouts.remove(ssn);
		}
		canon.setBanks(incomingBanks.get(ssn));
		incomingBanks.remove(ssn);

		// Now sending.
		outChannel = createChannel(connection, queueOut);
		String xmlStr = convertDtoToString(canon);
		outChannel.basicPublish("", queueOut, null, xmlStr.getBytes());
	}

	private void addBankToMaps(CanonicalDTO newDto) {
		String ssn = newDto.getSsn();
		if (incomingBanks.containsKey(ssn)) {
			List<BankDTO> banks = incomingBanks.get(ssn);
			if (newDto.getBanks() != null) {
				for (BankDTO newBank : newDto.getBanks()) {
					banks.add(newBank);
				}
			}
		} else {
			if (newDto.getBanks() != null) {
				incomingBanks.put(ssn, newDto.getBanks());
			}
		}
	}

	private void cleanupMessages() {
		// TODO: Need to use timeout.
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
					addCanon(allDto);
				}

				log.trace("receiveDto");
				CanonicalDTO receiveDto = receiveMessage(connection, channel, queueIn);
				if (receiveDto != null) {
					log.debug("Received DTO: " + receiveDto);
					addBankToMaps(receiveDto);
				}

				sendAllSendableMessages(connection, channel);
				cleanupMessages();
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

	private void addCanon(CanonicalDTO allDto) {
		canons.put(allDto.getSsn(), allDto);
	}
}
