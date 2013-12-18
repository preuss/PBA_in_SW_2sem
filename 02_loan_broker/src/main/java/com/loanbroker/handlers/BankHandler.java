package com.loanbroker.handlers;

/**
 *
 * @author Andreas
 */
import com.loanbroker.logging.Logger;
import com.loanbroker.models.BankDTO;
import com.loanbroker.models.CanonicalDTO;
import com.rabbitmq.client.AMQP;
import java.io.IOException;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.ConsumerCancelledException;
import com.rabbitmq.client.QueueingConsumer;
import com.rabbitmq.client.QueueingConsumer.Delivery;
import com.rabbitmq.client.ShutdownSignalException;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.core.Persister;

/**
 *
 * @author Andreas
 */
public class BankHandler extends HandlerThread {

	private final Logger log = Logger.getLogger(BankHandler.class);

	private String receiveQueue;
	private String sendFanoutExchange;

	public BankHandler(String receiveQueue, String sendFanoutExchange) {
		this.receiveQueue = receiveQueue;
		this.sendFanoutExchange = sendFanoutExchange;
	}

	private List<BankDTO> generateBankList(CanonicalDTO dto) {
		ArrayList<BankDTO> banks = new ArrayList<BankDTO>();
		BankDTO bank;
		if (dto.getCreditScore() > 0) {
			bank = new BankDTO();
			//bank.setName("Bank of Tolerance");
			bank.setName("webservice");
			banks.add(bank);
		}
		if (dto.getCreditScore() > 200) {
			bank = new BankDTO();
			//bank.setName("Bank of the Average");
			bank.setName("json");
			banks.add(bank);
		}
		if (dto.getCreditScore() > 400) {
			bank = new BankDTO();
			//bank.setName("Bank of the Rich");
			bank.setName("xml");
			banks.add(bank);
		}
		if (dto.getCreditScore() > 600) {
			bank = new BankDTO();
			//bank.setName("Bank of the Elite");
			bank.setName("rabbitmq");
			banks.add(bank);
		}
		return banks;
	}

	@Override
	protected void doRun() {
		Connection connection = null;
		Channel channel = null;
		QueueingConsumer consumer = null;
		while (isPleaseStop() == false) {
			try {
				if (connection == null) {
					connection = getConnection();
					channel = createChannel(connection, receiveQueue);
					channel.exchangeDeclare(sendFanoutExchange, "fanout");
					log.debug("sendFanoutExchange: " + sendFanoutExchange);
					consumer = new QueueingConsumer(channel);
					channel.basicConsume(receiveQueue, true, consumer);
				}
				// Receive
				QueueingConsumer.Delivery delivery = consumer.nextDelivery();
				String message = null;
				CanonicalDTO dto = null;
				if (delivery != null && delivery.getBody() != null) {
					byte[] messageRaw = delivery.getBody();
					message = new String(messageRaw);
					dto = convertStringToDto(message);
					log.debug("receivedCreditScore DTO: " + dto);
					System.out.println("the score is " + dto.getCreditScore());
				}

				// Get bankList and send to exchange
				if (dto != null) {
					// Generate banklist
					List<BankDTO> bankList = generateBankList(dto);
					dto.setBanks(bankList);

					message = convertDtoToString(dto);

					// Send to exchange
					AMQP.BasicProperties props = new AMQP.BasicProperties.Builder().replyTo(channel.queueDeclare().getQueue()).build();
					channel.basicPublish(sendFanoutExchange, "", props, message.getBytes());
					log.debug("BankHandler sent message to Exchange: " + message.replace("\t", "").replace("\r", "").replace("\n", "").replace(" ", ""));
				}

			} catch (IOException e) {
				log.warning(e.getClass() + ", Message: " + e.getMessage());
				e.printStackTrace();
				if (e.getCause() != null) {
					log.warning("\t" + e.getCause().getClass() + ", " + e.getCause().getMessage());
					if (e.getCause().getCause() != null) {
						log.warning("\t\t" + e.getCause().getCause().getClass() + ", " + e.getCause().getCause().getMessage());
					}
				}
				throw new RuntimeException(e);
			} catch (ShutdownSignalException e) {
				log.warning(e.getClass() + ", Message: " + e.getMessage());
				e.printStackTrace();
				if (e.getCause() != null) {
					log.warning("\t" + e.getCause().getClass() + ", " + e.getCause().getMessage());
					if (e.getCause().getCause() != null) {
						log.warning("\t\t" + e.getCause().getCause().getClass() + ", " + e.getCause().getCause().getMessage());
					}
				}
				throw new RuntimeException(e);
			} catch (ConsumerCancelledException e) {
				log.warning(e.getClass() + ", Message: " + e.getMessage());
				e.printStackTrace();
				if (e.getCause() != null) {
					log.warning("\t" + e.getCause().getClass() + ", " + e.getCause().getMessage());
					if (e.getCause().getCause() != null) {
						log.warning("\t\t" + e.getCause().getCause().getClass() + ", " + e.getCause().getCause().getMessage());
					}
				}
				throw new RuntimeException(e);
			} catch (InterruptedException e) {
				log.warning(e.getClass() + ", Message: " + e.getMessage());
				e.printStackTrace();
				if (e.getCause() != null) {
					log.warning("\t" + e.getCause().getClass() + ", " + e.getCause().getMessage());
					if (e.getCause().getCause() != null) {
						log.warning("\t\t" + e.getCause().getCause().getClass() + ", " + e.getCause().getCause().getMessage());
					}
				}
				throw new RuntimeException(e);
			} catch (Exception e) {
				log.warning(e.getClass() + ", Message: " + e.getMessage());
				e.printStackTrace();
				if (e.getCause() != null) {
					log.warning("\t" + e.getCause().getClass() + ", " + e.getCause().getMessage());
					if (e.getCause().getCause() != null) {
						log.warning("\t\t" + e.getCause().getCause().getClass() + ", " + e.getCause().getCause().getMessage());
					}
				}
			}
		}
	}
}
