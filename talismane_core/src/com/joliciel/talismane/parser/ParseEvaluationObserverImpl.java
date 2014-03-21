package com.joliciel.talismane.parser;

import java.io.IOException;
import java.io.Writer;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.joliciel.talismane.posTagger.PosTaggedToken;
import com.joliciel.talismane.utils.LogUtils;

public class ParseEvaluationObserverImpl implements ParseEvaluationObserver {
	private static final Log LOG = LogFactory.getLog(ParseEvaluationObserverImpl.class);

	ParseConfigurationProcessor processor;
	Set<String> errorLabels = new HashSet<String>();
	Writer writer;
	
	public ParseEvaluationObserverImpl(ParseConfigurationProcessor processor) {
		super();
		this.processor = processor;
	}

	@Override
	public void onNextParseConfiguration(ParseConfiguration refConfiguration,
			List<ParseConfiguration> guessedConfigurations) {
		try {
			boolean includeMe = true;
			if (errorLabels!=null && errorLabels.size()>0) {
				includeMe = false;
				int i=0;
				ParseConfiguration guessConfiguration = guessedConfigurations.get(0);
				
				Set<PosTaggedToken> refTokensToExplain = new HashSet<PosTaggedToken>();
				Set<PosTaggedToken> guessTokensToExplain = new HashSet<PosTaggedToken>();
				Set<PosTaggedToken> refTokensToHighlight = new HashSet<PosTaggedToken>();
				Set<PosTaggedToken> guessTokensToHighlight = new HashSet<PosTaggedToken>();
				for (PosTaggedToken refToken : refConfiguration.getPosTagSequence()) {
					if (i!=0) {
						DependencyArc refArc = refConfiguration.getGoverningDependency(refToken);
						PosTaggedToken guessToken = guessConfiguration.getPosTagSequence().get(i);
						if (errorLabels.contains(refArc.getLabel())) {
							DependencyArc guessedArc = guessConfiguration.getGoverningDependency(guessToken);
							if (!guessedArc.getLabel().equals(refArc.getLabel())
									|| guessedArc.getHead().getIndex()!=refArc.getHead().getIndex()) {
								refTokensToExplain.add(refToken);
								refTokensToHighlight.add(refArc.getHead());
								guessTokensToExplain.add(guessToken);
								guessTokensToHighlight.add(guessedArc.getHead());
								includeMe = true;
							}
						}
					}
					i++;
				}
				
				StringBuilder refBuilder = new StringBuilder();
				for (PosTaggedToken refToken : refConfiguration.getPosTagSequence()) {
					if (refTokensToExplain.contains(refToken)) {
						DependencyArc refArc = refConfiguration.getGoverningDependency(refToken);
						refBuilder.append("#" + refToken.getToken().getOriginalText().replace(' ', '_') + "|" + refToken.getTag().getCode() + "|" + refToken.getIndex() + "|Gov" + refArc.getHead().getIndex() + "|" + refArc.getLabel() + "# ");					
					} else if (refTokensToHighlight.contains(refToken)) {
						refBuilder.append("#" + refToken.getToken().getOriginalText().replace(' ', '_') + "|" + refToken.getTag().getCode() + "|" + refToken.getIndex() + "# ");										
					} else {
						refBuilder.append(refToken.getToken().getOriginalText().replace(' ', '_') + "|" + refToken.getTag().getCode() + "|" + refToken.getIndex() + " ");										
					}
				}
				StringBuilder guessBuilder = new StringBuilder();
				for (PosTaggedToken guessToken : guessConfiguration.getPosTagSequence()) {
					if (guessTokensToExplain.contains(guessToken)) {
						DependencyArc guessArc = guessConfiguration.getGoverningDependency(guessToken);
						guessBuilder.append("#" + guessToken.getToken().getOriginalText().replace(' ', '_') + "|" + guessToken.getTag().getCode() + "|" + guessToken.getIndex() + "|Gov" + guessArc.getHead().getIndex() + "|" + guessArc.getLabel() + "# ");					
					} else if (guessTokensToHighlight.contains(guessToken)) {
						guessBuilder.append("#" + guessToken.getToken().getOriginalText().replace(' ', '_') + "|" + guessToken.getTag().getCode() + "|" + guessToken.getIndex() + "# ");										
					} else {
						guessBuilder.append(guessToken.getToken().getOriginalText().replace(' ', '_') + "|" + guessToken.getTag().getCode() + "|" + guessToken.getIndex() + " ");										
					}
				}
				if (includeMe) {
					writer.write("\n");
					writer.write(refBuilder.toString() + "\n");
					writer.write(guessBuilder.toString() + "\n");
				}
			}
			if (includeMe)
				processor.onNextParseConfiguration(guessedConfigurations.get(0), null);
		} catch (IOException e) {
			LogUtils.logError(LOG, e);
			throw new RuntimeException(e);			
		}
	}

	@Override
	public void onEvaluationComplete() {
		processor.onCompleteParse();
	}

	public Set<String> getErrorLabels() {
		return errorLabels;
	}

	public void setErrorLabels(Set<String> errorLabels) {
		this.errorLabels = errorLabels;
	}

	public Writer getWriter() {
		return writer;
	}

	public void setWriter(Writer writer) {
		this.writer = writer;
	}
}
