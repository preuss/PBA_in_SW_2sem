package com.loanbroker;

import com.loanbroaker.translators.XmlTranslator;
import com.loanbroker.bank.JSONMockBank;
import com.loanbroker.bank.RabbitBank;
import com.loanbroker.logging.Level;
import com.loanbroker.logging.Logger;
import com.loanbroker.logging.LoggingSetup;
import com.loanbroker.handlers.BankHandler;
import com.loanbroker.handlers.CreditHandler;
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

		String rabbitBankIn = "rabbit_bankReceive";
		String rabbitBankOut = "rabbit_bankSend";
		RabbitBank rabbitBank = new RabbitBank(rabbitBankIn, rabbitBankOut);
//		rabbitBank.start();

		String mockJsonBankIn = "json_bankReceive";
		String mockJsonBankOut = "json_bankSend";
		JSONMockBank jsonMockBank = new JSONMockBank(mockJsonBankIn, mockJsonBankOut);
//		jsonMockBank.start();

		String creditIn = "creditReceive";
		String creditOut = "creditSend";
		CreditHandler creditHandler = new CreditHandler(creditIn, creditOut);
//		creditHandler.start();

		String bankIn = "ratingReceive";
		String bankOut = "ratingSend";
		log.debug("Starting bankhandler: " + bankIn + " >--> " + bankOut);
		BankHandler bankHandler = new BankHandler(bankIn, bankOut);
//		bankHandler.start();

		String recipientIn = "recipientReceive";
		Map<String, String> recipientOut = new HashMap<>();
		recipientOut.put("xml", "receipientSend_xml");
		recipientOut.put("json", "receipientSend_json");
		recipientOut.put("rabbitmq", "receipientSend?rabbitmq");
		recipientOut.put("webservice", "receipientSend_webservice");
		for (Iterator<Map.Entry<String, String>> it = recipientOut.entrySet().iterator(); it.hasNext();) {
			Map.Entry<String, String> entry = it.next();
			log.debug("Starting recipientHandler: " + bankIn + " >--> " + entry.getValue() + "(" + entry.getKey() + ")");
		}
		RecipientHandler recipientHandler = new RecipientHandler(recipientIn, recipientOut);
//		recipientHandler.start();

		XmlTranslator xmlTranslator = new XmlTranslator("02_bankXML", "02_xml_reply_queue");
//		xmlTranslator.start();

		String aggPeepIn = "aggPeepIn";
		String aggIn = "aggIn";
		String aggOut = "aggOut";
		Aggregator aggregator = new Aggregator(aggPeepIn, aggIn, aggOut);
//		aggregator.start();
	}

}
