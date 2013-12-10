package com.loanbroker.handlers;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import java.io.IOException;
import wservices.CreditScoreService;
import wservices.CreditScoreService_Service;
import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.ConsumerCancelledException;
import com.rabbitmq.client.ShutdownSignalException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Andreas
 */
public class CreditHandler {

	//private final static String QUEUE_NAME = "02_rating_channel";
	private final String QUEUE_NAME_IN;
	private final String QUEUE_NAME_OUT;

	public CreditHandler(String inQueueName, String outQueueName) {
		QUEUE_NAME_IN = inQueueName;
		QUEUE_NAME_OUT = outQueueName;
	}

	public void getCreditScore() {

		try { // Call Web Service Operation
			CreditScoreService_Service service = new CreditScoreService_Service();
			CreditScoreService port = service.getCreditScoreServicePort();
			// TODO initialize WS operation arguments here
			String ssn = "123456-1234";
			// TODO process result here
			int result = port.creditScore(ssn);
			System.out.println("Result from Credit Bureau = " + result);
			if (result != -1) {
				getBanks(result);
			}
		} catch (IOException ex) {
			System.out.println(ex.getMessage());
		}

	}

	private String generateCorrelationID() {
		return java.util.UUID.randomUUID().toString();
	}

	private void getBanks(int rating) throws IOException {
		ConnectionFactory connfac = new ConnectionFactory();
		connfac.setHost("datdb.cphbusiness.dk");
		connfac.setPort(5672); //dette er rabbitMQ protokol-porten. 
		connfac.setUsername("student");
		connfac.setPassword("cph");
		Connection connection = connfac.newConnection();
		Channel channel = connection.createChannel();

		channel.queueDeclare(QUEUE_NAME_OUT, false, false, false, null);
		String message = "" + rating;
		System.out.println(" [x] Sent '" + message + "'");
		String corrId = generateCorrelationID();
		String replyTo = channel.queueDeclare().getQueue();
		BasicProperties.Builder propBuilder = new BasicProperties.Builder();
		propBuilder.correlationId(corrId);
		propBuilder.replyTo(replyTo);
		BasicProperties props = propBuilder.build();
		channel.basicPublish("", QUEUE_NAME_OUT, props, message.getBytes());

		BankHandler bh = new BankHandler();
		try {
			bh.receiveCreditScore();
		} catch (ShutdownSignalException ex) {
			Logger.getLogger(CreditHandler.class.getName()).log(Level.SEVERE, null, ex);
		} catch (ConsumerCancelledException ex) {
			Logger.getLogger(CreditHandler.class.getName()).log(Level.SEVERE, null, ex);
		} catch (InterruptedException ex) {
			Logger.getLogger(CreditHandler.class.getName()).log(Level.SEVERE, null, ex);
		} catch (Exception ex) {
                Logger.getLogger(CreditHandler.class.getName()).log(Level.SEVERE, null, ex);
            }
		channel.close();
		connection.close();

	}

}
