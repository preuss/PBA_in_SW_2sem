package com.loanbroker.loan_broker;

import com.loanbroker.loan_broker.logging.*;
import com.loanbroker.handlers.BankHandler;

/**
 * @author Preuss
 */
public class Starter {
	private static Logger log = Logger.getLogger(Starter.class);
	
	public static void main(String[] args) {
		LoggingSetup.setupLogging(Level.DEBUG);

		log.debug("Starting bankhandler");
		BankHandler bankHandler = new BankHandler();
	}
	
}
