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
package com.joliciel.talismane.terminology.postgres;

import java.beans.PropertyVetoException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
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
    
    private static final String SELECT_TERM_ONLY = "term_id, term_marked, term_text, term_lexical_words";
    private static final String SELECT_TERM = SELECT_TERM_ONLY + ", count(context_id) AS term_frequency";
    private static final String SELECT_CONTEXT = "context_id, context_start_row, context_start_column, context_end_row, context_end_column, context_text, context_file_id, context_term_id";
    
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
	public List<Term> findTerms(int frequencyThreshold, String searchText, final int maxLexicalWords,
			Boolean marked, Boolean markedExpansions) {
		MONITOR.startTask("getTerms");
		try {
			NamedParameterJdbcTemplate jt = new NamedParameterJdbcTemplate(this.getDataSource());
	        String sql = "SELECT " + SELECT_TERM + " FROM term" +
	        		" INNER JOIN context ON context_term_id = term_id" +
	        		" INNER JOIN projectfile ON context_file_id = projectfile_file_id" +
	        		" WHERE projectfile_project_id = :term_project_id";
	        if (marked!=null && markedExpansions!=null && marked && markedExpansions) {
	        	sql += " AND term_marked = :term_marked";
		        if (searchText!=null && searchText.length()>0)
		        	sql += " AND term_text LIKE :term_text";
		        sql += " GROUP BY " + SELECT_TERM_ONLY;
	        } else {
		        if (searchText!=null && searchText.length()>0)
		        	sql += " AND term_text LIKE :term_text";
		        if (marked!=null && marked)
		        	sql += " AND term_marked = :term_marked";
		        if (maxLexicalWords>0)
		        	sql += " AND term_lexical_words <= :max_lexical_words";
		        sql += " GROUP BY " + SELECT_TERM_ONLY;
		        if (frequencyThreshold>0)
		        	sql += " HAVING count(context_id) >= :term_frequency";
	        }
	        
	        sql += " ORDER BY term_frequency DESC, term_text";
	        MapSqlParameterSource paramSource = new MapSqlParameterSource();
	        if (frequencyThreshold>0)
	        	paramSource.addValue("term_frequency", frequencyThreshold);
	        if (searchText!=null && searchText.length()>0)
	        	paramSource.addValue("term_text",  searchText + "%");
	        if (marked!=null && marked)
	        	paramSource.addValue("term_marked", true);
	        if (maxLexicalWords>0)
	        	paramSource.addValue("max_lexical_words", maxLexicalWords);

	        paramSource.addValue("term_project_id", this.getCurrentProjectId());
	       
	        LOG.trace(sql);
	        LogParameters(paramSource);
	        TermMapper termMapper = new TermMapper();
	        SqlRowSet rs = jt.queryForRowSet(sql, paramSource);
	        List<Term> terms = new ArrayList<Term>();
	        while (rs.next()) {
	        	PostGresTerm term = termMapper.mapRow(rs);
	        	terms.add(term);
	        }
			
	        
	        if (marked!=null && markedExpansions!=null && marked && markedExpansions) {
	        	this.addParents(terms);
	        	List<Term> termsWithFrequency = new ArrayList<Term>();
	        	for (Term term : terms) {
	        		int maxAncestorFrequency = this.getMaxAncestorFrequency(term);
	        		if (maxAncestorFrequency>=frequencyThreshold)
	        			termsWithFrequency.add(term);
	        	}
	        	terms = termsWithFrequency;
	        }

	        this.addHeadAndExpansionCounts(terms);
			return terms;
		} finally {
			MONITOR.endTask();
		}
	}
	
	void addHeadAndExpansionCounts(Collection<Term> terms) {
		if (terms.size()>0) {
			Map<Integer,PostGresTerm> termMap = new HashMap<Integer, PostGresTerm>();
			
			for (Term term : terms) {
				PostGresTerm postGresTerm = (PostGresTerm) term;
				termMap.put(postGresTerm.getId(), postGresTerm);
			}
			
			NamedParameterJdbcTemplate jt = new NamedParameterJdbcTemplate(this.getDataSource());
	        String sql = "SELECT termexp_term_id, count(termexp_expansion_id) AS term_expansion_count FROM term_expansions" +
	        		" INNER JOIN context ON termexp_expansion_id = context_term_id" +
	        		" INNER JOIN projectfile ON context_file_id = projectfile_file_id" +
	        		" WHERE projectfile_project_id = :term_project_id" +
	        		" AND termexp_term_id IN (:term_ids)" +
	        		" GROUP BY termexp_term_id";
	
	        MapSqlParameterSource paramSource = new MapSqlParameterSource();
	        paramSource.addValue("term_project_id", this.getCurrentProjectId());
	        paramSource.addValue("term_ids", termMap.keySet());
			
	        SqlRowSet rs = jt.queryForRowSet(sql, paramSource);
	        while (rs.next()) {
	        	int termId = rs.getInt("termexp_term_id");
	        	PostGresTerm postGresTerm = termMap.get(termId);
	        	int termExpansionCount = rs.getInt("term_expansion_count");
	        	postGresTerm.setExpansionCount(termExpansionCount);
	        	postGresTerm.setDirty(false);
	        }
	        
	        jt = new NamedParameterJdbcTemplate(this.getDataSource());
	        sql = "SELECT termhead_term_id, count(termhead_head_id) AS term_head_count FROM term_heads" +
				" INNER JOIN context ON termhead_head_id = context_term_id" +
				" INNER JOIN projectfile ON context_file_id = projectfile_file_id" +
				" WHERE projectfile_project_id = :term_project_id" +
				" AND termhead_term_id IN (:term_ids)" +
				" GROUP BY termhead_term_id";
	        
	        paramSource = new MapSqlParameterSource();
	        paramSource.addValue("term_project_id", this.getCurrentProjectId());
	        paramSource.addValue("term_ids", termMap.keySet());
			
	        rs = jt.queryForRowSet(sql, paramSource);
	        while (rs.next()) {
	        	int termId = rs.getInt("termhead_term_id");
	        	PostGresTerm postGresTerm = termMap.get(termId);
	        	int termHeadCount = rs.getInt("term_head_count");
	        	postGresTerm.setHeadCount(termHeadCount);
	        	postGresTerm.setDirty(false);
	        }
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
        String sql = "SELECT " + SELECT_TERM + ", term_expansion_id FROM term_expansions" +
			" INNER JOIN context ON termexp_expansion_id = context_term_id" +
			" INNER JOIN projectfile ON context_file_id = projectfile_file_id" +
			" WHERE projectfile_project_id = :term_project_id" +
			" AND termexp_expansion_id IN (:child_terms)" +
			" GROUP BY " + SELECT_TERM_ONLY;
        
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

	@Override
	public Term findTerm(final String text) {
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
			MONITOR.endTask();
		}
	}

	@Override
	public void storeTerm(Term term) {
		MONITOR.startTask("storeTerm");
		try {
			PostGresTerm termInternal = (PostGresTerm) term;
			this.saveTerm(termInternal);
			this.saveExpansions(termInternal);
			this.saveHeads(termInternal);

		} finally {
			MONITOR.endTask();
		}
	}

	@Override
	public void storeContext(Context context) {
		MONITOR.startTask("storeContext");
		try {
			this.saveContext((PostGresContext)context);
		} finally {
			MONITOR.endTask();
		}
	}

	@Override
	public void commit() {
		MONITOR.startTask("commit");
		try {
			// nothing to do here, not being transactional about it
		} finally {
			MONITOR.endTask();
		}
	}

	@Override
	public Set<Term> getParents(final Term term) {
		MONITOR.startTask("getParents");
		try {
			NamedParameterJdbcTemplate jt = new NamedParameterJdbcTemplate(this.getDataSource());
	        String sql = "SELECT " + SELECT_TERM + " FROM term" +
    			" INNER JOIN context ON context_term_id = term_id" +
        		" INNER JOIN projectfile ON context_file_id = projectfile_file_id" +
        		" INNER JOIN term_expansions ON term_id = termexp_term_id" +
        		" WHERE projectfile_project_id = :term_project_id" +
	        	" AND termexp_expansion_id = :term_id" +
	        	" GROUP BY " + SELECT_TERM_ONLY +
	        	" ORDER BY term_text";
	        MapSqlParameterSource paramSource = new MapSqlParameterSource();
	        paramSource.addValue("term_id", ((PostGresTerm) term).getId());
	        paramSource.addValue("term_project_id", this.getCurrentProjectId());
       
	        LOG.trace(sql);
	        LogParameters(paramSource);
	        @SuppressWarnings("unchecked")
			List<Term> terms = jt.query(sql, paramSource, new TermMapper());
	        
	        this.addHeadAndExpansionCounts(terms);
	        
	        Set<Term> termSet = new TreeSet<Term>(new TermFrequencyComparator());
	        termSet.addAll(terms);
	        
			return termSet;
		} finally {
			MONITOR.endTask();
		}
	}

	@Override
	public Set<Term> getExpansions(Term term) {
		MONITOR.startTask("getExpansions");
		try {
			NamedParameterJdbcTemplate jt = new NamedParameterJdbcTemplate(this.getDataSource());
	        String sql = "SELECT " + SELECT_TERM + " FROM term" +
   				" INNER JOIN context ON context_term_id = term_id" +
   		 	   	" INNER JOIN projectfile ON context_file_id = projectfile_file_id" +
	    		" INNER JOIN term_expansions ON term_id = termexp_expansion_id" +
	    		" WHERE projectfile_project_id = :term_project_id" +
	        	" AND termexp_term_id = :term_id" +
	        	" GROUP BY " + SELECT_TERM_ONLY +
	        	" ORDER BY term_text";
	        MapSqlParameterSource paramSource = new MapSqlParameterSource();
	        paramSource.addValue("term_id", ((PostGresTerm) term).getId());
	        paramSource.addValue("term_project_id", this.getCurrentProjectId());
	       
	        LOG.trace(sql);
	        LogParameters(paramSource);
	        @SuppressWarnings("unchecked")
			List<Term> terms = jt.query(sql, paramSource, new TermMapper());
	        this.addHeadAndExpansionCounts(terms);
	        
	        Set<Term> termSet = new TreeSet<Term>(new TermFrequencyComparator());
	        termSet.addAll(terms);
			return termSet;
		} finally {
			MONITOR.endTask();
		}
	}



	@Override
	public Set<Term> getHeads(Term term) {
		MONITOR.startTask("getHeads");
		try {
			NamedParameterJdbcTemplate jt = new NamedParameterJdbcTemplate(this.getDataSource());
	        String sql = "SELECT " + SELECT_TERM + " FROM term" +
				" INNER JOIN context ON context_term_id = term_id" +
	    		" INNER JOIN projectfile ON context_file_id = projectfile_file_id" +
	    		" INNER JOIN term_heads ON term_id = termhead_head_id" +
	    		" WHERE projectfile_project_id = :term_project_id" +
	        	" AND termhead_term_id = :term_id" +
	        	" GROUP BY " + SELECT_TERM_ONLY +
	        	" ORDER BY term_text";
	        MapSqlParameterSource paramSource = new MapSqlParameterSource();
	        paramSource.addValue("term_id", ((PostGresTerm) term).getId());
	        paramSource.addValue("term_project_id", this.getCurrentProjectId());
	       
	        LOG.trace(sql);
	        LogParameters(paramSource);
	        @SuppressWarnings("unchecked")
			List<Term> terms = jt.query(sql, paramSource, new TermMapper());
	        this.addHeadAndExpansionCounts(terms);
	        
	        Set<Term> termSet = new TreeSet<Term>(new TermFrequencyComparator());
	        termSet.addAll(terms);
			return termSet;
		} finally {
			MONITOR.endTask();
		}
	}


	@Override
	public List<Context> getContexts(Term term) {
		MONITOR.startTask("getContexts");
		try {
			NamedParameterJdbcTemplate jt = new NamedParameterJdbcTemplate(this.getDataSource());
	        String sql = "SELECT " + SELECT_CONTEXT + " FROM context" +
		    		" INNER JOIN projectfile ON context_file_id = projectfile_file_id" +
		    		" WHERE projectfile_project_id = :term_project_id" +
	        		" AND context_term_id = :context_term_id" +
	        		" ORDER BY context_id";
	        MapSqlParameterSource paramSource = new MapSqlParameterSource();
	        paramSource.addValue("context_term_id", ((PostGresTerm) term).getId());
	        paramSource.addValue("term_project_id", this.getCurrentProjectId());
	       
	        LOG.trace(sql);
	        LogParameters(paramSource);
	        @SuppressWarnings("unchecked")
			List<Context> contexts = jt.query(sql, paramSource, new ContextMapper());

			return contexts;
		} finally {
			MONITOR.endTask();
		}
	}

    Term loadTerm(int termId) {
        NamedParameterJdbcTemplate jt = new NamedParameterJdbcTemplate(this.getDataSource());
        String sql = "SELECT " + SELECT_TERM + " FROM term" +
			" LEFT JOIN context ON context_term_id = term_id" +
			" LEFT JOIN projectfile ON context_file_id = projectfile_file_id" +
	   		" AND projectfile_project_id = :term_project_id" +
	   	 	" WHERE term_id=:term_id" +
        	" GROUP BY " + SELECT_TERM_ONLY;
        MapSqlParameterSource paramSource = new MapSqlParameterSource();
        paramSource.addValue("term_id", termId);
        paramSource.addValue("term_project_id", this.getCurrentProjectId());
       
        LOG.trace(sql);
        LogParameters(paramSource);
        Term term = null;
        try {
            term = (Term)  jt.queryForObject(sql, paramSource, new TermMapper());
        } catch (EmptyResultDataAccessException ex) {
            ex.hashCode();
        }
        
        if (term!=null)
        	this.addHeadAndExpansionCounts(new ArrayList<Term>(Arrays.asList(term)));

        return term;
    }
    
    public Term loadTerm(String termText) {
       NamedParameterJdbcTemplate jt = new NamedParameterJdbcTemplate(this.getDataSource());
        
       String sql = "SELECT " + SELECT_TERM + " FROM term" +
			" LEFT JOIN context ON context_term_id = term_id" +
			" LEFT JOIN projectfile ON context_file_id = projectfile_file_id" +
			" AND projectfile_project_id = :term_project_id" +
			" WHERE term_text=:term_text" +
			" GROUP BY " + SELECT_TERM_ONLY;
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
        
        if (term!=null)
        	this.addHeadAndExpansionCounts(new ArrayList<Term>(Arrays.asList(term)));

        return term;
    }

    void saveTerm(PostGresTerm term) {
    	if (term.isDirty()) {
	        NamedParameterJdbcTemplate jt = new NamedParameterJdbcTemplate(this.getDataSource());
	        MapSqlParameterSource paramSource = new MapSqlParameterSource();
	        paramSource.addValue("term_text", term.getText());
	        paramSource.addValue("term_marked", term.isMarked());
	        paramSource.addValue("term_lexical_words", term.getLexicalWordCount());
	        
	        if (term.isNew()) {
	            String sql = "SELECT nextval('seq_term_id')";
	            LOG.trace(sql);
	            int termId = jt.queryForInt(sql, paramSource);
	            paramSource.addValue("term_id", termId);
	            
	            sql = "INSERT INTO term (term_id, term_marked, term_text, term_lexical_words)" +
	            		" VALUES (:term_id, :term_marked, :term_text, :term_lexical_words)";
	            
	            LOG.trace(sql);
	            LogParameters(paramSource);
	            jt.update(sql, paramSource);
	            term.setId(termId);
	        } else {
	            String sql = "UPDATE term" +
	            		" SET term_marked = :term_marked" +
	            		", term_text = :term_text" +
	            		", term_lexical_words = :term_lexical_words" +
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

        public PostGresTerm mapRow(SqlRowSet rs) {
            PostGresTerm term = newTerm();
            term.setId(rs.getInt("term_id"));
            term.setText(rs.getString("term_text"));
            term.setFrequency(rs.getInt("term_frequency"));
            term.setMarked(rs.getBoolean("term_marked"));
            term.setLexicalWordCount(rs.getInt("term_lexical_words"));
            term.setDirty(false);
            return term;
        }
    }
    
    Context loadContext(int contextId) {
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
    
	@Override
	public Context findContext(Term term, String fileName, int lineNumber, int columnNumber) {
        NamedParameterJdbcTemplate jt = new NamedParameterJdbcTemplate(this.getDataSource());
        String sql = "SELECT " + SELECT_CONTEXT + " FROM context" +
        		" WHERE context_term_id=:context_term_id" +
        		" AND context_file_id=:context_file_id" +
        		" AND context_start_row=:context_start_row" +
        		" AND context_start_column=:context_start_column";
        
        int fileId = this.getFileId(fileName);
        MapSqlParameterSource paramSource = new MapSqlParameterSource();
        paramSource.addValue("context_term_id", ((PostGresTerm)term).getId());
        paramSource.addValue("context_file_id", fileId);
        paramSource.addValue("context_start_row", lineNumber);
        paramSource.addValue("context_start_column", columnNumber);
       
        LOG.trace(sql);
        LogParameters(paramSource);
        PostGresContext context = null;
        try {
            context = (PostGresContext)  jt.queryForObject(sql, paramSource, new ContextMapper());
        } catch (EmptyResultDataAccessException ex) {
            ex.hashCode();
        }
        if (context==null) {
	        context = this.newContext();
			context.setFileName(fileName);
			context.setFileId(fileId);
			context.setLineNumber(lineNumber);
			context.setColumnNumber(columnNumber);
			context.setTerm(term);
        }
		return context;
	}
	
    void saveContext(PostGresContext context) {
    	if (context.isDirty()) {
	        NamedParameterJdbcTemplate jt = new NamedParameterJdbcTemplate(this.getDataSource());
	        MapSqlParameterSource paramSource = new MapSqlParameterSource();
	        paramSource.addValue("context_start_row", context.getLineNumber());
	        paramSource.addValue("context_start_column", context.getColumnNumber());
	        paramSource.addValue("context_end_row", context.getEndLineNumber());
	        paramSource.addValue("context_end_column", context.getEndColumnNumber());
	        paramSource.addValue("context_text", context.getTextSegment());
	        paramSource.addValue("context_term_id", context.getTermId());
	        paramSource.addValue("context_file_id", context.getFileId());
	
	        if (context.isNew()) {
	            String sql = "SELECT nextval('seq_context_id')";
	            LOG.trace(sql);
	            int contextId = jt.queryForInt(sql, paramSource);
	            paramSource.addValue("context_id", contextId);
	            
	            sql = "INSERT INTO context (context_id, context_start_row, context_start_column, context_end_row, context_end_column, context_text, context_file_id, context_term_id)" +
	            		" VALUES (:context_id, :context_start_row, :context_start_column, :context_end_row, :context_end_column, :context_text, :context_file_id, :context_term_id)";
	            
	            LOG.trace(sql);
	            LogParameters(paramSource);
	            jt.update(sql, paramSource);
	            context.setId(contextId);
	        } else {
	            String sql = "UPDATE context" +
	        		" SET context_start_row = :context_start_row" +
	        		", context_start_column = :context_start_column" +
	        		", context_end_row = :context_end_row" +
	        		", context_end_column = :context_end_column" +
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

        public PostGresContext mapRow(SqlRowSet rs) {
        	// context_id, context_start_row, context_start_column, context_text, context_file_id, context_term_id
            PostGresContext context = newContext();
            context.setId(rs.getInt("context_id"));
            context.setTextSegment(rs.getString("context_text"));
            context.setColumnNumber(rs.getInt("context_start_column"));
            context.setLineNumber(rs.getInt("context_start_row"));
            context.setEndColumnNumber(rs.getInt("context_end_column"));
            context.setEndLineNumber(rs.getInt("context_end_row"));
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
	        
	        boolean haveProjectFile = false;
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
	        } else {
	        	sql = "SELECT projectfile_file_id FROM projectfile" +
	        		" WHERE projectfile_file_id = :file_id" +
	        		" AND projectfile_project_id = :project_id";
		        paramSource = new MapSqlParameterSource();
		        paramSource.addValue("file_id", fileId);
		        paramSource.addValue("project_id", this.getCurrentProjectId());
		        LOG.trace(sql);
		        LogParameters(paramSource);
		        try {
			        jt.queryForInt(sql, paramSource);
			        haveProjectFile = true;
		        } catch (EmptyResultDataAccessException ex) {
		        	haveProjectFile = false;
		        }
	        }
	        filenameMap.put(fileName, fileId);
	        
	        if (!haveProjectFile) {
	            sql = "INSERT INTO projectfile (projectfile_project_id, projectfile_file_id)" +
        			" VALUES (:project_id, :file_id)";
	            paramSource = new MapSqlParameterSource();
		        paramSource.addValue("file_id", fileId);
		        paramSource.addValue("project_id", this.getCurrentProjectId());
        
		        LOG.trace(sql);
		        LogParameters(paramSource);
		        jt.update(sql, paramSource); 
	        }
	        
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
	
	PostGresTerm newTerm() {
		PostGresTerm term = new PostGresTermImpl();
		term.setTerminologyBase(this);
		return term;
	}

	PostGresContext newContext() {
		PostGresContext context = new PostGresContextImpl();
		context.setTerminologyBase(this);
		return context;
	}


}
