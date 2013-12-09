package com.loanbroker.loan_broker;

import com.loanbroker.loan_broker.models.CanonicalDTO;
import com.loanbroker.loan_broker.models.BankDTO;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.ConsumerCancelledException;
import com.rabbitmq.client.QueueingConsumer;
import com.rabbitmq.client.ShutdownSignalException;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.core.Persister;

/**
 *
 * @author Marc
 */
public class RecipientHandler extends Thread {

    private boolean pleaseStop = false;

    private String QUEUE_NAME;
    private String QUEUE_NAME_BANK_1;
    private String QUEUE_NAME_BANK_2;
    private String QUEUE_NAME_BANK_3;
    private String QUEUE_NAME_BANK_4;
    /*	private final String QUEUE_NAME ="02_recipient_list_channel";
     private final String QUEUE_NAME_BANK_1 = "02_bank_xml_channel";
     private final String QUEUE_NAME_BANK_2 = "02_bank_json_channel";
     private final String QUEUE_NAME_BANK_3 = "02_bank_rabbitmq_channel";
     private final String QUEUE_NAME_BANK_4 = "02_bank_webservice_channel";*/

    private Map<String, String> bankQueues;

//    public RecipientHandler() {
//        QUEUE_NAME = "reciepidequeu";
//        Map<String, String> ques = new HashMap<>();
//        ques.put("xml", "xml_channel");
//        ques.put("json", "json_channel");
//        ques.put("rabbitmq", "rabbitmq_channel");
//        ques.put("webservice", "websercice_channel");
//    }

    public RecipientHandler(Map<String, String> bankQueues) {
        this.bankQueues = bankQueues;
    }

    @Override
    public void run() {
        try {
            Connection conn = getConnection();
            Channel chan = conn.createChannel();
            chan.queueDeclare(QUEUE_NAME, false, false, false, null);
            System.out.println(" [*] Waiting for messages. To exit press CTRL+C");
            QueueingConsumer consumer = new QueueingConsumer(chan);
            chan.basicConsume(QUEUE_NAME, true, consumer);
            //start polling messages
            while (pleaseStop == false) {
                String consumerTag = consumer.getConsumerTag();
                System.out.println(" [-] ConsumerTag: '" + consumerTag + "'");
                QueueingConsumer.Delivery delivery = consumer.nextDelivery();
                String xmlStr = delivery.getBody().toString();
                processBankOutput(xmlStr);
				//CanonicalDTO dto = new String(delivery.getBody());
                //System.out.println(" [x] Received '" + message + "'");
            }
        } catch (IOException ex) {
            Logger.getLogger(RecipientHandler.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InterruptedException ex) {
            Logger.getLogger(RecipientHandler.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ShutdownSignalException ex) {
            Logger.getLogger(RecipientHandler.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ConsumerCancelledException ex) {
            Logger.getLogger(RecipientHandler.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void processBankOutput(String xmlStr) {
        Serializer serializer = new Persister();
        try {
            CanonicalDTO dto = serializer.read(CanonicalDTO.class, xmlStr);

//                        OutputStream outputStream = new ByteArrayOutputStream();
//                        serializer.write(dto, outputStream);
            Connection conn = getConnection();
            for (BankDTO bank : dto.getBanks()) {
                Channel channel = conn.createChannel();

                String queueName = getBankChannelName(bank.getName());
                if (queueName != null) {
                    channel.queueDeclare(queueName, false, false, false, null);
                    channel.basicPublish("", queueName, null, xmlStr.getBytes());
                    System.out.println(" [x] Sent '" + xmlStr + "'");
                } else {
                    // TODO: publish to error queue.
                }
            }
        } catch (Exception ex) {
            Logger.getLogger(RecipientHandler.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private String getBankChannelName(String bankName) {
        if (!bankQueues.containsKey(bankName)) {
            return null;
        }
        return bankQueues.get(bankName);
    }

    public void pleaseStop() {
        pleaseStop = true;
    }

    private Connection getConnection() throws IOException {
        ConnectionFactory connfac = new ConnectionFactory();
        connfac.setHost("datdb.cphbusiness.dk");
        connfac.setPort(5672);
        connfac.setUsername("student");
        connfac.setPassword("cph");
        Connection connection = connfac.newConnection();
        return connection;
    }

    public void sendRecipients() throws IOException {
        Connection connection = getConnection();
        Channel channel = connection.createChannel();

        channel.queueDeclare(QUEUE_NAME, true, false, false, null);
        String message = "Hello World!";
        for (int i = 0; i < Integer.MAX_VALUE; i++) {
            channel.basicPublish("", QUEUE_NAME, null, message.getBytes());
        }
        System.out.println(" [x] Sent '" + message + "'");

        channel.close();
        connection.close();

    }

}
