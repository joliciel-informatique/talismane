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

import com.joliciel.talismane.posTagger.PosTagSet;
import com.joliciel.talismane.posTagger.PosTaggerLexiconService;

/**
 * A class storing session-wide reference data.
 * @author Assaf Urieli
 *
 */
public class TalismaneSession {
	private static ThreadLocal<Locale> localeHolder = new ThreadLocal<Locale>();
	private static ThreadLocal<PosTagSet> posTagSetHolder = new ThreadLocal<PosTagSet>();
	private static ThreadLocal<PosTaggerLexiconService> lexiconServiceHolder = new ThreadLocal<PosTaggerLexiconService>();
	
	public static void setPosTagSet(PosTagSet posTagSet) {
		posTagSetHolder.set(posTagSet);
	}
	
	public static PosTagSet getPosTagSet() {
		return posTagSetHolder.get();
	}
	
	public static void setLexiconService(PosTaggerLexiconService lexiconService) {
		lexiconServiceHolder.set(lexiconService);
	}
	
	public static PosTaggerLexiconService getLexiconService() {
		return lexiconServiceHolder.get();
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
