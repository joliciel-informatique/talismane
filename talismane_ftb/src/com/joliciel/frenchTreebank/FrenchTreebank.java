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
package com.joliciel.frenchTreebank;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.joliciel.frenchTreebank.export.FrenchTreebankXmlWriter;
import com.joliciel.frenchTreebank.export.TreebankExportService;
import com.joliciel.frenchTreebank.search.SearchResult;
import com.joliciel.frenchTreebank.search.SearchService;
import com.joliciel.frenchTreebank.search.XmlPatternSearch;
import com.joliciel.frenchTreebank.upload.TreebankRawTextAssigner;
import com.joliciel.frenchTreebank.upload.TreebankSAXParser;
import com.joliciel.frenchTreebank.upload.TreebankUploadService;
import com.joliciel.talismane.TalismaneServiceLocator;
import com.joliciel.talismane.tokeniser.TokenSequence;
import com.joliciel.talismane.tokeniser.TokeniserAnnotatedCorpusReader;

public class FrenchTreebank {
    private static final Log LOG = LogFactory.getLog(FrenchTreebank.class);

    /**
     * @param args
     */
    public static void main(String[] args) throws Exception {
        String command = args[0];
        
        String outFilePath = "";
        String outDirPath = "";
        String treebankPath = "";
        String ftbFileName = "";
        String rawTextDir = "";
        String queryPath = "";
        String sentenceNumber = null;
        boolean firstArg = true;
		for (String arg : args) {
			if (firstArg) {
				firstArg = false;
				continue;
			}
			int equalsPos = arg.indexOf('=');
			String argName = arg.substring(0, equalsPos);
			String argValue = arg.substring(equalsPos+1);
			if (argName.equals("outfile")) 
				outFilePath = argValue;
			else if (argName.equals("outdir")) 
				outDirPath = argValue;
			else if (argName.equals("ftbFileName")) 
				ftbFileName = argValue;
			else if (argName.equals("treebank")) 
				treebankPath = argValue;
			else if (argName.equals("sentence")) 
				sentenceNumber = argValue;
			else if (argName.equals("query")) 
				queryPath = argValue;
			else if (argName.equals("rawTextDir")) 
				rawTextDir = argValue;
			else
				throw new RuntimeException("Unknown argument: " + argName);
		}
		
		TalismaneServiceLocator talismaneServiceLocator = TalismaneServiceLocator.getInstance("");
		
		TreebankServiceLocator locator = TreebankServiceLocator.getInstance(talismaneServiceLocator);
		
		if (treebankPath.length()==0)
			locator.setDataSourcePropertiesFile("jdbc-live.properties");

        if (command.equals("search")) {
            final SearchService searchService = locator.getSearchService();
            final XmlPatternSearch search = searchService.newXmlPatternSearch();
            search.setXmlPatternFile(queryPath);
            List<SearchResult> searchResults = search.perform();
            
            FileWriter fileWriter = new FileWriter(outFilePath);
            for (SearchResult searchResult : searchResults) {
                String lineToWrite = "";
                Sentence sentence = searchResult.getSentence();
                Phrase phrase = searchResult.getPhrase();
                lineToWrite += sentence.getFile().getFileName() + "|";
                lineToWrite += sentence.getSentenceNumber() + "|";
                List<PhraseUnit> phraseUnits = searchResult.getPhraseUnits();
                LOG.debug("Phrase: " + phrase.getId());
                for (PhraseUnit phraseUnit : phraseUnits)
                    lineToWrite += phraseUnit.getLemma().getText() + "|";
                lineToWrite += phrase.getText();
                fileWriter.write(lineToWrite + "\n");
            }
            fileWriter.flush();
            fileWriter.close();
        } else if (command.equals("load")) {
            final TreebankService treebankService = locator.getTreebankService();
            final TreebankSAXParser parser = new TreebankSAXParser();
            parser.setTreebankService(treebankService);
            parser.parseDocument(treebankPath);
        } else if (command.equals("loadAll")) {
            final TreebankService treebankService = locator.getTreebankService();
            
            File dir = new File(treebankPath);
            
            String firstFile = null;
            if (args.length>2)
            	firstFile = args[2];
            String[] files = dir.list();
            if (files == null) {
                throw new RuntimeException("Not a directory or no children: " + treebankPath);
            } else {
            	boolean startProcessing = true;
            	if (firstFile != null)
            		startProcessing = false;
                for (int i=0; i<files.length; i++) {
                	if (!startProcessing && files[i].equals(firstFile))
                		startProcessing = true;
                	if (startProcessing) {
		                String filePath = args[1] + "/" +  files[i];
		                LOG.debug(filePath);
		                final TreebankSAXParser parser = new TreebankSAXParser();
		                parser.setTreebankService(treebankService);
		                parser.parseDocument(filePath);
                	}
                }
            } 
        } else if (command.equals("loadRawText")) {
            final TreebankService treebankService = locator.getTreebankService();
            final TreebankRawTextAssigner assigner = new TreebankRawTextAssigner();
            assigner.setTreebankService(treebankService);
            assigner.setRawTextDirectory(rawTextDir);
            assigner.loadRawText();
        } else if (command.equals("tokenize")) {
        	Writer csvFileWriter = null;
    		if (outFilePath!=null&&outFilePath.length()>0) {
    			if (outFilePath.lastIndexOf("/")>0) {
	    			String outputDirPath = outFilePath.substring(0, outFilePath.lastIndexOf("/"));
	    			File outputDir = new File(outputDirPath);
	    			outputDir.mkdirs();
    			}
    			
    			File csvFile = new File(outFilePath);
    			csvFile.delete();
    			csvFile.createNewFile();
    			csvFileWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(csvFile, false),"UTF8"));
    		}
    		try {
    		
		        final TreebankService treebankService = locator.getTreebankService();
		        TreebankExportService treebankExportService = locator.getTreebankExportServiceLocator().getTreebankExportService();
		        TreebankUploadService treebankUploadService = locator.getTreebankUploadServiceLocator().getTreebankUploadService();
				TreebankReader treebankReader = null;
				
				if (treebankPath.length()>0) {
					File treebankFile = new File(treebankPath);
					if (sentenceNumber!=null)
						treebankReader = treebankUploadService.getXmlReader(treebankFile, sentenceNumber);
					else
						treebankReader = treebankUploadService.getXmlReader(treebankFile);
					
				} else {
					treebankReader = treebankService.getDatabaseReader(TreebankSubSet.ALL, 0);
				}
		        
		        TokeniserAnnotatedCorpusReader reader = treebankExportService.getTokeniserAnnotatedCorpusReader(treebankReader, csvFileWriter);

		    	while (reader.hasNextTokenSequence()) {
		    		TokenSequence tokenSequence = reader.nextTokenSequence();
		    		List<Integer> tokenSplits = tokenSequence.getTokenSplits();
		    		String sentence = tokenSequence.getText();
		    		LOG.debug(sentence);
		    		int currentPos = 0;
		    		StringBuilder sb = new StringBuilder();
		    		for (int split : tokenSplits) {
		    			if (split==0)
		    				continue;
		    			String token = sentence.substring(currentPos, split);
		    			sb.append('|');
		    			sb.append(token);
		    			currentPos = split;
		    		}
		    		LOG.debug(sb.toString());
		    	}
    		} finally {
    			csvFileWriter.flush();
    			csvFileWriter.close();
    		}
        } else if (command.equals("export")) {
        	if (outDirPath.length()==0)
        		throw new RuntimeException("Parameter required: outdir");
        	File outDir = new File(outDirPath);
        	outDir.mkdirs();
        	
            final TreebankService treebankService = locator.getTreebankService();
            FrenchTreebankXmlWriter xmlWriter = new FrenchTreebankXmlWriter();
            xmlWriter.setTreebankService(treebankService);
            
            if (ftbFileName.length()==0) {
            	xmlWriter.write(outDir);
            } else {
  	            TreebankFile ftbFile = treebankService.loadTreebankFile(ftbFileName);
	            String fileName = ftbFileName.substring(ftbFileName.lastIndexOf('/')+1);
	            File xmlFile = new File(outDir, fileName);
	            xmlFile.delete();
	            xmlFile.createNewFile();
	            
	            Writer xmlFileWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(xmlFile, false),"UTF8"));
	            xmlWriter.write(xmlFileWriter, ftbFile);
	            xmlFileWriter.flush();
	            xmlFileWriter.close();
            }
        } else {
            throw new RuntimeException("Unknown command: " + command);
         }
        LOG.debug("========== END ============");
    }

}
