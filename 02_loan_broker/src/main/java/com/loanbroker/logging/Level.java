package com.loanbroker.logging;

/**
 * @User: Preuss
 */
public class Level extends java.util.logging.Level {

	protected Level(String name, int value) {
		super(name, value);
	}

	protected Level(String name, int value, String resourceBundleName) {
		super(name, value, resourceBundleName);
	}

	public static final void initializeCustomLogLevels() {
		//Create your levels *before* you retrieve loggers.
		//This needs to be called the first in the main, before using anything like loading config for logging.
		java.util.logging.Level x = FATAL; // Now all static is initialized.
	}

	//[FATAL]	[EMERGENCY]	[CRITICAL]	[ERROR]	SEVERE	WARNING	[STATUS]	INFO	CONFIG	[DEBUG]	FINE	FINER	FINEST/[TRACE]
	//[1400]	[1300]		1200		1100	1000	900		850			800		700		[600]	500     400     300
	public static final Level OFF = new Level("OFF", Integer.MAX_VALUE);

	public static final Level FATAL = new Level("FATAL", 1400);
	public static final Level EMERGENCY = new Level("EMERGENCY", 1300);
	public static final Level CRITICAL = new Level("CRITICAL", 1200);
	public static final Level ERROR = new Level("ERROR", 1100);

	// START old
	public static final Level SEVERE = new Level("SEVERE", 1000);
	public static final Level WARNING = new Level("WARNING", 900);
	//END old

	public static final Level STATUS = new Level("STATUS", 850);

	// START old
	public static final Level INFO = new Level("INFO", 800);
	public static final Level CONFIG = new Level("CONFIG", 700);
	// END old

	public static final Level DEBUG = new Level("DEBUG", 600);

	// START old
	public static final Level FINE = new Level("FINE", 500);
	public static final Level FINER = new Level("FINER", 400);
	public static final Level FINEST = new Level("FINEST", 300);
	// END old

	public static final Level TRACE = new Level("TRACE", 300);

	public static final Level ALL = new Level("ALL", Integer.MIN_VALUE);

	//---------- Level sorted aliases ----------
	public static final Level L01 = new Level("FATAL", 1400);
	public static final Level L02 = new Level("EMERGENCY", 1300);
	public static final Level L03 = new Level("CRITICAL", 1200);
	public static final Level L04 = new Level("ERROR", 1100);
	public static final Level L05 = new Level("SEVERE", 1000);
	public static final Level L06 = new Level("WARNING", 900);
	public static final Level L07 = new Level("STATUS", 850);
	public static final Level L08 = new Level("INFO", 800);
	public static final Level L09 = new Level("CONFIG", 700);
	public static final Level L10 = new Level("DEBUG", 600);
	public static final Level L11 = new Level("FINE", 500);
	public static final Level L12 = new Level("FINER", 400);
	public static final Level L13 = new Level("FINEST", 300);
	public static final Level L14 = new Level("TRACE", 300);
}
