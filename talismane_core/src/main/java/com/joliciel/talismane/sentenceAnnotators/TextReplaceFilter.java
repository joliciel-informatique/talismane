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
package com.joliciel.talismane.sentenceAnnotators;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joliciel.talismane.AnnotatedText;
import com.joliciel.talismane.Annotation;
import com.joliciel.talismane.NeedsTalismaneSession;
import com.joliciel.talismane.TalismaneSession;
import com.joliciel.talismane.tokeniser.Tokeniser;
import com.joliciel.talismane.utils.LogUtils;

/**
 * Applies a list of {@link TextReplacer} to an annotated text, where each
 * replacer is applied to the result of the previous replacer. For any replaced
 * text, will add an {@link Annotation} containing a {@link TextReplacement}.
 * <br/>
 * <br/>
 * To instantiate, create a list of tabs, each of which contains the name of a
 * registered TextReplacer. For example:<br/>
 * 
 * <pre>
 * TextReplaceFilter	QuoteNormaliser	LowercaseKnownFirstWordFilter	UppercaseSeriesFilter
 * </pre>
 * 
 * @author Assaf Urieli
 *
 */
public class TextReplaceFilter implements SentenceAnnotator {
	private static final Logger LOG = LoggerFactory.getLogger(TextReplaceFilter.class);
	private List<TextReplacer> textReplacers;
	private final Map<String, Class<? extends TextReplacer>> registeredTextReplacers;
	private final TalismaneSession session;

	public TextReplaceFilter(Map<String, Class<? extends TextReplacer>> registeredTextReplacers, TalismaneSession session) {
		this.registeredTextReplacers = registeredTextReplacers;
		this.session = session;
	}

	@Override
	public void annotate(AnnotatedText annotatedText) {
		List<String> tokens = Tokeniser.bruteForceTokenise(annotatedText.getText(), session);
		List<String> originalTokens = new ArrayList<>(tokens);
		for (TextReplacer textReplacer : textReplacers) {
			textReplacer.replace(tokens);
		}
		int currentPos = 0;
		List<Annotation<TextReplacement>> replacements = new ArrayList<>();

		for (int i = 0; i < originalTokens.size(); i++) {
			String originalToken = originalTokens.get(i);
			String token = tokens.get(i);
			if (!originalToken.equals(token)) {
				Annotation<TextReplacement> replacement = new Annotation<>(currentPos, currentPos + originalToken.length(), new TextReplacement(token));
				replacements.add(replacement);
			}
			currentPos += originalToken.length();
		}

		annotatedText.addAnnotations(replacements);
	}

	@Override
	public void load(Map<String, String> parameters, List<String> tabs) throws SentenceAnnotatorLoadException {
		try {
			textReplacers = new ArrayList<>();

			for (String className : tabs) {
				Class<? extends TextReplacer> clazz = this.registeredTextReplacers.get(className);
				if (clazz == null) {
					throw new SentenceAnnotatorLoadException("Unknown TextReplacer: " + className);
				}

				TextReplacer textReplacer = clazz.newInstance();
				if (textReplacer instanceof NeedsTalismaneSession) {
					((NeedsTalismaneSession) textReplacer).setTalismaneSession(session);
				}

				textReplacers.add(textReplacer);
			}
		} catch (InstantiationException e) {
			LogUtils.logError(LOG, e);
			throw new RuntimeException(e);
		} catch (IllegalAccessException e) {
			LogUtils.logError(LOG, e);
			throw new RuntimeException(e);
		} catch (IllegalArgumentException e) {
			LogUtils.logError(LOG, e);
			throw new RuntimeException(e);
		}
	}

	@Override
	public boolean isExcluded() {
		return false;
	}

}
