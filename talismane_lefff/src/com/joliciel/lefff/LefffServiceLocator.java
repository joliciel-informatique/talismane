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
package com.joliciel.lefff;

import java.beans.PropertyVetoException;
import java.io.FileInputStream;
import java.net.URL;
import java.util.Properties;

import javax.sql.DataSource;

import com.joliciel.talismane.posTagger.PosTagSet;
import com.joliciel.talismane.utils.ObjectCache;
import com.joliciel.talismane.utils.SimpleObjectCache;
import com.mchange.v2.c3p0.ComboPooledDataSource;

public class LefffServiceLocator {
    private DataSource dataSource;
    private String dataSourcePropertiesFile;
    private Properties dataSourceProperties;
    private LefffServiceImpl lefffService;
    private PosTagSet posTagSet;
    
    public String getDataSourcePropertiesFile() {
        return dataSourcePropertiesFile;
    }

    public void setDataSourcePropertiesFile(String dataSourcePropertiesFile) {
        this.dataSourcePropertiesFile = dataSourcePropertiesFile;
    }

    public LefffService getLefffService() {
        if (this.lefffService == null) {
            lefffService = new LefffServiceImpl();
            ObjectCache objectCache = new SimpleObjectCache();
            LefffDaoImpl lefffDao = new LefffDaoImpl();
            lefffDao.setDataSource(this.getDataSource());
            lefffService.setObjectCache(objectCache);
            lefffService.setLefffDao(lefffDao);
        }
        
        return lefffService;
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
    
    private DataSource getDataSource() {
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

	public PosTagSet getPosTagSet() {
		return posTagSet;
	}

	public void setPosTagSet(PosTagSet posTagSet) {
		this.posTagSet = posTagSet;
	}

}
