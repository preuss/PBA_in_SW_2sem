/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.loanbroker.loan_broker.rabbit_bank;

import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.QueueingConsumer;
import java.io.IOException;

/**
 *
 * @author Marc
 */
public class RabbitBank {

    private static final String QUEUE_NAME = "rabbit_bank2556";
    private static final String QUEUE_NAME_1 = "rabbit_bank5625";

    public static void main(String[] argv) throws IOException, InterruptedException {

        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("datdb.cphbusiness.dk");
        factory.setUsername("student");
        factory.setPassword("cph");
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();

        channel.queueDeclare(QUEUE_NAME_1, false, false, false, null);
        String messageOut = "rabbit bank";
    
        QueueingConsumer consumer = new QueueingConsumer(channel);
        channel.basicConsume(QUEUE_NAME, true, consumer);

        while (true) {
            QueueingConsumer.Delivery delivery = consumer.nextDelivery();
            String messageIn = new String(delivery.getBody());
            System.out.println(" [x] Received by rabbit_bank: '" + messageIn + "'");
            channel.basicPublish("", QUEUE_NAME_1, null, messageOut.getBytes());
            System.out.println(" [x] Sent by rabbit_bank: '" + messageOut + "'");
        }
    }

}
