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
package com.joliciel.talismane.parser;

import com.joliciel.talismane.posTagger.PosTag;
import com.joliciel.talismane.posTagger.PosTaggedToken;
import com.joliciel.talismane.tokeniser.Token;

public final class ParseConfigurationTokenOutput {
  private PosTaggedToken posTaggedToken;
  private Token token;
  private PosTag tag;
  private ParseConfigurationTokenOutput governor;
  private DependencyArc arc;
  private String label;

  private ParseConfigurationTokenOutput nonProjectiveGovernor;
  private DependencyArc nonProjectiveArc;
  private String nonProjectiveLabel;

  public ParseConfigurationTokenOutput(PosTaggedToken posTaggedToken) {
    this.posTaggedToken = posTaggedToken;
    this.token = posTaggedToken.getToken();
    this.tag = posTaggedToken.getTag();
  }

  public PosTaggedToken getPosTaggedToken() {
    return posTaggedToken;
  }

  public Token getToken() {
    return token;
  }

  public PosTag getTag() {
    return tag;
  }

  public ParseConfigurationTokenOutput getGovernor() {
    return governor;
  }

  public void setGovernor(ParseConfigurationTokenOutput governor) {
    this.governor = governor;
  }

  public DependencyArc getArc() {
    return arc;
  }

  public void setArc(DependencyArc arc) {
    this.arc = arc;
  }

  public String getLabel() {
    return label;
  }

  public void setLabel(String label) {
    this.label = label;
  }

  public ParseConfigurationTokenOutput getNonProjectiveGovernor() {
    return nonProjectiveGovernor;
  }

  public void setNonProjectiveGovernor(ParseConfigurationTokenOutput nonProjectiveGovernor) {
    this.nonProjectiveGovernor = nonProjectiveGovernor;
  }

  public DependencyArc getNonProjectiveArc() {
    return nonProjectiveArc;
  }

  public void setNonProjectiveArc(DependencyArc nonProjectiveArc) {
    this.nonProjectiveArc = nonProjectiveArc;
  }

  public String getNonProjectiveLabel() {
    return nonProjectiveLabel;
  }

  public void setNonProjectiveLabel(String nonProjectiveLabel) {
    this.nonProjectiveLabel = nonProjectiveLabel;
  }

}
