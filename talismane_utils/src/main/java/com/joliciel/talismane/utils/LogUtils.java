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

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import org.apache.commons.logging.Log;

/**
 * Various generic logging utilities.
 * @author Assaf Urieli
 *
 */
public class LogUtils {
	private static final int MEGABYTE = 1024*1024;

	public enum LogLevel {
		TRACE, DEBUG, INFO, WARN, ERROR, FATAL
	}
	
    /**
    * Return the current exception and stack trace as a String.
    */
    public static String getErrorString(Throwable e)
    {
        String s = null;
        try {
            StringWriter sw = new StringWriter();
            PrintWriter ps = new PrintWriter((Writer) sw);
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
    * Log the exception passed.
    */
    public static void logError(Log logger, Throwable e) {
        logError(null, logger, e);
    }
    
    /**
    * Log the exception passed, including the prefix passed.
    */
    public static void logError(String prefix, Log logger, Throwable e) {
        logError(prefix, logger, LogLevel.ERROR, e);
    }
    
    /**
    * Log the exception passed, including the prefix if not null, at the requested level.
    */
    public static void logError(String prefix, Log logger, LogLevel logLevel, Throwable e) {
    	if (prefix!=null)
    		log(logger, logLevel, prefix + " " + getErrorString(e));
    	else
    		log(logger, logLevel, getErrorString(e));
    }
    
    public static void log(Log log, LogLevel logLevel, String message) {
    	switch (logLevel) {
    	case TRACE:
            log.trace(message);
            break;
    	case DEBUG:
            log.debug(message);
        	break;
    	case INFO:
            log.info(message);
        	break;
    	case WARN:
            log.warn(message);
        	break;
    	case ERROR:
        	log.error(message);
        	break;
    	case FATAL:
    		log.fatal(message);
    		break;
    	}
    }
    
    /**
    * Log the exception passed.
    */
    public static void logError(String customer, Log logger, Throwable e, LogLevel logLevel) {
        logger.error(customer + " " + e);
        logger.error(LogUtils.getErrorString(e));
    }
    
    /**
     * Log the available runtime memory.
     */
    public static void logMemory(Log logger) {
    	if (logger.isTraceEnabled()) {
	    	//Getting the runtime reference from system
			Runtime runtime = Runtime.getRuntime();
			logger.trace("##### Heap utilization statistics [MB] #####");
	
			//Print used memory
			logger.trace("Used Memory:"
				+ (runtime.totalMemory() - runtime.freeMemory()) / MEGABYTE);
	
			//Print free memory
			logger.trace("Free Memory:"
				+ runtime.freeMemory() / MEGABYTE);
	
			//Print total available memory
			logger.trace("Total Memory:" + runtime.totalMemory() / MEGABYTE);
	
			//Print Maximum available memory
			logger.trace("Max Memory:" + runtime.maxMemory() / MEGABYTE);	
    	}
	}
}
