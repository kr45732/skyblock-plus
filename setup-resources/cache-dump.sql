-- Dumped from database version 14.4


--
-- Name: guild; Type: TABLE; Schema: public
--

CREATE TABLE public.guild (
    guild_id character varying(32) NOT NULL,
    request_time bigint NOT NULL,
    members jsonb NOT NULL,
    request_discord text NOT NULL,
    guild_name text NOT NULL
);


--
-- Name: json_cache; Type: TABLE; Schema: public
--

CREATE TABLE public.json_cache (
    id character varying(255) NOT NULL,
    expiry bigint NOT NULL,
    data jsonb NOT NULL
);


--
-- Name: json_storage; Type: TABLE; Schema: public
--

CREATE TABLE public.json_storage (
    id integer NOT NULL,
    data jsonb NOT NULL
);


--
-- Name: haste; Type: TABLE; Schema: public
--

CREATE TABLE public.haste (
    id uuid NOT NULL DEFAULT gen_random_uuid(),
    expiry bigint NOT NULL,
    data text NOT NULL
);


--
-- Name: guild guild_pkey; Type: CONSTRAINT; Schema: public
--

ALTER TABLE ONLY public.guild
    ADD CONSTRAINT guild_pkey PRIMARY KEY (guild_id);


--
-- Name: json_cache json_cache_pkey; Type: CONSTRAINT; Schema: public
--

ALTER TABLE ONLY public.json_cache
    ADD CONSTRAINT json_cache_pkey PRIMARY KEY (id);


--
-- Name: json_storage json_storage_pkey; Type: CONSTRAINT; Schema: public
--

ALTER TABLE ONLY public.json_storage
    ADD CONSTRAINT json_storage_pkey PRIMARY KEY (id);


--
-- Name: haste haste_pkey; Type: CONSTRAINT; Schema: public
--

ALTER TABLE ONLY public.haste
    ADD CONSTRAINT haste_pkey PRIMARY KEY (id);