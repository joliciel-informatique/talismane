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
package com.joliciel.talismane.extensions.corpus;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.io.Writer;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import com.joliciel.talismane.TalismaneSession;
import com.joliciel.talismane.posTagger.PosTag;
import com.joliciel.talismane.posTagger.PosTagOpenClassIndicator;
import com.joliciel.talismane.posTagger.PosTagSequence;
import com.joliciel.talismane.posTagger.PosTagSequenceProcessor;
import com.joliciel.talismane.posTagger.PosTagSet;
import com.joliciel.talismane.posTagger.PosTaggedToken;
import com.joliciel.talismane.tokeniser.Token;
import com.joliciel.talismane.utils.CSVFormatter;
import com.joliciel.talismane.utils.LogUtils;

/**
 * A class for gathering statistics from a given corpus.
 * @author Assaf Urieli
 *
 */
public class PosTaggerStatistics implements PosTagSequenceProcessor, Serializable {
	private static final long serialVersionUID = 1L;
	private static final Log LOG = LogFactory.getLog(PosTaggerStatistics.class);
	private static final CSVFormatter CSV = new CSVFormatter();
	
	private Set<String> words = new TreeSet<String>();
	private Set<String> lowerCaseWords = new TreeSet<String>();
	private Map<String,Integer> posTagCounts = new TreeMap<String,Integer>();
	private int tokenCount;
	private int unknownTokenCount;
	private int alphanumericCount;
	private int unknownAlphanumericCount;
	private int unknownInLexiconCount;
	private int unknownAlphaInLexiconCount;
	private int sentenceCount;
	private int openClassCount;
	private int openClassUnknownInRefCorpus;
	private int openClassUnknownInLexicon;
	private int closedClassCount;
	private int closedClassUnknownInRefCorpus;
	private int closedClassUnknownInLexicon;
	private DescriptiveStatistics sentenceLengthStats = new DescriptiveStatistics();
	
	private Pattern alphanumeric = Pattern.compile("[a-zA-Z0-9àáçéèëêïîíöôóòüûú]");
	
	private transient Set<String> referenceWords = null;
	private transient Set<String> referenceLowercaseWords = null;
	private transient Writer writer;
	private transient File serializationFile;
	
	
	private TalismaneSession talismaneSession;
	
	public PosTaggerStatistics(TalismaneSession talismaneSession) {
		super();
		this.talismaneSession = talismaneSession;
	}
	
	@Override
	public void onNextPosTagSequence(PosTagSequence posTagSequence,
			Writer writer) {
		sentenceCount++;
		sentenceLengthStats.addValue(posTagSequence.size());
		
		for (PosTaggedToken posTaggedToken : posTagSequence) {
			if (posTaggedToken.getTag().equals(PosTag.ROOT_POS_TAG))
				continue;
			
			Token token = posTaggedToken.getToken();

			
			boolean knownInRefCorpus = false;
			boolean knownInLexicon = false;
			if (token.getPossiblePosTags().size()>0)
				knownInLexicon = true;
			
			String word = token.getOriginalText();
			words.add(word);
			
			if (referenceWords!=null)
				if (referenceWords.contains(word))
					knownInRefCorpus = true;
			
			if (!knownInLexicon) {
				unknownInLexiconCount++;
			}
			if (posTaggedToken.getTag().getOpenClassIndicator()==PosTagOpenClassIndicator.CLOSED) {
				closedClassCount++;
				if (!knownInRefCorpus)
					closedClassUnknownInRefCorpus++;
				if (!knownInLexicon)
					closedClassUnknownInLexicon++;
			} else if (posTaggedToken.getTag().getOpenClassIndicator()==PosTagOpenClassIndicator.OPEN) {
				openClassCount++;
				if (!knownInRefCorpus)
					openClassUnknownInRefCorpus++;
				if (!knownInLexicon)
					openClassUnknownInLexicon++;
			}
			
			if (!knownInRefCorpus)
				unknownTokenCount++;
			
			if (alphanumeric.matcher(token.getOriginalText()).find()) {
				String lowercase = word.toLowerCase(talismaneSession.getLocale());
				lowerCaseWords.add(lowercase);
				alphanumericCount++;
				if (!knownInRefCorpus)
					unknownAlphanumericCount++;
				
				if (!knownInLexicon)
					unknownAlphaInLexiconCount++;
			}
			
			tokenCount++;
			
			Integer countObj = posTagCounts.get(posTaggedToken.getTag().getCode());
			int count = countObj==null ? 0 : countObj.intValue();
			count++;
			posTagCounts.put(posTaggedToken.getTag().getCode(), count);
		}
		
	}

	@Override
	public void onCompleteAnalysis() {
		try {
			if (writer!=null) {
				PosTagSet posTagSet = talismaneSession.getPosTagSet();
				for (PosTag posTag : posTagSet.getTags()) {
					if (!posTagCounts.containsKey(posTag.getCode())) {
						posTagCounts.put(posTag.getCode(), 0);
					}
				}
				
				double unknownLexiconPercent = 1;
				if (referenceWords!=null) {
					int unknownLexiconCount = 0;
					for (String word : words) {
						if (!referenceWords.contains(word))
							unknownLexiconCount++;
					}
					unknownLexiconPercent = (double) unknownLexiconCount / (double) words.size();
				}
				double unknownLowercaseLexiconPercent = 1;
				if (referenceLowercaseWords!=null) {
					int unknownLowercaseLexiconCount = 0;
					for (String lowercase : lowerCaseWords) {
						if (!referenceLowercaseWords.contains(lowercase))
							unknownLowercaseLexiconCount++;
					}
					unknownLowercaseLexiconPercent = (double) unknownLowercaseLexiconCount / (double) lowerCaseWords.size();
				}

				writer.write(CSV.format("sentenceCount") + CSV.format(sentenceCount) + "\n");
				writer.write(CSV.format("sentenceLengthMean") + CSV.format(sentenceLengthStats.getMean()) + "\n");
				writer.write(CSV.format("sentenceLengthStdDev") + CSV.format(sentenceLengthStats.getStandardDeviation()) + "\n");
				writer.write(CSV.format("lexiconSize") + CSV.format(words.size()) + "\n");
				writer.write(CSV.format("lexiconUnknownInRefCorpus") + CSV.format(unknownLexiconPercent * 100.0) + "\n");
				writer.write(CSV.format("tokenCount") + CSV.format(tokenCount) + "\n");
				
				double unknownTokenPercent = ((double) unknownTokenCount / (double) tokenCount) * 100.0;
				writer.write(CSV.format("tokenUnknownInRefCorpus") + CSV.format(unknownTokenPercent) + "\n");
				
				double unknownInLexiconPercent = ((double) unknownInLexiconCount / (double) tokenCount) * 100.0;
				writer.write(CSV.format("tokenUnknownInRefLexicon") + CSV.format(unknownInLexiconPercent) + "\n");
				
				writer.write(CSV.format("lowercaseLexiconSize") + CSV.format(lowerCaseWords.size()) + "\n");
				writer.write(CSV.format("lowercaseLexiconUnknownInRefCorpus") + CSV.format(unknownLowercaseLexiconPercent * 100.0) + "\n");
				writer.write(CSV.format("alphanumericCount") + CSV.format(alphanumericCount) + "\n");
				
				double unknownAlphanumericPercent = ((double) unknownAlphanumericCount / (double) alphanumericCount) * 100.0;
				writer.write(CSV.format("alphaUnknownInRefCorpus") + CSV.format(unknownAlphanumericPercent) + "\n");

				double unknownAlphaInLexiconPercent = ((double) unknownAlphaInLexiconCount / (double) alphanumericCount) * 100.0;
				writer.write(CSV.format("alphaUnknownInRefLexicon") + CSV.format(unknownAlphaInLexiconPercent) + "\n");

				writer.write(CSV.format("openClassCount") + CSV.format(openClassCount) + "\n");
				
				double openClassUnknownPercent = ((double) openClassUnknownInRefCorpus / (double) openClassCount) * 100.0;
				writer.write(CSV.format("openClassUnknownInRefCorpus") + CSV.format(openClassUnknownPercent) + "\n");

				double openClassUnknownInLexiconPercent = ((double) openClassUnknownInLexicon / (double) openClassCount) * 100.0;
				writer.write(CSV.format("openClassUnknownInRefLexicon") + CSV.format(openClassUnknownInLexiconPercent) + "\n");
				
				writer.write(CSV.format("closedClassCount") + CSV.format(closedClassCount) + "\n");
				
				double closedClassUnknownPercent = ((double) closedClassUnknownInRefCorpus / (double) closedClassCount) * 100.0;
				writer.write(CSV.format("closedClassUnknownInRefCorpus") + CSV.format(closedClassUnknownPercent) + "\n");

				double closedClassUnknownInLexiconPercent = ((double) closedClassUnknownInLexicon / (double) closedClassCount) * 100.0;
				writer.write(CSV.format("closedClassUnknownInRefLexicon") + CSV.format(closedClassUnknownInLexiconPercent) + "\n");

				for (String posTag : posTagCounts.keySet()) {
					int count = posTagCounts.get(posTag);
					writer.write(CSV.format(posTag) + CSV.format(count) + CSV.format(((double) count / (double) tokenCount)* 100.0) + "\n");
				}
				
				writer.flush();
				writer.close();
			}
			
			if (this.serializationFile!=null) {
				ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(serializationFile,false));
				zos.putNextEntry(new ZipEntry("Contents.obj"));
				ObjectOutputStream oos = new ObjectOutputStream(zos);
				try {
					oos.writeObject(this);
				} finally {
					oos.flush();
				}
				zos.flush();
				zos.close();
			}
		} catch (IOException e) {
			LogUtils.logError(LOG, e);
			throw new RuntimeException(e);
		}
		
	}


	public static PosTaggerStatistics loadFromFile(File inFile) {
		try {
			ZipInputStream zis = new ZipInputStream(new FileInputStream(inFile));
			zis.getNextEntry();
			ObjectInputStream in = new ObjectInputStream(zis);
			PosTaggerStatistics stats = null;
			try {
				stats = (PosTaggerStatistics) in.readObject();
			} catch (ClassNotFoundException e) {
				LogUtils.logError(LOG, e);
				throw new RuntimeException(e);
			}
			return stats;
		} catch (IOException ioe) {
			LogUtils.logError(LOG, ioe);
			throw new RuntimeException(ioe);
		}
	}
	public Set<String> getReferenceWords() {
		return referenceWords;
	}

	public void setReferenceWords(Set<String> referenceWords) {
		this.referenceWords = referenceWords;
	}

	public Set<String> getReferenceLowercaseWords() {
		return referenceLowercaseWords;
	}

	public void setReferenceLowercaseWords(Set<String> referenceLowercaseWords) {
		this.referenceLowercaseWords = referenceLowercaseWords;
	}

	public Writer getWriter() {
		return writer;
	}

	public void setWriter(Writer writer) {
		this.writer = writer;
	}

	public Set<String> getWords() {
		return words;
	}

	public Set<String> getLowerCaseWords() {
		return lowerCaseWords;
	}

	public Map<String, Integer> getPosTagCounts() {
		return posTagCounts;
	}

	public int getWordCount() {
		return tokenCount;
	}

	public int getAlphanumericCount() {
		return alphanumericCount;
	}

	public int getSentenceCount() {
		return sentenceCount;
	}

	public DescriptiveStatistics getSentenceLengthStats() {
		return sentenceLengthStats;
	}

	public void setSerializationFile(File serializationFile) {
		this.serializationFile = serializationFile;
	}

}
