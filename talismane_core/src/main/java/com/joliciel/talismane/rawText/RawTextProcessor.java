///////////////////////////////////////////////////////////////////////////////
//Copyright (C) 2016 Joliciel Informatique
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
package com.joliciel.talismane.rawText;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Stack;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joliciel.talismane.AnnotatedText;
import com.joliciel.talismane.Annotation;
import com.joliciel.talismane.AnnotationObserver;
import com.joliciel.talismane.TalismaneSession;
import com.joliciel.talismane.sentenceDetector.SentenceBoundary;
import com.joliciel.talismane.utils.io.CurrentFileObserver;

/**
 * A class capable of processing a set of RawTextFilters so that a list of
 * sentences can be extracted.
 * 
 * @author Assaf Urieli
 *
 */
public abstract class RawTextProcessor extends AnnotatedText implements CurrentFileObserver {
  private static final Logger LOG = LoggerFactory.getLogger(RawTextProcessor.class);
  private SentenceHolder sentenceHolder;
  private List<Annotation<SentenceBoundary>> sentenceBoundaries = new ArrayList<>();
  private final TalismaneSession session;
  private static Pattern newlinePattern = Pattern.compile("\r\n|[\r\n]");
  private final Stack<Boolean> shouldProcessStack;
  private final Stack<Boolean> shouldOutputStack;
  private final int originalStartIndex;

  /**
   * The original index just after the last block of processed text. This is
   * not necessarily the same as the originalStartIndex, since text be
   * processed in portions without changing the start index of the full block.
   */
  private int originalIndexProcessed = 0;

  private String leftoverOutput = "";

  private int lineNumber = 1; // this is so that the line following the first
  // newline character will be given 1 (and the
  // initial line 0)
  private int leftoverNewline = 0;

  private Sentence leftover;

  private static final int NUM_CHARS = 30;

  protected RawTextProcessor(CharSequence text, boolean processByDefault, TalismaneSession session) {
    super(text, 0, text.length(), Collections.emptyList());
    this.session = session;

    this.shouldProcessStack = new Stack<>();
    this.shouldProcessStack.push(processByDefault);
    this.shouldOutputStack = new Stack<>();
    this.shouldOutputStack.push(false);

    this.originalStartIndex = 0;
  }

  protected RawTextProcessor(RawTextProcessor predecessor, CharSequence text, int analysisStart, int analysisEnd, List<Annotation<?>> annotations,
      int originalStartIndex) {
    super(text, analysisStart, analysisEnd, annotations);
    this.session = predecessor.session;

    this.shouldOutputStack = predecessor.shouldOutputStack;
    this.shouldProcessStack = predecessor.shouldProcessStack;
    this.originalIndexProcessed = predecessor.originalIndexProcessed;
    this.leftoverOutput = predecessor.leftoverOutput;
    this.lineNumber = predecessor.lineNumber;
    this.leftoverNewline = predecessor.leftoverNewline;
    this.leftover = predecessor.leftover;
    this.originalStartIndex = originalStartIndex;
  }

  /**
   * Processes the current text based on annotations added to block 3, and
   * returns a SentenceHolder.
   * 
   * @return SentenceHolder to retrieve the sentences.
   */
  protected final SentenceHolder processText(int textStartPos, int textEndPos, CharSequence rawText, boolean finalBlock) {
    if (this.sentenceHolder != null)
      return this.sentenceHolder;

    LOG.debug("processText");
    List<Annotation<RawTextMarker>> annotations = this.getAnnotations(RawTextMarker.class);
    if (LOG.isTraceEnabled()) {
      LOG.trace("annotations: " + annotations.toString());
    }

    Map<Integer, List<Pair<Boolean, Annotation<RawTextMarker>>>> markMap = new TreeMap<>();

    for (Annotation<RawTextMarker> annotation : annotations) {
      if (LOG.isTraceEnabled())
        LOG.trace("Annotation: " + annotation.toString());

      if (annotation.getStart() >= textStartPos && annotation.getStart() < textEndPos) {
        if (LOG.isTraceEnabled()) {
          LOG.trace("Start in range: textStartPos " + textStartPos + ">= matcherStart [[" + annotation.getStart() + "]] < textEndPos " + textEndPos);
        }
        List<Pair<Boolean, Annotation<RawTextMarker>>> startMarks = markMap.get(annotation.getStart());
        if (startMarks == null) {
          startMarks = new ArrayList<>();
          markMap.put(annotation.getStart(), startMarks);
        }
        startMarks.add(new ImmutablePair<Boolean, Annotation<RawTextMarker>>(true, annotation));
      } else {
        if (LOG.isTraceEnabled()) {
          LOG.trace("Start out of range: textStartPos " + textStartPos + ">= matcherStart [[" + annotation.getStart() + "]] < textEndPos "
              + textEndPos);
        }
      }

      // if the matcher ends within the textblock
      // or if the matcher ends exactly on the textblock, and the
      // following text block is empty
      // we add the end match
      // the 2nd condition is to ensure we add the end match, since empty
      // blocks can never match anything
      if (annotation.getEnd() >= textStartPos
          && (annotation.getEnd() < textEndPos || (annotation.getEnd() == textEndPos && rawText.length() > 0 && finalBlock))) {
        if (LOG.isTraceEnabled()) {
          LOG.trace("End in range: textStartPos " + textStartPos + ">= matcherEnd [[" + annotation.getEnd() + "]] < textEndPos " + textEndPos);
        }
        List<Pair<Boolean, Annotation<RawTextMarker>>> endMarks = markMap.get(annotation.getEnd());
        if (endMarks == null) {
          endMarks = new ArrayList<>();
          markMap.put(annotation.getEnd(), endMarks);
        }
        endMarks.add(new ImmutablePair<Boolean, Annotation<RawTextMarker>>(false, annotation));
      } else {
        if (LOG.isTraceEnabled()) {
          LOG.trace("End out of range: textStartPos " + textStartPos + ">= matcherEnd [[" + annotation.getEnd() + "]] < textEndPos " + textEndPos);
        }
      }
    }

    if (LOG.isTraceEnabled()) {
      LOG.trace("currentText: " + rawText.toString().replace('\n', '¶').replace('\r', '¶'));
      LOG.trace("marks: " + markMap.toString());
    }

    SentenceHolder sentenceHolder = new SentenceHolder(session, originalIndexProcessed, finalBlock);

    // find any newlines
    sentenceHolder.addNewline(leftoverNewline, lineNumber - 1);

    Matcher matcher = newlinePattern.matcher(rawText);
    while (matcher.find()) {
      sentenceHolder.addNewline(originalIndexProcessed + matcher.end(), lineNumber++);
      leftoverNewline = originalIndexProcessed + matcher.end();
    }

    Map<Integer, Integer> insertionPoints = new TreeMap<Integer, Integer>();
    StringBuilder processedText = new StringBuilder();
    int currentPos = 0;
    int outputPos = 0;

    for (int markPos : markMap.keySet()) {
      List<Pair<Boolean, Annotation<RawTextMarker>>> marks = markMap.get(markPos);

      // collect all instructions at a given position
      List<Triple<RawTextInstruction, Annotation<RawTextMarker>, Boolean>> instructions = new ArrayList<>();
      for (Pair<Boolean, Annotation<RawTextMarker>> mark : marks) {
        boolean isStart = mark.getLeft();
        Annotation<RawTextMarker> annotation = mark.getRight();
        RawTextMarker marker = annotation.getData();

        List<RawTextInstruction> actions = new ArrayList<>();
        if (isStart) {
          switch (marker.getType()) {
          case SKIP:
            actions.add(RawTextInstruction.PUSH_SKIP);
            break;
          case SENTENCE_BREAK:
            break;
          case SPACE:
            actions.add(RawTextInstruction.INSERT);
            actions.add(RawTextInstruction.PUSH_SKIP);
            break;
          case REPLACE:
            actions.add(RawTextInstruction.INSERT);
            actions.add(RawTextInstruction.PUSH_SKIP);
            break;
          case OUTPUT:
            actions.add(RawTextInstruction.PUSH_OUTPUT);
            actions.add(RawTextInstruction.PUSH_SKIP);
            break;
          case INCLUDE:
            actions.add(RawTextInstruction.PUSH_INCLUDE);
            break;
          case OUTPUT_START:
            actions.add(RawTextInstruction.PUSH_OUTPUT);
            break;
          case STOP:
            actions.add(RawTextInstruction.STOP);
            break;
          case NO_SENTENCE_BREAK:
          case NONE:
          case OUTPUT_STOP:
          case START:
          case TAG:
            break;
          }
        } else {
          // end of annotation
          switch (marker.getType()) {
          case SKIP:
          case SPACE:
          case REPLACE:
            actions.add(RawTextInstruction.POP_SKIP);
            break;
          case SENTENCE_BREAK:
            actions.add(RawTextInstruction.SENTENCE_BREAK);
            break;
          case OUTPUT:
            actions.add(RawTextInstruction.STOP_OUTPUT);
            actions.add(RawTextInstruction.POP_SKIP);
            break;
          case INCLUDE:
            actions.add(RawTextInstruction.POP_INCLUDE);
            break;
          case START:
            actions.add(RawTextInstruction.START);
            break;
          case OUTPUT_STOP:
            actions.add(RawTextInstruction.STOP_OUTPUT);
            break;
          case NO_SENTENCE_BREAK:
          case NONE:
          case OUTPUT_START:
          case STOP:
          case TAG:
            break;
          }
        }

        for (RawTextInstruction action : actions) {
          instructions.add(new ImmutableTriple<>(action, annotation, isStart));
        }
      }

      // sort the instructions to ensure they're applied in the correct
      // order
      instructions = instructions.stream().sorted((i1, i2) -> i1.getLeft().compareTo(i2.getLeft())).collect(Collectors.toList());

      for (Triple<RawTextInstruction, Annotation<RawTextMarker>, Boolean> triple : instructions) {
        RawTextInstruction instruction = triple.getLeft();
        Annotation<RawTextMarker> annotation = triple.getMiddle();
        RawTextMarker marker = annotation.getData();
        boolean isStart = triple.getRight();
        int position = isStart ? annotation.getStart() : annotation.getEnd();
        int relativePosition = position - textStartPos;
        if (LOG.isTraceEnabled()) {
          LOG.trace((isStart ? "Start " : "Stop ") + marker.getType() + " at " + position + ", relative pos: " + relativePosition);
          LOG.trace("instruction: " + instruction);
          LOG.trace("Stack before: " + shouldProcessStack);
          LOG.trace("Text before: " + processedText.toString());
          LOG.trace("Added by filter: " + marker.getSource());
          LOG.trace("Match text: "
              + this.getText().subSequence(annotation.getStart(), annotation.getEnd()).toString().replace('\n', '¶').replace('\r', '¶'));
        }

        boolean shouldProcess = shouldProcessStack.peek();
        boolean shouldOutput = shouldOutputStack.peek();

        switch (instruction) {
        case PUSH_SKIP:
          if (shouldProcess) {
            insertionPoints.put(processedText.length(), currentPos);
            processedText.append(rawText.subSequence(currentPos, relativePosition));
            if (shouldOutput) {
              outputPos = relativePosition;
            }
          }
          shouldProcessStack.push(false);
          break;
        case PUSH_OUTPUT:
          if (!shouldOutput && !shouldProcess) {
            outputPos = relativePosition;
          }
          shouldOutputStack.push(true);
          break;
        case PUSH_INCLUDE:
          if (!shouldProcess) {
            currentPos = relativePosition;
            if (shouldOutput) {
              CharSequence outputText = rawText.subSequence(outputPos, relativePosition);
              this.addOutputText(sentenceHolder, processedText.length(), outputText);
              outputPos = relativePosition;
            }
          }
          shouldProcessStack.push(true);
          break;
        case SPACE:
          if (shouldProcess) {
            insertionPoints.put(processedText.length(), currentPos);
            CharSequence leftoverText = rawText.subSequence(currentPos, relativePosition);
            processedText.append(leftoverText);
            currentPos = relativePosition;
            if (leftoverText.length() > 0 && leftoverText.charAt(leftoverText.length() - 1) != ' ') {
              insertionPoints.put(processedText.length(), currentPos);
              processedText.append(" ");
            }
          }
          break;
        case INSERT:
          if (shouldProcess) {
            insertionPoints.put(processedText.length(), currentPos);
            CharSequence leftoverText = rawText.subSequence(currentPos, relativePosition);
            processedText.append(leftoverText);
            currentPos = relativePosition;
            for (int i = 0; i < marker.getInsertionText().length(); i++) {
              insertionPoints.put(processedText.length() + i, currentPos);
            }
            if (LOG.isTraceEnabled())
              LOG.trace("Inserting: " + marker.getInsertionText());
            processedText.append(marker.getInsertionText());
          }
          break;
        case SENTENCE_BREAK: {
          if (shouldProcess) {
            insertionPoints.put(processedText.length(), currentPos);
            CharSequence leftoverText = rawText.subSequence(currentPos, relativePosition);
            processedText.append(leftoverText);
            currentPos = relativePosition;
          }

          // add the sentence boundary after the last character that
          // was added
          sentenceHolder.addSentenceBoundary(processedText.length());
          if (LOG.isTraceEnabled()) {
            int boundary = processedText.length() - 1;
            if (boundary >= 0) {
              String string = null;
              int start1 = boundary - NUM_CHARS;

              if (start1 < 0)
                start1 = 0;
              String startString = processedText.subSequence(start1, boundary).toString();

              String middleString = "" + processedText.charAt(boundary);

              string = startString + "[" + middleString + "]";
              string = string.replace('\n', '¶');
              LOG.trace("Adding sentence break at position " + boundary + ": " + string);
            }
          }
          if (shouldProcess) {
            if (processedText.length() > 0 && processedText.charAt(processedText.length() - 1) != ' ') {
              insertionPoints.put(processedText.length(), currentPos);
              processedText.append(" ");
            }
          }
          break;
        }
        case POP_SKIP:
        case POP_INCLUDE:
        case STOP:
        case START: {
          boolean wasProcessing = shouldProcess;
          boolean wasOutputting = shouldOutput && !shouldProcess;
          if (instruction == RawTextInstruction.POP_SKIP || instruction == RawTextInstruction.POP_INCLUDE) {
            shouldProcessStack.pop();
          } else if (instruction == RawTextInstruction.STOP) {
            shouldProcessStack.pop();
            shouldProcessStack.push(false);
          } else if (instruction == RawTextInstruction.START) {
            shouldProcessStack.pop();
            shouldProcessStack.push(true);
          }
          shouldProcess = shouldProcessStack.peek();
          shouldOutput = shouldOutput && !shouldProcess;

          if (wasProcessing && !shouldProcess) {
            insertionPoints.put(processedText.length(), currentPos);
            processedText.append(rawText.subSequence(currentPos, relativePosition));
          } else if (!wasProcessing && shouldProcess) {
            currentPos = relativePosition;
          } // shouldProcess?

          if (wasOutputting && (!shouldOutput || !shouldProcess)) {
            CharSequence outputText = rawText.subSequence(outputPos, relativePosition);
            this.addOutputText(sentenceHolder, processedText.length(), outputText);
            outputPos = relativePosition;
          } else if (!wasOutputting && (shouldOutput && !shouldProcess)) {
            outputPos = relativePosition;
          } // shouldOutput?
          break;
        }
        case POP_OUTPUT:
        case STOP_OUTPUT:
        case START_OUTPUT: {
          boolean wasOutputting = shouldOutput && !shouldProcess;
          if (instruction == RawTextInstruction.POP_OUTPUT) {
            shouldOutputStack.pop();
          } else if (instruction == RawTextInstruction.STOP_OUTPUT) {
            shouldOutputStack.pop();
            shouldOutputStack.push(false);
          } else if (instruction == RawTextInstruction.START_OUTPUT) {
            shouldOutputStack.pop();
            shouldOutputStack.push(true);
          }
          shouldOutput = shouldOutputStack.peek();

          if (wasOutputting && (!shouldOutput || !shouldProcess)) {
            CharSequence outputText = rawText.subSequence(outputPos, relativePosition);
            this.addOutputText(sentenceHolder, processedText.length(), outputText);
            outputPos = relativePosition;
          } else if (!wasOutputting && (shouldOutput && !shouldProcess)) {
            outputPos = relativePosition;
          } // shouldOutput?
          break;
        }
        } // marker type

        if (LOG.isTraceEnabled()) {
          LOG.trace("Stack after: " + shouldProcessStack);
          LOG.trace("Text after: " + processedText.toString());
        }
      } // next action
    } // next text marker

    boolean shouldProcess = shouldProcessStack.peek();
    boolean shouldOutput = shouldOutputStack.peek();

    if (shouldProcess) {
      insertionPoints.put(processedText.length(), currentPos);
      processedText.append(rawText.subSequence(currentPos, rawText.length()));
    }

    if (shouldOutput && !shouldProcess) {
      leftoverOutput = leftoverOutput + rawText.subSequence(outputPos, rawText.length());
    }

    String finalProcessedText = processedText.toString();
    if (LOG.isTraceEnabled())
      LOG.trace("Text after processing: " + finalProcessedText);
    sentenceHolder.setProcessedText(finalProcessedText);

    int lastIndex = 0;
    int lastOriginalIndex = 0;
    for (Entry<Integer, Integer> insertionPoint : insertionPoints.entrySet()) {
      int j = 0;
      for (int i = lastIndex; i < insertionPoint.getKey(); i++) {
        sentenceHolder.addOriginalIndex(originalIndexProcessed + lastOriginalIndex + j);
        j++;
      }
      lastIndex = insertionPoint.getKey();
      lastOriginalIndex = insertionPoint.getValue();
    }

    if (lastIndex < sentenceHolder.getProcessedText().length()) {
      int j = 0;
      for (int i = lastIndex; i < sentenceHolder.getProcessedText().length(); i++) {
        sentenceHolder.addOriginalIndex(originalIndexProcessed + lastOriginalIndex + j);
        j++;
      }
    }

    originalIndexProcessed += rawText.length();

    this.sentenceHolder = sentenceHolder;
    return sentenceHolder;

  }

  private void addOutputText(SentenceHolder holder, int position, CharSequence text) {
    holder.addOriginalTextSegment(position, leftoverOutput + text);
    leftoverOutput = "";
  }

  /**
   * The raw position in which text processing should start.
   * 
   * @return
   */
  protected abstract int getTextProcessingStart();

  /**
   * The raw position in which text processing should end.
   * 
   * @return
   */
  protected abstract int getTextProcessingEnd();

  protected abstract SentenceHolder getPreviousSentenceHolder();

  /**
   * The sentence holder in which we want to detect sentences.
   * 
   * @return
   */
  protected abstract SentenceHolder getCurrentSentenceHolder();

  protected abstract SentenceHolder getNextSentenceHolder();

  /**
   * Return processed text ready for sentence detection.
   * 
   * It has sentence break and non-sentence-break annotations inherited from
   * the present RawTextProcessor. Any sentence-break annotations added will
   * automatically get reflected in the current RollingTextBlock.
   * 
   * @return
   */
  public final AnnotatedText getProcessedText() {
    LOG.trace("getProcessedTextBlock");

    int textStartPos = this.getTextProcessingStart();
    int textEndPos = this.getTextProcessingEnd();
    SentenceHolder prevHolder = this.getPreviousSentenceHolder();
    SentenceHolder currentHolder = this.getCurrentSentenceHolder();
    SentenceHolder nextHolder = this.getNextSentenceHolder();

    StringBuilder sb = new StringBuilder();
    String processedText1 = prevHolder.getProcessedText();
    String processedText2 = currentHolder.getProcessedText();
    String processedText3 = nextHolder.getProcessedText();
    sb.append(processedText1);
    sb.append(processedText2);
    sb.append(processedText3);
    String processedText = sb.toString();

    List<Annotation<RawTextMarker>> myAnnotations = this.getAnnotations(RawTextMarker.class);
    List<Annotation<RawTextMarker>> hisAnnotations = new ArrayList<>();

    int prevHolderOriginalIndex = prevHolder.getOriginalStartIndex();
    for (Annotation<RawTextMarker> myAnnotation : myAnnotations) {
      if ((myAnnotation.getStart() >= textStartPos && myAnnotation.getStart() < textEndPos)
          || ((myAnnotation.getEnd() >= textStartPos && myAnnotation.getEnd() < textEndPos))) {
        int originalStart = prevHolderOriginalIndex + myAnnotation.getStart();
        int originalEnd = prevHolderOriginalIndex + myAnnotation.getEnd();
        int localStart = processedText1.length();
        if (originalStart >= currentHolder.getOriginalStartIndex())
          localStart += currentHolder.getIndex(originalStart);
        int localEnd = processedText1.length() + currentHolder.getIndex(originalEnd);

        Annotation<RawTextMarker> hisAnnotation = myAnnotation.getAnnotation(localStart, localEnd);
        hisAnnotations.add(hisAnnotation);
      }
    }

    if (LOG.isTraceEnabled()) {
      LOG.trace("raw annotations: " + myAnnotations);
      LOG.trace("processed annotations: " + hisAnnotations);
    }

    AnnotatedText processedTextBlock = new AnnotatedText(processedText, processedText1.length(), processedText1.length() + processedText2.length());
    processedTextBlock.addAnnotations(hisAnnotations);

    processedTextBlock.addObserver(new AnnotationObserver() {
      // an observer which adds any annotations added to the
      // processedTextBlock back to myself, at the correct position
      @Override
      public <T extends Serializable> void beforeAddAnnotations(AnnotatedText subject, List<Annotation<T>> annotations) {
        int offset = textStartPos;

        int length1 = prevHolder.getProcessedText().length();
        int length2 = currentHolder.getProcessedText().length();

        int sentence2HolderStart = currentHolder.getOriginalStartIndex();
        List<Annotation<T>> newAnnotations = new ArrayList<>();
        for (Annotation<T> annotation : annotations) {
          int originalStart = -1;
          if (annotation.getStart() < length1)
            originalStart = prevHolder.getOriginalIndex(annotation.getStart());
          else if (annotation.getStart() < length1 + length2)
            originalStart = currentHolder.getOriginalIndex(annotation.getStart() - length1);
          if (originalStart >= 0) {
            int originalEnd = -1;
            if (annotation.getEnd() <= length1 + length2)
              originalEnd = currentHolder.getOriginalIndex(annotation.getEnd() - length1);
            else
              originalEnd = nextHolder.getOriginalIndex(annotation.getEnd() - (length1 + length2));

            if (originalEnd >= 0) {
              Annotation<T> newAnnotation = annotation.getAnnotation(originalStart - sentence2HolderStart + offset,
                  originalEnd - sentence2HolderStart + offset);
              newAnnotations.add(newAnnotation);

              if (annotation.getData() instanceof SentenceBoundary) {
                @SuppressWarnings("unchecked")
                Annotation<SentenceBoundary> sentenceBoundary = (Annotation<SentenceBoundary>) annotation;
                sentenceBoundaries.add(sentenceBoundary);
              }
            }
          }
        }
        RawTextProcessor.this.addAnnotations(newAnnotations);

        if (LOG.isTraceEnabled()) {
          LOG.trace("ProcessedTextBlock Annotations received: " + annotations);
          LOG.trace("ProcessedTextBlock Annotations added: " + newAnnotations);
        }
      }

      @Override
      public <T extends Serializable> void afterAddAnnotations(AnnotatedText subject) {
      }
    });

    return processedTextBlock;
  }

  /**
   * Get a list of sentences currently detected. All sentences will be
   * complete - if the list ends with an incomplete sentence it is kept for
   * another round.
   * 
   * @return
   */
  public final List<Sentence> getDetectedSentences() {
    SentenceHolder prevHolder = this.getPreviousSentenceHolder();
    SentenceHolder currentHolder = this.getCurrentSentenceHolder();

    for (Annotation<SentenceBoundary> sentenceBoundary : sentenceBoundaries) {
      currentHolder.addSentenceBoundary(sentenceBoundary.getStart() - prevHolder.getProcessedText().length());
      currentHolder.addSentenceBoundary(sentenceBoundary.getEnd() - prevHolder.getProcessedText().length());
    }

    List<Sentence> sentences = currentHolder.getDetectedSentences(leftover);
    leftover = null;
    if (sentences.size() > 0) {
      Sentence lastSentence = sentences.get(sentences.size() - 1);
      if (!lastSentence.isComplete()) {
        leftover = lastSentence;
        if (LOG.isTraceEnabled())
          LOG.trace("Set leftover to: " + leftover.toString());
        sentences.remove(sentences.size() - 1);
      }
    }

    // ensure that sentence annotations get added to the raw text as well
    for (Sentence sentence : sentences) {
      sentence.addObserver(new AnnotationObserver() {
        int myOrigin = RawTextProcessor.this.originalStartIndex;

        @Override
        public <T extends Serializable> void beforeAddAnnotations(AnnotatedText subject, List<Annotation<T>> annotations) {
          List<Annotation<T>> newAnnotations = new ArrayList<>();
          for (Annotation<T> annotation : annotations) {
            int originalStart = sentence.getOriginalIndex(annotation.getStart());
            int originalEnd = sentence.getOriginalIndex(annotation.getEnd());
            Annotation<T> newAnnotation = annotation.getAnnotation(originalStart - myOrigin, originalEnd - myOrigin);
            newAnnotations.add(newAnnotation);
          }
          RawTextProcessor.this.addAnnotations(newAnnotations);
        }

        @Override
        public <T extends Serializable> void afterAddAnnotations(AnnotatedText subject) {
        }
      });
    }

    // If we have any leftover original text segments, copy them over they
    // are necessarily at position 0 - since otherwise they would
    // have gotten added to the leftover sentence. The only case where there
    // isn't a leftover sentence is the case where
    // the sentenceHolder boundary happens to be a sentence boundary, hence
    // position 0.
    if (currentHolder.getOriginalTextSegments().size() > 0) {
      String fileName = "";
      File file = null;

      if (leftover == null) {
        leftover = new Sentence("", fileName, file, session);
      }
      StringBuilder segmentsToInsert = new StringBuilder();

      if (leftover.getLeftoverOriginalText().length() > 0)
        segmentsToInsert.append(session.getOutputDivider());

      for (String originalTextSegment : currentHolder.getOriginalTextSegments().values()) {
        segmentsToInsert.append(originalTextSegment);
      }

      leftover.setLeftoverOriginalText(leftover.getLeftoverOriginalText() + segmentsToInsert.toString());
    }

    return sentences;
  }

  public String getLeftoverOriginalText() {
    if (leftover != null) {
      return leftover.getLeftoverOriginalText();
    } else {
      return "";
    }
  }

  /**
   * The index in the original file or text at which the current raw text
   * starts.
   * 
   * @return
   */
  public int getOriginalStartIndex() {
    return originalStartIndex;
  }

  @Override
  public void onNextFile(File file) {
    this.lineNumber = 1;
    this.originalIndexProcessed = 0;
    this.leftoverNewline = 0;
  }
}
