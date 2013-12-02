package com.loanbroker._loan_broker;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import java.io.IOException;

/**
 *
 * @author Marc
 */
public class RecipientHandler {

	private final static String QUEUE_NAME = "02_recipient_list_channel";
	private final static String QUEUE_NAME_BANK_1 = "02_bank_xml_channel";
	private final static String QUEUE_NAME_BANK_2 = "02_bank_json_channel";
	private final static String QUEUE_NAME_BANK_3 = "02_bank_rabbitmq_channel";
	private final static String QUEUE_NAME_BANK_4 = "02_bank_webservice_channel";

	public RecipientHandler() {
	}

	private Connection GetConnection() throws IOException {
		ConnectionFactory connfac = new ConnectionFactory();
		connfac.setHost("datdb.cphbusiness.dk");
		connfac.setPort(5672);
		connfac.setUsername("student");
		connfac.setPassword("cph");
		Connection connection = connfac.newConnection();
		return connection;
	}

	public void SendRecipients() throws IOException {
		Connection connection = GetConnection();
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
}
