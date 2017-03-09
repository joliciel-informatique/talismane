///////////////////////////////////////////////////////////////////////////////
//Copyright (C) 2017 Joliciel Informatique
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
package com.joliciel.talismane.lexicon;

import java.io.Serializable;
import java.util.Map;
import java.util.stream.Collectors;

import com.joliciel.talismane.TalismaneException;

import gnu.trove.map.hash.THashMap;

/**
 * A class storing reference data to support compact lexical entries.
 * 
 * @author Assaf Urieli
 *
 */
public class CompactLexicalEntrySupport implements Serializable {
	private static final long serialVersionUID = 1L;

	private static final int INITIAL_CAPACITY = 1000;

	private Map<LexicalAttribute, Map<String, Byte>> attributeStringToByteMap = new THashMap<LexicalAttribute, Map<String, Byte>>(INITIAL_CAPACITY, 0.75f);
	private Map<LexicalAttribute, Map<Byte, String>> attributeByteToStringMap = new THashMap<LexicalAttribute, Map<Byte, String>>(INITIAL_CAPACITY, 0.75f);
	private Map<String, LexicalAttribute> nameToAttributeMap = new THashMap<String, LexicalAttribute>();
	private int otherAttributeIndex = 0;

	private String name;

	protected CompactLexicalEntrySupport() {
	}

	public CompactLexicalEntrySupport(String name) {
		this.name = name;
	}

	public byte getOrCreateAttributeCode(LexicalAttribute attribute, String value) {
		Map<String, Byte> attributeCodes = attributeStringToByteMap.get(attribute);
		Map<Byte, String> attributeValues = attributeByteToStringMap.get(attribute);
		byte code = 0;
		if (attributeCodes == null) {
			attributeCodes = new THashMap<String, Byte>();
			attributeStringToByteMap.put(attribute, attributeCodes);
			attributeValues = new THashMap<Byte, String>();
			attributeByteToStringMap.put(attribute, attributeValues);

		}

		Byte codeObj = attributeCodes.get(value);
		code = codeObj == null ? 0 : codeObj.byteValue();

		if (code == 0) {
			code = (byte) (attributeCodes.size() + 1);
			attributeCodes.put(value, code);
			attributeValues.put(code, value);
		}

		return code;
	}

	public byte getAttributeCode(LexicalAttribute attribute, String value) {
		Map<String, Byte> attributeCodes = attributeStringToByteMap.get(attribute);
		byte code = 0;
		if (attributeCodes != null) {
			Byte codeObj = attributeCodes.get(value);
			code = codeObj == null ? 0 : codeObj.byteValue();
		}
		return code;
	}

	public String getAttributeValue(LexicalAttribute attribute, byte code) {
		Map<Byte, String> attributeValues = attributeByteToStringMap.get(attribute);
		String value = null;
		if (attributeValues != null) {
			value = attributeValues.get(code);
		}
		if (value == null)
			value = "";
		return value;
	}

	public LexicalAttribute getAttributeForName(String name) {
		LexicalAttribute attribute = this.nameToAttributeMap.get(name);
		if (attribute == null) {
			otherAttributeIndex++;
			if (otherAttributeIndex > 8)
				throw new TalismaneException(
						"Only 8 OtherAttributes allowed. Already used: " + this.nameToAttributeMap.keySet().stream().collect(Collectors.joining(", ")));
			attribute = LexicalAttribute.valueOf("OtherAttribute" + otherAttributeIndex);
			nameToAttributeMap.put(name, attribute);
		}
		return attribute;
	}

	public String getName() {
		return name;
	}
}
