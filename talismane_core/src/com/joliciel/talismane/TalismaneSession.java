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

import java.util.List;
import java.util.Locale;

import com.joliciel.talismane.lexicon.Diacriticizer;
import com.joliciel.talismane.lexicon.PosTaggerLexicon;
import com.joliciel.talismane.parser.TransitionSystem;
import com.joliciel.talismane.posTagger.PosTagSet;

/**
 * A class storing session-wide reference data.
 * @author Assaf Urieli
 *
 */
public interface TalismaneSession {

	public LanguageImplementation getImplementation();
	public void setImplementation(LanguageImplementation implementation);

	public PosTagSet getPosTagSet();
	public void setPosTagSet(PosTagSet posTagSet);

	public TransitionSystem getTransitionSystem();
	public void setTransitionSystem(TransitionSystem transitionSystem);

	/**
	 * A list of lexicons setup for the current session.
	 * @return
	 */
	public List<PosTaggerLexicon> getLexicons();
	public void addLexicon(PosTaggerLexicon lexicon);
	
	/**
	 * Get a lexicon which merges all of the lexicons added, prioritised in the order in which they were added.
	 * @return
	 */
	public PosTaggerLexicon getMergedLexicon();

	public Locale getLocale();
	public void setLocale(Locale locale);

	public LinguisticRules getLinguisticRules();
	public void setLinguisticRules(LinguisticRules linguisticRules);
	
	public Diacriticizer getDiacriticizer();
	public void setDiacriticizer(Diacriticizer diacriticizer);

}