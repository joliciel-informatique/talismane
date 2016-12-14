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
package com.joliciel.talismane.sentenceAnnotators;

/**
 * Thrown when an error occurs on token feature loading.
 * 
 * @author Assaf Urieli
 *
 */
public class SentenceAnnotatorLoadException extends Exception {
	private static final long serialVersionUID = 1L;

	public SentenceAnnotatorLoadException() {
		super();
	}

	public SentenceAnnotatorLoadException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public SentenceAnnotatorLoadException(String message, Throwable cause) {
		super(message, cause);
	}

	public SentenceAnnotatorLoadException(String message) {
		super(message);
	}

	public SentenceAnnotatorLoadException(Throwable cause) {
		super(cause);
	}

}
