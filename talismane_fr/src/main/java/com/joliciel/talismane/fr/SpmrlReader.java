package com.joliciel.talismane.fr;

import java.io.IOException;
import java.io.Reader;

import com.joliciel.talismane.TalismaneSession;
import com.joliciel.talismane.parser.ParserRegexBasedCorpusReader;
import com.typesafe.config.Config;

public class SpmrlReader extends ParserRegexBasedCorpusReader {

	public SpmrlReader(Reader reader, Config config, TalismaneSession session) throws IOException {
		super("%INDEX%\\t%TOKEN%\\t.*\\t.*\\t%POSTAG%\\t.*\\t%NON_PROJ_GOVERNOR%\\t%NON_PROJ_LABEL%\\t%GOVERNOR%\\t%LABEL%", reader, config, session);
	}

}
