package com.loanbroker.handlers;

import com.loanbroker.logging.Level;
import com.loanbroker.logging.Logger;
import com.loanbroker.models.CanonicalDTO;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.core.Persister;

/**
 * @author Preuss
 */
public abstract class HandlerThread extends Thread {

	private final Logger log = Logger.getLogger(HandlerThread.class);

	private boolean pleaseStop = false;

	protected final boolean isPleaseStop() {
		return pleaseStop;
	}

	public final void pleaseStop() {
		pleaseStop = true;
	}

	protected final Connection getConnection() throws IOException {
		ConnectionFactory connfac = new ConnectionFactory();
		connfac.setHost("datdb.cphbusiness.dk");
		connfac.setPort(5672);
		connfac.setUsername("student");
		connfac.setPassword("cph");
		Connection connection = connfac.newConnection();
		return connection;
	}

	protected final Channel createChannel(String queueName) throws IOException {
		Connection conn = getConnection();
		Channel channel = conn.createChannel();
		if (queueExist(queueName) == false) {
			channel.queueDeclare(queueName, false, false, false, null);
		}
		return channel;
	}

	protected final boolean queueExist(String queueName) {
		boolean queueExists = false;
		try {
			// Need to get own channel, to test.
			// Because the test "declarePassive" closes the channel.
			Channel channel = getConnection().createChannel();
			AMQP.Queue.DeclareOk declare = channel.queueDeclarePassive(queueName);
			//log.debug("Declare: " + declare);
			queueExists = true;
		} catch (IOException e) {
			//log.log(Level.SEVERE, null, e);
			//log.critical("queue does not exists? : " + e.getMessage());
			queueExists = false;
		}
		return queueExists;
	}

	protected final String convertDtoToString(CanonicalDTO canonicalDTO) {
		String retVal = null;
		Serializer serializer = new Persister();
		OutputStream stream = new ByteArrayOutputStream();
		try {
			serializer.write(canonicalDTO, stream);
			retVal = stream.toString();
		} catch (Exception ex) {
			Logger.getLogger(HandlerThread.class.getName()).log(Level.SEVERE, null, ex);
		}
		return retVal;
	}

	protected final CanonicalDTO convertStringToDto(String xmlString) {
		CanonicalDTO dto = null;
		Serializer serializer = new Persister();
		try {
			dto = serializer.read(CanonicalDTO.class, xmlString);
		} catch (Exception e) {
			log.log(Level.SEVERE, null, e);
		}
		return dto;
	}

	@Override
	public final void run() {
		doRun();
	}

	protected abstract void doRun();
}
