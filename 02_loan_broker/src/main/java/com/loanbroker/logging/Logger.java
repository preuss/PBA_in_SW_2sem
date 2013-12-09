package com.loanbroker.logging;

import java.util.logging.Handler;
import java.util.logging.LogRecord;

/**
 * User: Preuss
 * Date: 07-12-13
 * Time: 09:40
 */
public class Logger {
	java.util.logging.Logger log;

	public Logger(java.util.logging.Logger realLogger) {
		this.log = realLogger;
	}

	public static Logger getGlobal() {
		return new Logger(java.util.logging.Logger.getGlobal());
	}

	public static Logger getLogger(String name) {
		return new Logger(java.util.logging.Logger.getLogger(name));
	}

	public static Logger getLogger(Class clazz) {
		return getLogger(clazz.getName());
	}

	public void addHandler(Handler handler) throws SecurityException {
		log.addHandler(handler);
	}

	public void removeHandler(Handler handler) throws SecurityException {
		log.removeHandler(handler);
	}

	public Handler[] getHandlers() {
		return log.getHandlers();
	}

	public void log(LogRecord record) {
		log.log(record);
	}

	public void log(java.util.logging.Level level, String msg) {
		log.log(level, msg);
	}

	public void log(java.util.logging.Level level, String msg, Object param1) {
		log.log(level, msg, param1);
	}

	public void log(java.util.logging.Level level, String msg, Object[] params) {
		log.log(level, msg, params);
	}

	public void log(java.util.logging.Level level, String msg, Throwable thrown) {
		log.log(level, msg, thrown);
	}

	public void setLevel(java.util.logging.Level newLevel) throws SecurityException {
		log.setLevel(newLevel);
	}

	public java.util.logging.Level getLevel() {
		return log.getLevel();
	}

	public boolean isLoggable(java.util.logging.Level level) {
		return log.isLoggable(level);
	}

	public String getName() {
		return log.getName();
	}

	public void fatal(String msg) {
		log.log(Level.FATAL, msg);
	}

	public void emergency(String msg) {
		log.log(Level.EMERGENCY, msg);
	}

	public void critical(String msg) {
		log.log(Level.CRITICAL, msg);
	}

	public void error(String msg) {
		log.log(Level.ERROR, msg);
	}

	public void severe(String msg) {
		log.severe(msg);
	}

	public void warning(String msg) {
		log.warning(msg);
	}

	public void status(String msg) {
		log.log(Level.STATUS, msg);
	}

	public void info(String msg) {
		log.info(msg);
	}

	public void config(String msg) {
		log.config(msg);
	}

	public void debug(String msg) {
		log.log(Level.DEBUG, msg);
	}

	public void fine(String msg) {
		log.fine(msg);
	}

	public void finer(String msg) {
		log.finer(msg);
	}

	public void finest(String msg) {
		log.finest(msg);
	}

	public void trace(String msg) {
		log.log(Level.TRACE, msg);
	}
}
