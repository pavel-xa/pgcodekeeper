CREATE DOMAIN public.dom2 AS integer DEFAULT (-100)
	CONSTRAINT dom2_check CHECK ((VALUE < 1000));