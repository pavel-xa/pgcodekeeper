SET search_path = pg_catalog;

REVOKE ALL ON SCHEMA test_schema FROM maindb;

-- SCHEMA test_schema GRANT

REVOKE ALL ON SCHEMA test_schema FROM PUBLIC;
REVOKE ALL ON SCHEMA test_schema FROM botov_av;
GRANT ALL ON SCHEMA test_schema TO botov_av;
GRANT ALL ON SCHEMA test_schema TO fordfrog;

REVOKE ALL ON TYPE public.typ_composite FROM maindb;

-- TYPE public.typ_composite GRANT

REVOKE ALL ON TYPE public.typ_composite FROM PUBLIC;
REVOKE ALL ON TYPE public.typ_composite FROM botov_av;
GRANT ALL ON TYPE public.typ_composite TO botov_av;
GRANT ALL ON TYPE public.typ_composite TO PUBLIC;
GRANT ALL ON TYPE public.typ_composite TO fordfrog;

REVOKE ALL ON TYPE public.dom FROM maindb;

-- DOMAIN public.dom GRANT

REVOKE ALL ON TYPE public.dom FROM PUBLIC;
REVOKE ALL ON TYPE public.dom FROM botov_av;
GRANT ALL ON TYPE public.dom TO botov_av;
GRANT ALL ON TYPE public.dom TO PUBLIC;
GRANT ALL ON TYPE public.dom TO fordfrog;

REVOKE ALL ON SEQUENCE public.test_id_seq FROM maindb;

-- SEQUENCE public.test_id_seq GRANT

REVOKE ALL ON SEQUENCE public.test_id_seq FROM PUBLIC;
REVOKE ALL ON SEQUENCE public.test_id_seq FROM botov_av;
GRANT ALL ON SEQUENCE public.test_id_seq TO botov_av;
GRANT ALL ON SEQUENCE public.test_id_seq TO fordfrog;

REVOKE ALL ON FUNCTION public.test_fnc(arg character varying) FROM maindb;

-- FUNCTION public.test_fnc(character varying) GRANT

REVOKE ALL ON FUNCTION public.test_fnc(arg character varying) FROM PUBLIC;
REVOKE ALL ON FUNCTION public.test_fnc(arg character varying) FROM botov_av;
GRANT ALL ON FUNCTION public.test_fnc(arg character varying) TO botov_av;
GRANT ALL ON FUNCTION public.test_fnc(arg character varying) TO PUBLIC;
GRANT ALL ON FUNCTION public.test_fnc(arg character varying) TO fordfrog;

REVOKE ALL ON FUNCTION public.trigger_fnc() FROM maindb CASCADE;

-- FUNCTION public.trigger_fnc() GRANT

REVOKE ALL ON FUNCTION public.trigger_fnc() FROM PUBLIC;
REVOKE ALL ON FUNCTION public.trigger_fnc() FROM botov_av;
GRANT ALL ON FUNCTION public.trigger_fnc() TO botov_av;
GRANT ALL ON FUNCTION public.trigger_fnc() TO PUBLIC;
GRANT ALL ON FUNCTION public.trigger_fnc() TO fordfrog;

REVOKE ALL ON TABLE public.test FROM maindb;

-- TABLE public.test GRANT

REVOKE ALL ON TABLE public.test FROM PUBLIC;
REVOKE ALL ON TABLE public.test FROM botov_av;
GRANT ALL ON TABLE public.test TO botov_av;
GRANT ALL ON TABLE public.test TO fordfrog;

REVOKE ALL(id) ON TABLE public.test FROM maindb;

-- COLUMN public.test.id GRANT

REVOKE ALL(id) ON TABLE public.test FROM PUBLIC;
REVOKE ALL(id) ON TABLE public.test FROM botov_av;
GRANT ALL(id) ON TABLE public.test TO fordfrog;

REVOKE ALL ON TABLE public.test_view FROM maindb;

-- VIEW public.test_view GRANT

REVOKE ALL ON TABLE public.test_view FROM PUBLIC;
REVOKE ALL ON TABLE public.test_view FROM botov_av;
GRANT ALL ON TABLE public.test_view TO botov_av;
GRANT ALL ON TABLE public.test_view TO fordfrog;