package com.joliciel.talismane.parser;

import java.io.IOException;
import java.io.Writer;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joliciel.talismane.posTagger.PosTagSequence;
import com.joliciel.talismane.posTagger.PosTaggedToken;
import com.joliciel.talismane.utils.LogUtils;

public class ParseEvaluationObserverImpl implements ParseEvaluationObserver {
	private static final Logger LOG = LoggerFactory.getLogger(ParseEvaluationObserverImpl.class);

	private final ParseConfigurationProcessor processor;
	Set<String> errorLabels = new HashSet<String>();
	Writer writer;

	public ParseEvaluationObserverImpl(ParseConfigurationProcessor processor) {
		this.processor = processor;
	}

	@Override
	public void onParseEnd(ParseConfiguration refConfiguration, List<ParseConfiguration> guessedConfigurations) {
		try {
			boolean includeMe = true;
			if (errorLabels != null && errorLabels.size() > 0) {
				includeMe = false;
				int i = 0;
				ParseConfiguration guessConfiguration = guessedConfigurations.get(0);

				Set<PosTaggedToken> refTokensToExplain = new HashSet<PosTaggedToken>();
				Set<PosTaggedToken> guessTokensToExplain = new HashSet<PosTaggedToken>();
				Set<PosTaggedToken> refTokensToHighlight = new HashSet<PosTaggedToken>();
				Set<PosTaggedToken> guessTokensToHighlight = new HashSet<PosTaggedToken>();
				for (PosTaggedToken refToken : refConfiguration.getPosTagSequence()) {
					if (i != 0) {
						DependencyArc refArc = refConfiguration.getGoverningDependency(refToken);
						if (refArc != null) {
							PosTaggedToken guessToken = guessConfiguration.getPosTagSequence().get(i);
							if (errorLabels.contains(refArc.getLabel())) {
								DependencyArc guessArc = guessConfiguration.getGoverningDependency(guessToken);
								if (guessArc == null || !refArc.getLabel().equals(guessArc.getLabel())
										|| (refArc.getHead() == null && guessArc.getHead() != null) || (refArc.getHead() != null && guessArc.getHead() == null)
										|| refArc.getHead().getIndex() != guessArc.getHead().getIndex()) {
									refTokensToExplain.add(refToken);
									if (refArc.getHead() != null)
										refTokensToHighlight.add(refArc.getHead());
									guessTokensToExplain.add(guessToken);
									if (guessArc != null && guessArc.getHead() != null)
										guessTokensToHighlight.add(guessArc.getHead());
									includeMe = true;
								}
							}
						} // have refArc
					}
					i++;
				}

				StringBuilder refBuilder = new StringBuilder();
				for (PosTaggedToken refToken : refConfiguration.getPosTagSequence()) {
					if (refTokensToExplain.contains(refToken)) {
						DependencyArc refArc = refConfiguration.getGoverningDependency(refToken);
						if (refArc == null)
							refBuilder.append("#" + refToken.getToken().getOriginalText().replace(' ', '_') + "|" + refToken.getTag().getCode() + "|"
									+ refToken.getIndex() + "|Gov0|null# ");
						else
							refBuilder.append("#" + refToken.getToken().getOriginalText().replace(' ', '_') + "|" + refToken.getTag().getCode() + "|"
									+ refToken.getIndex() + "|Gov" + (refArc.getHead() == null ? 0 : refArc.getHead().getIndex()) + "|" + refArc.getLabel()
									+ "# ");
					} else if (refTokensToHighlight.contains(refToken)) {
						refBuilder.append("#" + refToken.getToken().getOriginalText().replace(' ', '_') + "|" + refToken.getTag().getCode() + "|"
								+ refToken.getIndex() + "# ");
					} else {
						refBuilder.append(
								refToken.getToken().getOriginalText().replace(' ', '_') + "|" + refToken.getTag().getCode() + "|" + refToken.getIndex() + " ");
					}
				}
				StringBuilder guessBuilder = new StringBuilder();
				for (PosTaggedToken guessToken : guessConfiguration.getPosTagSequence()) {
					if (guessTokensToExplain.contains(guessToken)) {
						DependencyArc guessArc = guessConfiguration.getGoverningDependency(guessToken);
						if (guessArc == null)
							guessBuilder.append("#" + guessToken.getToken().getOriginalText().replace(' ', '_') + "|" + guessToken.getTag().getCode() + "|"
									+ guessToken.getIndex() + "|Gov0|null# ");
						else
							guessBuilder.append("#" + guessToken.getToken().getOriginalText().replace(' ', '_') + "|" + guessToken.getTag().getCode() + "|"
									+ guessToken.getIndex() + "|Gov" + (guessArc.getHead() == null ? 0 : guessArc.getHead().getIndex()) + "|"
									+ guessArc.getLabel() + "# ");
					} else if (guessTokensToHighlight.contains(guessToken)) {
						guessBuilder.append("#" + guessToken.getToken().getOriginalText().replace(' ', '_') + "|" + guessToken.getTag().getCode() + "|"
								+ guessToken.getIndex() + "# ");
					} else {
						guessBuilder.append(guessToken.getToken().getOriginalText().replace(' ', '_') + "|" + guessToken.getTag().getCode() + "|"
								+ guessToken.getIndex() + " ");
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

	@Override
	public void onParseStart(ParseConfiguration realConfiguration, List<PosTagSequence> posTagSequences) {
	}
}
