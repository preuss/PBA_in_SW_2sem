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

	private String queueIn;
	/*	private String QUEUE_NAME_BANK_1;
	 private String QUEUE_NAME_BANK_2;
	 private String QUEUE_NAME_BANK_3;
	 private String QUEUE_NAME_BANK_4;*/
	/*	private final String QUEUE_NAME ="02_recipient_list_channel";
	 private final String QUEUE_NAME_BANK_1 = "02_bank_xml_channel";
	 private final String QUEUE_NAME_BANK_2 = "02_bank_json_channel";
	 private final String QUEUE_NAME_BANK_3 = "02_bank_rabbitmq_channel";
	 private final String QUEUE_NAME_BANK_4 = "02_bank_webservice_channel";*/

	private Map<String, String> queueOutBanks;

//    public RecipientHandler() {
//        QUEUE_NAME = "reciepidequeu";
//        Map<String, String> ques = new HashMap<>();
//        ques.put("xml", "xml_channel");
//        ques.put("json", "json_channel");
//        ques.put("rabbitmq", "rabbitmq_channel");
//        ques.put("webservice", "websercice_channel");
//    }
	public RecipientHandler(String queueIn, Map<String, String> bankQueues) {
		this.queueIn = queueIn;
		this.queueOutBanks = bankQueues;
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
					channel = createChannel(connection, queueIn);
					consumer = new QueueingConsumer(channel);
					consumerTag = channel.basicConsume(queueIn, true, consumer);
					for (String queueOut : queueOutBanks.values()) {
						//channel.queueDeclare(queueIn, false, false, false, null);
						channel.queueDeclare(queueOut, false, false, false, null);
					}
					System.out.println(" [*] Waiting for messages. To exit press CTRL+C");
					System.out.println(" [-] ConsumerTag: '" + consumerTag + "'");
				}

				log.debug("Try to read a message to RecipientHandler");
				QueueingConsumer.Delivery delivery = consumer.nextDelivery();
				byte [] messageRaw = delivery.getBody();
				String xmlStr = new String(messageRaw);
				log.debug("Message: " + xmlStr.replace("\r", "").replace("\n", "").replace(" ", "").replace("\t", ""));
				processBankOutput(channel, xmlStr);
				//CanonicalDTO dto = new String(delivery.getBody());
				//System.out.println(" [x] Received '" + message + "'");
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

	private void processBankOutput(Channel channel, String xmlStr) throws IOException {
		Serializer serializer = new Persister();
		CanonicalDTO dto = convertStringToDto(xmlStr);
		if(dto == null) {
			log.debug("Problem with decoding message to DTO, throwing away :-<");
			return;
		}

		boolean noBanks = true;
		for (BankDTO bank : dto.getBanks()) {
			String queueName = getBankChannelName(bank.getName());
			if (queueName != null) {
				channel.basicPublish("", queueName, null, xmlStr.getBytes());
				System.out.println(" [x] Sent '" + xmlStr + "'");
				noBanks = false;
				log.info("Delivered to bank: " + bank.getName());
			} else {
				// TODO: publish to error queue.
			}
		}
		if (noBanks) {
			log.info("RecipientHandler: No Banks for this DTO: " + dto);
		}
	}

	private String getBankChannelName(String bankName) {
		if (!queueOutBanks.containsKey(bankName)) {
			return null;
		}
		return queueOutBanks.get(bankName);
	}

	public void sendRecipients() throws IOException {
		Connection connection = getConnection();
		Channel channel = connection.createChannel();

		channel.queueDeclare(queueIn, true, false, false, null);
		String message = "Hello World!";
		for (int i = 0; i < Integer.MAX_VALUE; i++) {
			channel.basicPublish("", queueIn, null, message.getBytes());
		}
		System.out.println(" [x] Sent '" + message + "'");

		channel.close();
		connection.close();

	}

}
