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
package com.joliciel.talismane.fr.ftb;

/**
 * An element of a containing phrase: either a word or a sub-phrase.
 * @author Assaf Urieli
 */
public interface PhraseElement extends Entity {
    /** position of this element within it's parent phrase, with respect to all other elements (words or sub-phrases) at the same level */
    public int getPositionInPhrase();
    
    /**
     * Is this element a phrase or a word?
     * @return
     */
    public boolean isPhrase();

}
