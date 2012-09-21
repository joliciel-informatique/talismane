package com.joliciel.ftbDep;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.joliciel.talismane.machineLearning.Decision;
import com.joliciel.talismane.parser.DependencyArc;
import com.joliciel.talismane.parser.ParseConfiguration;
import com.joliciel.talismane.parser.ParseAnnotatedCorpusReader;
import com.joliciel.talismane.parser.ParserService;
import com.joliciel.talismane.parser.TransitionSystem;
import com.joliciel.talismane.posTagger.PosTag;
import com.joliciel.talismane.posTagger.PosTagSequence;
import com.joliciel.talismane.posTagger.PosTagSet;
import com.joliciel.talismane.posTagger.PosTaggedToken;
import com.joliciel.talismane.posTagger.PosTaggerService;
import com.joliciel.talismane.tokeniser.Token;
import com.joliciel.talismane.tokeniser.TokenSequence;
import com.joliciel.talismane.tokeniser.TokeniserService;
import com.joliciel.talismane.tokeniser.filters.TokenFilter;
import com.joliciel.talismane.utils.PerformanceMonitor;

public class FtbDepReader implements ParseAnnotatedCorpusReader {
    private static final Log LOG = LogFactory.getLog(FtbDepReader.class);
	private File ftbDepFile = null;
	private ParseConfiguration configuration = null;
	private Scanner scanner;
	
	private ParserService parserService;
	private PosTaggerService posTaggerService;
	private TokeniserService tokeniserService;
	
	private Pattern separators = Pattern.compile(" ");
	
	private int maxSentenceCount = 0;
	private int sentenceCount = 0;
	private PosTagSet posTagSet;
	private TransitionSystem transitionSystem;

	private List<TokenFilter> tokenFilters = new ArrayList<TokenFilter>();

	public FtbDepReader(File ftbDepFile) {
		super();
		this.ftbDepFile = ftbDepFile;
	}

	@Override
	public boolean hasNextSentence() {
		PerformanceMonitor.startTask("FtbDepReader.hasNextSentence");
		try {
			this.initialise();
			if (maxSentenceCount>0 && sentenceCount>=maxSentenceCount) {
				// we've reached the end, do nothing
			} else {
				while (configuration==null) {
					List<ConnlDataLine> dataLines = new ArrayList<FtbDepReader.ConnlDataLine>();
					boolean badConfig = false;
					while (scanner.hasNextLine()) {
						String line = scanner.nextLine();
						
						if (line.length()==0) {
							break;
						}
						
						ConnlDataLine dataLine = new ConnlDataLine(line);
						if (dataLine.label.equals("missinghead")) {
							badConfig = true;
						}
						
						dataLines.add(dataLine);
		
						if (dataLine.subCategory.equals("P+D")) {
							dataLine.category = "P";
							dataLine.subCategory = "P";
							
							ConnlDataLine otherLine = new ConnlDataLine();
							otherLine.category = "D";
							otherLine.subCategory = "DET";
							otherLine.word = "";
							otherLine.lemma = "le";
							otherLine.id = dataLine.id + 0.5;
							otherLine.headId = dataLine.id;
							
							if (dataLine.word.equals("au")||dataLine.word.equals("du")) {
								otherLine.morphology = "g=m|n=s|s=def";
							} else if (dataLine.word.equals("aux")||dataLine.word.equals("des")) {
								otherLine.morphology = "g=m|n=p|s=def";
							}
							dataLines.add(otherLine);
							
						} else if (dataLine.subCategory.equals("P+PRO")) {
							dataLine.category = "P";
							dataLine.subCategory = "P";
		
							ConnlDataLine otherLine = new ConnlDataLine();
							otherLine.category = "PRO";
							otherLine.subCategory = "PROREL";
							otherLine.word = "";
							otherLine.lemma = dataLine.lemma;
							otherLine.morphology = dataLine.morphology;
							otherLine.id = dataLine.id + 0.5;
							otherLine.headId = dataLine.id;
							
							if (dataLine.word.equals("auquel")||dataLine.word.equals("duquel")) {
								dataLine.lemma = "à";
								dataLine.morphology = "_";
							} else if (dataLine.word.equals("auxquels")||dataLine.word.equals("auxquelles")
									||dataLine.word.equals("desquels")||dataLine.word.equals("desquelles")) {
								dataLine.lemma = "de";
								dataLine.morphology = "_";
							}
							dataLines.add(otherLine);
						}
		
						
					}
					if (dataLines.size()>0 && !badConfig) {
						String sentence = "";
						boolean firstItem = true;
						for (ConnlDataLine dataLine : dataLines) {
							if (!firstItem&&dataLine.word.length()>0)
								sentence += " ";
							sentence += dataLine.word;
							firstItem = false;
						}
						LOG.debug("Sentence " + sentenceCount + ": " + sentence);
						
						TokenSequence tokenSequence = this.tokeniserService.getTokenSequence(sentence, separators);
						
						int currentPos = 0;
						for (ConnlDataLine dataLine : dataLines) {
							if (dataLine.word.length()==0) {
								tokenSequence.addEmptyToken(currentPos);
							} else {
								tokenSequence.addToken(currentPos, currentPos+dataLine.word.length());
								currentPos += dataLine.word.length() + 1;
							}
						}
						
						for (TokenFilter tokenFilter : this.tokenFilters) {
							tokenFilter.apply(tokenSequence);
						}
						
						PosTagSequence posTagSequence = this.posTaggerService.getPosTagSequence(tokenSequence, dataLines.size());
						
						int i = 0;
						Map<Double, PosTaggedToken> idTokenMap = new HashMap<Double, PosTaggedToken>();
						
						for (ConnlDataLine dataLine : dataLines) {
							Token token = tokenSequence.get(i);
							PosTag posTag = this.getPosTag(dataLine);
							Decision<PosTag> posTagDecision = posTagSet.createDefaultDecision(posTag);
							PosTaggedToken posTaggedToken = this.posTaggerService.getPosTaggedToken(token, posTagDecision);
							posTagSequence.add(posTaggedToken);
							idTokenMap.put(dataLine.id, posTaggedToken);
							i++;
						}
		
						i = 0;
						for (ConnlDataLine dataLine : dataLines) {
							if (dataLine.word.length()==0) {
								// correct the dependencies
								if (dataLine.category.equals("D")) {
									// if it's a P+D, the D needs to become dependent on the noun that depends on the P
									for (int j=i+1;j<dataLines.size();j++) {
										ConnlDataLine otherLine = dataLines.get(j);
										if (otherLine.headId==dataLine.headId) {
											dataLine.headId = otherLine.id;
											break;
										}
									}
								} else if (dataLine.category.equals("PRO")) {
									// nothing to do, it's already correctly dependent on the P
								}
							}
							i++;
						} // next data line
						
						PosTaggedToken rootToken = posTagSequence.prependRoot();
						idTokenMap.put(0.0, rootToken);
						
						Set<DependencyArc> dependencies = new TreeSet<DependencyArc>();
						for (ConnlDataLine dataLine : dataLines) {
							PosTaggedToken head = idTokenMap.get(dataLine.headId);
							PosTaggedToken dependent = idTokenMap.get(dataLine.id);
							DependencyArc arc = this.parserService.getDependencyArc(head, dependent, dataLine.label);
							dependencies.add(arc);
						}
						
						configuration = this.parserService.getInitialConfiguration(posTagSequence);
						transitionSystem.predictTransitions(configuration, dependencies);

						sentenceCount++;
					} // have we data lines?
					
					if (!scanner.hasNextLine())
						break;
					
				} // is configuration still null?
			} // have we reached the max sentence count?
			
			if (configuration==null) {
				scanner.close();
			}
			
			return configuration!=null;
		} finally {
			PerformanceMonitor.endTask("FtbDepReader.hasNextSentence");
		}
	}

	private PosTag getPosTag(ConnlDataLine dataLine) {
		String subcatCode = dataLine.subCategory;
		if (subcatCode.equals("P+D"))
			subcatCode = "P";
		else if (subcatCode.equals("P+PRO")) 
			subcatCode = "PROREL";
		else if (subcatCode.equals("PREF"))
			subcatCode = "ADV";
		
		PosTag posTag = posTagSet.getPosTag(subcatCode);
		if (posTag==null) {
			throw new RuntimeException("Unknown posTag: " + subcatCode);
		}
		return posTag;
	}

	@Override
	public ParseConfiguration nextSentence() {
		ParseConfiguration nextConfiguration = null;
		if (this.hasNextSentence()) {
			nextConfiguration = configuration;
			configuration = null;
		}
		return nextConfiguration;
	}

	void initialise() {
		if (this.scanner==null) {
			try {
				this.scanner = new Scanner(ftbDepFile);
			} catch (FileNotFoundException e) {
				throw new RuntimeException(e);
			}
		}
	}

	public PosTaggerService getPosTaggerService() {
		return posTaggerService;
	}

	public void setPosTaggerService(PosTaggerService posTaggerService) {
		this.posTaggerService = posTaggerService;
	}
	
	public TokeniserService getTokeniserService() {
		return tokeniserService;
	}

	public void setTokeniserService(TokeniserService tokeniserService) {
		this.tokeniserService = tokeniserService;
	}

	public ParserService getParserService() {
		return parserService;
	}

	public void setParserService(ParserService parserService) {
		this.parserService = parserService;
	}

	private static class ConnlDataLine {
		public ConnlDataLine(String line) {
			String[] portions = line.split("\t");
			
			id = Integer.parseInt(portions[0]);
			word = portions[1].replace('_', ' ');
			if (word.contains(" - "))
				word = word.replace(" - ", "-");
			
			lemma = portions[2].replace('_', ' ');
			if (lemma.contains(" - "))
				lemma = lemma.replace(" - ", "-");
			
			category = portions[3];
			subCategory = portions[4];
			morphology = portions[5];
			headId = Integer.parseInt(portions[6]);
			label = portions[7];
		}
		
		public ConnlDataLine() {
			
		}
		
		public double id;
		public String word = "";
		public String lemma = "";
		public String category = "";
		public String subCategory = "";
		public String morphology = "";
		public double headId;
		public String label = "";
	}

	/**
	 * If 0, all sentences will be read - otherwise will only read a certain number of sentences.
	 * @return
	 */
	public int getMaxSentenceCount() {
		return maxSentenceCount;
	}

	public void setMaxSentenceCount(int maxSentenceCount) {
		this.maxSentenceCount = maxSentenceCount;
	}

	/**
	 * The Crabbé & Candito tagset used by the ftbDep dependency treebank.
	 * @return
	 */
	public PosTagSet getPosTagSet() {
		return posTagSet;
	}

	public void setPosTagSet(PosTagSet posTagSet) {
		this.posTagSet = posTagSet;
	}
	
	
	public TransitionSystem getTransitionSystem() {
		return transitionSystem;
	}

	public void setTransitionSystem(TransitionSystem transitionSystem) {
		this.transitionSystem = transitionSystem;
	}

	public void addTokenFilter(TokenFilter tokenFilter) {
		this.tokenFilters.add(tokenFilter);
	}

	@Override
	public Map<String, Object> getAttributes() {
		Map<String,Object> attributes = new HashMap<String, Object>();

		attributes.put("ftpDebFile", this.ftbDepFile.getName());
		attributes.put("maxSentenceCount", this.maxSentenceCount);
		
		if (transitionSystem!=null)
			attributes.put("transitionSystem", "" + transitionSystem.getClass().getSimpleName());
		
		int i = 0;
		for (TokenFilter tokenFilter : this.tokenFilters) {
			attributes.put("filter" + i, "" + tokenFilter.getClass().getSimpleName());
			
			i++;
		}
		return attributes;
	}

	
}
