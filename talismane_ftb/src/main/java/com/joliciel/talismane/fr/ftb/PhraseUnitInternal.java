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

interface PhraseUnitInternal extends PhraseUnit, EntityInternal {

    public void setPositionInSentence(int position);

    public void setWord(Word word);

    public void setLemma(Word lemma);

    public void setCategory(Category category);

    public void setSubCategory(SubCategory subCategory);

    public void setMorphology(Morphology morphology);

    public void setCompound(boolean compound);

    /** save this phrase unit to the database */
    public void save();

    public void setPhrase(Phrase phrase);

    public void setPositionInPhrase(int positionInPhrase);

    public void setWordId(int wordId);

    public void setPhraseId(int phraseId);

    public void setLemmaId(int lemmaId);

    public void setCategoryId(int categoryId);

    public void setSubCategoryId(int subCategoryId);

    public void setMorphologyId(int morphologyId);

	public abstract void setSplitCompoundPrevId(String compoundPrevId);

	public abstract String getSplitCompoundPrevId();

	public abstract void setSplitCompoundNextId(String compoundNextId);

	public abstract String getSplitCompoundNextId();

	public abstract void setSplitCompoundId(String compoundId);

	public abstract String getSplitCompoundId();

	public abstract void setNextCompoundPartId(int nextCompoundPartId);

	public abstract void setNextCompoundPart(PhraseUnit nextCompoundPart);

	public abstract void setDirty(boolean dirty);

	public abstract boolean isDirty();
	public List<PhraseSubunit> getSubunitsInternal();
	public void setSubunitsInternal(List<PhraseSubunit> subunits);
	
	
	public void setPreviousCompoundPartId(int previousCompoundPartId);
	
	public void finalisePhraseUnit();

}
