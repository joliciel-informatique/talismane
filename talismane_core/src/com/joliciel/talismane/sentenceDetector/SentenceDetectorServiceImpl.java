package com.joliciel.talismane.sentenceDetector;

import java.util.List;
import java.util.Set;

import com.joliciel.talismane.filters.TextFilter;
import com.joliciel.talismane.machineLearning.CorpusEventStream;
import com.joliciel.talismane.machineLearning.DecisionFactory;
import com.joliciel.talismane.machineLearning.DecisionMaker;
import com.joliciel.talismane.machineLearning.MachineLearningService;
import com.joliciel.talismane.sentenceDetector.features.SentenceDetectorFeature;
import com.joliciel.talismane.sentenceDetector.features.SentenceDetectorFeatureService;
import com.joliciel.talismane.tokeniser.TokeniserService;

public class SentenceDetectorServiceImpl implements SentenceDetectorService {
	SentenceDetectorFeatureService sentenceDetectorFeatureService;
	TokeniserService tokeniserService;
	private MachineLearningService machineLearningService;
	
	@Override
	public PossibleSentenceBoundary getPossibleSentenceBoundary(String text,
			int index) {
		PossibleSentenceBoundaryImpl boundary = new PossibleSentenceBoundaryImpl(text, index);
		boundary.setTokeniserService(tokeniserService);
		return boundary;
	}

	@Override
	public SentenceDetector getSentenceDetector(
			DecisionMaker<SentenceDetectorOutcome> decisionMaker,
			Set<SentenceDetectorFeature<?>> features) {
		SentenceDetectorImpl sentenceDetector = new SentenceDetectorImpl(decisionMaker, features);
		sentenceDetector.setSentenceDetectorService(this);
		sentenceDetector.setSentenceDetectorFeatureService(sentenceDetectorFeatureService);
		return sentenceDetector;
	}

	@Override
	public SentenceDetectorEvaluator getEvaluator(
			SentenceDetector sentenceDetector) {
		SentenceDetectorEvaluatorImpl evaluator = new SentenceDetectorEvaluatorImpl(sentenceDetector);
		return evaluator;
	}

	public SentenceDetectorFeatureService getSentenceDetectorFeatureService() {
		return sentenceDetectorFeatureService;
	}

	public void setSentenceDetectorFeatureService(
			SentenceDetectorFeatureService sentenceDetectorFeatureService) {
		this.sentenceDetectorFeatureService = sentenceDetectorFeatureService;
	}

	public TokeniserService getTokeniserService() {
		return tokeniserService;
	}

	public void setTokeniserService(TokeniserService tokeniserService) {
		this.tokeniserService = tokeniserService;
	}


	@Override
	public CorpusEventStream getSentenceDetectorEventStream(
			SentenceDetectorAnnotatedCorpusReader corpusReader,
			Set<SentenceDetectorFeature<?>> features,
			List<TextFilter> filters) {
		SentenceDetectorEventStream eventStream = new SentenceDetectorEventStream(corpusReader, features);
		eventStream.setSentenceDetectorService(this);
		eventStream.setSentenceDetectorFeatureService(sentenceDetectorFeatureService);
		eventStream.setMachineLearningService(machineLearningService);
		eventStream.setFilters(filters);
		return eventStream;
	}

	@Override
	public DecisionFactory<SentenceDetectorOutcome> getDecisionFactory() {
		SentenceDetectorDecisionFactory factory = new SentenceDetectorDecisionFactory();
		return factory;
	}

	public MachineLearningService getMachineLearningService() {
		return machineLearningService;
	}

	public void setMachineLearningService(
			MachineLearningService machineLearningService) {
		this.machineLearningService = machineLearningService;
	}

}
