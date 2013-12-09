package com.loanbroker;

import com.loanbroker.logging.Level;
import com.loanbroker.logging.Logger;
import com.loanbroker.logging.LoggingSetup;
import com.loanbroker.handlers.BankHandler;

/**
 * @author Preuss
 */
public class Starter {

	private static Logger log = Logger.getLogger(Starter.class);

	public static void main(String[] args) {
		LoggingSetup.setupLogging(Level.DEBUG);

		String bankIn = "02_rating_channel";
		String bankOut = "02_rating_channel";
		log.debug("Starting bankhandler: " + bankIn + " >--> " + bankOut);
		BankHandler bankHandler = new BankHandler(bankIn, bankOut);
		bankHandler.start();
	}

}
