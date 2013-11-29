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
package com.joliciel.talismane.terminology.postgres;

import java.beans.PropertyVetoException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;

import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.rowset.ResultSetWrappingSqlRowSet;
import org.springframework.jdbc.support.rowset.SqlRowSet;

import com.joliciel.talismane.TalismaneException;
import com.joliciel.talismane.terminology.Context;
import com.joliciel.talismane.terminology.Term;
import com.joliciel.talismane.terminology.TermFrequencyComparator;
import com.joliciel.talismane.terminology.TerminologyBase;
import com.joliciel.talismane.utils.DaoUtils;
import com.joliciel.talismane.utils.PerformanceMonitor;
import com.mchange.v2.c3p0.ComboPooledDataSource;

public class PostGresTerminologyBase implements TerminologyBase {
	private static final Log LOG = LogFactory.getLog(PostGresTerminologyBase.class);
	private static final PerformanceMonitor MONITOR = PerformanceMonitor.getMonitor(PostGresTerminologyBase.class);
	 
	private DataSource dataSource;
    
    private static final String SELECT_TERM = "term_id, term_text_id, term_project_id, term_frequency, term_marked, term_expansion_count, term_head_count, text_text";
    private static final String SELECT_CONTEXT = "context_id, context_start_row, context_start_column, context_text, context_file_id, context_term_id";
    
    private Map<String,Integer> filenameMap = new HashMap<String, Integer>();
    private Map<Integer,String> fileIdMap = new HashMap<Integer, String>();
    
    private String projectCode;
    private int projectId;
    
	public PostGresTerminologyBase(String projectCode, Properties connectionProperties) {
		this.projectCode = projectCode;
		
		ComboPooledDataSource ds = new ComboPooledDataSource();
        try {
            ds.setDriverClass(connectionProperties.getProperty("jdbc.driverClassName"));
        } catch (PropertyVetoException e) {
             e.printStackTrace();
             throw new RuntimeException(e);
        }
        ds.setJdbcUrl(connectionProperties.getProperty("jdbc.url"));
        ds.setUser(connectionProperties.getProperty("jdbc.username"));
        ds.setPassword(connectionProperties.getProperty("jdbc.password"));
        dataSource = ds;
	}


	@Override
	public List<Term> getTerms(int frequencyThreshold, String searchText,
			boolean marked, boolean markedExpansions) {
		MONITOR.startTask("getTerms");
		try {
			NamedParameterJdbcTemplate jt = new NamedParameterJdbcTemplate(this.getDataSource());
	        String sql = "SELECT " + SELECT_TERM + " FROM term" +
	        		" INNER JOIN text ON term_text_id=text_id" +
	        		" WHERE term_project_id = :term_project_id";
	        if (marked && markedExpansions) {
	        	sql += " AND term_marked = :term_marked";
		        if (searchText.length()>0)
		        	sql += " AND text_text LIKE :term_text";
	        } else {
		        if (frequencyThreshold>0)
		        	sql += " AND term_frequency >= :term_frequency";
		        if (searchText.length()>0)
		        	sql += " AND text_text LIKE :term_text";
		        if (marked)
		        	sql += " AND term_marked = :term_marked";
	        }
	        sql += " ORDER BY term_frequency DESC, text_text";
	        MapSqlParameterSource paramSource = new MapSqlParameterSource();
	        if (frequencyThreshold>0)
	        	paramSource.addValue("term_frequency", frequencyThreshold);
	        if (searchText.length()>0)
	        	paramSource.addValue("term_text",  searchText + "%");
	        if (marked)
	        	paramSource.addValue("term_marked", true);
	        paramSource.addValue("term_project_id", this.getCurrentProjectId());
	       
	        LOG.trace(sql);
	        LogParameters(paramSource);
	        @SuppressWarnings("unchecked")
			List<Term> terms = jt.query(sql, paramSource, new TermMapper());
	        
	        if (marked && markedExpansions) {
	        	this.addParents(terms);
	        	List<Term> termsWithFrequency = new ArrayList<Term>();
	        	for (Term term : terms) {
	        		int maxAncestorFrequency = this.getMaxAncestorFrequency(term);
	        		if (maxAncestorFrequency>=frequencyThreshold)
	        			termsWithFrequency.add(term);
	        	}
	        	terms = termsWithFrequency;
	        }

			return terms;
		} finally {
			MONITOR.endTask("getTerms");
		}
	}
	
	int getMaxAncestorFrequency(Term term) {
		int maxFrequency = term.getFrequency();
		for (Term parent : term.getParents()) {
			int parentFrequency = this.getMaxAncestorFrequency(parent);
			if (parentFrequency>maxFrequency)
				maxFrequency = parentFrequency;
		}
		return maxFrequency;
	}
	
	void addParents(List<Term> childTerms) {
		NamedParameterJdbcTemplate jt = new NamedParameterJdbcTemplate(this.getDataSource());
        String sql = "SELECT " + SELECT_TERM + ", term_expansion_id FROM term" +
				" INNER JOIN text ON term_text_id=text_id" +
        		" INNER JOIN term_expansions ON term_id = termexp_term_id" +
        		" WHERE term_project_id = :term_project_id" +
        		" AND termexp_expansion_id IN (:child_terms)";
        
        MapSqlParameterSource paramSource = new MapSqlParameterSource();
        paramSource.addValue("term_project_id", this.getCurrentProjectId());
        List<Integer> termIds = new ArrayList<Integer>();
        Map<Integer,PostGresTerm> childTermMap = new HashMap<Integer, PostGresTerm>();
        for (Term childTerm : childTerms) {
        	PostGresTerm termInternal = (PostGresTerm) childTerm;
        	if (termInternal.getParentsInternal()==null) {
        		termIds.add(termInternal.getId());
        		termInternal.setParentsInternal(new TreeSet<Term>());
        		childTermMap.put(termInternal.getId(), termInternal);
        	}
        }
        paramSource.addValue("child_terms", termIds);
       
        LOG.trace(sql);
        LogParameters(paramSource);
        
        SqlRowSet rs = jt.queryForRowSet(sql, paramSource);
        TermMapper termMapper = new TermMapper();
        List<Term> parentTerms = new ArrayList<Term>();
        while (rs.next()) {
        	Term term = termMapper.mapRow(rs);
        	parentTerms.add(term);
        	int childId = rs.getInt("termexp_expansion_id");
        	PostGresTerm childTerm = childTermMap.get(childId);
        	childTerm.getParentsInternal().add(term);
        }
        if (parentTerms.size()>0) {
        	this.addParents(parentTerms);
        }
	}
	
	public List<Term> getTerms(final int frequencyThreshold,
			final String searchText, final boolean marked) {
		return this.getTerms(frequencyThreshold, searchText, marked, false);
	}
	
	@Override
	public List<Term> getTermsByFrequency(final int frequencyThreshold) {
		MONITOR.startTask("getTermsByFrequency");
		try {
			NamedParameterJdbcTemplate jt = new NamedParameterJdbcTemplate(this.getDataSource());
	        String sql = "SELECT " + SELECT_TERM + " FROM term" +
    				" INNER JOIN text ON term_text_id=text_id" +
	        		" WHERE term_frequency >= :term_frequency" +
	        		" AND term_project_id = :term_project_id" +
	        		" ORDER BY term_frequency DESC, text_text";
	        MapSqlParameterSource paramSource = new MapSqlParameterSource();
	        paramSource.addValue("term_frequency", frequencyThreshold);
	        paramSource.addValue("term_project_id", this.getCurrentProjectId());
	       
	        LOG.trace(sql);
	        LogParameters(paramSource);
	        @SuppressWarnings("unchecked")
			List<Term> terms = jt.query(sql, paramSource, new TermMapper());

			return terms;
		} finally {
			MONITOR.endTask("getTermsByFrequency");
		}
	}
	
	@Override
	public List<Term> getTermsByText(final String searchText) {
		MONITOR.startTask("getTermsByText");
		try {
			NamedParameterJdbcTemplate jt = new NamedParameterJdbcTemplate(this.getDataSource());
	        String sql = "SELECT " + SELECT_TERM + " FROM term" +
    				" INNER JOIN text ON term_text_id=text_id" +
	        		" WHERE text_text LIKE :term_text" +
	        		" AND term_project_id = :term_project_id" +
	        		" ORDER BY text_text";
	        MapSqlParameterSource paramSource = new MapSqlParameterSource();
	        paramSource.addValue("term_text", searchText + "%");
	        paramSource.addValue("term_project_id", this.getCurrentProjectId());
	       
	        LOG.trace(sql);
	        LogParameters(paramSource);
	        @SuppressWarnings("unchecked")
			List<Term> terms = jt.query(sql, paramSource, new TermMapper());

			return terms;

		} finally {
			MONITOR.endTask("getTermsByText");
		}
	}
	

	@Override
	public List<Term> getMarkedTerms() {
		MONITOR.startTask("getMarkedTerms");
		try {
			NamedParameterJdbcTemplate jt = new NamedParameterJdbcTemplate(this.getDataSource());
	        String sql = "SELECT " + SELECT_TERM + " FROM term" +
					" INNER JOIN text ON term_text_id=text_id" +
	        		" WHERE term_marked = :term_marked" +
	        		" AND term_project_id = :term_project_id" +
	        		" ORDER BY text_text";
	        MapSqlParameterSource paramSource = new MapSqlParameterSource();
	        paramSource.addValue("term_marked", true);
	        paramSource.addValue("term_project_id", this.getCurrentProjectId());
	       
	        LOG.trace(sql);
	        LogParameters(paramSource);
	        @SuppressWarnings("unchecked")
			List<Term> terms = jt.query(sql, paramSource, new TermMapper());

			return terms;

		} finally {
			MONITOR.endTask("getMarkedTerms");
		}
	}

	@Override
	public Term getTerm(final String text) {
		MONITOR.startTask("getTerm");
		try {
	    	if (text==null || text.trim().length()==0)
	    		throw new TalismaneException("Cannot get an empty term");
	    	
			Term term = this.loadTerm(text);
			if (term==null) {
				PostGresTerm postGresTerm = this.newTerm();
				postGresTerm.setText(text);
				postGresTerm.getHeads();
				postGresTerm.getExpansions();
				term = postGresTerm;
			}
			return term;
		} finally {
			MONITOR.endTask("getTerm");
		}
	}

	@Override
	public void storeTerm(Term term) {
		MONITOR.startTask("storeTerm");
		try {
			PostGresTerm termInternal = (PostGresTerm) term;
			this.saveTerm(termInternal);
			for (Context context : term.getContexts()) {
				PostGresContext contextInternal = (PostGresContext) context;
				this.saveContext(contextInternal);
			}
			this.saveExpansions(termInternal);
			this.saveHeads(termInternal);

		} finally {
			MONITOR.endTask("storeTerm");
		}
	}

	@Override
	public void storeContext(Context context) {
		MONITOR.startTask("storeContext");
		try {
			this.saveContext((PostGresContext)context);
		} finally {
			MONITOR.endTask("storeContext");
		}
	}

	@Override
	public void commit() {
		MONITOR.startTask("commit");
		try {
			// nothing to do here, not being transactional about it
		} finally {
			MONITOR.endTask("commit");
		}
	}

	@Override
	public Set<Term> getParents(final Term term) {
		MONITOR.startTask("getParents");
		try {
			NamedParameterJdbcTemplate jt = new NamedParameterJdbcTemplate(this.getDataSource());
	        String sql = "SELECT " + SELECT_TERM
	        	+ " FROM term"
				+ " INNER JOIN text ON term_text_id=text_id"
	        	+ " INNER JOIN term_expansions ON term_id = termexp_term_id"
	        	+ " WHERE termexp_expansion_id = :term_id"
	        	+ " ORDER BY text_text";
	        MapSqlParameterSource paramSource = new MapSqlParameterSource();
	        paramSource.addValue("term_id", ((PostGresTerm) term).getId());
	       
	        LOG.trace(sql);
	        LogParameters(paramSource);
	        @SuppressWarnings("unchecked")
			List<Term> terms = jt.query(sql, paramSource, new TermMapper());
	        
	        Set<Term> termSet = new TreeSet<Term>(new TermFrequencyComparator());
	        termSet.addAll(terms);
			return termSet;
		} finally {
			MONITOR.endTask("getHeads");
		}
	}

	@Override
	public Set<Term> getExpansions(Term term) {
		MONITOR.startTask("getExpansions");
		try {
			NamedParameterJdbcTemplate jt = new NamedParameterJdbcTemplate(this.getDataSource());
	        String sql = "SELECT " + SELECT_TERM
	        	+ " FROM term"
				+ " INNER JOIN text ON term_text_id=text_id"
	        	+ " INNER JOIN term_expansions ON term_id = termexp_expansion_id"
	        	+ " WHERE termexp_term_id = :term_id"
	        	+ " ORDER BY text_text";
	        MapSqlParameterSource paramSource = new MapSqlParameterSource();
	        paramSource.addValue("term_id", ((PostGresTerm) term).getId());
	       
	        LOG.trace(sql);
	        LogParameters(paramSource);
	        @SuppressWarnings("unchecked")
			List<Term> terms = jt.query(sql, paramSource, new TermMapper());
	        
	        Set<Term> termSet = new TreeSet<Term>(new TermFrequencyComparator());
	        termSet.addAll(terms);
			return termSet;
		} finally {
			MONITOR.endTask("getExpansions");
		}
	}



	@Override
	public Set<Term> getHeads(Term term) {
		MONITOR.startTask("getHeads");
		try {
			NamedParameterJdbcTemplate jt = new NamedParameterJdbcTemplate(this.getDataSource());
	        String sql = "SELECT " + SELECT_TERM
	        	+ " FROM term"
				+ " INNER JOIN text ON term_text_id=text_id"
	        	+ " INNER JOIN term_heads ON term_id = termhead_head_id"
	        	+ " WHERE termhead_term_id = :term_id"
	        	+ " ORDER BY text_text";
	        MapSqlParameterSource paramSource = new MapSqlParameterSource();
	        paramSource.addValue("term_id", ((PostGresTerm) term).getId());
	       
	        LOG.trace(sql);
	        LogParameters(paramSource);
	        @SuppressWarnings("unchecked")
			List<Term> terms = jt.query(sql, paramSource, new TermMapper());
	        
	        Set<Term> termSet = new TreeSet<Term>(new TermFrequencyComparator());
	        termSet.addAll(terms);
			return termSet;
		} finally {
			MONITOR.endTask("getHeads");
		}
	}



	@Override
	public List<Context> getContexts(Term term) {
		MONITOR.startTask("getContexts");
		try {
			NamedParameterJdbcTemplate jt = new NamedParameterJdbcTemplate(this.getDataSource());
	        String sql = "SELECT " + SELECT_CONTEXT + " FROM context" +
	        		" WHERE context_term_id = :context_term_id" +
	        		" ORDER BY context_id";
	        MapSqlParameterSource paramSource = new MapSqlParameterSource();
	        paramSource.addValue("context_term_id", ((PostGresTerm) term).getId());
	       
	        LOG.trace(sql);
	        LogParameters(paramSource);
	        @SuppressWarnings("unchecked")
			List<Context> contexts = jt.query(sql, paramSource, new ContextMapper());

			return contexts;
		} finally {
			MONITOR.endTask("getContexts");
		}
	}

    public Term loadTerm(int termId) {
        NamedParameterJdbcTemplate jt = new NamedParameterJdbcTemplate(this.getDataSource());
        String sql = "SELECT " + SELECT_TERM + " FROM term"
			+ " INNER JOIN text ON term_text_id=text_id"
			+ " WHERE term_id=:term_id";
        MapSqlParameterSource paramSource = new MapSqlParameterSource();
        paramSource.addValue("term_id", termId);
       
        LOG.trace(sql);
        LogParameters(paramSource);
        Term term = null;
        try {
            term = (Term)  jt.queryForObject(sql, paramSource, new TermMapper());
        } catch (EmptyResultDataAccessException ex) {
            ex.hashCode();
        }
        return term;
    }
    
    public Term loadTerm(String termText) {
        NamedParameterJdbcTemplate jt = new NamedParameterJdbcTemplate(this.getDataSource());
        
        String sql = "SELECT " + SELECT_TERM + " FROM term" +
			" INNER JOIN text ON term_text_id=text_id" +
       		" WHERE text_text=:term_text" +
        	" AND term_project_id=:term_project_id";
        MapSqlParameterSource paramSource = new MapSqlParameterSource();
        paramSource.addValue("term_text", termText);
        paramSource.addValue("term_project_id", this.getCurrentProjectId());
        
        LOG.trace(sql);
        LogParameters(paramSource);
        Term term = null;
        try {
            term = (Term)  jt.queryForObject(sql, paramSource, new TermMapper());
        } catch (EmptyResultDataAccessException ex) {
            ex.hashCode();
        }
        return term;
    }

    public void saveTerm(PostGresTerm term) {
    	if (term.isDirty()) {
	        int textId = this.getTextId(term.getText());
	        term.setTextId(textId);
	        
	        NamedParameterJdbcTemplate jt = new NamedParameterJdbcTemplate(this.getDataSource());
	        MapSqlParameterSource paramSource = new MapSqlParameterSource();
	        paramSource.addValue("term_text_id", term.getTextId());
	        paramSource.addValue("term_frequency", term.getFrequency());
	        paramSource.addValue("term_project_id", this.getCurrentProjectId());
	        paramSource.addValue("term_marked", term.isMarked());
	        paramSource.addValue("term_expansion_count", term.getExpansionCount());
	        paramSource.addValue("term_head_count", term.getHeadCount());
	        
	        if (term.isNew()) {
	            String sql = "SELECT nextval('seq_term_id')";
	            LOG.trace(sql);
	            int termId = jt.queryForInt(sql, paramSource);
	            paramSource.addValue("term_id", termId);
	            
	            sql = "INSERT INTO term (term_id, term_text_id, term_project_id, term_frequency, term_marked, term_expansion_count, term_head_count)" +
	            		" VALUES (:term_id, :term_text_id, :term_project_id, :term_frequency, :term_marked, :term_expansion_count, :term_head_count)";
	            
	            LOG.trace(sql);
	            LogParameters(paramSource);
	            jt.update(sql, paramSource);
	            term.setId(termId);
	        } else {
	            String sql = "UPDATE term" +
	            		" SET term_text_id = :term_text_id" +
	            		", term_frequency = :term_frequency" +
	            		", term_project_id = :term_project_id" +
	            		", term_marked = :term_marked" +
	            		", term_expansion_count = :term_expansion_count" +
	            		", term_head_count = :term_head_count" +
	            		" WHERE term_id = :term_id";
	            
	            paramSource.addValue("term_id", term.getId());
	            LOG.trace(sql);
	            LogParameters(paramSource);
	            jt.update(sql, paramSource);           
	        }
	        term.setDirty(false);
    	}
    }
    
    protected final class TermMapper implements RowMapper {
        public TermMapper() {};

        public Object mapRow(ResultSet rs, int rowNum) throws SQLException {
            return this.mapRow(new ResultSetWrappingSqlRowSet(rs));
        }

        public Term mapRow(SqlRowSet rs) {
            PostGresTerm term = newTerm();
            term.setId(rs.getInt("term_id"));
            term.setTextId(rs.getInt("term_text_id"));
            term.setText(rs.getString("text_text"));
            term.setFrequency(rs.getInt("term_frequency"));
            term.setMarked(rs.getBoolean("term_marked"));
            term.setExpansionCount(rs.getInt("term_expansion_count"));
            term.setHeadCount(rs.getInt("term_head_count"));
            term.setDirty(false);
            return term;
        }
    }
    
    public Context loadContext(int contextId) {
        NamedParameterJdbcTemplate jt = new NamedParameterJdbcTemplate(this.getDataSource());
        String sql = "SELECT " + SELECT_CONTEXT + " FROM context WHERE context_id=:context_id";
        MapSqlParameterSource paramSource = new MapSqlParameterSource();
        paramSource.addValue("context_id", contextId);
       
        LOG.trace(sql);
        LogParameters(paramSource);
        Context context = null;
        try {
            context = (Context)  jt.queryForObject(sql, paramSource, new ContextMapper());
        } catch (EmptyResultDataAccessException ex) {
            ex.hashCode();
        }
        return context;
    }
    
    public void saveContext(PostGresContext context) {
    	if (context.isDirty()) {
	        NamedParameterJdbcTemplate jt = new NamedParameterJdbcTemplate(this.getDataSource());
	        MapSqlParameterSource paramSource = new MapSqlParameterSource();
	        paramSource.addValue("context_start_row", context.getLineNumber());
	        paramSource.addValue("context_start_column", context.getColumnNumber());
	        paramSource.addValue("context_text", context.getTextSegment());
	        paramSource.addValue("context_term_id", ((PostGresTerm) context.getTerm()).getId());
	        paramSource.addValue("context_file_id", this.getFileId(context.getFileName()));
	
	        // context_id, context_start_row, context_start_column, context_text, context_file_id, context_term_id
	        if (context.isNew()) {
	            String sql = "SELECT nextval('seq_context_id')";
	            LOG.trace(sql);
	            int contextId = jt.queryForInt(sql, paramSource);
	            paramSource.addValue("context_id", contextId);
	            
	            sql = "INSERT INTO context (context_id, context_start_row, context_start_column, context_text, context_file_id, context_term_id)" +
	            		" VALUES (:context_id, :context_start_row, :context_start_column, :context_text, :context_file_id, :context_term_id)";
	            
	            LOG.trace(sql);
	            LogParameters(paramSource);
	            jt.update(sql, paramSource);
	            context.setId(contextId);
	        } else {
	            String sql = "UPDATE context" +
	        		" SET context_start_row = :context_start_row" +
	        		", context_start_column = :context_start_column" +
	        		", context_text = :context_text" +
	        		", context_file_id = :context_file_id" +
	        		", context_term_id = :context_term_id" +
	           		" WHERE context_id = :context_id";
	            
	            paramSource.addValue("context_id", context.getId());
	            LOG.trace(sql);
	            LogParameters(paramSource);
	            jt.update(sql, paramSource);           
	        }
            context.setDirty(false);
    	}
    }
    
    protected final class ContextMapper implements RowMapper {
        public Object mapRow(ResultSet rs, int rowNum) throws SQLException {
            return this.mapRow(new ResultSetWrappingSqlRowSet(rs));
        }

        public Context mapRow(SqlRowSet rs) {
        	// context_id, context_start_row, context_start_column, context_text, context_file_id, context_term_id
            PostGresContext context = newContext();
            context.setId(rs.getInt("context_id"));
            context.setTextSegment(rs.getString("context_text"));
            context.setColumnNumber(rs.getInt("context_start_column"));
            context.setLineNumber(rs.getInt("context_start_row"));
            context.setFileName(getFileName(rs.getInt("context_file_id")));
            context.setDirty(false);
            return context;
        }
    }

	public DataSource getDataSource() {
		return dataSource;
	}

	public void setDataSource(DataSource dataSource) {
		this.dataSource = dataSource;
	}
 

	@SuppressWarnings("unchecked")
    public static void LogParameters(MapSqlParameterSource paramSource) {
       DaoUtils.LogParameters(paramSource.getValues(), LOG);
    }
	
	int getCurrentProjectId() {
		if (projectId==0) {
	        NamedParameterJdbcTemplate jt = new NamedParameterJdbcTemplate(this.getDataSource());
	        String sql = "SELECT project_id FROM project WHERE project_code=:project_code";
	        MapSqlParameterSource paramSource = new MapSqlParameterSource();
	        paramSource.addValue("project_code", this.projectCode);
	       
	        LOG.trace(sql);
	        LogParameters(paramSource);
	        try {
	        	projectId = jt.queryForInt(sql, paramSource);
	        } catch (EmptyResultDataAccessException ex) {
	        	// do nothing
	        }
	        
	        if (projectId==0) {
	            sql = "SELECT nextval('seq_project_id')";
	            LOG.trace(sql);
	            projectId = jt.queryForInt(sql, paramSource);
	            paramSource.addValue("project_id", projectId);
	            
	            sql = "INSERT INTO project (project_id, project_code)" +
	            		" VALUES (:project_id, :project_code)";
	            
	            LOG.trace(sql);
	            LogParameters(paramSource);
	            jt.update(sql, paramSource); 
	        }
		}
		return projectId;
	}
	
	String getFileName(int fileId) {
		String fileName = fileIdMap.get(fileId);
		if (fileName==null) {
			NamedParameterJdbcTemplate jt = new NamedParameterJdbcTemplate(this.getDataSource());
			String sql = "SELECT file_name FROM file WHERE file_id=:file_id";
			MapSqlParameterSource paramSource = new MapSqlParameterSource();
			paramSource.addValue("file_id", fileId);

			LOG.trace(sql);
			LogParameters(paramSource);

			fileName = (String) jt.queryForObject(sql, paramSource, String.class);

			fileIdMap.put(fileId, fileName);
		}
		return fileName;	
	}
	

	int getTextId(String text) {
        int textId = 0;

        NamedParameterJdbcTemplate jt = new NamedParameterJdbcTemplate(this.getDataSource());
        String sql = "SELECT text_id FROM text WHERE text_text=:text_text";
        MapSqlParameterSource paramSource = new MapSqlParameterSource();
        paramSource.addValue("text_text", text);
       
        LOG.trace(sql);
        LogParameters(paramSource);
        try {
	        textId = jt.queryForInt(sql, paramSource);
        } catch (EmptyResultDataAccessException ex) {
        	// do nothing
        }
        
        if (textId==0) {
            sql = "SELECT nextval('seq_text_id')";
            LOG.trace(sql);
            textId = jt.queryForInt(sql, paramSource);
            paramSource.addValue("text_id", textId);
            
            sql = "INSERT INTO text (text_id, text_text)" +
            		" VALUES (:text_id, :text_text)";
            
            LOG.trace(sql);
            LogParameters(paramSource);
            jt.update(sql, paramSource); 
        }

		return textId;
	}
	
	int getFileId(String fileName) {
        int fileId = 0;
		Integer fileIdObj = filenameMap.get(fileName);
		if (fileIdObj==null) {
	        NamedParameterJdbcTemplate jt = new NamedParameterJdbcTemplate(this.getDataSource());
	        String sql = "SELECT file_id FROM file WHERE file_name=:file_name";
	        MapSqlParameterSource paramSource = new MapSqlParameterSource();
	        paramSource.addValue("file_name", fileName);
	       
	        LOG.trace(sql);
	        LogParameters(paramSource);
	        try {
		        fileId = jt.queryForInt(sql, paramSource);
	        } catch (EmptyResultDataAccessException ex) {
	        	// do nothing
	        }
	        
	        if (fileId==0) {
	            sql = "SELECT nextval('seq_file_id')";
	            LOG.trace(sql);
	            fileId = jt.queryForInt(sql, paramSource);
	            paramSource.addValue("file_id", fileId);
	            
	            sql = "INSERT INTO file (file_id, file_name)" +
	            		" VALUES (:file_id, :file_name)";
	            
	            LOG.trace(sql);
	            LogParameters(paramSource);
	            jt.update(sql, paramSource); 
	        }
	        filenameMap.put(fileName, fileId);
		} else {
			fileId = fileIdObj.intValue();
		}
		return fileId;
	}
	
	void saveExpansions(Term term) {
		PostGresTerm iTerm = (PostGresTerm) term;
		for (Term expansion : iTerm.getExpansionSet().getItemsAdded()) {
	        NamedParameterJdbcTemplate jt = new NamedParameterJdbcTemplate(this.getDataSource());
	        String sql = "INSERT INTO term_expansions (termexp_term_id, termexp_expansion_id)" +
				" VALUES (:termexp_term_id, :termexp_expansion_id)";
	        MapSqlParameterSource paramSource = new MapSqlParameterSource();
	        paramSource.addValue("termexp_term_id", iTerm.getId());
	        paramSource.addValue("termexp_expansion_id", ((PostGresTerm)expansion).getId());
	        
	        LOG.trace(sql);
            LogParameters(paramSource);
            jt.update(sql, paramSource); 
		}
		iTerm.getExpansionSet().cleanSlate();
	}
	

	void saveHeads(Term term) {
		PostGresTerm iTerm = (PostGresTerm) term;
		for (Term head : iTerm.getHeadSet().getItemsAdded()) {
	        NamedParameterJdbcTemplate jt = new NamedParameterJdbcTemplate(this.getDataSource());
	        String sql = "INSERT INTO term_heads (termhead_term_id, termhead_head_id)" +
				" VALUES (:termhead_term_id, :termhead_head_id)";
	        MapSqlParameterSource paramSource = new MapSqlParameterSource();
	        paramSource.addValue("termhead_head_id", ((PostGresTerm)head).getId());
	        paramSource.addValue("termhead_term_id", iTerm.getId());
	        
	        LOG.trace(sql);
            LogParameters(paramSource);
            jt.update(sql, paramSource); 
		}
		iTerm.getHeadSet().cleanSlate();
	}

	@Override
	public Context getContext(String fileName, int lineNumber, int columnNumber) {
		PostGresContext context = this.newContext();
		context.setFileName(fileName);
		context.setLineNumber(lineNumber);
		context.setColumnNumber(columnNumber);
		return context;
	}
	
	PostGresTerm newTerm() {
		PostGresTerm term = new PostGresTermImpl();
		term.setTerminologyBase(this);
		return term;
	}

	PostGresContext newContext() {
		PostGresContextImpl context = new PostGresContextImpl();
		return context;
	}


}
