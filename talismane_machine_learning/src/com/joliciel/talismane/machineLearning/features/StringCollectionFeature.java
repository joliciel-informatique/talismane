///////////////////////////////////////////////////////////////////////////////
//Copyright (C) 2013 Assaf Urieli
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
package com.joliciel.talismane.machineLearning.features;

import java.util.List;

import com.joliciel.talismane.utils.WeightedOutcome;

/**
 * A feature that returns a collection of strings, each potentially with a weight.
 * Because this can return multiple results at runtime, it is only applicable to contexts
 * implementing HasRuntimeCollectionSupport.
 * @author Assaf Urieli
 *
 * @param <T> must implement HasRuntimeCollectionSupport, will cause runtime error otherwise
 */
public interface StringCollectionFeature<T> extends Feature<T, List<WeightedOutcome<String>>> {

}
