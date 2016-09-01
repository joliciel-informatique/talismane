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
package com.joliciel.talismane.tokeniser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joliciel.talismane.TalismaneSession;
import com.joliciel.talismane.filters.Sentence;
import com.joliciel.talismane.tokeniser.filters.TokenPlaceholder;

class TokenSequenceImpl extends AbstractTokenSequence implements TokenSequence {
	@SuppressWarnings("unused")
	private static final Logger LOG = LoggerFactory.getLogger(TokenSequenceImpl.class);
	private static final long serialVersionUID = 2675309892340757939L;

	private TokenSequenceImpl(TokenSequenceImpl sequenceToClone) {
		super(sequenceToClone);
	}

	public TokenSequenceImpl(Sentence sentence, TalismaneSession talismaneSession) {
		super(sentence, talismaneSession);
	}

	public TokenSequenceImpl(Sentence sentence, Pattern separatorPattern, TalismaneSession talismaneSession) {
		this(sentence, separatorPattern, null, talismaneSession);
	}

	public TokenSequenceImpl(Sentence sentence, Pattern separatorPattern, List<TokenPlaceholder> placeholders, TalismaneSession talismaneSession) {
		super(sentence, talismaneSession);
		this.initialise(sentence.getText(), separatorPattern, placeholders);
	}

	public TokenSequenceImpl(Sentence sentence, TokenisedAtomicTokenSequence tokenisedAtomicTokenSequence, TalismaneSession talismaneSession) {
		super(sentence, talismaneSession);
		this.underlyingAtomicTokenSequence = tokenisedAtomicTokenSequence;
	}

	private void initialise(String text, Pattern separatorPattern, List<TokenPlaceholder> placeholders) {
		Matcher matcher = separatorPattern.matcher(text);
		Set<Integer> separatorMatches = new HashSet<Integer>();
		while (matcher.find())
			separatorMatches.add(matcher.start());

		Map<Integer, TokenPlaceholder> placeholderMap = new HashMap<Integer, TokenPlaceholder>();
		List<TokenPlaceholder> attributePlaceholders = new ArrayList<TokenPlaceholder>();
		if (placeholders != null) {
			for (TokenPlaceholder placeholder : placeholders) {
				if (placeholder.isSingleToken()) {
					// take the first placeholder at this start index only
					// thus declaration order is the order at which they're
					// applied
					if (!placeholderMap.containsKey(placeholder.getStartIndex()))
						placeholderMap.put(placeholder.getStartIndex(), placeholder);
				} else {
					attributePlaceholders.add(placeholder);
				}
			}
		}

		int currentPos = 0;
		for (int i = 0; i < text.length(); i++) {
			if (placeholderMap.containsKey(i)) {
				if (i > currentPos)
					this.addToken(currentPos, i);
				TokenPlaceholder placeholder = placeholderMap.get(i);
				Token token = this.addToken(placeholder.getStartIndex(), placeholder.getEndIndex());
				if (placeholder.getReplacement() != null)
					token.setText(placeholder.getReplacement());

				for (String key : placeholder.getAttributes().keySet())
					token.addAttribute(key, placeholder.getAttributes().get(key));

				if (separatorPattern.matcher(token.getText()).matches())
					token.setSeparator(true);

				// skip until after the placeholder
				i = placeholder.getEndIndex() - 1;
				currentPos = placeholder.getEndIndex();
			} else if (separatorMatches.contains(i)) {
				if (i > currentPos)
					this.addToken(currentPos, i);
				Token separator = this.addToken(i, i + 1);
				separator.setSeparator(true);
				currentPos = i + 1;
			}
		}

		if (currentPos < text.length())
			this.addToken(currentPos, text.length());

		// select order in which to add attributes, when attributes exist for
		// identical keys
		Map<String, NavigableSet<TokenPlaceholder>> attributeOrderingMap = new HashMap<>();
		for (TokenPlaceholder placeholder : attributePlaceholders) {
			for (String key : placeholder.getAttributes().keySet()) {
				NavigableSet<TokenPlaceholder> placeholderSet = attributeOrderingMap.get(key);
				if (placeholderSet == null) {
					placeholderSet = new TreeSet<>();
					attributeOrderingMap.put(key, placeholderSet);
					placeholderSet.add(placeholder);
				} else {
					// only add one placeholder per location for each attribute
					// key
					TokenPlaceholder floor = placeholderSet.floor(placeholder);
					if (floor == null || floor.compareTo(placeholder) < 0) {
						placeholderSet.add(placeholder);
					}
				}
			}
		}

		// add any attributes provided by attribute placeholders
		for (String key : attributeOrderingMap.keySet()) {
			for (TokenPlaceholder placeholder : attributeOrderingMap.get(key)) {
				for (Token token : this) {
					if (token.getStartIndex() < placeholder.getStartIndex()) {
						continue;
					} else if (token.getEndIndex() <= placeholder.getEndIndex()) {
						if (token.getAttributes().containsKey(key)) {
							// this token already contains the key in question,
							// only possible when two placeholders overlap
							// in this case, the second placeholder is
							// completely skipped
							// so we break out of the token loop to avoid
							// assigning the remaining tokens
							break;
						}
						token.addAttribute(key, placeholder.getAttributes().get(key));
					} else {
						// token.getEndIndex() > placeholder.getEndIndex()
						break;
					}
				}
			}
		}

		this.finalise();
	}

	@Override
	public TokenSequence cloneTokenSequence() {
		TokenSequenceImpl tokenSequence = new TokenSequenceImpl(this);
		return tokenSequence;
	}

}
