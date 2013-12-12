package com.loanbroaker.translators;

import com.loanbroker.handlers.HandlerThread;
import com.loanbroker.models.CanonicalDTO;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.QueueingConsumer;
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
public class JsonTranslator extends HandlerThread {

    private final String EXCHANGE_NAME = "cphbusiness.bankXML";
    private String queueNameReceive;    //"02_bankXML"
    private String replyToQueue;        //"02_xml_reply_queue"

    public JsonTranslator(String queueNameReceive, String replyToQueue) {
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
            CanonicalDTO dto = convertStringToDto(message);
            System.out.println("the score is " + dto.getCreditScore());
            sendRequestToXmlBank(translateMessage(dto));
        }
    }

    private String translateMessage(CanonicalDTO dto) {

        String jsonValue = "{\"ssn\":" + dto.getSsn().replace("-", "").trim() 
                + ",\"creditScore\":" + dto.getCreditScore() 
                + ",\"loanAmount\":" + dto.getLoanAmount() 
                + ",\"loanDuration\":" + dto.getLoanDuration() + "}";

        return jsonValue;
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
