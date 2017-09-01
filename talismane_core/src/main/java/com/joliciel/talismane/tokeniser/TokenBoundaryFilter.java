package com.joliciel.talismane.tokeniser;

public class TokenBoundaryFilter {

  protected void filter(TokenBoundary tokenBoundary, String newAnalysisText) {
    tokenBoundary.setAnalysisText(newAnalysisText);
  }
}
