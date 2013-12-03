/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.loanbroker.loan_broker.models;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Default;
import org.simpleframework.xml.DefaultType;
import org.simpleframework.xml.Element;

/**
 *
 * @author Preuss
 */
public class CanonicalDTO {

	@Element(required = true)
	private String ssn;
	@Element(required = true)
	private double loanAmount;

	@Element(required = false)
	private Integer creditScore;

	@Element(required = false)
	private String test;

	public CanonicalDTO() {
		ssn = "";
		loanAmount = 0;
	}

	public String getSsn() {
		return ssn;
	}

	public void setSsn(String ssn) {
		this.ssn = ssn;
	}

	public double getLoanAmount() {
		return loanAmount;
	}

	public void setLoanAmount(double loanAmount) {
		this.loanAmount = loanAmount;
	}

	public int getCreditScore() {
		return creditScore;
	}

	public void setCreditScore(int creditScore) {
		this.creditScore = creditScore;
	}
}
