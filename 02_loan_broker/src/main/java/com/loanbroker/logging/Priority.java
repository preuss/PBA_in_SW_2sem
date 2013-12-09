package com.loanbroker.logging;

/**
 * User: Preuss Date: 07-12-13 Time: 09:36
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
