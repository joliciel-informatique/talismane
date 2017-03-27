///////////////////////////////////////////////////////////////////////////////
//Copyright (C) 2017 Joliciel Informatique
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
package com.joliciel.talismane.corpus;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joliciel.talismane.corpus.CorpusLine.CorpusElement;
import com.typesafe.config.Config;

/**
 * A single rule used to transform the elements on a corpus line.
 * 
 * @author Assaf Urieli
 *
 */
public class CorpusRule {
  private static final Logger LOG = LoggerFactory.getLogger(CorpusRule.class);
  private final Map<CorpusElement, Pattern> criteria;
  private final Map<CorpusElement, String> actions;

  public CorpusRule(Map<CorpusElement, Pattern> criteria, Map<CorpusElement, String> actions) {
    this.criteria = criteria;
    this.actions = actions;
  }

  /**
   * Reads "criteria" and "actions" from a configuration.<br/>
   * "criteria" is a map of {@link CorpusElement} to regex, which need to
   * match the full value of the element.<br/>
   * "actions" is a map of {@link CorpusElement} to values, which will be
   * updated when the rule is matched.
   */
  public CorpusRule(Config config) {
    criteria = new HashMap<>();
    Config criteriaConfig = config.getConfig("criteria");
    for (String key : criteriaConfig.root().keySet()) {
      CorpusElement element = CorpusElement.valueOf(key);
      String regex = criteriaConfig.getString(key);
      Pattern pattern = Pattern.compile(regex);
      criteria.put(element, pattern);
    }
    actions = new HashMap<>();
    Config actionsConfig = config.getConfig("actions");
    for (String key : actionsConfig.root().keySet()) {
      CorpusElement element = CorpusElement.valueOf(key);
      String value = actionsConfig.getString(key);
      actions.put(element, value);
    }
  }

  /**
   * Apply the rule to a corpus line, and add the values that need to be
   * updated to the values map, as long as no value yet exists for the element
   * being updated. Thus, the first rule to match that updates a given element
   * will win, for each element.
   */
  public void apply(CorpusLine corpusLine, Map<CorpusElement, String> values) {
    boolean match = true;
    for (CorpusElement element : criteria.keySet()) {
      if (corpusLine.hasElement(element)) {
        Pattern pattern = criteria.get(element);
        if (!pattern.matcher(corpusLine.getElement(element)).matches()) {
          match = false;
          break;
        }
      } else {
        match = false;
        break;
      }
    }
    if (match) {
      if (LOG.isTraceEnabled()) {
        LOG.trace("Rule " + this.toString() + " matched line " + corpusLine);
      }
      for (CorpusElement element : actions.keySet()) {
        if (!values.containsKey(element)) {
          String value = actions.get(element);
          values.put(element, value);
        }
      }
    }
  }

  /**
   * The criteria to be met - ALL criteria must match.
   */
  public Map<CorpusElement, Pattern> getCriteria() {
    return criteria;
  }

  /**
   * The actions to be applied if the criteria are met.
   */
  public Map<CorpusElement, String> getActions() {
    return actions;
  }

  @Override
  public String toString() {
    return "CorpusRule [criteria=" + criteria + ", actions=" + actions + "]";
  }

}
