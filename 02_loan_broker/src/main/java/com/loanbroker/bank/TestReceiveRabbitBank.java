/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.loanbroker.bank;

import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.QueueingConsumer;
import java.io.IOException;
/**
 *
 * @author Marc
 */
public class TestReceiveRabbitBank {
   private static final String QUEUE_NAME = "rabbit_bank5625";
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws IOException, InterruptedException {
        // TODO code application logic here
        
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("datdb.cphbusiness.dk");
        factory.setUsername("student");
        factory.setPassword("cph");
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();
        
        QueueingConsumer consumer = new QueueingConsumer(channel);
        channel.basicConsume(QUEUE_NAME, true, consumer);

        while (true) {
            QueueingConsumer.Delivery delivery = consumer.nextDelivery();
            String messageIn = new String(delivery.getBody());
            System.out.println(" [x] Received by tester: '" + messageIn + "'");
        }
        
    }
    
}
