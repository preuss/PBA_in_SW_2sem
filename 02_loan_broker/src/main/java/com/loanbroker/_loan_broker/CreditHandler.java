/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.loanbroker._loan_broker;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import java.io.IOException;
import wservices.CreditScoreService;
import wservices.CreditScoreService_Service;

/**
 *
 * @author Andreas
 */
public class CreditHandler {

	private final static String QUEUE_NAME = "02_banklist_channel";

	public void getCreditScore() {

		try { // Call Web Service Operation
			CreditScoreService_Service service = new CreditScoreService_Service();
			CreditScoreService port = service.getCreditScoreServicePort();
			// TODO initialize WS operation arguments here
			String ssn = "12345678-1234";
			// TODO process result here
			int result = port.creditScore(ssn);
			System.out.println("Result from Credit Bureau = " + result);
		} catch (Exception ex) {
			System.out.println(ex.getMessage());
		}

	}

	public void getBanks() throws IOException {
		ConnectionFactory connfac = new ConnectionFactory();
		connfac.setHost("datdb.cphbusiness.dk");
		connfac.setPort(5672);
		connfac.setUsername("student");
		connfac.setPassword("cph");
		Connection connection = connfac.newConnection();
		Channel channel = connection.createChannel();

		channel.queueDeclare(QUEUE_NAME, false, false, false, null);
		String message = "Hello World!";
		channel.basicPublish("", QUEUE_NAME, null, message.getBytes());
		System.out.println(" [x] Sent '" + message + "'");

		channel.close();
		connection.close();

	}

}
