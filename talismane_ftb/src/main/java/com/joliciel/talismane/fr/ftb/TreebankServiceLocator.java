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
package com.joliciel.talismane.fr.ftb;

import java.beans.PropertyVetoException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.sql.DataSource;

import com.joliciel.talismane.TalismaneSession;
import com.joliciel.talismane.fr.ftb.export.TreebankExportServiceLocator;
import com.joliciel.talismane.fr.ftb.search.SearchService;
import com.joliciel.talismane.fr.ftb.search.SearchServiceImpl;
import com.joliciel.talismane.fr.ftb.upload.TreebankUploadServiceLocator;
import com.joliciel.talismane.utils.ObjectCache;
import com.joliciel.talismane.utils.SimpleObjectCache;
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

	private static Map<String, TreebankServiceLocator> instances = new HashMap<>();

	private final TalismaneSession talismaneSession;

	private TreebankServiceLocator(TalismaneSession talismaneSession) {
		this.talismaneSession = talismaneSession;
	}

	public static TreebankServiceLocator getInstance(TalismaneSession talismaneSession) {
		TreebankServiceLocator instance = instances.get(talismaneSession.getSessionId());
		if (instance == null) {
			instance = new TreebankServiceLocator(talismaneSession);
			instances.put(talismaneSession.getSessionId(), instance);
		}
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
		if (this.treebankDao == null) {
			if (this.dataSourcePropertiesFile != null) {
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
		if (dataSourceProperties == null) {
			dataSourceProperties = new Properties();
			try {
				String path = this.getDataSourcePropertiesFile();
				InputStream inputStream = TreebankServiceLocator.class.getResourceAsStream(path);

				dataSourceProperties.load(inputStream);
			} catch (Exception e) {
				e.printStackTrace();
				throw new RuntimeException(e);
			}
		}
		return dataSourceProperties;
	}

	public DataSource getDataSource() {
		if (dataSource == null) {
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
		if (treebankUploadServiceLocator == null)
			treebankUploadServiceLocator = new TreebankUploadServiceLocator(this);
		return treebankUploadServiceLocator;
	}

	public TreebankExportServiceLocator getTreebankExportServiceLocator() {
		if (treebankExportServiceLocator == null)
			treebankExportServiceLocator = new TreebankExportServiceLocator(this);
		return treebankExportServiceLocator;
	}

	public TalismaneSession getTalismaneSession() {
		return talismaneSession;
	}

}
