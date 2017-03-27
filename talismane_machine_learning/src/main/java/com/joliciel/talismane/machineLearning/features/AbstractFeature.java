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

import java.util.ArrayList;
import java.util.List;

/**
 * An Abstract base class for features, basically defining feature equality.
 * 
 * @author Assaf Urieli
 *
 */
public abstract class AbstractFeature<T, Y> implements Feature<T, Y>, Comparable<Feature<T, ?>> {

  private String name = null;
  private String groupName = null;
  private List<Feature<T, ?>> arguments = new ArrayList<Feature<T, ?>>();
  private boolean topLevelFeature = false;

  public AbstractFeature() {
    super();
  }

  @Override
  public final String getName() {
    if (name == null) {
      name = this.getClass().getSimpleName();
    }
    return name;
  }

  @Override
  public final void setName(String name) {
    this.name = name;
  }

  @Override
  public String getCollectionName() {
    if (groupName == null) {
      groupName = this.getName();
    }
    return groupName;
  }

  @Override
  public void setCollectionName(String groupName) {
    this.groupName = groupName;
  }

  @Override
  public final int hashCode() {
    return this.getName().hashCode();
  }

  @Override
  public final boolean equals(Object obj) {
    if (!(obj instanceof Feature))
      return false;
    Feature<?, ?> feature = (Feature<?, ?>) obj;
    return (feature.getName().equals(this.getName()));
  }

  protected final FeatureResult<Y> generateResult(Y outcome) {
    if (outcome == null)
      return null;
    return new FeatureResultImpl<Y>(this, outcome);
  }

  @Override
  public final int compareTo(Feature<T, ?> o) {
    return this.getName().compareTo(o.getName());
  }

  @SuppressWarnings({ "rawtypes" })
  @Override
  public Class<? extends Feature> getFeatureType() {
    if (this instanceof BooleanFeature) {
      return BooleanFeature.class;
    }
    if (this instanceof StringFeature) {
      return StringFeature.class;
    }
    if (this instanceof DoubleFeature) {
      return DoubleFeature.class;
    }
    if (this instanceof IntegerFeature) {
      return IntegerFeature.class;
    }
    throw new RuntimeException("Unknown feature return type for " + this.getName());
  }

  @Override
  public void addArgument(Feature<T, ?> argument) {
    this.arguments.add(argument);
  }

  @Override
  public List<Feature<T, ?>> getArguments() {
    return this.arguments;
  }

  @Override
  public boolean isTopLevelFeature() {
    return topLevelFeature;
  }

  @Override
  public void setTopLevelFeature(boolean topLevelFeature) {
    this.topLevelFeature = topLevelFeature;
  }

  @Override
  public String toString() {
    return this.getName();
  }

}
