package com.joliciel.talismane.parser;

import java.io.File;
import java.io.Writer;
import java.util.Collection;
import java.util.Set;

import com.joliciel.talismane.TalismaneException;
import com.joliciel.talismane.TalismaneService;
import com.joliciel.talismane.machineLearning.ClassificationEventStream;
import com.joliciel.talismane.machineLearning.ClassificationModel;
import com.joliciel.talismane.machineLearning.DecisionMaker;
import com.joliciel.talismane.machineLearning.ExternalResource;
import com.joliciel.talismane.machineLearning.MachineLearningModel;
import com.joliciel.talismane.parser.Parser.ParseComparisonStrategyType;
import com.joliciel.talismane.parser.features.ParseConfigurationFeature;
import com.joliciel.talismane.parser.features.ParserFeatureService;
import com.joliciel.talismane.posTagger.PosTaggerService;

public class ParserServiceImpl implements ParserServiceInternal {
	TalismaneService talismaneService;
	ParserFeatureService parseFeatureService;
	PosTaggerService posTaggerService;

	@Override
	public NonDeterministicParser getTransitionBasedParser(DecisionMaker decisionMaker, TransitionSystem transitionSystem,
			Set<ParseConfigurationFeature<?>> parseFeatures, int beamWidth) {
		TransitionBasedParserImpl parser = new TransitionBasedParserImpl(decisionMaker, transitionSystem, parseFeatures, beamWidth);
		parser.setParserServiceInternal(this);
		parser.setTalismaneService(this.getTalismaneService());
		return parser;
	}

	@Override
	public ClassificationEventStream getParseEventStream(ParserAnnotatedCorpusReader corpusReader, Set<ParseConfigurationFeature<?>> parseFeatures) {
		ParseEventStream eventStream = new ParseEventStream(corpusReader, parseFeatures);
		eventStream.setParserServiceInternal(this);
		return eventStream;
	}

	@Override
	public ParserEvaluator getParserEvaluator() {
		ParserEvaluatorImpl evaluator = new ParserEvaluatorImpl();
		evaluator.setParserServiceInternal(this);
		return evaluator;
	}

	@Override
	public TransitionSystem getShiftReduceTransitionSystem() {
		ShiftReduceTransitionSystem transitionSystem = new ShiftReduceTransitionSystem();
		return transitionSystem;
	}

	@Override
	public TransitionSystem getArcEagerTransitionSystem() {
		ArcEagerTransitionSystem transitionSystem = new ArcEagerTransitionSystem();
		return transitionSystem;
	}

	@Override
	public TransitionSystem getTransitionSystem(MachineLearningModel model) {
		TransitionSystem transitionSystem = null;
		String transitionSystemClassName = (String) model.getModelAttributes().get("transitionSystem");
		if (transitionSystemClassName.equalsIgnoreCase("ShiftReduceTransitionSystem")) {
			transitionSystem = this.getShiftReduceTransitionSystem();
		} else if (transitionSystemClassName.equalsIgnoreCase("ArcEagerTransitionSystem")) {
			transitionSystem = this.getArcEagerTransitionSystem();
		} else {
			throw new TalismaneException("Unknown transition system: " + transitionSystemClassName);
		}
		return transitionSystem;
	}

	@Override
	public NonDeterministicParser getTransitionBasedParser(MachineLearningModel model, int beamWidth, boolean dynamiseFeatures) {
		Collection<ExternalResource<?>> externalResources = model.getExternalResources();
		if (externalResources != null) {
			for (ExternalResource<?> externalResource : externalResources) {
				this.getParseFeatureService().getExternalResourceFinder().addExternalResource(externalResource);
			}
		}

		TransitionSystem transitionSystem = this.getTransitionSystem(model);

		Set<ParseConfigurationFeature<?>> parseFeatures = this.getParseFeatureService().getFeatures(model.getFeatureDescriptors(), dynamiseFeatures);

		NonDeterministicParser parser = null;
		if (model instanceof ClassificationModel) {
			ClassificationModel classificationModel = (ClassificationModel) model;
			DecisionMaker decisionMaker = classificationModel.getDecisionMaker();

			parser = this.getTransitionBasedParser(decisionMaker, transitionSystem, parseFeatures, beamWidth);
		} else {
			throw new TalismaneException("Unknown parser model type: " + model.getClass().getSimpleName());
		}
		return parser;
	}

	public ParserFeatureService getParseFeatureService() {
		return parseFeatureService;
	}

	public void setParseFeatureService(ParserFeatureService parseFeatureService) {
		this.parseFeatureService = parseFeatureService;
	}

	public PosTaggerService getPosTaggerService() {
		return posTaggerService;
	}

	public void setPosTaggerService(PosTaggerService posTaggerService) {
		this.posTaggerService = posTaggerService;
	}

	@Override
	public ParseComparator getParseComparator() {
		ParseComparatorImpl parseComparator = new ParseComparatorImpl();
		parseComparator.setParserServiceInternal(this);
		return parseComparator;
	}

	@Override
	public ParseComparisonStrategy getParseComparisonStrategy(ParseComparisonStrategyType type) {
		switch (type) {
		case transitionCount:
			return new TransitionCountComparisonStrategy();
		case bufferSize:
			return new BufferSizeComparisonStrategy();
		case stackAndBufferSize:
			return new StackAndBufferSizeComparsionStrategy();
		case dependencyCount:
			return new DependencyCountComparisonStrategy();
		default:
			throw new TalismaneException("Unknown parse comparison strategy: " + type);
		}
	}

	@Override
	public ParseConfigurationProcessor getParseFeatureTester(Set<ParseConfigurationFeature<?>> parserFeatures, File file) {
		ParseFeatureTester tester = new ParseFeatureTester(parserFeatures, file);
		return tester;
	}

	@Override
	public ParseConfigurationProcessor getTransitionLogWriter(Writer csvFileWriter) {
		TransitionLogWriter processor = new TransitionLogWriter(csvFileWriter);
		return processor;
	}

	public TalismaneService getTalismaneService() {
		return talismaneService;
	}

	public void setTalismaneService(TalismaneService talismaneService) {
		this.talismaneService = talismaneService;
	}

}
