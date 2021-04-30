package com.joliciel.talismane.posTagger;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joliciel.talismane.TalismaneException;
import com.joliciel.talismane.TalismaneSession;
import com.joliciel.talismane.machineLearning.ClassificationSolution;
import com.joliciel.talismane.machineLearning.Decision;
import com.joliciel.talismane.machineLearning.GeometricMeanScoringStrategy;
import com.joliciel.talismane.machineLearning.ScoringStrategy;
import com.joliciel.talismane.machineLearning.Solution;
import com.joliciel.talismane.tokeniser.Token;
import com.joliciel.talismane.tokeniser.TokenSequence;

/**
 * A sequence of postags applied to a given token sequence.
 * 
 * @author Assaf Urieli
 *
 */
public class PosTagSequence extends ArrayList<PosTaggedToken> implements Comparable<PosTagSequence>, ClassificationSolution, Serializable {
  private static final Logger LOG = LoggerFactory.getLogger(PosTagSequence.class);
  private static final long serialVersionUID = 1L;
  private double score = 0.0;
  private boolean scoreCalculated = false;
  private String string = null;
  private final List<Decision> decisions = new ArrayList<>();
  private final List<Solution> underlyingSolutions = new ArrayList<>();

  @SuppressWarnings("rawtypes")
  private ScoringStrategy scoringStrategy = new GeometricMeanScoringStrategy();

  private TokenSequence tokenSequence;
  private final String sessionId;

  /**
   * Construct an empty pos-tag sequence, based on a given {@link TokenSequence}
   * that needs to be pos-tagged.
   * 
   * @param tokenSequence
   *          the token sequence to be pos-tagged.
   */
  public PosTagSequence(TokenSequence tokenSequence) {
    super(tokenSequence.size());
    this.sessionId = tokenSequence.getSessionId();
    this.tokenSequence = tokenSequence;
    this.initialize();
  }

  PosTagSequence(PosTagSequence history) {
    super(history.size() + 1);
    this.sessionId = history.sessionId;

    for (PosTaggedToken posTaggedToken : history)
      this.addPosTaggedToken(posTaggedToken);
    this.decisions.addAll(history.getDecisions());
    this.tokenSequence = history.tokenSequence;

    this.initialize();
  }

  PosTagSequence(PosTagSequence clone, boolean fromClone) {
    this.sessionId = clone.sessionId;
    this.tokenSequence = clone.getTokenSequence().cloneTokenSequence();
    this.initialize();

    this.setScoringStrategy(clone.getScoringStrategy());
    int i = 0;
    for (PosTaggedToken posTaggedToken : clone) {
      PosTaggedToken newPosTaggedToken = posTaggedToken.clonePosTaggedToken(this.getTokenSequence().get(i++));
      this.add(newPosTaggedToken);
    }
    this.decisions.addAll(clone.getDecisions());
  }

  private void initialize() {
    if (tokenSequence.getUnderlyingAtomicTokenSequence() != null) {
      this.underlyingSolutions.add(tokenSequence.getUnderlyingAtomicTokenSequence());
    }
  }

  /**
   * The score is calculated as the geometric mean of postag decisions,
   * multiplied by the score of the token sequence.
   */
  @SuppressWarnings("unchecked")
  @Override
  public double getScore() {
    if (!scoreCalculated) {
      score = this.scoringStrategy.calculateScore(this);
      scoreCalculated = true;
    }
    return score;
  }

  /**
   * Get the Token Sequence on which this PosTagSequence is based.
   */
  public TokenSequence getTokenSequence() {
    return tokenSequence;
  }

  /**
   * Get the next token for which no pos tag has yet been assigned.
   */
  public Token getNextToken() {
    return this.tokenSequence.get(this.size());
  }

  @Override
  public int compareTo(PosTagSequence o) {
    if (this.getScore() < o.getScore()) {
      return 1;
    } else if (this.getScore() > o.getScore()) {
      return -1;
    } else {
      return 0;
    }
  }

  @Override
  public synchronized String toString() {
    if (string == null) {
      StringBuilder builder = new StringBuilder();
      builder.append("Sequence: ");
      for (PosTaggedToken taggedToken : this) {
        builder.append(taggedToken.toString());
        builder.append(", ");
      }
      string = builder.toString();
    }
    return string;
  }

  /**
   * Prepend a root to this PosTagSequence, unless there's a root already, and
   * return the prepended root.
   */
  public PosTaggedToken prependRoot() {
    PosTaggedToken rootToken = null;
    if (this.size() > 0) {
      rootToken = this.get(0);
      if (!rootToken.getTag().equals(PosTag.ROOT_POS_TAG))
        rootToken = null;
    }
    if (rootToken == null) {
      Token emptyToken = tokenSequence.addEmptyToken(0);
      emptyToken.setText("[ROOT]");
      tokenSequence.setWithRoot(true);

      Decision rootDecision = new Decision(PosTag.ROOT_POS_TAG.getCode());
      try {
        rootToken = new PosTaggedToken(emptyToken, rootDecision, this.sessionId);
      } catch (UnknownPosTagException e) {
        // should never happen
        LOG.error(e.getMessage(), e);
        throw new RuntimeException(e);
      }
      this.add(0, rootToken);
    }
    this.string = null;
    return rootToken;
  }

  /**
   * Remove a previously pre-pended root.
   */
  public void removeRoot() {
    PosTaggedToken rootToken = null;
    if (this.size() > 0) {
      rootToken = this.get(0);
      if (!rootToken.getTag().equals(PosTag.ROOT_POS_TAG))
        rootToken = null;
    }
    if (rootToken != null) {
      Token emptyToken = rootToken.getToken();
      try {
        tokenSequence.removeEmptyToken(emptyToken);
      } catch (TalismaneException e) {
        // should never happen
        LOG.error(e.getMessage(), e);
        throw new RuntimeException(e);
      }
      this.remove(0);
      tokenSequence.setWithRoot(false);
      tokenSequence.reindex();
    }
  }

  @Override
  public List<Decision> getDecisions() {
    return this.decisions;
  }

  @Override
  public List<Solution> getUnderlyingSolutions() {
    return this.underlyingSolutions;
  }

  @Override
  public void addDecision(Decision decision) {
    this.decisions.add(decision);
  }

  @Override
  @SuppressWarnings("rawtypes")
  public ScoringStrategy getScoringStrategy() {
    return scoringStrategy;
  }

  @Override
  public void setScoringStrategy(@SuppressWarnings("rawtypes") ScoringStrategy scoringStrategy) {
    this.scoringStrategy = scoringStrategy;
  }

  /**
   * Add a PosTaggedToken to the end of the current sequence.
   */
  public void addPosTaggedToken(PosTaggedToken posTaggedToken) {
    this.add(posTaggedToken);
    this.string = null;
  }

  /**
   * Make a deep clone of this pos-tag sequence.
   */
  public PosTagSequence clonePosTagSequence() {
    PosTagSequence clone = new PosTagSequence(this, true);
    return clone;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    if (!super.equals(o)) return false;
    PosTagSequence that = (PosTagSequence) o;
    return decisions.equals(that.decisions) &&
      tokenSequence.equals(that.tokenSequence) &&
      sessionId.equals(that.sessionId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), decisions, tokenSequence, sessionId);
  }
}
