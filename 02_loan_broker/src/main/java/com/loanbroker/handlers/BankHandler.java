package com.loanbroker.handlers;

/**
 *
 * @author Andreas
 */
import com.loanbroker.logging.Logger;
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

    private final static String BANKLIST_QUEUE = "02_rating_channel";
    private final static String RATING_QUEUE = "02_rating_channel";

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

        for (String s : banks) {
            System.out.println(s);
        }
    }

    public void receiveCreditScore() throws IOException, ShutdownSignalException, ConsumerCancelledException, InterruptedException, Exception {
        Channel chan = getConnection().createChannel();
        //Declare a queue
        chan.queueDeclare(RATING_QUEUE, false, false, false, null);
        System.out.println(" [*] Waiting for messages. To exit press CTRL+C");
        QueueingConsumer consumer = new QueueingConsumer(chan);
        chan.basicConsume(RATING_QUEUE, true, consumer);
        //start polling messages
        while (true) {
            QueueingConsumer.Delivery delivery = consumer.nextDelivery();
            String message = new String(delivery.getBody());
            System.out.println("BankHandler Received " + message);
            generateBankList(Integer.parseInt(message));
            CanonicalDTO dto = convertStringToDto(message);
            sendBanks(dto);
        }
    }

    private void sendBanks(CanonicalDTO dto) throws IOException {
        Channel channel = getConnection().createChannel();
        channel.queueDeclare(BANKLIST_QUEUE, false, false, false, null);
        String message = convertDtoToString(dto);
        AMQP.BasicProperties props = new AMQP.BasicProperties.Builder().replyTo(channel.queueDeclare().getQueue()).build();
        channel.basicPublish("", BANKLIST_QUEUE, props, message.getBytes());
        System.out.println(" [x] Sent '" + message + "'");
    }

    private String convertDtoToString(CanonicalDTO dto) {
        String value = null;
        try {
            Serializer serializer = new Persister();
            OutputStream outputStream = new ByteArrayOutputStream();
            serializer.write(dto, outputStream);
            value = outputStream.toString();
        } catch (Exception ex) {
        }
        return value;
    }

    private CanonicalDTO convertStringToDto(String xmlString) {
        Serializer serializer = new Persister();
        CanonicalDTO dto = null;
        try {
            dto = serializer.read(CanonicalDTO.class, xmlString);
        } catch (Exception ex) {
            java.util.logging.Logger.getLogger(BankHandler.class.getName()).log(Level.SEVERE, null, ex);
        }
        return dto;
    }

    @Override
    protected void doRun() {
        while (isPleaseStop() == false) {
            try {
                receiveCreditScore();
//                sendBanks();
            } catch (IOException e) {
                log.trace(e.getMessage());
                throw new RuntimeException(e);
            } catch (ShutdownSignalException e) {
                log.trace(e.getMessage());
                throw new RuntimeException(e);
            } catch (ConsumerCancelledException e) {
                log.trace(e.getMessage());
                throw new RuntimeException(e);
            } catch (InterruptedException e) {
                log.trace(e.getMessage());
                throw new RuntimeException(e);
            } catch (Exception ex) {
                java.util.logging.Logger.getLogger(BankHandler.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
}
