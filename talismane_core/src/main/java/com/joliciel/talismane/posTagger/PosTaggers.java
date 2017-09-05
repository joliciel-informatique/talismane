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
package com.joliciel.talismane.posTagger;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joliciel.talismane.TalismaneException;
import com.joliciel.talismane.TalismaneSession;
import com.typesafe.config.Config;

/**
 * A factory class for getting a pos-tagger from the configuration.
 * 
 * @author Assaf Urieli
 *
 */
public class PosTaggers {
  @SuppressWarnings("unused")
  private static final Logger LOG = LoggerFactory.getLogger(PosTaggers.class);
  private static final Map<String, PosTagger> posTaggerMap = new HashMap<>();

  public static PosTagger getPosTagger(TalismaneSession session) throws IOException, TalismaneException, ClassNotFoundException, ReflectiveOperationException {
    PosTagger posTagger = null;
    if (session.getId() != null)
      posTagger = posTaggerMap.get(session.getId());
    if (posTagger == null) {
      Config config = session.getConfig();
      Config posTaggerConfig = config.getConfig("talismane.core." + session.getId() + ".pos-tagger");
      String className = posTaggerConfig.getString("pos-tagger");

      @SuppressWarnings("rawtypes")
      Class untypedClass = Class.forName(className);
      if (!PosTagger.class.isAssignableFrom(untypedClass))
        throw new TalismaneException("Class " + className + " does not implement interface " + PosTagger.class.getSimpleName());

      @SuppressWarnings("unchecked")
      Class<? extends PosTagger> clazz = untypedClass;

      Constructor<? extends PosTagger> cons = null;

      if (cons == null) {
        try {
          cons = clazz.getConstructor(TalismaneSession.class);
        } catch (NoSuchMethodException e) {
          // do nothing
        }
        if (cons != null) {
          posTagger = cons.newInstance(session);
        } else {
          throw new TalismaneException("No constructor found with correct signature for: " + className);
        }
      }

      if (session.getId() != null)
        posTaggerMap.put(session.getId(), posTagger);
    }
    return posTagger.clonePosTagger();
  }
}
