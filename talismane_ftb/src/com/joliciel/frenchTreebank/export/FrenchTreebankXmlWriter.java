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
package com.joliciel.frenchTreebank.export;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.joliciel.frenchTreebank.TreebankFile;
import com.joliciel.frenchTreebank.TreebankService;

import freemarker.cache.NullCacheStorage;
import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapper;
import freemarker.template.Template;
import freemarker.template.TemplateException;

/**
 * Writes the French Treebank to an xml file, in the format defined by Anne Abeillé
 * (plus certain additions)
 * @author Assaf Urieli
 *
 */
public class FrenchTreebankXmlWriter {
    private static final Log LOG = LogFactory.getLog(FrenchTreebankXmlWriter.class);

    private TreebankService treebankService;
    
	public FrenchTreebankXmlWriter() {
		// export all files
	}
	
	public void write(File outDir) throws TemplateException, IOException {
		List<TreebankFile> treebankFiles = treebankService.findTreebankFiles();
		for (TreebankFile treebankFile : treebankFiles) {
			String ftbFileName = treebankFile.getFileName();
			String fileName = ftbFileName.substring(ftbFileName.lastIndexOf('/')+1);
            File xmlFile = new File(outDir, fileName);
            xmlFile.delete();
            xmlFile.createNewFile();
           
            Writer xmlFileWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(xmlFile, false),"UTF8"));
            this.write(xmlFileWriter, treebankFile);
            xmlFileWriter.flush();
            xmlFileWriter.close();
		}
	}
	
	public void write(Writer writer, TreebankFile treebankFile) throws TemplateException {
		try {
			Configuration cfg = new Configuration();
			// Specify the data source where the template files come from.
			cfg.setClassForTemplateLoading(FrenchTreebankXmlWriter.class, "/com/joliciel/frenchTreebank/export");
//			cfg.setDirectoryForTemplateLoading(new File("templates"));
			cfg.setCacheStorage(new NullCacheStorage());
			
			cfg.setObjectWrapper(new DefaultObjectWrapper());
			
			Map<String,Object> model = new HashMap<String, Object>();
			model.put("file", treebankFile);
			model.put("LOG", LOG);
			Template temp = cfg.getTemplate("xml.ftl");
			
			temp.process(model, writer);
			writer.flush();
		} catch (IOException ioe) {
			throw new RuntimeException(ioe);
		}
	}

	public TreebankService getTreebankService() {
		return treebankService;
	}

	public void setTreebankService(TreebankService treebankService) {
		this.treebankService = treebankService;
	}
	
	
}
