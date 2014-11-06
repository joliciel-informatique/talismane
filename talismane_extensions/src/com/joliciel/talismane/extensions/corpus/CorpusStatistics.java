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
import java.util.HashSet;
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
import com.joliciel.talismane.parser.DependencyArc;
import com.joliciel.talismane.parser.ParseConfiguration;
import com.joliciel.talismane.parser.ParseConfigurationProcessor;
import com.joliciel.talismane.posTagger.PosTag;
import com.joliciel.talismane.posTagger.PosTaggedToken;
import com.joliciel.talismane.tokeniser.Token;
import com.joliciel.talismane.utils.CSVFormatter;
import com.joliciel.talismane.utils.LogUtils;

/**
 * A class for gathering statistics from a given corpus.
 * @author Assaf Urieli
 *
 */
public class CorpusStatistics implements ParseConfigurationProcessor, Serializable {
	private static final long serialVersionUID = 1L;
	private static final Log LOG = LogFactory.getLog(CorpusStatistics.class);
	private static final CSVFormatter CSV = new CSVFormatter();
	
	private Set<String> words = new TreeSet<String>();
	private Set<String> lowerCaseWords = new TreeSet<String>();
	private Map<String,Integer> posTagCounts = new TreeMap<String,Integer>();
	private Map<String,Integer> depLabelCounts = new TreeMap<String, Integer>();
	private int tokenCount;
	private int unknownTokenCount;
	private int alphanumericCount;
	private int unknownAlphanumericCount;
	private int sentenceCount;
	private int nonProjectiveCount;
	private int totalDepCount;
	private DescriptiveStatistics sentenceLengthStats = new DescriptiveStatistics();
	private DescriptiveStatistics syntaxDepthStats = new DescriptiveStatistics();
	private DescriptiveStatistics maxSyntaxDepthStats = new DescriptiveStatistics();
	private DescriptiveStatistics avgSyntaxDepthStats = new DescriptiveStatistics();
	private DescriptiveStatistics syntaxDistanceStats = new DescriptiveStatistics();
	
	private Pattern alphanumeric = Pattern.compile("[a-zA-Z0-9àáçéèëêïîíöôóòüûú]");
	
	private transient Set<String> referenceWords = null;
	private transient Set<String> referenceLowercaseWords = null;
	private transient Writer writer;
	private transient File serializationFile;
	
	private TalismaneSession talismaneSession;
	
	public CorpusStatistics(TalismaneSession talismaneSession) {
		super();
		this.talismaneSession = talismaneSession;
	}

	@Override
	public void onNextParseConfiguration(ParseConfiguration parseConfiguration, Writer writer) {
		sentenceCount++;
		sentenceLengthStats.addValue(parseConfiguration.getPosTagSequence().size());
		
		for (PosTaggedToken posTaggedToken : parseConfiguration.getPosTagSequence()) {
			if (posTaggedToken.getTag().equals(PosTag.ROOT_POS_TAG))
				continue;
			
			Token token = posTaggedToken.getToken();
			
			String word = token.getOriginalText();
			words.add(word);
			if (referenceWords!=null) {
				if (!referenceWords.contains(word))
					unknownTokenCount++;
			}
			if (alphanumeric.matcher(token.getOriginalText()).find()) {
				String lowercase = word.toLowerCase(talismaneSession.getLocale());
				lowerCaseWords.add(lowercase);
				alphanumericCount++;
				if (referenceLowercaseWords!=null) {
					if (!referenceLowercaseWords.contains(lowercase))
						unknownAlphanumericCount++;
				}
			}
			
			tokenCount++;
			
			Integer countObj = posTagCounts.get(posTaggedToken.getTag().getCode());
			int count = countObj==null ? 0 : countObj.intValue();
			count++;
			posTagCounts.put(posTaggedToken.getTag().getCode(), count);
		}
		
		int maxDepth = 0;
		DescriptiveStatistics avgSyntaxDepthForSentenceStats = new DescriptiveStatistics();
		for (DependencyArc arc : parseConfiguration.getDependencies()) {
			Integer countObj = depLabelCounts.get(arc.getLabel());
			int count = countObj==null ? 0 : countObj.intValue();
			count++;
			depLabelCounts.put(arc.getLabel(), count);
			totalDepCount++;
						
			if (arc.getHead().getTag().equals(PosTag.ROOT_POS_TAG) && (arc.getLabel()==null||arc.getLabel().length()==0)) {
				// do nothing for unattached stuff (e.g. punctuation)
			} else if (arc.getLabel().equals("ponct")) {
				// do nothing for punctuation
			} else {
				int depth = 0;
				DependencyArc theArc = arc;
				while (theArc!=null && !theArc.getHead().getTag().equals(PosTag.ROOT_POS_TAG)) {
					theArc = parseConfiguration.getGoverningDependency(theArc.getHead());
					depth++;
				}
				if (depth>maxDepth)
					maxDepth = depth;

				syntaxDepthStats.addValue(depth);
				avgSyntaxDepthForSentenceStats.addValue(depth);
				
				int distance = Math.abs(arc.getHead().getToken().getIndex() - arc.getDependent().getToken().getIndex());
				syntaxDistanceStats.addValue(distance);
			}
			
			maxSyntaxDepthStats.addValue(maxDepth);
			if (avgSyntaxDepthForSentenceStats.getN()>0)
				avgSyntaxDepthStats.addValue(avgSyntaxDepthForSentenceStats.getMean());
		}
		
		// we cheat a little bit by only allowing each arc to count once
		// there could be a situation where there are two independent non-projective arcs
		// crossing the same mother arc, but we prefer here to underestimate,
		// as this phenomenon is quite rare.
		Set<DependencyArc> nonProjectiveArcs = new HashSet<DependencyArc>();
		int i = 0;
		for (DependencyArc arc : parseConfiguration.getDependencies()) {
			i++;
			if (arc.getHead().getTag().equals(PosTag.ROOT_POS_TAG) && (arc.getLabel()==null||arc.getLabel().length()==0))
				continue;
			if (nonProjectiveArcs.contains(arc))
				continue;
			
			int headIndex = arc.getHead().getToken().getIndex();
			int depIndex = arc.getDependent().getToken().getIndex();
			int startIndex = headIndex < depIndex ? headIndex : depIndex;
			int endIndex = headIndex >= depIndex ? headIndex : depIndex;
			int j=0;
			for (DependencyArc otherArc : parseConfiguration.getDependencies()) {
				j++;
				if (j<=i)
					continue;
				if (otherArc.getHead().getTag().equals(PosTag.ROOT_POS_TAG) && (otherArc.getLabel()==null||otherArc.getLabel().length()==0))
					continue;
				if (nonProjectiveArcs.contains(otherArc))
					continue;
				
				int headIndex2 = otherArc.getHead().getToken().getIndex();
				int depIndex2 = otherArc.getDependent().getToken().getIndex();
				int startIndex2 = headIndex2 < depIndex2 ? headIndex2 : depIndex2;
				int endIndex2 = headIndex2 >= depIndex2 ? headIndex2 : depIndex2;
				boolean nonProjective = false;
				if (startIndex2<startIndex && endIndex2>startIndex && endIndex2<endIndex) {
					nonProjective = true;
				} else if (startIndex2>startIndex && startIndex2<endIndex && endIndex2>endIndex) {
					nonProjective = true;
				}
				if (nonProjective) {
					nonProjectiveArcs.add(arc);
					nonProjectiveArcs.add(otherArc);
					nonProjectiveCount++;
					LOG.debug("Non-projective arcs in sentence: " + parseConfiguration.getSentence().getText());
					LOG.debug(arc.toString());
					LOG.debug(otherArc.toString());
					break;
				}
			}
		}
	}

	@Override
	public void onCompleteParse() {
		try {
			if (writer!=null) {
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
				writer.write(CSV.format("tokenLexiconSize") + CSV.format(words.size()) + "\n");
				writer.write(CSV.format("tokenLexiconUnknown") + CSV.format(unknownLexiconPercent * 100.0) + "\n");
				writer.write(CSV.format("tokenCount") + CSV.format(tokenCount) + "\n");
				
				double unknownTokenPercent = ((double) unknownTokenCount / (double) tokenCount) * 100.0;
				writer.write(CSV.format("tokenUnknown") + CSV.format(unknownTokenPercent) + "\n");

				writer.write(CSV.format("lowercaseLexiconSize") + CSV.format(lowerCaseWords.size()) + "\n");
				writer.write(CSV.format("lowercaseLexiconUnknown") + CSV.format(unknownLowercaseLexiconPercent * 100.0) + "\n");
				writer.write(CSV.format("alphanumericCount") + CSV.format(alphanumericCount) + "\n");
				
				double unknownAlphanumericPercent = ((double) unknownAlphanumericCount / (double) alphanumericCount) * 100.0;
				writer.write(CSV.format("alphanumericUnknown") + CSV.format(unknownAlphanumericPercent) + "\n");

				writer.write(CSV.format("syntaxDepthMean") + CSV.format(syntaxDepthStats.getMean()) + "\n");
				writer.write(CSV.format("syntaxDepthStdDev") + CSV.format(syntaxDepthStats.getStandardDeviation()) + "\n");
				writer.write(CSV.format("maxSyntaxDepthMean") + CSV.format(maxSyntaxDepthStats.getMean()) + "\n");
				writer.write(CSV.format("maxSyntaxDepthStdDev") + CSV.format(maxSyntaxDepthStats.getStandardDeviation()) + "\n");
				writer.write(CSV.format("sentAvgSyntaxDepthMean") + CSV.format(avgSyntaxDepthStats.getMean()) + "\n");
				writer.write(CSV.format("sentAvgSyntaxDepthStdDev") + CSV.format(avgSyntaxDepthStats.getStandardDeviation()) + "\n");
				writer.write(CSV.format("syntaxDistanceMean") + CSV.format(syntaxDistanceStats.getMean()) + "\n");
				writer.write(CSV.format("syntaxDistanceStdDev") + CSV.format(syntaxDistanceStats.getStandardDeviation()) + "\n");

				double nonProjectivePercent = ((double) nonProjectiveCount / (double) totalDepCount) * 100.0;
				writer.write(CSV.format("nonProjectiveCount") + CSV.format(nonProjectiveCount) + "\n");
				writer.write(CSV.format("nonProjectivePercent") + CSV.format(nonProjectivePercent) + "\n");
				writer.write(CSV.format("PosTagCounts") + "\n");
				
				for (String posTag : posTagCounts.keySet()) {
					int count = posTagCounts.get(posTag);
					writer.write(CSV.format(posTag) + CSV.format(count) + CSV.format(((double) count / (double) tokenCount)* 100.0) + "\n");
				}
				
				writer.write(CSV.format("DepLabelCounts") + "\n");
				for (String depLabel : depLabelCounts.keySet()) {
					int count = depLabelCounts.get(depLabel);
					writer.write(CSV.format(depLabel) + CSV.format(count) + CSV.format(((double) count / (double) totalDepCount)* 100.0) + "\n");
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


	public static CorpusStatistics loadFromFile(File inFile) {
		try {
			ZipInputStream zis = new ZipInputStream(new FileInputStream(inFile));
			zis.getNextEntry();
			ObjectInputStream in = new ObjectInputStream(zis);
			CorpusStatistics stats = null;
			try {
				stats = (CorpusStatistics) in.readObject();
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

	public Map<String, Integer> getDepLabelCounts() {
		return depLabelCounts;
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
