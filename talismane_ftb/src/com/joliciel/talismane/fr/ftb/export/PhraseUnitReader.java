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
package com.joliciel.talismane.fr.ftb.export;

import java.util.List;

import com.joliciel.talismane.fr.ftb.TreebankService;
import com.joliciel.talismane.posTagger.PosTag;

/**
 * Given a List of PhraseUnit, reads out the tokens one at a time.
 * @author Assaf
 *
 */
interface PhraseUnitReader {

	public abstract String nextString();
	
	/**
	 * A list of strings representing where the PhraseUnitReader is currently stationed
	 * (to export to a file in case of an error).
	 */
	public abstract List<String> getCurrentInfo();

	/**
	 * Get the PosTag of the last string read.
	 */
	public PosTag getPosTag();

	public FtbPosTagMapper getFtbPosTagMapper();
	public void setFtbPosTagMapper(FtbPosTagMapper ftbPosTagMapper);
	
	public TreebankService getTreebankService();
	public void setTreebankService(TreebankService treebankService);
}