package com.loanbroker.handlers;

import com.loanbroker.logging.Logger;
import com.loanbroker.models.CanonicalDTO;
import com.rabbitmq.client.*;
import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.core.Persister;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * @author Preuss
 */
public abstract class HandlerThread extends Thread {

	private final Logger log = Logger.getLogger(HandlerThread.class);
	private boolean pleaseStop = false;
	private Connection connection;
	private Channel channel;

	protected final boolean isPleaseStop() {
		return pleaseStop;
	}

	public final void pleaseStop() {
		pleaseStop = true;
	}

	protected final ConnectionFactory newConnectionFactory() {
		ConnectionFactory connfac = new ConnectionFactory();
		connfac.setHost("datdb.cphbusiness.dk");
		connfac.setPort(5672);
		connfac.setUsername("student");
		connfac.setPassword("cph");
		return connfac;
	}

	protected final Connection getConnection() throws IOException {
		try {
			if (connection == null) {
				connection = newConnectionFactory().newConnection();
			}
			return connection;
		} catch (IOException e) {
			log.debug("getConnection: Inside IOException");
			if (e.getCause() != null && e.getMessage() == null) {
				log.debug("getConnection: Now Message is null");
				if (e.getCause() instanceof ShutdownSignalException) {
					ShutdownSignalException exception = (ShutdownSignalException) e.getCause();
					throw exception;
				}
			}
			connection = null;
			throw e;
		}
	}

	protected final Channel createChannel(String queueName) throws IOException {
		try {
			return this.createChannel(getConnection(), queueName);
		} catch (IOException e) {
			if (e.getCause() != null && e.getMessage() == null) {
				log.debug("getConnection: Now Message is null");
				if (e.getCause() instanceof ShutdownSignalException) {
					ShutdownSignalException exception = (ShutdownSignalException) e.getCause();
					throw exception;
				}
			}
			throw e;
		}
	}

	protected final Channel createChannel(Connection conn, String queueName) throws IOException {
		try {
			if (channel == null) {
				channel = getConnection().createChannel();
			}
		} catch (IOException e) {
			log.debug("getConnection: Inside IOException");
			if (e.getCause() != null && e.getMessage() == null) {
				log.debug("getConnection: Now Message is null");
				if (e.getCause() instanceof ShutdownSignalException) {
					ShutdownSignalException exception = (ShutdownSignalException) e.getCause();
					throw exception;
				}
			}
			throw e;
		}
		if (queueExist(queueName) == false) {
			channel.queueDeclare(queueName, false, false, false, null);
		}
		return channel;
	}

	protected final void closeConnection(Connection conn) {
		if (conn != null) {
			try {
				conn.close();
			} catch (IOException e) {
				log.debug(e.getClass() + ": " + e.getMessage());
				if (e.getCause() != null) {
					log.debug("\t" + e.getCause().getClass() + ": " + e.getCause().getMessage());
				}
			}
		}
	}

	protected final void closeChannel(Channel channel) {
		if (channel != null) {
			try {
				if (channel.getConnection().isOpen()) {
					channel.close();
				}
			} catch (IOException e) {
				log.debug(e.getClass() + ": " + e.getMessage());
				if (e.getCause() != null) {
					log.debug("\t" + e.getCause().getClass() + ": " + e.getCause().getMessage());
				}
			}
		}
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
			log.trace(e.getClass() + ", " + e.getMessage());
			if (e.getCause() != null) {
				log.trace("\t" + e.getCause().getClass() + ", " + e.getCause().getMessage());
				if (e.getCause().getCause() != null) {
					log.trace("\t\t" + e.getCause().getCause().getClass() + ", " + e.getCause().getCause().getMessage());
				}
			}
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
		} catch (Exception e) {
			log.severe(e.getClass() + ": " + e.getMessage());
			if (e.getCause() != null) {
				log.severe("\t" + e.getCause().getClass() + ": " + e.getCause().getMessage());
			}
		}
		return retVal;
	}

	protected final CanonicalDTO convertStringToDto(String xmlString) {
		CanonicalDTO dto = null;
		Serializer serializer = new Persister();
		try {
			dto = serializer.read(CanonicalDTO.class, xmlString);
		} catch (Exception e) {
			e.printStackTrace();
			log.severe(e.getClass() + ": " + e.getMessage());
			if (e.getCause() != null) {
				log.severe("\t" + e.getCause().getClass() + ": " + e.getCause().getMessage());
			}
		}
		return dto;
	}

	@Override
	public void run() {
		doRun();
	}

	protected abstract void doRun();
}
