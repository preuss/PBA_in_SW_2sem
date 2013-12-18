package com.loanbroker.webservice.bank;

public class Offer {

	private String ssn;
	private double interestRate;

	public Offer(String ssn, double interestRate) {
		this.ssn = ssn;
		this.interestRate = interestRate;
	}

	public String getSsn() {
		return ssn;
	}

	public void setSsn(String ssn) {
		this.ssn = ssn;
	}

	public double getInterestRate() {
		return interestRate;
	}

	public void setInterestRate(double interestRate) {
		this.interestRate = interestRate;
	}
}
