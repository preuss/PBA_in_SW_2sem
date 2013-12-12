package com.loanbroker.translators;

import com.loanbroaker.translators.*;
import com.loanbroker.handlers.HandlerThread;
import com.loanbroker.logging.Level;
import com.loanbroker.logging.Logger;
import com.loanbroker.models.CanonicalDTO;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.QueueingConsumer;
import java.io.IOException;

/**
 *
 * @author Andreas
 */
public class RabbitmqTranslator extends HandlerThread {

	private final Logger log = Logger.getLogger(RabbitmqTranslator.class);

	//private final String EXCHANGE_NAME = "cphbusiness.bankXML";
	private final String RABBITMQ_BANK_IN = "Group2.RabbitBank.Receive";
	private final String queueNameReceive;
	private final String replyToQueue;

	public RabbitmqTranslator(String queueNameReceive, String replyToQueue) {
		this.queueNameReceive = queueNameReceive;
		this.replyToQueue = replyToQueue;
	}

	public void receiveBankName() throws IOException, InterruptedException, Exception {
		Channel channel = createChannel(queueNameReceive);
		
		System.out.println("RabbitmqTranslator Waiting for messages");
		QueueingConsumer consumer = new QueueingConsumer(channel);
		channel.basicConsume(queueNameReceive, true, consumer);
		//start polling messages
		while (true) {
			QueueingConsumer.Delivery delivery = consumer.nextDelivery();
			String message = new String(delivery.getBody());
			System.out.println("Received at RabbitmqTranslator" + message);
			CanonicalDTO dto = convertStringToDto(message);
			System.out.println("the score is " + dto.getCreditScore());
			sendRequestToXmlBank(translateMessage(dto));
		}
	}

	private String translateMessage(CanonicalDTO dto) {
		String rabbitmqValue = "ssn:" + dto.getSsn()
				+ "#creditScore:" + dto.getCreditScore()
				+ "#loanAmount:" + dto.getLoanAmount()
				+ "#loanDuration:" + dto.getLoanDuration();

		return rabbitmqValue;
	}

	private void sendRequestToXmlBank(String xmlString) throws IOException {
		Channel channel = createChannel(replyToQueue);
		
		AMQP.BasicProperties.Builder builder = new AMQP.BasicProperties().builder();
		builder.replyTo(replyToQueue);
		AMQP.BasicProperties props = builder.build();
		channel.basicPublish("", RABBITMQ_BANK_IN, props, xmlString.getBytes());
//		channel.basicPublish(EXCHANGE_NAME, "", props, xmlString.getBytes());
		System.out.println("Message Sent from translator: " + xmlString);
//      channel.close();
//      connection.close();
	}

	@Override
	protected void doRun() {
		while (isPleaseStop() == false) {
			try {
				receiveBankName();
			} catch (InterruptedException e) {
				log.log(Level.SEVERE, null, e);
			} catch (Exception e) {
				log.log(Level.SEVERE, null, e);
			}
		}
	}

}
