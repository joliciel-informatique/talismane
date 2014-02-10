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

package com.joliciel.lefff;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import com.joliciel.talismane.lexicon.LexicalEntry;
import com.joliciel.talismane.lexicon.LexicalEntryMorphologyReader;
import com.joliciel.talismane.lexicon.PosTagMapper;
import com.joliciel.talismane.posTagger.PosTagSet;

public interface LefffService {
	public PosTagMapper getPosTagMapper(File file, PosTagSet posTagSet);
	public PosTagMapper getPosTagMapper(Scanner scanner, PosTagSet posTagSet);
	public PosTagMapper getPosTagMapper(List<String> descriptors, PosTagSet posTagSet);
	
    /**
     * Get an empty entry for saving.
     */
    public LefffEntry newEntry();
    
    public LefffEntry loadEntry(int entryId);
    
    public Word loadWord(int wordId);

    public Lemma loadLemma(int lemmaId);

    public Predicate loadPredicate(int predicateId);
    
    public Category loadCategory(int categoryId);

    public Attribute loadAttribute(int attributeId);

	public LefffLoader getLefffLoader();
	public LefffLoader getLefff3Loader();

	public Map<String, List<LexicalEntry>> findEntryMap();

	public Map<String, List<LexicalEntry>> findEntryMap(List<String> categories);
	
	public LexicalEntryMorphologyReader getLexicalEntryMorphologyReader();
}
