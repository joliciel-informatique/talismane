--
-- PostgreSQL database dump
--

-- Dumped from database version 9.2.3
-- Dumped by pg_dump version 9.2.3
-- Started on 2015-01-22 17:59:27

SET statement_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SET check_function_bodies = false;
SET client_min_messages = warning;

--
-- TOC entry 179 (class 3079 OID 11727)
-- Name: plpgsql; Type: EXTENSION; Schema: -; Owner: 
--

CREATE EXTENSION IF NOT EXISTS plpgsql WITH SCHEMA pg_catalog;


--
-- TOC entry 1982 (class 0 OID 0)
-- Dependencies: 179
-- Name: EXTENSION plpgsql; Type: COMMENT; Schema: -; Owner: 
--

COMMENT ON EXTENSION plpgsql IS 'PL/pgSQL procedural language';


SET search_path = public, pg_catalog;

SET default_with_oids = false;

--
-- TOC entry 168 (class 1259 OID 48622)
-- Name: context; Type: TABLE; Schema: public; Owner: nlp
--

CREATE TABLE context (
    context_id integer NOT NULL,
    context_start_row integer,
    context_start_column integer,
    context_text character varying(4000) NOT NULL,
    context_file_id integer NOT NULL,
    context_term_id integer NOT NULL,
    context_end_row integer,
    context_end_column integer
);


ALTER TABLE public.context OWNER TO nlp;

--
-- TOC entry 169 (class 1259 OID 48628)
-- Name: file; Type: TABLE; Schema: public; Owner: nlp
--

CREATE TABLE file (
    file_id integer NOT NULL,
    file_name character varying(256) NOT NULL
);


ALTER TABLE public.file OWNER TO nlp;

--
-- TOC entry 170 (class 1259 OID 48631)
-- Name: project; Type: TABLE; Schema: public; Owner: nlp
--

CREATE TABLE project (
    project_id integer NOT NULL,
    project_code character varying(256) NOT NULL
);


ALTER TABLE public.project OWNER TO nlp;

--
-- TOC entry 178 (class 1259 OID 48735)
-- Name: projectfile; Type: TABLE; Schema: public; Owner: nlp
--

CREATE TABLE projectfile (
    projectfile_project_id integer NOT NULL,
    projectfile_file_id integer NOT NULL
);


ALTER TABLE public.projectfile OWNER TO nlp;

--
-- TOC entry 171 (class 1259 OID 48637)
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
-- TOC entry 172 (class 1259 OID 48639)
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
-- TOC entry 173 (class 1259 OID 48641)
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
-- TOC entry 174 (class 1259 OID 48645)
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
-- TOC entry 175 (class 1259 OID 48649)
-- Name: term; Type: TABLE; Schema: public; Owner: nlp
--

CREATE TABLE term (
    term_id integer NOT NULL,
    term_marked boolean DEFAULT false NOT NULL,
    term_text character varying(4000),
    term_lexical_words smallint DEFAULT 0 NOT NULL
);


ALTER TABLE public.term OWNER TO nlp;

--
-- TOC entry 176 (class 1259 OID 48657)
-- Name: term_expansions; Type: TABLE; Schema: public; Owner: nlp
--

CREATE TABLE term_expansions (
    termexp_term_id integer NOT NULL,
    termexp_expansion_id integer NOT NULL
);


ALTER TABLE public.term_expansions OWNER TO nlp;

--
-- TOC entry 177 (class 1259 OID 48660)
-- Name: term_heads; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE term_heads (
    termhead_term_id integer NOT NULL,
    termhead_head_id integer NOT NULL
);


ALTER TABLE public.term_heads OWNER TO postgres;

--
-- TOC entry 1951 (class 2606 OID 48664)
-- Name: pk_context; Type: CONSTRAINT; Schema: public; Owner: nlp
--

ALTER TABLE ONLY context
    ADD CONSTRAINT pk_context PRIMARY KEY (context_id);


--
-- TOC entry 1955 (class 2606 OID 48666)
-- Name: pk_file; Type: CONSTRAINT; Schema: public; Owner: nlp
--

ALTER TABLE ONLY file
    ADD CONSTRAINT pk_file PRIMARY KEY (file_id);


--
-- TOC entry 1959 (class 2606 OID 48668)
-- Name: pk_project; Type: CONSTRAINT; Schema: public; Owner: nlp
--

ALTER TABLE ONLY project
    ADD CONSTRAINT pk_project PRIMARY KEY (project_id);


--
-- TOC entry 1969 (class 2606 OID 48739)
-- Name: pk_projectfile; Type: CONSTRAINT; Schema: public; Owner: nlp
--

ALTER TABLE ONLY projectfile
    ADD CONSTRAINT pk_projectfile PRIMARY KEY (projectfile_project_id, projectfile_file_id);


--
-- TOC entry 1961 (class 2606 OID 48670)
-- Name: pk_term; Type: CONSTRAINT; Schema: public; Owner: nlp
--

ALTER TABLE ONLY term
    ADD CONSTRAINT pk_term PRIMARY KEY (term_id);


--
-- TOC entry 1965 (class 2606 OID 48672)
-- Name: pk_termexp; Type: CONSTRAINT; Schema: public; Owner: nlp
--

ALTER TABLE ONLY term_expansions
    ADD CONSTRAINT pk_termexp PRIMARY KEY (termexp_term_id, termexp_expansion_id);


--
-- TOC entry 1967 (class 2606 OID 48674)
-- Name: pk_termhead; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY term_heads
    ADD CONSTRAINT pk_termhead PRIMARY KEY (termhead_term_id, termhead_head_id);


--
-- TOC entry 1953 (class 2606 OID 48734)
-- Name: uk_context; Type: CONSTRAINT; Schema: public; Owner: nlp
--

ALTER TABLE ONLY context
    ADD CONSTRAINT uk_context UNIQUE (context_file_id, context_term_id, context_start_row, context_start_column);


--
-- TOC entry 1957 (class 2606 OID 48680)
-- Name: uk_file_name; Type: CONSTRAINT; Schema: public; Owner: nlp
--

ALTER TABLE ONLY file
    ADD CONSTRAINT uk_file_name UNIQUE (file_name);


--
-- TOC entry 1963 (class 2606 OID 48722)
-- Name: uk_term; Type: CONSTRAINT; Schema: public; Owner: nlp
--

ALTER TABLE ONLY term
    ADD CONSTRAINT uk_term UNIQUE (term_text);


--
-- TOC entry 1970 (class 2606 OID 48723)
-- Name: fk_context_file; Type: FK CONSTRAINT; Schema: public; Owner: nlp
--

ALTER TABLE ONLY context
    ADD CONSTRAINT fk_context_file FOREIGN KEY (context_file_id) REFERENCES file(file_id);


--
-- TOC entry 1971 (class 2606 OID 48728)
-- Name: fk_context_term; Type: FK CONSTRAINT; Schema: public; Owner: nlp
--

ALTER TABLE ONLY context
    ADD CONSTRAINT fk_context_term FOREIGN KEY (context_term_id) REFERENCES term(term_id);


--
-- TOC entry 1972 (class 2606 OID 48691)
-- Name: fk_termexp_exp; Type: FK CONSTRAINT; Schema: public; Owner: nlp
--

ALTER TABLE ONLY term_expansions
    ADD CONSTRAINT fk_termexp_exp FOREIGN KEY (termexp_expansion_id) REFERENCES term(term_id);


--
-- TOC entry 1973 (class 2606 OID 48696)
-- Name: fk_termexp_term; Type: FK CONSTRAINT; Schema: public; Owner: nlp
--

ALTER TABLE ONLY term_expansions
    ADD CONSTRAINT fk_termexp_term FOREIGN KEY (termexp_term_id) REFERENCES term(term_id);


--
-- TOC entry 1974 (class 2606 OID 48701)
-- Name: fk_termhead_head; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY term_heads
    ADD CONSTRAINT fk_termhead_head FOREIGN KEY (termhead_head_id) REFERENCES term(term_id);


--
-- TOC entry 1975 (class 2606 OID 48706)
-- Name: fk_termhead_term; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY term_heads
    ADD CONSTRAINT fk_termhead_term FOREIGN KEY (termhead_term_id) REFERENCES term(term_id);


-- Completed on 2015-01-22 17:59:30

--
-- PostgreSQL database dump complete
--

