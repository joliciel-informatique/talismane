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

interface PhraseSubunitInternal extends PhraseSubunit, EntityInternal {

    public void setCategory(Category category);

	public void setPhraseUnitId(int phraseUnitId);

	public void setWordId(int wordId);

    public void setCategoryId(int categoryId);

    public void setSubCategoryId(int subCategoryId);

    public void setMorphologyId(int morphologyId);

	public abstract void setDirty(boolean dirty);

	public abstract boolean isDirty();

	public void finalisePhraseSubunit();
}
