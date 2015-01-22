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
package com.joliciel.talismane.terminology.postgres;

import java.io.Serializable;

import com.joliciel.talismane.terminology.Term;
import com.joliciel.talismane.terminology.TerminologyBase;

public class PostGresContextImpl implements PostGresContext, Serializable {
	private static final long serialVersionUID = -3445559171526018029L;
	private int id;
	private int termId;
	private String fileName = "";
	private int fileId;
	private int lineNumber;
	private int columnNumber;
	private int endLineNumber;
	private int endColumnNumber;
	private String textSegment = "";
	private Term term;
	private boolean dirty = true;
	private TerminologyBase terminologyBase;

	protected PostGresContextImpl() {
		// for frameworks & DAO
	}
	
	@Override
	public String getFileName() {
		return fileName;
	}
	
	@Override
	public void setFileName(String fileName) {
		if (!this.fileName.equals(fileName)) {
			this.fileName = fileName;
			this.dirty = true;
		}
	}
	
	@Override
	public int getLineNumber() {
		return lineNumber;
	}
	
	@Override
	public void setLineNumber(int lineNumber) {
		if (this.lineNumber!=lineNumber) {
			this.lineNumber = lineNumber;
			this.dirty = true;
		}
	}
	
	@Override
	public int getColumnNumber() {
		return columnNumber;
	}
	
	@Override
	public void setColumnNumber(int columnNumber) {
		if (this.columnNumber!=columnNumber) {
			this.columnNumber = columnNumber;
			this.dirty = true;
		}
	}

	public String getTextSegment() {
		return textSegment;
	}
	
	public void setTextSegment(String textSegment) {
		this.textSegment = textSegment;
	}
	
	@Override
	public String toString() {
		return "Context [fileName=" + fileName + ", lineNumber="
				+ lineNumber + ", columnNumber=" + columnNumber + "]";
	}
	
	@Override
	public int getId() {
		return id;
	}
	
	@Override
	public void setId(int id) {
		this.id = id;
	}

	@Override
	public boolean isNew() {
		return this.id == 0;
	}

	public Term getTerm() {
		return term;
	}

	public void setTerm(Term term) {
		if (this.term==null || !this.term.equals(term)) {
			this.term = term;
			this.termId = ((PostGresTerm) term).getId();
			this.dirty = true;
		}
	}

	public boolean isDirty() {
		return dirty;
	}

	public void setDirty(boolean dirty) {
		this.dirty = dirty;
	}

	public int getEndLineNumber() {
		return endLineNumber;
	}

	@Override
	public void setEndLineNumber(int endLineNumber) {
		if (this.endLineNumber!=endLineNumber) {
			this.endLineNumber = endLineNumber;
			this.dirty = true;
		}
	}

	public int getEndColumnNumber() {
		return endColumnNumber;
	}

	@Override
	public void setEndColumnNumber(int endColumnNumber) {
		if (this.endColumnNumber!=endColumnNumber) {
			this.endColumnNumber = endColumnNumber;
			this.dirty = true;
		}
	}

	public int getFileId() {
		return fileId;
	}

	public void setFileId(int fileId) {
		if (this.fileId!=fileId) {
			this.fileId = fileId;
			this.dirty = true;
		}
	}

	public int getTermId() {
		return termId;
	}

	public void setTermId(int termId) {
		if (this.termId!=termId) {
			this.termId = termId;
			this.dirty = true;
		}
	}

	public TerminologyBase getTerminologyBase() {
		return terminologyBase;
	}

	public void setTerminologyBase(TerminologyBase terminologyBase) {
		this.terminologyBase = terminologyBase;
	}
	

	@Override
	public void save() {
		this.terminologyBase.storeContext(this);
	}
}
