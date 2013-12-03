package com.loanbroker.loan_broker;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import java.io.IOException;
import wservices.CreditScoreService;
import wservices.CreditScoreService_Service;
import com.rabbitmq.client.AMQP.BasicProperties;

/**
 *
 * @author Andreas
 */
public class CreditHandler {

    private final static String QUEUE_NAME = "02_banklist_channel";

    public void getCreditScore() {

        try { // Call Web Service Operation
            CreditScoreService_Service service = new CreditScoreService_Service();
            CreditScoreService port = service.getCreditScoreServicePort();
            // TODO initialize WS operation arguments here
            String ssn = "123456-1234";
            // TODO process result here
            int result = port.creditScore(ssn);
            System.out.println("Result from Credit Bureau = " + result);
            if (result != -1) {
                getBanks(result);
            }
        } catch (IOException ex) {
            System.out.println(ex.getMessage());
        }

    }

    private String generateCorrelationID() {
        String corrID = java.util.UUID.randomUUID().toString();

        return null;
    }

    public void getBanks(int rating) throws IOException {
        ConnectionFactory connfac = new ConnectionFactory();
        connfac.setHost("datdb.cphbusiness.dk");
        connfac.setPort(5672); //dette er rabbitMQ protokol-porten. 
        connfac.setUsername("student");
        connfac.setPassword("cph");
        Connection connection = connfac.newConnection();
        Channel channel = connection.createChannel();

        channel.queueDeclare(QUEUE_NAME, false, false, false, null);
        String message = "" + rating;
        channel.basicPublish("", QUEUE_NAME, null, message.getBytes());
        System.out.println(" [x] Sent '" + message + "'");
        String corrId = generateCorrelationID();
        BasicProperties props = new BasicProperties.Builder().correlationId(corrId).replyTo(channel.queueDeclare().getQueue()).build();
        channel.basicPublish("", QUEUE_NAME, props, message.getBytes());
        channel.close();
        connection.close();

    }

}
