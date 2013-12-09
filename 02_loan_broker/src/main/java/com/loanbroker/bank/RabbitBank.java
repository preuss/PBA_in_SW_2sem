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

/**
 * consumes messages that have the format
 * ssn#creditScore(int)#loanAmount(double)#loanDuration(int - months)
 *
 * @author Marc
 */
public class RabbitBank extends HandlerThread {

	private final Logger log = Logger.getLogger(RabbitBank.class);

	private String receiveQueue;
	private String sendQueue;

	public RabbitBank() {
	}

	public RabbitBank(String receiveQueue, String sendQueue) {
		this.receiveQueue = receiveQueue;
		this.sendQueue = sendQueue;
	}

	public static void main(String[] argv) throws IOException, InterruptedException {
		String rabbitBankIn = "rabbit_bankRecieve";
		String rabbitBankOut = "rabbit_bankSend";
		RabbitBank rabbitBank = new RabbitBank(rabbitBankIn, rabbitBankOut);
		rabbitBank.start();

	}

	@Override
	protected void doRun() {
		try {
			ConnectionFactory factory = new ConnectionFactory();
			factory.setHost("datdb.cphbusiness.dk");
			factory.setUsername("student");
			factory.setPassword("cph");
			Connection connection = factory.newConnection();
			Channel channel = connection.createChannel();

			channel.queueDeclare(sendQueue, false, false, false, null);

			QueueingConsumer consumer = new QueueingConsumer(channel);
			channel.basicConsume(receiveQueue, true, consumer);

			while (!isPleaseStop()) {
				QueueingConsumer.Delivery delivery = consumer.nextDelivery();
				String messageIn = new String(delivery.getBody());
				String interestRate = Math.random() * 12 + 3 + "";

				String ssn = messageIn.split("#")[0].split(":")[1];
				String messageOut = "interestRate:" + interestRate + "#ssn:" + ssn;
				System.out.println(" [x] Received by rabbit_bank: '" + messageIn + "'");
				channel.basicPublish("", sendQueue, null, messageOut.getBytes());
				System.out.println(" [x] Sent by rabbit_bank: '" + messageOut + "'");
			}
		} catch (IOException ex) {
			Logger.getLogger(RabbitBank.class.getName()).log(Level.SEVERE, null, ex);
		} catch (InterruptedException ex) {
			Logger.getLogger(RabbitBank.class.getName()).log(Level.SEVERE, null, ex);
		} catch (ShutdownSignalException ex) {
			Logger.getLogger(RabbitBank.class.getName()).log(Level.SEVERE, null, ex);
		} catch (ConsumerCancelledException ex) {
			Logger.getLogger(RabbitBank.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

}
