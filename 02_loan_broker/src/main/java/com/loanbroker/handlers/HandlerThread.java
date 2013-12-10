package com.loanbroker.handlers;

import com.loanbroker.models.CanonicalDTO;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.core.Persister;

/**
 * @author Preuss
 */
public abstract class HandlerThread extends Thread {

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
		} catch (Exception ex) {
			Logger.getLogger(HandlerThread.class.getName()).log(Level.SEVERE, null, ex);
		}
		return dto;
	}

	@Override
	public final void run() {
		doRun();
	}

	protected abstract void doRun();
}
