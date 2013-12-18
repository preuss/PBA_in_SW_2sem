package com.loanbroker.translators;

import com.loanbroker.handlers.HandlerThread;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConsumerCancelledException;
import com.rabbitmq.client.QueueingConsumer;
import com.rabbitmq.client.QueueingConsumer.Delivery;
import com.rabbitmq.client.ShutdownSignalException;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class WebserviceTranslator extends HandlerThread {

	private String queueIn;
	private String QUEUE_OUT = "Group2.WebserviceBank.Receive";
	private String replyToQueue;

	public WebserviceTranslator(String queueIn, String replyToQueue) {
		this.queueIn = queueIn;
		this.replyToQueue = replyToQueue;
	}

	private void receiveBankName() throws IOException, ConsumerCancelledException, ShutdownSignalException, InterruptedException {
		Connection connection = getConnection();
		Channel channel = connection.createChannel();
		
		channel.queueDeclare(queueIn, false, false, false, null);
		channel.queueDeclare(QUEUE_OUT, false, false, false, null);
		channel.queueDeclare(replyToQueue, false, false, false, null);
		
		QueueingConsumer consumer = new QueueingConsumer(channel);
		channel.basicConsume(queueIn, true, consumer);
		
		AMQP.BasicProperties.Builder builder = new AMQP.BasicProperties().builder();
		builder.replyTo(replyToQueue);
		AMQP.BasicProperties props = builder.build();

		while(!isPleaseStop()) {
			QueueingConsumer.Delivery delivery = consumer.nextDelivery();
			String xmlString = new String(delivery.getBody());
			channel.basicPublish("", QUEUE_OUT, props, xmlString.getBytes());
		}
	}

	@Override
	protected void doRun() {
		while (!isPleaseStop()) {
			try {
				receiveBankName();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (ConsumerCancelledException e) {
				e.printStackTrace();
			} catch (ShutdownSignalException e) {
				e.printStackTrace();
			} catch (InterruptedException e) {
				e.printStackTrace();
			} finally {

			}

		}
	}
}
