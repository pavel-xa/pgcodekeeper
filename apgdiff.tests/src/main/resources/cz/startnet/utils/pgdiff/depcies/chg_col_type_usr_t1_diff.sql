SET search_path = pg_catalog;

-- DEPCY: This VIEW depends on the COLUMN: t1.c1

DROP VIEW public.v1;

-- DEPCY: This DOMAIN is a dependency of COLUMN: t1.c1

CREATE DOMAIN public.dom2 AS integer
	CONSTRAINT chechk1 CHECK ((VALUE > 0));

ALTER DOMAIN public.dom2 OWNER TO botov_av;

ALTER TABLE public.t1
	ALTER COLUMN c1 TYPE public.dom2 USING c1::dom2; /* TYPE change - table: t1 original: integer new: dom2 */

CREATE VIEW public.v1 AS
	SELECT t1.c1
   FROM public.t1;

ALTER VIEW public.v1 OWNER TO botov_av;
