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

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;
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

import com.joliciel.talismane.utils.DaoUtils;

class TreebankDaoImpl implements TreebankDao {
	static final Log LOG = LogFactory.getLog(TreebankDaoImpl.class);
	TreebankServiceInternal treebankServiceInternal;
	private DataSource dataSource;

	private static final String SELECT_CATEGORY = "cat_id, cat_code, cat_description";
	private static final String SELECT_FUNCTION = "function_id, function_code, function_description";
	private static final String SELECT_MORPHOLOGY = "morph_id, morph_code, morph_description";
	private static final String SELECT_PHRASE_TYPE = "ptype_id, ptype_code, ptype_description";
	private static final String SELECT_SUB_CATEGORY = "subcat_id, subcat_cat_id, subcat_code, subcat_description";
	private static final String SELECT_WORD = "word_id, word_text, word_original_text";
	private static final String SELECT_TREEBANK_FILE = "file_id, file_name";
	private static final String SELECT_PHRASE = "phrase_id, phrase_ptype_id, phrase_parent_id, phrase_function_id, phrase_position, phrase_depth";
	private static final String SELECT_PHRASE_UNIT = "punit_id, punit_word_id, punit_phrase_id, punit_position, punit_lemma_id, punit_cat_id, punit_subcat_id" +
		", punit_morph_id, punit_compound, punit_pos_in_phrase, punit_compound_next, punit_guessed_postag_id";
	private static final String SELECT_PHRASE_SUBUNIT = "psubunit_id, psubunit_punit_id, psubunit_word_id, psubunit_position" +
		", psubunit_cat_id, psubunit_subcat_id, psubunit_morph_id";
	private static final String SELECT_SENTENCE = "sentence_id, sentence_number, sentence_file_id, sentence_text_id, sentence_text";
	private static final String SELECT_TEXT_ITEM = "text_id, text_file_id, text_external_id";
	
	public Category loadCategory(int categoryId) {
		NamedParameterJdbcTemplate jt = new NamedParameterJdbcTemplate(this.getDataSource());
		String sql = "SELECT " + SELECT_CATEGORY + " FROM ftb_category WHERE cat_id=:cat_id";
		MapSqlParameterSource paramSource = new MapSqlParameterSource();
		paramSource.addValue("cat_id", categoryId);

		LOG.info(sql);
		TreebankDaoImpl.LogParameters(paramSource);
		Category category = null;
		try {
			category = (Category)  jt.queryForObject(sql, paramSource, new CategoryMapper());
		} catch (EmptyResultDataAccessException ex) {
			ex.hashCode();
		}
		return category;
	}

	public Category loadCategory(String categoryCode) {
		NamedParameterJdbcTemplate jt = new NamedParameterJdbcTemplate(this.getDataSource());
		String sql = "SELECT " + SELECT_CATEGORY + " FROM ftb_category WHERE cat_code=:cat_code";
		MapSqlParameterSource paramSource = new MapSqlParameterSource();
		paramSource.addValue("cat_code", categoryCode);

		LOG.info(sql);
		TreebankDaoImpl.LogParameters(paramSource);
		Category category = null;
		try {
			category = (Category)  jt.queryForObject(sql, paramSource, new CategoryMapper());
		} catch (EmptyResultDataAccessException ex) {
			ex.hashCode();
		}
		return category;
	}


	public void saveCategory(CategoryInternal category) {
		NamedParameterJdbcTemplate jt = new NamedParameterJdbcTemplate(this.getDataSource());
		MapSqlParameterSource paramSource = new MapSqlParameterSource();
		paramSource.addValue("cat_code", category.getCode());
		paramSource.addValue("cat_description", category.getDescription());
		if (category.isNew()) {
			String sql = "SELECT nextval('ftb_category_cat_id_seq')";
			LOG.info(sql);
			int categoryId = jt.queryForInt(sql, paramSource);
			paramSource.addValue("cat_id", categoryId);

			sql = "INSERT INTO ftb_category (cat_id, cat_code, cat_description) VALUES (:cat_id, :cat_code, :cat_description)";

			LOG.info(sql);
			TreebankDaoImpl.LogParameters(paramSource);
			jt.update(sql, paramSource);
			category.setId(categoryId);
		} else {
			String sql = "UPDATE ftb_category" +
			" SET cat_code = :cat_code" +
			", cat_description = :cat_description" +
			" WHERE cat_id = :cat_id";

			paramSource.addValue("cat_id", category.getId());
			LOG.info(sql);
			TreebankDaoImpl.LogParameters(paramSource);
			jt.update(sql, paramSource);           
		}
	}

	public void deleteCategory(int categoryId) {
		NamedParameterJdbcTemplate jt = new NamedParameterJdbcTemplate(this.getDataSource());
		MapSqlParameterSource paramSource = new MapSqlParameterSource();
		String sql = "DELETE FROM ftb_category WHERE cat_id = :cat_id";
		paramSource.addValue("cat_id", categoryId);
		LOG.info(sql);
		TreebankDaoImpl.LogParameters(paramSource);
		jt.update(sql, paramSource);
	}

	protected static final class CategoryMapper implements RowMapper {

		public Object mapRow(ResultSet rs, int rowNum) throws SQLException {
			return this.mapRow(new ResultSetWrappingSqlRowSet(rs));
		}

		public Category mapRow(SqlRowSet rs) {
			CategoryInternal category = new CategoryImpl();
			category.setId(rs.getInt("cat_id"));
			category.setCode(rs.getString("cat_code"));
			category.setDescription(rs.getString("cat_description"));
			return category;
		}
	}
	
	public void savePhrase(PhraseInternal phrase) {
		NamedParameterJdbcTemplate jt = new NamedParameterJdbcTemplate(this.getDataSource());
		MapSqlParameterSource paramSource = new MapSqlParameterSource();
		paramSource.addValue("phrase_ptype_id", phrase.getPhraseTypeId()==0? null : phrase.getPhraseTypeId());
		paramSource.addValue("phrase_parent_id", phrase.getParent()==null? null : phrase.getParent().getId());
		paramSource.addValue("phrase_function_id", phrase.getFunctionId()==0? null : phrase.getFunctionId());
		paramSource.addValue("phrase_position", phrase.getPositionInPhrase());
		paramSource.addValue("phrase_depth", phrase.getDepth());

		if (phrase.isNew()) {
			String sql = "SELECT nextval('ftb_phrase_phrase_id_seq')";
			LOG.info(sql);
			int phraseId = jt.queryForInt(sql, paramSource);
			paramSource.addValue("phrase_id", phraseId);

			sql = "INSERT INTO ftb_phrase (phrase_id, phrase_ptype_id, phrase_parent_id, phrase_function_id, phrase_position, phrase_depth) " +
			"VALUES (:phrase_id, :phrase_ptype_id, :phrase_parent_id, :phrase_function_id, :phrase_position, :phrase_depth)";

			LOG.info(sql);
			TreebankDaoImpl.LogParameters(paramSource);
			jt.update(sql, paramSource);

			phrase.setId(phraseId);
		} 
	}

	public void savePhraseUnit(PhraseUnitInternal phraseUnit) {
		NamedParameterJdbcTemplate jt = new NamedParameterJdbcTemplate(this.getDataSource());
		MapSqlParameterSource paramSource = new MapSqlParameterSource();
		paramSource.addValue("punit_word_id", phraseUnit.getWordId()==0? null : phraseUnit.getWordId());
		paramSource.addValue("punit_phrase_id", phraseUnit.getPhraseId()==0? null : phraseUnit.getPhraseId());
		paramSource.addValue("punit_position", phraseUnit.getPositionInSentence());
		paramSource.addValue("punit_lemma_id", phraseUnit.getLemmaId()==0? null : phraseUnit.getLemmaId());
		paramSource.addValue("punit_cat_id", phraseUnit.getCategoryId()==0? null : phraseUnit.getCategoryId());
		paramSource.addValue("punit_subcat_id", phraseUnit.getSubCategoryId()==0? null : phraseUnit.getSubCategoryId());
		paramSource.addValue("punit_morph_id", phraseUnit.getMorphologyId()==0? null : phraseUnit.getMorphologyId());
		paramSource.addValue("punit_compound", phraseUnit.isCompound());
		paramSource.addValue("punit_pos_in_phrase", phraseUnit.getPositionInPhrase());
		paramSource.addValue("punit_compound_next", phraseUnit.getNextCompoundPartId()==0 ? null : phraseUnit.getNextCompoundPartId());
		paramSource.addValue("punit_guessed_postag_id", phraseUnit.getGuessedPosTagId()==0 ? null : phraseUnit.getGuessedPosTagId());
		if (phraseUnit.isNew()) {
			String sql = "SELECT nextval('ftb_phrase_unit_punit_id_seq')";
			LOG.info(sql);
			int phraseUnitId = jt.queryForInt(sql, paramSource);
			paramSource.addValue("punit_id", phraseUnitId);

			sql = "INSERT INTO ftb_phrase_unit (punit_id, punit_word_id, punit_phrase_id, punit_position, punit_lemma_id, punit_cat_id" +
			", punit_subcat_id, punit_morph_id, punit_compound, punit_pos_in_phrase, punit_compound_next, punit_guessed_postag_id) " +
			"VALUES (:punit_id, :punit_word_id, :punit_phrase_id, :punit_position, :punit_lemma_id, :punit_cat_id" +
			", :punit_subcat_id, :punit_morph_id, :punit_compound, :punit_pos_in_phrase, :punit_compound_next, :punit_guessed_postag_id)";

			LOG.info(sql);
			TreebankDaoImpl.LogParameters(paramSource);
			jt.update(sql, paramSource);

			phraseUnit.setId(phraseUnitId);
		} else {
			paramSource.addValue("punit_id", phraseUnit.getId());
			String sql = "UPDATE ftb_phrase_unit" +
			" SET punit_word_id = :punit_word_id" +
			", punit_phrase_id=:punit_phrase_id" +
			", punit_position=:punit_position" +
			", punit_lemma_id=:punit_lemma_id" +
			", punit_cat_id=:punit_cat_id" +
			", punit_subcat_id=:punit_subcat_id" +
			", punit_morph_id=:punit_morph_id" +
			", punit_compound=:punit_compound" +
			", punit_pos_in_phrase=:punit_pos_in_phrase" +
			", punit_compound_next=:punit_compound_next" +
			", punit_guessed_postag_id=:punit_guessed_postag_id" +
			" WHERE punit_id=:punit_id";
			LOG.info(sql);
			TreebankDaoImpl.LogParameters(paramSource);
			jt.update(sql, paramSource);
		}
	}

	public void savePhraseSubunit(PhraseSubunitInternal phraseSubunit) {
		NamedParameterJdbcTemplate jt = new NamedParameterJdbcTemplate(this.getDataSource());
		MapSqlParameterSource paramSource = new MapSqlParameterSource();
		paramSource.addValue("psubunit_punit_id", phraseSubunit.getPhraseUnit()==null? null : phraseSubunit.getPhraseUnit().getId());
		paramSource.addValue("psubunit_word_id", phraseSubunit.getWordId()==0? null : phraseSubunit.getWordId());
		paramSource.addValue("psubunit_position", phraseSubunit.getPosition());
		paramSource.addValue("psubunit_cat_id", phraseSubunit.getCategoryId()==0? null : phraseSubunit.getCategoryId());
		paramSource.addValue("psubunit_subcat_id", phraseSubunit.getSubCategoryId()==0? null : phraseSubunit.getSubCategoryId());
		paramSource.addValue("psubunit_morph_id", phraseSubunit.getMorphologyId()==0? null : phraseSubunit.getMorphologyId());

		if (phraseSubunit.isNew()) {
			String sql = "SELECT nextval('ftb_phrase_subunit_psubunit_id_seq')";
			LOG.info(sql);
			int phraseSubunitId = jt.queryForInt(sql, paramSource);
			paramSource.addValue("psubunit_id", phraseSubunitId);

			sql = "INSERT INTO ftb_phrase_subunit (psubunit_id, psubunit_punit_id, psubunit_word_id, psubunit_position" +
					", psubunit_cat_id, psubunit_subcat_id, psubunit_morph_id) " +
					"VALUES (:psubunit_id, :psubunit_punit_id, :psubunit_word_id, :psubunit_position" +
					", :psubunit_cat_id, :psubunit_subcat_id, :psubunit_morph_id)";

			LOG.info(sql);
			TreebankDaoImpl.LogParameters(paramSource);
			jt.update(sql, paramSource);

			phraseSubunit.setId(phraseSubunitId);
		} else {
			paramSource.addValue("psubunit_id", phraseSubunit.getId());
			String sql = "UPDATE ftb_phrase_subunit" +
					" SET psubunit_punit_id=:psubunit_punit_id" +
					", psubunit_word_id=:psubunit_word_id" +
					", psubunit_position=:psubunit_position" +
					", psubunit_cat_id=:psubunit_cat_id" +
					", psubunit_subcat_id=:psubunit_subcat_id" +
					", psubunit_morph_id=:psubunit_morph_id " +
					" WHERE psubunit_id = :psubunit_id";

			LOG.info(sql);
			TreebankDaoImpl.LogParameters(paramSource);
			jt.update(sql, paramSource);
		}
	}

	public void saveSentence(SentenceInternal sentence) {
		NamedParameterJdbcTemplate jt = new NamedParameterJdbcTemplate(this.getDataSource());
		MapSqlParameterSource paramSource = new MapSqlParameterSource();
		paramSource.addValue("sentence_number", sentence.getSentenceNumber());
		paramSource.addValue("sentence_file_id", sentence.getFileId()==0? null : sentence.getFileId());
		paramSource.addValue("sentence_text_id", sentence.getTextItemId()==0? null : sentence.getTextItemId());
		paramSource.addValue("sentence_id", sentence.getId());
		paramSource.addValue("sentence_text", sentence.getText());

		if (sentence.isNew()) {
			String sql = "INSERT INTO ftb_sentence (sentence_id, sentence_number, sentence_file_id, sentence_text_id, sentence_text) " +
			"VALUES (:sentence_id, :sentence_number, :sentence_file_id, :sentence_text_id, :sentence_text)";

			LOG.info(sql);
			TreebankDaoImpl.LogParameters(paramSource);
			jt.update(sql, paramSource);
		} else {
			String sql = "UPDATE ftb_sentence" +
					" SET sentence_number = :sentence_number" +
					", sentence_file_id = :sentence_file_id" +
					", sentence_text_id = :sentence_text_id" +
					", sentence_text = :sentence_text" +
					" WHERE sentence_id = :sentence_id";

			LOG.info(sql);
			TreebankDaoImpl.LogParameters(paramSource);
			jt.update(sql, paramSource);
		}
	}

	public void savePhraseDescendantMapping(Phrase parent,
			Phrase descendant) {
		NamedParameterJdbcTemplate jt = new NamedParameterJdbcTemplate(this.getDataSource());
		MapSqlParameterSource paramSource = new MapSqlParameterSource();
		paramSource.addValue("pchild_phrase_id", parent.getId());
		paramSource.addValue("pchild_child_id", descendant.getId());

		String sql = "INSERT INTO ftb_phrase_child (pchild_phrase_id, pchild_child_id) VALUES (:pchild_phrase_id, :pchild_child_id)";

		LOG.info(sql);
		TreebankDaoImpl.LogParameters(paramSource);
		jt.update(sql, paramSource);
	}

	public TreebankServiceInternal getTreebankServiceInternal() {
		return treebankServiceInternal;
	}

	public void setTreebankServiceInternal(
			TreebankServiceInternal treebankServiceInternal) {
		this.treebankServiceInternal = treebankServiceInternal;
	}

	public DataSource getDataSource() {
		return dataSource;
	}

	public void setDataSource(DataSource dataSource) {
		this.dataSource = dataSource;
	}


	public Function loadFunction(String functionCode) {
		NamedParameterJdbcTemplate jt = new NamedParameterJdbcTemplate(this.getDataSource());
		String sql = "SELECT " + SELECT_FUNCTION + " FROM ftb_function WHERE function_code=:function_code";
		MapSqlParameterSource paramSource = new MapSqlParameterSource();
		paramSource.addValue("function_code", functionCode);

		LOG.info(sql);
		TreebankDaoImpl.LogParameters(paramSource);
		Function function = null;
		try {
			function = (Function)  jt.queryForObject(sql, paramSource, new FunctionMapper());
		} catch (EmptyResultDataAccessException ex) {
			ex.hashCode();
		}
		return function;
	}

	protected static final class FunctionMapper implements RowMapper {

		public Object mapRow(ResultSet rs, int rowNum) throws SQLException {
			return this.mapRow(new ResultSetWrappingSqlRowSet(rs));
		}

		public Function mapRow(SqlRowSet rs) {
			FunctionInternal function = new FunctionImpl();
			function.setId(rs.getInt("function_id"));
			function.setCode(rs.getString("function_code"));
			function.setDescription(rs.getString("function_description"));
			return function;
		}
	}


	public Morphology loadMorphology(int morphologyId) {
		NamedParameterJdbcTemplate jt = new NamedParameterJdbcTemplate(this.getDataSource());
		String sql = "SELECT " + SELECT_MORPHOLOGY + " FROM ftb_morph WHERE morph_id=:morph_id";
		MapSqlParameterSource paramSource = new MapSqlParameterSource();
		paramSource.addValue("morph_id", morphologyId);

		LOG.info(sql);
		TreebankDaoImpl.LogParameters(paramSource);
		Morphology morphology = null;
		try {
			morphology = (Morphology)  jt.queryForObject(sql, paramSource, new MorphologyMapper());
		} catch (EmptyResultDataAccessException ex) {
			ex.hashCode();
		}
		return morphology;        
	}

	public Morphology loadMorphology(String morphologyCode) {
		NamedParameterJdbcTemplate jt = new NamedParameterJdbcTemplate(this.getDataSource());
		String sql = "SELECT " + SELECT_MORPHOLOGY + " FROM ftb_morph WHERE morph_code=:morph_code";
		MapSqlParameterSource paramSource = new MapSqlParameterSource();
		paramSource.addValue("morph_code", morphologyCode);

		LOG.info(sql);
		TreebankDaoImpl.LogParameters(paramSource);
		Morphology morphology = null;
		try {
			morphology = (Morphology)  jt.queryForObject(sql, paramSource, new MorphologyMapper());
		} catch (EmptyResultDataAccessException ex) {
			ex.hashCode();
		}
		return morphology;
	}


	protected static final class MorphologyMapper implements RowMapper {

		public Object mapRow(ResultSet rs, int rowNum) throws SQLException {
			return this.mapRow(new ResultSetWrappingSqlRowSet(rs));
		}

		public Morphology mapRow(SqlRowSet rs) {
			MorphologyInternal morphology = new MorphologyImpl();
			morphology.setId(rs.getInt("morph_id"));
			morphology.setCode(rs.getString("morph_code"));
			morphology.setDescription(rs.getString("morph_description"));
			return morphology;
		}
	}

	public PhraseType loadPhraseType(String phraseTypeCode) {
		NamedParameterJdbcTemplate jt = new NamedParameterJdbcTemplate(this.getDataSource());
		String sql = "SELECT " + SELECT_PHRASE_TYPE + " FROM ftb_phrase_type WHERE ptype_code=:ptype_code";
		MapSqlParameterSource paramSource = new MapSqlParameterSource();
		paramSource.addValue("ptype_code", phraseTypeCode);

		LOG.info(sql);
		TreebankDaoImpl.LogParameters(paramSource);
		PhraseType phraseType = null;
		try {
			phraseType = (PhraseType)  jt.queryForObject(sql, paramSource, new PhraseTypeMapper());
		} catch (EmptyResultDataAccessException ex) {
			ex.hashCode();
		}
		return phraseType;
	}

	
	public PhraseType loadPhraseType(int phraseTypeId) {
		NamedParameterJdbcTemplate jt = new NamedParameterJdbcTemplate(this.getDataSource());
		String sql = "SELECT " + SELECT_PHRASE_TYPE + " FROM ftb_phrase_type WHERE ptype_id=:ptype_id";
		MapSqlParameterSource paramSource = new MapSqlParameterSource();
		paramSource.addValue("ptype_id", phraseTypeId);

		LOG.info(sql);
		TreebankDaoImpl.LogParameters(paramSource);
		PhraseType phraseType = null;
		try {
			phraseType = (PhraseType)  jt.queryForObject(sql, paramSource, new PhraseTypeMapper());
		} catch (EmptyResultDataAccessException ex) {
			ex.hashCode();
		}
		return phraseType;
	}

	protected static final class PhraseTypeMapper implements RowMapper {

		public Object mapRow(ResultSet rs, int rowNum) throws SQLException {
			return this.mapRow(new ResultSetWrappingSqlRowSet(rs));
		}

		public PhraseType mapRow(SqlRowSet rs) {
			PhraseTypeInternal phraseType = new PhraseTypeImpl();
			phraseType.setId(rs.getInt("ptype_id"));
			phraseType.setCode(rs.getString("ptype_code"));
			phraseType.setDescription(rs.getString("ptype_description"));
			return phraseType;
		}
	}


	public SubCategory loadSubCategory(int subCategoryId) {
		NamedParameterJdbcTemplate jt = new NamedParameterJdbcTemplate(this.getDataSource());
		String sql = "SELECT " + SELECT_SUB_CATEGORY + " FROM ftb_sub_category WHERE subcat_id = :subcat_id";
		MapSqlParameterSource paramSource = new MapSqlParameterSource();
		paramSource.addValue("subcat_id", subCategoryId);

		LOG.info(sql);
		TreebankDaoImpl.LogParameters(paramSource);
		SubCategory subCategory = null;
		try {
			subCategory = (SubCategory)  jt.queryForObject(sql, paramSource, new SubCategoryMapper());
		} catch (EmptyResultDataAccessException ex) {
			ex.hashCode();
		}
		return subCategory;
	}

	public SubCategory loadSubCategory(Category category, String subCategoryCode) {
		NamedParameterJdbcTemplate jt = new NamedParameterJdbcTemplate(this.getDataSource());
		String sql = "SELECT " + SELECT_SUB_CATEGORY + " FROM ftb_sub_category WHERE subcat_cat_id = :subcat_cat_id AND subcat_code=:subcat_code";
		MapSqlParameterSource paramSource = new MapSqlParameterSource();
		paramSource.addValue("subcat_cat_id", category.getId());
		paramSource.addValue("subcat_code", subCategoryCode);

		LOG.info(sql);
		TreebankDaoImpl.LogParameters(paramSource);
		SubCategory subCategory = null;
		try {
			subCategory = (SubCategory)  jt.queryForObject(sql, paramSource, new SubCategoryMapper());
		} catch (EmptyResultDataAccessException ex) {
			ex.hashCode();
		}
		return subCategory;
	}

	protected static final class SubCategoryMapper implements RowMapper {

		public Object mapRow(ResultSet rs, int rowNum) throws SQLException {
			return this.mapRow(new ResultSetWrappingSqlRowSet(rs));
		}

		public SubCategory mapRow(SqlRowSet rs) {
			SubCategoryInternal subCategory = new SubCategoryImpl();
			subCategory.setId(rs.getInt("subcat_id"));
			subCategory.setCategoryId(rs.getInt("subcat_cat_id"));
			subCategory.setCode(rs.getString("subcat_code"));
			subCategory.setDescription(rs.getString("subcat_description"));
			return subCategory;
		}
	}

	public Word loadWord(String text, String originalText) {
		NamedParameterJdbcTemplate jt = new NamedParameterJdbcTemplate(this.getDataSource());
		MapSqlParameterSource paramSource = new MapSqlParameterSource();

		if (text==null)
			text = "";
		String sql = "SELECT " + SELECT_WORD + " FROM ftb_word" +
		" WHERE word_text=:word_text" +
		" AND word_original_text=:word_original_text";
		paramSource.addValue("word_text", text);
		paramSource.addValue("word_original_text", originalText);


		LOG.info(sql);
		TreebankDaoImpl.LogParameters(paramSource);
		Word word = null;
		try {
			word = (Word)  jt.queryForObject(sql, paramSource, new WordMapper( this.treebankServiceInternal));
		} catch (EmptyResultDataAccessException ex) {
			ex.hashCode();
		}
		return word;
	}

	public Word loadWord(int wordId) {
		NamedParameterJdbcTemplate jt = new NamedParameterJdbcTemplate(this.getDataSource());
		String sql = "SELECT " + SELECT_WORD + " FROM ftb_word WHERE word_id=:word_id";
		MapSqlParameterSource paramSource = new MapSqlParameterSource();
		paramSource.addValue("word_id", wordId);

		LOG.info(sql);
		TreebankDaoImpl.LogParameters(paramSource);
		Word word = null;
		try {
			word = (Word)  jt.queryForObject(sql, paramSource, new WordMapper( this.treebankServiceInternal));
		} catch (EmptyResultDataAccessException ex) {
			ex.hashCode();
		}
		return word;
	}



	public List<Word> findWords(Phrase phrase) {
		NamedParameterJdbcTemplate jt = new NamedParameterJdbcTemplate(this.getDataSource());
		String sql = "SELECT " + SELECT_WORD + " FROM ftb_word w, ftb_phrase_unit pu, ftb_phrase_child pc" +
		" WHERE word_id=punit_word_id AND punit_phrase_id = pchild_child_id AND pchild_phrase_id = :pchild_phrase_id" +
		" ORDER BY punit_position";
		MapSqlParameterSource paramSource = new MapSqlParameterSource();
		paramSource.addValue("pchild_phrase_id", phrase.getId());

		LOG.info(sql);
		TreebankDaoImpl.LogParameters(paramSource);
		@SuppressWarnings("unchecked")
		List<Word> words = jt.query(sql, paramSource, new WordMapper(this.treebankServiceInternal));

		return words;
	}

	public List<Word> findWords(String text) {
		NamedParameterJdbcTemplate jt = new NamedParameterJdbcTemplate(this.getDataSource());
		String sql = "SELECT " + SELECT_WORD + " FROM ftb_word w" +
		" WHERE word_text = :word_text" +
		" ORDER BY word_id";
		MapSqlParameterSource paramSource = new MapSqlParameterSource();
		paramSource.addValue("word_text", text);

		LOG.info(sql);
		TreebankDaoImpl.LogParameters(paramSource);
		@SuppressWarnings("unchecked")
		List<Word> words = jt.query(sql, paramSource, new WordMapper(this.treebankServiceInternal));

		return words;
	}

	public List<PhraseUnit> findAllPhraseUnits(Phrase phrase) {
		NamedParameterJdbcTemplate jt = new NamedParameterJdbcTemplate(this.getDataSource());
		String sql = "SELECT " + SELECT_PHRASE_UNIT + "," + SELECT_PHRASE_SUBUNIT +
			" FROM ftb_phrase_unit pu" +
			" LEFT JOIN ftb_phrase_subunit psu ON pu.punit_id = psu.psubunit_punit_id" +
			" INNER JOIN ftb_phrase_child pc ON punit_phrase_id = pchild_child_id AND pchild_phrase_id = :pchild_phrase_id" +
			" ORDER BY punit_position, psubunit_position";
		MapSqlParameterSource paramSource = new MapSqlParameterSource();
		paramSource.addValue("pchild_phrase_id", phrase.getId());

		LOG.info(sql);
		TreebankDaoImpl.LogParameters(paramSource);
		SqlRowSet rowSet = jt.queryForRowSet(sql, paramSource);
		PhraseUnitMapper phraseUnitMapper = new PhraseUnitMapper(this.treebankServiceInternal);
		PhraseSubUnitMapper phraseSubUnitMapper = new PhraseSubUnitMapper(this.treebankServiceInternal);
		
		List<PhraseUnit> phraseUnits = new ArrayList<PhraseUnit>();
		int currentPunitId = 0;
		PhraseUnitInternal currentPhraseUnit = null;
		while (rowSet.next()) {
			int phraseUnitId = rowSet.getInt("punit_id");
			if (phraseUnitId!=currentPunitId) {
				currentPhraseUnit = phraseUnitMapper.mapRow(rowSet);
				currentPhraseUnit.setSubunitsInternal(new ArrayList<PhraseSubunit>());
				phraseUnits.add(currentPhraseUnit);
				currentPunitId = phraseUnitId;
			}

			int phraseSubunitId = rowSet.getInt("psubunit_id");
			if (phraseSubunitId!=0) {
				PhraseSubunit psubunit = phraseSubUnitMapper.mapRow(rowSet);
				currentPhraseUnit.getSubunitsInternal().add(psubunit);
			}
		}
		return phraseUnits;        
	}
	
	public List<PhraseUnitInternal> findPhraseUnits(Phrase phrase) {
		NamedParameterJdbcTemplate jt = new NamedParameterJdbcTemplate(this.getDataSource());
		String sql = "SELECT " + SELECT_PHRASE_UNIT + "," + SELECT_PHRASE_SUBUNIT +
			" FROM ftb_phrase_unit pu" +
			" LEFT JOIN ftb_phrase_subunit psu ON pu.punit_id = psu.psubunit_punit_id" +
			" WHERE punit_phrase_id = :phrase_id" +
			" ORDER BY punit_position, psubunit_position";
		MapSqlParameterSource paramSource = new MapSqlParameterSource();
		paramSource.addValue("phrase_id", phrase.getId());

		LOG.info(sql);
		TreebankDaoImpl.LogParameters(paramSource);
		SqlRowSet rowSet = jt.queryForRowSet(sql, paramSource);
		PhraseUnitMapper phraseUnitMapper = new PhraseUnitMapper(this.treebankServiceInternal);
		PhraseSubUnitMapper phraseSubUnitMapper = new PhraseSubUnitMapper(this.treebankServiceInternal);
		
		List<PhraseUnitInternal> phraseUnits = new ArrayList<PhraseUnitInternal>();
		int currentPunitId = 0;
		PhraseUnitInternal currentPhraseUnit = null;
		while (rowSet.next()) {
			int phraseUnitId = rowSet.getInt("punit_id");
			if (phraseUnitId!=currentPunitId) {
				currentPhraseUnit = phraseUnitMapper.mapRow(rowSet);
				currentPhraseUnit.setSubunitsInternal(new ArrayList<PhraseSubunit>());
				phraseUnits.add(currentPhraseUnit);
				currentPunitId = phraseUnitId;
			}

			int phraseSubunitId = rowSet.getInt("psubunit_id");
			if (phraseSubunitId!=0) {
				PhraseSubunit psubunit = phraseSubUnitMapper.mapRow(rowSet);
				currentPhraseUnit.getSubunitsInternal().add(psubunit);
			}
		}
		return phraseUnits;        
	}

	@Override
	public void findAllWordsAndLemmas(Phrase phrase, List<? extends PhraseUnit> phraseUnits) {
		NamedParameterJdbcTemplate jt = new NamedParameterJdbcTemplate(this.getDataSource());
		String sql = "SELECT punit_id, w.word_id as w_word_id, w.word_text as w_word_text, w.word_original_text as w_word_original_text" +
		", l.word_id as l_word_id, l.word_text as l_word_text, l.word_original_text as l_word_original_text" +
		" FROM ftb_phrase_unit pu" +
		" INNER JOIN ftb_phrase_child pc ON punit_phrase_id = pchild_child_id AND pchild_phrase_id = :pchild_phrase_id" +
		" INNER JOIN ftb_word w ON punit_word_id = w.word_id" +
		" INNER JOIN ftb_word l ON punit_lemma_id = l.word_id" +
		" ORDER BY punit_position";
		MapSqlParameterSource paramSource = new MapSqlParameterSource();
		paramSource.addValue("pchild_phrase_id", phrase.getId());

		LOG.info(sql);
		TreebankDaoImpl.LogParameters(paramSource);
		SqlRowSet rowSet = jt.queryForRowSet(sql, paramSource);
		WordMapper wordMapper = new WordMapper("w", this.treebankServiceInternal);
		WordMapper lemmaMapper = new WordMapper("l", this.treebankServiceInternal);
		while (rowSet.next()) {
			int phraseUnitId = rowSet.getInt("punit_id");
			Word word = wordMapper.mapRow(rowSet);
			Word lemma = lemmaMapper.mapRow(rowSet);
			for (PhraseUnit phraseUnit : phraseUnits) {
				if (phraseUnit.getId()==phraseUnitId) {
					PhraseUnitInternal iPhraseUnit = (PhraseUnitInternal) phraseUnit;
					iPhraseUnit.setWord(word);
					iPhraseUnit.setLemma(lemma);
					break;
				}
			}
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<PhraseSubunit> findPhraseSubunits(PhraseUnit phraseUnit) {
		NamedParameterJdbcTemplate jt = new NamedParameterJdbcTemplate(this.getDataSource());
		String sql = "SELECT " + SELECT_PHRASE_SUBUNIT + " FROM ftb_phrase_subunit" +
		" WHERE psubunit_punit_id = :psubunit_punit_id" +
		" ORDER BY psubunit_position";
		MapSqlParameterSource paramSource = new MapSqlParameterSource();
		paramSource.addValue("psubunit_punit_id", phraseUnit.getId());

		LOG.info(sql);
		TreebankDaoImpl.LogParameters(paramSource);
		List<PhraseSubunit> phraseSubunits = jt.query(sql, paramSource, new PhraseSubUnitMapper(this.treebankServiceInternal));

		return phraseSubunits;     
	}

	public PhraseUnit loadPhraseUnit(int phraseUnitId) {
		NamedParameterJdbcTemplate jt = new NamedParameterJdbcTemplate(this.getDataSource());
		String sql = "SELECT " + SELECT_PHRASE_UNIT + " FROM ftb_phrase_unit WHERE punit_id=:punit_id";
		MapSqlParameterSource paramSource = new MapSqlParameterSource();
		paramSource.addValue("punit_id", phraseUnitId);

		LOG.info(sql);
		TreebankDaoImpl.LogParameters(paramSource);
		PhraseUnit phraseUnit = null;
		try {
			phraseUnit = (PhraseUnit)  jt.queryForObject(sql, paramSource, new PhraseUnitMapper(this.treebankServiceInternal));
		} catch (EmptyResultDataAccessException ex) {
			ex.hashCode();
		}
		return phraseUnit;
	}

	protected static final class WordMapper implements RowMapper {
		private String alias = "";
		private TreebankServiceInternal treebankService;
		public WordMapper(TreebankServiceInternal treebankService) {
			this.treebankService = treebankService;
		};
		public WordMapper(String alias, TreebankServiceInternal treebankService) {
			this.treebankService = treebankService;
			this.alias = alias + "_"; 
		}
		public Object mapRow(ResultSet rs, int rowNum) throws SQLException {
			return this.mapRow(new ResultSetWrappingSqlRowSet(rs));
		}

		public Word mapRow(SqlRowSet rs) {
			WordInternal word = treebankService.newWord();
			word.setId(rs.getInt(alias + "word_id"));
			word.setText(rs.getString(alias + "word_text"));
			word.setOriginalText(rs.getString(alias + "word_original_text"));
			return word;
		}
	}

	public TreebankFile loadTreebankFile(String fileName) {
		NamedParameterJdbcTemplate jt = new NamedParameterJdbcTemplate(this.getDataSource());
		String sql = "SELECT " + SELECT_TREEBANK_FILE + " FROM ftb_file WHERE file_name=:file_name";
		MapSqlParameterSource paramSource = new MapSqlParameterSource();
		paramSource.addValue("file_name", fileName);

		LOG.info(sql);
		TreebankDaoImpl.LogParameters(paramSource);
		TreebankFile treebankFile = null;
		try {
			treebankFile = (TreebankFile)  jt.queryForObject(sql, paramSource, new TreebankFileMapper(treebankServiceInternal));
		} catch (EmptyResultDataAccessException ex) {
			ex.hashCode();
		}
		return treebankFile;
	}
	
	@Override
	public List<TreebankFile> findTreebankFiles() {
		NamedParameterJdbcTemplate jt = new NamedParameterJdbcTemplate(this.getDataSource());
		String sql = "SELECT " + SELECT_TREEBANK_FILE + " FROM ftb_file" +
			" ORDER BY file_name";
		MapSqlParameterSource paramSource = new MapSqlParameterSource();

		LOG.info(sql);
		TreebankDaoImpl.LogParameters(paramSource);
		@SuppressWarnings("unchecked")
		List<TreebankFile> files = jt.query(sql, paramSource, new TreebankFileMapper(treebankServiceInternal));

		return files;
	}

	public TreebankFile loadTreebankFile(int fileId) {
		NamedParameterJdbcTemplate jt = new NamedParameterJdbcTemplate(this.getDataSource());
		String sql = "SELECT " + SELECT_TREEBANK_FILE + " FROM ftb_file WHERE file_id=:file_id";
		MapSqlParameterSource paramSource = new MapSqlParameterSource();
		paramSource.addValue("file_id", fileId);

		LOG.info(sql);
		TreebankDaoImpl.LogParameters(paramSource);
		TreebankFile treebankFile = null;
		try {
			treebankFile = (TreebankFile)  jt.queryForObject(sql, paramSource, new TreebankFileMapper(treebankServiceInternal));
		} catch (EmptyResultDataAccessException ex) {
			ex.hashCode();
		}
		return treebankFile;
	}

	protected static final class TreebankFileMapper implements RowMapper {
		private TreebankServiceInternal treebankServiceInternal;
		
		public TreebankFileMapper(
				TreebankServiceInternal treebankServiceInternal) {
			super();
			this.treebankServiceInternal = treebankServiceInternal;
		}

		public Object mapRow(ResultSet rs, int rowNum) throws SQLException {
			return this.mapRow(new ResultSetWrappingSqlRowSet(rs));
		}

		public TreebankFile mapRow(SqlRowSet rs) {
			TreebankFileInternal treebankFile = this.treebankServiceInternal.newTreebankFile();
			treebankFile.setId(rs.getInt("file_id"));
			treebankFile.setFileName(rs.getString("file_name"));
			return treebankFile;
		}
	}
	public void saveFunction(FunctionInternal function) {
		NamedParameterJdbcTemplate jt = new NamedParameterJdbcTemplate(this.getDataSource());
		MapSqlParameterSource paramSource = new MapSqlParameterSource();
		paramSource.addValue("function_code", function.getCode());
		paramSource.addValue("function_description", function.getDescription());
		if (function.isNew()) {
			String sql = "SELECT nextval('ftb_function_function_id_seq')";
			LOG.info(sql);
			int functionId = jt.queryForInt(sql, paramSource);
			paramSource.addValue("function_id", functionId);

			sql = "INSERT INTO ftb_function (function_id, function_code, function_description) VALUES (:function_id, :function_code, :function_description)";

			LOG.info(sql);
			TreebankDaoImpl.LogParameters(paramSource);
			jt.update(sql, paramSource);

			function.setId(functionId);
		} else {
			String sql = "UPDATE ftb_function" +
			" SET function_code = :function_code" +
			", function_description = :function_description" +
			" WHERE function_id = :function_id";

			paramSource.addValue("function_id", function.getId());
			LOG.info(sql);
			TreebankDaoImpl.LogParameters(paramSource);
			jt.update(sql, paramSource);           
		}

	}


	public void saveMorphology(MorphologyInternal morphology) {
		NamedParameterJdbcTemplate jt = new NamedParameterJdbcTemplate(this.getDataSource());
		MapSqlParameterSource paramSource = new MapSqlParameterSource();
		paramSource.addValue("morph_code", morphology.getCode());
		paramSource.addValue("morph_description", morphology.getDescription());
		if (morphology.isNew()) {
			String sql = "SELECT nextval('ftb_morph_morph_id_seq')";
			LOG.info(sql);
			int morphologyId = jt.queryForInt(sql, paramSource);
			paramSource.addValue("morph_id", morphologyId);

			sql = "INSERT INTO ftb_morph (morph_id, morph_code, morph_description) VALUES (:morph_id, :morph_code, :morph_description)";

			LOG.info(sql);
			TreebankDaoImpl.LogParameters(paramSource);
			jt.update(sql, paramSource);

			morphology.setId(morphologyId);
		} else {
			String sql = "UPDATE ftb_morph" +
			" SET morph_code = :morph_code" +
			", morph_description = :morph_description" +
			" WHERE morph_id = :morph_id";

			paramSource.addValue("morph_id", morphology.getId());
			LOG.info(sql);
			TreebankDaoImpl.LogParameters(paramSource);
			jt.update(sql, paramSource);           
		}

	}


	public void savePhraseType(PhraseTypeInternal phraseType) {
		NamedParameterJdbcTemplate jt = new NamedParameterJdbcTemplate(this.getDataSource());
		MapSqlParameterSource paramSource = new MapSqlParameterSource();
		paramSource.addValue("ptype_code", phraseType.getCode());
		paramSource.addValue("ptype_description", phraseType.getDescription());
		if (phraseType.isNew()) {
			String sql = "SELECT nextval('ftb_phrase_type_ptype_id_seq')";
			LOG.info(sql);
			int phraseTypeId = jt.queryForInt(sql, paramSource);
			paramSource.addValue("ptype_id", phraseTypeId);

			sql = "INSERT INTO ftb_phrase_type (ptype_id, ptype_code, ptype_description) VALUES (:ptype_id, :ptype_code, :ptype_description)";

			LOG.info(sql);
			TreebankDaoImpl.LogParameters(paramSource);
			jt.update(sql, paramSource);

			phraseType.setId(phraseTypeId);
		} else {
			String sql = "UPDATE ftb_phrase_type" +
			" SET ptype_code = :ptype_code" +
			", ptype_description = :ptype_description" +
			" WHERE ptype_id = :ptype_id";

			paramSource.addValue("ptype_id", phraseType.getId());
			LOG.info(sql);
			TreebankDaoImpl.LogParameters(paramSource);
			jt.update(sql, paramSource);           
		}
	}


	public void saveSubCategory(SubCategoryInternal subCategory) {
		NamedParameterJdbcTemplate jt = new NamedParameterJdbcTemplate(this.getDataSource());
		MapSqlParameterSource paramSource = new MapSqlParameterSource();
		paramSource.addValue("subcat_code", subCategory.getCode());
		paramSource.addValue("subcat_cat_id", subCategory.getCategoryId());
		paramSource.addValue("subcat_description", subCategory.getDescription());
		if (subCategory.isNew()) {
			String sql = "SELECT nextval('ftb_sub_category_subcat_id_seq')";
			LOG.info(sql);
			int subCategoryId = jt.queryForInt(sql, paramSource);
			paramSource.addValue("subcat_id", subCategoryId);

			sql = "INSERT INTO ftb_sub_category (subcat_id, subcat_code, subcat_cat_id, subcat_description) VALUES (:subcat_id, :subcat_code, :subcat_cat_id, :subcat_description)";

			LOG.info(sql);
			TreebankDaoImpl.LogParameters(paramSource);
			jt.update(sql, paramSource);

			subCategory.setId(subCategoryId);
		} else {
			String sql = "UPDATE ftb_sub_subCategory" +
			" SET subcat_code = :subcat_code" +
			", subcat_cat_id = :subcat_cat_id" +
			", subcat_description = :subcat_description" +
			" WHERE subcat_id = :subcat_id";

			paramSource.addValue("subcat_id", subCategory.getId());
			LOG.info(sql);
			TreebankDaoImpl.LogParameters(paramSource);
			jt.update(sql, paramSource);           
		}
	}


	public void saveWord(WordInternal word) {
		NamedParameterJdbcTemplate jt = new NamedParameterJdbcTemplate(this.getDataSource());
		MapSqlParameterSource paramSource = new MapSqlParameterSource();
		paramSource.addValue("word_text", word.getText());
		paramSource.addValue("word_original_text", word.getOriginalText());
		if (word.isNew()) {
			String sql = "SELECT nextval('ftb_word_word_id_seq')";
			LOG.info(sql);
			int wordId = jt.queryForInt(sql, paramSource);
			paramSource.addValue("word_id", wordId);

			sql = "INSERT INTO ftb_word (word_id, word_text, word_original_text) VALUES (:word_id, :word_text, :word_original_text)";

			LOG.info(sql);
			TreebankDaoImpl.LogParameters(paramSource);
			jt.update(sql, paramSource);

			word.setId(wordId);
		} else {
			String sql = "UPDATE ftb_word" +
			" SET word_text = :word_text" +
			", word_original_text = :word_original_text" +
			" WHERE word_id = :word_id";

			paramSource.addValue("word_id", word.getId());
			LOG.info(sql);
			TreebankDaoImpl.LogParameters(paramSource);
			jt.update(sql, paramSource);           
		}
	}



	public void saveTreebankFile(TreebankFileInternal treebankFile) {
		NamedParameterJdbcTemplate jt = new NamedParameterJdbcTemplate(this.getDataSource());
		MapSqlParameterSource paramSource = new MapSqlParameterSource();
		paramSource.addValue("file_name", treebankFile.getFileName());

		if (treebankFile.isNew()) {
			String sql = "SELECT nextval('ftb_file_file_id_seq')";
			LOG.info(sql);
			int fileId = jt.queryForInt(sql, paramSource);
			paramSource.addValue("file_id", fileId);

			sql = "INSERT INTO ftb_file (file_id, file_name) " +
			"VALUES (:file_id, :file_name)";

			LOG.info(sql);
			TreebankDaoImpl.LogParameters(paramSource);
			jt.update(sql, paramSource);

			treebankFile.setId(fileId);
		} 
	}

	public List<List<Entity>> findStuff(List<String> tablesToReturn, List<String>tables, List<String> conditions, List<String> orderBy) {
		NamedParameterJdbcTemplate jt = new NamedParameterJdbcTemplate(this.getDataSource());
		MapSqlParameterSource paramSource = new MapSqlParameterSource();

		List<String> tableNames = new ArrayList<String>();
		List<String> aliases = new ArrayList<String>();

		for (String tableToReturn : tablesToReturn) {
			StringTokenizer st = new StringTokenizer(tableToReturn, " ", false);
			String tableName = st.nextToken().trim();
			st.nextElement(); // skip the word "as"
			String alias = st.nextToken().trim();
			tableNames.add(tableName);
			aliases.add(alias);
		}

		String sql = "SELECT DISTINCT ";
		boolean firstOne = true;
		int i = 0;
		for (String tableName : tableNames) {
			String alias = aliases.get(i++);
			List<String> columns = null;
			if (tableName.equals("ftb_phrase"))
				columns = DaoUtils.getSelectArray(SELECT_PHRASE, alias);
			else if (tableName.equals("ftb_phrase_unit"))
				columns = DaoUtils.getSelectArray(SELECT_PHRASE_UNIT, alias);
			else if (tableName.equals("ftb_word"))
				columns = DaoUtils.getSelectArray(SELECT_WORD, alias);
			else if (tableName.equals("ftb_sentence"))
				columns = DaoUtils.getSelectArray(SELECT_SENTENCE, alias);
			else
				throw new TreebankException("Unsupported  table for findStuff: " + tableName);

			for (String column : columns) {
				if (firstOne) {
					sql += column;
					firstOne = false;
				} else
					sql += ", " + column;
			}
		}

		firstOne = true;
		for (String table : tables) {
			if (firstOne) {
				sql += " FROM " + table;
				firstOne = false;
			} else
				sql += ", " + table;
		}
		firstOne = true;
		for (String condition : conditions) {
			if (firstOne) {
				sql += " WHERE " + condition;
				firstOne = false;
			} else {
				sql += " AND " + condition;
			}
		}

		if (orderBy.size()>0) {
			firstOne = true;
			for (String column : orderBy) {
				if (firstOne) {
					sql += " ORDER BY " + column;
					firstOne = false;
				} else {
					sql += ", " + column;
				}
			}
		}
		LOG.info(sql);

		SqlRowSet rowSet = jt.queryForRowSet(sql, paramSource);
		List<List<Entity>> stuff = new ArrayList<List<Entity>>();
		while (rowSet.next()) {
			List<Entity> oneRow = new ArrayList<Entity>();
			i = 0;
			for (String tableName : tableNames) {
				String alias = aliases.get(i++);
				Entity entity = null;
				if (tableName.equals("ftb_phrase")) {
					PhraseMapper phraseMapper = new PhraseMapper(alias, this.treebankServiceInternal);
					Phrase phrase = phraseMapper.mapRow(rowSet);
					entity = phrase;
				} else if (tableName.equals("ftb_phrase_unit")) {
					PhraseUnitMapper phraseUnitMapper = new PhraseUnitMapper(alias, this.treebankServiceInternal);
					PhraseUnit phraseUnit = phraseUnitMapper.mapRow(rowSet);
					entity = phraseUnit;
				} else if (tableName.equals("ftb_word")) {
					WordMapper wordMapper = new WordMapper(alias, this.treebankServiceInternal);
					Word  word = wordMapper.mapRow(rowSet);
					entity = word;
				}
				oneRow.add(entity);
			}
			i = 0;
			for (String tableName : tableNames) {
				String alias = aliases.get(i++);
				if (tableName.equals("ftb_sentence")) {
					// need to replace the phrase already created with a sentence
					int sentenceId = rowSet.getInt(alias + "_sentence_id");
					PhraseInternal sentencePhrase = null;
					for (Entity entity : oneRow) {
						if (entity instanceof PhraseInternal) {
							if (entity.getId()==sentenceId) {
								sentencePhrase = (PhraseInternal) entity;
								break;
							}
						}
					}
					if (sentencePhrase==null)
						throw new TreebankException("Cannot return ftb_sentence without associated ftb_phrase");
					SentenceMapper sentenceMapper = new SentenceMapper(alias, this.treebankServiceInternal, sentencePhrase);
					Sentence  sentence = sentenceMapper.mapRow(rowSet);
					oneRow.remove(sentencePhrase);
					oneRow.add(sentence);
				}
			}            
			stuff.add(oneRow);
		}
		return stuff;
	}

	protected static final class PhraseUnitMapper implements RowMapper {
		private String alias = "";
		private TreebankServiceInternal treebankService;
		public PhraseUnitMapper(TreebankServiceInternal treebankService) { this.treebankService = treebankService; };
		public PhraseUnitMapper(String alias, TreebankServiceInternal treebankService) {
			this.treebankService = treebankService;
			this.alias = alias + "_"; 
		}
		public Object mapRow(ResultSet rs, int rowNum) throws SQLException {
			return this.mapRow(new ResultSetWrappingSqlRowSet(rs));
		}

		public PhraseUnitInternal mapRow(SqlRowSet rs) {
			PhraseUnitInternal phraseUnit =  this.treebankService.newPhraseUnit();
			//punit_id, punit_word_id, punit_phrase_id, punit_position, punit_lemma_id, punit_cat_id, punit_subcat_id
			// punit_morph_id, punit_compound, punit_sentence_id, punit_pos_in_phrase
			phraseUnit.setId(rs.getInt(alias + "punit_id"));
			phraseUnit.setWordId(rs.getInt(alias + "punit_word_id"));
			phraseUnit.setPhraseId(rs.getInt(alias + "punit_phrase_id"));
			phraseUnit.setPositionInSentence(rs.getInt(alias + "punit_position"));
			phraseUnit.setLemmaId(rs.getInt(alias + "punit_lemma_id"));
			phraseUnit.setCategoryId(rs.getInt(alias + "punit_cat_id"));
			phraseUnit.setSubCategoryId(rs.getInt(alias + "punit_subcat_id"));
			phraseUnit.setMorphologyId(rs.getInt(alias + "punit_morph_id"));
			phraseUnit.setCompound(rs.getBoolean(alias + "punit_compound"));
			phraseUnit.setPositionInPhrase(rs.getInt(alias + "punit_pos_in_phrase"));
			phraseUnit.setNextCompoundPartId(rs.getInt(alias + "punit_compound_next"));
			phraseUnit.setGuessedPosTagId(rs.getInt(alias + "punit_guessed_postag_id"));

			phraseUnit.setDirty(false);
			return phraseUnit;
		}
	}

	protected static final class PhraseSubUnitMapper implements RowMapper {
		private String alias = "";
		private TreebankServiceInternal treebankService;
		public PhraseSubUnitMapper(TreebankServiceInternal treebankService) { this.treebankService = treebankService; };
		public PhraseSubUnitMapper(String alias, TreebankServiceInternal treebankService) {
			this.treebankService = treebankService;
			this.alias = alias + "_"; 
		}
		public Object mapRow(ResultSet rs, int rowNum) throws SQLException {
			return this.mapRow(new ResultSetWrappingSqlRowSet(rs));
		}

		public PhraseSubunit mapRow(SqlRowSet rs) {
			PhraseSubunitInternal phraseSubunit =  this.treebankService.newPhraseSubunit();
			//psubunit_id, psubunit_punit_id, psubunit_word_id, psubunit_position, psubunit_cat_id
			phraseSubunit.setId(rs.getInt(alias + "psubunit_id"));
			phraseSubunit.setPhraseUnitId(rs.getInt(alias + "psubunit_punit_id"));
			phraseSubunit.setWordId(rs.getInt(alias + "psubunit_word_id"));
			phraseSubunit.setPosition(rs.getInt(alias + "psubunit_position"));
			phraseSubunit.setCategoryId(rs.getInt(alias + "psubunit_cat_id"));
			phraseSubunit.setSubCategoryId(rs.getInt(alias + "psubunit_subcat_id"));
			phraseSubunit.setMorphologyId(rs.getInt(alias + "psubunit_morph_id"));
			
			phraseSubunit.setDirty(false);
			return phraseSubunit;
		}
	}

	@Override
	public List<PhraseInternal> findChildren(Phrase phrase) {
		NamedParameterJdbcTemplate jt = new NamedParameterJdbcTemplate(this.getDataSource());
		String sql = "SELECT " + SELECT_PHRASE + " FROM ftb_phrase" +
		" WHERE phrase_parent_id = :phrase_id" +
		" ORDER BY phrase_position";
		MapSqlParameterSource paramSource = new MapSqlParameterSource();
		paramSource.addValue("phrase_id", phrase.getId());

		LOG.info(sql);
		TreebankDaoImpl.LogParameters(paramSource);
		@SuppressWarnings("unchecked")
		List<PhraseInternal> children = jt.query(sql, paramSource, new PhraseMapper(this.treebankServiceInternal));

		return children;     
	}
	
	
	
	@Override
	public Phrase loadPhrase(int phraseId) {
		NamedParameterJdbcTemplate jt = new NamedParameterJdbcTemplate(this.getDataSource());
		String sql = "SELECT " + SELECT_PHRASE + " FROM ftb_phrase WHERE phrase_id=:phrase_id";
		MapSqlParameterSource paramSource = new MapSqlParameterSource();
		paramSource.addValue("phrase_id", phraseId);

		LOG.info(sql);
		TreebankDaoImpl.LogParameters(paramSource);
		Phrase phrase = null;
		try {
			phrase = (Phrase)  jt.queryForObject(sql, paramSource, new PhraseMapper(this.treebankServiceInternal));
		} catch (EmptyResultDataAccessException ex) {
			ex.hashCode();
		}
		return phrase;
	}

	protected static final class PhraseMapper implements RowMapper {
		private String alias = "";
		private TreebankServiceInternal treebankService;
		public PhraseMapper(TreebankServiceInternal treebankService) { this.treebankService = treebankService; };
		public PhraseMapper(String alias, TreebankServiceInternal treebankService) {
			this.treebankService = treebankService;
			this.alias = alias + "_"; 
		}

		public Object mapRow(ResultSet rs, int rowNum) throws SQLException {
			return this.mapRow(new ResultSetWrappingSqlRowSet(rs));
		}

		public Phrase mapRow(SqlRowSet rs) {
			PhraseInternal phrase = treebankService.newPhrase();
			// phrase_id, phrase_ptype_id, phrase_parent_id, phrase_function_id, phrase_position, phrase_depth
			phrase.setId(rs.getInt(alias + "phrase_id"));
			phrase.setPhraseTypeId(rs.getInt(alias + "phrase_ptype_id"));
			phrase.setParentId(rs.getInt(alias + "phrase_parent_id"));
			phrase.setFunctionId(rs.getInt(alias + "phrase_function_id"));
			phrase.setPositionInPhrase(rs.getInt(alias + "phrase_position"));
			phrase.setDepth(rs.getInt(alias + "phrase_depth"));
			
			phrase.setDirty(false);

			return phrase;
		}
	}

	public Sentence loadFullSentence(int sentenceId) {
		PhraseMapper phraseMapper = new PhraseMapper(this.treebankServiceInternal);

		NamedParameterJdbcTemplate jt = new NamedParameterJdbcTemplate(this.getDataSource());
		String sql = "SELECT " + SELECT_SENTENCE + ", " + SELECT_PHRASE + " FROM ftb_phrase p, ftb_sentence s" +
			" WHERE sentence_id=:sentence_id AND phrase_id=sentence_id";
		MapSqlParameterSource paramSource = new MapSqlParameterSource();
		paramSource.addValue("sentence_id", sentenceId);

		LOG.info(sql);
		TreebankDaoImpl.LogParameters(paramSource);
		Sentence sentence = null;
		SqlRowSet rowSet = jt.queryForRowSet(sql, paramSource);

		if (rowSet.next()) {
			PhraseInternal phrase = (PhraseInternal) phraseMapper.mapRow(rowSet);
			SentenceMapper sentenceMapper = new SentenceMapper(this.treebankServiceInternal, phrase);
			sentence = sentenceMapper.mapRow(rowSet);
			this.treebankServiceInternal.getObjectCache().putEntity(Phrase.class, sentence.getId(), sentence);

		}
		
		sql = "SELECT " + SELECT_PHRASE + " FROM ftb_phrase " +
				" INNER JOIN ftb_phrase_child ON pchild_child_id = phrase_id" +
				" WHERE pchild_phrase_id = :sentence_id" +
				" ORDER BY phrase_depth, phrase_position";
		
		paramSource = new MapSqlParameterSource();
		paramSource.addValue("sentence_id", sentenceId);

		LOG.info(sql);
		TreebankDaoImpl.LogParameters(paramSource);
		
		rowSet = jt.queryForRowSet(sql, paramSource);
		
		List<PhraseInternal> phrases = new ArrayList<PhraseInternal>();
		
		while (rowSet.next()) {
			PhraseInternal phrase = (PhraseInternal) phraseMapper.mapRow(rowSet);
			phrase = (PhraseInternal) this.treebankServiceInternal.getObjectCache().getOrPutEntity(Phrase.class, phrase.getId(), phrase);
			phrases.add(phrase);
		}
		
		for (PhraseInternal phrase : phrases) {
			PhraseInternal parent = (PhraseInternal) phrase.getParent();
			if (parent!=null) {
				parent.getChildrenDB().add(phrase);
			}
		}
		
		List<PhraseUnit> phraseUnits = this.findAllPhraseUnits(sentence);
		
		PhraseInternal sentenceInternal = (PhraseInternal) sentence;
		
		for (PhraseUnit phraseUnit : phraseUnits) {
			PhraseInternal phrase = (PhraseInternal) phraseUnit.getPhrase();
			PhraseUnitInternal punit = (PhraseUnitInternal) phraseUnit;
			phrase.getPhraseUnitsDB().add(punit);
			sentenceInternal.getAllPhraseUnitsDB().add(phraseUnit);
		}
		
		this.findAllWordsAndLemmas(sentence, phraseUnits);

		this.treebankServiceInternal.getObjectCache().clearCache(Phrase.class);

		return sentence;
	}
	
	public Sentence loadSentence(int sentenceId) {
		NamedParameterJdbcTemplate jt = new NamedParameterJdbcTemplate(this.getDataSource());
		String sql = "SELECT " + SELECT_SENTENCE + ", " + SELECT_PHRASE + " FROM ftb_phrase p, ftb_sentence s" +
		" WHERE sentence_id=:sentence_id AND phrase_id=sentence_id";
		MapSqlParameterSource paramSource = new MapSqlParameterSource();
		paramSource.addValue("sentence_id", sentenceId);

		LOG.info(sql);
		TreebankDaoImpl.LogParameters(paramSource);
		Sentence sentence = null;
		SqlRowSet rowSet = jt.queryForRowSet(sql, paramSource);

		if (rowSet.next()) {
			PhraseMapper phraseMapper = new PhraseMapper(this.treebankServiceInternal);
			PhraseInternal phrase = (PhraseInternal) phraseMapper.mapRow(rowSet);
			SentenceMapper sentenceMapper = new SentenceMapper(this.treebankServiceInternal, phrase);
			sentence = sentenceMapper.mapRow(rowSet);
		}

		return sentence;
	}
	
	
	
	public List<Sentence> findSentences(TreebankFile treebankFile) {
		NamedParameterJdbcTemplate jt = new NamedParameterJdbcTemplate(this.getDataSource());
		String sql = "SELECT " + SELECT_SENTENCE + ", " + SELECT_PHRASE +
			" FROM ftb_sentence" +
			" INNER JOIN ftb_phrase ON phrase_id=sentence_id" +
			" WHERE sentence_file_id=:sentence_file_id" +
			" ORDER BY sentence_id";
		MapSqlParameterSource paramSource = new MapSqlParameterSource();
		paramSource.addValue("sentence_file_id", treebankFile.getId());

		LOG.info(sql);
		TreebankDaoImpl.LogParameters(paramSource);

		List<Sentence> sentences = new ArrayList<Sentence>();
		SqlRowSet rowSet = jt.queryForRowSet(sql, paramSource);

		while (rowSet.next()) {
			PhraseMapper phraseMapper = new PhraseMapper(this.treebankServiceInternal);
			PhraseInternal phrase = (PhraseInternal) phraseMapper.mapRow(rowSet);
			SentenceMapper sentenceMapper = new SentenceMapper(this.treebankServiceInternal, phrase);
			Sentence sentence = sentenceMapper.mapRow(rowSet);
			sentences.add(sentence);
		}
		return sentences;
	}

	protected static final class SentenceMapper implements RowMapper {
		private String alias = "";
		private TreebankServiceInternal treebankService;
		private PhraseInternal phrase = null;

		public SentenceMapper(TreebankServiceInternal treebankService, PhraseInternal phrase) 
		{
			this.treebankService = treebankService; 
			this.phrase = phrase;
		}
		public SentenceMapper(String alias, TreebankServiceInternal treebankService, PhraseInternal phrase) {
			this.treebankService = treebankService;
			this.alias = alias + "_"; 
			this.phrase = phrase;
		}

		public Object mapRow(ResultSet rs, int rowNum) throws SQLException {
			return this.mapRow(new ResultSetWrappingSqlRowSet(rs));
		}

		public Sentence mapRow(SqlRowSet rs) {
			SentenceInternal sentence = treebankService.newSentenceInternal(this.phrase);

			// sentence_file_id, sentence_text_id
			sentence.setId(rs.getInt(alias + "sentence_id"));
			sentence.setSentenceNumber(rs.getString(alias + "sentence_number"));
			sentence.setFileId(rs.getInt(alias + "sentence_file_id"));
			sentence.setTextItemId(rs.getInt(alias + "sentence_text_id"));
			sentence.setText(rs.getString(alias + "sentence_text"));

			sentence.setIsNew(false);
			sentence.setSentenceDirty(false);

			return sentence;
		}
	}


	@SuppressWarnings("unchecked")
	public List<Integer> findSentenceIds(int minId, int maxId) {
		NamedParameterJdbcTemplate jt = new NamedParameterJdbcTemplate(this.getDataSource());
		String sql = "SELECT sentence_id FROM ftb_sentence" +
		" WHERE sentence_id >= :min_id" +
		" AND sentence_id <= :max_id" +
		" ORDER BY sentence_id";
		MapSqlParameterSource paramSource = new MapSqlParameterSource();
		paramSource.addValue("min_id", minId);
		paramSource.addValue("max_id", maxId);

		LOG.info(sql);
		TreebankDaoImpl.LogParameters(paramSource);
		List<Integer> sentenceIds = jt.queryForList(sql, paramSource, Integer.class);

		return sentenceIds;  
	}

	public List<Integer> findSentenceIds(TreebankFile treebankFile) {
		NamedParameterJdbcTemplate jt = new NamedParameterJdbcTemplate(this.getDataSource());
		String sql = "SELECT sentence_id" +
			" FROM ftb_sentence" +
			" WHERE sentence_file_id=:sentence_file_id" +
			" ORDER BY sentence_id";
		
		MapSqlParameterSource paramSource = new MapSqlParameterSource();
		paramSource.addValue("sentence_file_id", treebankFile.getId());

		LOG.info(sql);
		TreebankDaoImpl.LogParameters(paramSource);

		@SuppressWarnings("unchecked")
		List<Integer> sentenceIds = jt.queryForList(sql, paramSource, Integer.class);

		return sentenceIds;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public List<Integer> findSentenceIds(TreebankSubSet treebankSubSet, int numIds, int startId) {
		NamedParameterJdbcTemplate jt = new NamedParameterJdbcTemplate(this.getDataSource());
		MapSqlParameterSource paramSource = new MapSqlParameterSource();
		String sql = "SELECT sentence_id FROM ftb_sentence" +
				" WHERE sentence_id >= :min_id";
		paramSource.addValue("min_id", startId);

		String andWord = " AND ";
		for (String whereClause : this.getWhereClausesForSubSet(treebankSubSet, paramSource)) {
			sql += andWord + whereClause;
		}
		
		sql += " ORDER BY sentence_id";
		if (numIds>0) {
			sql += " LIMIT :maxResults";
			paramSource.addValue("maxResults", numIds);
		}      
		LOG.info(sql);
		TreebankDaoImpl.LogParameters(paramSource);
		List<Integer> sentenceIds = jt.queryForList(sql, paramSource, Integer.class);

		return sentenceIds;
	}
	
	public List<Integer> findSentenceIds(TreebankSubSet treebankSubSet, int numIds) {
		return this.findSentenceIds(treebankSubSet, numIds, 0);
	}

	private List<String> getWhereClausesForSubSet(TreebankSubSet treebankSubSet, MapSqlParameterSource paramSource) {
		return this.getWhereClausesForSubSet(treebankSubSet, paramSource, "");
	}
	
	private List<String> getWhereClausesForSubSet(TreebankSubSet treebankSubSet, MapSqlParameterSource paramSource, String prefix) {
		List<String> whereClauses = new ArrayList<String>();
		if (treebankSubSet.getFileNumbersToInclude().length>0) {
			int i = 0;
			String includeClause = "(";
			boolean firstOne = true;
			for (int id : treebankSubSet.getFileNumbersToInclude()) {
				if (!firstOne)
					includeClause += " OR ";
				includeClause += "sentence_file_id % 10 = :" + prefix + "includeId" + i;
				paramSource.addValue( prefix + "includeId" + i, id);
				firstOne = false;
				i++;
			}
			includeClause += ")";
			whereClauses.add(includeClause);
		}
		if (treebankSubSet.getFileNumbersToExclude().length>0) {
			int i = 0;
			for (int id : treebankSubSet.getFileNumbersToExclude()) {
				whereClauses.add("sentence_file_id % 10 != :" + prefix + "excludeId" + i);
				paramSource.addValue(prefix + "excludeId" + i, id);
				i++;
			}
		}
		return whereClauses;
	}
	public Set<String> findUnknownWords(TreebankSubSet knownSet, TreebankSubSet unknownSet) {
		Set<String> unknownWords = new TreeSet<String>();

		NamedParameterJdbcTemplate jt = new NamedParameterJdbcTemplate(this.getDataSource());
		MapSqlParameterSource paramSource = new MapSqlParameterSource();
		String sql = "SELECT word_text" +
				" FROM ftb_phrase_unit pu1" +
				" INNER JOIN ftb_phrase_child pc1 ON pu1.punit_phrase_id = pc1.pchild_child_id" +
				" INNER JOIN ftb_sentence s1 ON s1.sentence_id = pc1.pchild_phrase_id" +
				" INNER JOIN ftb_word ON pu1.punit_word_id = word_id" +
				" WHERE NOT EXISTS" +
				" (SELECT pu2.punit_word_id FROM ftb_phrase_unit pu2" +
				" INNER JOIN ftb_phrase_child pc2 ON pu2.punit_phrase_id = pc2.pchild_child_id" +
				" INNER JOIN ftb_sentence s2 ON s2.sentence_id = pc2.pchild_phrase_id" +
				" WHERE pu2.punit_word_id = pu1.punit_word_id";
		
		for (String whereClause : this.getWhereClausesForSubSet(knownSet, paramSource, "known_")) {
			sql += " AND " + whereClause;
		}
		sql += ")";
		
		for (String whereClause : this.getWhereClausesForSubSet(unknownSet, paramSource, "unknown_")) {
			sql += " AND " + whereClause;
		}
		
		sql += " GROUP BY word_text";
		
		LOG.info(sql);
		TreebankDaoImpl.LogParameters(paramSource);
		@SuppressWarnings("unchecked")
		List<String> unknownWordList = jt.queryForList(sql, paramSource, String.class);

		unknownWords.addAll(unknownWordList);
		return unknownWords;
	}

	@SuppressWarnings("unchecked")
	public List<Integer> findWordIds(int minId, int maxId) {
		NamedParameterJdbcTemplate jt = new NamedParameterJdbcTemplate(this.getDataSource());
		String sql = "SELECT word_id FROM ftb_word" +
		" WHERE word_id >= :min_id" +
		" AND word_id <= :max_id" +
		" ORDER BY word_id";
		MapSqlParameterSource paramSource = new MapSqlParameterSource();
		paramSource.addValue("min_id", minId);
		paramSource.addValue("max_id", maxId);

		LOG.info(sql);
		TreebankDaoImpl.LogParameters(paramSource);
		List<Integer> sentenceIds = jt.queryForList(sql, paramSource, Integer.class);

		return sentenceIds;  
	}

	@SuppressWarnings("unchecked")
	public List<Integer> findCompoundWordIds(int minId, int maxId) {
		NamedParameterJdbcTemplate jt = new NamedParameterJdbcTemplate(this.getDataSource());
		String sql = "SELECT word_id FROM ftb_word, ftb_phrase_unit" +
		" WHERE word_id >= :min_id" +
		" AND word_id <= :max_id" +
		" AND word_id = punit_word_id" +
		" AND punit_compound = true" +
		" AND punit_id not in" +
		" (select psubunit_punit_id from ftb_phrase_subunit" +
		" group by psubunit_punit_id" +
		" having count(*)=1)" +
		" GROUP BY word_id" +
		" ORDER BY word_id";
		MapSqlParameterSource paramSource = new MapSqlParameterSource();
		paramSource.addValue("min_id", minId);
		paramSource.addValue("max_id", maxId);

		LOG.info(sql);
		TreebankDaoImpl.LogParameters(paramSource);
		List<Integer> wordIds = jt.queryForList(sql, paramSource, Integer.class);

		return wordIds;  
	}

	@SuppressWarnings("unchecked")
	public List<Integer> findCompoundPhraseUnitIds(int minId, int maxId, boolean includeSingleSubunitCompounds) {
		NamedParameterJdbcTemplate jt = new NamedParameterJdbcTemplate(this.getDataSource());
		String sql = "SELECT punit_id FROM ftb_phrase_unit" +
		" WHERE punit_id >= :min_id" +
		" AND punit_id <= :max_id" +
		" AND punit_compound = true";
		if (!includeSingleSubunitCompounds) {
			sql += " AND punit_id not in" +
				" (select psubunit_punit_id from ftb_phrase_subunit" +
				" group by psubunit_punit_id" +
				" having count(*)=1)";
		}
		sql += " ORDER BY punit_id";
		MapSqlParameterSource paramSource = new MapSqlParameterSource();
		paramSource.addValue("min_id", minId);
		paramSource.addValue("max_id", maxId);

		LOG.info(sql);
		TreebankDaoImpl.LogParameters(paramSource);
		List<Integer> phraseUnitIds = jt.queryForList(sql, paramSource, Integer.class);

		return phraseUnitIds;  
	}
	
	@Override
	public List<Integer> findCompoundPhraseUnitIds(int minId, int maxId) {
		return this.findCompoundPhraseUnitIds(minId, maxId, true);
	}

	public void beginTransaction() {
		Connection connection;
		try {
			connection = this.getDataSource().getConnection();
			Statement statement = connection.createStatement();
			LOG.info("begin transaction");
			statement.execute("begin");
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}

	}

	public void rollbackTransaction() {
		Connection connection;
		try {
			connection = this.getDataSource().getConnection();
			Statement statement = connection.createStatement();
			LOG.info("rollback transaction");
			statement.execute("rollback");
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	public void commitTransaction() {
		Connection connection;
		try {
			connection = this.getDataSource().getConnection();
			Statement statement = connection.createStatement();
			LOG.info("commit transaction");
			statement.execute("commit");
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public TextItem loadTextItem(int textItemId) {
		NamedParameterJdbcTemplate jt = new NamedParameterJdbcTemplate(this.getDataSource());
		String sql = "SELECT " + SELECT_TEXT_ITEM + " FROM ftb_text WHERE text_id=:text_id";
		MapSqlParameterSource paramSource = new MapSqlParameterSource();
		paramSource.addValue("text_id", textItemId);

		LOG.info(sql);
		TreebankDaoImpl.LogParameters(paramSource);
		TextItem textItem = null;
		try {
			textItem = (TextItem)  jt.queryForObject(sql, paramSource, new TextItemMapper());
		} catch (EmptyResultDataAccessException ex) {
			ex.hashCode();
		}
		return textItem;
	}

	@Override
	public TextItem loadTextItem(String externalId) {
		NamedParameterJdbcTemplate jt = new NamedParameterJdbcTemplate(this.getDataSource());
		String sql = "SELECT " + SELECT_TEXT_ITEM + " FROM ftb_text WHERE text_external_id=:text_external_id";
		MapSqlParameterSource paramSource = new MapSqlParameterSource();
		paramSource.addValue("text_external_id", externalId);

		LOG.info(sql);
		TreebankDaoImpl.LogParameters(paramSource);
		TextItem textItem = null;
		try {
			textItem = (TextItem)  jt.queryForObject(sql, paramSource, new TextItemMapper());
		} catch (EmptyResultDataAccessException ex) {
			ex.hashCode();
		}
		return textItem;
	}

	@Override
	public void saveTextItem(TextItemInternal textItem) {
		NamedParameterJdbcTemplate jt = new NamedParameterJdbcTemplate(this.getDataSource());
		MapSqlParameterSource paramSource = new MapSqlParameterSource();
		paramSource.addValue("text_file_id", textItem.getFileId());
		paramSource.addValue("text_external_id", textItem.getExternalId());
		if (textItem.isNew()) {
			String sql = "SELECT nextval('ftb_text_text_id_seq')";
			LOG.info(sql);
			int textItemId = jt.queryForInt(sql, paramSource);
			paramSource.addValue("text_id", textItemId);

			sql = "INSERT INTO ftb_text (text_id, text_file_id, text_external_id) VALUES (:text_id, :text_file_id, :text_external_id)";

			LOG.info(sql);
			TreebankDaoImpl.LogParameters(paramSource);
			jt.update(sql, paramSource);
			textItem.setId(textItemId);
		} else {
			String sql = "UPDATE ftb_text" +
			" SET text_file_id = :text_file_id" +
			", text_external_id = :text_external_id" +
			" WHERE text_id = :text_id";

			paramSource.addValue("text_id", textItem.getId());
			LOG.info(sql);
			TreebankDaoImpl.LogParameters(paramSource);
			jt.update(sql, paramSource);           
		}
	}

	@Override
	public void deleteTextItem(int textItemId) {
		NamedParameterJdbcTemplate jt = new NamedParameterJdbcTemplate(this.getDataSource());
		MapSqlParameterSource paramSource = new MapSqlParameterSource();
		String sql = "DELETE FROM ftb_text WHERE text_id = :text_id";
		paramSource.addValue("text_id", textItemId);
		LOG.info(sql);
		TreebankDaoImpl.LogParameters(paramSource);
		jt.update(sql, paramSource);
	}

	protected static final class TextItemMapper implements RowMapper {

		public Object mapRow(ResultSet rs, int rowNum) throws SQLException {
			return this.mapRow(new ResultSetWrappingSqlRowSet(rs));
		}

		public TextItem mapRow(SqlRowSet rs) {
			TextItemInternal textItem = new TextItemImpl();
			textItem.setId(rs.getInt("text_id"));
			textItem.setFileId(rs.getInt("text_file_id"));
			textItem.setExternalId(rs.getString("text_external_id"));
			return textItem;
		}
	}
	
    @SuppressWarnings("unchecked")
    public static void LogParameters(MapSqlParameterSource paramSource) {
       DaoUtils.LogParameters(paramSource.getValues());
    }
    
}
