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

class SubCategoryImpl extends EntityImpl implements SubCategoryInternal {
    /**
	 * 
	 */
	private static final long serialVersionUID = -1588822436580784653L;
	int categoryId;
    Category category;
    String code;
    String description;
    TreebankServiceInternal treebankServiceInternal;

    public int getCategoryId() {
        return categoryId;
    }
    public void setCategoryId(int categoryId) {
        this.categoryId = categoryId;
    }
    public Category getCategory() {
        return category;
    }
    public void setCategory(Category category) {
        if (this.category==null||!this.category.equals(category)) {
            this.category = category;
            this.setCategoryId(category.getId());
        }
    }
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
        if (obj instanceof SubCategory) {
            return ((SubCategory) obj).getId()==this.getId();
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
        this.treebankServiceInternal.saveSubCategoryInternal(this);
    }
}
