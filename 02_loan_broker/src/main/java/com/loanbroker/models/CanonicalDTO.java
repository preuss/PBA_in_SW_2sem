/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.loanbroker.models;

import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author Preuss
 */
public class CanonicalDTO {

	@Element(required = false) // True
	private String ssn;
	@Element(required = false) // True
	private double loanAmount;
	@Element(required = false) // True
	private int loanDuration; // Month.
	@Element(required = false)
	private Integer creditScore;
	@ElementList(required = false)
	private ArrayList<BankDTO> banks;

	public CanonicalDTO() {
		ssn = "";
		loanAmount = 0;
		loanDuration = 360;
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

	public int getLoanDuration() {
		return loanDuration;
	}

	public void setLoanDuration(int loanDuration) {
		this.loanDuration = loanDuration;
	}

	public Integer getCreditScore() {
		return creditScore;
	}

	public void setCreditScore(Integer creditScore) {
		this.creditScore = creditScore;
	}

	public List<BankDTO> getBanks() {
		return banks;
	}

	public void setBanks(List<BankDTO> banks) {
		this.banks = new ArrayList<BankDTO>(banks);
	}

	public boolean addBank(BankDTO bank) {
		if (this.banks == null) {
			this.banks = new ArrayList<BankDTO>();
		}
		return this.banks.add(bank);
	}

	@Override
	public String toString() {

		return "CanonicalDTO{" +
				"ssn='" + ssn + '\'' +
				", loanAmount=" + loanAmount +
				", loanDuration=" + loanDuration +
				", creditScore=" + creditScore +
				", banks=" + (banks != null ? Arrays.toString(banks.toArray(new BankDTO[0])) : "null") +
				'}';
	}
}
