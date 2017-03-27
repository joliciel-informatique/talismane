///////////////////////////////////////////////////////////////////////////////
//Copyright (C) 2016 Joliciel Informatique
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
package com.joliciel.talismane.tokeniser;

import java.io.Serializable;
import java.util.Map;

/**
 * A marker for token annotations.
 * 
 * @author Assaf Urieli
 *
 */
public class TokenBoundary implements Serializable {
  private static final long serialVersionUID = 1L;

  private final String text;
  private final String analysisText;
  private final Map<String, TokenAttribute<?>> attributes;

  public TokenBoundary(String text, String analysisText, Map<String, TokenAttribute<?>> attributes) {
    this.text = text;
    this.analysisText = analysisText;
    this.attributes = attributes;
  }

  /**
   * The token's text for analysis purposes, see
   * {@link Token#getAnalyisText()}.
   * 
   * @return
   */
  public String getAnalysisText() {
    return analysisText;
  }

  /**
   * The token's processed text, see {@link Token#getText()}.
   * 
   * @return
   */
  public String getText() {
    return text;
  }

  /**
   * Any attributes assigned to this token.
   */
  public Map<String, TokenAttribute<?>> getAttributes() {
    return attributes;
  }

  /**
   * Return the attribute value corresponding to a particular key, or null if
   * the key is missing.
   */
  public <T extends Serializable> T getAttributeValue(String key) {
    @SuppressWarnings("unchecked")
    TokenAttribute<T> attribute = (TokenAttribute<T>) attributes.get(key);
    if (attribute == null)
      return null;
    return attribute.getValue();
  }

  @Override
  public String toString() {
    return this.getClass().getSimpleName();
  }

}
