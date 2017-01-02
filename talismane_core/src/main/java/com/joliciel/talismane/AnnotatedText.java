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
package com.joliciel.talismane;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Text with related annotations, and observers when annotations are added.
 * 
 * @author Assaf Urieli
 *
 */
public class AnnotatedText {
	private final CharSequence text;
	private final int analysisStart;
	private final int analysisEnd;
	private List<Annotation<?>> annotations;
	private final List<AnnotationObserver> observers = new ArrayList<>();

	/**
	 * Construct an annotated text with no annotations, and analysis start at
	 * text start, and analysis end text end.
	 * 
	 * @param text
	 *            the text to annotate
	 */
	public AnnotatedText(CharSequence text) {
		this(text, 0, text.length());
	}

	/**
	 * Construct an annotated text with an explicit analysis start and end.
	 * 
	 * @param text
	 *            the text to annotate
	 * @param analysisStart
	 *            the analysis start-point.
	 * @param analysisEnd
	 *            the analysis end-point
	 */
	public AnnotatedText(CharSequence text, int analysisStart, int analysisEnd) {
		this.text = text;
		this.analysisStart = analysisStart;
		this.analysisEnd = analysisEnd;
		this.annotations = Collections.emptyList();
	}

	/**
	 * Construct an annotated text with an explicit analysis end and an initial
	 * list of annotations.
	 * 
	 * @param text
	 * @param analysisEnd
	 * @param annotations
	 */
	public AnnotatedText(CharSequence text, int analysisStart, int analysisEnd, List<Annotation<?>> annotations) {
		this.text = text;
		this.analysisStart = analysisStart;
		this.analysisEnd = analysisEnd;
		Collections.sort(annotations);
		this.annotations = Collections.unmodifiableList(annotations);
	}

	/**
	 * Text to which annotations have been applied.
	 */
	public final CharSequence getText() {
		return text;
	}

	/**
	 * Immutable List of annotations ordered in natural order for the spans, and
	 * original order if spans are equal.
	 */
	public List<Annotation<?>> getAnnotations() {
		return annotations;
	}

	/**
	 * The point in the text beyond which annotations can begin.<br/>
	 * More specifically, {@link Annotation#getStart()} &lt;= analysisStart.
	 * <br/>
	 * This is useful when the annotator needs more context, but must only add
	 * annotations in a particular part of the text.
	 */
	public int getAnalysisStart() {
		return analysisStart;
	}

	/**
	 * The point in the text beyond which no more annotations should begin.<br/>
	 * More specifically, {@link Annotation#getStart()} &lt; analysisEnd.<br/>
	 * This is useful when the annotator needs more context, but must only add
	 * annotations in a particular part of the text.
	 */
	public int getAnalysisEnd() {
		return analysisEnd;
	}

	/**
	 * Add annotations to the current text.
	 * 
	 * @param annotations
	 */
	public <T extends Serializable> void addAnnotations(List<Annotation<T>> annotations) {
		if (annotations.size() > 0) {
			for (AnnotationObserver observer : observers) {
				observer.beforeAddAnnotations(this, annotations);
			}
			List<Annotation<?>> newAnnotations = new ArrayList<>(this.annotations.size() + annotations.size());
			newAnnotations.addAll(this.annotations);
			newAnnotations.addAll(annotations);
			Collections.sort(newAnnotations);
			this.annotations = Collections.unmodifiableList(newAnnotations);

			for (AnnotationObserver observer : observers) {
				observer.afterAddAnnotations(this);
			}

		}
	}

	/**
	 * Add an observer to be notified of annotation events.
	 */
	public void addObserver(AnnotationObserver observer) {
		this.observers.add(observer);
	}

	/**
	 * Return all annotations of a particular type.
	 */
	public <T extends Serializable> List<Annotation<T>> getAnnotations(Class<T> clazz) {
		List<Annotation<T>> typedAnnotations = new ArrayList<>();
		for (Annotation<?> annotation : annotations) {
			if (clazz.isAssignableFrom(annotation.getData().getClass())) {
				@SuppressWarnings("unchecked")
				Annotation<T> typedAnnotation = (Annotation<T>) annotation;
				typedAnnotations.add(typedAnnotation);
			}
		}
		return typedAnnotations;
	}
}
