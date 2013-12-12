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
import java.util.HashMap;
import java.util.Map;

/**
 * consumes messages that have the format:
 * ssn:xxxxxx-xxxx#creditScore:y#loanAmount:z#loanDuration:a loanDuration is
 * expressed in months listens on the channel: 02_rabbitBankRecieve sends on the
 * channel: 02_rabbitBankSend
 *
 * @author Marc
 */
public class RabbitBank extends HandlerThread {

	private final Logger log = Logger.getLogger(RabbitBank.class);

	private String receiveQueue;
	//private String sendQueue;

	public RabbitBank() {
	}

	/*
	 public RabbitBank(String receiveQueue, String sendQueue) {
	 this.receiveQueue = receiveQueue;
	 this.sendQueue = sendQueue;
	 }*/
	public RabbitBank(String receiveQueue) {
		this.receiveQueue = receiveQueue;
	}
	
	private double calculateInterestRate(String ssn, int creditScore, double loanAmount, int loanDuration) {
		double interestRate = (Math.random() * (12 - 3) + 3);
		return interestRate;
	}
	/**
	 * @param message, format ssn:123456-1234#creditScore:666#loanAmount:2050.0#loanDuration:60 Output
	 * @return String
	 */
	private Map<String, String> convertMessageToMap(String message) {
		Map messageMap = new HashMap();
		for (String keyValue : message.split("#")) {
			String key = keyValue.split(":")[0];
			String value = keyValue.split(":")[1];
			messageMap.put(key, value);
		}
		return messageMap;
	}

		/**
	 * Input Message Format:
	 * ssn:123456-1234#creditScore:666#loanAmount:2050.0#loanDuration:60 Output
	 * Message Format: interestRate:5.8#ssn:123456-1234;
	 */
	@Override
	protected void doRun() {
		try {
			Channel channel = createChannel(receiveQueue);

			QueueingConsumer consumer = new QueueingConsumer(channel);
			channel.basicConsume(receiveQueue, true, consumer);

			while (!isPleaseStop()) {
				QueueingConsumer.Delivery delivery = consumer.nextDelivery();
				String messageIn = new String(delivery.getBody());
				Map<String, String> messageMap = convertMessageToMap(messageIn);
				String ssn = messageMap.get("ssn");
				int creditScore = Integer.parseInt(messageMap.get("creditScore"));
				double loanAmount = Double.parseDouble(messageMap.get("loanAmount"));
				int loanDuration = Integer.parseInt(messageMap.get("loanDuration"));
				
				String replyTo = delivery.getProperties().getReplyTo();
				
				// calculate interest rate
				String interestRate = calculateInterestRate(ssn, creditScore, loanAmount, loanDuration) + "";

				String messageOut = "interestRate:" + interestRate + "#ssn:" + ssn;
				System.out.println(" [x] Received by 02_rabbitBank: '" + messageIn + "'");
				channel.basicPublish("", replyTo, null, messageOut.getBytes());
				System.out.println(" [x] Sent by 02_rabbitBank: '" + messageOut + "'");
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
	
	/**
	 * Input Message Format:
	 * ssn:123456-1234#creditScore:666#loanAmount:2050.0#loanDuration:60 Output
	 * Message Format: interestRate:5.8#ssn:123456-1234;
	 */
	/*
	@Override
	protected void doRun() {
		try {
			Channel channel = createChannel(sendQueue);
			channel.queueDeclare(receiveQueue, true, true, true, null);

			QueueingConsumer consumer = new QueueingConsumer(channel);
			channel.basicConsume(receiveQueue, true, consumer);

			while (!isPleaseStop()) {
				QueueingConsumer.Delivery delivery = consumer.nextDelivery();
				String messageIn = new String(delivery.getBody());
				// calculate interest rate
				String interestRate = (Math.random() * (12 - 3) + 3) + "";

				String ssn = messageIn.split("#")[0].split(":")[1];
				String messageOut = "interestRate:" + interestRate + "#ssn:" + ssn;
				System.out.println(" [x] Received by 02_rabbitBank: '" + messageIn + "'");
				channel.basicPublish("", sendQueue, null, messageOut.getBytes());
				System.out.println(" [x] Sent by 02_rabbitBank: '" + messageOut + "'");
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
	}*/
}
