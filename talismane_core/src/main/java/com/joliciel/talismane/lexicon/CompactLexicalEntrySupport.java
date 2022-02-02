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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

  private Map<LexicalAttribute, Map<String, byte[]>> attributeStringToByteMap = new THashMap<>(INITIAL_CAPACITY, 0.75f);
  private Map<LexicalAttribute, List<String>> attributeByteToStringMap = new THashMap<>(INITIAL_CAPACITY, 0.75f);
  private Map<String, LexicalAttribute> nameToAttributeMap = new THashMap<String, LexicalAttribute>();
  private int otherAttributeIndex = 0;

  private String name;

  protected CompactLexicalEntrySupport() {
  }

  public CompactLexicalEntrySupport(String name) {
    this.name = name;
  }

  /**
   * Get or create a byte array used to uniquely represent this attribute value.
   * <br>
   * The last byte is guaranteed to be positive (e.g. bit 8 is 0). All other
   * bytes are guaranteed to be negative (e.g. bit 8 is 1).
   */
  public byte[] getOrCreateAttributeCode(LexicalAttribute attribute, String value) {
    Map<String, byte[]> attributeCodes = attributeStringToByteMap.get(attribute);
    List<String> attributeValues = attributeByteToStringMap.get(attribute);
    if (attributeCodes == null) {
      attributeCodes = new THashMap<>();
      attributeStringToByteMap.put(attribute, attributeCodes);
      attributeValues = new ArrayList<>();
      attributeByteToStringMap.put(attribute, attributeValues);
    }

    byte[] bytes = attributeCodes.get(value);

    if (bytes == null) {
      int code = (attributeCodes.size() + 1);

      // the 8th bit is used to mark the use of an additional byte
      // thus if we need another byte, we OR it with bit 8
      int bigBytes = code / 128;
      int remainder = code % 128;
      bytes = new byte[] { (byte) remainder };
      while (bigBytes > 0) {
        byte[] newBytes = new byte[bytes.length + 1];
        for (int i = 0; i < bytes.length - 1; i++)
          newBytes[i] = bytes[i];
        newBytes[bytes.length - 1] = (byte) (bytes[bytes.length - 1] | 0b10000000);
        remainder = bigBytes % 128;
        bigBytes = bigBytes / 128;
        newBytes[bytes.length] = (byte) remainder;
        bytes = newBytes;
      }

      attributeCodes.put(value, bytes);
      attributeValues.add(value);
    }

    return bytes;
  }

  /**
   * Get the attribute value corresponding to a particular position in the byte
   * array, where the bytes at this position were returned by a call to
   * {@link #getOrCreateAttributeCode(LexicalAttribute, String)}.
   */
  public String getAttributeValue(LexicalAttribute attribute, byte[] bytes, int pos) {
    byte b = bytes[pos];
    int code = 0;
    int i = 0;
    while (b < 0) {
      // remove the bit marking that an additional byte is required
      int val = b & 0b01111111;
      for (int j = 0; j < i; j++)
        val *= 128;
      code += val;
      b = bytes[++pos];
      i++;
    }
    int val = b;
    for (int j = 0; j < i; j++)
      val *= 128;
    code += val;
    List<String> attributeValues = attributeByteToStringMap.get(attribute);
    String value = attributeValues.get(code - 1);
    return value;
  }

  /**
   * Return the attribute corresponding to a particular attribute name.
   */
  public LexicalAttribute getAttributeForName(String name) {
    LexicalAttribute attribute = this.nameToAttributeMap.get(name);
    if (attribute == null) {
      otherAttributeIndex++;
      if (otherAttributeIndex > 8)
        throw new RuntimeException(
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
