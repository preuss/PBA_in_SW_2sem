package com.loanbroker.handlers;

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
import java.util.logging.Level;
import java.util.logging.Logger;
import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.core.Persister;

/**
 *
 * @author Marc
 */
public class RecipientHandler extends HandlerThread {

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
		try {
			Connection conn = getConnection();
			Channel chan = conn.createChannel();
			chan.queueDeclare(queueIn, false, false, false, null);
			System.out.println(" [*] Waiting for messages. To exit press CTRL+C");
			QueueingConsumer consumer = new QueueingConsumer(chan);
			String consumerTag = chan.basicConsume(queueIn, true, consumer);
			//start polling messages
			while (isPleaseStop() == false) {
				//String consumerTag = consumer.getConsumerTag();
				System.out.println(" [-] ConsumerTag: '" + consumerTag + "'");
				QueueingConsumer.Delivery delivery = consumer.nextDelivery();
				String xmlStr = delivery.getBody().toString();
				processBankOutput(xmlStr);
				//CanonicalDTO dto = new String(delivery.getBody());
				//System.out.println(" [x] Received '" + message + "'");
			}
		} catch (IOException e) {
			Logger.getLogger(RecipientHandler.class.getName()).log(Level.SEVERE, null, e);
			e.printStackTrace();
		} catch (InterruptedException e) {
			Logger.getLogger(RecipientHandler.class.getName()).log(Level.SEVERE, null, e);
			e.printStackTrace();
		} catch (ShutdownSignalException e) {
			Logger.getLogger(RecipientHandler.class.getName()).log(Level.SEVERE, null, e);
			e.printStackTrace();
		} catch (ConsumerCancelledException e) {
			Logger.getLogger(RecipientHandler.class.getName()).log(Level.SEVERE, null, e);
			e.printStackTrace();
		}
	}

	private void processBankOutput(String xmlStr) {
		Serializer serializer = new Persister();
		try {
			CanonicalDTO dto = serializer.read(CanonicalDTO.class, xmlStr);

//          OutputStream outputStream = new ByteArrayOutputStream();
//          serializer.write(dto, outputStream);
			Connection conn = getConnection();
			for (BankDTO bank : dto.getBanks()) {
				Channel channel = conn.createChannel();

				String queueName = getBankChannelName(bank.getName());
				if (queueName != null) {
					channel.queueDeclare(queueName, false, false, false, null);
					channel.basicPublish("", queueName, null, xmlStr.getBytes());
					System.out.println(" [x] Sent '" + xmlStr + "'");
				} else {
					// TODO: publish to error queue.
				}
			}
		} catch (Exception ex) {
			Logger.getLogger(RecipientHandler.class.getName()).log(Level.SEVERE, null, ex);
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
