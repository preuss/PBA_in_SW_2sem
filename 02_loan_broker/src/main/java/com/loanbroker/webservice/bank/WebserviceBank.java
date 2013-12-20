package com.loanbroker.webservice.bank;

import javax.jws.WebService;
import javax.jws.WebMethod;
import javax.jws.WebParam;

/**
 * @author Preuss
 */
@WebService(serviceName = "WebserviceBank")
public class WebserviceBank {

	/**
	 * This is a sample web service operation
	 */
	@WebMethod(operationName = "hello")
	public String hello(@WebParam(name = "name") String txt) {
		return "Hello " + txt + " !";
	}

	/**
	 * Web service operation
	 */
	@WebMethod(operationName = "CalculateInterest")
	public Offer CalculateInterest(@WebParam(name = "ssn") String ssn, @WebParam(name = "creditScore") int creditScore, @WebParam(name = "loanAmount") double loanAmount, @WebParam(name = "loanDuration") int loanDuration) {
		double interestRate = ((Math.random() * (12 - 3) + 3));

		Offer offer = new Offer(ssn, interestRate);
		return offer;
	}
}
