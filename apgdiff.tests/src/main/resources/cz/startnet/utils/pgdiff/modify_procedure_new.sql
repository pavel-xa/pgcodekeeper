--
-- PostgreSQL database dump
--

-- Dumped from database version 11.0 (Ubuntu 11.0-1.pgdg16.04+2)
-- Dumped by pg_dump version 11.0 (Ubuntu 11.0-1.pgdg16.04+2)

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET client_min_messages = warning;
SET row_security = off;

--
-- Name: insert_data(integer, integer); Type: PROCEDURE; Schema: public; Owner: shamsutdinov_lr
--

CREATE PROCEDURE public.insert_data(a integer, b integer)
    LANGUAGE sql
    AS $$
INSERT INTO tbl VALUES (a);
$$;


ALTER PROCEDURE public.insert_data(a integer, b integer) OWNER TO shamsutdinov_lr;

--
-- Name: insert_data(integer, integer, integer); Type: PROCEDURE; Schema: public; Owner: shamsutdinov_lr
--

CREATE PROCEDURE public.insert_data(a integer, b integer, c integer)
    LANGUAGE sql
    AS $$
INSERT INTO tbl VALUES (a);
INSERT INTO tbl VALUES (b);
INSERT INTO tbl VALUES (c);
$$;

ALTER PROCEDURE public.insert_data(a integer, b integer, c integer) OWNER TO shamsutdinov_lr;

SET default_tablespace = '';

SET default_with_oids = false;

--
-- Name: tbl; Type: TABLE; Schema: public; Owner: shamsutdinov_lr
--

CREATE TABLE public.tbl (
    c1 integer
);


ALTER TABLE public.tbl OWNER TO shamsutdinov_lr;

--
-- PostgreSQL database dump complete
--

