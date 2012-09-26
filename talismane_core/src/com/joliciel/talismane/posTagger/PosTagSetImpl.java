///////////////////////////////////////////////////////////////////////////////
//Copyright (C) 2010 Assaf Urieli
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
package com.joliciel.talismane.posTagger;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.joliciel.talismane.machineLearning.AbstractDecisionFactory;
import com.joliciel.talismane.utils.LogUtils;

class PosTagSetImpl extends AbstractDecisionFactory<PosTag> implements PosTagSet {
	private static final long serialVersionUID = 4894889727388356815L;
	private static final Log LOG = LogFactory.getLog(PosTagSetImpl.class);
	
	private String name;
	private Locale locale;
	private Set<PosTag> tags = new TreeSet<PosTag>();
	private Map<String,PosTag> tagMap = null;
	

	public PosTagSetImpl(File file) {
		Scanner scanner = null;
		try {
			scanner = new Scanner(file, "UTF-8");
			this.load(scanner);
		} catch (FileNotFoundException e) {
			LogUtils.logError(LOG, e);
			throw new RuntimeException(e);
		}
	}
	
	public PosTagSetImpl(Scanner scanner) {
		this.load(scanner);
	}
	
	public PosTagSetImpl(List<String> descriptors) {
		this.load(descriptors);
	}
	
	void load(Scanner scanner) {
		List<String> descriptors = new ArrayList<String>();
		while (scanner.hasNextLine()) {
			String line = scanner.nextLine();
			descriptors.add(line);
		}
		this.load(descriptors);
	}
	
	void load(List<String> descriptors) {
		boolean nameFound = false;
		boolean localeFound = false;
		for (String descriptor : descriptors) {
			LOG.debug(descriptor);
			if (descriptor.startsWith("#")) {
				continue;
			} else if (!nameFound) {
				this.name = descriptor;
				nameFound = true;
			} else if (!localeFound) {
				this.locale = new Locale(descriptor);
				localeFound = true;
			} else {
				String[] parts = descriptor.split("\t");
				tags.add(new PosTagImpl(parts[0], parts[1], PosTagOpenClassIndicator.valueOf(parts[2])));
			}
		}		
	}
	
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Locale getLocale() {
		return locale;
	}

	public void setLocale(Locale locale) {
		this.locale = locale;
	}

	public Set<PosTag> getTags() {
		return tags;
	}

	@Override
	public PosTag getPosTag(String code) {
		if (tagMap==null) {
			tagMap = new HashMap<String, PosTag>();
			for (PosTag posTag : this.getTags()) {
				tagMap.put(posTag.getCode(), posTag);
			}
		}
		PosTag posTag = tagMap.get(code);
		if (posTag==null) {
			throw new UnknownPosTagException("Unknown PosTag: " + code);
		}
		return posTag;
	}

	@Override
	public int hashCode() {
		return name.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		PosTagSetImpl other = (PosTagSetImpl) obj;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		return true;
	}

	@Override
	public PosTag createOutcome(String name) {
		return this.getPosTag(name);
	}
}
