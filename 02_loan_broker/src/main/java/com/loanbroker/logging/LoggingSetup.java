package com.loanbroker.logging;

import java.util.logging.Handler;

/**
 * User: Preuss Date: 09-12-13 Time: 00:34
 */
public class LoggingSetup {

	public static void setupLogging(java.util.logging.Level level) {
		//static {
		// Get the root logger
		java.util.logging.Logger rootLogger = java.util.logging.Logger.getLogger("");
		for (Handler handler : rootLogger.getHandlers()) {
			// Change log level of default handler(s) of root logger
			// The paranoid would check that this is the ConsoleHandler ;)
			handler.setFormatter(new MyCustomFormatter());
			handler.setLevel(java.util.logging.Level.FINEST);
		}
		// Set root logger level
		rootLogger.setLevel(level);
		//}
	}
}
