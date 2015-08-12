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
package com.joliciel.talismane.fr.ftb.upload;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.joliciel.talismane.fr.ftb.EntityNotFoundException;
import com.joliciel.talismane.fr.ftb.Sentence;
import com.joliciel.talismane.fr.ftb.TextItem;
import com.joliciel.talismane.fr.ftb.TreebankFile;
import com.joliciel.talismane.fr.ftb.TreebankService;
import com.joliciel.talismane.utils.LogUtils;

public class TreebankRawTextAssigner {
    private static final Log LOG = LogFactory.getLog(TreebankRawTextAssigner.class);
	private TreebankService treebankService;

	private String rawTextDirectory;
	private boolean assignText = false;
	
	public void loadRawText() {
		try {
			List<TreebankFile> files = this.treebankService.findTreebankFiles();
			for (TreebankFile file : files) {
				String filename = file.getFileName();
				String rawFileName = filename.substring(filename.lastIndexOf('/'));
				String rawTextFileName = rawTextDirectory  + rawFileName;
				LOG.debug(rawTextFileName);
				TreebankFile treebankFile = this.treebankService.loadTreebankFile(rawTextFileName);
				if (treebankFile==null)
					throw new RuntimeException("Cannot find file for " + rawTextFileName);
				TextItem currentTextItem = null;
				
				InputStream in = new BufferedInputStream(new FileInputStream(rawTextFileName));
			    BufferedReader reader = new BufferedReader(new InputStreamReader(in, Charset.forName("ISO-8859-1")));
			    try {
					List<Sentence> sentences = this.treebankService.findSentences(file);
					int sentenceIndex = 0;
					String lastSentenceNumber = "";
					while (sentenceIndex<sentences.size()) {
						String line = "";
						while (line.length()==0
								|| line.startsWith("//")
								|| line.startsWith("<?xml")
								|| line.startsWith("<TEI")
								|| line.startsWith("<dateline")
								|| line.startsWith("<docAuthor")
								|| line.startsWith("<argument")
								|| line.startsWith("</text>")
								|| line.startsWith("</TEI>")
								|| (line.startsWith("<head>") && !rawFileName.startsWith("/lmf3_"))
//										(rawFileName.startsWith("/lmf7aa1ep")
//											|| rawFileName.startsWith("/lmf7aa2ep")
//											|| rawFileName.startsWith("/lmf7ab1co")
//											|| rawFileName.startsWith("/lmf7ab2ep")
//										)
//									)
								) {
							line = reader.readLine();
							if (line==null) {
								throw new RuntimeException("Unexpected EOF for file " + file.getFileName() + ", sentence " + lastSentenceNumber);
							}
							line = line.trim();
						}
						if (line.startsWith("<text")) {
							String idString = "xml:id=\"";
							int idStart = line.indexOf(idString);
							int idEnd = line.indexOf("\"", idStart+idString.length());
							String externalId = line.substring(idStart + idString.length(), idEnd);
							try {
								currentTextItem = treebankService.loadTextItem(externalId);
							} catch (EntityNotFoundException enfe) {
								currentTextItem = treebankService.newTextItem();
								currentTextItem.setExternalId(externalId);
								currentTextItem.setFile(treebankFile);
								currentTextItem.save();
							}
						} else {
							Sentence sentence = sentences.get(sentenceIndex++);
							LOG.debug("Sentence " + sentence.getSentenceNumber());
							lastSentenceNumber = sentence.getSentenceNumber();

							line = line.replace('_', '-');
							line = line.replace("&gt;", ">");
							line = line.replace("&lt;", "<");
							line = line.replace("&amp;", "&");
							
							LOG.debug(line);
							if (line.startsWith("<head>")) {
								line = line.substring(6, line.length()-7);
								LOG.debug("Without head: " + line);
							}
							if (line.startsWith("<"))
								throw new RuntimeException("Unexpected tag: " + line);
							
							if (assignText)
								sentence.setText(line);
							sentence.setTextItem(currentTextItem);
							sentence.save();
						}
					}
			    } finally {
			    	reader.close();
			    }
			}
		} catch (IOException ioe) {
			LogUtils.logError(LOG, ioe);
			throw new RuntimeException(ioe);
		}
	}
	
	public TreebankService getTreebankService() {
		return treebankService;
	}

	public void setTreebankService(TreebankService treebankService) {
		this.treebankService = treebankService;
	}

	public String getRawTextDirectory() {
		return rawTextDirectory;
	}

	public void setRawTextDirectory(String rawTextDirectory) {
		this.rawTextDirectory = rawTextDirectory;
	}

	public boolean isAssignText() {
		return assignText;
	}

	public void setAssignText(boolean assignText) {
		this.assignText = assignText;
	}
	
	
}
