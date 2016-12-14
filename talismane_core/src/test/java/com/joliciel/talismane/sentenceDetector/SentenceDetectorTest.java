package com.joliciel.talismane.sentenceDetector;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Test;

import com.joliciel.talismane.AnnotatedText;
import com.joliciel.talismane.Annotation;
import com.joliciel.talismane.TalismaneSession;
import com.joliciel.talismane.machineLearning.ClassificationSolution;
import com.joliciel.talismane.machineLearning.Decision;
import com.joliciel.talismane.machineLearning.DecisionMaker;
import com.joliciel.talismane.machineLearning.GeometricMeanScoringStrategy;
import com.joliciel.talismane.machineLearning.ScoringStrategy;
import com.joliciel.talismane.machineLearning.features.FeatureResult;
import com.joliciel.talismane.rawText.RawTextMarker.RawTextNoSentenceBreakMarker;
import com.joliciel.talismane.sentenceDetector.features.SentenceDetectorFeature;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

public class SentenceDetectorTest {

	@Test
	public void testDetectSentences() throws Exception {
		System.setProperty("config.file", "src/test/resources/test.conf");
		ConfigFactory.invalidateCaches();
		final Config config = ConfigFactory.load();

		final TalismaneSession session = new TalismaneSession(config, "");

		DecisionMaker decisionMaker = new DecisionMaker() {

			@Override
			public ScoringStrategy<ClassificationSolution> getDefaultScoringStrategy() {
				return new GeometricMeanScoringStrategy();
			}

			@Override
			public List<Decision> decide(List<FeatureResult<?>> featureResults) {
				List<Decision> decisions = new ArrayList<>();
				Decision decision = new Decision(SentenceDetectorOutcome.IS_BOUNDARY.name(), 1.0);
				decisions.add(decision);
				return decisions;
			}
		};

		Set<SentenceDetectorFeature<?>> features = new HashSet<>();

		SentenceDetector sentenceDetector = new SentenceDetector(decisionMaker, features, session);

		String text = "Before analysis. Hello Mr. Jones. How are you, Mr. Jones? After analysis.";

		AnnotatedText annotatedText = new AnnotatedText(text, "Before analysis. ".length(),
				"Before analysis. Hello Mr. Jones. How are you, Mr. Jones?".length());

		List<Annotation<RawTextNoSentenceBreakMarker>> noSentenceBreakMarkers = new ArrayList<>();
		noSentenceBreakMarkers.add(new Annotation<RawTextNoSentenceBreakMarker>("Before analysis. Hello ".length(), "Before analysis. Hello Mr.".length(),
				new RawTextNoSentenceBreakMarker("me")));
		noSentenceBreakMarkers.add(new Annotation<RawTextNoSentenceBreakMarker>("Before analysis. Hello Mr. Jones. How are you, ".length(),
				"Before analysis. Hello Mr. Jones. How are you, Mr.".length(), new RawTextNoSentenceBreakMarker("me")));
		annotatedText.addAnnotations(noSentenceBreakMarkers);

		List<Integer> sentenceBreaks = sentenceDetector.detectSentences(annotatedText);
		assertEquals(2, sentenceBreaks.size());

		assertEquals("Before analysis. Hello Mr. Jones".length(), sentenceBreaks.get(0).intValue());
		assertEquals("Before analysis. Hello Mr. Jones. How are you, Mr. Jones".length(), sentenceBreaks.get(1).intValue());

		List<Annotation<DetectedSentenceBreak>> sentenceBoudaries = annotatedText.getAnnotations(DetectedSentenceBreak.class);
		assertEquals(2, sentenceBoudaries.size());
		assertEquals("Before analysis. Hello Mr. Jones".length(), sentenceBoudaries.get(0).getStart());
		assertEquals("Before analysis. Hello Mr. Jones.".length(), sentenceBoudaries.get(0).getEnd());
		assertEquals("Before analysis. Hello Mr. Jones. How are you, Mr. Jones".length(), sentenceBoudaries.get(1).getStart());
		assertEquals("Before analysis. Hello Mr. Jones. How are you, Mr. Jones?".length(), sentenceBoudaries.get(1).getEnd());

	}

}
