///////////////////////////////////////////////////////////////////////////////
//Copyright (C) 2014 Joliciel Informatique
//
//This file is part of Talismane.
//
//Talismane is free software: you can redistribute it and/or modify
//it under the terms of the GNU Affero General Public License as published by
//the Free Software Foundation, either version 3 of the License, or
//(at your option) any later version.
//
//Talismane is distributed in the hope that it will be useful,
//but WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//GNU Affero General Public License for more details.
//
//You should have received a copy of the GNU Affero General Public License
//along with Talismane.  If not, see <http://www.gnu.org/licenses/>.
//////////////////////////////////////////////////////////////////////////////
package com.joliciel.talismane.posTagger;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import com.joliciel.talismane.TalismaneSession;
import com.joliciel.talismane.lexicon.LexicalAttribute;
import com.joliciel.talismane.lexicon.LexicalEntry;
import com.joliciel.talismane.machineLearning.Decision;
import com.joliciel.talismane.machineLearning.features.Feature;
import com.joliciel.talismane.machineLearning.features.FeatureResult;
import com.joliciel.talismane.machineLearning.features.HasFeatureCache;
import com.joliciel.talismane.machineLearning.features.RuntimeEnvironment;
import com.joliciel.talismane.posTagger.features.PosTaggedTokenWrapper;
import com.joliciel.talismane.tokeniser.StringAttribute;
import com.joliciel.talismane.tokeniser.TaggedToken;
import com.joliciel.talismane.tokeniser.Token;

/**
 * A token with a postag tagged onto it.<br>
 * Note: this class naturally implements {@link PosTaggerContext} since if a
 * token has already been pos-tagged, the once before it have been pos-tagged as
 * well. This allows us to use the same features on the token currently being
 * pos-tagged and tokens already pos-tagged.
 * 
 * @author Assaf Urieli
 *
 */
public class PosTaggedToken extends TaggedToken<PosTag> implements PosTaggedTokenWrapper, HasFeatureCache, Serializable {
  private static final long serialVersionUID = 1L;
  
  private transient Map<String, FeatureResult<?>> featureResults = new HashMap<>();

  private transient List<LexicalEntry> lexicalEntries = null;
  private transient boolean lemmaFetched = false;
  private transient String lemma = null;
  private transient String comment = "";
  private transient String morphologyForCoNLL = null;
  
  private final String sessionId;

  PosTaggedToken(PosTaggedToken taggedTokenToClone, Token token) {
    super(taggedTokenToClone, token);
    this.featureResults = taggedTokenToClone.featureResults;
    this.lexicalEntries = taggedTokenToClone.lexicalEntries;
    this.sessionId = taggedTokenToClone.sessionId;
  }

  /**
   * Construct a pos-tagged token for a given token and given decision - the
   * {@link Decision#getOutcome()} must be a valid {@link PosTag#getCode()} from
   * the current {@link PosTagSet}.
   * 
   * @param token
   *          the token to be tagged
   * @param decision
   *          the decision used to tag it
   * @throws UnknownPosTagException
   */
  public PosTaggedToken(Token token, Decision decision, String sessionId) throws UnknownPosTagException {
    super(token, decision, TalismaneSession.get(sessionId).getPosTagSet().getPosTag(decision.getOutcome()));
    this.sessionId = sessionId;
  }

  /**
   * All lexical entries for this token/postag combination.
   */
  public List<LexicalEntry> getLexicalEntries() {
    if (lexicalEntries == null) {
      lexicalEntries = TalismaneSession.get(sessionId).getMergedLexicon().findLexicalEntries(this.getToken().getText(), this.getTag());
      if (lexicalEntries.size() == 0) {
        lexicalEntries = TalismaneSession.get(sessionId).getMergedLexicon().findLexicalEntries(this.getToken().getText().toLowerCase(TalismaneSession.get(sessionId).getLocale()),
            this.getTag());
      }
    }
    return lexicalEntries;
  }

  public void setLexicalEntries(List<LexicalEntry> lexicalEntries) {
    this.lexicalEntries = lexicalEntries;
  }

  @Override
  public String toString() {
    return this.getToken().getText() + "|" + this.getTag() + "|" + this.getToken().getIndex();
  }

  @Override
  @SuppressWarnings("unchecked")

  public <T, Y> FeatureResult<Y> getResultFromCache(Feature<T, Y> feature, RuntimeEnvironment env) {
    FeatureResult<Y> result = null;

    String key = feature.getName() + env.getKey();
    if (this.featureResults.containsKey(key)) {
      result = (FeatureResult<Y>) this.featureResults.get(key);
    }
    return result;
  }

  @Override
  public <T, Y> void putResultInCache(Feature<T, Y> feature, FeatureResult<Y> featureResult, RuntimeEnvironment env) {
    String key = feature.getName() + env.getKey();
    this.featureResults.put(key, featureResult);
  }

  @Override
  public PosTaggedToken getPosTaggedToken() {
    return this;
  }

  public PosTaggedToken clonePosTaggedToken(Token token) {
    PosTaggedToken posTaggedToken = new PosTaggedToken(this, token);
    return posTaggedToken;
  }

  /**
   * This pos-tagged token's lemma, or null if no lemma found.<br>
   * If there are multiple lexical entries, the first one's lemma is returned.
   * <br>
   * If all possible lemmas are required, they need to be retrieved from
   * {@link #getLexicalEntries()}.
   */
  public String getLemma() {
    if (!this.lemmaFetched) {
      String lemmaType = null;
      StringAttribute lemmaTypeAttribute = (StringAttribute) this.getToken().getAttributes().get(PosTagger.LEMMA_TYPE_ATTRIBUTE);
      if (lemmaTypeAttribute != null)
        lemmaType = lemmaTypeAttribute.getValue();
      String explicitLemma = null;
      StringAttribute explicitLemmaAttribute = (StringAttribute) this.getToken().getAttributes().get(PosTagger.LEMMA_ATTRIBUTE);
      if (explicitLemmaAttribute != null)
        explicitLemma = explicitLemmaAttribute.getValue();
      if (explicitLemma != null) {
        this.lemma = explicitLemma;
      } else if (lemmaType != null && lemmaType.equals("originalLower")) {
        this.lemma = this.getToken().getOriginalText().toLowerCase(TalismaneSession.get(sessionId).getLocale());
      } else if (this.getLexicalEntries().size() > 0) {
        this.lemma = this.getLexicalEntries().get(0).getLemma();
      }
      this.lemmaFetched = true;
    }
    return this.lemma;
  }

  /**
   * Like {@link #getLemma()} but encoded for the CoNLL output format.
   */
  public String getLemmaForCoNLL() {
    return TalismaneSession.get(sessionId).getCoNLLFormatter().toCoNLL(this.getLemma());
  }

  /**
   * A comment regarding this pos-tag annotation.
   */
  public String getComment() {
    return comment;
  }

  public void setComment(String comment) {
    this.comment = comment;
  }

  /**
   * This token's index in the containing sentence.
   */
  public int getIndex() {
    return this.getToken().getIndex();
  }

  /**
   * A string representation of all of the morpho-syntaxic information combined
   * in CoNLL-X format.
   */
  public String getMorphologyForCoNLL() {
    if (morphologyForCoNLL == null) {
      StringBuilder sb = new StringBuilder();
      Set<String> items = new TreeSet<>();
      for (LexicalEntry lexicalEntry : this.getLexicalEntries()) {
        if (lexicalEntry.hasAttribute(LexicalAttribute.SubCategory) && lexicalEntry.getSubCategory().length() > 0)
          items.add(lexicalEntry.getSubCategory());
      }
      if (items.size() > 0) {
        if (sb.length() > 0)
          sb.append("|");
        sb.append("s=");
        sb.append(items.stream().collect(Collectors.joining(",")));
      }

      items = new TreeSet<>();
      for (LexicalEntry lexicalEntry : this.getLexicalEntries()) {
        if (lexicalEntry.hasAttribute(LexicalAttribute.Case)) {
          items.addAll(lexicalEntry.getCase());
        }
      }
      if (items.size() > 0) {
        if (sb.length() > 0)
          sb.append("|");
        sb.append("c=");
        sb.append(items.stream().collect(Collectors.joining(",")));
      }

      items = new TreeSet<>();
      for (LexicalEntry lexicalEntry : this.getLexicalEntries()) {
        if (lexicalEntry.hasAttribute(LexicalAttribute.Number)) {
          items.addAll(lexicalEntry.getNumber());
        }
      }
      if (items.size() > 0) {
        if (sb.length() > 0)
          sb.append("|");
        sb.append("n=");
        sb.append(items.stream().collect(Collectors.joining(",")));
      }

      items = new TreeSet<>();
      for (LexicalEntry lexicalEntry : this.getLexicalEntries()) {
        if (lexicalEntry.hasAttribute(LexicalAttribute.Gender)) {
          items.addAll(lexicalEntry.getGender());
        }
      }
      if (items.size() > 0) {
        if (sb.length() > 0)
          sb.append("|");
        sb.append("g=");
        sb.append(items.stream().collect(Collectors.joining(",")));
      }

      items = new TreeSet<>();
      for (LexicalEntry lexicalEntry : this.getLexicalEntries()) {
        if (lexicalEntry.hasAttribute(LexicalAttribute.Tense)) {
          items.addAll(lexicalEntry.getTense());
        }
      }
      if (items.size() > 0) {
        if (sb.length() > 0)
          sb.append("|");
        sb.append("t=");
        sb.append(items.stream().collect(Collectors.joining(",")));
      }

      items = new TreeSet<>();
      for (LexicalEntry lexicalEntry : this.getLexicalEntries()) {
        if (lexicalEntry.hasAttribute(LexicalAttribute.Mood)) {
          items.addAll(lexicalEntry.getMood());
        }
      }
      if (items.size() > 0) {
        if (sb.length() > 0)
          sb.append("|");
        sb.append("m=");
        sb.append(items.stream().collect(Collectors.joining(",")));
      }

      items = new TreeSet<>();
      for (LexicalEntry lexicalEntry : this.getLexicalEntries()) {
        if (lexicalEntry.hasAttribute(LexicalAttribute.Aspect)) {
          items.addAll(lexicalEntry.getAspect());
        }
      }
      if (items.size() > 0) {
        if (sb.length() > 0)
          sb.append("|");
        sb.append("a=");
        sb.append(items.stream().collect(Collectors.joining(",")));
      }

      items = new TreeSet<>();
      for (LexicalEntry lexicalEntry : this.getLexicalEntries()) {
        if (lexicalEntry.hasAttribute(LexicalAttribute.Person)) {
          items.addAll(lexicalEntry.getPerson());
        }
      }
      if (items.size() > 0) {
        if (sb.length() > 0)
          sb.append("|");
        sb.append("p=");
        sb.append(items.stream().collect(Collectors.joining(",")));
      }

      items = new TreeSet<>();
      for (LexicalEntry lexicalEntry : this.getLexicalEntries()) {
        if (lexicalEntry.hasAttribute(LexicalAttribute.PossessorNumber)) {
          items.addAll(lexicalEntry.getPossessorNumber());
        }
      }
      if (items.size() > 0) {
        if (sb.length() > 0)
          sb.append("|");
        sb.append("poss=");
        sb.append(items.stream().collect(Collectors.joining(",")));
      }
      morphologyForCoNLL = sb.toString();
    }
    return morphologyForCoNLL;
  }

  /**
   * Is this an artificial root pos-tagged token?
   */
  public boolean isRoot() {
    return this.getTag().equals(PosTag.ROOT_POS_TAG);
  }
}
