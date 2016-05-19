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
package com.joliciel.talismane.posTagger;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joliciel.talismane.utils.LogUtils;

final class PosTagSetImpl implements PosTagSet {
	private static final long serialVersionUID = 4894889727388356815L;
	private static final Logger LOG = LoggerFactory.getLogger(PosTagSetImpl.class);

	private String name;
	private Locale locale;
	private Set<PosTag> tags = new TreeSet<PosTag>();
	private Map<String, PosTag> tagMap = null;

	public PosTagSetImpl(File file) {
		try (Scanner scanner = new Scanner(new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8")))) {
			this.load(scanner);
		} catch (IOException e) {
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
			}

			if (!nameFound) {
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

	@Override
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Override
	public Locale getLocale() {
		return locale;
	}

	public void setLocale(Locale locale) {
		this.locale = locale;
	}

	@Override
	public Set<PosTag> getTags() {
		return tags;
	}

	@Override
	public PosTag getPosTag(String code) {
		if (tagMap == null) {
			tagMap = new HashMap<String, PosTag>();
			for (PosTag posTag : this.getTags()) {
				tagMap.put(posTag.getCode(), posTag);
				tagMap.put(RootPosTag.ROOT_POS_TAG_CODE, RootPosTag.ROOT_POS_TAG);
			}
		}
		PosTag posTag = tagMap.get(code);
		if (posTag == null) {
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
}
