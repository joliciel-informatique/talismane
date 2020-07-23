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
package com.joliciel.talismane.parser;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;

import com.typesafe.config.ConfigFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joliciel.talismane.TalismaneException;
import com.joliciel.talismane.TalismaneSession;
import com.typesafe.config.Config;

/**
 * A factory class for getting a dependency parser from the configuration.
 * 
 * @author Assaf Urieli
 *
 */
public class Parsers {
  @SuppressWarnings("unused")
  private static final Logger LOG = LoggerFactory.getLogger(Parsers.class);
  private static final Map<String, Parser> parserMap = new HashMap<>();

  public static Parser getParser(String sessionId) throws IOException, TalismaneException, ClassNotFoundException, ReflectiveOperationException {
    Parser parser = parserMap.get(sessionId);
    if (parser == null) {
      Config config = ConfigFactory.load();
      Config parserConfig = config.getConfig("talismane.core." + sessionId + ".parser");
      String className = parserConfig.getString("parser");

      @SuppressWarnings("rawtypes")
      Class untypedClass = Class.forName(className);
      if (!Parser.class.isAssignableFrom(untypedClass))
        throw new TalismaneException("Class " + className + " does not implement interface " + Parser.class.getSimpleName());

      @SuppressWarnings("unchecked")
      Class<? extends Parser> clazz = untypedClass;

      Constructor<? extends Parser> cons = null;

      if (cons == null) {
        try {
          cons = clazz.getConstructor(String.class);
        } catch (NoSuchMethodException e) {
          // do nothing
        }
        if (cons != null) {
          parser = cons.newInstance(sessionId);
        } else {
          throw new TalismaneException("No constructor found with correct signature for: " + className);
        }
      }

      parserMap.put(sessionId, parser);
    }
    return parser.cloneParser();
  }
}
