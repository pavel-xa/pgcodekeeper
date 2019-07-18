CREATE TABLE public.test_1 (
    c1 integer NOT NULL GENERATED BY DEFAULT AS IDENTITY
);

CREATE TABLE public.test_2 (
    "c / 2" integer NOT NULL GENERATED BY DEFAULT AS IDENTITY
);

CREATE TABLE public.test_3 (
    c1 integer NOT NULL GENERATED ALWAYS AS IDENTITY (SEQUENCE NAME sss)
);

CREATE TABLE public.test_4 (
    c1 integer NOT NULL GENERATED ALWAYS AS IDENTITY (SEQUENCE NAME test_seq START WITH 2 INCREMENT BY 3 MINVALUE -4 MAXVALUE 5 CACHE 1 CYCLE)
);