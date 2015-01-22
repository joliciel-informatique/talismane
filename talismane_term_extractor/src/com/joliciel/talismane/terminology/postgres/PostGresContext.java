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

import com.joliciel.talismane.terminology.Context;
import com.joliciel.talismane.terminology.TerminologyBase;


interface PostGresContext extends Context {
	public void setId(int id);
	public int getId();
	public void setColumnNumber(int columnNumber);
	public void setLineNumber(int lineNumber);
	public void setFileName(String fileName);
	public int getFileId();
	public void setFileId(int fileId);
	public int getTermId();
	public void setTermId(int termId);
	
	public boolean isDirty();
	public void setDirty(boolean dirty);

	public void setTerminologyBase(TerminologyBase terminologyBase);
	public TerminologyBase getTerminologyBase();
}
