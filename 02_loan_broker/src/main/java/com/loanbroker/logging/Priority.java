package com.loanbroker.logging;

/**
 * User: Preuss
 */
public class Priority extends Level {

	// Another alias
	protected Priority(String name, int value) {
		super(name, value);
	}

	protected Priority(String name, int value, String resourceBundleName) {
		super(name, value, resourceBundleName);
	}
}
