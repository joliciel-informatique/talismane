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

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joliciel.talismane.TalismaneException;
import com.joliciel.talismane.TalismaneSession;
import com.joliciel.talismane.corpus.CorpusLine;
import com.joliciel.talismane.corpus.CorpusLine.CorpusElement;
import com.joliciel.talismane.corpus.CorpusLineReader;
import com.joliciel.talismane.lexicon.LexicalEntry;
import com.joliciel.talismane.machineLearning.Decision;
import com.joliciel.talismane.tokeniser.Token;
import com.joliciel.talismane.tokeniser.TokenRegexBasedCorpusReader;
import com.typesafe.config.Config;

/**
 * A corpus reader that expects one pos-tagged token per line, and analyses the
 * line content based on a regex supplied during construction, via a
 * {@link CorpusLineReader}.<br/>
 * 
 * The following placeholders are required:<br/>
 * {@link CorpusElement#TOKEN}, {@link CorpusElement#POSTAG}. <br/>
 * These are included surrounded by % signs on both sides, and without the
 * prefix "CorpusElement."<br/>
 * 
 * Example (note that the regex is applied to one line, so no endline is
 * necessary):
 * 
 * <pre>
 * .*\t%TOKEN%\t.*\t%POSTAG%\t.*\t.*\t
 * </pre>
 * 
 * @author Assaf Urieli
 *
 */
public class PosTagRegexBasedCorpusReader extends TokenRegexBasedCorpusReader implements PosTagAnnotatedCorpusReader {
	private static final Logger LOG = LoggerFactory.getLogger(PosTagRegexBasedCorpusReader.class);

	protected PosTagSequence posTagSequence = null;
	protected Map<Integer, PosTaggedToken> idTokenMap = new HashMap<>();

	/**
	 * Similar to
	 * {@link PosTagRegexBasedCorpusReader#PosTagRegexBasedCorpusReader(String,Reader,Config,TalismaneSession)}
	 * , but reads the regex from the setting:
	 * <ul>
	 * <li>preannotated-pattern</li>
	 * </ul>
	 * 
	 * @throws TalismaneException
	 */
	public PosTagRegexBasedCorpusReader(Reader reader, Config config, TalismaneSession session) throws IOException, TalismaneException {
		this(config.getString("preannotated-pattern"), reader, config, session);
	}

	/**
	 * @throws TalismaneException
	 * @see TokenRegexBasedCorpusReader#TokenRegexBasedCorpusReader(String,Reader,Config,TalismaneSession)
	 */
	public PosTagRegexBasedCorpusReader(String regex, Reader reader, Config config, TalismaneSession session) throws IOException, TalismaneException {
		super(regex, reader, config, session);
	}

	@Override
	protected CorpusElement[] getRequiredElements() {
		return new CorpusElement[] { CorpusElement.TOKEN, CorpusElement.POSTAG };
	}

	@Override
	protected void processSentence(List<CorpusLine> corpusLines) throws TalismaneException {
		super.processSentence(corpusLines);
		posTagSequence = new PosTagSequence(tokenSequence);
		int i = 0;
		for (CorpusLine corpusLine : corpusLines) {
			PosTaggedToken posTaggedToken = this.convertToPosTaggedToken(corpusLine, posTagSequence, i++, this.getCurrentFile());
			this.idTokenMap.put(corpusLine.getIndex(), posTaggedToken);
		}
	}

	@Override
	public PosTagSequence nextPosTagSequence() throws TalismaneException {
		PosTagSequence nextSentence = null;
		if (this.hasNextSentence()) {
			nextSentence = posTagSequence;
			this.clearSentence();
		}
		return nextSentence;
	}

	@Override
	protected void clearSentence() {
		super.clearSentence();
		this.posTagSequence = null;
		this.idTokenMap = new HashMap<>();
	}

	@Override
	public Map<String, String> getCharacteristics() {
		Map<String, String> attributes = super.getCharacteristics();
		attributes.put("tagset", session.getPosTagSet().getName());
		return attributes;
	}

	protected PosTaggedToken convertToPosTaggedToken(CorpusLine corpusLine, PosTagSequence posTagSequence, int index, File currentFile)
			throws TalismaneException {
		Token token = posTagSequence.getTokenSequence().get(index);

		PosTagSet posTagSet = session.getPosTagSet();
		PosTag posTag = null;
		try {
			posTag = posTagSet.getPosTag(corpusLine.getElement(CorpusElement.POSTAG));
		} catch (UnknownPosTagException upte) {
			String fileName = "";
			if (currentFile != null)
				fileName = currentFile.getPath();

			throw new TalismaneException(
					"Unknown posTag, " + fileName + ", on line " + corpusLine.getLineNumber() + ": " + corpusLine.getElement(CorpusElement.POSTAG));
		}
		Decision posTagDecision = new Decision(posTag.getCode());
		PosTaggedToken posTaggedToken = new PosTaggedToken(token, posTagDecision, session);
		if (LOG.isTraceEnabled()) {
			LOG.trace(posTaggedToken.toString());
		}

		if (corpusLine.hasElement(CorpusElement.POSTAG_COMMENT))
			posTaggedToken.setComment(corpusLine.getElement(CorpusElement.POSTAG_COMMENT));

		// set the lexical entry if we have one
		if (corpusLine.getLexicalEntry() != null) {
			List<LexicalEntry> lexicalEntrySet = new ArrayList<LexicalEntry>(1);
			lexicalEntrySet.add(corpusLine.getLexicalEntry());
			posTaggedToken.setLexicalEntries(lexicalEntrySet);
		}
		posTagSequence.addPosTaggedToken(posTaggedToken);
		return posTaggedToken;
	}
}
