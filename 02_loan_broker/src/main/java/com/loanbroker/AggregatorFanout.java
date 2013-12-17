package com.loanbroker;

import com.loanbroker.handlers.HandlerThread;
import com.loanbroker.logging.Logger;
import com.loanbroker.models.CanonicalDTO;
import com.rabbitmq.client.*;

import java.io.IOException;
import java.util.logging.Level;

/**
 * @author Preuss
 */
public class AggregatorFanout extends HandlerThread {
	
	private final Logger log = Logger.getLogger(AggregatorFanout.class);
	private final String queueIn;
	private final String[] queuesOut;
	
	public AggregatorFanout(String queueIn, String[] queuesOut) {
		this.queueIn = queueIn;
		this.queuesOut = queuesOut;
	}
	
	@Override
	protected void doRun() {
		Connection connection = null;
		Channel channel = null;
		QueueingConsumer consumer = null;
		while (!isPleaseStop()) {
			try {
				// Opretter vi connections og queues.
				if (connection == null) {
					connection = getConnection();
					channel = createChannel(connection, queueIn);
					consumer = new QueueingConsumer(channel);
					channel.basicConsume(queueIn, true, consumer);
					for (int i = 0; i < queuesOut.length; i++) {
						channel.queueDeclare(queuesOut[i], false, false, false, null);
					}
				}
				
				CanonicalDTO dto = readMessage(consumer);
				log.debug("Fanout Message: " + dto);
				writeMessage(channel, dto);
			} catch (IOException e) {
				e.printStackTrace();
				if (e.getCause() != null) {
					log.warning("\t" + e.getCause().getClass() + ", " + e.getCause().getMessage());
					if (e.getCause().getCause() != null) {
						log.warning("\t\t" + e.getCause().getCause().getClass() + ", " + e.getCause().getCause().getMessage());
					}
				}
				connection = null;
			} catch (ConsumerCancelledException e) {
				e.printStackTrace();
				if (e.getCause() != null) {
					log.warning("\t" + e.getCause().getClass() + ", " + e.getCause().getMessage());
					if (e.getCause().getCause() != null) {
						log.warning("\t\t" + e.getCause().getCause().getClass() + ", " + e.getCause().getCause().getMessage());
					}
				}
			} catch (ShutdownSignalException e) {
				e.printStackTrace();
				if (e.getCause() != null) {
					log.warning("\t" + e.getCause().getClass() + ", " + e.getCause().getMessage());
					if (e.getCause().getCause() != null) {
						log.warning("\t\t" + e.getCause().getCause().getClass() + ", " + e.getCause().getCause().getMessage());
					}
				}
				connection = null;
			} catch (InterruptedException e) {
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
	
	private CanonicalDTO readMessage(QueueingConsumer consumer) throws ConsumerCancelledException, ShutdownSignalException, InterruptedException {
		CanonicalDTO dto = null;
		QueueingConsumer.Delivery delivery = consumer.nextDelivery();
		String message = new String(delivery.getBody());
		dto = convertStringToDto(message);
		return dto;
	}
	
	private void writeMessage(Channel channel, CanonicalDTO dto) throws IOException {
		String xmlStr = convertDtoToString(dto);
		for (String queueOut : queuesOut) {
			channel.basicPublish("", queueOut, null, xmlStr.getBytes());
			log.debug("Fanout to Queue (" + queueOut + ") : " + xmlStr.replace("\r", "").replace("\n", "").replace("\t", "").replace(" ", ""));
		}
	}
}
