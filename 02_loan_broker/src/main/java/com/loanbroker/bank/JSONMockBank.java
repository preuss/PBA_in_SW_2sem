/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.loanbroker.bank;

import com.loanbroker.handlers.HandlerThread;
import com.loanbroker.logging.*;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.ConsumerCancelledException;
import com.rabbitmq.client.QueueingConsumer;
import com.rabbitmq.client.ShutdownSignalException;
import java.io.IOException;
import org.json.JSONException;
import org.json.JSONObject;

/**
 *
 * @author Marc
 */
public class JSONMockBank extends HandlerThread {

	private final Logger log = Logger.getLogger(JSONMockBank.class);

	private String receiveQueue;
	private String sendQueue;

	public JSONMockBank(String queueIn, String queueOut) {
		this.receiveQueue = queueIn;
		this.sendQueue = queueOut;
	}

	@Override
	protected void doRun() {
		while (!isPleaseStop()) {
			try {
				ConnectionFactory factory = new ConnectionFactory();
				factory.setHost("datdb.cphbusiness.dk");
				factory.setUsername("student");
				factory.setPassword("cph");
				Connection connection = factory.newConnection();
				Channel channel = connection.createChannel();

				channel.queueDeclare(sendQueue, false, false, false, null);
				String messageOutTemplate = "{\"interestRate\":#InterestRate#,\"ssn\":#SSN#}";

				QueueingConsumer consumer = new QueueingConsumer(channel);
				channel.basicConsume(receiveQueue, true, consumer);

				while (true) {
					QueueingConsumer.Delivery delivery = consumer.nextDelivery();
					String messageIn = new String(delivery.getBody());
					System.out.println(" [x] Received by rabbit_bank: '" + messageIn + "'");
					String messageOut = messageOutTemplate;
					double interestRate = (Math.random() * 20) + 80; // 80 - 100 % :-(
					String ssn = "Unknown";
					try {
						ssn = new JSONObject(messageIn).getString("ssn");
					} catch (JSONException ex) {
						Logger.getLogger(JSONMockBank.class.getName()).log(Level.SEVERE, null, ex);
					}
					messageOut = messageOut.replace("#InterestRate#", interestRate + "");
					messageOut = messageOut.replace("#SSN#", ssn);

					channel.basicPublish("", sendQueue, null, messageOut.getBytes());
					System.out.println(" [x] Sent by rabbit_bank: '" + messageOut + "'");
				}
			} catch (IOException ex) {
				Logger.getLogger(JSONMockBank.class.getName()).log(Level.SEVERE, null, ex);
			} catch (InterruptedException ex) {
				Logger.getLogger(JSONMockBank.class.getName()).log(Level.SEVERE, null, ex);
			} catch (ShutdownSignalException ex) {
				Logger.getLogger(JSONMockBank.class.getName()).log(Level.SEVERE, null, ex);
			} catch (ConsumerCancelledException ex) {
				Logger.getLogger(JSONMockBank.class.getName()).log(Level.SEVERE, null, ex);
			}
		}
	}

	public static void main(String[] argv) throws IOException, InterruptedException {
		String mockJsonBankIn = "json_bankReceiver";
		String mockJsonBankOut = "json_bankSend";
		JSONMockBank jsonMockBank = new JSONMockBank(mockJsonBankIn, mockJsonBankOut);
		jsonMockBank.start();
	}

}
