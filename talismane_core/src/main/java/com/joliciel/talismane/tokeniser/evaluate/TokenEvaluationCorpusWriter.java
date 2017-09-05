package com.joliciel.talismane.tokeniser.evaluate;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joliciel.talismane.TalismaneSession;
import com.joliciel.talismane.posTagger.PosTaggedToken;
import com.joliciel.talismane.tokeniser.TaggedToken;
import com.joliciel.talismane.tokeniser.Token;
import com.joliciel.talismane.tokeniser.TokenSequence;
import com.joliciel.talismane.tokeniser.TokenisedAtomicTokenSequence;
import com.joliciel.talismane.tokeniser.TokeniserOutcome;

public class TokenEvaluationCorpusWriter implements TokenEvaluationObserver {
  @SuppressWarnings("unused")
  private static final Logger LOG = LoggerFactory.getLogger(TokenEvaluationCorpusWriter.class);
  private final Writer corpusWriter;

  public TokenEvaluationCorpusWriter(File outDir, TalismaneSession session) throws IOException {
    File corpusFile = new File(outDir, session.getBaseName() + ".corpus.txt");
    corpusFile.delete();
    corpusFile.createNewFile();
    corpusWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(corpusFile, false), "UTF8"));
  }

  @Override
  public void onNextTokenSequence(TokenSequence realSequence, List<TokenisedAtomicTokenSequence> guessedAtomicSequences) throws IOException {
    List<Integer> realSplits = realSequence.getTokenSplits();

    TokenisedAtomicTokenSequence tokenisedAtomicTokenSequence = guessedAtomicSequences.get(0);

    Map<Integer, TokeniserOutcome> realOutcomes = new HashMap<Integer, TokeniserOutcome>();
    Map<Integer, TokeniserOutcome> guessedOutcomes = new HashMap<Integer, TokeniserOutcome>();
    Map<Integer, List<String>> guessedAuthorities = new HashMap<Integer, List<String>>();
    List<Integer> indexes = new ArrayList<Integer>();

    corpusWriter.write(realSequence.getSentence().getText() + "\n");
    for (TaggedToken<TokeniserOutcome> guessTag : tokenisedAtomicTokenSequence) {
      TokeniserOutcome guessDecision = guessTag.getTag();
      int startIndex = guessTag.getToken().getStartIndex();
      boolean realSplit = realSplits.contains(startIndex);

      TokeniserOutcome realDecision = realSplit ? TokeniserOutcome.SEPARATE : TokeniserOutcome.JOIN;

      indexes.add(startIndex);
      realOutcomes.put(startIndex, realDecision);
      guessedOutcomes.put(startIndex, guessDecision);
      guessedAuthorities.put(startIndex, guessTag.getDecision().getAuthorities());
    }

    int prevEndIndex = 0;
    int i = 0;
    for (Token token : realSequence) {
      corpusWriter.write(token.getOriginalText());
      Set<String> authorities = new TreeSet<String>();
      boolean correct = true;
      for (int index : indexes) {
        if (prevEndIndex <= index && index < token.getEndIndex()) {
          correct = correct && realOutcomes.get(index) == guessedOutcomes.get(index);
          authorities.addAll(guessedAuthorities.get(index));
        }
      }
      if (realSequence.getPosTagSequence() != null) {
        PosTaggedToken posTaggedToken = realSequence.getPosTagSequence().get(i);
        corpusWriter.write("\t" + posTaggedToken.getTag().getCode());
      }
      corpusWriter.write("\t" + correct);
      for (String authority : authorities) {
        if (!authority.startsWith("_")) {
          corpusWriter.write("\t" + authority);
        }
      }
      corpusWriter.write("\n");
      corpusWriter.flush();
      prevEndIndex = token.getEndIndex();
      i++;
    }
    corpusWriter.write("\n");
  }

  @Override
  public void onEvaluationComplete() throws IOException {
    corpusWriter.close();
  }

}
