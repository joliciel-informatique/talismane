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

import java.util.List;

/**
 * a phrase within a sentence, either top-level (no parent) or lower level (has parent)
 * @author Assaf Urieli
 */
public interface Phrase extends PhraseElement, Entity {
     /** phrase type */
    public PhraseType getPhraseType();
    
    /** parent phrase or null if top-level */
    public Phrase getParent();
    
    /** List of phrase units contained directly in this phrase */
    public List<PhraseUnit> getPhraseUnits();
    
    /** List of all phrase units contained in this phrase, either directly or indirectly */
    public List<PhraseUnit> getAllPhraseUnits();
    
    /** Direct children of this phrase */
    public List<Phrase> getChildren();

    /** Function of this phrase */
    public Function getFunction();

    /** id of this phrase's function */
    public int getFunctionId();

    public int getDepth();
    
    public String getText();

    /**
     * Return all elements of this phrase, whether sub-phrases or words.
     * @return
     */
    public List<PhraseElement> getElements();
}
