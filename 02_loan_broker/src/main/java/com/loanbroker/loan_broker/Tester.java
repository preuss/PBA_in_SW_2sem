package com.loanbroker.loan_broker;

import com.loanbroker.loan_broker.models.BankDTO;
import com.loanbroker.loan_broker.models.CanonicalDTO;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.core.Persister;

/**
 *
 * @author Andreas
 */
public class Tester {

	public static void main(String[] args) {
//        CreditHandler cr = new CreditHandler();
//        cr.getCreditScore();

               /* RecipientHandler rh = new RecipientHandler("Group 2 - Banks", "Group 2 - Recipient List");
                Serializer ser = new Persister();
		CanonicalDTO can = new CanonicalDTO();
		List<BankDTO> banks = new ArrayList<BankDTO>();
		
                
		banks.add(new BankDTO("xml", 5.6));
		banks.add(new BankDTO("xml", 5.6));
		banks.add(new BankDTO("xml", 5.6));
		banks.add(new BankDTO("xml", 5.6));
		can.setBanks(banks);
		
		OutputStream o = new ByteArrayOutputStream();
		try {
			ser.write(can, o);
			System.out.println("String: " + o.toString());
		} catch (Exception ex) {
			Logger.getLogger(RecipientHandler.class.getName()).log(Level.SEVERE, null, ex);
		}
                       */
            
            CreditHandler ch = new CreditHandler("02_rating_channel", "02_bank_channel");
            ch.getCreditScore();
        }
}
