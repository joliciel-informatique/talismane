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
package com.joliciel.talismane.utils.io;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * A reader which reads through all the files in a given directory structure.
 * @author Assaf Urieli
 *
 */
public class DirectoryReader extends Reader implements CurrentFileProvider {
	@SuppressWarnings("unused")
	private static final Log LOG = LogFactory.getLog(DirectoryReader.class);
	private File dir;
	private List<File> files;
	private int currentIndex = 0;
	private Reader reader;
	private Charset charset;
	private String endOfFileString = "";
	private List<CurrentFileObserver> observers = new ArrayList<CurrentFileObserver>();
	private char[] leftoverBuf = null;
	
	public DirectoryReader(File dir, Charset charset) {
		super();
		this.dir = dir;
		this.charset = charset;
		files = new ArrayList<File>();
		if (this.dir.isDirectory()) {
			this.addFiles(this.dir, files);
		} else {
			this.files.add(this.dir);
		}
	}

	@Override
	public int read(char[] cbuf, int off, int len) throws IOException {
		if (leftoverBuf!=null) {
			char[] newLeftovers = null;
			int j=0;
			int k=0;
			for (int i=0; i<leftoverBuf.length; i++) {
				if (i<cbuf.length) {
					cbuf[i] = leftoverBuf[i];
					k++;
				} else {
					if (newLeftovers==null)
						newLeftovers = new char[leftoverBuf.length - cbuf.length];
					newLeftovers[j++] = leftoverBuf[i];
				}
			}
			leftoverBuf = newLeftovers;
			return k;
		}
		if (reader==null) {
			if (currentIndex<files.size()) {
				File file = files.get(currentIndex++);
				this.reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), charset));
				for (CurrentFileObserver observer : observers) {
					observer.onNextFile(file);
				}
			} else {
				return -1;
			}
		}
		int result = this.reader.read(cbuf, off, len);
		if (result<0) {
			result = endOfFileString.length();
			if (endOfFileString.length()>cbuf.length)
				result = cbuf.length;
			int j=0;
			for (int i=0; i<endOfFileString.length(); i++) {
				if (i<cbuf.length)
					cbuf[i] = endOfFileString.charAt(i);
				else {
					if (leftoverBuf==null)
						leftoverBuf = new char[endOfFileString.length()-cbuf.length];
					leftoverBuf[j++] = endOfFileString.charAt(i);;
				}
			}
			this.reader.close();
			this.reader = null;
		}
		return result;
	}

	@Override
	public void close() throws IOException {
		if (this.reader!=null)
			this.reader.close();
	}


	private void addFiles(File directory, List<File> files) {
		File[] theFiles = directory.listFiles();
		Arrays.sort(theFiles);
		for (File file : theFiles) {
			if (file.isDirectory())
				this.addFiles(file, files);
			else
				files.add(file);
		}
	}

	public void addCurrentFileObserver(CurrentFileObserver observer) {
		this.observers.add(observer);
	}

	/**
	 * A string to add at the end of each file to ensure proper processing.
	 */
	public String getEndOfFileString() {
		return endOfFileString;
	}

	public void setEndOfFileString(String endOfFileString) {
		this.endOfFileString = endOfFileString;
	}
	
}
