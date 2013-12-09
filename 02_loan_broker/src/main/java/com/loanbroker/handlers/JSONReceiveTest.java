/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.loanbroker.handlers;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.QueueingConsumer;
import java.io.IOException;

/**
 *
 * @author Marc
 */
public class JSONReceiveTest {
    private static final String EXCHANGE_NAME = "cphbusiness.bankJSON";
    
    public static void main(String[] argv) throws IOException, InterruptedException {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("datdb.cphbusiness.dk");
        factory.setUsername("student");
        factory.setPassword("cph");
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();
        
        channel.exchangeDeclare(EXCHANGE_NAME, "fanout");
        String queueName = "";
        channel.queueBind(queueName, EXCHANGE_NAME, "");
        
        
        QueueingConsumer consumer = new QueueingConsumer(channel);
        channel.basicConsume(queueName, true, consumer);

        while (true) {
            QueueingConsumer.Delivery delivery = consumer.nextDelivery();
            String messageIn = new String(delivery.getBody());
            System.out.println(" [x] Received by tester: '" + messageIn + "'");
        }
       
    }
}
