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
package com.joliciel.frenchTreebank;

class PartOfSpeechImpl implements PartOfSpeech {
	private Category category;
	private SubCategory subCategory;
	private Morphology morphology;
	
	private String displayName = null;

	public boolean isEmpty() {
		return (category == null && subCategory == null && morphology == null);
	}
	
	public Category getCategory() {
		return category;
	}

	public void setCategory(Category category) {
		this.category = category;
	}

	public SubCategory getSubCategory() {
		return subCategory;
	}

	public void setSubCategory(SubCategory subCategory) {
		this.subCategory = subCategory;
	}

	public Morphology getMorphology() {
		return morphology;
	}

	public void setMorphology(Morphology morphology) {
		this.morphology = morphology;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null)
			return false;
		if (!(obj instanceof PartOfSpeech))
			return false;
		PartOfSpeech pos = (PartOfSpeech) obj;
		if (this.category==null && pos.getCategory()!=null)
			return false;
		if (this.category!=null && pos.getCategory()==null)
			return false;
		if (this.category!=null && pos.getCategory()!=null)
			if (this.category!=pos.getCategory())
				return false;
		
		if (this.subCategory==null && pos.getSubCategory()!=null)
			return false;
		if (this.subCategory!=null && pos.getSubCategory()==null)
			return false;
		if (this.subCategory!=null && pos.getSubCategory()!=null)
			if (this.subCategory!=pos.getSubCategory())
				return false;

		if (this.morphology==null && pos.getMorphology()!=null)
			return false;
		if (this.morphology!=null && pos.getMorphology()==null)
			return false;
		if (this.morphology!=null && pos.getMorphology()!=null)
			if (this.morphology!=pos.getMorphology())
				return false;

		return true;
	}

	@Override
	public int hashCode() {
		return this.toString().hashCode();
	}

	@Override
	public String toString() {
		if (displayName==null)
			displayName = (category==null? "" : category.getDescription()) + " " 
			+ (subCategory==null ? "" : subCategory.getDescription()) + " "
			+ (morphology==null? "" :morphology.getCode());
		return displayName;
	}

	
}
