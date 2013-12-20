package com.loanbroker.webservice.loanwebservice;

import com.loanbroker.logging.Logger;
import com.loanbroker.models.BankDTO;
import com.loanbroker.models.CanonicalDTO;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.QueueingConsumer;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import javax.jws.WebService;
import javax.jws.WebMethod;
import javax.jws.WebParam;
import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.core.Persister;

/**
 *
 * @author Preuss
 */
@WebService(serviceName = "LoanWebservice")
public class LoanWebservice {

	static Connection connection = null;
	static Channel channel = null;
	static QueueingConsumer consumer = null;

	private final Logger log = Logger.getLogger(LoanWebservice.class);

	/**
	 * This is a sample web service operation
	 */
	@WebMethod(operationName = "hello")
	public String hello(@WebParam(name = "name") String txt) {
		return "Hello " + txt + " !";
	}

	/**
	 * Web service operation
	 */
	@WebMethod(operationName = "loanBroker")
	public String loanBroker(@WebParam(name = "ssn") String ssn, @WebParam(name = "loanAmount") double loanAmount, @WebParam(name = "loanDuration") int loanDuration) throws IOException, InterruptedException {
		StringBuilder b = new StringBuilder();

		String creditIn = "Group2.CreditHandler.Receive";
		String aggOut = "Group2.Aggregator.Send";

		ConnectionFactory connfac = new ConnectionFactory();
		connfac.setHost("datdb.cphbusiness.dk");
		connfac.setPort(5672);
		connfac.setUsername("student");
		connfac.setPassword("cph");

		System.out.println("Try to go");
		try {
			if (connection == null) {
				System.out.println("Connection new");
				connection = connfac.newConnection();
				channel = connection.createChannel();
				channel.queueDeclare(creditIn, false, false, false, null);
				channel.queueDeclare(aggOut, false, false, false, null);
				consumer = new QueueingConsumer(channel);
				channel.basicConsume(aggOut, true, consumer);
			}

			// Send message request for interestRate.
			CanonicalDTO dto = new CanonicalDTO();
			dto.setSsn(ssn);
			dto.setLoanAmount(loanAmount);
			dto.setLoanDuration(loanDuration);

			System.out.println("Try to ask ssn: " + ssn);
			channel.basicPublish("", creditIn, null, convertDtoToString(dto).getBytes());

			// Receive message
			System.out.println("Try to get.");
			QueueingConsumer.Delivery delivery = consumer.nextDelivery(25000);
			System.out.println("Gotten the delivery.");
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
			connection = null;
			b.append(e.getMessage());
			e.printStackTrace();
			throw e;
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
