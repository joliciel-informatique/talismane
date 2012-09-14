///////////////////////////////////////////////////////////////////////////////
//Copyright (C) 2012 Assaf Urieli
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

import java.beans.PropertyVetoException;
import java.io.FileInputStream;
import java.net.URL;
import java.util.Properties;

import javax.sql.DataSource;

import com.joliciel.frenchTreebank.export.TreebankExportServiceLocator;
import com.joliciel.frenchTreebank.search.SearchService;
import com.joliciel.frenchTreebank.search.SearchServiceImpl;
import com.joliciel.frenchTreebank.upload.TreebankUploadServiceLocator;
import com.joliciel.talismane.posTagger.PosTaggerService;
import com.joliciel.talismane.tokeniser.TokeniserService;
import com.joliciel.talismane.utils.util.ObjectCache;
import com.joliciel.talismane.utils.util.SimpleObjectCache;
import com.mchange.v2.c3p0.ComboPooledDataSource;

public class TreebankServiceLocator {
    private DataSource dataSource;
    private String dataSourcePropertiesFile;
    private Properties dataSourceProperties;
    private TreebankServiceImpl treebankService;
    private TreebankDaoImpl treebankDao;
    private SearchServiceImpl searchService;
    private TreebankUploadServiceLocator treebankUploadServiceLocator;
    private TreebankExportServiceLocator treebankExportServiceLocator;  
    
    private TokeniserService tokeniserService;
    private PosTaggerService posTaggerService;
    
    private static TreebankServiceLocator instance = null;
    
    private TreebankServiceLocator() {}
    
    public static TreebankServiceLocator getInstance() {
    	if (instance==null)
    		instance = new TreebankServiceLocator();
    	return instance;
    }
    public String getDataSourcePropertiesFile() {
        return dataSourcePropertiesFile;
    }

    public void setDataSourcePropertiesFile(String dataSourcePropertiesFile) {
        this.dataSourcePropertiesFile = dataSourcePropertiesFile;
    }

    public TreebankService getTreebankService() {
        if (this.treebankService == null) {
            treebankService = new TreebankServiceImpl();
            ObjectCache objectCache = new SimpleObjectCache();
            treebankService.setObjectCache(objectCache);
            treebankService.setTreebankDao(this.getTreebankDao());
        }
        
        return treebankService;
    }
    
    TreebankDao getTreebankDao() {
    	if (this.treebankDao==null) {
	    	if (this.dataSourcePropertiesFile!=null) {
		        treebankDao = new TreebankDaoImpl();
		        treebankDao.setDataSource(this.getDataSource());
	    	}
    	}
        return treebankDao;
    }
    
    public SearchService getSearchService() {
        if (this.searchService == null) {
            searchService = new SearchServiceImpl();
            searchService.setTreebankService(this.getTreebankService());
        }
        return searchService;
    }

    private Properties getDataSourceProperties() {
        if (dataSourceProperties==null) {
            dataSourceProperties = new Properties();
            try {
                URL url =  ClassLoader.getSystemResource(this.getDataSourcePropertiesFile());
                String file = url.getFile();
                FileInputStream fis = new FileInputStream(file);
                dataSourceProperties.load(fis);
            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }        
        }
        return dataSourceProperties;
    }
    
    public DataSource getDataSource() {
        if (dataSource==null) {
            ComboPooledDataSource ds = new ComboPooledDataSource();
            Properties props = this.getDataSourceProperties();
            try {
                ds.setDriverClass(props.getProperty("jdbc.driverClassName"));
            } catch (PropertyVetoException e) {
                 e.printStackTrace();
                 throw new RuntimeException(e);
            }
            ds.setJdbcUrl(props.getProperty("jdbc.url"));
            ds.setUser(props.getProperty("jdbc.username"));
            ds.setPassword(props.getProperty("jdbc.password"));
            dataSource = ds;
        }
        return dataSource;
    }
    
	public TreebankUploadServiceLocator getTreebankUploadServiceLocator() {
		if (treebankUploadServiceLocator==null)
			treebankUploadServiceLocator = new TreebankUploadServiceLocator(this);
		return treebankUploadServiceLocator;
	}
 
	public TreebankExportServiceLocator getTreebankExportServiceLocator() {
		if (treebankExportServiceLocator==null)
			treebankExportServiceLocator = new TreebankExportServiceLocator(this);
		return treebankExportServiceLocator;
	}

	public TokeniserService getTokeniserService() {
		return tokeniserService;
	}

	public void setTokeniserService(TokeniserService tokeniserService) {
		this.tokeniserService = tokeniserService;
	}

	public PosTaggerService getPosTaggerService() {
		return posTaggerService;
	}

	public void setPosTaggerService(PosTaggerService posTaggerService) {
		this.posTaggerService = posTaggerService;
	}

}
