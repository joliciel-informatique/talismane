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

class PhraseTypeImpl extends EntityImpl implements PhraseTypeInternal {
    /**
	 * 
	 */
	private static final long serialVersionUID = -6701330948042421882L;
	String code = "";
    String description = "";
    TreebankServiceInternal treebankServiceInternal;

    public String getCode() {
        return code;
    }
    public void setCode(String code) {
        this.code = code;
    }
    public String getDescription() {
        return description;
    }
    public void setDescription(String description) {
        this.description = description;
    }
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof PhraseType) {
            return ((PhraseType) obj).getId()==this.getId();
        }
        return false;
    }
    @Override
    public int hashCode() {
        if (this.getId()!=0)
            return this.getId();
        else
            return super.hashCode();
    }
    public TreebankServiceInternal getTreebankServiceInternal() {
        return treebankServiceInternal;
    }
    public void setTreebankServiceInternal(
            TreebankServiceInternal treebankServiceInternal) {
        this.treebankServiceInternal = treebankServiceInternal;
    }
    @Override
    public void saveInternal() {
        this.treebankServiceInternal.savePhraseTypeInternal(this);
    }
}
