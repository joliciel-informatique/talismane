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
package com.joliciel.talismane.machineLearning.features;

import java.util.List;

import com.joliciel.talismane.TalismaneException;

/**
 * A single feature that checks a given object T (e.g. a token) and returns an
 * outcome. The outcome Y could be a String (handled as a nominal class), a
 * Boolean, a Double (generally between 0 and 1), an Integer, etc. A feature's
 * constructor should not include any native types - these should be replaced
 * respectively by StringFeature, IntegerFeature, DoubleFeature and
 * BooleanFeature, to make it possible for a feature to use another feature's
 * result as its argument. Native types in constructors will cause an exception
 * to be thrown when trying to find the constructor in AbstractFeatureParser.
 * 
 * @author Assaf Urieli
 * 
 * @param <T>
 *          the type of the object (or context) being tested
 * @param <Y>
 *          the type of the feature outcome (the feature return type)
 */
public interface Feature<T, Y> extends Comparable<Feature<T, ?>> {

  /**
   * Check the feature on this context and return the result.
   * 
   * @throws TalismaneException
   */
  public FeatureResult<Y> check(T context, RuntimeEnvironment env) throws TalismaneException;

  /**
   * The name of this feature.
   */
  public String getName();

  /**
   * Set a name for this feature, that will over-write the default name.
   */
  public void setName(String name);

  /**
   * Returns the feature's return type interface, e.g. BooleanFeature,
   * StringFeature or DoubleFeature.
   */
  @SuppressWarnings("rawtypes")
  public Class<? extends Feature> getFeatureType();

  /**
   * A single feature descriptor can result in the generation of multiple
   * features (e.g. in the case where it refers to a range of indexes). The
   * collection name groups all these features under one name - that of the
   * feature descriptor. It can used, for example, for measuring the performance
   * of the entire collection of features together.
   */
  public String getCollectionName();

  public void setCollectionName(String groupName);

  /**
   * Explicitly add a constructor argument to this feature, so that the system
   * can recursively iterate through a feature's argument tree.
   */
  public void addArgument(Feature<T, ?> argument);

  /**
   * The features fed to this feature as constructor arguments.
   */
  public List<Feature<T, ?>> getArguments();

  /**
   * Was this feature defined in a top-level descriptor, or as part of another
   * feature.
   */
  public boolean isTopLevelFeature();

  public void setTopLevelFeature(boolean topLevelFeature);
}
