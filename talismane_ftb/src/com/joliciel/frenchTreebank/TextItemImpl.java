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

class TextItemImpl extends EntityImpl implements TextItemInternal {
    /**
	 * 
	 */
	private static final long serialVersionUID = -3891870004620744011L;
	int fileId;
    TreebankFile file;
    String externalId;
    TreebankServiceInternal treebankServiceInternal;

    public int getFileId() {
        return fileId;
    }
    public void setFileId(int fileId) {
        this.fileId = fileId;
    }
    public TreebankFile getFile() {
        return file;
    }
    public void setFile(TreebankFile file) {
        this.file = file;
        if (this.fileId==0 && file!=null) {
        	this.setFileId(file.getId());
        } else if (file==null) {
        	this.setFileId(0);
        }
    }
    public String getExternalId() {
        return externalId;
    }
    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof TextItem) {
            return ((TextItem) obj).getId()==this.getId();
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
        this.treebankServiceInternal.saveTextItemInternal(this);
    }
}
