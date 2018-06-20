SET search_path = pg_catalog;

-- DEPCY: This VIEW depends on the COLUMN: t1.c1

DROP VIEW public.v1;

ALTER TABLE public.t1
	ALTER COLUMN c1 TYPE bigInt USING c1::bigInt; /* TYPE change - table: t1 original: integer new: bigInt */

ALTER TABLE public.t2
	ALTER COLUMN c4 TYPE bigInt USING c4::bigInt; /* TYPE change - table: t2 original: integer new: bigInt */

CREATE VIEW public.v1 AS
	SELECT * FROM public.t1, (SELECT * FROM public.t2) q1;
