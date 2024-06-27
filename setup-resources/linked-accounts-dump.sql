-- Dumped from database version 14.1


--
-- Name: linked_account; Type: TABLE; Schema: public
--

CREATE TABLE public.linked_account (
    id integer NOT NULL,
    last_updated bigint,
    discord character varying(255),
    username character varying(255),
    uuid character varying(255)
);


--
-- Name: linked_account_id_seq; Type: SEQUENCE; Schema: public
--

CREATE SEQUENCE public.linked_account_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: linked_account_id_seq; Type: SEQUENCE OWNED BY; Schema: public
--

ALTER SEQUENCE public.linked_account_id_seq OWNED BY public.linked_account.id;


--
-- Name: linked_account id; Type: DEFAULT; Schema: public
--

ALTER TABLE ONLY public.linked_account ALTER COLUMN id SET DEFAULT nextval('public.linked_account_id_seq'::regclass);


--
-- Name: linked_account linked_account_discord_key; Type: CONSTRAINT; Schema: public
--

ALTER TABLE ONLY public.linked_account
    ADD CONSTRAINT linked_account_discord_key UNIQUE (discord);


--
-- Name: linked_account linked_account_pkey; Type: CONSTRAINT; Schema: public
--

ALTER TABLE ONLY public.linked_account
    ADD CONSTRAINT linked_account_pkey PRIMARY KEY (id);


--
-- Name: linked_account linked_account_username_key; Type: CONSTRAINT; Schema: public
--

ALTER TABLE ONLY public.linked_account
    ADD CONSTRAINT linked_account_username_key UNIQUE (username);


--
-- Name: linked_account linked_account_uuid_key; Type: CONSTRAINT; Schema: public
--

ALTER TABLE ONLY public.linked_account
    ADD CONSTRAINT linked_account_uuid_key UNIQUE (uuid);
