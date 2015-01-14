--
-- PostgreSQL database dump
--

-- Dumped from database version 8.4.4
-- Dumped by pg_dump version 9.2.3
-- Started on 2013-11-29 17:12:29

SET statement_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = off;
SET check_function_bodies = false;
SET client_min_messages = warning;
SET escape_string_warning = off;

--
-- TOC entry 475 (class 2612 OID 16386)
-- Name: plpgsql; Type: PROCEDURAL LANGUAGE; Schema: -; Owner: postgres
--

CREATE OR REPLACE PROCEDURAL LANGUAGE plpgsql;


ALTER PROCEDURAL LANGUAGE plpgsql OWNER TO postgres;

SET search_path = public, pg_catalog;

SET default_tablespace = '';

SET default_with_oids = false;

--
-- TOC entry 140 (class 1259 OID 5758883)
-- Name: context; Type: TABLE; Schema: public; Owner: nlp; Tablespace: 
--

CREATE TABLE context (
    context_id integer NOT NULL,
    context_start_row integer,
    context_start_column integer,
    context_text character varying(4000) NOT NULL,
    context_file_id integer NOT NULL,
    context_term_id integer NOT NULL
);


ALTER TABLE public.context OWNER TO nlp;

--
-- TOC entry 141 (class 1259 OID 5758889)
-- Name: file; Type: TABLE; Schema: public; Owner: nlp; Tablespace: 
--

CREATE TABLE file (
    file_id integer NOT NULL,
    file_name character varying(256) NOT NULL
);


ALTER TABLE public.file OWNER TO nlp;

--
-- TOC entry 142 (class 1259 OID 5758892)
-- Name: project; Type: TABLE; Schema: public; Owner: nlp; Tablespace: 
--

CREATE TABLE project (
    project_id integer NOT NULL,
    project_code character varying(256) NOT NULL
);


ALTER TABLE public.project OWNER TO nlp;

--
-- TOC entry 143 (class 1259 OID 5758895)
-- Name: seq_context_id; Type: SEQUENCE; Schema: public; Owner: nlp
--

CREATE SEQUENCE seq_context_id
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.seq_context_id OWNER TO nlp;

--
-- TOC entry 144 (class 1259 OID 5758897)
-- Name: seq_file_id; Type: SEQUENCE; Schema: public; Owner: nlp
--

CREATE SEQUENCE seq_file_id
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.seq_file_id OWNER TO nlp;

--
-- TOC entry 145 (class 1259 OID 5758899)
-- Name: seq_project_id; Type: SEQUENCE; Schema: public; Owner: nlp
--

CREATE SEQUENCE seq_project_id
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.seq_project_id OWNER TO nlp;

--
-- TOC entry 146 (class 1259 OID 5758901)
-- Name: seq_term_id; Type: SEQUENCE; Schema: public; Owner: nlp
--

CREATE SEQUENCE seq_term_id
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.seq_term_id OWNER TO nlp;

--
-- TOC entry 151 (class 1259 OID 5759004)
-- Name: seq_text_id; Type: SEQUENCE; Schema: public; Owner: nlp
--

CREATE SEQUENCE seq_text_id
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.seq_text_id OWNER TO nlp;

--
-- TOC entry 147 (class 1259 OID 5758903)
-- Name: term; Type: TABLE; Schema: public; Owner: nlp; Tablespace: 
--

CREATE TABLE term (
    term_id integer NOT NULL,
    term_project_id integer NOT NULL,
    term_text_id integer NOT NULL,
    term_frequency integer DEFAULT 0 NOT NULL,
    term_marked boolean DEFAULT false NOT NULL,
    term_expansion_count integer DEFAULT 0 NOT NULL,
    term_head_count integer DEFAULT 0 NOT NULL
);


ALTER TABLE public.term OWNER TO nlp;

--
-- TOC entry 148 (class 1259 OID 5758912)
-- Name: term_expansions; Type: TABLE; Schema: public; Owner: nlp; Tablespace: 
--

CREATE TABLE term_expansions (
    termexp_term_id integer NOT NULL,
    termexp_expansion_id integer NOT NULL
);


ALTER TABLE public.term_expansions OWNER TO nlp;

--
-- TOC entry 150 (class 1259 OID 5758987)
-- Name: term_heads; Type: TABLE; Schema: public; Owner: postgres; Tablespace: 
--

CREATE TABLE term_heads (
    termhead_term_id integer NOT NULL,
    termhead_head_id integer NOT NULL
);


ALTER TABLE public.term_heads OWNER TO postgres;

--
-- TOC entry 149 (class 1259 OID 5758915)
-- Name: text; Type: TABLE; Schema: public; Owner: nlp; Tablespace: 
--

CREATE TABLE text (
    text_id integer NOT NULL,
    text_text character varying(4000)
);


ALTER TABLE public.text OWNER TO nlp;

--
-- TOC entry 1811 (class 2606 OID 5758940)
-- Name: pk_context; Type: CONSTRAINT; Schema: public; Owner: nlp; Tablespace: 
--

ALTER TABLE ONLY context
    ADD CONSTRAINT pk_context PRIMARY KEY (context_id);


--
-- TOC entry 1813 (class 2606 OID 5758947)
-- Name: pk_file; Type: CONSTRAINT; Schema: public; Owner: nlp; Tablespace: 
--

ALTER TABLE ONLY file
    ADD CONSTRAINT pk_file PRIMARY KEY (file_id);


--
-- TOC entry 1817 (class 2606 OID 5758976)
-- Name: pk_project; Type: CONSTRAINT; Schema: public; Owner: nlp; Tablespace: 
--

ALTER TABLE ONLY project
    ADD CONSTRAINT pk_project PRIMARY KEY (project_id);


--
-- TOC entry 1819 (class 2606 OID 5758933)
-- Name: pk_term; Type: CONSTRAINT; Schema: public; Owner: nlp; Tablespace: 
--

ALTER TABLE ONLY term
    ADD CONSTRAINT pk_term PRIMARY KEY (term_id);


--
-- TOC entry 1823 (class 2606 OID 5758949)
-- Name: pk_termexp; Type: CONSTRAINT; Schema: public; Owner: nlp; Tablespace: 
--

ALTER TABLE ONLY term_expansions
    ADD CONSTRAINT pk_termexp PRIMARY KEY (termexp_term_id, termexp_expansion_id);


--
-- TOC entry 1829 (class 2606 OID 5758993)
-- Name: pk_termhead; Type: CONSTRAINT; Schema: public; Owner: postgres; Tablespace: 
--

ALTER TABLE ONLY term_heads
    ADD CONSTRAINT pk_termhead PRIMARY KEY (termhead_term_id, termhead_head_id);


--
-- TOC entry 1825 (class 2606 OID 5758919)
-- Name: pk_termtext; Type: CONSTRAINT; Schema: public; Owner: nlp; Tablespace: 
--

ALTER TABLE ONLY text
    ADD CONSTRAINT pk_termtext PRIMARY KEY (text_id);


--
-- TOC entry 1815 (class 2606 OID 5759007)
-- Name: uk_file_name; Type: CONSTRAINT; Schema: public; Owner: nlp; Tablespace: 
--

ALTER TABLE ONLY file
    ADD CONSTRAINT uk_file_name UNIQUE (file_name);


--
-- TOC entry 1821 (class 2606 OID 5759019)
-- Name: uk_term_text; Type: CONSTRAINT; Schema: public; Owner: nlp; Tablespace: 
--

ALTER TABLE ONLY term
    ADD CONSTRAINT uk_term_text UNIQUE (term_project_id, term_text_id);


--
-- TOC entry 1827 (class 2606 OID 5758924)
-- Name: uk_termtext_text; Type: CONSTRAINT; Schema: public; Owner: nlp; Tablespace: 
--

ALTER TABLE ONLY text
    ADD CONSTRAINT uk_termtext_text UNIQUE (text_text);


--
-- TOC entry 1831 (class 2606 OID 5758965)
-- Name: fk_context_file; Type: FK CONSTRAINT; Schema: public; Owner: nlp
--

ALTER TABLE ONLY context
    ADD CONSTRAINT fk_context_file FOREIGN KEY (context_file_id) REFERENCES file(file_id);


--
-- TOC entry 1830 (class 2606 OID 5758960)
-- Name: fk_context_term; Type: FK CONSTRAINT; Schema: public; Owner: nlp
--

ALTER TABLE ONLY context
    ADD CONSTRAINT fk_context_term FOREIGN KEY (context_term_id) REFERENCES term(term_id);


--
-- TOC entry 1832 (class 2606 OID 5759045)
-- Name: fk_term_project; Type: FK CONSTRAINT; Schema: public; Owner: nlp
--

ALTER TABLE ONLY term
    ADD CONSTRAINT fk_term_project FOREIGN KEY (term_project_id) REFERENCES project(project_id);


--
-- TOC entry 1833 (class 2606 OID 5759050)
-- Name: fk_term_text; Type: FK CONSTRAINT; Schema: public; Owner: nlp
--

ALTER TABLE ONLY term
    ADD CONSTRAINT fk_term_text FOREIGN KEY (term_text_id) REFERENCES text(text_id);


--
-- TOC entry 1835 (class 2606 OID 5758955)
-- Name: fk_termexp_exp; Type: FK CONSTRAINT; Schema: public; Owner: nlp
--

ALTER TABLE ONLY term_expansions
    ADD CONSTRAINT fk_termexp_exp FOREIGN KEY (termexp_expansion_id) REFERENCES term(term_id);


--
-- TOC entry 1834 (class 2606 OID 5758950)
-- Name: fk_termexp_term; Type: FK CONSTRAINT; Schema: public; Owner: nlp
--

ALTER TABLE ONLY term_expansions
    ADD CONSTRAINT fk_termexp_term FOREIGN KEY (termexp_term_id) REFERENCES term(term_id);


--
-- TOC entry 1837 (class 2606 OID 5758999)
-- Name: fk_termhead_head; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY term_heads
    ADD CONSTRAINT fk_termhead_head FOREIGN KEY (termhead_head_id) REFERENCES term(term_id);


--
-- TOC entry 1836 (class 2606 OID 5758994)
-- Name: fk_termhead_term; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY term_heads
    ADD CONSTRAINT fk_termhead_term FOREIGN KEY (termhead_term_id) REFERENCES term(term_id);


--
-- TOC entry 1856 (class 0 OID 0)
-- Dependencies: 6
-- Name: public; Type: ACL; Schema: -; Owner: postgres
--

REVOKE ALL ON SCHEMA public FROM PUBLIC;
REVOKE ALL ON SCHEMA public FROM postgres;
GRANT ALL ON SCHEMA public TO postgres;
GRANT ALL ON SCHEMA public TO PUBLIC;


-- Completed on 2013-11-29 17:12:35

--
-- PostgreSQL database dump complete
--

