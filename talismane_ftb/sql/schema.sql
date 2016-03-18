--
-- PostgreSQL database dump
--

SET client_encoding = 'UTF8';
SET check_function_bodies = false;
SET client_min_messages = warning;

--
-- Name: SCHEMA public; Type: COMMENT; Schema: -; Owner: postgres
--

COMMENT ON SCHEMA public IS 'Standard public schema';


--
-- Name: plpgsql; Type: PROCEDURAL LANGUAGE; Schema: -; Owner: 
--

CREATE PROCEDURAL LANGUAGE plpgsql;


SET search_path = public, pg_catalog;

--
-- Name: wipedb(); Type: FUNCTION; Schema: public; Owner: postgres
--

CREATE FUNCTION wipedb() RETURNS void
    AS $$
BEGIN
delete from ftb_phrase_child;
delete from ftb_phrase_subunit;
delete from ftb_phrase_unit;
delete from ftb_sentence;
delete from ftb_phrase;
delete from ftb_text;
delete from ftb_file;
delete from ftb_sub_category;
delete from ftb_category;
delete from ftb_morph;
delete from ftb_function;
delete from ftb_phrase_type;
delete from ftb_word;

ALTER SEQUENCE ftb_category_cat_id_seq RESTART WITH 1;
ALTER SEQUENCE ftb_file_file_id_seq RESTART WITH 1;
ALTER SEQUENCE ftb_function_function_id_seq RESTART WITH 1;
ALTER SEQUENCE ftb_morph_morph_id_seq RESTART WITH 1;
ALTER SEQUENCE ftb_phrase_phrase_id_seq RESTART WITH 1;
ALTER SEQUENCE ftb_phrase_subunit_psubunit_id_seq RESTART WITH 1;
ALTER SEQUENCE ftb_phrase_type_ptype_id_seq RESTART WITH 1;
ALTER SEQUENCE ftb_phrase_unit_punit_id_seq RESTART WITH 1;
ALTER SEQUENCE ftb_sentence_sentence_id_seq RESTART WITH 1;
ALTER SEQUENCE ftb_sub_category_subcat_id_seq RESTART WITH 1;
ALTER SEQUENCE ftb_text_text_id_seq RESTART WITH 1;
ALTER SEQUENCE ftb_word_word_id_seq RESTART WITH 1;

insert into ftb_category (cat_id, cat_code, cat_description) values (nextval('ftb_category_cat_id_seq'), 'N', 'Noun');

RETURN;
END;
$$
    LANGUAGE plpgsql;


ALTER FUNCTION public.wipedb() OWNER TO postgres;

SET default_tablespace = '';

SET default_with_oids = false;

--
-- Name: ftb_category; Type: TABLE; Schema: public; Owner: postgres; Tablespace: 
--

CREATE TABLE ftb_category (
    cat_id smallint NOT NULL,
    cat_code character varying(20) NOT NULL,
    cat_description character varying(256)
);


ALTER TABLE public.ftb_category OWNER TO postgres;

--
-- Name: TABLE ftb_category; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON TABLE ftb_category IS 'a grammatical category';


--
-- Name: COLUMN ftb_category.cat_code; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN ftb_category.cat_code IS 'the code of this category';


--
-- Name: COLUMN ftb_category.cat_description; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN ftb_category.cat_description IS 'description of this category';


--
-- Name: ftb_file; Type: TABLE; Schema: public; Owner: postgres; Tablespace: 
--

CREATE TABLE ftb_file (
    file_id integer NOT NULL,
    file_name character varying(256)
);


ALTER TABLE public.ftb_file OWNER TO postgres;

--
-- Name: TABLE ftb_file; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON TABLE ftb_file IS 'an actual XML file from which data was extracted';


--
-- Name: ftb_function; Type: TABLE; Schema: public; Owner: postgres; Tablespace: 
--

CREATE TABLE ftb_function (
    function_id smallint NOT NULL,
    function_code character varying(20) NOT NULL,
    function_description character varying(256)
);


ALTER TABLE public.ftb_function OWNER TO postgres;

--
-- Name: TABLE ftb_function; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON TABLE ftb_function IS 'a function that a given phrase can play, e.g. subject, etc.';


--
-- Name: ftb_morph; Type: TABLE; Schema: public; Owner: postgres; Tablespace: 
--

CREATE TABLE ftb_morph (
    morph_id smallint NOT NULL,
    morph_code character varying(20) NOT NULL,
    morph_description character varying(256)
);


ALTER TABLE public.ftb_morph OWNER TO postgres;

--
-- Name: TABLE ftb_morph; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON TABLE ftb_morph IS 'morphological characteristic';


--
-- Name: COLUMN ftb_morph.morph_code; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN ftb_morph.morph_code IS 'code representing this characteristic';


--
-- Name: ftb_phrase; Type: TABLE; Schema: public; Owner: postgres; Tablespace: 
--

CREATE TABLE ftb_phrase (
    phrase_id integer NOT NULL,
    phrase_ptype_id smallint NOT NULL,
    phrase_parent_id integer,
    phrase_function_id smallint,
    phrase_position smallint NOT NULL,
    phrase_depth smallint NOT NULL
);


ALTER TABLE public.ftb_phrase OWNER TO postgres;

--
-- Name: TABLE ftb_phrase; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON TABLE ftb_phrase IS 'a phrase within a sentence, either top-level (no parent) or lower level (has parent)';


--
-- Name: COLUMN ftb_phrase.phrase_ptype_id; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN ftb_phrase.phrase_ptype_id IS 'the phrase type';


--
-- Name: COLUMN ftb_phrase.phrase_parent_id; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN ftb_phrase.phrase_parent_id IS 'the parent if it''s an embedded phrase, null if it''s top-level';


--
-- Name: COLUMN ftb_phrase.phrase_function_id; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN ftb_phrase.phrase_function_id IS 'the function played by this phrase, if any';


--
-- Name: COLUMN ftb_phrase.phrase_position; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN ftb_phrase.phrase_position IS 'the position of the phrase within the sentence - note that when embedded, position will be counted serially, i.e. parent 1, child 2, child 3, parent 4, for ease of selecting all words in a sentence in the right order';


--
-- Name: COLUMN ftb_phrase.phrase_depth; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN ftb_phrase.phrase_depth IS 'Depth of phrase, where a phrase directly beneath the sentence level has depth 1.';


--
-- Name: ftb_phrase_child; Type: TABLE; Schema: public; Owner: postgres; Tablespace: 
--

CREATE TABLE ftb_phrase_child (
    pchild_phrase_id integer NOT NULL,
    pchild_child_id integer NOT NULL
);


ALTER TABLE public.ftb_phrase_child OWNER TO postgres;

--
-- Name: TABLE ftb_phrase_child; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON TABLE ftb_phrase_child IS 'A flattened view of a phrase, which maps a phrase to itself and all of its descendents, useful for getting all of the units (words) in a given phrase without recursion.';


--
-- Name: COLUMN ftb_phrase_child.pchild_phrase_id; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN ftb_phrase_child.pchild_phrase_id IS 'parent id';


--
-- Name: COLUMN ftb_phrase_child.pchild_child_id; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN ftb_phrase_child.pchild_child_id IS 'child id, including one record mapping a phrase to itself';


--
-- Name: ftb_phrase_subunit; Type: TABLE; Schema: public; Owner: postgres; Tablespace: 
--

CREATE TABLE ftb_phrase_subunit (
    psubunit_id integer NOT NULL,
    psubunit_punit_id integer,
    psubunit_word_id integer,
    psubunit_position smallint,
    psubunit_cat_id smallint
);


ALTER TABLE public.ftb_phrase_subunit OWNER TO postgres;

--
-- Name: TABLE ftb_phrase_subunit; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON TABLE ftb_phrase_subunit IS 'used to contain individual members of a compound word';


--
-- Name: COLUMN ftb_phrase_subunit.psubunit_punit_id; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN ftb_phrase_subunit.psubunit_punit_id IS 'the phrase unit containing this subunit, must be compound';


--
-- Name: COLUMN ftb_phrase_subunit.psubunit_word_id; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN ftb_phrase_subunit.psubunit_word_id IS 'the actual word referenced by this sub-unit';


--
-- Name: COLUMN ftb_phrase_subunit.psubunit_position; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN ftb_phrase_subunit.psubunit_position IS 'position within the compound word, starts at 0';


--
-- Name: COLUMN ftb_phrase_subunit.psubunit_cat_id; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN ftb_phrase_subunit.psubunit_cat_id IS 'category of this subunit';


--
-- Name: ftb_phrase_type; Type: TABLE; Schema: public; Owner: postgres; Tablespace: 
--

CREATE TABLE ftb_phrase_type (
    ptype_id smallint NOT NULL,
    ptype_code character varying(20) NOT NULL,
    ptype_description character varying(256)
);


ALTER TABLE public.ftb_phrase_type OWNER TO postgres;

--
-- Name: TABLE ftb_phrase_type; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON TABLE ftb_phrase_type IS 'represents one possible phrase type, e.g. NP (noun phrase)';


--
-- Name: ftb_phrase_unit; Type: TABLE; Schema: public; Owner: postgres; Tablespace: 
--

CREATE TABLE ftb_phrase_unit (
    punit_id integer NOT NULL,
    punit_word_id integer NOT NULL,
    punit_phrase_id integer,
    punit_position smallint NOT NULL,
    punit_lemma_id integer,
    punit_cat_id smallint NOT NULL,
    punit_subcat_id smallint,
    punit_morph_id smallint,
    punit_compound boolean DEFAULT false NOT NULL,
    punit_pos_in_phrase smallint NOT NULL
);


ALTER TABLE public.ftb_phrase_unit OWNER TO postgres;

--
-- Name: TABLE ftb_phrase_unit; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON TABLE ftb_phrase_unit IS 'a single word within the French Treebank, along with it''s various attributes and position inside a phrase';


--
-- Name: COLUMN ftb_phrase_unit.punit_word_id; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN ftb_phrase_unit.punit_word_id IS 'link to the actual word represented by this';


--
-- Name: COLUMN ftb_phrase_unit.punit_phrase_id; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN ftb_phrase_unit.punit_phrase_id IS 'the phrase directly containing this phrase unit, or none if top-level in sentence';


--
-- Name: COLUMN ftb_phrase_unit.punit_position; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN ftb_phrase_unit.punit_position IS 'the position within the sentence, starting at 0';


--
-- Name: COLUMN ftb_phrase_unit.punit_lemma_id; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN ftb_phrase_unit.punit_lemma_id IS 'a link to the lemma corresponding to this usage of a word';


--
-- Name: COLUMN ftb_phrase_unit.punit_cat_id; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN ftb_phrase_unit.punit_cat_id IS 'the main grammatical category of this word';


--
-- Name: COLUMN ftb_phrase_unit.punit_subcat_id; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN ftb_phrase_unit.punit_subcat_id IS 'the grammatical sub-category of the word, may be null for some category types';


--
-- Name: COLUMN ftb_phrase_unit.punit_morph_id; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN ftb_phrase_unit.punit_morph_id IS 'the morphological class of the word';


--
-- Name: COLUMN ftb_phrase_unit.punit_compound; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN ftb_phrase_unit.punit_compound IS 'is this a compound or not? compound phrase units must contain 2 or more subunits';


--
-- Name: COLUMN ftb_phrase_unit.punit_pos_in_phrase; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN ftb_phrase_unit.punit_pos_in_phrase IS 'A position that takes into accounts the phrase_position of other sub-phrases within the parent phrase.';


--
-- Name: ftb_sentence; Type: TABLE; Schema: public; Owner: postgres; Tablespace: 
--

CREATE TABLE ftb_sentence (
    sentence_id integer NOT NULL,
    sentence_number integer NOT NULL,
    sentence_file_id integer NOT NULL,
    sentence_text_id integer
);


ALTER TABLE public.ftb_sentence OWNER TO postgres;

--
-- Name: TABLE ftb_sentence; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON TABLE ftb_sentence IS 'a single sentence, containing any number of phrases';


--
-- Name: COLUMN ftb_sentence.sentence_number; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN ftb_sentence.sentence_number IS 'the reference number for this sentence in the original file';


--
-- Name: COLUMN ftb_sentence.sentence_file_id; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN ftb_sentence.sentence_file_id IS 'the file in which this sentence was found';


--
-- Name: COLUMN ftb_sentence.sentence_text_id; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN ftb_sentence.sentence_text_id IS 'the text containing this sentence';


--
-- Name: ftb_sub_category; Type: TABLE; Schema: public; Owner: postgres; Tablespace: 
--

CREATE TABLE ftb_sub_category (
    subcat_id smallint NOT NULL,
    subcat_cat_id smallint NOT NULL,
    subcat_code character varying(20) NOT NULL,
    subcat_description character varying(256)
);


ALTER TABLE public.ftb_sub_category OWNER TO postgres;

--
-- Name: TABLE ftb_sub_category; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON TABLE ftb_sub_category IS 'sub-category within a given category';


--
-- Name: COLUMN ftb_sub_category.subcat_cat_id; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN ftb_sub_category.subcat_cat_id IS 'category containing this subcategory';


--
-- Name: COLUMN ftb_sub_category.subcat_code; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN ftb_sub_category.subcat_code IS 'code of this sub-category';


--
-- Name: ftb_text; Type: TABLE; Schema: public; Owner: postgres; Tablespace: 
--

CREATE TABLE ftb_text (
    text_id integer NOT NULL,
    text_file_id integer,
    text_text text
);


ALTER TABLE public.ftb_text OWNER TO postgres;

--
-- Name: TABLE ftb_text; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON TABLE ftb_text IS 'a textual passage for the treebank';


--
-- Name: COLUMN ftb_text.text_file_id; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN ftb_text.text_file_id IS 'the file containing the text';


--
-- Name: COLUMN ftb_text.text_text; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN ftb_text.text_text IS 'the actual text';


--
-- Name: ftb_word; Type: TABLE; Schema: public; Owner: postgres; Tablespace: 
--

CREATE TABLE ftb_word (
    word_id integer NOT NULL,
    word_text character varying(256) NOT NULL
);


ALTER TABLE public.ftb_word OWNER TO postgres;

--
-- Name: TABLE ftb_word; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON TABLE ftb_word IS 'an actual graphical representation of a word or lemma';


--
-- Name: COLUMN ftb_word.word_text; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN ftb_word.word_text IS 'may include spaces for compound words, includes capitalisation and accents';


--
-- Name: pk_category; Type: CONSTRAINT; Schema: public; Owner: postgres; Tablespace: 
--

ALTER TABLE ONLY ftb_category
    ADD CONSTRAINT pk_category PRIMARY KEY (cat_id);


--
-- Name: pk_file; Type: CONSTRAINT; Schema: public; Owner: postgres; Tablespace: 
--

ALTER TABLE ONLY ftb_file
    ADD CONSTRAINT pk_file PRIMARY KEY (file_id);


--
-- Name: pk_function; Type: CONSTRAINT; Schema: public; Owner: postgres; Tablespace: 
--

ALTER TABLE ONLY ftb_function
    ADD CONSTRAINT pk_function PRIMARY KEY (function_id);


--
-- Name: pk_morph; Type: CONSTRAINT; Schema: public; Owner: postgres; Tablespace: 
--

ALTER TABLE ONLY ftb_morph
    ADD CONSTRAINT pk_morph PRIMARY KEY (morph_id);


--
-- Name: pk_phrase; Type: CONSTRAINT; Schema: public; Owner: postgres; Tablespace: 
--

ALTER TABLE ONLY ftb_phrase
    ADD CONSTRAINT pk_phrase PRIMARY KEY (phrase_id);


--
-- Name: pk_phrase_child; Type: CONSTRAINT; Schema: public; Owner: postgres; Tablespace: 
--

ALTER TABLE ONLY ftb_phrase_child
    ADD CONSTRAINT pk_phrase_child PRIMARY KEY (pchild_phrase_id, pchild_child_id);


--
-- Name: pk_phrase_subunit; Type: CONSTRAINT; Schema: public; Owner: postgres; Tablespace: 
--

ALTER TABLE ONLY ftb_phrase_subunit
    ADD CONSTRAINT pk_phrase_subunit PRIMARY KEY (psubunit_id);


--
-- Name: pk_phrase_unit; Type: CONSTRAINT; Schema: public; Owner: postgres; Tablespace: 
--

ALTER TABLE ONLY ftb_phrase_unit
    ADD CONSTRAINT pk_phrase_unit PRIMARY KEY (punit_id);


--
-- Name: pk_ptype; Type: CONSTRAINT; Schema: public; Owner: postgres; Tablespace: 
--

ALTER TABLE ONLY ftb_phrase_type
    ADD CONSTRAINT pk_ptype PRIMARY KEY (ptype_id);


--
-- Name: pk_sentence; Type: CONSTRAINT; Schema: public; Owner: postgres; Tablespace: 
--

ALTER TABLE ONLY ftb_sentence
    ADD CONSTRAINT pk_sentence PRIMARY KEY (sentence_id);


--
-- Name: pk_subcat; Type: CONSTRAINT; Schema: public; Owner: postgres; Tablespace: 
--

ALTER TABLE ONLY ftb_sub_category
    ADD CONSTRAINT pk_subcat PRIMARY KEY (subcat_id);


--
-- Name: pk_text; Type: CONSTRAINT; Schema: public; Owner: postgres; Tablespace: 
--

ALTER TABLE ONLY ftb_text
    ADD CONSTRAINT pk_text PRIMARY KEY (text_id);


--
-- Name: pk_word; Type: CONSTRAINT; Schema: public; Owner: postgres; Tablespace: 
--

ALTER TABLE ONLY ftb_word
    ADD CONSTRAINT pk_word PRIMARY KEY (word_id);


--
-- Name: uk_cat_code; Type: CONSTRAINT; Schema: public; Owner: postgres; Tablespace: 
--

ALTER TABLE ONLY ftb_category
    ADD CONSTRAINT uk_cat_code UNIQUE (cat_code);


--
-- Name: uk_function_code; Type: CONSTRAINT; Schema: public; Owner: postgres; Tablespace: 
--

ALTER TABLE ONLY ftb_function
    ADD CONSTRAINT uk_function_code UNIQUE (function_code);


--
-- Name: uk_morph_code; Type: CONSTRAINT; Schema: public; Owner: postgres; Tablespace: 
--

ALTER TABLE ONLY ftb_morph
    ADD CONSTRAINT uk_morph_code UNIQUE (morph_code);


--
-- Name: uk_ptype_code; Type: CONSTRAINT; Schema: public; Owner: postgres; Tablespace: 
--

ALTER TABLE ONLY ftb_phrase_type
    ADD CONSTRAINT uk_ptype_code UNIQUE (ptype_code);


--
-- Name: uk_subcat_code; Type: CONSTRAINT; Schema: public; Owner: postgres; Tablespace: 
--

ALTER TABLE ONLY ftb_sub_category
    ADD CONSTRAINT uk_subcat_code UNIQUE (subcat_cat_id, subcat_code);


--
-- Name: uk_word_text; Type: CONSTRAINT; Schema: public; Owner: postgres; Tablespace: 
--

ALTER TABLE ONLY ftb_word
    ADD CONSTRAINT uk_word_text UNIQUE (word_text);


--
-- Name: idx_pchild_child_id; Type: INDEX; Schema: public; Owner: postgres; Tablespace: 
--

CREATE INDEX idx_pchild_child_id ON ftb_phrase_child USING btree (pchild_child_id);


--
-- Name: idx_phrase_depth; Type: INDEX; Schema: public; Owner: postgres; Tablespace: 
--

CREATE INDEX idx_phrase_depth ON ftb_phrase USING btree (phrase_depth);


--
-- Name: idx_phrase_function_id; Type: INDEX; Schema: public; Owner: postgres; Tablespace: 
--

CREATE INDEX idx_phrase_function_id ON ftb_phrase USING btree (phrase_function_id);


--
-- Name: idx_phrase_parent_id; Type: INDEX; Schema: public; Owner: postgres; Tablespace: 
--

CREATE INDEX idx_phrase_parent_id ON ftb_phrase USING btree (phrase_parent_id);


--
-- Name: idx_phrase_position; Type: INDEX; Schema: public; Owner: postgres; Tablespace: 
--

CREATE INDEX idx_phrase_position ON ftb_phrase USING btree (phrase_position);


--
-- Name: idx_phrase_ptype_id; Type: INDEX; Schema: public; Owner: postgres; Tablespace: 
--

CREATE INDEX idx_phrase_ptype_id ON ftb_phrase USING btree (phrase_ptype_id);


--
-- Name: idx_punit_cat_id; Type: INDEX; Schema: public; Owner: postgres; Tablespace: 
--

CREATE INDEX idx_punit_cat_id ON ftb_phrase_unit USING btree (punit_cat_id);


--
-- Name: idx_punit_compound; Type: INDEX; Schema: public; Owner: postgres; Tablespace: 
--

CREATE INDEX idx_punit_compound ON ftb_phrase_unit USING btree (punit_compound);


--
-- Name: idx_punit_lemma_id; Type: INDEX; Schema: public; Owner: postgres; Tablespace: 
--

CREATE INDEX idx_punit_lemma_id ON ftb_phrase_unit USING btree (punit_lemma_id);


--
-- Name: idx_punit_morph_id; Type: INDEX; Schema: public; Owner: postgres; Tablespace: 
--

CREATE INDEX idx_punit_morph_id ON ftb_phrase_unit USING btree (punit_morph_id);


--
-- Name: idx_punit_phrase_id; Type: INDEX; Schema: public; Owner: postgres; Tablespace: 
--

CREATE INDEX idx_punit_phrase_id ON ftb_phrase_unit USING btree (punit_phrase_id);


--
-- Name: idx_punit_pos_in_phrase; Type: INDEX; Schema: public; Owner: postgres; Tablespace: 
--

CREATE INDEX idx_punit_pos_in_phrase ON ftb_phrase_unit USING btree (punit_pos_in_phrase);


--
-- Name: idx_punit_position; Type: INDEX; Schema: public; Owner: postgres; Tablespace: 
--

CREATE INDEX idx_punit_position ON ftb_phrase_unit USING btree (punit_position);


--
-- Name: idx_punit_subcat_id; Type: INDEX; Schema: public; Owner: postgres; Tablespace: 
--

CREATE INDEX idx_punit_subcat_id ON ftb_phrase_unit USING btree (punit_subcat_id);


--
-- Name: idx_punit_word_id; Type: INDEX; Schema: public; Owner: postgres; Tablespace: 
--

CREATE INDEX idx_punit_word_id ON ftb_phrase_unit USING btree (punit_word_id);


--
-- Name: idx_sentence_file_id; Type: INDEX; Schema: public; Owner: postgres; Tablespace: 
--

CREATE INDEX idx_sentence_file_id ON ftb_sentence USING btree (sentence_file_id);


--
-- Name: idx_sentence_number; Type: INDEX; Schema: public; Owner: postgres; Tablespace: 
--

CREATE INDEX idx_sentence_number ON ftb_sentence USING btree (sentence_number);


--
-- Name: fk_pchild_child; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY ftb_phrase_child
    ADD CONSTRAINT fk_pchild_child FOREIGN KEY (pchild_child_id) REFERENCES ftb_phrase(phrase_id);


--
-- Name: fk_pchild_parent; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY ftb_phrase_child
    ADD CONSTRAINT fk_pchild_parent FOREIGN KEY (pchild_phrase_id) REFERENCES ftb_phrase(phrase_id);


--
-- Name: fk_phrase_function; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY ftb_phrase
    ADD CONSTRAINT fk_phrase_function FOREIGN KEY (phrase_function_id) REFERENCES ftb_function(function_id);


--
-- Name: fk_phrase_parent; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY ftb_phrase
    ADD CONSTRAINT fk_phrase_parent FOREIGN KEY (phrase_parent_id) REFERENCES ftb_phrase(phrase_id);


--
-- Name: fk_psubunit_cat; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY ftb_phrase_subunit
    ADD CONSTRAINT fk_psubunit_cat FOREIGN KEY (psubunit_cat_id) REFERENCES ftb_category(cat_id);


--
-- Name: fk_psubunit_punit; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY ftb_phrase_subunit
    ADD CONSTRAINT fk_psubunit_punit FOREIGN KEY (psubunit_punit_id) REFERENCES ftb_phrase_unit(punit_id);


--
-- Name: fk_psubunit_word; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY ftb_phrase_subunit
    ADD CONSTRAINT fk_psubunit_word FOREIGN KEY (psubunit_word_id) REFERENCES ftb_word(word_id);


--
-- Name: fk_punit_cat; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY ftb_phrase_unit
    ADD CONSTRAINT fk_punit_cat FOREIGN KEY (punit_cat_id) REFERENCES ftb_category(cat_id);


--
-- Name: fk_punit_lemma; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY ftb_phrase_unit
    ADD CONSTRAINT fk_punit_lemma FOREIGN KEY (punit_lemma_id) REFERENCES ftb_word(word_id);


--
-- Name: fk_punit_morph; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY ftb_phrase_unit
    ADD CONSTRAINT fk_punit_morph FOREIGN KEY (punit_morph_id) REFERENCES ftb_morph(morph_id);


--
-- Name: fk_punit_subcat; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY ftb_phrase_unit
    ADD CONSTRAINT fk_punit_subcat FOREIGN KEY (punit_subcat_id) REFERENCES ftb_sub_category(subcat_id);


--
-- Name: fk_punit_word; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY ftb_phrase_unit
    ADD CONSTRAINT fk_punit_word FOREIGN KEY (punit_word_id) REFERENCES ftb_word(word_id);


--
-- Name: fk_sentence_file; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY ftb_sentence
    ADD CONSTRAINT fk_sentence_file FOREIGN KEY (sentence_file_id) REFERENCES ftb_file(file_id);


--
-- Name: fk_sentence_phrase; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY ftb_sentence
    ADD CONSTRAINT fk_sentence_phrase FOREIGN KEY (sentence_id) REFERENCES ftb_phrase(phrase_id);


--
-- Name: fk_subcat_cat; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY ftb_sub_category
    ADD CONSTRAINT fk_subcat_cat FOREIGN KEY (subcat_cat_id) REFERENCES ftb_category(cat_id);


--
-- Name: pk_text_file; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY ftb_text
    ADD CONSTRAINT pk_text_file FOREIGN KEY (text_file_id) REFERENCES ftb_file(file_id);


--
-- Name: public; Type: ACL; Schema: -; Owner: postgres
--

REVOKE ALL ON SCHEMA public FROM PUBLIC;
REVOKE ALL ON SCHEMA public FROM postgres;
GRANT ALL ON SCHEMA public TO postgres;
GRANT ALL ON SCHEMA public TO PUBLIC;


--
-- PostgreSQL database dump complete
--

