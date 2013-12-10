/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.loanbroaker.translators;

import com.loanbroker.handlers.HandlerThread;
import com.loanbroker.models.CanonicalDTO;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.ConsumerCancelledException;
import com.rabbitmq.client.QueueingConsumer;
import com.rabbitmq.client.ShutdownSignalException;
import java.io.IOException;
import java.util.Calendar;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.core.Persister;

/**
 *
 * @author Andreas
 */
public class XmlTranslator extends HandlerThread {
    
    private  final String EXCHANGE_NAME = "cphbusiness.bankXML";
    private String queueNameReceive;    //"02_bankXML"
    private String replyToQueue;        //"02_xml_reply_queue"
    
    public XmlTranslator(String queueNameReceive, String replyToQueue){
        this.queueNameReceive = queueNameReceive;
        this.replyToQueue = replyToQueue;
    }

    public void receiveBankName() throws IOException, InterruptedException, Exception {
        
        Channel channel = getConnection().createChannel();
        //Declare a queue
        channel.queueDeclare(queueNameReceive, false, false, false, null);
        System.out.println("XmlTranslator Waiting for messages");
        QueueingConsumer consumer = new QueueingConsumer(channel);
        channel.basicConsume(queueNameReceive, true, consumer);
        //start polling messages
        while (true) {
            QueueingConsumer.Delivery delivery = consumer.nextDelivery();
            String message = new String(delivery.getBody());
            System.out.println("Received at XmlTranslator" + message);
            CanonicalDTO dto = convertStringToDTO(message);
            System.out.println("the score is " + dto.getCreditScore());
            sendRequestToXmlBank(translateMessage(dto));
        }
    }

    private CanonicalDTO convertStringToDTO(String message) {
        Serializer serializer = new Persister();
        CanonicalDTO dto = null;
        try {
            dto = serializer.read(CanonicalDTO.class, message);
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
        }
        return dto;
    }

    private String translateMessage(CanonicalDTO dto) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(1970, 0, 0, 0, 0, 0);
        calendar.add(Calendar.MONTH, dto.getLoanDuration());
        String loanDuration = calendar.get(Calendar.YEAR) + "-" + calendar.get(Calendar.MONTH) + "-01 01:00:00.0 CET";
        
        String xmlValue = "<LoanRequest>"
                + "   <ssn>" + dto.getSsn().replace("-", "").trim() + "</ssn>"
                + "   <creditScore>" + dto.getCreditScore() + "</creditScore>"
                + "   <loanAmount>" + dto.getLoanAmount() + "</loanAmount>"
                + "   <loanDuration>" + loanDuration + "</loanDuration>"
                + "</LoanRequest>";

        return xmlValue;
    }

    private void sendRequestToXmlBank(String xmlString) throws IOException {
        Channel channel = getConnection().createChannel();
        channel.queueDeclare(replyToQueue, false, false, false, null);
        AMQP.BasicProperties.Builder builder = new AMQP.BasicProperties().builder();
        builder.replyTo(replyToQueue);
        AMQP.BasicProperties props = builder.build();
        channel.basicPublish(EXCHANGE_NAME, "", props, xmlString.getBytes());
        System.out.println("Message Sent from translator: " + xmlString);
//      channel.close();
//      connection.close();

    }

    @Override
    protected void doRun() {
        while (isPleaseStop() == false) {
            try {
                receiveBankName();
            } catch (InterruptedException ex) {
                Logger.getLogger(XmlTranslator.class.getName()).log(Level.SEVERE, null, ex);
            } catch (Exception ex) {
                Logger.getLogger(XmlTranslator.class.getName()).log(Level.SEVERE, null, ex);
            }
		}
    }

    
}
