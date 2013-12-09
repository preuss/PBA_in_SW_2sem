/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.loanbroker.bank;

import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.Channel;
import java.io.IOException;

/**
 *
 * @author Marc
 */
public class TestSendRabbitBank {

    private static final String QUEUE_NAME = "rabbit_bank2556";

    public static void main(String[] argv) throws IOException {
       // new RabbitBank(); 
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("datdb.cphbusiness.dk");
        factory.setUsername("student");
        factory.setPassword("cph");
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();
        
        channel.queueDeclare(QUEUE_NAME, false, false, false, null);
        String messageOut = "Test message";
        channel.basicPublish("", QUEUE_NAME, null, messageOut.getBytes());
        System.out.println(" [x] Sent by tester: '" + messageOut + "'");
    }

}
