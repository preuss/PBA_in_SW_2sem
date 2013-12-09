package com.loanbroker;

import com.loanbroker.bank.JSONMockBank;
import com.loanbroker.bank.RabbitBank;
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

		String rabbitBankIn = "rabbit_bankRecieve";
		String rabbitBankOut = "rabbit_bankSend";
		RabbitBank rabbitBank = new RabbitBank(rabbitBankIn, rabbitBankOut);
//		rabbitBank.start();

		String mockJsonBankIn = "json_bankReceiver";
		String mockJsonBankOut = "json_bankSend";
		JSONMockBank jsonMockBank = new JSONMockBank(mockJsonBankIn, mockJsonBankOut);
//		jsonMockBank.start();

		String bankIn = "02_rating_channel";
		String bankOut = "02_rating_channel";
		log.debug("Starting bankhandler: " + bankIn + " >--> " + bankOut);
		BankHandler bankHandler = new BankHandler(bankIn, bankOut);
//		bankHandler.start();

		String recipientIn = "Group2_recipientIn";
		Map<String, String> recipientOut = new HashMap<>();
		recipientOut.put("xml", "xml_channel");
		recipientOut.put("json", "json_channel");
		recipientOut.put("rabbitmq", "rabbitmq_channel");
		recipientOut.put("webservice", "websercice_channel");
		for (Iterator<Map.Entry<String, String>> it = recipientOut.entrySet().iterator(); it.hasNext();) {
			Map.Entry<String, String> entry = it.next();
			log.debug("Starting recipientHandler: " + bankIn + " >--> " + entry.getValue() + "(" + entry.getKey() + ")");
		}
		RecipientHandler recipientHandler = new RecipientHandler(recipientIn, recipientOut);
		//		recipientHandler.start();
	}

}
