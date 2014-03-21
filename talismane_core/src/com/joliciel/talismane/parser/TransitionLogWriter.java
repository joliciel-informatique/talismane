package com.joliciel.talismane.parser;

import java.io.IOException;
import java.io.Writer;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.joliciel.talismane.posTagger.PosTaggedToken;
import com.joliciel.talismane.utils.LogUtils;

class TransitionLogWriter implements ParseConfigurationProcessor {
	private static final Log LOG = LogFactory.getLog(TransitionLogWriter.class);
	ParserServiceInternal parserServiceInternal;
	Writer csvFileWriter = null;

	public TransitionLogWriter(Writer csvFileWriter) {
		super();
		this.csvFileWriter = csvFileWriter;
	}
	
	@Override
	public void onNextParseConfiguration(ParseConfiguration parseConfiguration,
			Writer writer) {
		try {
			ParseConfiguration currentConfiguration = parserServiceInternal.getInitialConfiguration(parseConfiguration.getPosTagSequence());
			
			csvFileWriter.write("\n");
			csvFileWriter.write("\t"
					+ this.getTopOfStack(currentConfiguration) + "\t"
					+ this.getTopOfBuffer(currentConfiguration) + "\t"
					+ "\n");
			Set<DependencyArc> dependencies = new HashSet<DependencyArc>();
			for (Transition transition : parseConfiguration.getTransitions()) {
				currentConfiguration = parserServiceInternal.getConfiguration(currentConfiguration);
				transition.apply(currentConfiguration);
				DependencyArc newDep = null;
				if (currentConfiguration.getDependencies().size()>dependencies.size()) {
					for (DependencyArc arc : currentConfiguration.getDependencies()) {
						if (dependencies.contains(arc)) {
							continue;
						} else {
							dependencies.add(arc);
							newDep = arc;
							break;
						}
					}
				}
				String newDepText = "";
				if (newDep!=null) {
					newDepText = newDep.getLabel()
						+ "[" + newDep.getHead().getToken().getOriginalText().replace(' ', '_') + "|" + newDep.getHead().getTag().getCode()
						+ "," + newDep.getDependent().getToken().getOriginalText().replace(' ', '_') + "|" + newDep.getDependent().getTag().getCode()
						+ "]";
				}
				csvFileWriter.write(transition.getCode() + "\t"
						+ this.getTopOfStack(currentConfiguration) + "\t"
						+ this.getTopOfBuffer(currentConfiguration) + "\t"
						+ newDepText + "\n");
			}
			csvFileWriter.flush();
		} catch (IOException e) {
			LogUtils.logError(LOG, e);
			throw new RuntimeException(e);
		}
	}
	
	private String getTopOfStack(ParseConfiguration configuration) {
		StringBuilder sb = new StringBuilder();
		Iterator<PosTaggedToken> stackIterator = configuration.getStack().iterator();
		int i=0;
		while (stackIterator.hasNext()) {
			if (i==5) {
				sb.insert(0, "... ");
				break;
			}
			
			PosTaggedToken token = stackIterator.next();
			sb.insert(0, token.getToken().getOriginalText().replace(' ', '_') + "|" + token.getTag().getCode() + " ");
			i++;
		}
		return sb.toString();
	}
	
	private String getTopOfBuffer(ParseConfiguration configuration) {
		StringBuilder sb = new StringBuilder();
		Iterator<PosTaggedToken> bufferIterator = configuration.getBuffer().iterator();
		int i=0;
		while (bufferIterator.hasNext()) {
			if (i==5) {
				sb.append(" ...");
				break;
			}
			
			PosTaggedToken token = bufferIterator.next();
			sb.append(" " + token.getToken().getOriginalText().replace(' ', '_') + "|" + token.getTag().getCode());
			i++;

		}
		return sb.toString();
	}


	
	@Override
	public void onCompleteParse() {
		try {
			this.csvFileWriter.flush();
			this.csvFileWriter.close();
		} catch (IOException e) {
			LogUtils.logError(LOG, e);
			throw new RuntimeException(e);
		}
	}

	public ParserServiceInternal getParserServiceInternal() {
		return parserServiceInternal;
	}

	public void setParserServiceInternal(ParserServiceInternal parserServiceInternal) {
		this.parserServiceInternal = parserServiceInternal;
	}


}
