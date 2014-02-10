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
package com.joliciel.talismane.other.corpus;

import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import com.joliciel.talismane.parser.DependencyArc;
import com.joliciel.talismane.parser.ParseConfiguration;
import com.joliciel.talismane.parser.ParseConfigurationProcessor;

/**
 * Modifies a corpus in simple ways, by replacing labels with other ones, or removing dependencies.<br/>
 * The input file has the following format:<br/>
 * GOVPOS\tGOV\tDEPPOS\tDEP\tLABEL\tACTION\tNEWLABEL<br/>
 * Where GOVPOS is the governor's pos-tag, or * for any.<br/>
 * GOV is the governor's word form, or * for any.<br/>
 * DEPPOS is the dependent's pos-tag, or * for any<br/>
 * DEP is the dependent's word form, or * for any<br/>
 * LABEL is the current label, or * for any<br/>
 * ACTION is either Replace or Remove<br/>
 * NEWLABEL is a new label, only required if action is Replace.<br/>
 * Any line starting with # is ignored.
 * @author Assaf Urieli
 *
 */
public class CorpusModifier implements ParseConfigurationProcessor {
	ParseConfigurationProcessor wrappedProcessor = null;
	private List<ModifyCommand> commands = new ArrayList<ModifyCommand>();
	private enum ModifyCommandType {
		Remove,
		Replace
	}
	private static final String WILDCARD = "*";
	
	public CorpusModifier(ParseConfigurationProcessor wrappedProcessor, List<String> commandList) {
		super();
		this.wrappedProcessor = wrappedProcessor;
		for (String command : commandList) {
			if (!command.startsWith("#")) {
				ModifyCommand modifyCommand = new ModifyCommand();
				String[] parts = command.split("\t");
				modifyCommand.govPosTag = parts[0];
				modifyCommand.governor = parts[1];
				modifyCommand.depPosTag = parts[2];
				modifyCommand.dependent = parts[3];
				modifyCommand.label = parts[4];
				modifyCommand.command = ModifyCommandType.valueOf(parts[5]);
				if (modifyCommand.command==ModifyCommandType.Replace) {
					modifyCommand.newLabel = parts[6];
				}
				commands.add(modifyCommand);
			}
		}
	}

	@Override
	public void onNextParseConfiguration(ParseConfiguration parseConfiguration, Writer writer) {
		
		List<DependencyArc> arcs = new ArrayList<DependencyArc>(parseConfiguration.getDependencies());
		for (DependencyArc arc : arcs) {
			for (ModifyCommand command : commands) {
				boolean applyCommand=true;
				if (!command.govPosTag.equals(WILDCARD)&&!command.govPosTag.equals(arc.getHead().getTag().getCode())) {
					applyCommand = false;
				}
				if (!command.governor.equals(WILDCARD)&&!command.governor.equals(arc.getHead().getToken().getOriginalText().toLowerCase())) {
					applyCommand = false;
				}
				if (!command.depPosTag.equals(WILDCARD)&&!command.depPosTag.equals(arc.getDependent().getTag().getCode())) {
					applyCommand = false;
				}
				if (!command.dependent.equals(WILDCARD)&&!command.dependent.equals(arc.getDependent().getToken().getOriginalText().toLowerCase())) {
					applyCommand = false;
				}
				if (!command.label.equals(WILDCARD)&&!command.label.equals(arc.getLabel())) {
					applyCommand = false;
				}

				if (applyCommand) {
					parseConfiguration.getDependencies().remove(arc);
					if (command.command==ModifyCommandType.Replace)
						parseConfiguration.addDependency(arc.getHead(), arc.getDependent(), command.newLabel, null);
				}
			}
		}
		parseConfiguration.clearMemory();
		this.wrappedProcessor.onNextParseConfiguration(parseConfiguration, writer);
	}

	@Override
	public void onCompleteParse() {
		this.wrappedProcessor.onCompleteParse();
	}
	
	private static final class ModifyCommand {
		public String governor;
		public String dependent;
		public String govPosTag;
		public String depPosTag;
		public String label;
		public ModifyCommandType command;
		public String newLabel;
	}

}
