package com.loanbroker.loan_broker;

import java.io.IOException;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.ConsumerCancelledException;
import com.rabbitmq.client.QueueingConsumer;
import com.rabbitmq.client.ShutdownSignalException;

public class Reciever {

    private static String QUEUE_NAME = "Group 2";

    public Reciever(String QUEUE) {
        QUEUE_NAME = QUEUE;
    }

    public static void main(String[] args) throws IOException, ShutdownSignalException, ConsumerCancelledException, InterruptedException {
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
            String consumerTag = consumer.getConsumerTag();
            System.out.println(" [-] ConsumerTag: '" + consumerTag + "'");
            QueueingConsumer.Delivery delivery = consumer.nextDelivery();
            String message = new String(delivery.getBody());
            System.out.println(" [x] Received '" + message + "'");
        }
    }

    public void setReciever(String QUEUE) {
        QUEUE_NAME = QUEUE;
    }

    public String getReciever() {
        return QUEUE_NAME;
    }
}
