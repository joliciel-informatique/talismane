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
package com.joliciel.talismane.filters;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joliciel.talismane.AnnotatedText;
import com.joliciel.talismane.Annotation;
import com.joliciel.talismane.AnnotationObserver;
import com.joliciel.talismane.TalismaneSession;

/**
 * A block of raw text, always containing four sub-blocks, which are rolled in
 * from right to left. The reasoning behind this is that filters and features
 * cannot be applied to a block without knowing its context to the left and
 * right. If we attempt to apply filters to a single block only, we might not
 * match a regex that crosses the block boundaries at its start or end. We
 * therefore apply filters to a single block at a time, but provide context to
 * the left and right.<br/>
 * <br/>
 * In the case of raw text filters, e.g. XML filters which tell the system which
 * parts of the file to analyse, or which correct XML encoding issues (e.g. &lt;
 * becomes &amp;lt;), we always apply these filters to the 3rd sub-block, with
 * block 4 as the right-hand context. Since blocks are rolled from right to
 * left, and since we begin with four empty blocks, any filters crossing the
 * border between blocks 2 and 3 have already been added by the predecessor. The
 * filters should be applied to the AnnotatedText returned by
 * {@link RollingTextBlock#getRawTextBlock()}, which encapsulates block 3 and 4
 * with analysis ending at the end of block 3. Annotations added to this object
 * will automatically get added to the parent RollingTextBlock. This system
 * ensures that blocks 1, 2 and 3 have always been "processed" (with block 4
 * serving only as context for correct processing of block 3). <br/>
 * <br/>
 * Sentence detection has to be performed on processed text, since the training
 * corpus is of course a simple text corpus and we cannot apply probabilistic
 * decisions on a formatted file, such as XML. But sentence detection also needs
 * a context to the right and left, since some features may need to look beyond
 * a processed text block boundary. Therefore, sentence detection is always
 * performed on block 2 of processed text, with blocks 1 and 3 as the left and
 * right context. The object required for sentence detection can be requested
 * through {@link #getProcessedText()}. Annotations added to this object will
 * automatically get added to the parent RollingTextBlock, hence enabling
 * sentence extraction.<br/>
 * <br/>
 * <br/>
 * Typical usage:
 * 
 * <pre>
 * RollingTextBlock rollingTextBlock = new RollingTextBlock(session, processByDefault);
 * List&lt;Sentence&gt; sentences = new ArrayList&lt;&gt;();
 * 
 * // ... find segments ...
 * 
 * // add three segments at the end
 * segments.add("");
 * segments.add("");
 * segments.add("");
 * for (String segment : segments) {
 * 	// roll in a new block 4, and roll the other blocks leftwards
 * 	rollingTextBlock = rollingTextBlock.roll(segment);
 * 
 * 	// annotate block 3 with raw text filters
 * 	AnnotatedText rawTextBlock = rollingTextBlock.getRawTextBlock();
 * 
 * 	for (RawTextFilter textMarkerFilter : session.getTextFilters()) {
 * 		textMarkerFilter.annotate(rawTextBlock);
 * 	}
 * 
 * 	// detect sentences in block 2 using the sentence detector
 * 	AnnotatedText processedText = rollingTextBlock.getProcessedText();
 * 	sentenceDetector.detectSentences(processedText);
 * 
 * 	// get the sentences detected in block 2
 * 	List&lt;Sentence&gt; theSentences = rollingTextBlock.getDetectedSentences();
 * 	for (Sentence sentence : theSentences) {
 * 		sentences.add(sentence);
 * 	}
 * }
 * </pre>
 * 
 * @author Assaf Urieli
 *
 */
public class RollingTextBlock extends RawTextProcessor {
	private static final Logger LOG = LoggerFactory.getLogger(RollingTextBlock.class);

	private final String block1;
	private final String block2;
	private final String block3;
	private final String block4;

	private String fileName = "";
	private File file = null;

	private final SentenceHolder sentenceHolder1;
	private final SentenceHolder sentenceHolder2;
	private SentenceHolder sentenceHolder3 = null;

	private final TalismaneSession session;

	/**
	 * Creates a new RollingTextBlock with prev, current and next all set to
	 * empty strings.
	 */
	public RollingTextBlock(boolean processByDefault, TalismaneSession session) {
		super("", processByDefault, session);
		this.session = session;
		this.block1 = "";
		this.block2 = "";
		this.block3 = "";
		this.block4 = "";

		this.sentenceHolder1 = new SentenceHolder(session, 0, true);
		this.sentenceHolder1.setProcessedText("");
		this.sentenceHolder2 = new SentenceHolder(session, 0, true);
		this.sentenceHolder2.setProcessedText("");
	}

	private RollingTextBlock(RollingTextBlock predecessor, String nextText, List<Annotation<?>> annotations) {
		super(predecessor, predecessor.block2 + predecessor.block3 + predecessor.block4 + nextText, predecessor.block2.length() + predecessor.block3.length(),
				predecessor.block2.length() + predecessor.block3.length() + predecessor.block4.length(), annotations,
				predecessor.getOriginalStartIndex() + predecessor.block1.length());
		this.block1 = predecessor.block2;
		this.block2 = predecessor.block3;
		this.block3 = predecessor.block4;
		this.block4 = nextText;

		this.session = predecessor.session;

		this.fileName = predecessor.fileName;
		this.file = predecessor.file;

		this.sentenceHolder1 = predecessor.sentenceHolder2;
		this.sentenceHolder2 = predecessor.sentenceHolder3;

		if (LOG.isTraceEnabled()) {
			LOG.trace("After roll: ");
			LOG.trace("block1: " + block1.replace('\n', '¶').replace('\r', '¶'));
			LOG.trace("block2: " + block2.replace('\n', '¶').replace('\r', '¶'));
			LOG.trace("block3: " + block3.replace('\n', '¶').replace('\r', '¶'));
			LOG.trace("block4: " + block4.replace('\n', '¶').replace('\r', '¶'));
		}
	}

	/**
	 * Creates a new RollingTextBlock.<br/>
	 * Moves block2 → block1, block3 → block2, block4 → block3, and nextText →
	 * block4.<br/>
	 * <br/>
	 * All existing annotations have their start and end decremented by
	 * block1.length(). If the new start &lt; 0, start = 0, if new end &lt; 0,
	 * annotation dropped.<br/>
	 * <br/>
	 * If the current block3 has not yet been processed, it is processed when
	 * rolling, thus ensuring that we always have blocks 1, 2 and 3 processed.
	 * <br/>
	 * 
	 * @param nextText
	 *            the next text segment to add onto this rolling text block
	 * @return a new text block as described above
	 */
	public RollingTextBlock roll(String nextText) {
		if (LOG.isTraceEnabled()) {
			LOG.trace("roll");
			LOG.trace("nextText: " + nextText.replace('\n', '¶').replace('\r', '¶'));
		}
		this.processText();

		int prevLength = this.block1.length();
		List<Annotation<?>> annotations = new ArrayList<>();
		for (Annotation<?> annotation : this.getAnnotations()) {
			int newStart = annotation.getStart() - prevLength;
			int newEnd = annotation.getEnd() - prevLength;
			if (newEnd > 0 || (newStart == 0 && newEnd == 0)) {
				if (newStart < 0)
					newStart = 0;
				Annotation<?> newAnnotation = annotation.getAnnotation(newStart, newEnd);
				annotations.add(newAnnotation);
				if (LOG.isTraceEnabled()) {
					LOG.trace("Moved " + annotation + " to " + newStart + ", " + newEnd);
				}
			} else {
				if (LOG.isTraceEnabled()) {
					LOG.trace("Removed annotation " + annotation + ", newEnd = " + newEnd);
				}
			}
		}
		RollingTextBlock textBlock = new RollingTextBlock(this, nextText, annotations);

		return textBlock;
	}

	/**
	 * Get a raw text block for annotation by filters. This covers blocks 3 and
	 * 4 only of the current RollingTextBlock, with analysis end at the end of
	 * block3. It is assumed that annotations crossing block 2 and 3 were
	 * already added by a predecessor.
	 */
	public AnnotatedText getRawTextBlock() {
		AnnotatedText rawTextBlock = new AnnotatedText(this.block3 + this.block4, 0, this.block3.length());
		rawTextBlock.addObserver(new AnnotationObserver() {

			@Override
			public <T> void beforeAddAnnotations(AnnotatedText subject, List<Annotation<T>> annotations) {
				if (annotations.size() > 0) {
					int offset = RollingTextBlock.this.block1.length() + RollingTextBlock.this.block2.length();
					List<Annotation<T>> newAnnotations = new ArrayList<>();
					for (Annotation<T> annotation : annotations) {
						Annotation<T> newAnnotation = annotation.getAnnotation(annotation.getStart() + offset, annotation.getEnd() + offset);
						newAnnotations.add(newAnnotation);
					}
					RollingTextBlock.this.addAnnotations(newAnnotations);

					if (LOG.isTraceEnabled()) {
						LOG.trace("RawTextBlock Annotations received: " + annotations);
						LOG.trace("RawTextBlock Annotations added: " + newAnnotations);
					}
				}
			}

			@Override
			public <T> void afterAddAnnotations(AnnotatedText subject) {
			}
		});
		return rawTextBlock;
	}

	/**
	 * Processes the current text based on annotations added to block 3, and
	 * returns a SentenceHolder.
	 * 
	 * @return SentenceHolder to retrieve the sentences.
	 */
	private void processText() {
		if (this.sentenceHolder3 != null)
			return;

		int textStartPos = this.block1.length() + this.block2.length();
		int textEndPos = this.block1.length() + this.block2.length() + this.block3.length();

		this.sentenceHolder3 = super.processText(textStartPos, textEndPos, this.block3, this.block4.length() == 0);
	}

	@Override
	protected int getTextProcessingStart() {
		return this.block1.length();
	}

	@Override
	protected int getTextProcessingEnd() {
		return this.block1.length() + this.block2.length();
	}

	@Override
	protected SentenceHolder getPreviousSentenceHolder() {
		return this.sentenceHolder1;
	}

	@Override
	protected SentenceHolder getCurrentSentenceHolder() {
		return this.sentenceHolder2;
	}

	@Override
	protected SentenceHolder getNextSentenceHolder() {
		this.processText();
		return this.sentenceHolder3;
	}

	@Override
	public void onNextFile(File file) {
		this.file = file;
		this.fileName = file.getPath();
		super.onNextFile(file);
	}

}
