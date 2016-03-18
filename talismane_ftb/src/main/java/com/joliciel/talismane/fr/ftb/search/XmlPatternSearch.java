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
package com.joliciel.talismane.fr.ftb.search;

/**
 * A search based on an XML pattern, where the elements actually filled in are added as criteria.
 * For example:
 * <code>
 * <phrase>
 *    &lt;phrase type="VN,VPinf"&gt;
 *        &lt;w cat="V" /&gt;
 *    &lt;/phrase&gt;
 *    &lt;NP&gt;
 *        &lt;w cat="N" /&gt;
 *        &lt;PP&gt;
 *            &lt;w cat="P" lemma="de,�"&gt;de,du,�,au&lt;/w&gt;
 *            &lt;NP&gt;
 *                &lt;w cat="N"/&gt;
 *            &lt;/NP&gt;
 *        &lt;/PP&gt;
 *    &lt;/NP&gt;
 * &lt;/phrase&gt;
 * </code>
 * Will search for elements nested as shown in any parent phrase.
 * Phrase type can either be limited using the type attribute on the phrase element, or may be indicated directly using NP elements, etc.
 * For words, categories, lemmas and word text will be respected, as well as element ordering.
 * Subcategories can only be used if a single category is specified.
 * Multiple attributes may be indicated using a comma-separated list.
 * The special indicator "[null]" is used to select only those elements where the attribute in question was empty of not specified. It must be used on its own.
 * The search results will include a PhraseUnit for each w element indicated, and the Phrase containing all of them.
 * @author Assaf Urieli
 */
public interface XmlPatternSearch extends Search {
    /** the XML pattern on which this search is based */
    public String getXmlPattern();
    public void setXmlPattern(String xmlPattern);
    public void setXmlPatternFile(String xmlPatternFile);
}
