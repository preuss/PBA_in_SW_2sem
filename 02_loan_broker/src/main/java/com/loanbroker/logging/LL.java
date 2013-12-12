package com.loanbroker.logging;

/**
 * @User: Preuss 
 */
public class LL extends Level {

	// Just an alias class, to easier use custom log levels.
	protected LL(String name, int value) {
		super(name, value);
	}

	protected LL(String name, int value, String resourceBundleName) {
		super(name, value, resourceBundleName);
	}
}
