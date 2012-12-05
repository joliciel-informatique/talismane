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
package com.joliciel.talismane;

import java.util.Locale;

import com.joliciel.talismane.parser.TransitionSystem;
import com.joliciel.talismane.posTagger.PosTagSet;
import com.joliciel.talismane.posTagger.PosTaggerLexicon;

/**
 * A class storing session-wide reference data.
 * @author Assaf Urieli
 *
 */
public class TalismaneSession {
	private static LanguageSpecificImplementation implementation = null;
	
	private static ThreadLocal<Locale> localeHolder = new ThreadLocal<Locale>();
	private static ThreadLocal<PosTagSet> posTagSetHolder = new ThreadLocal<PosTagSet>();
	private static ThreadLocal<PosTaggerLexicon> lexiconHolder = new ThreadLocal<PosTaggerLexicon>();
	private static ThreadLocal<TransitionSystem> transitionSystemHolder = new ThreadLocal<TransitionSystem>();
	
	public static void setImplementation(LanguageSpecificImplementation implementation) {
		TalismaneSession.implementation = implementation;
	}

	public static void setPosTagSet(PosTagSet posTagSet) {
		posTagSetHolder.set(posTagSet);
	}
	
	public static PosTagSet getPosTagSet() {
		PosTagSet posTagSet = posTagSetHolder.get();
		if (posTagSet==null && implementation!=null) {
			posTagSet = implementation.getDefaultPosTagSet();
			TalismaneSession.setPosTagSet(posTagSet);
		}
		return posTagSet;
	}
	
	public static void setTransitionSystem(TransitionSystem transitionSystem) {
		transitionSystemHolder.set(transitionSystem);
	}
	
	public static TransitionSystem getTransitionSystem() {
		TransitionSystem transitionSystem = transitionSystemHolder.get();
		if (transitionSystem==null && implementation!=null) {
			transitionSystem = implementation.getDefaultTransitionSystem();
			TalismaneSession.setTransitionSystem(transitionSystem);
		}
		return transitionSystem;
	}
	
	public static void setLexicon(PosTaggerLexicon lexicon) {
		lexiconHolder.set(lexicon);
	}
	
	public static PosTaggerLexicon getLexicon() {
		PosTaggerLexicon lexicon = lexiconHolder.get();
		if (lexicon==null && implementation!=null) {
			lexicon = implementation.getDefaultLexiconService();
			TalismaneSession.setLexicon(lexicon);
		}
		return lexicon;
	}
	
	public static Locale getLocale() {
		PosTagSet posTagSet = TalismaneSession.getPosTagSet();
		if (posTagSet!=null)
			return posTagSet.getLocale();
		return localeHolder.get();
	}
	
	public static void setLocale(Locale locale) {
		localeHolder.set(locale);
	}
}
