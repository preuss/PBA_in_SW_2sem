package com.loanbroker.models;

import java.util.Objects;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.Root;

/**
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

	@Override
	public int hashCode() {
		int hash = 7;
		hash = 73 * hash + Objects.hashCode(this.name);
		return hash;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		final BankDTO other = (BankDTO) obj;
		if (!Objects.equals(this.name, other.name)) {
			return false;
		}
		return true;
	}
	

	@Override
	public String toString() {
		return "BankDTO{" +
				"name='" + name + '\'' +
				", interestRate=" + interestRate +
				'}';
	}
}
