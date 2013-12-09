package com.loanbroker;

import com.loanbroker.logging.Level;
import com.loanbroker.logging.Logger;
import com.loanbroker.logging.LoggingSetup;
import com.loanbroker.handlers.BankHandler;
import com.loanbroker.handlers.RecipientHandler;
import java.util.HashMap;
import java.util.*;

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
		
        String recipientIn = "Group2_recipientIn";
        Map<String, String> recipientOut = new HashMap<>();
        recipientOut.put("xml", "xml_channel");
        recipientOut.put("json", "json_channel");
        recipientOut.put("rabbitmq", "rabbitmq_channel");
        recipientOut.put("webservice", "websercice_channel");
		RecipientHandler recipientHandler = new RecipientHandler(recipientIn, recipientOut);
		recipientHandler.start();
	}

}
