package com.loanbroker.loan_broker;

import com.loanbroker.loan_broker.models.CanonicalDTO;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.core.Persister;

/**
 *
 * @author Marc
 */
public class RecipientHandler extends Thread {

	private final String QUEUE_NAME;
	private final String QUEUE_NAME_BANK_1;
	private final String QUEUE_NAME_BANK_2;
	private final String QUEUE_NAME_BANK_3;
	private final String QUEUE_NAME_BANK_4;
	/*	private final String QUEUE_NAME ="02_recipient_list_channel";
	 private final String QUEUE_NAME_BANK_1 = "02_bank_xml_channel";
	 private final String QUEUE_NAME_BANK_2 = "02_bank_json_channel";
	 private final String QUEUE_NAME_BANK_3 = "02_bank_rabbitmq_channel";
	 private final String QUEUE_NAME_BANK_4 = "02_bank_webservice_channel";*/

	public RecipientHandler(String queueName, String bankPrefix) {
		QUEUE_NAME = queueName;
		QUEUE_NAME_BANK_1 = bankPrefix + "_xml";
		QUEUE_NAME_BANK_2 = bankPrefix + "_json";
		QUEUE_NAME_BANK_3 = bankPrefix + "_rabbitmq";
		QUEUE_NAME_BANK_4 = bankPrefix + "_webservice";
	}

	@Override
	public void run() {
		try {
			Connection conn = getConnection();
			
		} catch (IOException ex) {
			Logger.getLogger(RecipientHandler.class.getName()).log(Level.SEVERE, null, ex);
		}

	}

	private Connection getConnection() throws IOException {
		ConnectionFactory connfac = new ConnectionFactory();
		connfac.setHost("datdb.cphbusiness.dk");
		connfac.setPort(5672);
		connfac.setUsername("student");
		connfac.setPassword("cph");
		Connection connection = connfac.newConnection();
		return connection;
	}

	public void sendRecipients() throws IOException {
		Connection connection = getConnection();
		Channel channel = connection.createChannel();

		channel.queueDeclare(QUEUE_NAME, true, false, false, null);
		String message = "Hello World!";
		for (int i = 0; i < Integer.MAX_VALUE; i++) {
			channel.basicPublish("", QUEUE_NAME, null, message.getBytes());
		}
		System.out.println(" [x] Sent '" + message + "'");

		channel.close();
		connection.close();

	}
	public static void main(String[] args) {
		Serializer ser = new Persister();
		CanonicalDTO can = new CanonicalDTO();
		
		OutputStream o = new ByteArrayOutputStream();
		try {
			ser.write(can, o);
			System.out.println("String: " + o.toString());
		} catch (Exception ex) {
			Logger.getLogger(RecipientHandler.class.getName()).log(Level.SEVERE, null, ex);
		}
	}
}
