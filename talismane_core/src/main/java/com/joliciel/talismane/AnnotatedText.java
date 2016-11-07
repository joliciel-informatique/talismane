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
	private List<Annotation<?>> annotations;
	private final List<AnnotationObserver> observers = new ArrayList<>();

	/**
	 * Construct the initial instance with no annotations.
	 * 
	 * @param text
	 *            the text to annotate
	 */
	public AnnotatedText(CharSequence text) {
		this.text = text;
		this.annotations = Collections.emptyList();
	}

	/**
	 * Text to which annotations have been applied.
	 */
	public CharSequence getText() {
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
	 * Add annotations to the current text.
	 * 
	 * @param annotations
	 */
	public <T> void addAnnotations(List<Annotation<T>> annotations) {
		for (AnnotationObserver observer : observers) {
			observer.beforeAddAnnotations(this, annotations);
		}
		List<Annotation<?>> newAnnotations = new ArrayList<>(this.annotations.size() + annotations.size());
		newAnnotations.addAll(this.annotations);
		newAnnotations.addAll(annotations);
		Collections.sort(newAnnotations);
		this.annotations = Collections.unmodifiableList(newAnnotations);
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
	public <T> List<Annotation<T>> getAnnotations(Class<T> clazz) {
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
