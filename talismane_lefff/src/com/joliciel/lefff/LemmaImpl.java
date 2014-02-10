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
package com.joliciel.lefff;


class LemmaImpl extends EntityImpl implements LemmaInternal {
	/**
	 * 
	 */
	private static final long serialVersionUID = -7209324900442624710L;
	private String text = "";
	private int index = 0;
	private String complement = "";
	
    transient private LefffServiceInternal lefffServiceInternal;

	@Override
	public void saveInternal() {
		this.lefffServiceInternal.saveLemma(this);
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}

	public int getIndex() {
		return index;
	}

	public void setIndex(int index) {
		this.index = index;
	}

	public String getComplement() {
		return complement;
	}

	public void setComplement(String complement) {
		this.complement = complement;
	}

	public LefffServiceInternal getLefffServiceInternal() {
		return lefffServiceInternal;
	}

	public void setLefffServiceInternal(LefffServiceInternal lefffServiceInternal) {
		this.lefffServiceInternal = lefffServiceInternal;
	}

}
