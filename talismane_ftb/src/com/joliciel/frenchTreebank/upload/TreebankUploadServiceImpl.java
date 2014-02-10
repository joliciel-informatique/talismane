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
package com.joliciel.frenchTreebank.upload;

import java.io.File;

import com.joliciel.frenchTreebank.TreebankReader;
import com.joliciel.frenchTreebank.TreebankService;

class TreebankUploadServiceImpl implements TreebankUploadService {
	private TreebankService treebankService;
	
	@Override
	public TreebankReader getXmlReader(File file) {
		TreebankXmlReader treebankXmlReader = new TreebankXmlReader(file);
		treebankXmlReader.setTreebankService(this.getTreebankService());
		return treebankXmlReader;
	}

	@Override
	public TreebankReader getXmlReader(File file, String sentenceNumber) {
		TreebankXmlReader treebankXmlReader = (TreebankXmlReader) this.getXmlReader(file);
		treebankXmlReader.setSentenceNumber(sentenceNumber);
		return treebankXmlReader;
	}

	public TreebankService getTreebankService() {
		return treebankService;
	}

	public void setTreebankService(TreebankService treebankService) {
		this.treebankService = treebankService;
	}

	@Override
	public TreebankXmlLoader getTreebankXmlLoader(TreebankReader reader) {
		TreebankXmlLoaderImpl loader = new TreebankXmlLoaderImpl(reader);
		return loader;
	}

}
