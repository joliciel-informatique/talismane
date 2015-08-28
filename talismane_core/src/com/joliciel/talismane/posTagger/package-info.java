/**
* Package enabling the part-of-speech tagging of a {@link com.joliciel.talismane.tokeniser.TokenSequence}.<br/>
* The central class is the ({@link com.joliciel.talismane.posTagger.PosTagger}).<br/>
* It tags based on a previously constructed classificaton model, converting the model's outcomes to {@link com.joliciel.talismane.posTagger.PosTag} using the currently set {@link com.joliciel.talismane.posTagger.PosTagSet}.<br/>
* The PosTagSet is set via {@link com.joliciel.talismane.TalismaneSession#setPosTagSet(PosTagSet)}.<br/>
* The pos-tagger prodces a {@link com.joliciel.talismane.posTagger.PosTagSequence} which is a list of {@link com.joliciel.talismane.posTagger.PosTaggedToken}.<br/>
* It can be evaluated via a {@link com.joliciel.talismane.posTagger.PosTaggerEvaluator}.<br/>
*/
package com.joliciel.talismane.posTagger;