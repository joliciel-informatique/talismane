package com.joliciel.talismane.fr;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Scanner;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.PropertyConfigurator;

import com.joliciel.talismane.utils.CSVFormatter;
import com.joliciel.talismane.utils.StringUtils;

/**
 * Assuming a 1st-conjunct headed coordinated structure (SPMRL default),
 * converts to another equivalent annotation for coordination.
 * @author Assaf Urieli
 *
 */
public class SpmrlCoordinationAnnotator {
	@SuppressWarnings("unused")
	private static CSVFormatter CSV = new CSVFormatter(4);
	@SuppressWarnings("unused")
	private static NumberFormat format = NumberFormat.getInstance(Locale.ENGLISH);

	private static final Log LOG = LogFactory.getLog(SpmrlCoordinationAnnotator.class);
	
	private static enum Option {
		PrevConjunctGov,
		ConjunctionGov,
		PrevConjunctGovNoCommas,
		None
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		Map<String, String> argMap = StringUtils.convertArgs(args);
		SpmrlCoordinationAnnotator finder = new SpmrlCoordinationAnnotator();
		String command="find";
		if (argMap.containsKey("command")) {
			command = argMap.get("command");
		}
		argMap.remove("command");
		if (command.equals("find"))
			finder.calc(argMap);

	}
	
	public void calc(Map<String, String> argMap) throws Exception {
		
		String logConfigPath = argMap.get("logConfigFile");
		if (logConfigPath!=null) {
			argMap.remove("logConfigFile");
			Properties props = new Properties();
			props.load(new FileInputStream(logConfigPath));
			PropertyConfigurator.configure(props);
		}
		
		String refFilePath = null;
		Option option = Option.None;
		boolean fixCommas = false;
		
		for (String argName : argMap.keySet()) {
			String argValue = argMap.get(argName);
			if (argName.equals("refFile")) {
				refFilePath = argValue;
			} else if (argName.equals("fixCommas")) {
				fixCommas = argValue.equalsIgnoreCase("true");
			} else if (argName.equals("option")) {
				option = Option.valueOf(argValue);
			} else {
				throw new RuntimeException("Unknown option: " + argName);
			}
		}
		
		if ((option==Option.PrevConjunctGovNoCommas || option==Option.ConjunctionGov) && !fixCommas) {
			throw new RuntimeException("Option " + option + " only valid with fixCommas=true");
		}
		
		File refFile = new File(refFilePath);
		
		String baseName = refFile.getName();
		if (baseName.indexOf('.')>=0) {
			baseName = baseName.substring(0, baseName.lastIndexOf('.'));
		}
		
		String extension = "";
		if (refFile.getName().indexOf('.')>=0) {
			extension = refFile.getName().substring(refFile.getName().lastIndexOf('.'));
		}
		
		String suffix = "";
		switch (option) {
			case ConjunctionGov: suffix="CH"; break;
			case None: suffix="1H"; break;
			case PrevConjunctGov: suffix = "PH"; break;
			case PrevConjunctGovNoCommas: suffix = "PH2"; break;
		}
		
		if (fixCommas) suffix += "+P";
		
		String outFileName = baseName + "_" + suffix + extension;

		File outFile = new File(refFile.getParentFile(), outFileName);
		Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outFile), "UTF-8"));

		Scanner refFileScanner = new Scanner(new BufferedReader(new InputStreamReader(new FileInputStream(refFile), "UTF-8")));
		
		int lineCount = 0;
		
		String rootLineString = "0\tROOT\tROOT\tROOT\troot\troot\t-1\troot\t-1\troot";

		ConllLine rootLine = new ConllLine(rootLineString, -1);
		List<ConllLine> refLines = new ArrayList<ConllLine>();
		refLines.add(rootLine);
		
		while (refFileScanner.hasNextLine()) {
			lineCount++;

			String nextRefLine = refFileScanner.nextLine();
			
			if (nextRefLine.length()>0) {
				ConllLine refLine = new ConllLine(nextRefLine, lineCount);

				refLines.add(refLine);
			}
			
			if (nextRefLine.length()==0 || !refFileScanner.hasNextLine()) {
				// construct dependency map
				Map<Integer,List<ConllLine>> refDependencyMap = this.getDependencyMap(refLines);

				for (int j=1; j<refLines.size(); j++) {
					ConllLine refLine = refLines.get(j);
					ConllLine govLine = refLines.get(refLine.governor);
					if (govLine.label.equals("coord") && govLine.posTag.equals("CC") && refLine.label.equals("dep")) {
						refLine.nonProjectiveLabel="dep.coord";
						refLine.label="dep.coord";
					}
				}
				
				
				for (int j=1; j<refLines.size(); j++) {
					ConllLine refLine = refLines.get(j);
					if (refLine.label.equals("coord")||refLine.label.equals("dep.coord")) {
						if (refLine.governor>refLine.index) {
							LOG.info("Forward coord: Line: " +  refLine.lineNumber + ": " + refLine.toString());
						}
					}
				}
				
				// first we fix the conjuncts so that we're sure each depends on the previous conjunct
				for (int j=1; j<refLines.size(); j++) {
					if (option!=Option.None) {
						ConllLine refLine = refLines.get(j);
						int coordinatorCount = 0;
						List<ConllLine> refDeps = refDependencyMap.get(refLine.index);
						List<ConllLine> coordinators = new ArrayList<SpmrlCoordinationAnnotator.ConllLine>();
						int lastCoordinatorIndex = -1;
						for (ConllLine dep : refDeps) {
							if (dep.label.equals("coord")) {
								lastCoordinatorIndex = coordinators.size();
								coordinators.add(dep);
								coordinatorCount++;
							} else {
								coordinators.add(dep);
							}
						}
						while (coordinators.size()>lastCoordinatorIndex+1)
							coordinators.remove(lastCoordinatorIndex+1);
						
						if (coordinatorCount>1) {
							LOG.debug("### fix ###");
							LOG.debug(refLine);
							ConllLine lastConjunct = refLine;
							for (ConllLine coordinator : coordinators) {
								if (coordinator.label.equals("coord")) {
									LOG.debug("Line: " + coordinator.lineNumber);
									LOG.debug("Was: " + coordinator);
									List<ConllLine> coordinatorDeps = refDependencyMap.get(coordinator.index);
									ConllLine nextConjunct = null;
									int conjunctCount = 0;
									for (ConllLine dep : coordinatorDeps) {
										if (dep.label.equals("dep.coord")) {
											nextConjunct = dep;
											conjunctCount++;
										}
									}
									if (conjunctCount>1)
										nextConjunct = null;
									
									coordinator.nonProjectiveGovernor = lastConjunct.index;
									coordinator.governor = lastConjunct.index;
									LOG.debug("Now: " + coordinator);
									if (nextConjunct!=null) {
										lastConjunct = nextConjunct;
									}
									LOG.debug(nextConjunct);
								} else if (coordinator.label.equals("ponct")) {
									// non coordinators
									coordinator.nonProjectiveGovernor = lastConjunct.index;
									coordinator.governor = lastConjunct.index;
								} else {
									// non coordinators, retain non-projective dependency on first conjunct just in case it was right
									coordinator.governor = lastConjunct.index;
								}
							}
						} // more than one coordinator
					} // option != None
				} // next refline
				
				refDependencyMap = this.getDependencyMap(refLines);
				
				if (option==Option.ConjunctionGov) {
					for (int j=1; j<refLines.size(); j++) {
					
						ConllLine refLine = refLines.get(j);
						// first decide if the refLine is the last coordinator in a coordination (either the conjunction, or the last comma)
						boolean isLastCoordinator = false;
						if (refLine.posTag.equals("CC") && (refLine.label.equals("coord")||refLine.label.equals("root"))) {
							// we need to exclude the second conjunction in et/ou, which has no conjuncts
							for (ConllLine dep : refDependencyMap.get(refLine.index)) {
								if (dep.label.equals("dep.coord")) {
									isLastCoordinator = true;
									break;
								}
							}
						} else if (refLine.posTag.equals("PONCT") && refLine.label.equals("coord")) {
							List<ConllLine> coordinators = new ArrayList<SpmrlCoordinationAnnotator.ConllLine>();
							this.addCoordinators(refLine, coordinators, refDependencyMap);
							if (coordinators.size()==1)
								isLastCoordinator=true;
						}
						
						if (isLastCoordinator) {
							// find all conjuncts
							List<ConllLine> conjuncts = new ArrayList<SpmrlCoordinationAnnotator.ConllLine>();
							List<ConllLine> coordinators = new ArrayList<SpmrlCoordinationAnnotator.ConllLine>();
							
							if (!refLine.label.equals("root")) {
								ConllLine firstConjunct = refLines.get(refLine.governor);
								while (firstConjunct.label.equals("dep.coord")) {
									ConllLine firstConjunctGov = refLines.get(firstConjunct.governor);
									if (!firstConjunctGov.posTag.equals("CC"))
										firstConjunct = refLines.get(firstConjunctGov.governor);
									else
										break;
								}
								this.addConjuncts(firstConjunct, conjuncts, coordinators, refDependencyMap);

								refLine.nonProjectiveGovernor = firstConjunct.nonProjectiveGovernor;
								refLine.nonProjectiveLabel = firstConjunct.nonProjectiveLabel;
								refLine.governor = firstConjunct.governor;
								refLine.label = firstConjunct.label;
							}
							
							List<ConllLine> coordDeps = refDependencyMap.get(refLine.index);
							for (ConllLine coordDep : coordDeps) {
								if (coordDep.label.equals("dep.coord"))
									conjuncts.add(coordDep);
							}
							
							for (ConllLine conjunct : conjuncts) {
								conjunct.nonProjectiveGovernor = refLine.index;
								conjunct.nonProjectiveLabel = "coord";
								conjunct.governor = refLine.index;
								conjunct.label = "coord";
								// crazy case of et/ou:
								if (conjunct.posTag.equals("CC") && conjunct.index < refLine.index) {
									Map<Integer,List<ConllLine>> newDepMap = this.getDependencyMap(refLines);
									for (ConllLine dep : newDepMap.get(conjunct.index)) {
										if (dep.label.equals("coord")) {
											dep.nonProjectiveGovernor = refLine.index;
											dep.governor = refLine.index;
										}
									}							
								}
							}
							
							for (ConllLine coordinator : coordinators) {
								if (coordinator.index!=refLine.index) {
									int govIndex = coordinator.index-1;
									for (int k=coordinator.index-1; k>=0; k--) {
										if (!fixCommas || !refLines.get(k).posTag.equals("PONCT")) {
											govIndex = k;
											break;
										}
									}
									coordinator.nonProjectiveGovernor = govIndex;
									coordinator.governor = govIndex;
									coordinator.nonProjectiveLabel = "ponct";
									coordinator.label = "ponct";
								}
							}
							
							for (ConllLine conjunct : conjuncts) {
								if (conjunct.index < refLine.index) {
									List<ConllLine> conjunctDeps = refDependencyMap.get(conjunct.index);
									for (ConllLine conjunctDep : conjunctDeps) {
										if (conjunctDep.index > refLine.index) {
											conjunctDep.nonProjectiveGovernor = refLine.index;
											conjunctDep.governor = refLine.index;
										}
									}
								}
							}
						}
					} // next refLine
				} else if (option==Option.PrevConjunctGovNoCommas) {
					for (int j=1; j<refLines.size(); j++) {
						ConllLine refLine = refLines.get(j);
						if (refLine.posTag.equals("PONCT") && refLine.label.equals("coord")) {
							int prevCoord = refLine.governor;
							for (ConllLine dep : refDependencyMap.get(refLine.index)) {
								if (!dep.posTag.equals("PONCT")) {
									if (!dep.label.equals("dep.coord"))
										LOG.debug(dep.lineNumber + ": " + dep.toString());
									dep.nonProjectiveGovernor = prevCoord;
									dep.nonProjectiveLabel = "coord";
									dep.governor = prevCoord;
									dep.label = "coord";
								}
							}
							
							int govIndex = refLine.index-1;
							for (int k=refLine.index-1; k>=0; k--) {
								if (!refLines.get(k).posTag.equals("PONCT")) {
									govIndex = k;
									break;
								}
							}
							refLine.nonProjectiveGovernor = govIndex;
							refLine.governor = govIndex;
							refLine.nonProjectiveLabel = "ponct";
							refLine.label = "ponct";
						}
					}
				} // option = PrevConjunctGovNoCommas
				
				refDependencyMap = this.getDependencyMap(refLines);

				if (fixCommas) {
					for (int j=1; j<refLines.size(); j++) {
						ConllLine refLine = refLines.get(j);
						if (refLine.posTag.equals("PONCT")) {
							boolean fixThisLine = true;
							if (refLine.label.equals("dep_cpd"))
								fixThisLine = false;
							
							if (fixThisLine) {
								if (option==Option.ConjunctionGov) {
									for (ConllLine dep : refDependencyMap.get(refLine.index)) {
										if (dep.label.equals("coord")) {
											fixThisLine = false;
											break;
										}
									}
								} else {
									fixThisLine = !refLine.label.equals("coord");
								}
							}
							
							if (fixThisLine) {
								int govIndex = refLine.index-1;
								for (int k=refLine.index-1; k>=0; k--) {
									if (!refLines.get(k).posTag.equals("PONCT")) {
										govIndex = k;
										break;
									}
									if (refLines.get(k).posTag.equals("PONCT") && !refLines.get(k).label.equals("ponct")) {
										govIndex = k;
										break;
									}
								}
								refLine.nonProjectiveGovernor = govIndex;
								refLine.governor = govIndex;
								refLine.nonProjectiveLabel = "ponct";
								refLine.label = "ponct";
							}
						}
					}
				}
				
				for (ConllLine refLine : refLines) {
					if (refLine.index==0)
						continue;
					writer.write(refLine.toString() + "\n");
				}
				writer.write("\n");
				refLines = new ArrayList<ConllLine>();
				refLines.add(rootLine);
			} // next sentence

		} // next line
		
		writer.flush();
		writer.close();
	}
	
	private void addConjuncts(ConllLine conjunct, List<ConllLine> conjuncts, List<ConllLine> coordinators, Map<Integer,List<ConllLine>> refDependencyMap) {
		conjuncts.add(conjunct);
		List<ConllLine> deps = refDependencyMap.get(conjunct.index);
		for (ConllLine dep : deps) {
			if (dep.label.equals("coord") && dep.posTag.equals("PONCT")) {
				coordinators.add(dep);
				List<ConllLine> coordDeps = refDependencyMap.get(dep.index);
				for (ConllLine coordDep : coordDeps) {
					if (coordDep.label.equals("dep.coord"))
						this.addConjuncts(coordDep, conjuncts, coordinators, refDependencyMap);
				}
			}
		}
	}
	
	private void addCoordinators(ConllLine coordinator, List<ConllLine> coordinators, Map<Integer,List<ConllLine>> refDependencyMap) {
		coordinators.add(coordinator);
		List<ConllLine> deps = refDependencyMap.get(coordinator.index);
		for (ConllLine dep : deps) {
			if (dep.label.equals("dep.coord")) {				
				List<ConllLine> depCoordDeps = refDependencyMap.get(dep.index);
				for (ConllLine depCoordDep : depCoordDeps) {
					if (depCoordDep.label.equals("coord"))
						this.addCoordinators(depCoordDep, coordinators, refDependencyMap);
				}
			}
		}
	}
	
	private Map<Integer,List<ConllLine>> getDependencyMap(List<ConllLine> refLines) {
		Map<Integer,List<ConllLine>> refDependencyMap = new HashMap<Integer, List<ConllLine>>();
		for (int j=0; j<refLines.size(); j++) {
			refDependencyMap.put(j, new ArrayList<ConllLine>());
		}
		
		for (int j=1; j<refLines.size(); j++) {
			ConllLine refLine = refLines.get(j);
			List<ConllLine> refDeps = refDependencyMap.get(refLine.governor);
			refDeps.add(refLine);				
		} // next line in sentence
		return refDependencyMap;
	}
	
	private static final class ConllLine {
		public ConllLine(String line, int lineNumber) {
			String[] parts = line.split("\t", -1);
			index = Integer.parseInt(parts[0]);
			word = parts[1];
			lemma = parts[2];
			if (lemma.equals("_"))
				lemma = word;
			cat = parts[3];
			posTag = parts[4];
			morphology = parts[5];
			nonProjectiveGovernor = Integer.parseInt(parts[6]);
			nonProjectiveLabel = parts[7];
			governor = Integer.parseInt(parts[8]);
			if (parts.length>=10)
				label = parts[9];

			for (int i=10; i<parts.length; i++) {
				extras += "\t" + parts[i];
			}

			this.lineNumber = lineNumber;
		}
		
		int index;
		String word;
		String lemma;
		String cat;
		String posTag;
		String morphology;
		int nonProjectiveGovernor;
		String nonProjectiveLabel;
		int governor;
		String label = "";
		String extras = "";

		int lineNumber;
		
		public String toString() {
			return index + "\t" + word + "\t" + lemma + "\t" + cat + "\t" + posTag + "\t" + morphology + "\t" + nonProjectiveGovernor + "\t" + nonProjectiveLabel + "\t" + governor + "\t" + label + extras;
		}
	}
}
