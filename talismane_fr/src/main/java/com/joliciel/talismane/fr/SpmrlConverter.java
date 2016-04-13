package com.joliciel.talismane.fr;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.PropertyConfigurator;

import com.joliciel.talismane.TalismaneException;
import com.joliciel.talismane.TalismaneServiceLocator;
import com.joliciel.talismane.fr.ftb.util.LogUtils;
import com.joliciel.talismane.posTagger.PosTagSet;
import com.joliciel.talismane.posTagger.PosTaggerService;
import com.joliciel.talismane.posTagger.UnknownPosTagException;
import com.joliciel.talismane.utils.StringUtils;
import com.joliciel.talismane.utils.WeightedOutcome;

/**
 * Converts an SPMRL style corpus into a version useable by Talismane.
 * 
 * @author Assaf Urieli
 *
 */
public class SpmrlConverter {
	private static final Log LOG = LogFactory.getLog(SpmrlConverter.class);

	private static PosTagSet posTagSet = null;

	public static void main(String[] args) throws Exception {
		Map<String, String> argMap = StringUtils.convertArgs(args);

		String logConfigPath = argMap.get("logConfigFile");
		if (logConfigPath != null) {
			argMap.remove("logConfigFile");
			Properties props = new Properties();
			props.load(new FileInputStream(logConfigPath));
			PropertyConfigurator.configure(props);
		}

		String spmrlPath = "";
		String suffix = "tal";
		boolean compressCompounds = true;
		boolean convertCompounds = false;
		String inDirPath = null;
		String outDirPath = null;
		String inSuffix = ".conll";
		String posTagSetPath = null;

		for (String argName : argMap.keySet()) {
			String argValue = argMap.get(argName);
			if (argName.equals("inFile")) {
				spmrlPath = argValue;
			} else if (argName.equals("inDir")) {
				inDirPath = argValue;
			} else if (argName.equals("outDir")) {
				outDirPath = argValue;
			} else if (argName.equals("inSuffix")) {
				inSuffix = argValue;
			} else if (argName.equals("suffix")) {
				suffix = argValue;
			} else if (argName.equals("compressCompounds")) {
				compressCompounds = argValue.equalsIgnoreCase("true");
			} else if (argName.equals("convertCompounds")) {
				convertCompounds = argValue.equalsIgnoreCase("true");
			} else if (argName.equals("posTagSet")) {
				posTagSetPath = argValue;
			} else {
				throw new RuntimeException("Unknown option: " + argName);
			}
		}

		if (!inSuffix.startsWith("."))
			inSuffix = "." + inSuffix;

		final String inSuffixFinal = inSuffix;
		List<File> inFiles = new ArrayList<File>();
		if (inDirPath != null) {
			File inDir = new File(inDirPath);
			File[] inFileArray = inDir.listFiles(new FilenameFilter() {

				@Override
				public boolean accept(File dir, String name) {
					return name.endsWith(inSuffixFinal);
				}
			});

			for (File file : inFileArray)
				inFiles.add(file);
		} else {
			File spmrlFile = new File(spmrlPath);
			inFiles.add(spmrlFile);
		}

		if (posTagSetPath != null) {
			File posTagSetFile = new File(posTagSetPath);
			try (Scanner posTagSetScanner = new Scanner(new BufferedReader(new InputStreamReader(new FileInputStream(posTagSetFile), "UTF-8")))) {
				TalismaneServiceLocator locator = TalismaneServiceLocator.getInstance("");
				PosTaggerService posTaggerService = locator.getPosTaggerServiceLocator().getPosTaggerService();
				posTagSet = posTaggerService.getPosTagSet(posTagSetScanner);
			}
		}

		for (File inFile : inFiles) {
			try {
				File outDir = inFile.getParentFile();
				if (outDirPath != null) {
					outDir = new File(outDirPath);
					outDir.mkdirs();
				}
				String fileName = inFile.getName().substring(0, inFile.getName().length() - inSuffix.length()) + "." + suffix;
				Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(new File(outDir, fileName)), "UTF-8"));
				Scanner scanner = new Scanner(new BufferedReader(new InputStreamReader(new FileInputStream(inFile), "UTF-8")));

				List<ConllLine> lines = new ArrayList<SpmrlConverter.ConllLine>();
				int lineNumber = 0;
				int newLineNumber = 0;
				Map<String, Integer> compoundPatternCounts = new TreeMap<String, Integer>();

				int nonProjectiveCount = 0;
				boolean errorOnNonProjective = false;

				while (scanner.hasNextLine()) {
					String line = scanner.nextLine();
					lineNumber++;
					LOG.trace(lineNumber + ": " + line);
					if (line.trim().length() == 0) {
						List<TokenCluster> tokens = new ArrayList<TokenCluster>();

						boolean inCluster = false;
						ConllLine lastLine = null;
						for (ConllLine conllLine : lines) {
							TokenCluster tokenCluster = new TokenCluster();
							if (!conllLine.label.equals("dep_cpd")) {
								if (inCluster) {
									tokenCluster = tokens.get(tokens.size() - 1);
									inCluster = false;
								} else {
									tokens.add(tokenCluster);
								}
							} else {
								if (conllLine.index < conllLine.governor) {
									// forward looking cluster
									if (lastLine != null && lastLine.compPosTag != null) {
										tokenCluster = tokens.get(tokens.size() - 1);
									} else if (inCluster) {
										tokenCluster = tokens.get(tokens.size() - 1);
									} else {
										inCluster = true;
										tokens.add(tokenCluster);
									}
								} else if (tokens.size() > 0) {
									tokenCluster = tokens.get(tokens.size() - 1);
								} else {
									tokens.add(tokenCluster);
								}
							}
							tokenCluster.add(conllLine);
							lastLine = conllLine;
						}

						List<TokenCluster> newTokens = new ArrayList<TokenCluster>();
						for (TokenCluster tokenCluster : tokens) {
							if (tokenCluster.size() > 1) {
								boolean split = false;
								String posTags = "";
								String word = "";
								for (ConllLine conllLine : tokenCluster) {
									posTags += conllLine.posTag2 + "|";
									word += conllLine.word + " ";
								}

								Integer countObj = compoundPatternCounts.get(posTags);
								int count = countObj == null ? 0 : countObj.intValue();
								count++;
								compoundPatternCounts.put(posTags, count);

								if (convertCompounds) {
									split = true;
									if (posTags.equals("NC|ADJ|")) {
										tokenCluster.head = 0;
										tokenCluster.get(1).governor = tokenCluster.get(0).index;
										tokenCluster.get(1).label = "mod";
										tokenCluster.get(1).copyGovernor();
									} else if (posTags.equals("NC|NC|")) {
										tokenCluster.head = 0;
										tokenCluster.get(1).governor = tokenCluster.get(0).index;
										tokenCluster.get(1).label = "mod";
										tokenCluster.get(1).copyGovernor();
									} else if (posTags.equals("NC|ADJ|ADJ|")) {
										tokenCluster.head = 0;
										tokenCluster.get(1).governor = tokenCluster.get(0).index;
										tokenCluster.get(1).label = "mod";
										tokenCluster.get(1).copyGovernor();
										tokenCluster.get(2).governor = tokenCluster.get(0).index;
										tokenCluster.get(2).label = "mod";
										tokenCluster.get(2).copyGovernor();
									} else if (posTags.equals("NC|P|NC|") || posTags.equals("NC|P+D|NC|") || posTags.equals("NC|P|NPP|")
											|| posTags.equals("NC|P+D|NPP|")) {
										tokenCluster.head = 0;
										tokenCluster.get(1).governor = tokenCluster.get(0).index;
										tokenCluster.get(1).label = "dep";
										tokenCluster.get(1).copyGovernor();
										tokenCluster.get(2).governor = tokenCluster.get(1).index;
										tokenCluster.get(2).label = "prep";
										tokenCluster.get(2).copyGovernor();
									} else if (posTags.equals("NC|P|DET|NC|")) {
										tokenCluster.head = 0;
										tokenCluster.get(1).governor = tokenCluster.get(0).index;
										tokenCluster.get(1).label = "dep";
										tokenCluster.get(1).copyGovernor();
										tokenCluster.get(2).governor = tokenCluster.get(3).index;
										tokenCluster.get(2).label = "det";
										tokenCluster.get(2).copyGovernor();
										tokenCluster.get(3).governor = tokenCluster.get(1).index;
										tokenCluster.get(3).label = "prep";
										tokenCluster.get(3).copyGovernor();
									} else if (posTags.equals("NC|P|NC|ADJ|") || posTags.equals("NC|P+D|NC|ADJ|")) {
										tokenCluster.head = 0;
										tokenCluster.get(1).governor = tokenCluster.get(0).index;
										tokenCluster.get(1).label = "dep";
										tokenCluster.get(1).copyGovernor();
										tokenCluster.get(2).governor = tokenCluster.get(1).index;
										tokenCluster.get(2).label = "prep";
										tokenCluster.get(2).copyGovernor();
										tokenCluster.get(3).governor = tokenCluster.get(2).index;
										tokenCluster.get(3).label = "mod";
										tokenCluster.get(3).copyGovernor();
									} else if (posTags.equals("NC|ADJ|P|NC|") || posTags.equals("NC|ADJ|P+D|NC|")) {
										tokenCluster.head = 0;
										tokenCluster.get(1).governor = tokenCluster.get(0).index;
										tokenCluster.get(1).label = "mod";
										tokenCluster.get(1).copyGovernor();
										tokenCluster.get(2).governor = tokenCluster.get(0).index;
										tokenCluster.get(2).label = "dep";
										tokenCluster.get(2).copyGovernor();
										tokenCluster.get(3).governor = tokenCluster.get(2).index;
										tokenCluster.get(3).label = "prep";
										tokenCluster.get(3).copyGovernor();
									} else if (posTags.equals("ADJ|NC|")) {
										tokenCluster.head = 1;
										if (tokenCluster.get(1).governor == tokenCluster.get(0).index) {
											tokenCluster.get(1).governor = tokenCluster.get(0).governor;
											tokenCluster.get(1).label = tokenCluster.get(0).label;
											tokenCluster.get(1).projGov = tokenCluster.get(0).projGov;
											tokenCluster.get(1).projLabel = tokenCluster.get(0).projLabel;
										}

										tokenCluster.get(0).governor = tokenCluster.get(1).index;
										tokenCluster.get(0).label = "mod";
										tokenCluster.get(0).copyGovernor();
									} else {
										if (posTags.equals("DET|PONCT|DET|") || posTags.equals("DET|DET|")) {
											// do nothing
										} else {
											LOG.debug(posTags + ": " + word);
										}

										split = false;
									}

									if (split) {
										for (ConllLine conllLine : tokenCluster) {
											conllLine.removeMweHead();
										}
									}
								}

								if (!compressCompounds)
									split = true;

								if (split) {
									if (tokenCluster.head != 0) {
										int oldIndex = tokenCluster.get(0).index;
										int newIndex = tokenCluster.get(tokenCluster.head).index;
										for (ConllLine conllLine : lines) {
											if (conllLine.governor == oldIndex) {
												conllLine.governor = newIndex;
											}
											if (conllLine.projGov == oldIndex) {
												conllLine.projGov = newIndex;
											}
										}
									}
									for (ConllLine conllLine : tokenCluster) {
										TokenCluster newCluster = new TokenCluster();
										newCluster.add(conllLine);
										newTokens.add(newCluster);
									}
								} else {
									String compPosTag = null;
									for (ConllLine conllLine : tokenCluster) {
										if (conllLine.compPosTag != null) {
											compPosTag = conllLine.compPosTag;
											break;
										}
									}
									if (compPosTag == null) {
										throw new RuntimeException("Didn't find compPosTag on line: " + tokenCluster.get(0).lineNumber);
									}

									ConllLine head = null;
									for (ConllLine conllLine : tokenCluster) {
										if (!conllLine.label.equals("dep_cpd")) {
											head = conllLine;
											break;
										}
									}
									if (head == null) {
										throw new RuntimeException("Didn't find head on line: " + tokenCluster.get(0).lineNumber);
									}

									tokenCluster.get(0).posTag2 = compPosTag;
									tokenCluster.get(0).posTag = compPosTag;
									tokenCluster.get(0).governor = head.governor;
									tokenCluster.get(0).label = head.label;
									tokenCluster.get(0).projGov = head.projGov;
									tokenCluster.get(0).projLabel = head.projLabel;
									tokenCluster.get(0).removeMweHead();

									if (compPosTag.equals("NC") || compPosTag.equals("NPP")) {
										tokenCluster.get(0).posTag = "N";
									} else if (compPosTag.startsWith("V")) {
										tokenCluster.get(0).posTag = "V";
									} else if (compPosTag.startsWith("PRO")) {
										tokenCluster.get(0).posTag = "PRO";
									} else if (compPosTag.startsWith("ADJ")) {
										tokenCluster.get(0).posTag = "A";
									} else if (compPosTag.startsWith("DET")) {
										tokenCluster.get(0).posTag = "D";
									} else if (compPosTag.startsWith("CL")) {
										tokenCluster.get(0).posTag = "CL";
									} else if (compPosTag.startsWith("C")) {
										tokenCluster.get(0).posTag = "C";
									}

									newTokens.add(tokenCluster);
								}
							} else {
								newTokens.add(tokenCluster);
							} // multi-token cluster?
						}
						tokens = newTokens;

						int currentIndex = 1;
						Map<Integer, Integer> indexMap = new HashMap<Integer, Integer>();
						indexMap.put(0, 0);
						for (TokenCluster tokenCluster : tokens) {
							tokenCluster.newIndex = currentIndex++;
							for (ConllLine conllLine : tokenCluster) {
								indexMap.put(conllLine.index, tokenCluster.newIndex);
							}
							tokenCluster.word = tokenCluster.get(0).word;
							tokenCluster.lemma = tokenCluster.get(0).lemma;
							for (int i = 1; i < tokenCluster.size(); i++) {
								ConllLine conllLine = tokenCluster.get(i);

								if (tokenCluster.word.length() == 0 || tokenCluster.word.endsWith("'") || tokenCluster.word.endsWith("-")
										|| tokenCluster.word.endsWith(",") || conllLine.word.startsWith("-") || conllLine.word.equals(",")) {
									tokenCluster.word += conllLine.word;
								} else {
									tokenCluster.word += "_" + conllLine.word;
								}
							}

							if (tokenCluster.size() > 1) {
								tokenCluster.lemma = tokenCluster.word;
								if (Character.isUpperCase(tokenCluster.lemma.charAt(0))) {
									if (!Character.isUpperCase(tokenCluster.get(0).lemma.charAt(0))) {
										tokenCluster.lemma = tokenCluster.get(0).lemma.charAt(0) + tokenCluster.lemma.substring(1);
									}
								}
							}
						}

						List<ConllLine> newLines = new ArrayList<SpmrlConverter.ConllLine>();
						for (TokenCluster tokenCluster : tokens) {
							ConllLine conllLine = tokenCluster.get(0);

							if (conllLine.posTag2 == null || conllLine.posTag2.equals("null") || conllLine.posTag2.equals("UNK")) {
								throw new RuntimeException("Bad postag on line: " + lineNumber + ": " + conllLine);
							}
							newLineNumber++;
							String newLine = tokenCluster.newIndex + "\t" + tokenCluster.word + "\t" + tokenCluster.lemma + "\t" + conllLine.posTag + "\t"
									+ conllLine.posTag2 + "\t" + conllLine.morph + "\t" + indexMap.get(conllLine.governor) + "\t" + conllLine.label + "\t"
									+ indexMap.get(conllLine.projGov) + "\t" + conllLine.projLabel;
							ConllLine newConllLine = new ConllLine(newLine, lineNumber, newLineNumber);
							newLines.add(newConllLine);
						}

						for (ConllLine conllLine : newLines) {
							if (conllLine.word.toLowerCase().equals("car") && conllLine.posTag2.equals("CC")) {
								conllLine.posTag2 = "CS";
								conllLine.morph = "s=s";
								if (conllLine.label.equals("coord")) {
									conllLine.label = "mod";
								}
								if (conllLine.projLabel.equals("coord")) {
									conllLine.projLabel = "mod";
								}
								for (ConllLine otherLine : newLines) {
									if (otherLine.governor == conllLine.index && otherLine.label.equals("dep_coord")) {
										otherLine.label = "sub";
									}
									if (otherLine.projGov == conllLine.index && otherLine.projLabel.equals("dep_coord")) {
										otherLine.projLabel = "sub";
									}
								}
							}
						}

						int i = 0;
						boolean hasNonProjective = false;
						for (ConllLine conllLine : newLines) {
							i++;
							int headIndex = conllLine.projGov;
							int depIndex = conllLine.index;
							int startIndex = headIndex < depIndex ? headIndex : depIndex;
							int endIndex = headIndex >= depIndex ? headIndex : depIndex;
							int j = 0;
							for (ConllLine otherLine : newLines) {
								j++;
								if (j <= i)
									continue;

								int headIndex2 = otherLine.projGov;
								int depIndex2 = otherLine.index;
								int startIndex2 = headIndex2 < depIndex2 ? headIndex2 : depIndex2;
								int endIndex2 = headIndex2 >= depIndex2 ? headIndex2 : depIndex2;
								boolean nonProjective = false;
								if (startIndex2 < startIndex && endIndex2 > startIndex && endIndex2 < endIndex) {
									nonProjective = true;
								} else if (startIndex2 > startIndex && startIndex2 < endIndex && endIndex2 > endIndex) {
									nonProjective = true;
								}
								if (nonProjective) {
									LOG.error("Non-projective arcs at line: " + lineNumber);
									LOG.error(conllLine.lineNumber + ": " + conllLine.toString());
									LOG.error(otherLine.lineNumber + ": " + otherLine.toString());
									hasNonProjective = true;
									nonProjectiveCount++;
								}
							}
						}

						for (ConllLine conllLine : newLines) {
							writer.write(conllLine.toString() + "\n");
						}

						newLineNumber++;
						writer.write("\n");
						writer.flush();

						if (errorOnNonProjective && hasNonProjective)
							throw new RuntimeException("Found non projective arc");

						lines = new ArrayList<SpmrlConverter.ConllLine>();
					} else {
						ConllLine conllLine = new ConllLine(line, lineNumber, lineNumber);
						lines.add(conllLine);
					}
				}
				scanner.close();
				writer.close();

				Set<WeightedOutcome<String>> counts = new TreeSet<WeightedOutcome<String>>();
				for (String posTags : compoundPatternCounts.keySet()) {
					counts.add(new WeightedOutcome<String>(posTags, compoundPatternCounts.get(posTags)));
				}
				for (WeightedOutcome<String> count : counts) {
					LOG.info(count.getOutcome() + ": " + count.getWeight());
				}
				LOG.info("non projective count: " + nonProjectiveCount);
			} catch (Exception e) {
				LogUtils.logError(LOG, e);
			}
		}
	}

	private static final class TokenCluster extends ArrayList<ConllLine> {
		private static final long serialVersionUID = 1L;
		public int head;
		public int newIndex;
		public String word;
		public String lemma;
	}

	private static final class ConllLine {
		public ConllLine(String line, int lineNumber, int newLineNumber) {
			String[] parts = line.split("\t");
			index = Integer.parseInt(parts[0]);
			word = parts[1];
			lemma = parts[2];

			if (word.equals("-LRB-")) {
				word = "(";
				lemma = "(";
			} else if (word.equals("-RRB-")) {
				word = ")";
				lemma = ")";
			} else if (word.equals("-LSB-")) {
				word = "[";
				lemma = "[";
			} else if (word.equals("-RSB-")) {
				word = "]";
				lemma = "]";
			} else if (word.equals("-LRB-...-RRB-")) {
				word = "(...)";
				lemma = "(...)";
			}

			posTag = parts[3];
			posTag2 = parts[4];

			if (posTag2.equals("ADJWH")) {
				posTag2 = "DETWH";
				posTag = "D";
			}

			if (posTagSet != null) {
				// check if postag exists
				try {
					posTagSet.getPosTag(posTag2);
				} catch (UnknownPosTagException e) {
					throw new TalismaneException("Unknown postag on line " + lineNumber + ", " + posTag2 + ": " + line);
				}
			}

			morph = parts[5];
			if (morph.contains("mwehead")) {
				int mweheadPos = morph.indexOf("mwehead");
				int nextPart = morph.indexOf('|', mweheadPos);
				compPosTag = morph.substring(mweheadPos + "mwehead".length() + 1, nextPart - 1);
				if (compPosTag.equals("ADJWH"))
					compPosTag = "DETWH";
				if (posTagSet != null) {
					// check if compound postag exists
					try {
						posTagSet.getPosTag(posTag2);
					} catch (UnknownPosTagException e) {
						throw new TalismaneException("Unknown compound postag on line " + lineNumber + ", " + compPosTag + ": " + line);
					}
				}
			}

			governor = Integer.parseInt(parts[6]);
			label = parts[7].replace('.', '_');
			if (label.equals("obj_cpl"))
				label = "sub";
			else if (label.equals("obj_p"))
				label = "prep";

			projGov = Integer.parseInt(parts[8]);
			projLabel = parts[9].replace('.', '_');
			if (projLabel.equals("obj_cpl"))
				projLabel = "sub";
			else if (projLabel.equals("obj_p"))
				projLabel = "prep";

			this.lineNumber = newLineNumber;
		}

		int index;
		String word;
		String lemma;
		String posTag;
		String posTag2;
		String compPosTag = null;
		String morph;
		int governor;
		String label;
		int projGov;
		String projLabel;

		int lineNumber;

		void copyGovernor() {
			this.projGov = this.governor;
			this.projLabel = this.label;
		}

		void removeMweHead() {
			if (morph.contains("mwehead")) {
				int mweheadPos = morph.indexOf("mwehead");
				int nextPart = morph.indexOf('|', mweheadPos);
				if (mweheadPos == 0) {
					morph = morph.substring(nextPart + 1);
				} else {
					morph = morph.substring(0, mweheadPos) + morph.substring(nextPart + 1);
				}
			}
		}

		@Override
		public String toString() {
			String string = this.index + "\t" + this.word + "\t" + this.lemma + "\t" + this.posTag + "\t" + this.posTag2 + "\t" + this.morph + "\t"
					+ this.governor + "\t" + this.label + "\t" + this.projGov + "\t" + this.projLabel;
			return string;
		}
	}
}
