package com.joliciel.talismane.posTagger;

import java.util.ArrayList;
import java.util.List;

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
public class PosTagSequence extends ArrayList<PosTaggedToken>implements Comparable<PosTagSequence>, ClassificationSolution {
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
  private final TalismaneSession talismaneSession;

  /**
   * Construct an empty pos-tag sequence, based on a given
   * {@link TokenSequence} that needs to be pos-tagged.
   * 
   * @param tokenSequence
   *            the token sequence to be pos-tagged.
   */
  public PosTagSequence(TokenSequence tokenSequence) {
    super(tokenSequence.size());
    this.talismaneSession = tokenSequence.getTalismaneSession();
    this.tokenSequence = tokenSequence;
    this.initialize();
  }

  PosTagSequence(PosTagSequence history) {
    super(history.size() + 1);
    this.talismaneSession = history.getTalismaneSession();

    for (PosTaggedToken posTaggedToken : history)
      this.addPosTaggedToken(posTaggedToken);
    this.decisions.addAll(history.getDecisions());
    this.tokenSequence = history.getTokenSequence();

    this.initialize();
  }

  PosTagSequence(PosTagSequence clone, boolean fromClone) {
    this.talismaneSession = clone.getTalismaneSession();
    this.tokenSequence = clone.getTokenSequence().cloneTokenSequence();
    this.initialize();

    this.setScoringStrategy(clone.getScoringStrategy());
    int i = 0;
    for (PosTaggedToken posTaggedToken : clone) {
      PosTaggedToken newPosTaggedToken = posTaggedToken.clonePosTaggedToken();
      newPosTaggedToken.setToken(this.getTokenSequence().get(i++));
      this.add(newPosTaggedToken);
    }
    this.decisions.addAll(clone.getDecisions());
  }

  private void initialize() {
    if (tokenSequence.getUnderlyingAtomicTokenSequence() != null) {
      this.underlyingSolutions.add(tokenSequence.getUnderlyingAtomicTokenSequence());
    }
    tokenSequence.setPosTagSequence(this);
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
      tokenSequence.finalise();

      Decision rootDecision = new Decision(PosTag.ROOT_POS_TAG.getCode());
      try {
        rootToken = new PosTaggedToken(emptyToken, rootDecision, talismaneSession);
      } catch (UnknownPosTagException e) {
        // should never happen
        LOG.error(e.getMessage(), e);
        throw new RuntimeException(e);
      }
      this.add(0, rootToken);
      rootToken.setPosTagSequence(this);
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
      tokenSequence.finalise();
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
    posTaggedToken.setPosTagSequence(this);
    this.string = null;
  }

  /**
   * Remove all pos-tagged tokens that are empty and whose tag is null.
   * 
   * @throws TalismaneException
   */
  public void removeEmptyPosTaggedTokens() throws TalismaneException {
    boolean haveEmptyTokens = false;
    for (PosTaggedToken posTaggedToken : this) {
      if (posTaggedToken.getToken().isEmpty() && posTaggedToken.getTag().isEmpty()) {
        haveEmptyTokens = true;
        break;
      }
    }

    if (haveEmptyTokens) {
      List<PosTaggedToken> cloneList = new ArrayList<PosTaggedToken>();
      this.tokenSequence = this.tokenSequence.cloneTokenSequence();

      List<Token> emptyTokensToRemove = new ArrayList<Token>();
      int i = 0;
      for (PosTaggedToken posTaggedToken : this) {
        Token token = tokenSequence.get(i++);
        if (posTaggedToken.getToken().isEmpty() && posTaggedToken.getTag().isEmpty()) {
          if (LOG.isDebugEnabled())
            LOG.debug("Removing null empty pos-tagged token at position " + posTaggedToken.getToken().getStartIndex() + ": " + posTaggedToken);
          emptyTokensToRemove.add(token);
        } else {
          PosTaggedToken clonedTaggedToken = posTaggedToken.clonePosTaggedToken();
          cloneList.add(clonedTaggedToken);
        }
      }

      for (Token token : emptyTokensToRemove) {
        this.tokenSequence.removeEmptyToken(token);
      }
      this.tokenSequence.finalise();

      i = 0;
      for (PosTaggedToken posTaggedToken : cloneList) {
        posTaggedToken.setToken(this.tokenSequence.get(i++));
      }

      this.clear();
      this.addAll(cloneList);
      this.string = null;
    }
  }

  /**
   * Make a deep clone of this pos-tag sequence.
   */
  public PosTagSequence clonePosTagSequence() {
    PosTagSequence clone = new PosTagSequence(this, true);
    return clone;
  }

  protected TalismaneSession getTalismaneSession() {
    return talismaneSession;
  }

}
