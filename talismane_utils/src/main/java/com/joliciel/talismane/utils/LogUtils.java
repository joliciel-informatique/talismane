///////////////////////////////////////////////////////////////////////////////
//Copyright (C) 2014 Joliciel Informatique
//
//This file is part of Talismane.
//
//Talismane is free software: you can redistribute it and/or modify
//it under the terms of the GNU Affero General Public License as published by
//the Free Software Foundation, either version 3 of the License, or
//(at your option) any later version.
//
//Talismane is distributed in the hope that it will be useful,
//but WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//GNU Affero General Public License for more details.
//
//You should have received a copy of the GNU Affero General Public License
//along with Talismane.  If not, see <http://www.gnu.org/licenses/>.
//////////////////////////////////////////////////////////////////////////////
package com.joliciel.talismane.utils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;

/**
 * Various generic logging utilities.
 * 
 * @author Assaf Urieli
 *
 */
public class LogUtils {
	private static final Logger LOG = LoggerFactory.getLogger(LogUtils.class);
	private static final int MEGABYTE = 1024 * 1024;
	private static Marker fatal = MarkerFactory.getMarker("FATAL");

	public enum LogLevel {
		TRACE, DEBUG, INFO, WARN, ERROR, FATAL
	}

	/**
	 * Return the current exception and stack trace as a String.
	 */
	public static String getErrorString(Throwable e) {
		String s = null;
		try {
			StringWriter sw = new StringWriter();
			PrintWriter ps = new PrintWriter(sw);
			e.printStackTrace(ps);
			sw.flush();
			s = sw.toString();
			sw.close();
		} catch (IOException ioe) {
			// do nothing!
		}
		return s;
	}

	/**
	 * Logger the exception passed.
	 */
	public static void logError(Logger logger, Throwable e) {
		logError(null, logger, e);
	}

	/**
	 * Logger the exception passed, including the prefix passed.
	 */
	public static void logError(String prefix, Logger logger, Throwable e) {
		logError(prefix, logger, LogLevel.ERROR, e);
	}

	/**
	 * Logger the exception passed, including the prefix if not null, at the
	 * requested level.
	 */
	public static void logError(String prefix, Logger logger, LogLevel logLevel, Throwable e) {
		if (prefix != null)
			Logger(logger, logLevel, prefix + " " + getErrorString(e));
		else
			Logger(logger, logLevel, getErrorString(e));
	}

	public static void Logger(Logger Logger, LogLevel logLevel, String message) {
		switch (logLevel) {
		case TRACE:
			Logger.trace(message);
			break;
		case DEBUG:
			Logger.debug(message);
			break;
		case INFO:
			Logger.info(message);
			break;
		case WARN:
			Logger.warn(message);
			break;
		case ERROR:
			Logger.error(message);
			break;
		case FATAL:
			Logger.error(fatal, message);
			break;
		}
	}

	/**
	 * Logger the exception passed.
	 */
	public static void logError(String customer, Logger logger, Throwable e, LogLevel logLevel) {
		logger.error(customer + " " + e);
		logger.error(LogUtils.getErrorString(e));
	}

	/**
	 * Logger the available runtime memory.
	 */
	public static void logMemory(Logger logger) {
		if (logger.isTraceEnabled()) {
			// Getting the runtime reference from system
			Runtime runtime = Runtime.getRuntime();
			logger.trace("##### Heap utilization statistics [MB] #####");

			// Print used memory
			logger.trace("Used Memory:" + (runtime.totalMemory() - runtime.freeMemory()) / MEGABYTE);

			// Print free memory
			logger.trace("Free Memory:" + runtime.freeMemory() / MEGABYTE);

			// Print total available memory
			logger.trace("Total Memory:" + runtime.totalMemory() / MEGABYTE);

			// Print Maximum available memory
			logger.trace("Max Memory:" + runtime.maxMemory() / MEGABYTE);
		}
	}

	/**
	 * If logConfigPath is not null, use it to configure logging. Otherwise, use
	 * the default configuration file.
	 */
	public static void configureLogging(String logConfigPath) {
		try {
			LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
			JoranConfigurator configurator = new JoranConfigurator();
			configurator.setContext(loggerContext);
			if (logConfigPath != null) {
				File slf4jFile = new File(logConfigPath);

				if (slf4jFile.exists()) {
					// Call context.reset() to clear any previous configuration,
					// e.g. default configuration
					loggerContext.reset();
					configurator.doConfigure(slf4jFile);
				} else {
					throw new JolicielException("missing logConfigFile: " + slf4jFile.getCanonicalPath());
				}
			} else {
				InputStream stream = LogUtils.class.getResourceAsStream("/com/joliciel/talismane/utils/resources/default-logback.xml");

				configurator.setContext(loggerContext);
				// Call context.reset() to clear any previous configuration,
				// e.g. default configuration
				loggerContext.reset();
				configurator.doConfigure(stream);
			}
		} catch (JoranException e) {
			LogUtils.logError(LOG, e);
			throw new RuntimeException(e);
		} catch (IOException e) {
			LogUtils.logError(LOG, e);
			throw new RuntimeException(e);
		}
	}
}
