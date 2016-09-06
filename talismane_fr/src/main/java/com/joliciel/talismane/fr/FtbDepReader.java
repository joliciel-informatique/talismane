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
package com.joliciel.talismane.fr;

import java.io.IOException;
import java.io.Reader;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joliciel.talismane.TalismaneSession;
import com.joliciel.talismane.parser.ParserRegexBasedCorpusReader;

/**
 * A reader for the FrenchTreebank corpus, automatically converted from
 * constituent trees to dependencies as by Candito, Crabbé and Denis, see
 * <i>Candito M.-H., Crabbé B., and Denis P., Statistical French dependency
 * parsing: treebank conversion and first results, Proceedings of LREC'2010, La
 * Valletta, Malta, 2010.</i>
 * 
 * @author Assaf
 *
 */
public class FtbDepReader extends ParserRegexBasedCorpusReader {
	@SuppressWarnings("unused")
	private static final Logger LOG = LoggerFactory.getLogger(FtbDepReader.class);

	private boolean keepCompoundPosTags = false;

	public FtbDepReader(Reader reader, TalismaneSession talismaneSession) throws IOException {
		super("%INDEX%\\t%TOKEN%\\t.*\\t.*\\t%POSTAG%\\t.*\\t%GOVERNOR%\\t%LABEL%\\t.*\\t.*", reader, talismaneSession);
	}

	@Override
	protected boolean checkDataLine(ParseDataLine dataLine) {
		if (dataLine.getDependencyLabel().equals("missinghead")) {
			return false;
		}
		return true;
	}

	@Override
	protected void updateDataLine(List<ParseDataLine> dataLines, int index) {
		ParseDataLine dataLine = dataLines.get(index);
		if (dataLine.getPosTagCode().equals("PREF")) {
			dataLine.setPosTagCode("ADV");
		} else if (!keepCompoundPosTags) {
			if (dataLine.getPosTagCode().equals("P+D")) {
				dataLine.setPosTagCode("P");
			} else if (dataLine.getPosTagCode().equals("P+PRO")) {
				dataLine.setPosTagCode("PRO");
			} else if (dataLine.getPosTagCode().length() == 0) {
				ParseDataLine previousLine = dataLines.get(index - 1);
				ParseDataLine nextLine = dataLines.get(index + 1);
				if (nextLine.getPosTagCode().equals("P+PRO")) {
					dataLine.setPosTagCode("P");
					nextLine.setPosTagCode("PROREL");

					dataLine.setGovernorIndex(nextLine.getGovernorIndex());
					nextLine.setGovernorIndex(dataLine.getIndex());
				} else if (previousLine.getPosTagCode().equals("DET")) {
					// this empty token is equivalent to a null postag, and can
					// be removed
					dataLine.setSkip(true);
				} else {
					// Initially checked for "P+D" only here, but since there
					// are many other postags used,
					// especially in the case of compounds, we left off the
					// condition
					if (previousLine.getPosTagCode().equals("P+D"))
						previousLine.setPosTagCode("P");
					dataLine.setPosTagCode("DET");
					dataLine.setDependencyLabel("det");
					// if it's a P+D, the D needs to become dependent on the
					// noun that depends on the P
					ParseDataLine governor = null;
					int realGovernorIndex = 0;
					for (int i = index + 1; i < dataLines.size(); i++) {
						ParseDataLine otherLine = dataLines.get(i);
						if (otherLine.getPosTagCode().equals("PONCT"))
							continue;
						if (governor == null) {
							governor = otherLine;
							realGovernorIndex = i;
						}
						if (otherLine.getGovernorIndex() == previousLine.getIndex()) {
							governor = otherLine;
							realGovernorIndex = i;
							break;
						} else if (otherLine.getGovernorIndex() < previousLine.getIndex()) {
							break;
						}
					}

					for (int i = index + 1; i < realGovernorIndex; i++) {
						ParseDataLine otherLine = dataLines.get(i);
						if (otherLine.getPosTagCode().equals("PONCT") && otherLine.getGovernorIndex() == previousLine.getIndex()) {
							otherLine.setGovernorIndex(dataLine.getIndex());
						}
					}
					dataLine.setGovernorIndex(governor.getIndex());
				}
			}
		}
	}

	public boolean isKeepCompoundPosTags() {
		return keepCompoundPosTags;
	}

	public void setKeepCompoundPosTags(boolean keepCompoundPosTags) {
		this.keepCompoundPosTags = keepCompoundPosTags;
	}

	@Override
	protected String readWord(String rawWord) {
		if (rawWord.equals("_")) {
			return "";
		}
		String[] parts = rawWord.split("_");
		String word = "";
		boolean firstPart = true;
		String lastPart = "";
		for (String part : parts) {
			if (firstPart) {
				word += part;
				firstPart = false;
			} else if (lastPart.endsWith("-") || part.startsWith("-") || lastPart.endsWith("'")) {
				word += part;
			} else {
				word += " " + part;
			}
			lastPart = part;
		}
		return word;
	}

}
