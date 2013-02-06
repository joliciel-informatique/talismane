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
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.joliciel.talismane.TalismaneException;

import com.joliciel.talismane.utils.LogUtils;

class FilterServiceImpl implements FilterServiceInternal {
	private static final Log LOG = LogFactory.getLog(FilterServiceImpl.class);

	@Override
	public TextMarker getTextMarker(TextMarkerType type, int position) {
		return this.getTextMarker(type, position, null, null);
	}

	@Override
	public TextMarker getTextMarker(TextMarkerType type, int position,
			TextMarkerFilter source) {
		return this.getTextMarker(type, position, source, null);
	}


	@Override
	public TextMarker getTextMarker(TextMarkerType type, int position,
			TextMarkerFilter source, String matchText) {
		TextMarkerImpl textMarker = new TextMarkerImpl(type, position);
		textMarker.setSource(source);
		textMarker.setMatchText(matchText);
		return textMarker;
	}




	public Sentence getSentence(String text) {
		SentenceImpl sentence = new SentenceImpl();
		sentence.setText(text);
		return sentence;
	}
	
	@Override
	public Sentence getSentence() {
		SentenceImpl sentence = new SentenceImpl();
		return sentence;
	}


	@Override
	public SentenceHolder getSentenceHolder() {
		SentenceHolderImpl sentenceHolder = new SentenceHolderImpl();
		sentenceHolder.setFilterService(this);
		return sentenceHolder;
	}


	@Override
	public RollingSentenceProcessor getRollingSentenceProcessor(String fileName, boolean processByDefault) {
		RollingSentenceProcessorImpl processor = new RollingSentenceProcessorImpl(fileName, processByDefault);
		processor.setFilterService(this);
		return processor;
	}


	@Override
	public TextMarkerFilter getRegexMarkerFilter(List<MarkerFilterType> filterTypes,
			String regex, int blockSize) {
		return this.getRegexMarkerFilter(filterTypes, regex, 0, blockSize);
	}

	@Override
	public TextMarkerFilter getRegexMarkerFilter(List<MarkerFilterType> filterTypes,
			String regex, int groupIndex, int blockSize) {
		RegexMarkerFilter filter = new RegexMarkerFilter(filterTypes, regex, groupIndex);
		filter.setFilterService(this);
		filter.setBlockSize(blockSize);
		return filter;

	}

	@Override
	public TextMarkerFilter getRegexMarkerFilter(MarkerFilterType[] types,
			String regex, int blockSize) {
		return this.getRegexMarkerFilter(types, regex, 0, blockSize);
	}


	@Override
	public TextMarkerFilter getRegexMarkerFilter(MarkerFilterType[] types,
			String regex, int groupIndex, int blockSize) {
		RegexMarkerFilter filter = new RegexMarkerFilter(types, regex, groupIndex);
		filter.setFilterService(this);
		filter.setBlockSize(blockSize);
		return filter;
	}

	@Override
	public TextMarkerFilter getDuplicateWhiteSpaceFilter() {
		DuplicateWhiteSpaceFilter filter = new DuplicateWhiteSpaceFilter();
		filter.setFilterService(this);
		return filter;
	}


	@Override
	public TextMarkerFilter getNewlineEndOfSentenceMarker() {
		NewlineEndOfSentenceMarker filter = new NewlineEndOfSentenceMarker();
		filter.setFilterService(this);
		return filter;
	}


	@Override
	public TextMarkerFilter getNewlineSpaceMarker() {
		NewlineSpaceMarker filter = new NewlineSpaceMarker();
		filter.setFilterService(this);
		return filter;
	}


	@Override
	public TextMarkerFilter getTextMarkerFilter(String descriptor, int blockSize) {
		TextMarkerFilter filter = null;
		
		List<Class<? extends TextMarkerFilter>> classes = new ArrayList<Class<? extends TextMarkerFilter>>();
		classes.add(DuplicateWhiteSpaceFilter.class);
		classes.add(NewlineEndOfSentenceMarker.class);
		classes.add(NewlineSpaceMarker.class);
		
		String[] parts = descriptor.split("\t");
		String filterName = parts[0];
		
		if (filterName.equals(RegexMarkerFilter.class.getSimpleName())) {
			String[] filterTypeStrings = parts[1].split(",");
			List<MarkerFilterType> filterTypes = new ArrayList<MarkerFilterType>();
			for (String filterTypeString : filterTypeStrings) {
				filterTypes.add(MarkerFilterType.valueOf(filterTypeString));
			}
			boolean needsReplacement = false;
			int minParams = 3;
			if (filterTypes.contains(MarkerFilterType.REPLACE)) {
				needsReplacement = true;
				minParams = 4;
			}
			if (parts.length==minParams+1){
				filter = this.getRegexMarkerFilter(filterTypes, parts[2], Integer.parseInt(parts[3]), blockSize);
				if (needsReplacement)
					filter.setReplacement(parts[4]);
			} else if (parts.length==minParams) {
				filter = this.getRegexMarkerFilter(filterTypes, parts[2], blockSize);
				if (needsReplacement)
					filter.setReplacement(parts[3]);
			} else {
				throw new TalismaneException("Wrong number of arguments for " + RegexMarkerFilter.class.getSimpleName() + ". Expected " + minParams + " or " + (minParams+1) + ", but was " + parts.length);
			}
		} else {
			for (Class<? extends TextMarkerFilter> clazz : classes) {
				if (filterName.equals(clazz.getSimpleName())) {
					try {
						filter = clazz.newInstance();
					} catch (InstantiationException e) {
						LogUtils.logError(LOG, e);
						throw new RuntimeException(e);
					} catch (IllegalAccessException e) {
						LogUtils.logError(LOG, e);
						throw new RuntimeException(e);
					}
				}
			}
			filter.setBlockSize(blockSize);
		}
		
		if (filter==null) {
			throw new TalismaneException("Unknown TextMarkerFilter: " + descriptor);
		}
		
		return filter;
	}

	
}
