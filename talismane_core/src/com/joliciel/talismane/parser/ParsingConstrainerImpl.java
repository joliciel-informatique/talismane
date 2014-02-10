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
package com.joliciel.talismane.parser;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Writer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.joliciel.talismane.TalismaneSession;
import com.joliciel.talismane.utils.LogUtils;

class ParsingConstrainerImpl implements ParsingConstrainer {
	private static final Log LOG = LogFactory.getLog(ParsingConstrainerImpl.class);
	private static final long serialVersionUID = 1L;

	private Map<String,Set<String>> possibleTransitionMap = new HashMap<String, Set<String>>();
	private TransitionSystem transitionSystem;
	private transient ParserServiceInternal parseServiceInternal;
	private transient File file;
	
	@Override
	public Set<Transition> getPossibleTransitions(
			ParseConfiguration configuration) {
		String key = this.getKey(configuration);
		Set<String> transitionCodes = possibleTransitionMap.get(key);

		Set<Transition> transitions = new TreeSet<Transition>();
		if (transitionCodes!=null) {
			for (String code : transitionCodes) {
				transitions.add(this.getTransitionSystem().getTransitionForCode(code));
			}
		} else {
			transitions = this.getTransitionSystem().getTransitions();
		}
		
		return transitions;
	}
	
	String getKey(ParseConfiguration configuration) {
		String key = null;
		if (configuration.getStack().isEmpty() && configuration.getBuffer().isEmpty()) {
			key = "|";
		} else if (configuration.getBuffer().isEmpty()) {
			key = configuration.getStack().getFirst().getTag().getCode();
		} else if (configuration.getStack().isEmpty()) {
			key = configuration.getBuffer().getFirst().getTag().getCode();
		} else {
			key = configuration.getStack().getFirst().getTag().getCode()
				+ "|" + configuration.getBuffer().getFirst().getTag().getCode();
		}
		return key;
	}

	@Override
	public TransitionSystem getTransitionSystem() {
		return transitionSystem;
	}

	public ParserServiceInternal getParseServiceInternal() {
		return parseServiceInternal;
	}

	public void setParseServiceInternal(ParserServiceInternal parseServiceInternal) {
		this.parseServiceInternal = parseServiceInternal;
	}

	@Override
	public void onNextParseConfiguration(ParseConfiguration parseConfiguration, Writer writer) {		
		ParseConfiguration currentConfiguration = parseServiceInternal.getInitialConfiguration(parseConfiguration.getPosTagSequence());
		for (int i=0; i<parseConfiguration.getTransitions().size(); i++) {
			String key = this.getKey(currentConfiguration);
			Set<String> transitionCodes = this.possibleTransitionMap.get(key);
			if (transitionCodes==null) {
				transitionCodes = new HashSet<String>();
				this.possibleTransitionMap.put(key, transitionCodes);
			}
			Transition transition = parseConfiguration.getTransitions().get(i);
			transitionCodes.add(transition.getCode());
			transition.apply(currentConfiguration);
		}
	}

	public void setFile(File file) {
		this.file = file;
	}

	@Override
	public void onCompleteParse() {
		if (this.transitionSystem==null)
			this.transitionSystem = TalismaneSession.getTransitionSystem();
		
		if (this.file!=null) {
			try {
				ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(file,false));
				zos.putNextEntry(new ZipEntry("ParsingConstrainer.obj"));
				ObjectOutputStream oos = new ObjectOutputStream(zos);
				try {
					oos.writeObject(this);
				} finally {
					oos.flush();
				}
				zos.flush();
				zos.close();
			} catch (IOException ioe) {
				LogUtils.logError(LOG, ioe);
				throw new RuntimeException(ioe);
			}
		}
	}
	
	public static ParsingConstrainer loadFromFile(File inFile) {
		try {
			ZipInputStream zis = new ZipInputStream(new FileInputStream(inFile));
			zis.getNextEntry();
			ObjectInputStream in = new ObjectInputStream(zis);
			ParsingConstrainer parsingConstrainer = null;
			try {
				parsingConstrainer = (ParsingConstrainer) in.readObject();
			} catch (ClassNotFoundException e) {
				LogUtils.logError(LOG, e);
				throw new RuntimeException(e);
			}
			return parsingConstrainer;
		} catch (IOException ioe) {
			LogUtils.logError(LOG, ioe);
			throw new RuntimeException(ioe);
		}
	}

}
