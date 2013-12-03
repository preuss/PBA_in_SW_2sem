/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.loanbroker.loan_broker;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import java.io.IOException;

/**
 *
 * @author Andreas
 */
public class BankHandler {

    private final static String QUEUE_NAME = "hello";

    public void generateBankList() throws IOException {
        ConnectionFactory connfac = new ConnectionFactory();
        connfac.setHost("datdb.cphbusiness.dk");
        connfac.setPort(5672);
        connfac.setUsername("student");
        connfac.setPassword("cph");
        Connection connection = connfac.newConnection();
        Channel channel = connection.createChannel();

        channel.queueDeclare(QUEUE_NAME, false, false, false, null);
        String message = "Hello World!";
        for (int i = 0; i < Integer.MAX_VALUE; i++) {
            channel.basicPublish("", QUEUE_NAME, null, message.getBytes());
        }
        System.out.println(" [x] Sent '" + message + "'");

        channel.close();
        connection.close();
    }
}
