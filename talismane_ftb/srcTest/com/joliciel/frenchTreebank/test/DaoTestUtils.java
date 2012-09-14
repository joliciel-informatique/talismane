/*
 * Created on 12 Jan 2010
 */
package com.joliciel.frenchTreebank.test;

import java.beans.PropertyVetoException;
import java.io.FileInputStream;
import java.net.URL;
import java.util.Collection;
import java.util.Properties;

import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jmock.api.Action;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import com.mchange.v2.c3p0.ComboPooledDataSource;

public class DaoTestUtils {
    private static final Log LOG = LogFactory.getLog(DaoTestUtils.class);
    private static DataSource dataSource;
    
    public static DataSource getDataSource() throws Exception {
        if (dataSource==null) {
            ComboPooledDataSource ds = new ComboPooledDataSource();
            Properties dataSourceProperties = new Properties();
            try {
                URL url =  ClassLoader.getSystemResource("jdbc-test.properties");
                String file = url.getFile();
                FileInputStream fis = new FileInputStream(file);
                dataSourceProperties.load(fis);
            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }        
            try {
                ds.setDriverClass(dataSourceProperties.getProperty("jdbc.driverClassName"));
            } catch (PropertyVetoException e) {
                 e.printStackTrace();
                 throw new RuntimeException(e);
            }
            ds.setJdbcUrl(dataSourceProperties.getProperty("jdbc.url"));
            ds.setUser(dataSourceProperties.getProperty("jdbc.username"));
            ds.setPassword(dataSourceProperties.getProperty("jdbc.password"));
            dataSource = ds;
        }
        return dataSource;
    }
    
    /**
     * Used to save parameters from a mock call into a collection, for example when inserting a record.
     *    final Collection<Object> parameters = new ArrayList<Object>();
     *    ...
     *   oneOf (projectUserSave).setId(with(any(Long.class))); will (saveParameters(parameters));
     *   ...
     *   projectDao.loadProjectUser((Long)parameters.iterator().next());
     * @param collection
     * @return
     */
    public static Action saveParameters(Collection<Object> collection) {
        return new SaveParametersAction(collection);
    }
    
    public static void wipeDB() throws Exception {
        NamedParameterJdbcTemplate jt = new NamedParameterJdbcTemplate(DaoTestUtils.getDataSource());
        MapSqlParameterSource paramSource = new MapSqlParameterSource();
        String sql = "SELECT wipeDB();";
        LOG.info(sql);
        jt.queryForObject(sql, paramSource, Object.class);
    }
}
