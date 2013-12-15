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
		while (!isPleaseStop()) {
			try {
				CanonicalDTO dto = readMessage();
				writeMessage(dto);
			} catch (IOException e) {
				log.log(Level.SEVERE, null, e);
			} catch (ConsumerCancelledException e) {
				log.log(Level.SEVERE, null, e);
			} catch (ShutdownSignalException e) {
				log.log(Level.SEVERE, null, e);
			} catch (InterruptedException e) {
				log.log(Level.SEVERE, null, e);
			}
		}
	}

	private CanonicalDTO readMessage() throws IOException, ConsumerCancelledException, ShutdownSignalException, InterruptedException {
		CanonicalDTO dto = null;
		Connection conn = null;
		Channel channel = null;
		try {
			conn = getConnection();
			channel = createChannel(conn, queueIn);
			QueueingConsumer consumer = new QueueingConsumer(channel);
			channel.basicConsume(queueIn, true, consumer);
			QueueingConsumer.Delivery delivery = consumer.nextDelivery();
			String message = new String(delivery.getBody());
			dto = convertStringToDto(message);
		} finally {
			closeChannel(channel);
			closeConnection(conn);
		}
		return dto;
	}

	private void writeMessage(CanonicalDTO dto) throws IOException {
		String xmlStr = convertDtoToString(dto);
		for (String queueOut : queuesOut) {
			Connection conn = null;
			Channel channel = null;
			try {
				channel = createChannel(conn, queueOut);
				channel.basicPublish("", queueOut, null, xmlStr.getBytes());
			} finally {
				closeChannel(channel);
				closeConnection(conn);
			}
		}
	}
}
