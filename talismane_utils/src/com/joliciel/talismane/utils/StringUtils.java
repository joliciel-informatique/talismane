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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Various generic utilities for use with Strings.
 * @author Assaf Urieli
 *
 */
public class StringUtils {
	private static final Log LOG = LogFactory.getLog(StringUtils.class);
	public static String padRight(String s, int n) {
	     return String.format("%1$-" + n + "s", s);  
	}

	public static String padLeft(String s, int n) {
	    return String.format("%1$" + n + "s", s);  
	}
	

	public static Map<String, String> convertArgs(String[] args) {
		Map<String,String> argMap = new HashMap<String, String>();
		for (String arg : args) {
			int equalsPos = arg.indexOf('=');
			if (equalsPos<0) {
				throw new RuntimeException("Argument " + arg + " has no value");
			}
				
			String argName = arg.substring(0, equalsPos);
			String argValue = arg.substring(equalsPos+1);
			if (argMap.containsKey(argName))
				throw new RuntimeException("Duplicate command-line argument: " + argName);
			argMap.put(argName, argValue);
		}
		return argMap;
	}
	
	/**
	 * Get a map of strings from a path pointing at a properties file.
	 */
	public static Map<String,String> getArgMap(String propertiesPath) {
		File propsFile = new File(propertiesPath);
		return getArgMap(propsFile);
	}
	
	/**
	 * Get a map of strings from a properties file, using the UTF-8 encoding.
	 */
	public static Map<String,String> getArgMap(File propsFile) {
		return getArgMap(propsFile, "UTF-8");
	}
	
	/**
	 * Get a map of strings from a properties file.
	 */
	public static Map<String,String> getArgMap(File propsFile, String encoding) {
		return getArgMap(propsFile, Charset.forName(encoding));
	}
	
	/**
	 * Get a map of strings from a properties file.
	 */
	public static Map<String,String> getArgMap(File propsFile, Charset charset) {
		try {
			FileInputStream propsInputStream = new FileInputStream(propsFile);
			Properties props = new Properties();
			props.load(new BufferedReader(new InputStreamReader(propsInputStream, charset)));
			
			return getArgMap(props);
		} catch (IOException e) {
			LogUtils.logError(LOG, e);
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * Get a map of strings from Properties.
	 */
	public static Map<String,String> getArgMap(Properties props) {
		Map<String,String> argMap = new HashMap<String, String>();
		for (String propertyName : props.stringPropertyNames()) {
			argMap.put(propertyName, props.getProperty(propertyName));
		}
		return argMap;
	}
	
	/**
	 * Get a map of strings from Properties, for any properties beginning with a certain prefix.
	 * The prefix will be removed from the property keys.
	 */
	public static Map<String,String> getArgMap(Properties props, String prefix) {
		Map<String,String> argMap = new HashMap<String, String>();
		for (String propertyName : props.stringPropertyNames()) {
			if (propertyName.startsWith(prefix))
				argMap.put(propertyName.substring(prefix.length()), props.getProperty(propertyName));
		}
		return argMap;
	}
	
	public static String readerToString(Reader reader) {
		try {
			char[] chars = new char[1024];
			StringBuilder sb = new StringBuilder();
			int numChars;
	
			while ((numChars = reader.read(chars, 0, chars.length)) > 0) {
				sb.append(chars, 0, numChars);
			}
	
			return sb.toString();
		} catch (IOException e) {
			LogUtils.logError(LOG, e);
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * Count how many times a character is repeated in a string,
	 * starting at a given position and moving forwards or backwards.
	 */
	public static int countChar(String string, char c, int pos, boolean forwards) {
		int count = 0;
		int increment = 1;
		if (!forwards) increment = -1;
		for (int i=pos; i>=0 && i<string.length(); i+=increment) {
			char c2 = string.charAt(i);
			if (c2==c)
				count++;
			else
				break;
		}
		return count;
	}
}
