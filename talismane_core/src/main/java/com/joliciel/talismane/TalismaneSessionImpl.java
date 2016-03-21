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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.joliciel.talismane.lexicon.Diacriticizer;
import com.joliciel.talismane.lexicon.EmptyLexicon;
import com.joliciel.talismane.lexicon.LexiconChain;
import com.joliciel.talismane.lexicon.LexiconService;
import com.joliciel.talismane.lexicon.PosTaggerLexicon;
import com.joliciel.talismane.parser.TransitionSystem;
import com.joliciel.talismane.posTagger.PosTagSet;


class TalismaneSessionImpl implements TalismaneSession {
	private Locale locale;
	private PosTagSet posTagSet;
	private List<PosTaggerLexicon> lexicons = new ArrayList<PosTaggerLexicon>();
	private PosTaggerLexicon mergedLexicon;
	private TransitionSystem transitionSystem;
	private LanguageImplementation implementation;
	private LinguisticRules linguisticRules;
	private Diacriticizer diacriticizer;
	private LexiconService lexiconService;
	
	@Override
	public LanguageImplementation getImplementation() {
		return implementation;
	}

	@Override
	public void setImplementation(LanguageImplementation implementation) {
		this.implementation = implementation;
	}

	@Override
	public synchronized PosTagSet getPosTagSet() {
		if (posTagSet==null && implementation!=null) {
			posTagSet = implementation.getDefaultPosTagSet();
			this.setPosTagSet(posTagSet);
		}
		if (posTagSet==null)
			throw new TalismaneException("PosTagSet missing.");
		return posTagSet;
	}

	@Override
	public void setPosTagSet(PosTagSet posTagSet) {
		this.posTagSet = posTagSet;
	}

	
	@Override
	public synchronized TransitionSystem getTransitionSystem() {
		if (transitionSystem==null && implementation!=null) {
			transitionSystem = implementation.getDefaultTransitionSystem();
			this.setTransitionSystem(transitionSystem);
		}
		if (transitionSystem==null)
			throw new TalismaneException("TransitionSystem missing.");
		return transitionSystem;
	}

	@Override
	public void setTransitionSystem(TransitionSystem transitionSystem) {
		this.transitionSystem = transitionSystem;
	}
	
	@Override
	public synchronized List<PosTaggerLexicon> getLexicons() {
		if (lexicons.size()==0 && implementation!=null) {
			List<PosTaggerLexicon> defaultLexicons = implementation.getDefaultLexicons();
			if (defaultLexicons!=null) {
				for (PosTaggerLexicon lexicon : defaultLexicons)
					this.addLexicon(lexicon);
			}
		}
		
		return lexicons;
	}

	@Override
	public void addLexicon(PosTaggerLexicon lexicon) {
		this.lexicons.add(lexicon);
	}
	
	@Override
	public Locale getLocale() {
		if (locale==null && this.getPosTagSet()!=null) {
			locale = this.getPosTagSet().getLocale();
		}
		return locale;
	}
	
	@Override
	public void setLocale(Locale locale) {
		this.locale = locale;
	}

	@Override
	public synchronized LinguisticRules getLinguisticRules() {
		if (linguisticRules==null && implementation!=null) {
			linguisticRules = implementation.getDefaultLinguisticRules();
		}
		return linguisticRules;
	}

	@Override
	public void setLinguisticRules(LinguisticRules linguisticRules) {
		this.linguisticRules = linguisticRules;
	}

	@Override
	public synchronized PosTaggerLexicon getMergedLexicon() {
		if (mergedLexicon==null) {
			List<PosTaggerLexicon> lexicons = this.getLexicons();
			if (lexicons.size()==0)
				mergedLexicon = new EmptyLexicon();
			else if (lexicons.size()==1)
				mergedLexicon = lexicons.get(0);
			else {
				LexiconChain lexiconChain = new LexiconChain();
				for (PosTaggerLexicon lexicon : lexicons) {
					lexiconChain.addLexicon(lexicon);
				}
				mergedLexicon = lexiconChain;
			}
		}
		return mergedLexicon;
	}

	public Diacriticizer getDiacriticizer() {
		if (diacriticizer==null) {
			if (implementation!=null)
				diacriticizer = implementation.getDiacriticizer();
			if (diacriticizer==null)
				diacriticizer = lexiconService.getDiacriticizer(this, this.getMergedLexicon());
		}
		return diacriticizer;
	}

	public void setDiacriticizer(Diacriticizer diacriticizer) {
		this.diacriticizer = diacriticizer;
	}

	public LexiconService getLexiconService() {
		return lexiconService;
	}

	public void setLexiconService(LexiconService lexiconService) {
		this.lexiconService = lexiconService;
	}
	
}
