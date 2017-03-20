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
package com.joliciel.talismane.corpus;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joliciel.talismane.TalismaneSession;
import com.joliciel.talismane.lexicon.LexicalEntry;

/**
 * Represents one line in an annotated corpus, corresponding to a single token.
 * 
 * @author Assaf Urieli
 *
 */
public class CorpusLine {
	@SuppressWarnings("unused")
	private static final Logger LOG = LoggerFactory.getLogger(CorpusLine.class);
	private final String line;
	private final int lineNumber;
	private final Map<CorpusElement, String> elements = new HashMap<>();
	private LexicalEntry lexicalEntry;

	public enum CorpusElement {
		/**
		 * A unique index for a given token, starting at 1.
		 */
		INDEX("(\\d+)"),
		/**
		 * The token - note that we assume CoNLL formatting based on
		 * {@link TalismaneSession#getCoNLLFormatter()}
		 */
		TOKEN("(.*?)"),
		/**
		 * The lemma - note that we assume CoNLL formatting based on
		 * {@link TalismaneSession#getCoNLLFormatter()}
		 */
		LEMMA("(.*?)"),
		/**
		 * The token's pos-tag
		 */
		POSTAG("(.+?)"),
		/**
		 * The token's morphological traits.
		 */
		MORPHOLOGY("(.*?)"),
		/**
		 * The dependency label governing this token
		 */
		NON_PROJ_LABEL("(.*?)"),
		/**
		 * The index of the token governing this token. A value of 0 indicates
		 * an invisible "root" token as a governor
		 */
		NON_PROJ_GOVERNOR("(\\d+)"),
		/**
		 * The dependency label governing this token when the full tree has been
		 * made projective.
		 */
		LABEL("(.*?)"),
		/**
		 * This index of the token governing this token when the full tree has
		 * been made projective. A value of 0 indicates an invisible "root"
		 * token as a governor.
		 */
		GOVERNOR("(\\d+)"),
		/**
		 * The file containing the original token
		 */
		FILENAME("(.*?)"),
		/**
		 * The row containing the token
		 */
		ROW("(\\d+)"),
		/**
		 * The column on which the token starts
		 */
		COLUMN("(\\d+)"),
		/**
		 * The row containing the token's end
		 */
		END_ROW("(\\d+)"),
		/**
		 * The column just after the token end
		 */
		END_COLUMN("(\\d+)"),
		/**
		 * An arbitrary comment added to the pos-tag.
		 */
		POSTAG_COMMENT("(.*?)"),
		/**
		 * An arbitrary comment added to the dependency arc.
		 */
		DEP_COMMENT("(.*?)");

		private final String replacement;

		private CorpusElement(String replacement) {
			this.replacement = replacement;
		}

		public String getReplacement() {
			return replacement;
		}
	}

	public CorpusLine(String line, int lineNumber) {
		this.line = line;
		this.lineNumber = lineNumber;
	}

	/**
	 * Get a particular element from this corpus line.
	 */
	public String getElement(CorpusElement type) {
		return this.elements.get(type);
	}

	public void setElement(CorpusElement type, String value) {
		this.elements.put(type, value);
	}

	public boolean hasElement(CorpusElement type) {
		return this.elements.containsKey(type);
	}

	/**
	 * Get the lexical entry extracted from this line's elements, including the
	 * morphology.
	 */
	public LexicalEntry getLexicalEntry() {
		return lexicalEntry;
	}

	public void setLexicalEntry(LexicalEntry lexicalEntry) {
		this.lexicalEntry = lexicalEntry;
	}

	@Override
	public String toString() {
		return "CorpusLine [line=" + line + ", lineNumber=" + lineNumber + "]";
	}

	/**
	 * The original line out of which the corpus line was extracted.
	 * 
	 * @return
	 */
	public String getLine() {
		return line;
	}

	/**
	 * The line number of the original line in the corpus.
	 */
	public int getLineNumber() {
		return lineNumber;
	}

	public int getIndex() {
		if (this.hasElement(CorpusElement.INDEX)) {
			return Integer.parseInt(this.getElement(CorpusElement.INDEX));
		}
		return -1;
	}
}
