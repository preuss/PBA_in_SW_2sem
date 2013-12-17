package com.loanbroker;

import com.loanbroker.translators.JsonTranslator;
import com.loanbroker.translators.XmlTranslator;
import com.loanbroker.bank.RabbitBank;
import com.loanbroker.logging.Level;
import com.loanbroker.logging.Logger;
import com.loanbroker.logging.LoggingSetup;
import com.loanbroker.handlers.BankHandler;
import com.loanbroker.handlers.CreditHandler;
import com.loanbroker.handlers.RecipientHandler;
import com.loanbroker.translators.RabbitmqTranslator;
import java.util.HashMap;
import java.util.*;

/**
 * @author Preuss
 */
public class Starter {

	private static Logger log = Logger.getLogger(Starter.class);

	public static void main(String[] args) {
		LoggingSetup.setupLogging(Level.DEBUG);

		String rabbitBankIn = "Group2.RabbitBank.Receive";
//		String rabbitBankOut = "Group2.RabbitBank.Send";
//		RabbitBank rabbitBank = new RabbitBank(rabbitBankIn, rabbitBankOut);
		RabbitBank rabbitBank = new RabbitBank(rabbitBankIn);
		rabbitBank.start();

		/*
		 String mockJsonBankIn = "Group2.JsonBank.Receive";
		 String mockJsonBankOut = "Group2.JsonBank.Send";
		 JSONMockBank jsonMockBank = new JSONMockBank(mockJsonBankIn, mockJsonBankOut);
		 //		jsonMockBank.start();
		 */
		String creditIn = "Group2.CreditHandler.Receive";
		String creditOut = "Group2.BankHandler.Receive";
		CreditHandler creditHandler = new CreditHandler(creditIn, creditOut);
		creditHandler.start();

		String bankIn = "Group2.BankHandler.Receive";
		String bankOut = "Group2.BankHandler.Send";
		log.debug("Starting bankhandler: " + bankIn + " >--> " + bankOut);
		BankHandler bankHandler = new BankHandler(bankIn, bankOut);
		bankHandler.start();

		String aggregatorFanoutIn = "Group2.BankHandler.Send";
		String[] aggregatorFanoutOutArray = {
			"Group2.RecipientHandler.Receive", 
			"Group2.Aggregator.PeepIn"
		};
		AggregatorFanout fanout = new AggregatorFanout(aggregatorFanoutIn, aggregatorFanoutOutArray);
		fanout.start();

		String recipientIn = "Group2.RecipientHandler.Receive";
		Map<String, String> recipientOut = new HashMap<>();
		recipientOut.put("xml", "Group2.Translator.Xml");
		recipientOut.put("json", "Group2.Translator.Json");
		recipientOut.put("rabbitmq", "Group2.Translator.Rabbitmq");
//		recipientOut.put("webservice", "Group2.RecipientHandler.Webservice.Send");
		for (Iterator<Map.Entry<String, String>> it = recipientOut.entrySet().iterator(); it.hasNext();) {
			Map.Entry<String, String> entry = it.next();
			//log.debug("Starting recipientHandler: " + bankIn + " >--> " + entry.getValue() + "(" + entry.getKey() + ")");
		}
		RecipientHandler recipientHandler = new RecipientHandler(recipientIn, recipientOut);
		recipientHandler.start();

		XmlTranslator xmlTranslator = new XmlTranslator("Group2.Translator.Xml", "Group2.Normalizer.Xml");
		xmlTranslator.start();

		String jsonTranslatorIn = "Group2.Translator.Json";
		String jsonTranslatorReplyTo = "Group2.Normalizer.Json";
		JsonTranslator jsonTranslator = new JsonTranslator(jsonTranslatorIn, jsonTranslatorReplyTo);
		jsonTranslator.start();
		
		String rabbitmqTranslatorIn ="Group2.Translator.Rabbitmq";
		String rabbitmqTranslatorReplyTo = "Group2.Normalizer.Rabbitmq";
		RabbitmqTranslator rabbitTranslator = new RabbitmqTranslator(rabbitmqTranslatorIn, rabbitmqTranslatorReplyTo);
		rabbitTranslator.start();

		Map<String, String> normalizerBankIn = new HashMap<>();
		normalizerBankIn.put("xml", "Group2.Normalizer.Xml");
		normalizerBankIn.put("json", "Group2.Normalizer.Json");
		normalizerBankIn.put("rabbitmq", "Group2.Normalizer.Rabbitmq");
//		normalizerBankIn.put("webservice", "receipientSend_webservice");
		String normalizerOut = "Group2.Aggregator.Receive";
		Normalizer normalizer = new Normalizer(normalizerBankIn, normalizerOut);
		normalizer.start();

		String aggPeepIn = "Group2.Aggregator.PeepIn";
		String aggIn = "Group2.Aggregator.Receive";
		String aggOut = "Group2.Aggregator.Send";
		Aggregator aggregator = new Aggregator(aggPeepIn, aggIn, aggOut);
//		aggregator.start();

	}

}
