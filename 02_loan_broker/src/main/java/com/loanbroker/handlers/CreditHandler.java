package com.loanbroker.handlers;

import com.loanbroker.logging.Level;
import com.loanbroker.logging.Logger;
import com.loanbroker.models.CanonicalDTO;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import java.io.IOException;
import wservices.CreditScoreService;
import wservices.CreditScoreService_Service;
import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.ConsumerCancelledException;
import com.rabbitmq.client.QueueingConsumer;
import com.rabbitmq.client.ShutdownSignalException;

/**
 *
 * @author Andreas
 */
public class CreditHandler extends HandlerThread {
	private final Logger log = Logger.getLogger(CreditHandler.class);

	private final String queueIn;
	private final String queueOut;

	public CreditHandler(String inQueueName, String outQueueName) {
		queueIn = inQueueName;
		queueOut = outQueueName;
	}

	public int getCreditScore(String ssn) {
		int creditScore;

		CreditScoreService_Service service = new CreditScoreService_Service();
		CreditScoreService port = service.getCreditScoreServicePort();

		// TODO initialize WS operation arguments here
		// TODO process result here
		creditScore = port.creditScore(ssn);
		System.out.println("Result from Credit Bureau = " + creditScore);

		return creditScore;
	}

	/*private String generateCorrelationID() {
		return java.util.UUID.randomUUID().toString();
	}*/

	/*private void getBanks(int creditScore) throws IOException {
		ConnectionFactory connfac = new ConnectionFactory();
		connfac.setHost("datdb.cphbusiness.dk");
		connfac.setPort(5672); //dette er rabbitMQ protokol-porten. 
		connfac.setUsername("student");
		connfac.setPassword("cph");
		Connection connection = connfac.newConnection();
		Channel channel = connection.createChannel();

		channel.queueDeclare(queueOut, false, false, false, null);
		String message = "" + creditScore;
		System.out.println(" [x] Sent '" + message + "'");
		String corrId = generateCorrelationID();
		String replyTo = channel.queueDeclare().getQueue();
		BasicProperties.Builder propBuilder = new BasicProperties.Builder();
		propBuilder.correlationId(corrId);
		propBuilder.replyTo(replyTo);
		BasicProperties props = propBuilder.build();
		channel.basicPublish("", queueOut, props, message.getBytes());

		BankHandler bh = new BankHandler();
		try {
			bh.receiveCreditScore();
		} catch (ShutdownSignalException e) {
			throw new RuntimeException(e);
		} catch (ConsumerCancelledException e) {
			throw new RuntimeException(e);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		channel.close();
		connection.close();
	}*/

	private CanonicalDTO readMessage() throws IOException, ConsumerCancelledException, ShutdownSignalException, InterruptedException {
		CanonicalDTO returnMessage;

		Channel channel = createChannel(queueIn);
		QueueingConsumer consumer = new QueueingConsumer(channel);
		channel.basicConsume(queueIn, true, consumer);

		QueueingConsumer.Delivery delivery = consumer.nextDelivery();
		String xmlStr = delivery.getBody().toString();
		returnMessage = this.convertStringToDto(xmlStr);

		return returnMessage;
	}

	private void writeMessage(CanonicalDTO message) throws IOException {
		Channel channel = createChannel(queueOut);
		String xmlStr = convertDtoToString(message);
		channel.basicPublish("", queueOut, null, xmlStr.getBytes());
	}

	private CanonicalDTO enrichMessageWithCreditScore(CanonicalDTO message) {
		int creditScore = getCreditScore(message.getSsn());
		message.setCreditScore(creditScore);
		return message;
	}

	@Override
	protected void doRun() {
		while (!isPleaseStop()) {
			try {
				CanonicalDTO canonDto = readMessage();
				canonDto = enrichMessageWithCreditScore(canonDto);
				writeMessage(canonDto);
			} catch (IOException e) {
				Logger.getLogger(RecipientHandler.class.getName()).log(Level.SEVERE, null, e);
			} catch (ConsumerCancelledException e) {
				Logger.getLogger(RecipientHandler.class.getName()).log(Level.SEVERE, null, e);
			} catch (ShutdownSignalException e) {
				Logger.getLogger(RecipientHandler.class.getName()).log(Level.SEVERE, null, e);
			} catch (InterruptedException e) {
				Logger.getLogger(RecipientHandler.class.getName()).log(Level.SEVERE, null, e);
			}
		}
	}
}
