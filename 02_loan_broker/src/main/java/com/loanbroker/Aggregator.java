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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

/**
 * @author Preuss
 */
public class Aggregator extends HandlerThread {

	private long timeOutInMilliseconds = 1 * 20 * 1000; // 20 sekunder
	private Logger log = Logger.getLogger(Aggregator.class);
	private String exchangeIn;
	private String PEEP_QUEUE_IN = "Group2.Aggregator.PeepIn";
	private String queueIn;
	private String queueOut;
	private HashMap<String, CanonicalDTO> bankHandlerDtos = new HashMap();
	private HashMap<String, CanonicalDTO> normalizerDtos = new HashMap();
	private HashMap<String, Date> timeouts = new HashMap();

	public Aggregator(String exchangeIn, String queueIn, String queueOut) {
		this.exchangeIn = exchangeIn;
		this.queueIn = queueIn;
		this.queueOut = queueOut;
	}

	private void sendAllSendableMessages(Connection connection, Channel channel) throws IOException {
		List<String> sendable = new ArrayList();

		// Finds all finished banks.
		for (CanonicalDTO peekDto : bankHandlerDtos.values()) {
			String ssn = peekDto.getSsn();
			int numBanks = peekDto.getBanks().size();
			if (normalizerDtos.containsKey(ssn)) {
				List<BankDTO> banks = normalizerDtos.get(ssn).getBanks();
				if (banks.size() == numBanks) {
					log.debug("\t SSN:" + ssn + ", PeekDto: " + peekDto);
					sendable.add(ssn);
				}
			}
		}

		// Remove duplicates
		sendable = new ArrayList<String>(new LinkedHashSet<String>(sendable));

		// Send all sendables.
		for (String ssn : sendable) {
			log.debug("\tSend Message with SSN: " + ssn);
			sendMessage(channel, ssn);
		}
	}

	private void sendMessage(Channel outChannel, String ssn) throws IOException {
		CanonicalDTO incomingBank = normalizerDtos.get(ssn);

		// Cleanup
		normalizerDtos.remove(ssn);
		if (bankHandlerDtos.containsKey(ssn)) {
			CanonicalDTO peekBank = bankHandlerDtos.get(ssn);
			log.debug("\tOriginal Number of banks : " + peekBank.getBanks().size() + ", Now num: " + incomingBank.getBanks().size());
			String peekBanksStr = "";
			String incomBanksStr = "";
			List<BankDTO> peekBanks = peekBank.getBanks(), incomBanks = incomingBank.getBanks();
			for(BankDTO bank : peekBanks) {
				peekBanksStr += bank.getName() + ", ";
			}
			peekBanksStr = peekBanksStr.trim().substring(0, peekBanksStr.length()-1); // remove comma (,)
			for(BankDTO bank : incomBanks) {
				incomBanksStr += bank.getName() + ", ";
			}
			incomBanksStr = incomBanksStr.trim().substring(0, incomBanksStr.length()-1); // remove comma (,)
			log.debug("\tNeededbanks : " + peekBanksStr);
			log.debug("\tCurrentBanks: " + incomBanksStr);
			peekBank.setBanks(incomingBank.getBanks());
			incomingBank = peekBank;

			
			bankHandlerDtos.remove(ssn);
		} else {
			log.debug("\tOriginal Number of banks : " + null + ", Now num: " + incomingBank.getBanks().size());
		}
		if (timeouts.containsKey(ssn)) {
			timeouts.remove(ssn);
		}

		// Now sending.
		log.debug("SENDING incomingBank: " + incomingBank);
		String xmlStr = convertDtoToString(incomingBank);
		outChannel.basicPublish("", queueOut, null, xmlStr.getBytes());
	}

	private void addIncomingBank(CanonicalDTO newIncomingDto) {
		String ssn = newIncomingDto.getSsn();
		if (normalizerDtos.containsKey(ssn)) {
			CanonicalDTO currentDto = normalizerDtos.get(ssn);
			List<BankDTO> currentBanks = currentDto.getBanks();
			if (newIncomingDto.getBanks() != null) {
				for (BankDTO newBank : newIncomingDto.getBanks()) {
					if (!currentBanks.contains(newBank)) {
						currentBanks.add(newBank);
					}
				}
			}
			currentDto.setBanks(currentBanks);
		} else {
			if (newIncomingDto.getBanks() != null) {
				normalizerDtos.put(ssn, newIncomingDto);
			}
		}
	}

	private void cleanupMessages(Connection connection, Channel channel) throws IOException {
		List<String> sendNowAndRemoveSsn = new ArrayList<>();
		/*
		 for (Map.Entry<String, Date> entry : timeouts.entrySet()) {
		 String ssn = entry.getKey();
		 Date timeoutDate = entry.getValue();
		 if (new Date().getTime() > timeoutDate.getTime()) {
		 log.debug("Timeout for SSN: " + ssn);
		 sendNowAndRemoveSsn.add(ssn);
		 }
		 }
		 */
		// Find all timeouts
		long nowTime = new Date().getTime();
		for (String timeoutSsn : timeouts.keySet()) {
			long timeoutTime = timeouts.get(timeoutSsn).getTime();
			if (nowTime > timeoutTime) {
				if (normalizerDtos.containsKey(timeoutSsn)) {
					log.debug("Timeout for SSN: " + timeoutSsn + ", DTO: " + normalizerDtos.get(timeoutSsn));
					sendNowAndRemoveSsn.add(timeoutSsn);
				} else {
					log.debug("Timeout for SSN: " + timeoutSsn + ", but no banks.");
				}
			}
		}

		// Remove duplicates
		sendNowAndRemoveSsn = new ArrayList<String>(new LinkedHashSet<String>(sendNowAndRemoveSsn));

		for (String ssn : sendNowAndRemoveSsn) {
			timeouts.remove(ssn);
			sendMessage(channel, ssn); // Cleanup after itself.
		}
	}

	@Override
	protected void doRun() {
		Connection connection = null;
		Channel channel = null;
		QueueingConsumer consumer = null;
		while (!isPleaseStop()) {
			try {
				if (connection == null) {
					connection = getConnection();
					channel = connection.createChannel();
					channel.queueDeclare(queueIn, false, false, false, null);

					channel.queueDeclare(PEEP_QUEUE_IN, false, false, false, null);
					channel.exchangeDeclare(exchangeIn, "fanout");
					channel.queueBind(PEEP_QUEUE_IN, exchangeIn, "");
					log.debug("PEEP_QUEUE_IN: " + PEEP_QUEUE_IN);
					log.debug("exchangeIn: " + exchangeIn);
					log.debug("queueIn: " + queueIn);

					consumer = new QueueingConsumer(channel);
					channel.basicConsume(PEEP_QUEUE_IN, true, consumer);
					channel.basicConsume(queueIn, true, consumer);
				}

				//QueueingConsumer.Delivery delivery = consumer.nextDelivery();
				QueueingConsumer.Delivery delivery = consumer.nextDelivery(5000);
				if (delivery != null && delivery.getBody() != null) {
					if (delivery.getEnvelope().getExchange().length() > 0) {
						log.debug("Delivery gotten. Gotten from EXCHANGE: " + delivery.getEnvelope().getExchange() + ", Message: " + new String(delivery.getBody()).replace("\n", "").replace("\t", "").replace(" ", ""));
					} else {
						log.debug("Delivery gotten. Gotten from Routing Key: " + delivery.getEnvelope().getRoutingKey() + ", Message: " + new String(delivery.getBody()).replace("\n", "").replace("\t", "").replace(" ", ""));
					}

					byte[] messageRaw = delivery.getBody();
					String message = new String(messageRaw);
					CanonicalDTO dto = convertStringToDto(message);
					if (delivery.getEnvelope().getExchange().equalsIgnoreCase(exchangeIn)) {
						// Peep in
						addPeek(dto);
					} else {
						// From normalizer
						addIncomingBank(dto);
					}
					addTimeout(dto);
				} else {
					//log.debug("Delivery can't wait anymore.");
				}
				sendAllSendableMessages(connection, channel);
				cleanupMessages(connection, channel); // Send timeout messages and delete old.
			} catch (ConsumerCancelledException | ShutdownSignalException | InterruptedException e) {
				e.printStackTrace();
				closeChannel(channel);
				closeConnection(connection);
				connection = null;
			} catch (Exception e) {
				e.printStackTrace();
				pleaseStop();
			}
		}
	}

	private void addPeek(CanonicalDTO allDto) {
		bankHandlerDtos.put(allDto.getSsn(), allDto);
	}

	private void addTimeout(CanonicalDTO allDto) {
		Date timeoutDate = new Date();
		timeoutDate = new Date(timeoutDate.getTime() + timeOutInMilliseconds);
		if (!timeouts.containsKey(allDto.getSsn())) {
			timeouts.put(allDto.getSsn(), timeoutDate);
		}
	}
}
