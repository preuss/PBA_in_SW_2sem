/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.loanbroker._loan_broker;

import wservices.CreditScoreService;
import wservices.CreditScoreService_Service;

/**
 *
 * @author Andreas
 */
public class CreditRating {

	public void testService() {

		try { // Call Web Service Operation
			CreditScoreService_Service service = new CreditScoreService_Service();
			CreditScoreService port = service.getCreditScoreServicePort();
			// TODO initialize WS operation arguments here
			String ssn = "12345678-1234";
			// TODO process result here
			int result = port.creditScore(ssn);
			System.out.println("Result from Credit Bureau = " + result);
		} catch (Exception ex) {
			// TODO handle custom exceptions here
		}

	}
}
