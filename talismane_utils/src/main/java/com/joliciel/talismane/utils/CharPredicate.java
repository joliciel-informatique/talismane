///////////////////////////////////////////////////////////////////////////////
//Copyright (C) 2016 Safety Data -CFH
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
package com.joliciel.talismane.utils;

import java.util.Objects;
import java.util.function.Predicate;

/**
 * A predicate on character that avoids boxing. Similar to {@link Predicate}.
 * 
 * @author Lucas Satabin
 *
 */
@FunctionalInterface
public interface CharPredicate {

	boolean test(char c);

	default CharPredicate and(Predicate<? super Character> other) {
		Objects.requireNonNull(other);
		return (t) -> test(t) && other.test(t);
	}

	default CharPredicate negate() {
		return (t) -> !test(t);
	}

	default CharPredicate or(Predicate<? super Character> other) {
		Objects.requireNonNull(other);
		return (t) -> test(t) || other.test(t);
	}

	static CharPredicate isEqual(Object targetRef) {
		return (null == targetRef) ? Objects::isNull : object -> targetRef.equals(object);
	}

}
