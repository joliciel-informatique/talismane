/**
 * Package enabling the part-of-speech tagging of a
 * {@link com.joliciel.talismane.tokeniser.TokenSequence}.<br/>
 * The central class is the {@link com.joliciel.talismane.posTagger.PosTagger}.
 * <br/>
 * It tags based on a previously constructed classification model, converting
 * the model's outcomes to {@link com.joliciel.talismane.posTagger.PosTag} using
 * the currently set {@link com.joliciel.talismane.posTagger.PosTagSet}.<br/>
 * The PosTagSet loaded from the model if a model already exists, otherwise from
 * the configuration.<br/>
 * The pos-tagger produces a
 * {@link com.joliciel.talismane.posTagger.PosTagSequence} which is a list of
 * {@link com.joliciel.talismane.posTagger.PosTaggedToken}.<br/>
 * It can be evaluated via a
 * {@link com.joliciel.talismane.posTagger.PosTaggerEvaluator}.<br/>
 */
package com.joliciel.talismane.posTagger;
