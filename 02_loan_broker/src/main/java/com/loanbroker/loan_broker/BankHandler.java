package com.loanbroker.loan_broker;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import java.io.IOException;

/**
 *
 * @author Andreas
 */
import java.io.IOException;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.ConsumerCancelledException;
import com.rabbitmq.client.QueueingConsumer;
import com.rabbitmq.client.ShutdownSignalException;
import java.util.ArrayList;

/**
 *
 * @author Andreas
 */
public class BankHandler {

    private final static String QUEUE_NAME = "02_banklist_channel";
    
    private void generateBankList(Integer rating){
        ArrayList<String> banks = new ArrayList<String>();
        if(rating>0)
        {
            banks.add("Bank of Tolerance");
        }
        if(rating>200)
        {
            banks.add("Bank of the Average");
        }
        if(rating>400)
        {
            banks.add("Bank of the Rich");
        }
        if(rating>600)
        {
            banks.add("Bank of the Elite");
        }
            
        for(String s : banks){
            System.out.println(s);
        }
    }

    public void receiveCreditScore() throws IOException, ShutdownSignalException, ConsumerCancelledException, InterruptedException {
        //setting the connection to the RabbitMQ server
        ConnectionFactory connfac = new ConnectionFactory();
        connfac.setHost("datdb.cphbusiness.dk");
        connfac.setUsername("student");
        connfac.setPassword("cph");
        //make the connection
        Connection conn = connfac.newConnection();
        //make the channel for messaging
        Channel chan = conn.createChannel();
        //Declare a queue
        chan.queueDeclare(QUEUE_NAME, false, false, false, null);
        System.out.println(" [*] Waiting for messages. To exit press CTRL+C");
        QueueingConsumer consumer = new QueueingConsumer(chan);
        chan.basicConsume(QUEUE_NAME, true, consumer);
        //start polling messages
        while (true) {
            QueueingConsumer.Delivery delivery = consumer.nextDelivery();
            String message = new String(delivery.getBody());
            System.out.println(" [x] Received '" + message + "'");
            generateBankList(Integer.parseInt(message));
        }
    }
}
