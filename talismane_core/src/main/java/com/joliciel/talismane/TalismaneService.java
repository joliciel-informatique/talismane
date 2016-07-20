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
package com.joliciel.talismane;

import java.io.File;
import java.util.Map;

import com.typesafe.config.Config;

/**
 * A service for returning top-level talismane objects. In general, a
 * TalismaneConfig must have either a LanguageImplementation available, or a
 * unique sessionId.
 * 
 * @author Assaf Urieli
 *
 */
public interface TalismaneService {
	public TalismaneSession getTalismaneSession();

	/**
	 * A config using all default parameters.
	 */
	public TalismaneConfig getTalismaneConfig(String sessionId);

	/**
	 * Load using a previously constructed implementation, and all default
	 * parameters.
	 */
	public TalismaneConfig getTalismaneConfig(LanguageImplementation implementation);

	/**
	 * Like {@link #getTalismaneConfig(Config, String)} but with an argument
	 * list instead of a configuration.
	 */
	public TalismaneConfig getTalismaneConfig(Map<String, String> args, String sessionId);

	/**
	 * Like {@link #getTalismaneConfig(Config, LanguageImplementation)} but with
	 * an argument list instead of a configuration.
	 */
	public TalismaneConfig getTalismaneConfig(Map<String, String> args, LanguageImplementation implementation);

	/**
	 * Like {@link #getTalismaneConfig(Config, File, String)} but with an
	 * argument list instead of a configuration.
	 */
	public TalismaneConfig getTalismaneConfig(Map<String, String> args, File baseDir, String sessionId);

	/**
	 * Like {@link #getTalismaneConfig(Config, File, LanguageImplementation)}
	 * but with an argument list instead of a configuration.
	 */
	public TalismaneConfig getTalismaneConfig(Map<String, String> args, File baseDir, LanguageImplementation implementation);

	/**
	 * Like {@link #getTalismaneConfig(Config, File, LanguageImplementation)}
	 * but with the current working directory taken as the base dir.
	 */
	public TalismaneConfig getTalismaneConfig(Config config, LanguageImplementation implementation);

	/**
	 * Like {@link #getTalismaneConfig(Config, File, String)} but with the
	 * current working directory taken as the base dir.
	 */
	public TalismaneConfig getTalismaneConfig(Config config, String sessionId);

	/**
	 * A TalismaneConfig created with a specific language implementation.
	 * 
	 * @param config
	 *            configuration parameters
	 * @param baseDir
	 *            the directory from which to find relative directories in the
	 *            configuration parameters
	 * @param implementation
	 *            a specific language implementation, typically coded in a
	 *            separate package, or else a previously loaded generic
	 *            implementation.
	 */
	public TalismaneConfig getTalismaneConfig(Config config, File baseDir, LanguageImplementation implementation);

	/**
	 * A TalismaneConfig created without a language implementation: typically,
	 * all parameters are provided via the configuration, or a language pack is
	 * provided in the configuration.
	 * 
	 * @param config
	 *            configuration parameters
	 * @param baseDir
	 *            the directory from which to find relative directories in the
	 *            configuration parameters
	 * @param sessionId
	 *            a unique sessionId
	 */
	public TalismaneConfig getTalismaneConfig(Config config, File baseDir, String sessionId);

}
