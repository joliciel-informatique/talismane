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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import org.springframework.jdbc.support.rowset.ResultSetWrappingSqlRowSet;
import org.springframework.jdbc.support.rowset.SqlRowSet;

import com.joliciel.talismane.lexicon.LexicalEntry;
import com.joliciel.talismane.lexicon.LexicalEntryStatus;
import com.joliciel.talismane.utils.DaoUtils;
import com.joliciel.talismane.utils.DaoUtils.LogLevel;

class LefffDaoImpl implements LefffDao {
    private static final Log LOG = LogFactory.getLog(LefffDaoImpl.class);
    LefffServiceInternal lefffServiceInternal;
    
    private DataSource dataSource;

    private static final String SELECT_ATTRIBUTE = "attribute_id, attribute_code, attribute_value, attribute_morph";
    private static final String SELECT_CATEGORY = "category_id, category_code, category_description";
    private static final String SELECT_ENTRY = "entry_id, entry_word_id, entry_lemma_id, entry_predicate_id, entry_morph_id, entry_lexical_weight, entry_category_id, entry_status";
    private static final String SELECT_LEMMA = "lemma_id, lemma_text, lemma_index, lemma_complement";
    private static final String SELECT_PREDICATE = "predicate_id, predicate_text";
    private static final String SELECT_WORD = "word_id, word_text";

    public Attribute loadAttribute(int attributeId) {
        NamedParameterJdbcTemplate jt = new NamedParameterJdbcTemplate(this.getDataSource());
        String sql = "SELECT " + SELECT_ATTRIBUTE + " FROM lef_attribute WHERE attribute_id=:attribute_id";
        MapSqlParameterSource paramSource = new MapSqlParameterSource();
        paramSource.addValue("attribute_id", attributeId);
       
        LOG.info(sql);
        LefffDaoImpl.LogParameters(paramSource);
        Attribute attribute = null;
        try {
            attribute = (Attribute)  jt.queryForObject(sql, paramSource, new AttributeMapper(this.getLefffServiceInternal()));
        } catch (EmptyResultDataAccessException ex) {
            ex.hashCode();
        }
        return attribute;
    }
    
    public Attribute loadAttribute(String attributeCode, String attributeValue) {
        NamedParameterJdbcTemplate jt = new NamedParameterJdbcTemplate(this.getDataSource());
        
        String sql = "SELECT " + SELECT_ATTRIBUTE + " FROM lef_attribute" +
        		" WHERE attribute_code=:attribute_code" +
        		" AND attribute_value=:attribute_value";
        MapSqlParameterSource paramSource = new MapSqlParameterSource();
        paramSource.addValue("attribute_code", attributeCode);
        paramSource.addValue("attribute_value", attributeValue);
        
        LOG.info(sql);
        LefffDaoImpl.LogParameters(paramSource);
        Attribute attribute = null;
        try {
            attribute = (Attribute)  jt.queryForObject(sql, paramSource, new AttributeMapper(this.getLefffServiceInternal()));
        } catch (EmptyResultDataAccessException ex) {
            ex.hashCode();
        }
        return attribute;
    }
    
	@SuppressWarnings("unchecked")
	@Override
	public List<Attribute> findAttributes(LefffEntryInternal entry) {
		NamedParameterJdbcTemplate jt = new NamedParameterJdbcTemplate(this.getDataSource());
        String sql = "SELECT " + SELECT_ATTRIBUTE + " FROM lef_attribute, lef_entry_attribute" +
        		" WHERE attribute_id = entatt_attribute_id AND entatt_entry_id = :entry_id" +
        		" ORDER BY attribute_code, attribute_value";
        MapSqlParameterSource paramSource = new MapSqlParameterSource();
        paramSource.addValue("entry_id", entry.getId());
       
        LOG.info(sql);
        LefffDaoImpl.LogParameters(paramSource);
        List<Attribute> attributes = jt.query(sql, paramSource, new AttributeMapper(this.getLefffServiceInternal()));
       
        return attributes;
	}


	public void saveAttribute(AttributeInternal attribute) {
        NamedParameterJdbcTemplate jt = new NamedParameterJdbcTemplate(this.getDataSource());
        MapSqlParameterSource paramSource = new MapSqlParameterSource();
        paramSource.addValue("attribute_code", attribute.getCode());
        paramSource.addValue("attribute_value", attribute.getValue());
        paramSource.addValue("attribute_morph", attribute.isMorphological());
        if (attribute.isNew()) {
            String sql = "SELECT nextval('seq_attribute_id')";
            LOG.info(sql);
            int attributeId = jt.queryForInt(sql, paramSource);
            paramSource.addValue("attribute_id", attributeId);
            
            sql = "INSERT INTO lef_attribute (attribute_id, attribute_code, attribute_value, attribute_morph)" +
            		" VALUES (:attribute_id, :attribute_code, :attribute_value, :attribute_morph)";
            
            LOG.info(sql);
            LefffDaoImpl.LogParameters(paramSource);
            jt.update(sql, paramSource);
            attribute.setId(attributeId);
        } else {
            String sql = "UPDATE lef_attribute" +
            		" SET attribute_code = :attribute_code" +
            		", attribute_value = :attribute_value" +
            		", attribute_morph = :attribute_morph" +
            		" WHERE attribute_id = :attribute_id";
            
            paramSource.addValue("attribute_id", attribute.getId());
            LOG.info(sql);
            LefffDaoImpl.LogParameters(paramSource);
            jt.update(sql, paramSource);           
        }
    }
    
    public void deleteAttribute(int attributeId) {
        NamedParameterJdbcTemplate jt = new NamedParameterJdbcTemplate(this.getDataSource());
        MapSqlParameterSource paramSource = new MapSqlParameterSource();
        String sql = "DELETE FROM lef_attribute WHERE attribute_id = :attribute_id";
        paramSource.addValue("attribute_id", attributeId);
        LOG.info(sql);
        LefffDaoImpl.LogParameters(paramSource);
        jt.update(sql, paramSource);
    }
    
    protected static final class AttributeMapper implements RowMapper {
        private LefffServiceInternal lefffService;
        public AttributeMapper(LefffServiceInternal lefffService) {
            this.lefffService = lefffService;
        };

        public Object mapRow(ResultSet rs, int rowNum) throws SQLException {
            return this.mapRow(new ResultSetWrappingSqlRowSet(rs));
        }

        public AttributeInternal mapRow(SqlRowSet rs) {
            AttributeInternal attribute = lefffService.newAttribute();
            attribute.setId(rs.getInt("attribute_id"));
            attribute.setCode(rs.getString("attribute_code"));
            attribute.setValue(rs.getString("attribute_value"));
            attribute.setMorphological(rs.getBoolean("attribute_morph"));
            return attribute;
        }
    }

    public Category loadCategory(int categoryId) {
        NamedParameterJdbcTemplate jt = new NamedParameterJdbcTemplate(this.getDataSource());
        String sql = "SELECT " + SELECT_CATEGORY + " FROM lef_category WHERE category_id=:category_id";
        MapSqlParameterSource paramSource = new MapSqlParameterSource();
        paramSource.addValue("category_id", categoryId);
       
        LOG.info(sql);
        LefffDaoImpl.LogParameters(paramSource);
        Category category = null;
        try {
            category = (Category)  jt.queryForObject(sql, paramSource, new CategoryMapper(this.getLefffServiceInternal()));
        } catch (EmptyResultDataAccessException ex) {
            ex.hashCode();
        }
        return category;
    }
    
    public Category loadCategory(String categoryCode) {
        NamedParameterJdbcTemplate jt = new NamedParameterJdbcTemplate(this.getDataSource());
        String sql = "SELECT " + SELECT_CATEGORY + " FROM lef_category WHERE category_code=:category_code";
        MapSqlParameterSource paramSource = new MapSqlParameterSource();
        paramSource.addValue("category_code", categoryCode);
       
        LOG.info(sql);
        LefffDaoImpl.LogParameters(paramSource);
        Category category = null;
        try {
            category = (Category)  jt.queryForObject(sql, paramSource, new CategoryMapper(this.getLefffServiceInternal()));
        } catch (EmptyResultDataAccessException ex) {
            ex.hashCode();
        }
        return category;
    }

    public void saveCategory(CategoryInternal category) {
        NamedParameterJdbcTemplate jt = new NamedParameterJdbcTemplate(this.getDataSource());
        MapSqlParameterSource paramSource = new MapSqlParameterSource();
        paramSource.addValue("category_code", category.getCode());
        paramSource.addValue("category_description", category.getDescription());
        if (category.isNew()) {
            String sql = "SELECT nextval('seq_category_id')";
            LOG.info(sql);
            int categoryId = jt.queryForInt(sql, paramSource);
            paramSource.addValue("category_id", categoryId);
            
            sql = "INSERT INTO lef_category (category_id, category_code, category_description) VALUES (:category_id, :category_code, :category_description)";
            
            LOG.info(sql);
            LefffDaoImpl.LogParameters(paramSource);
            jt.update(sql, paramSource);
            category.setId(categoryId);
        } else {
            String sql = "UPDATE lef_category" +
            		" SET category_code = :category_code" +
            		", category_description = :category_description" +
            		" WHERE category_id = :category_id";
            
            paramSource.addValue("category_id", category.getId());
            LOG.info(sql);
            LefffDaoImpl.LogParameters(paramSource);
            jt.update(sql, paramSource);           
        }
    }
    
    public void deleteCategory(int categoryId) {
        NamedParameterJdbcTemplate jt = new NamedParameterJdbcTemplate(this.getDataSource());
        MapSqlParameterSource paramSource = new MapSqlParameterSource();
        String sql = "DELETE FROM lef_category WHERE category_id = :category_id";
        paramSource.addValue("category_id", categoryId);
        LOG.info(sql);
        LefffDaoImpl.LogParameters(paramSource);
        jt.update(sql, paramSource);
    }
    
    protected static final class CategoryMapper implements RowMapper {
        private LefffServiceInternal lefffService;
        public CategoryMapper(LefffServiceInternal lefffService) {
            this.lefffService = lefffService;
        };

        public Object mapRow(ResultSet rs, int rowNum) throws SQLException {
            return this.mapRow(new ResultSetWrappingSqlRowSet(rs));
        }

        public Category mapRow(SqlRowSet rs) {
            CategoryInternal category = lefffService.newCategory();
            category.setId(rs.getInt("category_id"));
            category.setCode(rs.getString("category_code"));
            category.setDescription(rs.getString("category_description"));
            return category;
        }
    }

    public Predicate loadPredicate(int predicateId) {
        NamedParameterJdbcTemplate jt = new NamedParameterJdbcTemplate(this.getDataSource());
        String sql = "SELECT " + SELECT_PREDICATE + " FROM lef_predicate WHERE predicate_id=:predicate_id";
        MapSqlParameterSource paramSource = new MapSqlParameterSource();
        paramSource.addValue("predicate_id", predicateId);
       
        LOG.info(sql);
        LefffDaoImpl.LogParameters(paramSource);
        Predicate predicate = null;
        try {
            predicate = (Predicate)  jt.queryForObject(sql, paramSource, new PredicateMapper(this.getLefffServiceInternal()));
        } catch (EmptyResultDataAccessException ex) {
            ex.hashCode();
        }
        return predicate;
    }
    
    public Predicate loadPredicate(String predicateText) {
        NamedParameterJdbcTemplate jt = new NamedParameterJdbcTemplate(this.getDataSource());
        String sql = "SELECT " + SELECT_PREDICATE + " FROM lef_predicate WHERE predicate_text=:predicate_text";
        MapSqlParameterSource paramSource = new MapSqlParameterSource();
        paramSource.addValue("predicate_text", predicateText);
       
        LOG.info(sql);
        LefffDaoImpl.LogParameters(paramSource);
        Predicate predicate = null;
        try {
            predicate = (Predicate)  jt.queryForObject(sql, paramSource, new PredicateMapper(this.getLefffServiceInternal()));
        } catch (EmptyResultDataAccessException ex) {
            ex.hashCode();
        }
        return predicate;
    }
    

    public void savePredicate(PredicateInternal predicate) {
        NamedParameterJdbcTemplate jt = new NamedParameterJdbcTemplate(this.getDataSource());
        MapSqlParameterSource paramSource = new MapSqlParameterSource();
        paramSource.addValue("predicate_text", predicate.getText());
        if (predicate.isNew()) {
            String sql = "SELECT nextval('seq_predicate_id')";
            LOG.info(sql);
            int predicateId = jt.queryForInt(sql, paramSource);
            paramSource.addValue("predicate_id", predicateId);
            
            sql = "INSERT INTO lef_predicate (predicate_id, predicate_text) VALUES (:predicate_id, :predicate_text)";
            
            LOG.info(sql);
            LefffDaoImpl.LogParameters(paramSource);
            jt.update(sql, paramSource);
            predicate.setId(predicateId);
        } else {
            String sql = "UPDATE lef_predicate" +
            		" SET predicate_text = :predicate_text" +
            		" WHERE predicate_id = :predicate_id";
            
            paramSource.addValue("predicate_id", predicate.getId());
            LOG.info(sql);
            LefffDaoImpl.LogParameters(paramSource);
            jt.update(sql, paramSource);           
        }
    }
    
    public void deletePredicate(int predicateId) {
        NamedParameterJdbcTemplate jt = new NamedParameterJdbcTemplate(this.getDataSource());
        MapSqlParameterSource paramSource = new MapSqlParameterSource();
        String sql = "DELETE FROM lef_predicate WHERE predicate_id = :predicate_id";
        paramSource.addValue("predicate_id", predicateId);
        LOG.info(sql);
        LefffDaoImpl.LogParameters(paramSource);
        jt.update(sql, paramSource);
    }
    
    protected static final class PredicateMapper implements RowMapper {
        private LefffServiceInternal lefffService;
        public PredicateMapper(LefffServiceInternal lefffService) {
            this.lefffService = lefffService;
        };

        public Object mapRow(ResultSet rs, int rowNum) throws SQLException {
            return this.mapRow(new ResultSetWrappingSqlRowSet(rs));
        }

        public PredicateInternal mapRow(SqlRowSet rs) {
            PredicateInternal predicate = lefffService.newPredicate();
            predicate.setId(rs.getInt("predicate_id"));
            predicate.setText(rs.getString("predicate_text"));
            return predicate;
        }
    }
    
	@Override
	public Lemma loadLemma(int lemmaId) {
       NamedParameterJdbcTemplate jt = new NamedParameterJdbcTemplate(this.getDataSource());
        String sql = "SELECT " + SELECT_LEMMA	 + " FROM lef_lemma WHERE lemma_id=:lemma_id";
        MapSqlParameterSource paramSource = new MapSqlParameterSource();
        paramSource.addValue("lemma_id", lemmaId);
       
        LOG.info(sql);
        LefffDaoImpl.LogParameters(paramSource);
        Lemma lemma = null;
        try {
            lemma = (Lemma)  jt.queryForObject(sql, paramSource, new LemmaMapper(this.getLefffServiceInternal()));
        } catch (EmptyResultDataAccessException ex) {
            ex.hashCode();
        }
        return lemma;
	}

	@Override
	public Lemma loadLemma(String text, int index, String complement) {
		 NamedParameterJdbcTemplate jt = new NamedParameterJdbcTemplate(this.getDataSource());
	        String sql = "SELECT " + SELECT_LEMMA + " FROM lef_lemma" +
	        		" WHERE lemma_text=:lemma_text" +
	        		" AND lemma_index=:lemma_index" +
	        		" AND lemma_complement=:lemma_complement";
	        MapSqlParameterSource paramSource = new MapSqlParameterSource();
	        paramSource.addValue("lemma_text", text);
	        paramSource.addValue("lemma_index", index);
	        paramSource.addValue("lemma_complement", complement);
	       
	        LOG.info(sql);
	        LefffDaoImpl.LogParameters(paramSource);
	        Lemma lemma = null;
	        try {
	            lemma = (Lemma)  jt.queryForObject(sql, paramSource, new LemmaMapper(this.getLefffServiceInternal()));
	        } catch (EmptyResultDataAccessException ex) {
	            ex.hashCode();
	        }
	        return lemma;
	}

	@Override
    public void saveLemma(LemmaInternal lemma) {
        NamedParameterJdbcTemplate jt = new NamedParameterJdbcTemplate(this.getDataSource());
        MapSqlParameterSource paramSource = new MapSqlParameterSource();
        paramSource.addValue("lemma_text", lemma.getText());
        paramSource.addValue("lemma_index", lemma.getIndex());
        paramSource.addValue("lemma_complement", lemma.getComplement());
        if (lemma.isNew()) {
            String sql = "SELECT nextval('seq_lemma_id')";
            LOG.info(sql);
            int lemmaId = jt.queryForInt(sql, paramSource);
            paramSource.addValue("lemma_id", lemmaId);
            
            sql = "INSERT INTO lef_lemma (lemma_id, lemma_text, lemma_index, lemma_complement)" +
            		" VALUES (:lemma_id, :lemma_text, :lemma_index, :lemma_complement)";
            
            LOG.info(sql);
            LefffDaoImpl.LogParameters(paramSource);
            jt.update(sql, paramSource);
            lemma.setId(lemmaId);
        } else {
            String sql = "UPDATE lef_lemma" +
            		" SET lemma_text = :lemma_text" +
            		", lemma_index = :lemma_index" +
            		", lemma_complement = :lemma_complement" +
            		" WHERE lemma_id = :lemma_id";
            
            paramSource.addValue("lemma_id", lemma.getId());
            LOG.info(sql);
            LefffDaoImpl.LogParameters(paramSource);
            jt.update(sql, paramSource);           
        }
    }
    
    public void deleteLemma(int lemmaId) {
        NamedParameterJdbcTemplate jt = new NamedParameterJdbcTemplate(this.getDataSource());
        MapSqlParameterSource paramSource = new MapSqlParameterSource();
        String sql = "DELETE FROM lef_lemma WHERE lemma_id = :lemma_id";
        paramSource.addValue("lemma_id", lemmaId);
        LOG.info(sql);
        LefffDaoImpl.LogParameters(paramSource);
        jt.update(sql, paramSource);
    }
	
    protected static final class LemmaMapper implements RowMapper {
        private LefffServiceInternal lefffService;
        public LemmaMapper(LefffServiceInternal lefffService) {
            this.lefffService = lefffService;
        };

        public Object mapRow(ResultSet rs, int rowNum) throws SQLException {
            return this.mapRow(new ResultSetWrappingSqlRowSet(rs));
        }

        public LemmaInternal mapRow(SqlRowSet rs) {
            LemmaInternal lemma = lefffService.newLemma();
            lemma.setId(rs.getInt("lemma_id"));
            lemma.setText(rs.getString("lemma_text"));
            lemma.setIndex(rs.getInt("lemma_index"));
            lemma.setComplement(rs.getString("lemma_complement"));
           return lemma;
        }
    }
    
    public LefffServiceInternal getLefffServiceInternal() {
        return lefffServiceInternal;
    }

    public void setLefffServiceInternal(
            LefffServiceInternal treebankServiceInternal) {
        this.lefffServiceInternal = treebankServiceInternal;
    }

    public DataSource getDataSource() {
        return dataSource;
    }

    public void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    
    public Word loadWord(String text) {
        NamedParameterJdbcTemplate jt = new NamedParameterJdbcTemplate(this.getDataSource());
        MapSqlParameterSource paramSource = new MapSqlParameterSource();
        
        if (text==null)
            text = "";
        String sql = "SELECT " + SELECT_WORD + " FROM lef_word WHERE word_text=:word_text";
        paramSource.addValue("word_text", text);
            
       
        LOG.info(sql);
        LefffDaoImpl.LogParameters(paramSource);
        Word word = null;
        try {
            word = (Word)  jt.queryForObject(sql, paramSource, new WordMapper( this.lefffServiceInternal));
        } catch (EmptyResultDataAccessException ex) {
            ex.hashCode();
        }
        return word;
    }
    
    public Word loadWord(int wordId) {
        NamedParameterJdbcTemplate jt = new NamedParameterJdbcTemplate(this.getDataSource());
        String sql = "SELECT " + SELECT_WORD + " FROM lef_word WHERE word_id=:word_id";
        MapSqlParameterSource paramSource = new MapSqlParameterSource();
        paramSource.addValue("word_id", wordId);
       
        LOG.info(sql);
        LefffDaoImpl.LogParameters(paramSource);
        Word word = null;
        try {
            word = (Word)  jt.queryForObject(sql, paramSource, new WordMapper( this.lefffServiceInternal));
        } catch (EmptyResultDataAccessException ex) {
            ex.hashCode();
        }
        return word;
    }
    

    protected static final class WordMapper implements RowMapper {
        private String alias = "";
        private LefffServiceInternal lefffService;
        public WordMapper(LefffServiceInternal lefffService) {
            this.lefffService = lefffService;
        };
        public WordMapper(String alias, LefffServiceInternal lefffService) {
            this.lefffService = lefffService;
            this.alias = alias + "_"; 
         }
        public Object mapRow(ResultSet rs, int rowNum) throws SQLException {
            return this.mapRow(new ResultSetWrappingSqlRowSet(rs));
        }

        public WordInternal mapRow(SqlRowSet rs) {
            WordInternal word = lefffService.newWord();
            word.setId(rs.getInt(alias + "word_id"));
            word.setText(rs.getString(alias + "word_text"));
            return word;
        }
    }

    public void saveWord(WordInternal word) {
        NamedParameterJdbcTemplate jt = new NamedParameterJdbcTemplate(this.getDataSource());
        MapSqlParameterSource paramSource = new MapSqlParameterSource();
        paramSource.addValue("word_text", word.getText());
        if (word.isNew()) {
            String sql = "SELECT nextval('seq_word_id')";
            LOG.info(sql);
            int wordId = jt.queryForInt(sql, paramSource);
            paramSource.addValue("word_id", wordId);
            
            sql = "INSERT INTO lef_word (word_id, word_text) VALUES (:word_id, :word_text)";
            
            LOG.info(sql);
            LefffDaoImpl.LogParameters(paramSource);
            jt.update(sql, paramSource);
            
            word.setId(wordId);
        } else {
            String sql = "UPDATE lef_word" +
                    " SET word_text = :word_text" +
                    " WHERE word_id = :word_id";
            
            paramSource.addValue("word_id", word.getId());
            LOG.info(sql);
            LefffDaoImpl.LogParameters(paramSource);
            jt.update(sql, paramSource);           
        }
    }



	@Override
	public LefffEntry loadEntry(int entryId) {
		NamedParameterJdbcTemplate jt = new NamedParameterJdbcTemplate(this.getDataSource());
        String sql = "SELECT " + SELECT_ENTRY + " FROM lef_entry WHERE entry_id=:entry_id";
        MapSqlParameterSource paramSource = new MapSqlParameterSource();
        paramSource.addValue("entry_id", entryId);
       
        LOG.info(sql);
        LefffDaoImpl.LogParameters(paramSource);
        LefffEntry entry = null;
        try {
            entry = (LefffEntry)  jt.queryForObject(sql, paramSource, new EntryMapper( this.lefffServiceInternal));
        } catch (EmptyResultDataAccessException ex) {
            ex.hashCode();
        }
        return entry;
	}
	

    @Override
	public LefffEntryInternal findEntry(Word word, Lemma lemma,
			Category category) {
		NamedParameterJdbcTemplate jt = new NamedParameterJdbcTemplate(this.getDataSource());
        String sql = "SELECT " + SELECT_ENTRY + " FROM lef_entry" +
        		" WHERE entry_word_id = :word_id" +
        		" AND entry_lemma_id = :lemma_id" +
        		" AND entry_category_id = :category_id" +
        		" ORDER BY entry_id";
        MapSqlParameterSource paramSource = new MapSqlParameterSource();
        paramSource.addValue("word_id", word.getId());
        paramSource.addValue("lemma_id", lemma.getId());
        paramSource.addValue("category_id", category.getId());
       
        LOG.info(sql);
        LefffDaoImpl.LogParameters(paramSource);
        @SuppressWarnings("unchecked")
		List<LefffEntryInternal> entries = jt.query(sql, paramSource, new EntryMapper(this.getLefffServiceInternal()));
       
        LefffEntryInternal entry = null;
        if (entries.size()>0)
        	entry = entries.get(0);
        return entry;
	}

	@Override
	public void saveEntry(LefffEntryInternal entry) {
		NamedParameterJdbcTemplate jt = new NamedParameterJdbcTemplate(this.getDataSource());
        MapSqlParameterSource paramSource = new MapSqlParameterSource();
        paramSource.addValue("entry_word_id", entry.getWordId());
        paramSource.addValue("entry_lemma_id", entry.getLemmaId());
        paramSource.addValue("entry_predicate_id", entry.getPredicateId()==0 ? null : entry.getPredicateId());
        paramSource.addValue("entry_morph_id", entry.getMorphologyId()==0 ? null : entry.getMorphologyId());
        paramSource.addValue("entry_lexical_weight", entry.getLexicalWeight());
        paramSource.addValue("entry_category_id", entry.getCategoryId());
        if (entry.isNew()) {
            String sql = "SELECT nextval('seq_entry_id')";
            LOG.info(sql);
            int entryId = jt.queryForInt(sql, paramSource);
            paramSource.addValue("entry_id", entryId);
            
            sql = "INSERT INTO lef_entry (entry_id, entry_word_id, entry_lemma_id, entry_predicate_id, entry_morph_id, entry_lexical_weight, entry_category_id)" +
            		" VALUES (:entry_id, :entry_word_id, :entry_lemma_id, :entry_predicate_id, :entry_morph_id, :entry_lexical_weight, :entry_category_id)";
            
            LOG.info(sql);
            LefffDaoImpl.LogParameters(paramSource);
            jt.update(sql, paramSource);
            
            entry.setId(entryId);
        } else {
            String sql = "UPDATE lef_entry" +
                    " SET entry_word_id = :entry_word_id" +
                    ", entry_lemma_id = :entry_lemma_id" +
                    ", entry_predicate_id = :entry_predicate_id" +
                    ", entry_morph_id = :entry_morph_id" +
                    ", entry_lexical_weight = :entry_lexical_weight" +
                    ", entry_category_id = :entry_category_id" +
                    " WHERE entry_id = :entry_id";
            
            paramSource.addValue("entry_id", entry.getId());
            LOG.info(sql);
            LefffDaoImpl.LogParameters(paramSource);
            jt.update(sql, paramSource);           
        }
	}

    protected static final class EntryMapper implements RowMapper {
        private LefffServiceInternal lefffService;
        public EntryMapper(LefffServiceInternal lefffService) {
            this.lefffService = lefffService;
        };

        public Object mapRow(ResultSet rs, int rowNum) throws SQLException {
            return this.mapRow(new ResultSetWrappingSqlRowSet(rs));
        }

        public LefffEntryInternal mapRow(SqlRowSet rs) {
            LefffEntryInternal entry = lefffService.newEntryInternal();
            entry.setId(rs.getInt("entry_id"));
            entry.setWordId(rs.getInt("entry_word_id"));
            entry.setLemmaId(rs.getInt("entry_lemma_id"));
            entry.setPredicateId(rs.getInt("entry_predicate_id"));
            entry.setMorphologyId(rs.getInt("entry_morph_id"));
            entry.setLexicalWeight(rs.getInt("entry_lexical_weight"));
            entry.setCategoryId(rs.getInt("entry_category_id"));
            
            int statusId = rs.getInt("entry_status");
            LexicalEntryStatus status = LexicalEntryStatus.NEUTRAL;
            if (statusId==1)
            	status = LexicalEntryStatus.SOMEWHAT_UNLIKELY;
            else if (statusId==2)
            	status = LexicalEntryStatus.UNLIKELY;
            else if (statusId==3)
            	status = LexicalEntryStatus.WRONG;
            
            entry.setStatus(status);
            return entry;
        }
    }
    
	@Override
	public void saveAttributes(LefffEntryInternal entry) {
		NamedParameterJdbcTemplate jt = new NamedParameterJdbcTemplate(this.getDataSource());
		MapSqlParameterSource paramSource = new MapSqlParameterSource();
        paramSource.addValue("entatt_entry_id", entry.getId());
         
        String sql = "DELETE FROM lef_entry_attribute WHERE entatt_entry_id = :entatt_entry_id";

	    LOG.info(sql);
	    LefffDaoImpl.LogParameters(paramSource);
	    jt.update(sql, paramSource);
	    
		for (Attribute attribute : entry.getAttributes()) {
			paramSource = new MapSqlParameterSource();
	        paramSource.addValue("entatt_entry_id", entry.getId());
	        paramSource.addValue("entatt_attribute_id", attribute.getId());
	        
	        sql = "INSERT INTO lef_entry_attribute (entatt_entry_id, entatt_attribute_id)" +
    			" VALUES (:entatt_entry_id, :entatt_attribute_id)";
    
		    LOG.info(sql);
		    LefffDaoImpl.LogParameters(paramSource);
		    jt.update(sql, paramSource);
		}
	}

	@Override
	public Map<String, List<LexicalEntry>> findEntryMap(List<String> categories) {
		int version = 3;
		
		NamedParameterJdbcTemplate jt = new NamedParameterJdbcTemplate(this.getDataSource());
        String sql = "SELECT " + SELECT_ENTRY + "," + SELECT_WORD + "," + SELECT_LEMMA + "," +
        	SELECT_CATEGORY + "," + SELECT_PREDICATE + "," + SELECT_ATTRIBUTE +
        	" FROM lef_entry" +
        	" INNER JOIN lef_word ON entry_word_id = word_id" +
        	" INNER JOIN lef_lemma ON entry_lemma_id = lemma_id" +
        	" INNER JOIN lef_category ON entry_category_id = category_id" +
        	" LEFT OUTER JOIN lef_predicate ON entry_predicate_id = predicate_id";
        
        // allow multiple morphological attributes as separate entries in version 3+
        if (version<3)
        	sql += " INNER JOIN lef_attribute ON entry_morph_id = attribute_id";
        else
        	sql += " INNER JOIN lef_entry_attribute ON entry_id = entatt_entry_id" +
        			" INNER JOIN lef_attribute ON entatt_attribute_id = attribute_id";
        
        sql += " WHERE entry_status < 3" +
        		" AND attribute_morph = true";
        
        if (categories!=null && categories.size()>0) {
        	sql += " AND category_code in (:categoryCodes)";
        }

        
        sql +=
        	" ORDER BY entry_status, entry_id";
        MapSqlParameterSource paramSource = new MapSqlParameterSource();
       
        if (categories!=null && categories.size()>0) {
        	paramSource.addValue("categoryCodes", categories);
        }
        LOG.info(sql);
        LefffDaoImpl.LogParameters(paramSource);
        double requiredCapacity = 560000;
        Map<String, List<LexicalEntry>> entryMap = new HashMap<String, List<LexicalEntry>>(((int) Math.ceil(requiredCapacity / 0.75)));
        EntryMapper entryMapper = new EntryMapper( this.lefffServiceInternal);
        WordMapper wordMapper = new WordMapper(this.lefffServiceInternal);
        CategoryMapper categoryMapper = new CategoryMapper(this.lefffServiceInternal);
        LemmaMapper lemmaMapper = new LemmaMapper(this.lefffServiceInternal);
        PredicateMapper predicateMapper = new PredicateMapper(this.lefffServiceInternal);
        AttributeMapper attributeMapper = new AttributeMapper(this.lefffServiceInternal);
        SqlRowSet rowSet = jt.queryForRowSet(sql, paramSource);
       
        Map<Integer,Category> categoryMap = new HashMap<Integer, Category>();
        Map<Integer,Predicate> predicateMap = new HashMap<Integer, Predicate>();
        Map<Integer,Attribute> attributeMap = new HashMap<Integer, Attribute>();
        Map<Integer,Lemma> lemmaMap = new HashMap<Integer, Lemma>();
       
        while (rowSet.next()) {
        	LefffEntryInternal entry =  entryMapper.mapRow(rowSet);
        	WordInternal word = wordMapper.mapRow(rowSet);
        	entry.setWord(word);
        	
        	int categoryId = rowSet.getInt("category_id");
        	Category category = categoryMap.get(categoryId);
        	if (category==null) {
        		category = categoryMapper.mapRow(rowSet);
        		categoryMap.put(categoryId, category);
        	}
        	entry.setCategory(category);
        	
        	
        	int predicateId = rowSet.getInt("predicate_id");
        	if (predicateId!=0) {
	        	Predicate predicate = predicateMap.get(predicateId);
	        	if (predicate==null) {
	        		predicate = predicateMapper.mapRow(rowSet);
	        		predicateMap.put(predicateId, predicate);
	        	}
	        	entry.setPredicate(predicate);
        	}
        	
        	int lemmaId = rowSet.getInt("lemma_id");
        	Lemma lemma = lemmaMap.get(lemmaId);
        	if (lemma==null) {
        		lemma = lemmaMapper.mapRow(rowSet);
        		lemmaMap.put(lemmaId, lemma);
        	}
        	entry.setLemma(lemma);
           	
        	int attributeId = rowSet.getInt("attribute_id");
        	Attribute attribute = attributeMap.get(attributeId);
        	if (attribute==null) {
        		attribute = attributeMapper.mapRow(rowSet);
        		attributeMap.put(attributeId, attribute);
        	}
        	entry.setMorphology(attribute);
        	
        	List<LexicalEntry> entries = entryMap.get(word.getText());
        	if (entries==null) {
        		entries = new ArrayList<LexicalEntry>();
        		entryMap.put(word.getText(), entries);
        	}
        	entries.add(entry);
        }
        
        for (String word : entryMap.keySet()) {
        	List<LexicalEntry> entries = entryMap.get(word);
        	ArrayList<LexicalEntry> entriesArrayList = (ArrayList<LexicalEntry>) entries;
        	entriesArrayList.trimToSize();
        }
        return entryMap;
    }
	
    @SuppressWarnings("unchecked")
    public static void LogParameters(MapSqlParameterSource paramSource) {
       DaoUtils.LogParameters(paramSource.getValues(), LogLevel.TRACE);
    }
}
