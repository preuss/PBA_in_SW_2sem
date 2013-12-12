package com.loanbroker;

import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.*;
import javax.xml.transform.stream.*;
import org.xml.sax.*;
import org.w3c.dom.*;
import com.loanbroker.handlers.HandlerThread;
import com.loanbroker.logging.Level;
import com.loanbroker.logging.Logger;
import com.loanbroker.models.BankDTO;
import com.loanbroker.models.CanonicalDTO;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.ConsumerCancelledException;
import com.rabbitmq.client.QueueingConsumer;
import com.rabbitmq.client.ShutdownSignalException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

/**
 * @author Preuss
 */
public class Normalizer extends HandlerThread {

	private final Logger log = Logger.getLogger(Normalizer.class);
	private Map<String, String> queuesIn;
	private String queueOut;

	public Normalizer(Map<String, String> bankInputQueues, String queueOut) {
		this.queuesIn = bankInputQueues;
		this.queueOut = queueOut;
	}

	private CanonicalDTO convertFromBankToDto(String bankName, String message) {
		CanonicalDTO dto = null;
		String ssn = null;
		double interestRate = Double.NaN;
		if ("xml".equalsIgnoreCase(bankName)) {
			/*
			 Example
			 <LoanResponse>
			 <interestRate>4.5600000000000005</interestRate>
			 <ssn>12345678</ssn>
			 </LoanResponse>
			 */
			Document document;
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			try {
				DocumentBuilder db = dbf.newDocumentBuilder();
				document = db.parse(message);
				String interestRateStr = document.getElementsByTagName("interestRate").item(0).getTextContent();
				interestRate = Double.parseDouble(interestRateStr);
				ssn = document.getElementsByTagName("ssn").item(0).getTextContent();
			} catch (ParserConfigurationException e) {
				log.log(java.util.logging.Level.SEVERE, null, e);
			} catch (SAXException e) {
				log.log(java.util.logging.Level.SEVERE, null, e);
			} catch (IOException e) {
				log.log(java.util.logging.Level.SEVERE, null, e);
			}
		} else if ("json".equalsIgnoreCase(bankName)) {
			try {
				/*
				 Example of json response:
				 {"interestRate":5.5,"ssn":1605789787}
				 */
				JSONObject json = new JSONObject(message);
				ssn = json.getString("ssn");
				interestRate = json.getDouble("interestRate");
			} catch (JSONException e) {
				log.log(java.util.logging.Level.SEVERE, null, e);
			}
		} else if ("rabbit".equalsIgnoreCase(bankName)) {
			/*
			 example output:
			 "interestRate:5.5#ssn:160578-9787"
			 */
			for (String keyValue : message.split("#")) {
				String key = keyValue.split(":")[0];
				String value = keyValue.split(":")[1];
				if ("interestRate".equalsIgnoreCase(key)) {
					interestRate = Double.parseDouble(value);
				} else if ("ssn".equalsIgnoreCase(key)) {
					ssn = value;
				}
			}
		}

		if (ssn == null || interestRate == Double.NaN) {
			dto = null;
		} else {
			if (ssn.length() == 10) {
				ssn = ssn.substring(0, 8) + "-" + ssn.substring(8);
			}
			if (ssn.length() == 11) {
				dto = new CanonicalDTO();
				dto.setSsn(ssn);
				BankDTO bank = new BankDTO(bankName, interestRate);
				dto.addBank(bank);
			} else {
				dto = null;
			}
		}
		return dto;
	}

	private CanonicalDTO readBankMessage(String bankName, String queueIn) throws IOException, ConsumerCancelledException, ShutdownSignalException, InterruptedException {
		CanonicalDTO dto;
		Channel channel = createChannel(queueIn);
		QueueingConsumer consumer = new QueueingConsumer(channel);
		channel.basicConsume(queueIn, true, consumer);
		int timeoutMilli = 10;
		QueueingConsumer.Delivery delivery = consumer.nextDelivery(timeoutMilli);

		if (delivery == null) {
			dto = null;
		} else {
			dto = convertFromBankToDto(bankName, delivery.getBody().toString());
		}
		return dto;
	}

	private List<CanonicalDTO> readBankMessages() throws InterruptedException, ShutdownSignalException, ConsumerCancelledException, IOException {
		List<CanonicalDTO> messages = new ArrayList<>();

		for (Entry<String, String> keyValue : queuesIn.entrySet()) {
			String bankName = keyValue.getKey();
			String queueIn = keyValue.getValue();
			CanonicalDTO message = readBankMessage(bankName, queueIn);
			if (message != null) {
				messages.add(message);
			}
		}
		return messages;
	}

	private void writeBankMessages(List<CanonicalDTO> messages) throws IOException {
		Channel channel = createChannel(queueOut);
		for (CanonicalDTO canonicalDTO : messages) {
			String xmlStr = convertDtoToString(canonicalDTO);
			channel.basicPublish("", queueOut, null, xmlStr.getBytes());
		}
	}

	@Override
	protected void doRun() {
		while (!isPleaseStop()) {
			try {
				List<CanonicalDTO> messages = readBankMessages();
				writeBankMessages(messages);
			} catch (InterruptedException e) {
				log.log(Level.SEVERE, null, e);
			} catch (ShutdownSignalException e) {
				log.log(Level.SEVERE, null, e);
			} catch (ConsumerCancelledException e) {
				log.log(Level.SEVERE, null, e);
			} catch (IOException e) {
				log.log(Level.SEVERE, null, e);
			}
		}
	}
}
