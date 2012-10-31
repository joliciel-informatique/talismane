///////////////////////////////////////////////////////////////////////////////
//Copyright (C) 2012 Assaf Urieli
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
package com.joliciel.talismane.filters;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class SentenceHolderImpl extends SentenceImpl implements SentenceHolder {
	private TreeSet<Integer> sentenceBoundaries = new TreeSet<Integer>();
	private Pattern duplicateWhiteSpacePattern = Pattern.compile("[ \t\\x0B\f]{2,}");
	private Pattern openingWhiteSpacePattern = Pattern.compile("\\A(\\s+)");
	private Pattern closingWhiteSpacePattern = Pattern.compile("(\\s+)\\z");
	
	private FilterService filterService;
	
	public Set<Integer> getSentenceBoundaries() {
		return sentenceBoundaries;
	}
	
	public void addSentenceBoundary(int boundary) {
		this.sentenceBoundaries.add(boundary);
	}

	@Override
	public List<Sentence> getDetectedSentences(Sentence leftover) {
		List<Sentence> sentences = new ArrayList<Sentence>();
		
		int currentIndex = 0;
		boolean haveLeftOvers = this.getText().length()>0;
		if (this.sentenceBoundaries.size()>0) {
			haveLeftOvers = false;
			int lastSentenceBoundary = this.sentenceBoundaries.descendingIterator().next();
			if (lastSentenceBoundary<this.getText().length()-1) {
				haveLeftOvers = true;
			}
		}
		List<Integer> allBoundaries = new ArrayList<Integer>(this.sentenceBoundaries);
		if (haveLeftOvers)
			allBoundaries.add(this.getText().length()-1);
		
		for (int sentenceBoundary : allBoundaries) {
			boolean isLeftover = haveLeftOvers && sentenceBoundary==this.getText().length()-1;
			
			Sentence sentence = filterService.getSentence();
			int leftOverTextLength = 0;
			String text = "";
			if (leftover!=null) {
				sentence = leftover;
				leftOverTextLength = leftover.getText().length();
				text = leftover.getText() + this.getText().substring(currentIndex, sentenceBoundary+1);
				leftover = null;
			} else {
				text = this.getText().substring(currentIndex, sentenceBoundary+1);
			}
			
			// handle trim & duplicate white space here
			Matcher matcherOpeningWhiteSpace = openingWhiteSpacePattern.matcher(text);
			int openingWhiteSpaceEnd = 0;
			if (matcherOpeningWhiteSpace.find()) {
				openingWhiteSpaceEnd = matcherOpeningWhiteSpace.end(1);
			}
			
			int closingWhiteSpaceStart = text.length();
			if (!isLeftover) {
				Matcher matcherClosingWhiteSpace = closingWhiteSpacePattern.matcher(text);
				if (matcherClosingWhiteSpace.find()) {
					closingWhiteSpaceStart = matcherClosingWhiteSpace.start(1);
				}
			}
			
			Matcher matcherDuplicateWhiteSpace = duplicateWhiteSpacePattern.matcher(text);
			Set<Integer> duplicateWhiteSpace = new HashSet<Integer>();
			while (matcherDuplicateWhiteSpace.find()) {
				// remove all white space barring the first
				for (int i = matcherDuplicateWhiteSpace.start()+1; i<matcherDuplicateWhiteSpace.end(); i++) {
					duplicateWhiteSpace.add(i);
				}
			}
			
			StringBuilder sb = new StringBuilder();
			int i = currentIndex;
			for (int j=0; j<text.length(); j++) {
				boolean appendLetter = false;
				if (j<openingWhiteSpaceEnd) {
					// do nothing
				} else if (j>=closingWhiteSpaceStart) {
					// do nothing
				} else if (duplicateWhiteSpace.contains(j)) {
					// do nothing
				} else {
					appendLetter = true;
				}
				
				if (j>=leftOverTextLength) {
					// if we're past the leftovers and onto the new stuff
					if (appendLetter)
						sentence.addOriginalIndex(this.getOriginalIndexes().get(i));
					
					if (this.getOriginalTextSegments().containsKey(i))
						sentence.getOriginalTextSegments().put(sb.length(), this.getOriginalTextSegments().get(i));
	
					i++;
				}
				
				if (appendLetter)
					sb.append(text.charAt(j));
			}
			sentence.setText(sb.toString());
			
			sentence.setComplete(!isLeftover);
			
			for (Entry<Integer,Integer> newlineLocation : this.newlines.entrySet()) {
				sentence.addNewline(newlineLocation.getKey(), newlineLocation.getValue());
			}
			
			sentence.setFileName(this.getFileName());
			
			sentences.add(sentence);
			currentIndex = sentenceBoundary + 1;
		}
		
		return sentences;
	}

	public FilterService getFilterService() {
		return filterService;
	}

	public void setFilterService(FilterService filterService) {
		this.filterService = filterService;
	}

}
