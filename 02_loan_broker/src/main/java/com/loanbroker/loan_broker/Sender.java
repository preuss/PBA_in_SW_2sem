package com.loanbroker.loan_broker;

import java.io.IOException;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

public class Sender {

    private static String QUEUE_NAME = "Group 2";
    private static String message = "Default";

    public Sender(String Queue, String message) {
        QUEUE_NAME = Queue;
        message = message;
    }

    public static void main(String[] args) throws IOException {

        ConnectionFactory connfac = new ConnectionFactory();
        connfac.setHost("datdb.cphbusiness.dk");
        connfac.setPort(5672);
        connfac.setUsername("student");
        connfac.setPassword("cph");
        Connection connection = connfac.newConnection();
        Channel channel = connection.createChannel();
        channel.queueDeclare(QUEUE_NAME, false, false, false, null);
        channel.basicPublish("", QUEUE_NAME, null, message.getBytes());
        System.out.println(" [x] Sent '" + message + "'");

        channel.close();
        connection.close();
    }

    public void setSender(String QUEUE) {
        QUEUE_NAME = QUEUE;
    }

    public String getSender() {
        return QUEUE_NAME;
    }

    public static String getMessage() {
        return message;
    }

    public static void setMessage(String message) {
        Sender.message = message;
    }

}
