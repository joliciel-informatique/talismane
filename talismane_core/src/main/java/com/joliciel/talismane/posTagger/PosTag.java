package com.joliciel.talismane.posTagger;

import java.io.Serializable;

import com.joliciel.talismane.tokeniser.TokenTag;

/**
 * A part of speech tag, representing a certain morpho-syntaxic category for a
 * word.
 * 
 * @author Assaf Urieli
 * 
 */
public class PosTag implements TokenTag, Comparable<PosTag>, Serializable {

  private static final long serialVersionUID = 1L;
  private String code;
  private String description;
  private PosTagOpenClassIndicator openClassIndicator;

  /**
   * Construct a pos-tag for a given code, desciption and open-class indicator.
   * 
   * @param code
   *          the pos-tag's code
   * @param description
   *          the pos-tag's description
   * @param openClassIndicator
   *          the pos-tag's open class indicator.
   */
  public PosTag(String code, String description, PosTagOpenClassIndicator openClassIndicator) {
    this.code = code;
    this.description = description;
    this.openClassIndicator = openClassIndicator;
  }

  /**
   * An empty PosTag is used for the "empty" sentence start element in n-gram
   * models.
   * 
   * @return true if this is the empty PosTag, false otherwise
   */
  public boolean isEmpty() {
    return this.code.equals(PosTag.NULL_POS_TAG_CODE);
  }

  /**
   * The PosTag's unique code.
   */
  public String getCode() {
    return code;
  }

  /**
   * Description of this PosTag.
   */
  public String getDescription() {
    return description;
  }

  /**
   * Is this PosTag an open or a closed class, and which type of open/closed
   * class?
   */
  public PosTagOpenClassIndicator getOpenClassIndicator() {
    return openClassIndicator;
  }

  @Override
  public int hashCode() {
    return this.code.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (!(obj instanceof PosTag))
      return false;
    PosTag other = (PosTag) obj;
    if (code == null) {
      if (other.getCode() != null)
        return false;
    } else if (!code.equals(other.getCode()))
      return false;
    return true;
  }

  @Override
  public int compareTo(PosTag posTag) {
    return this.getCode().compareTo(posTag.getCode());
  }

  @Override
  public String toString() {
    return this.code;
  }

  /**
   * The code to be used by the null pos tag.
   */
  public static String NULL_POS_TAG_CODE = "null";

  /**
   * The code to be used by the root pos tag.
   */
  public static String ROOT_POS_TAG_CODE = "root";

  /**
   * A null pos tag, to be used for empty tokens that should be discarded.
   */
  public static PosTag NULL_POS_TAG = new PosTag(PosTag.NULL_POS_TAG_CODE, "null pos tag", PosTagOpenClassIndicator.CLOSED);

  /**
   * An artificial pos-tag used to indicate the artificial root added to all
   * sentences for parsing.
   */
  public static PosTag ROOT_POS_TAG = new PosTag(PosTag.ROOT_POS_TAG_CODE, "root pos tag", PosTagOpenClassIndicator.CLOSED);
}
