package com.joliciel.ftbDep;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.List;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.joliciel.talismane.TalismaneException;
import com.joliciel.talismane.parser.ParserRegexBasedCorpusReaderImpl;

public class FtbDepReader extends ParserRegexBasedCorpusReaderImpl {
    @SuppressWarnings("unused")
	private static final Log LOG = LogFactory.getLog(FtbDepReader.class);
     
	public FtbDepReader(File ftbDepFile) throws FileNotFoundException {
		super(new BufferedReader(new FileReader(ftbDepFile)));
		this.setRegex("INDEX\\tTOKEN\\t.*\\t.*\\tPOSTAG\\t.*\\tGOVERNOR\\tLABEL\\t_\\t_");
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
		if (dataLine.getPosTagCode().equals("PREF"))
			dataLine.setPosTagCode("ADV");
		
		if (dataLine.getPosTagCode().length()==0) {
			ParseDataLine previousLine = dataLines.get(index-1);
			ParseDataLine nextLine = dataLines.get(index+1);
			if (previousLine.getPosTagCode().equals("P+D")||previousLine.getPosTagCode().equals("P")) {
				previousLine.setPosTagCode("P");
				dataLine.setPosTagCode("DET");
				dataLine.setDependencyLabel("det");
				// if it's a P+D, the D needs to become dependent on the noun that depends on the P
				for (int i=index+1; i<dataLines.size(); i++) {
					ParseDataLine otherLine = dataLines.get(i);
					if (otherLine.getGovernorIndex()==previousLine.getIndex()) {
						dataLine.setGovernorIndex(otherLine.getIndex());
						break;
					}
				}
			} else if (nextLine.getPosTagCode().equals("P+PRO")) {
				dataLine.setPosTagCode("P");
				nextLine.setPosTagCode("PROREL");
				
				dataLine.setGovernorIndex(nextLine.getGovernorIndex());
				nextLine.setGovernorIndex(dataLine.getIndex());
			} else if (previousLine.getPosTagCode().equals("DET")) {
				// this empty token is equivalent to a null postag, and can be removed
				dataLine.setSkip(true);
			} else {
				throw new TalismaneException("Unexpected empty token on line: " + dataLine.getLineNumber());
			}
		}
	}
	
}
