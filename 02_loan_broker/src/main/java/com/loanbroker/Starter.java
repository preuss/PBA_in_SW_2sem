package com.loanbroker;

import com.loanbroker.logging.Level;
import com.loanbroker.logging.Logger;
import com.loanbroker.logging.LoggingSetup;
import com.loanbroker.handlers.BankHandler;
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
