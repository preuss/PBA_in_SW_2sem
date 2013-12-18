package com.loanbroker.handlers;

import com.loanbroker.logging.Level;
import com.loanbroker.logging.Logger;
import com.loanbroker.models.CanonicalDTO;
import com.loanbroker.models.BankDTO;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.ConsumerCancelledException;
import com.rabbitmq.client.QueueingConsumer;
import com.rabbitmq.client.ShutdownSignalException;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.core.Persister;

/**
 *
 * @author Marc
 */
public class RecipientHandler extends HandlerThread {

	private final Logger log = Logger.getLogger(RecipientHandler.class);

	private String exchangeIn;
	private final String QUEUE_IN = "Group2.RecipientHandler.Receive";
	private Map<String, String> queueOutBanks;

	public RecipientHandler(String exchangeIn, Map<String, String> bankQueues) {
		this.exchangeIn = exchangeIn;
		this.queueOutBanks = bankQueues;
	}

	private String getBankChannelName(String bankName) {
		if (!queueOutBanks.containsKey(bankName)) {
			return null;
		}
		return queueOutBanks.get(bankName);
	}

	@Override
	public void doRun() {
		Connection connection = null;
		Channel channel = null;
		QueueingConsumer consumer = null;
		String consumerTag = null;
		//start polling messages
		while (isPleaseStop() == false) {
			try {
				if (connection == null) {
					connection = getConnection();
					channel = connection.createChannel();
					channel.queueDeclare(QUEUE_IN, false, false, false, null);
					channel.exchangeDeclare(exchangeIn, "fanout");
					channel.queueBind(QUEUE_IN, exchangeIn, "");
					consumer = new QueueingConsumer(channel);
					consumerTag = channel.basicConsume(QUEUE_IN, true, consumer);
					for (String queueOut : queueOutBanks.values()) {
						//channel.queueDeclare(queueIn, false, false, false, null);
						channel.queueDeclare(queueOut, false, false, false, null);
					}
					System.out.println(" [*] Waiting for messages. To exit press CTRL+C");
					System.out.println(" [-] ConsumerTag: '" + consumerTag + "'");
				}

				log.debug("Try to read a message to RecipientHandler");
				QueueingConsumer.Delivery delivery = consumer.nextDelivery();
				byte[] messageRaw = delivery.getBody();
				String xmlStr = new String(messageRaw);
				log.debug("Message: " + xmlStr.replace("\r", "").replace("\n", "").replace(" ", "").replace("\t", ""));

				CanonicalDTO dto = convertStringToDto(xmlStr);
				boolean noBanks = true;
				for (BankDTO bank : dto.getBanks()) {
					String queueName = getBankChannelName(bank.getName());
					if (queueName != null) {
						channel.basicPublish("", queueName, null, xmlStr.getBytes());
						System.out.println(" [x] Sent '" + xmlStr.replace("\r", "").replace("\n", "").replace(" ", "").replace("\t", "") + "'");
						noBanks = false;
						log.info("Delivered to bank: " + bank.getName());
					} else {
						// TODO: publish to error queue.
					}
				}
				if (noBanks) {
					log.info("RecipientHandler: No Banks for this DTO: " + dto);
				}
			} catch (IOException e) {
				e.printStackTrace();
				if (e.getCause() != null) {
					log.warning("\t" + e.getCause().getClass() + ", " + e.getCause().getMessage());
					if (e.getCause().getCause() != null) {
						log.warning("\t\t" + e.getCause().getCause().getClass() + ", " + e.getCause().getCause().getMessage());
					}
				}
			} catch (ShutdownSignalException e) {
				e.printStackTrace();
				if (e.getCause() != null) {
					log.warning("\t" + e.getCause().getClass() + ", " + e.getCause().getMessage());
					if (e.getCause().getCause() != null) {
						log.warning("\t\t" + e.getCause().getCause().getClass() + ", " + e.getCause().getCause().getMessage());
					}
				}
			} catch (ConsumerCancelledException e) {
				e.printStackTrace();
				if (e.getCause() != null) {
					log.warning("\t" + e.getCause().getClass() + ", " + e.getCause().getMessage());
					if (e.getCause().getCause() != null) {
						log.warning("\t\t" + e.getCause().getCause().getClass() + ", " + e.getCause().getCause().getMessage());
					}
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
				if (e.getCause() != null) {
					log.warning("\t" + e.getCause().getClass() + ", " + e.getCause().getMessage());
					if (e.getCause().getCause() != null) {
						log.warning("\t\t" + e.getCause().getCause().getClass() + ", " + e.getCause().getCause().getMessage());
					}
				}
			}
		}
	}
}
