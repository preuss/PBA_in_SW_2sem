package com.loanbroker.handlers;

/**
 *
 * @author Andreas
 */
import java.io.IOException;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.ConsumerCancelledException;
import com.rabbitmq.client.QueueingConsumer;
import com.rabbitmq.client.ShutdownSignalException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Andreas
 */
public class BankHandler {

    private final static String BANKLIST_QUEUE = "02_rating_channel";
    private final static String RATING_QUEUE = "02_rating_channel";
    
    private String receiveQueue;
    private String sendQueue;

    public BankHandler(){}
    
    public BankHandler(String receiveQueue, String sendQueue){
    this.receiveQueue = receiveQueue;
    this.sendQueue = sendQueue;
    }
    
    private void generateBankList(Integer rating){
        ArrayList<String> banks = new ArrayList<String>();
        if(rating>0)
        {
            banks.add("Bank of Tolerance");
        }
        if(rating>200)
        {
            banks.add("Bank of the Average");
        }
        if(rating>400)
        {
            banks.add("Bank of the Rich");
        }
        if(rating>600)
        {
            banks.add("Bank of the Elite");
        }
            
        for(String s : banks){
            System.out.println(s);
        }
    }

    private Connection getConnection(){
        ConnectionFactory connfac = new ConnectionFactory();
        connfac.setHost("datdb.cphbusiness.dk");
        connfac.setUsername("student");
        connfac.setPassword("cph");
        Connection conn = null;
        try {
            //make the connection
             conn = connfac.newConnection();
        } catch (IOException ex) {
            Logger.getLogger(BankHandler.class.getName()).log(Level.SEVERE, null, ex);
        }
        return conn;
    }
    
    public void receiveCreditScore() throws IOException, ShutdownSignalException, ConsumerCancelledException, InterruptedException {
        Channel chan = getConnection().createChannel();
        //Declare a queue
        chan.queueDeclare(RATING_QUEUE, false, false, false, null);
        System.out.println(" [*] Waiting for messages. To exit press CTRL+C");
        QueueingConsumer consumer = new QueueingConsumer(chan);
        chan.basicConsume(RATING_QUEUE, true, consumer);
        //start polling messages
        while (true) {
            QueueingConsumer.Delivery delivery = consumer.nextDelivery();
            String corrID = delivery.getProperties().getCorrelationId();
            String message = new String(delivery.getBody());
            System.out.println(" [x] Received '" + message + "' correlationId is: "+corrID);
            generateBankList(Integer.parseInt(message));
        }
    }
    
    public void sendBanks() throws IOException{
        Channel channel = getConnection().createChannel();
        channel.queueDeclare(BANKLIST_QUEUE, false, false, false, null);
        
    }
}
