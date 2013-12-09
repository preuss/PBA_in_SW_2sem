package com.loanbroker.handlers;

import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import java.io.IOException;

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

	@Override
	public final void run() {
		doRun();
	}

	protected abstract void doRun();
}
