package com.loanbroker.translators;

import com.loanbroker.bank.RabbitBank;
import com.loanbroker.handlers.HandlerThread;
import com.loanbroker.logging.Level;
import com.loanbroker.logging.Logger;
import com.loanbroker.models.CanonicalDTO;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConsumerCancelledException;
import com.rabbitmq.client.QueueingConsumer;
import com.rabbitmq.client.ShutdownSignalException;

import java.io.IOException;

/**
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

	public void receiveBankName(Connection connection, Channel channel) throws IOException, InterruptedException {
		try {

			System.out.println("RabbitmqTranslator Waiting for messages");
			QueueingConsumer consumer = new QueueingConsumer(channel);
			channel.basicConsume(queueNameReceive, true, consumer);
			//start polling messages
			while (true) {
				QueueingConsumer.Delivery delivery = consumer.nextDelivery();
				String message = new String(delivery.getBody());
				System.out.println("Received at RabbitmqTranslator: " + message.replace("\t", "").replace(" ", "").replace("\n", "").replace("\r", ""));
				CanonicalDTO dto = convertStringToDto(message);
				System.out.println("the score is " + dto.getCreditScore());
				sendRequestToXmlBank(channel, translateMessage(dto));
			}
			/*} catch (IOException e) {
			 log.severe(e.getClass() + ": " + e.getMessage());
			 if (e.getCause() != null) {
			 log.severe("\t" + e.getCause().getClass() + ": " + e.getCause().getMessage());
			 }*/
		} finally {
			closeChannel(channel);
			closeConnection(connection);
		}
	}

	private String translateMessage(CanonicalDTO dto) {
		String rabbitmqValue = "ssn:" + dto.getSsn()
			+ "#creditScore:" + dto.getCreditScore()
			+ "#loanAmount:" + dto.getLoanAmount()
			+ "#loanDuration:" + dto.getLoanDuration();

		return rabbitmqValue;
	}

	private void sendRequestToXmlBank(Channel channel, String xmlString) throws IOException {
		channel.queueDeclare(replyToQueue, false, false, false, null);

		AMQP.BasicProperties.Builder builder = new AMQP.BasicProperties().builder();
		builder.replyTo(replyToQueue);
		AMQP.BasicProperties props = builder.build();
		channel.basicPublish("", RABBITMQ_BANK_IN, props, xmlString.getBytes());
//		channel.basicPublish(EXCHANGE_NAME, "", props, xmlString.getBytes());
		System.out.println("Message Sent from translator: " + xmlString.replace("\t", "").replace(" ", "").replace("\n", "").replace("\r", ""));
	}

	@Override
	protected void doRun() {
		Connection connection = null;
		Channel channel = null;
		try {
			connection = getConnection();
			channel = createChannel(connection, queueNameReceive);

			while (isPleaseStop() == false) {
				receiveBankName(connection, channel);
			}
		} catch (IOException e) {
			Logger.getLogger(RabbitBank.class.getName()).log(Level.SEVERE, null, e);
			log.critical(e.getMessage());
		} catch (InterruptedException e) {
			Logger.getLogger(RabbitBank.class.getName()).log(Level.SEVERE, null, e);
			log.critical(e.getMessage());
		} catch (ShutdownSignalException e) {
			Logger.getLogger(RabbitBank.class.getName()).log(Level.SEVERE, null, e);
			log.critical("Shutdown: " + e.getMessage());
		} catch (ConsumerCancelledException e) {
			Logger.getLogger(RabbitBank.class.getName()).log(Level.SEVERE, null, e);
			log.critical(e.getMessage());
		} finally {
			closeChannel(channel);
			closeConnection(connection);
		}
	}

}
