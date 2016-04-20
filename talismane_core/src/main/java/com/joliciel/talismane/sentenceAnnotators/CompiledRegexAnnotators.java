///////////////////////////////////////////////////////////////////////////////
//Copyright (C) 2016 Safety Data -CFH
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

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joliciel.talismane.Annotation;
import com.joliciel.talismane.rawText.Sentence;
import com.joliciel.talismane.regex.Flags;
import com.joliciel.talismane.regex.Match;
import com.joliciel.talismane.regex.Matcher;
import com.joliciel.talismane.regex.bytecode.CompiledRegex;
import com.joliciel.talismane.regex.compiler.Compiler;
import com.joliciel.talismane.resources.WordListFinder;
import com.joliciel.talismane.tokeniser.TokenAttribute;
import com.joliciel.talismane.utils.RegexUtils;

import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;

/**
 * Represents a set of compiled {@link AbstractRegexAnnotator}. The filters are
 * applied in parallel but match in declaration order. Hence, if two regular
 * expressions match at the same index in the input, the first one wins in case
 * of {@link RegexAnnotator#isSingleToken()} is <code>true</code>.
 *
 * The advantage of using compiled token filters over textual resources is that
 * the input is read only once no matter how many filters are declared. The
 * compiled filters are obtained from the textual resources and behave the same.
 * They cannot be modified easily but are more efficient. During resource design
 * phase, you can use the usual textual resources. Once they are complete,
 * compile them to improve analysis performances.
 * 
 * @author Lucas Satabin
 *
 */
public class CompiledRegexAnnotators implements SentenceAnnotator {

	private static final Logger LOG = LoggerFactory.getLogger(CompiledRegexAnnotators.class);

	final CompiledRegex regex;
	final int[] indices;
	final String[] replacements;
	final boolean[] singleTokens;
	final Map<String, TokenAttribute<?>>[] attributes;

	public CompiledRegexAnnotators(CompiledRegex regex, int[] indices, String[] replacements, boolean[] singleTokens,
			Map<String, TokenAttribute<?>>[] attributes) {
		this.regex = regex;
		this.indices = indices;
		this.replacements = replacements;
		this.singleTokens = singleTokens;
		this.attributes = attributes;
	}

	public CompiledRegexAnnotators(List<SentenceAnnotator> annotators, WordListFinder wordListFinder) {
		CompiledRegex regex = null;
		TIntList indices = new TIntArrayList(annotators.size());
		List<Boolean> singleTokens = new ArrayList<>(annotators.size());
		List<String> replacements = new ArrayList<>(annotators.size());
		List<Map<String, TokenAttribute<?>>> attributes = new ArrayList<>(annotators.size());
		for (SentenceAnnotator annotator : annotators) {
			if (annotator instanceof RegexAnnotator) {
				RegexAnnotator regexAnnotator = (RegexAnnotator) annotator;
				LOG.debug("Compiling filter: " + regexAnnotator.getRegex());

				// compile the filter regular expression
				int flags = 0;
				if (regexAnnotator.isAutoWordBoundaries()) {
					flags |= Flags.AUTO_WORD_BOUNDARIES;
				}
				if (!regexAnnotator.isCaseSensitive()) {
					flags |= Flags.CASE_INSENSITIVE;
				}
				if (!regexAnnotator.isDiacriticSensitive()) {
					flags |= Flags.DIACRITICS_INSENSITIVE;
				}
				Compiler compiler = new Compiler(flags, wordListFinder);
				final int savedBase;
				if (regex == null) {
					savedBase = 0;
					regex = compiler.compile(regexAnnotator.getRegex(), indices.size());
				} else {
					savedBase = regex.nbSaved;
					regex = regex.or(compiler.compile(regexAnnotator.getRegex(), indices.size()));
				}

				// add the group index for this filter
				indices.add(regexAnnotator.getGroupIndex() + savedBase);

				// add the single token flag for this filter
				singleTokens.add(regexAnnotator.isSingleToken());

				// retrieve the replacements for this filter
				if (regexAnnotator instanceof RegexTokenAnnotator) {
					replacements.add(((RegexTokenAnnotator) regexAnnotator).getReplacement());
				} else {
					replacements.add(null);
				}

				// extract the attributes
				attributes.add(regexAnnotator.getAttributes());
			}
		}
		this.regex = regex;
		this.indices = indices.toArray();
		this.replacements = replacements.toArray(new String[0]);
		// it is not possible to get a primitive boolean array out of a generic
		// list in Java using the toArray() method
		this.singleTokens = new boolean[singleTokens.size()];
		for (int i = 0; i < singleTokens.size(); i++) {
			this.singleTokens[i] = singleTokens.get(i);
		}
		@SuppressWarnings("unchecked")
		Map<String, TokenAttribute<?>>[] a = (Map<String, TokenAttribute<?>>[]) Array.newInstance(Map.class, 0);
		this.attributes = attributes.toArray(a);
	}

	@Override
	public void annotate(Sentence annotatedText, String... labels) {
		List<Annotation<TokenPlaceholder>> placeholders = new ArrayList<>();
		List<Annotation<TokenAttribute<?>>> annotations = new ArrayList<>();

		Matcher matcher = new Matcher(annotatedText.getText(), regex);
		int lastStart = -1;
		for (Match match : matcher) {
			int id = match.getId();
			Map<String, TokenAttribute<?>> attributes = this.attributes[id];
			int groupIndex = indices[id];
			int start = match.start(groupIndex);
			if (start > lastStart) {
				int end = match.end(groupIndex);

				if (LOG.isTraceEnabled()) {
					LOG.trace(
							"Next match: " + annotatedText.getText().subSequence(match.start(), match.end()).toString().replace('\n', '¶').replace('\r', '¶'));
					if (match.start() != start || match.end() != end) {
						LOG.trace("But matching group: " + annotatedText.getText().subSequence(start, end).toString().replace('\n', '¶').replace('\r', '¶'));
					}
				}

				if (singleTokens[id]) {
					String replacement = this.findReplacement(id, annotatedText.getText(), match);
					TokenPlaceholder placeholder = new TokenPlaceholder(replacement);
					Annotation<TokenPlaceholder> placeholderAnnotation = new Annotation<>(start, end, placeholder, labels);
					placeholders.add(placeholderAnnotation);

					if (LOG.isTraceEnabled())
						LOG.trace("Added placeholder: " + placeholderAnnotation.toString());
				}

				for (String key : attributes.keySet()) {
					TokenAttribute<?> attribute = attributes.get(key);
					Annotation<TokenAttribute<?>> annotation = new Annotation<>(start, end, attribute, labels);
					annotations.add(annotation);
					if (LOG.isTraceEnabled())
						LOG.trace("Added attribute: " + annotation.toString());
				}
			}
			lastStart = start;

		}

		annotatedText.addAnnotations(placeholders);
		annotatedText.addAnnotations(annotations);
	}

	private String findReplacement(int index, CharSequence text, Match matcher) {
		String newText = RegexUtils.getReplacement(replacements[index], text, matcher);
		return newText;
	}

	@Override
	public boolean isExcluded() {
		return false;
	}

}
