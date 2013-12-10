/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.loanbroker.old;

import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import java.io.IOException;

/**
 *
 * @author Marc
 */
public class JSONSendTest {

	private static final String EXCHANGE_NAME = "cphbusiness.bankJSON";

	public static void main(String[] argv) throws IOException {

		ConnectionFactory factory = new ConnectionFactory();
		factory.setHost("datdb.cphbusiness.dk");
		factory.setUsername("student");
		factory.setPassword("cph");
		Connection connection = factory.newConnection();
		Channel channel = connection.createChannel();

		channel.exchangeDeclare(EXCHANGE_NAME, "fanout");
                String queueName = "Kender du hende der Arne";
                channel.queueDeclare(queueName, false, false, false, null);
		
		channel.queueBind(queueName, EXCHANGE_NAME, "");
		String messageOut = "{\"ssn\":1605789787,\"creditScore\":699,\"loanAmount\":10.0,\"loanDuration\":360}";
		BasicProperties.Builder props = new BasicProperties().builder();
		props.replyTo("");
		BasicProperties bp = props.build();
		channel.basicPublish(EXCHANGE_NAME, "", bp, messageOut.getBytes());
		System.out.println(" [x] Sent by JSONtester: '" + messageOut + "'");

		channel.close();
		connection.close();
	}

}
