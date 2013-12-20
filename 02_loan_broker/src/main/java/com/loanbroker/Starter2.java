package com.loanbroker;

import com.loanbroker.logging.Logger;
import com.loanbroker.models.BankDTO;
import com.loanbroker.models.CanonicalDTO;
import com.loanbroker.webservice.loanwebservice.client.LoanWebservice;
import com.loanbroker.webservice.loanwebservice.client.LoanWebservice_Service;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.QueueingConsumer;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.core.Persister;

/**
 *
 * @author Preuss
 */
public class Starter2 {
	private final Logger log = Logger.getLogger(Starter2.class);

	private static void TestWebserviceClient() {
		System.out.println("Start Client");
		try { // Call Web Service Operation
			LoanWebservice_Service service = new LoanWebservice_Service();
			System.out.println("Service");
			LoanWebservice port = service.getLoanWebservicePort();
			System.out.println("Port");
			// TODO initialize WS operation arguments here
			String ssn = "123456-7891";
			double loanAmount = 5.0d;
			int loanDuration = 100;
			// TODO process result here
			String result = null;
			//result = port.loanBroker(ssn, loanAmount, loanDuration);
			
			result = port.loanBroker("100000-1000", 11000, 10);
			System.out.println("Result = " + result);
			result = port.loanBroker("100001-1001", 12000, 20);
			System.out.println("Result = " + result);
			result = port.loanBroker("100002-1002", 13000, 30);
			System.out.println("Result = " + result);
			result = port.loanBroker("100003-1003", 14000, 40);
			System.out.println("Result = " + result);
			result = port.loanBroker("100004-1004", 15000, 50);
			System.out.println("Result = " + result);
			result = port.loanBroker("100005-1005", 16000, 60);
			System.out.println("Result = " + result);
			result = port.loanBroker("100006-1006", 17000, 70);
			System.out.println("Result = " + result);
			
			//System.out.println("Result = " + result);
/*			result = port.loanBroker(ssn, loanAmount, loanDuration);
			System.out.println("Result = " + result);
			result = port.loanBroker(ssn, loanAmount, loanDuration);
			System.out.println("Result = " + result);*/
		} catch (Exception ex) {
			// TODO handle custom exceptions here
			ex.printStackTrace();
		}

	}

	public static void main(String[] args) throws IOException, InterruptedException {
		TestWebserviceClient();
		/*
		Starter2 s = new Starter2();
		String result = null;
		result = s.loanBroker("123456-7890", 10000, 60);
		System.out.println(result);
		result = s.loanBroker("123456-7891", 10000, 60);
		System.out.println(result);
		result = s.loanBroker("123456-7892", 10000, 60);
		System.out.println(result);
		result = s.loanBroker("123456-7893", 10000, 60);
		System.out.println(result);
		result = s.loanBroker("123456-7894", 10000, 60);
		System.out.println(result);
		*/
	}

	public String loanBroker(String ssn, double loanAmount, int loanDuration) throws IOException, InterruptedException {
		StringBuilder b = new StringBuilder();

		String creditIn = "Group2.CreditHandler.Receive";
		String aggOut = "Group2.Aggregator.Send";

		Connection connection = null;
		Channel channel = null;
		QueueingConsumer consumer = null;

		ConnectionFactory connfac = new ConnectionFactory();
		connfac.setHost("datdb.cphbusiness.dk");
		connfac.setPort(5672);
		connfac.setUsername("student");
		connfac.setPassword("cph");

		try {
			connection = connfac.newConnection();
			channel = connection.createChannel();
			channel.queueDeclare(creditIn, false, false, false, null);
			channel.queueDeclare(aggOut, false, false, false, null);

			// Send message request for interestRate.
			CanonicalDTO dto = new CanonicalDTO();
			dto.setSsn(ssn);
			dto.setLoanAmount(loanAmount);
			dto.setLoanDuration(loanDuration);

			System.out.println("Publish");
			channel.basicPublish("", creditIn, null, convertDtoToString(dto).getBytes());
			System.out.println("Publish finished");

			// Receive message
			consumer = new QueueingConsumer(channel);
			channel.basicConsume(aggOut, true, consumer);
			QueueingConsumer.Delivery delivery = consumer.nextDelivery();

			dto = convertStringToDto(new String(delivery.getBody()));

			b.append("Kaere: ").append(dto.getSsn()).append("\n");
			b.append("Med disse data:").append("\n");
			b.append("\tLaeneStoerrelse: " + loanAmount).append("\n");
			b.append("\tLaeneLaengde: ").append(loanDuration).append("\n");
			b.append("Du kan faa laan hos foelgende banker:\n");
			b.append("\n");
			for (BankDTO bank : dto.getBanks()) {
				b.append("Bank: " + bank.getName()).append("\n");
				b.append("\tRente: " + bank.getInterestRate()).append("\n");
				b.append("\n");
			}
		} catch (Exception e) {
			b.append(e.getMessage());
			e.printStackTrace();
		}
		// TODO: Make string.
		return b.toString();
	}

	protected final String convertDtoToString(CanonicalDTO canonicalDTO) {
		String retVal = null;
		Serializer serializer = new Persister();
		OutputStream stream = new ByteArrayOutputStream();
		try {
			serializer.write(canonicalDTO, stream);
			retVal = stream.toString();
		} catch (Exception e) {
			log.severe(e.getClass() + ": " + e.getMessage());
			if (e.getCause() != null) {
				log.severe("\t" + e.getCause().getClass() + ": " + e.getCause().getMessage());
			}
		}
		return retVal;
	}

	protected final CanonicalDTO convertStringToDto(String xmlString) {
		CanonicalDTO dto = null;
		Serializer serializer = new Persister();
		try {
			dto = serializer.read(CanonicalDTO.class, xmlString);
		} catch (Exception e) {
			e.printStackTrace();
			log.severe(e.getClass() + ": " + e.getMessage());
			if (e.getCause() != null) {
				log.severe("\t" + e.getCause().getClass() + ": " + e.getCause().getMessage());
			}
		}
		return dto;
	}
}
