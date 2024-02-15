--
-- PostgreSQL database dump
--

-- Dumped from database version 15.5 (Debian 15.5-0+deb12u1)
-- Dumped by pg_dump version 15.5 (Debian 15.5-0+deb12u1)

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

--
-- Name: todefer; Type: SCHEMA; Schema: -; Owner: -
--

CREATE SCHEMA todefer;


--
-- Name: pagetype; Type: TYPE; Schema: public; Owner: -
--

CREATE TYPE public.pagetype AS ENUM (
    'task',
    'habit'
);


--
-- Name: timeunit; Type: TYPE; Schema: public; Owner: -
--

CREATE TYPE public.timeunit AS ENUM (
    'days',
    'weeks',
    'months',
    'years'
);


SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: apppage; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.apppage (
    page_id integer NOT NULL,
    page_name text NOT NULL,
    order_key integer NOT NULL,
    page_type public.pagetype NOT NULL
);


--
-- Name: apppage_order_key_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.apppage_order_key_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: apppage_order_key_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.apppage_order_key_seq OWNED BY public.apppage.order_key;


--
-- Name: apppage_page_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.apppage_page_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: apppage_page_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.apppage_page_id_seq OWNED BY public.apppage.page_id;


--
-- Name: defcatdated; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.defcatdated (
    cat_id integer NOT NULL,
    def_date date,
    order_key integer NOT NULL
);


--
-- Name: defcatdated_cat_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.defcatdated_cat_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: defcatdated_cat_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.defcatdated_cat_id_seq OWNED BY public.defcatdated.cat_id;


--
-- Name: defcatdated_order_key_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.defcatdated_order_key_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: defcatdated_order_key_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.defcatdated_order_key_seq OWNED BY public.defcatdated.order_key;


--
-- Name: defcatnamed; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.defcatnamed (
    cat_id integer NOT NULL,
    cat_name text,
    order_key integer NOT NULL
);


--
-- Name: deferredcategory_cat_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.deferredcategory_cat_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: deferredcategory_cat_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.deferredcategory_cat_id_seq OWNED BY public.defcatnamed.cat_id;


--
-- Name: deferredcategory_order_key_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.deferredcategory_order_key_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: deferredcategory_order_key_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.deferredcategory_order_key_seq OWNED BY public.defcatnamed.order_key;


--
-- Name: habit; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.habit (
    habit_id integer NOT NULL,
    habit_name text NOT NULL,
    page_ref integer,
    freq_unit public.timeunit,
    freq_value integer,
    date_scheduled date DEFAULT CURRENT_DATE,
    last_done date,
    highlight text,
    sort_id integer
);


--
-- Name: habit_habit_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.habit_habit_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: habit_habit_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.habit_habit_id_seq OWNED BY public.habit.habit_id;


--
-- Name: schema_migrations; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.schema_migrations (
    id bigint NOT NULL,
    applied timestamp without time zone,
    description character varying(1024)
);


--
-- Name: task; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.task (
    task_id integer NOT NULL,
    task_name text NOT NULL,
    page_ref integer,
    defcat_named integer,
    defcat_dated integer,
    highlight text,
    sort_id integer
);


--
-- Name: task_task_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.task_task_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: task_task_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.task_task_id_seq OWNED BY public.task.task_id;


--
-- Name: users; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.users (
    login text NOT NULL,
    password text NOT NULL,
    created_at timestamp without time zone DEFAULT now() NOT NULL
);


--
-- Name: apppage page_id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.apppage ALTER COLUMN page_id SET DEFAULT nextval('public.apppage_page_id_seq'::regclass);


--
-- Name: apppage order_key; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.apppage ALTER COLUMN order_key SET DEFAULT nextval('public.apppage_order_key_seq'::regclass);


--
-- Name: defcatdated cat_id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.defcatdated ALTER COLUMN cat_id SET DEFAULT nextval('public.defcatdated_cat_id_seq'::regclass);


--
-- Name: defcatdated order_key; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.defcatdated ALTER COLUMN order_key SET DEFAULT nextval('public.defcatdated_order_key_seq'::regclass);


--
-- Name: defcatnamed cat_id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.defcatnamed ALTER COLUMN cat_id SET DEFAULT nextval('public.deferredcategory_cat_id_seq'::regclass);


--
-- Name: defcatnamed order_key; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.defcatnamed ALTER COLUMN order_key SET DEFAULT nextval('public.deferredcategory_order_key_seq'::regclass);


--
-- Name: habit habit_id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.habit ALTER COLUMN habit_id SET DEFAULT nextval('public.habit_habit_id_seq'::regclass);


--
-- Name: task task_id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.task ALTER COLUMN task_id SET DEFAULT nextval('public.task_task_id_seq'::regclass);


--
-- Data for Name: apppage; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.apppage (page_id, page_name, order_key, page_type) FROM stdin;
1	Lorem ipsum	0	task
13	Dolor sit amet	3	habit
10	Consectetur adipiscing	1	habit
\.


--
-- Data for Name: defcatdated; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.defcatdated (cat_id, def_date, order_key) FROM stdin;
65	2023-03-29	65
4	2022-12-10	4
5	2022-11-21	5
7	2022-11-28	7
22	2023-01-07	22
81	2023-05-01	81
31	2024-01-01	31
36	2023-02-26	36
93	2023-05-10	93
96	2024-04-15	96
103	2023-07-01	103
50	2023-03-02	50
118	2026-08-13	118
119	2023-09-18	119
121	2033-02-05	121
140	2024-01-25	140
141	2024-02-09	141
143	2024-03-19	143
148	2024-02-15	148
\.


--
-- Data for Name: defcatnamed; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.defcatnamed (cat_id, cat_name, order_key) FROM stdin;
1	Lorem ipsum	1
2	Dolor sit	2
3	Amet consectetur	3
4	Adipiscing elit	4
5	Sed do	5
6	Eiusmod tempor	6
7	Incididunt ut	7
8	Labore et	8
9	Dolore magna	9
10	Aliqua Ut	10
11	Enim ad	11
12	Minim veniam	12
13	Quis nostrud	13
\.


--
-- Data for Name: habit; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.habit (habit_id, habit_name, page_ref, freq_unit, freq_value, date_scheduled, last_done, highlight, sort_id) FROM stdin;
39	Lorem ipsum	10	days	4	2024-02-13	2024-02-09	\N	\N
2	Dolor sit	10	weeks	1	2024-02-17	2024-02-10	\N	\N
45	Amet consectetur	10	months	2	2024-03-02	\N	\N	\N
25	Adipiscing elit	13	weeks	1	2024-02-17	2024-02-10	\N	\N
36	Sed do	10	days	3	2024-02-17	2024-02-14	\N	0
31	Eiusmod tempor	10	weeks	1	2024-02-21	2024-02-14	\N	\N
4	Incididunt ut	13	months	1	2022-12-05	\N	\N	\N
42	Labore et	13	days	4	2024-01-17	\N	\N	\N
34	Dolore magna	10	weeks	1	2024-01-30	2024-01-23	\N	\N
35	Aliqua Ut	10	weeks	1	2024-02-04	2024-01-28	\N	\N
5	Enim ad	10	months	1	2023-01-24	\N	\N	\N
41	Minim veniam	10	months	1	2023-11-12	\N	\N	\N
10	Quis nostrud	10	months	1	2024-02-21	2024-01-24	\N	\N
8	Exercitation	10	months	3	2024-04-21	2024-01-29	\N	\N
38	Ullamco laboris	13	days	5	2024-02-03	2024-01-29	\N	\N
33	Nisi ut	13	weeks	1	2024-01-21	2024-01-14	\N	\N
7	Aliquip ex	13	months	1	2023-04-10	\N	\N	\N
6	Ea commodo	13	months	1	2023-05-02	\N	\N	\N
9	Consequat	13	years	2	2024-10-03	\N	\N	\N
28	Duis aute	13	months	1	2024-02-21	2024-01-24	\N	0
26	Irure dolor	10	weeks	3	2024-02-19	2024-01-29	\N	\N
37	In reprehenderit	10	days	9	2024-02-07	2024-01-29	\N	\N
27	In voluptate	10	weeks	1	2023-11-18	2023-11-12	\N	\N
24	Velit esse	10	months	1	2023-07-14	\N	\N	\N
44	Cillum dolore	10	weeks	2	2024-02-03	\N	\N	\N
46	Eu fugiat	10	months	1	2024-02-04	\N	\N	\N
43	Nulla pariatur	10	weeks	1	2024-02-16	2024-02-09	\N	\N
\.


--
-- Data for Name: schema_migrations; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.schema_migrations (id, applied, description) FROM stdin;
20221022121300	2022-11-06 18:24:55.284	initial-models
20221029163813	2022-11-06 18:24:55.295	date-type
20221030072254	2022-11-06 18:24:55.307	refactor-deferred-category
20221105063616	2022-11-06 18:24:55.32	reference-deletes
20221106105630	2022-11-06 18:24:55.332	habit-date-type
20221106113341	2022-11-06 18:24:55.34	rename-hpage-ref
20221106161052	2022-11-06 18:24:55.354	user
20221127162806	2022-11-27 17:49:25.545	add-highlight
20230917055207	2023-09-17 08:19:38.994	add-task-sort
20230917083614	2023-09-17 08:58:49.633	add-habit-sort
\.


--
-- Data for Name: task; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.task (task_id, task_name, page_ref, defcat_named, defcat_dated, highlight, sort_id) FROM stdin;
1812	Lorem ipsum	1	\N	\N	lightblue	1
1797	Dolor sit	1	\N	\N	\N	\N
1799	Amet consectetur	1	\N	\N	\N	\N
1739	Adipiscing elit	1	\N	\N	\N	\N
1407	Sed do	1	\N	\N	\N	\N
1719	Eiusmod tempor	1	\N	143	\N	\N
657	Incididunt ut	1	\N	96	\N	\N
443	Labore et	1	\N	118	\N	\N
1674	Dolore magna	1	\N	\N	\N	\N
1786	Aliqua Ut	1	\N	\N	\N	\N
1788	Enim ad	1	\N	\N	\N	\N
1790	Minim veniam	1	\N	\N	\N	\N
1759	Quis nostrud	1	\N	148	\N	\N
1806	Exercitation ullamco	1	\N	\N	lightblue	1
1809	Laboris nisi	1	\N	\N	\N	\N
1811	Ut aliquip	1	\N	\N	lightblue	1
1798	Ex ea	1	\N	\N	\N	\N
1781	Commodo consequat	1	\N	\N	\N	\N
1735	Duis aute	1	\N	\N	\N	\N
1262	Irure dolor	1	\N	121	\N	\N
1526	Lorem ipsum	1	13	\N	\N	\N
1527	Dolor sit	1	13	\N	\N	\N
1787	Amet consectetur	1	\N	\N	\N	\N
1789	Adipiscing elit	1	\N	\N	\N	\N
1791	Sed do	1	\N	\N	\N	\N
1794	Eiusmod tempor	1	\N	\N	\N	\N
1804	Incididunt ut	1	\N	\N	\N	\N
1810	Labore et	1	\N	\N	\N	\N
\.


--
-- Data for Name: users; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.users (login, password, created_at) FROM stdin;
joseph	redacted	2022-11-06 18:36:13.080093
\.


--
-- Name: apppage_order_key_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.apppage_order_key_seq', 13, true);


--
-- Name: apppage_page_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.apppage_page_id_seq', 13, true);


--
-- Name: defcatdated_cat_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.defcatdated_cat_id_seq', 148, true);


--
-- Name: defcatdated_order_key_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.defcatdated_order_key_seq', 148, true);


--
-- Name: deferredcategory_cat_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.deferredcategory_cat_id_seq', 13, true);


--
-- Name: deferredcategory_order_key_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.deferredcategory_order_key_seq', 13, true);


--
-- Name: habit_habit_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.habit_habit_id_seq', 46, true);


--
-- Name: task_task_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.task_task_id_seq', 1812, true);


--
-- Name: apppage apppage_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.apppage
    ADD CONSTRAINT apppage_pkey PRIMARY KEY (page_id);


--
-- Name: defcatdated defcatdated_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.defcatdated
    ADD CONSTRAINT defcatdated_pkey PRIMARY KEY (cat_id);


--
-- Name: defcatnamed deferredcategory_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.defcatnamed
    ADD CONSTRAINT deferredcategory_pkey PRIMARY KEY (cat_id);


--
-- Name: habit habit_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.habit
    ADD CONSTRAINT habit_pkey PRIMARY KEY (habit_id);


--
-- Name: schema_migrations schema_migrations_id_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.schema_migrations
    ADD CONSTRAINT schema_migrations_id_key UNIQUE (id);


--
-- Name: task task_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.task
    ADD CONSTRAINT task_pkey PRIMARY KEY (task_id);


--
-- Name: users users_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_pkey PRIMARY KEY (login);


--
-- Name: habit habit_hpage_ref_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.habit
    ADD CONSTRAINT habit_hpage_ref_fkey FOREIGN KEY (page_ref) REFERENCES public.apppage(page_id);


--
-- Name: task task_defcat_dated_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.task
    ADD CONSTRAINT task_defcat_dated_fkey FOREIGN KEY (defcat_dated) REFERENCES public.defcatdated(cat_id) ON DELETE SET DEFAULT;


--
-- Name: task task_defcat_ref_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.task
    ADD CONSTRAINT task_defcat_ref_fkey FOREIGN KEY (defcat_named) REFERENCES public.defcatnamed(cat_id) ON DELETE SET DEFAULT;


--
-- Name: task task_page_ref_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.task
    ADD CONSTRAINT task_page_ref_fkey FOREIGN KEY (page_ref) REFERENCES public.apppage(page_id);


--
-- PostgreSQL database dump complete
--

