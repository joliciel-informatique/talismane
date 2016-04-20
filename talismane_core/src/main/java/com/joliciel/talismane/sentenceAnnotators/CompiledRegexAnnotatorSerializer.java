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
package com.joliciel.talismane.sentenceAnnotators;

import java.io.Externalizable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.lang.reflect.Array;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.joliciel.talismane.TalismaneSession;
import com.joliciel.talismane.regex.bytecode.CompiledRegex;
import com.joliciel.talismane.regex.bytecode.CompiledRegexSerializer;
import com.joliciel.talismane.tokeniser.TokenAttribute;
import com.joliciel.talismane.utils.LogUtils;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;

/**
 * Allows for reading and writing {@link CompiledRegexAnnotators} from and to
 * streams. The structure of a serialized {@link CompiledRegexAnnotators} is as
 * follows:
 * <ul>
 * <li>number of filters {@code nbFilters} (int32)</li>
 * <li>for each filter, its serialized for each filter ({@code nbFilters} times)
 * </li>
 * <ul>
 * <li>single token or not (byte)</li>
 * <li>possible sentence boundary (byte)</li>
 * <li>group index (int32)</li>
 * <li>replacement (unicode string)</li>
 * <li>attributes
 * <ul>
 * <li>number of attributes (int32)</li>
 * <li>for each attribute
 * <ul>
 * <li>the attribute name (unicode string)</li>
 * <li>attribute serialized using the
 * {@link Externalizable#writeExternal(java.io.ObjectOutput)} method</li>
 * </ul>
 * </li>
 * </ul>
 * </li>
 * </ul>
 * <li>regular expression representing all filters (as described in
 * {@link CompiledRegexSerializer})</li>
 * </ul>
 * 
 * @author Lucas Satabin
 *
 */
public class CompiledRegexAnnotatorSerializer {

	public static void main(String[] args) throws Exception {

		OptionParser parser = new OptionParser();
		parser.accepts("compileRegexFilters", "compile regex filters");

		OptionSpec<File> filterFileOption = parser.accepts("compiledFiltersFile", "file in which to store the compiled regexes").withRequiredArg().required()
				.ofType(File.class);

		OptionSpec<File> logConfigFileSpec = parser.accepts("logConfigFile", "logback configuration file").withRequiredArg().ofType(File.class);

		if (args.length <= 1) {
			parser.printHelpOn(System.out);
			return;
		}

		OptionSet options = parser.parse(args);

		if (options.has(logConfigFileSpec))
			LogUtils.configureLogging(options.valueOf(logConfigFileSpec));

		File compiledFiltersFile = options.valueOf(filterFileOption);

		Config config = ConfigFactory.load();

		String sessionId = "";
		TalismaneSession talismaneSession = new TalismaneSession(config, sessionId);
		List<SentenceAnnotator> filters = talismaneSession.getSentenceAnnotators().stream()
				.flatMap(t -> t instanceof SentenceAnnotator ? Stream.of(t) : Stream.empty()).collect(Collectors.toList());

		CompiledRegexAnnotators compiledFilters = new CompiledRegexAnnotators(filters, talismaneSession.getWordListFinder());

		try (ObjectOutput out = new ObjectOutputStream(new FileOutputStream(compiledFiltersFile, false))) {
			CompiledRegexAnnotatorSerializer.serialize(out, compiledFilters);
		}

	}

	public static void serialize(ObjectOutput out, CompiledRegexAnnotators filters) throws IOException {
		// write the number of filters
		out.writeInt(filters.indices.length);
		// for each filter write its elements
		for (int i = 0; i < filters.indices.length; i++) {
			// write whether this is a single token
			out.writeBoolean(filters.singleTokens[i]);
			// write the group index
			out.writeInt(filters.indices[i]);
			// write the replacement
			if (filters.replacements[i] != null) {
				out.writeBoolean(true);
				out.writeUTF(filters.replacements[i]);
			} else {
				out.writeBoolean(false);
			}
			// write the attributes
			out.writeInt(filters.attributes[i].size());
			for (Entry<String, TokenAttribute<?>> entry : filters.attributes[i].entrySet()) {
				// write the attribute name
				out.writeUTF(entry.getKey());
				// then the attribute identity
				out.writeUTF(entry.getValue().getClass().getName());
				// and finally the attribute itself
				entry.getValue().writeExternal(out);
			}
		}
		// write the regular expression
		CompiledRegexSerializer.serialize(out, filters.regex);
	}

	public static CompiledRegexAnnotators deserialize(ObjectInput in)
			throws IOException, InstantiationException, IllegalAccessException, ClassNotFoundException {
		// read the number of filters
		final int nbFilters = in.readInt();
		final boolean[] singleTokens = new boolean[nbFilters];
		final int[] indices = new int[nbFilters];
		final String[] replacements = new String[nbFilters];
		@SuppressWarnings("unchecked")
		final Map<String, TokenAttribute<?>>[] attributes = (Map<String, TokenAttribute<?>>[]) Array.newInstance(Map.class, nbFilters);
		for (int i = 0; i < nbFilters; i++) {
			// is this filter a single token
			singleTokens[i] = in.readBoolean();
			// the group index
			indices[i] = in.readInt();
			// the replacement (if any)
			if (in.readBoolean()) {
				replacements[i] = in.readUTF();
			} else {
				replacements[i] = null;
			}
			// the attributes
			// XXX really naive and inefficient way to serialize a map, but we
			// expect the map to be small and this
			// should not be a problem in practice
			int nbAttributes = in.readInt();
			attributes[i] = new HashMap<>();
			for (int j = 0; j < nbAttributes; j++) {
				// read the attribute name
				String name = in.readUTF();
				// the attribute class name
				String className = in.readUTF();
				TokenAttribute<?> attribute = (TokenAttribute<?>) Class.forName(className).newInstance();
				// initialize the attribute
				attribute.readExternal(in);
				attributes[i].put(name, attribute);
			}
		}

		final CompiledRegex regex = CompiledRegexSerializer.deserialize(in);

		return new CompiledRegexAnnotators(regex, indices, replacements, singleTokens, attributes);
	}

}
