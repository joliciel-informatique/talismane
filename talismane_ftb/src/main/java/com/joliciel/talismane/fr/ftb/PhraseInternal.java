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

interface PhraseInternal extends Phrase, EntityInternal {
    
    /** save this phrase to the database */
    public void save();
    
    /** Create a new child for this phrase and add it to the list of children */
    public PhraseInternal newChild();

    public void setFunction(Function function);

    public Phrase getParent();

    public void setParent(Phrase parent);

    public int getParentId();

    public void setParentId(int parentId);

    public PhraseType getPhraseType();

    public void setPhraseType(PhraseType phraseType);

    public int getPhraseTypeId();

    public void setPhraseTypeId(int phraseTypeId);

    public List<PhraseUnit> getPhraseUnits();

    public int getPositionInPhrase();

    public void setPositionInPhrase(int position);

    public List<Phrase> getChildren();

    public Function getFunction();

    public int getFunctionId();

    public void setFunctionId(int functionId);

    public void setDepth(int depth);

    public void addPhraseUnit(PhraseUnitInternal phraseUnit);

    /**
     * Save the flattened phrase-to-descendant map for this phrase - will include a map from the phrase for itself.
     */
    public List<PhraseInternal> savePhraseDescendentMapping();

	public abstract void setDirty(boolean dirty);

	public abstract boolean isDirty();

	public abstract void assignVerbSubcategories();
	
	public List<PhraseInternal> getChildrenDB();
	public List<PhraseUnitInternal> getPhraseUnitsDB();
	public List<PhraseUnit> getAllPhraseUnitsDB();
	
	public void finalisePhrase();
}
