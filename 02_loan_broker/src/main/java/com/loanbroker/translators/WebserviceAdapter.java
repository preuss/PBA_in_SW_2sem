package com.loanbroker.translators;

import com.loanbroker.handlers.HandlerThread;
import com.loanbroker.logging.Logger;
import com.loanbroker.logging.Level;
import com.loanbroker.models.BankDTO;
import com.loanbroker.models.CanonicalDTO;
import com.loanbroker.webservice.client.Offer;
import com.loanbroker.webservice.client.WebserviceBank;
import com.loanbroker.webservice.client.WebserviceBank_Service;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConsumerCancelledException;
import com.rabbitmq.client.QueueingConsumer;
import com.rabbitmq.client.QueueingConsumer.Delivery;
import com.rabbitmq.client.ShutdownSignalException;
import java.io.IOException;
import java.util.ArrayList;

public class WebserviceAdapter extends HandlerThread {
	private Logger log = Logger.getLogger(WebserviceAdapter.class);

	private String queueIn;

	public WebserviceAdapter(String queueIn) {
		this.queueIn = queueIn;
	}

	private CanonicalDTO getLoanOffer(CanonicalDTO dto) {
		try { // Call Web Service Operation
			WebserviceBank_Service service = new WebserviceBank_Service();
			WebserviceBank port = service.getWebserviceBankPort();
			// TODO initialize WS operation arguments here
			java.lang.String ssn = dto.getSsn();
			int creditScore = dto.getCreditScore();
			double loanAmount = dto.getLoanAmount();
			int loanDuration = dto.getLoanDuration();
			// TODO process result here
			Offer result = port.calculateInterest(ssn, creditScore, loanAmount, loanDuration);

			dto.setBanks(new ArrayList<BankDTO>());
			dto.addBank(new BankDTO("Webservice", result.getInterestRate()));
		} catch (Exception e) {
			e.printStackTrace();
		}
		return dto;
	}

	//private Offer 
	@Override
	protected void doRun() {
		Connection connection = null;
		Channel channel = null;
		QueueingConsumer consumer = null;
		while (!isPleaseStop()) {
			try {
				if (connection == null) {
					connection = getConnection();
					channel = connection.createChannel();
					channel.queueDeclare(queueIn, false, false, false, null);
					consumer = new QueueingConsumer(channel);
					channel.basicConsume(queueIn, true, consumer);
				}
				QueueingConsumer.Delivery delivery = consumer.nextDelivery();
				CanonicalDTO dto = convertStringToDto(new String(delivery.getBody()));
				dto = getLoanOffer(dto);
				
				String replyToQueue = delivery.getProperties().getReplyTo();
				channel.queueDeclare(replyToQueue, false, false, false, null);
				
				channel.basicPublish("", replyToQueue, null, convertDtoToString(dto).getBytes());
			} catch (IOException e) {
				e.printStackTrace();
				connection = null;
			} catch (InterruptedException e) {
				e.printStackTrace();
			} catch (ShutdownSignalException e) {
				e.printStackTrace();
			} catch (ConsumerCancelledException e) {
				e.printStackTrace();
			} finally {

			}
		}
		log.debug("Stopped: WebserviceAdapter");
	}

}
