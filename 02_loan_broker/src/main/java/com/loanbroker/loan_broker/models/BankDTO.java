package com.loanbroker.loan_broker.models;

import org.simpleframework.xml.Element;
import org.simpleframework.xml.Root;

/**
 *
 * @author Preuss
 */
@Root
public class BankDTO {

	@Element(required = true)
	private String name;
	@Element(required = true)
	private double interestRate;

	public BankDTO() {
	}

	public BankDTO(String name, double interestRate) {
		this.name = name;
		this.interestRate = interestRate;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public double getInterestRate() {
		return interestRate;
	}

	public void setInterestRate(double interestRate) {
		this.interestRate = interestRate;
	}

}
