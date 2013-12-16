package com.loanbroker.handlers;

import com.loanbroker.logging.Level;
import com.loanbroker.logging.Logger;
import com.loanbroker.models.CanonicalDTO;
import com.rabbitmq.client.*;
import wservices.CreditScoreService;
import wservices.CreditScoreService_Service;

import java.io.IOException;

/**
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
	private CanonicalDTO readMessage(Connection connection, Channel channel, QueueingConsumer consumer) throws IOException, ConsumerCancelledException, ShutdownSignalException, InterruptedException {
		CanonicalDTO returnMessage = null;

		try {
			QueueingConsumer.Delivery delivery = consumer.nextDelivery();
			String xmlStr = null;
			if (delivery != null && delivery.getBody() != null) {
				xmlStr = new String(delivery.getBody());
			}
			if (xmlStr != null) {
				returnMessage = this.convertStringToDto(xmlStr);
			}
		} finally {
		}
		return returnMessage;
	}

	private void writeMessage(Connection connection, Channel channel, CanonicalDTO message) throws IOException {
		try {
			channel = createChannel(connection, queueOut);
			String xmlStr = convertDtoToString(message);
			channel.basicPublish("", queueOut, null, xmlStr.getBytes());
		} finally {
		}
	}

	private CanonicalDTO enrichMessageWithCreditScore(CanonicalDTO message) {
		int creditScore = getCreditScore(message.getSsn());
		log.debug("CreditScore: " + creditScore);
		message.setCreditScore(creditScore);
		return message;
	}

	@Override
	protected void doRun() {
		Connection connection = null;
		Channel channel = null;
		QueueingConsumer consumer = null;
		while (!isPleaseStop()) {
			try {
				if (connection == null) {
					connection = getConnection();
					channel = createChannel(queueIn);
					consumer = new QueueingConsumer(channel);
					channel.basicConsume(queueIn, true, consumer);
					channel.queueDeclare(queueOut, false, false, false, null);
				}
				CanonicalDTO canonDto = readMessage(connection, channel, consumer);
				if (canonDto != null) {
					canonDto = enrichMessageWithCreditScore(canonDto);
				} else {
					log.debug("Message is null");
				}
				writeMessage(connection, channel, canonDto);
			} catch (IOException e) {
				log.warning(e.getClass() + ", Message: " + e.getMessage());
				e.printStackTrace();
				if (e.getCause() != null) {
					log.warning("\t" + e.getCause().getClass() + ", " + e.getCause().getMessage());
					if (e.getCause().getCause() != null) {
						log.warning("\t\t" + e.getCause().getCause().getClass() + ", " + e.getCause().getCause().getMessage());
					}
				}
				connection = null;
			} catch (ConsumerCancelledException e) {
				log.warning(e.getClass() + ", Message: " + e.getMessage());
				e.printStackTrace();
				if (e.getCause() != null) {
					log.warning("\t" + e.getCause().getClass() + ", " + e.getCause().getMessage());
					if (e.getCause().getCause() != null) {
						log.warning("\t\t" + e.getCause().getCause().getClass() + ", " + e.getCause().getCause().getMessage());
					}
				}
			} catch (ShutdownSignalException e) {
				log.warning(e.getClass() + ", Message: " + e.getMessage());
				e.printStackTrace();
				if (e.getCause() != null) {
					log.warning("\t" + e.getCause().getClass() + ", " + e.getCause().getMessage());
					if (e.getCause().getCause() != null) {
						log.warning("\t\t" + e.getCause().getCause().getClass() + ", " + e.getCause().getCause().getMessage());
					}
				}
			} catch (InterruptedException e) {
				log.warning(e.getClass() + ", Message: " + e.getMessage());
				e.printStackTrace();
				if (e.getCause() != null) {
					log.warning("\t" + e.getCause().getClass() + ", " + e.getCause().getMessage());
					if (e.getCause().getCause() != null) {
						log.warning("\t\t" + e.getCause().getCause().getClass() + ", " + e.getCause().getCause().getMessage());
					}
				}
			}
		}
	}
}
