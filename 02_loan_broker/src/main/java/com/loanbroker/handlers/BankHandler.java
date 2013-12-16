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
import com.rabbitmq.client.ShutdownSignalException;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.logging.Level;
import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.core.Persister;

/**
 *
 * @author Andreas
 */
public class BankHandler extends HandlerThread {

	private final Logger log = Logger.getLogger(BankHandler.class);

//    private final static String BANKLIST_QUEUE = "02_rating_channel";
//    private final static String RATING_QUEUE = "02_rating_channel";
	private String receiveQueue;
	private String sendQueue;

	public BankHandler() {
	}

	public BankHandler(String receiveQueue, String sendQueue) {
		this.receiveQueue = receiveQueue;
		this.sendQueue = sendQueue;
	}

	private void generateBankList(Integer rating) {
		ArrayList<String> banks = new ArrayList<String>();
		if (rating > 0) {
			banks.add("Bank of Tolerance");
		}
		if (rating > 200) {
			banks.add("Bank of the Average");
		}
		if (rating > 400) {
			banks.add("Bank of the Rich");
		}
		if (rating > 600) {
			banks.add("Bank of the Elite");
		}

    private CanonicalDTO generateBankList(CanonicalDTO dto) {
        ArrayList<BankDTO> banks = new ArrayList<BankDTO>();
        BankDTO bank;
        if (dto.getCreditScore() > 0) {
            bank = new BankDTO();
            bank.setName("Bank of Tolerance");
            banks.add(bank);
        }
        if (dto.getCreditScore() > 200) {
            bank = new BankDTO();
            bank.setName("Bank of the Average");
            banks.add(bank);
        }
        if (dto.getCreditScore() > 400) {
            bank = new BankDTO();
            bank.setName("Bank of the Rich");
            banks.add(bank);
        }
        if (dto.getCreditScore() > 600) {
            bank = new BankDTO();
            bank.setName("Bank of the Elite");
            banks.add(bank);
        }
        dto.setBanks(banks);
        for (int i = 0; i < dto.getBanks().size(); i++) {
        }
        return dto;
    }

    public void receiveCreditScore() throws IOException, ShutdownSignalException, ConsumerCancelledException, InterruptedException, Exception {
        Channel chan = getConnection().createChannel();
        //Declare a queue
        chan.queueDeclare(receiveQueue, false, false, false, null);
        System.out.println(" [*] Waiting for messages. To exit press CTRL+C");
        QueueingConsumer consumer = new QueueingConsumer(chan);
        chan.basicConsume(receiveQueue, true, consumer);
        //start polling messages
       while (true) {
            QueueingConsumer.Delivery delivery = consumer.nextDelivery();
            String message = new String(delivery.getBody());
            System.out.println(" [x] Received '" + message);
            CanonicalDTO dto = convertStringToDto(message);
            System.out.println("the score is " + dto.getCreditScore());
            sendBanks(generateBankList(dto));
            //Thread.sleep(10000);
        }
    }
    
    

	@Override
	protected void doRun() {
		while (isPleaseStop() == false) {
			try {
				receiveCreditScore();
//                sendBanks();
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
