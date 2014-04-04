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
package com.joliciel.talismane;

import java.util.Locale;

import com.joliciel.talismane.lexicon.PosTaggerLexicon;
import com.joliciel.talismane.parser.TransitionSystem;
import com.joliciel.talismane.posTagger.PosTagSet;

/**
 * A class storing session-wide reference data.
 * @author Assaf Urieli
 *
 */
public class TalismaneSession {
	private static ThreadLocal<Locale> localeHolder = new ThreadLocal<Locale>();
	private static ThreadLocal<PosTagSet> posTagSetHolder = new ThreadLocal<PosTagSet>();
	private static ThreadLocal<PosTaggerLexicon> lexiconHolder = new ThreadLocal<PosTaggerLexicon>();
	private static ThreadLocal<TransitionSystem> transitionSystemHolder = new ThreadLocal<TransitionSystem>();
	private static ThreadLocal<LanguageSpecificImplementation> implementationHolder = new ThreadLocal<LanguageSpecificImplementation>();
	private static ThreadLocal<LinguisticRules> linguisticRulesHolder = new ThreadLocal<LinguisticRules>();
	
	public static void setImplementation(LanguageSpecificImplementation implementation) {
		implementationHolder.set(implementation);
	}
	public static LanguageSpecificImplementation getImplementation() {
		LanguageSpecificImplementation implementation = implementationHolder.get();
		return implementation;
	}
	
	public static void setPosTagSet(PosTagSet posTagSet) {
		posTagSetHolder.set(posTagSet);
	}
	
	public static PosTagSet getPosTagSet() {
		PosTagSet posTagSet = posTagSetHolder.get();
		if (posTagSet==null && implementationHolder.get()!=null) {
			posTagSet = implementationHolder.get().getDefaultPosTagSet();
			TalismaneSession.setPosTagSet(posTagSet);
		}
		return posTagSet;
	}
	
	public static void setTransitionSystem(TransitionSystem transitionSystem) {
		transitionSystemHolder.set(transitionSystem);
	}
	
	public static TransitionSystem getTransitionSystem() {
		TransitionSystem transitionSystem = transitionSystemHolder.get();
		if (transitionSystem==null && implementationHolder.get()!=null) {
			transitionSystem = implementationHolder.get().getDefaultTransitionSystem();
			TalismaneSession.setTransitionSystem(transitionSystem);
		}
		return transitionSystem;
	}
	
	public static void setLexicon(PosTaggerLexicon lexicon) {
		lexiconHolder.set(lexicon);
	}
	
	public static PosTaggerLexicon getLexicon() {
		PosTaggerLexicon lexicon = lexiconHolder.get();
		if (lexicon==null && implementationHolder.get()!=null) {
			lexicon = implementationHolder.get().getDefaultLexicon();
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
	
	
	public static void setLinguisticRules(LinguisticRules rules) {
		linguisticRulesHolder.set(rules);
	}
	public static LinguisticRules getLinguisticRules() {
		LinguisticRules rules = linguisticRulesHolder.get();
		return rules;
	}
	

}
