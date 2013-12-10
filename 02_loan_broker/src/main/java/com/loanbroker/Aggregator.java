package com.loanbroker;

import com.loanbroker.handlers.HandlerThread;
import com.loanbroker.logging.Level;
import com.loanbroker.logging.Logger;
import com.loanbroker.models.BankDTO;
import com.loanbroker.models.CanonicalDTO;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConsumerCancelledException;
import com.rabbitmq.client.QueueingConsumer;
import com.rabbitmq.client.ShutdownSignalException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.core.Persister;

/**
 * @author Preuss
 */
public class Aggregator extends HandlerThread {

	private Logger log = Logger.getLogger(Aggregator.class);

	private String queueIn;
	private String queueOut;

	private HashMap<String, CanonicalDTO> canons = new HashMap();
	private HashMap<String, List<BankDTO>> incomingBanks = new HashMap();
	private HashMap<String, Integer> timeouts = new HashMap();

	public Aggregator(String queueIn, String queueOut) {
		this.queueIn = queueIn;
		this.queueOut = queueOut;
	}

	private boolean queueExist(Channel channel, String queueName) {
		boolean retVal = true;
		try {
			channel.queueDeclarePassive(queueName);
			retVal = false;
		} catch (IOException e) {
			log.log(Level.SEVERE, null, e);
		}
		return retVal;
	}

	private Channel createChannel(String queueName) throws IOException {
		Connection conn = getConnection();
		Channel channel = conn.createChannel();
		if (!queueExist(channel, queueName)) {
			channel.queueDeclare(queueName, false, false, false, null);
		}
		return channel;
	}

	private CanonicalDTO receiveMessage() throws IOException, ConsumerCancelledException, ShutdownSignalException, InterruptedException, Exception {
		CanonicalDTO dto = null;

		Channel channel = createChannel(queueIn);
		QueueingConsumer consumer = new QueueingConsumer(channel);
		channel.basicConsume(queueIn, true, consumer);
		QueueingConsumer.Delivery delivery = consumer.nextDelivery();

		String xmlStr = delivery.getBody().toString();
		Serializer serializer = new Persister();
		dto = serializer.read(CanonicalDTO.class, xmlStr);
		return dto;
	}

	private void sendAllSendableMessages() throws IOException {
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
			sendMessage(ssn);
		}
	}

	private void sendMessage(String ssn) throws IOException {
		CanonicalDTO canon = canons.get(ssn);

		// Cleanup
		canons.remove(ssn);
		if (timeouts.containsKey(ssn)) {
			timeouts.remove(ssn);
		}
		canon.setBanks(incomingBanks.get(ssn));
		incomingBanks.remove(ssn);

		// Now sending.
		Channel outChannel = createChannel(queueOut);
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

	@Override
	protected void doRun() {
		while (!isPleaseStop()) {
			try {
				CanonicalDTO receiveDto = receiveMessage();
				addBankToMaps(receiveDto);
				sendAllSendableMessages();
				//cleanup();
			} catch (ConsumerCancelledException e) {
				log.log(Level.SEVERE, null, e);
			} catch (ShutdownSignalException e) {
				log.log(Level.SEVERE, null, e);
			} catch (InterruptedException e) {
				log.log(Level.SEVERE, null, e);
			} catch (Exception e) {
				log.log(Level.SEVERE, null, e);
			}
		}
	}

}