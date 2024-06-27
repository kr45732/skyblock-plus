-- Dumped from database version 14.1


--
-- Name: apply_requirement; Type: TABLE; Schema: public
--

CREATE TABLE public.apply_requirement (
    id bigint NOT NULL,
    automated_guild_id bigint NOT NULL
);


--
-- Name: apply_requirement_requirements; Type: TABLE; Schema: public
--

CREATE TABLE public.apply_requirement_requirements (
    apply_requirement_id bigint NOT NULL,
    requirements character varying(255),
    requirements_key character varying(255) NOT NULL
);


--
-- Name: apply_requirement_seq; Type: SEQUENCE; Schema: public
--

CREATE SEQUENCE public.apply_requirement_seq
    START WITH 1
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: automated_guild; Type: TABLE; Schema: public
--

CREATE TABLE public.automated_guild (
    id bigint NOT NULL,
    apply_accept_message character varying(2048),
    apply_category character varying(255),
    apply_check_api character varying(255),
    apply_deny_message character varying(2048),
    apply_enable character varying(255),
    apply_gamemode character varying(255),
    apply_message character varying(2048),
    apply_message_channel character varying(255),
    apply_prev_message character varying(255),
    apply_scammer_check character varying(255),
    apply_staff_channel character varying(255),
    apply_users_cache text,
    apply_waiting_channel character varying(255),
    apply_waitlist_message character varying(2048),
    guild_counter_channel character varying(255),
    guild_counter_enable character varying(255),
    guild_id character varying(255),
    guild_member_role character varying(255),
    guild_member_role_enable character varying(255),
    guild_name character varying(255),
    guild_ranks_enable character varying(255),
    server_settings_id bigint NOT NULL,
    apply_closed character varying(255)
);


--
-- Name: automated_guild_apply_staff_roles; Type: TABLE; Schema: public
--

CREATE TABLE public.automated_guild_apply_staff_roles (
    automated_guild_id bigint NOT NULL,
    apply_staff_roles character varying(255)
);


--
-- Name: automated_guild_guild_ranks; Type: TABLE; Schema: public
--

CREATE TABLE public.automated_guild_guild_ranks (
    automated_guild_id bigint NOT NULL,
    guild_ranks_role_id character varying(255),
    guild_ranks_value character varying(255)
);


--
-- Name: automated_guild_seq; Type: SEQUENCE; Schema: public
--

CREATE SEQUENCE public.automated_guild_seq
    START WITH 1
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: hibernate_sequence; Type: SEQUENCE; Schema: public
--

CREATE SEQUENCE public.hibernate_sequence
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: hibernate_sequences; Type: TABLE; Schema: public
--

CREATE TABLE public.hibernate_sequences (
    sequence_name character varying(255) NOT NULL,
    next_val bigint
);


--
-- Name: role_model; Type: TABLE; Schema: public
--

CREATE TABLE public.role_model (
    id bigint NOT NULL,
    name character varying(255),
    server_settings_id bigint NOT NULL
);


--
-- Name: role_model_levels; Type: TABLE; Schema: public
--

CREATE TABLE public.role_model_levels (
    role_model_id bigint NOT NULL,
    levels_role_id character varying(255),
    levels_value character varying(255)
);


--
-- Name: role_model_seq; Type: SEQUENCE; Schema: public
--

CREATE SEQUENCE public.role_model_seq
    START WITH 1
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: server_settings_model; Type: TABLE; Schema: public
--

CREATE TABLE public.server_settings_model (
    id bigint NOT NULL,
    apply_guest_role character varying(255),
    automated_roles_use_highest character varying(255),
    automated_verify_dm_on_sync character varying(255),
    automated_verify_enable character varying(255),
    automated_verify_enable_automatic_sync character varying(255),
    automated_verify_enable_roles_claim character varying(255),
    automated_verify_enable_verify_video character varying(255),
    automated_verify_message_text character varying(2048),
    automated_verify_message_text_channel_id character varying(255),
    automated_verify_previous_message_id character varying(255),
    automated_verify_verified_nickname character varying(255),
    automated_verify_verified_remove_role character varying(255),
    event_notif_enable character varying(255),
    fetchur_channel character varying(255),
    fetchur_role character varying(255),
    jacob_settings_channel character varying(255),
    jacob_settings_enable character varying(255),
    log_channel character varying(255),
    mayor_channel character varying(255),
    mayor_role character varying(255),
    sb_event_announcement_id character varying(255),
    sb_event_announcement_message_id character varying(255),
    sb_event_event_guild_id character varying(255),
    sb_event_event_type character varying(255),
    sb_event_max_amount character varying(255),
    sb_event_min_amount character varying(255),
    sb_event_time_ending_seconds character varying(255),
    sb_event_whitelist_role character varying(255),
    server_id character varying(255),
    server_name character varying(255),
    automated_roles_enable character varying(255),
    automated_roles_enable_automatic_sync character varying(255),
    sync_unlinked_members character varying(255)
);


--
-- Name: server_settings_model_automated_verify_verified_roles; Type: TABLE; Schema: public
--

CREATE TABLE public.server_settings_model_automated_verify_verified_roles (
    server_settings_model_id bigint NOT NULL,
    automated_verify_verified_roles character varying(255)
);


--
-- Name: server_settings_model_blacklist_blacklist; Type: TABLE; Schema: public
--

CREATE TABLE public.server_settings_model_blacklist_blacklist (
    server_settings_model_id bigint NOT NULL,
    blacklist_blacklist_reason character varying(255),
    blacklist_blacklist_username character varying(255),
    blacklist_blacklist_uuid character varying(255)
);


--
-- Name: server_settings_model_blacklist_can_use; Type: TABLE; Schema: public
--

CREATE TABLE public.server_settings_model_blacklist_can_use (
    server_settings_model_id bigint NOT NULL,
    blacklist_can_use character varying(255)
);


--
-- Name: server_settings_model_blacklist_features; Type: TABLE; Schema: public
--

CREATE TABLE public.server_settings_model_blacklist_features (
    server_settings_model_id bigint NOT NULL,
    blacklist_features character varying(255)
);


--
-- Name: server_settings_model_blacklist_is_using; Type: TABLE; Schema: public
--

CREATE TABLE public.server_settings_model_blacklist_is_using (
    server_settings_model_id bigint NOT NULL,
    blacklist_is_using character varying(255)
);


--
-- Name: server_settings_model_bot_manager_roles; Type: TABLE; Schema: public
--

CREATE TABLE public.server_settings_model_bot_manager_roles (
    server_settings_model_id bigint NOT NULL,
    bot_manager_roles character varying(255)
);


--
-- Name: server_settings_model_event_notif_events; Type: TABLE; Schema: public
--

CREATE TABLE public.server_settings_model_event_notif_events (
    server_settings_model_id bigint NOT NULL,
    event_notif_events_channel_id character varying(255),
    event_notif_events_role_id character varying(255),
    event_notif_events_value character varying(255)
);


--
-- Name: server_settings_model_jacob_settings_crops; Type: TABLE; Schema: public
--

CREATE TABLE public.server_settings_model_jacob_settings_crops (
    server_settings_model_id bigint NOT NULL,
    jacob_settings_crops_role_id character varying(255),
    jacob_settings_crops_value character varying(255)
);


--
-- Name: server_settings_model_log_events; Type: TABLE; Schema: public
--

CREATE TABLE public.server_settings_model_log_events (
    server_settings_model_id bigint NOT NULL,
    log_events character varying(255)
);


--
-- Name: server_settings_model_sb_event_members_list; Type: TABLE; Schema: public
--

CREATE TABLE public.server_settings_model_sb_event_members_list (
    server_settings_model_id bigint NOT NULL,
    sb_event_members_list_profile_name character varying(255),
    sb_event_members_list_starting_amount character varying(255),
    sb_event_members_list_username character varying(255),
    sb_event_members_list_uuid character varying(255)
);


--
-- Name: server_settings_model_sb_event_prize_map; Type: TABLE; Schema: public
--

CREATE TABLE public.server_settings_model_sb_event_prize_map (
    server_settings_model_id bigint NOT NULL,
    sb_event_prize_map character varying(255),
    prize_map_key integer NOT NULL
);


--
-- Name: server_settings_model_seq; Type: SEQUENCE; Schema: public
--

CREATE SEQUENCE public.server_settings_model_seq
    START WITH 1
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: apply_requirement apply_requirement_pkey; Type: CONSTRAINT; Schema: public
--

ALTER TABLE ONLY public.apply_requirement
    ADD CONSTRAINT apply_requirement_pkey PRIMARY KEY (id);


--
-- Name: apply_requirement_requirements apply_requirement_requirements_pkey; Type: CONSTRAINT; Schema: public
--

ALTER TABLE ONLY public.apply_requirement_requirements
    ADD CONSTRAINT apply_requirement_requirements_pkey PRIMARY KEY (apply_requirement_id, requirements_key);


--
-- Name: automated_guild automated_guild_pkey; Type: CONSTRAINT; Schema: public
--

ALTER TABLE ONLY public.automated_guild
    ADD CONSTRAINT automated_guild_pkey PRIMARY KEY (id);


--
-- Name: hibernate_sequences hibernate_sequences_pkey; Type: CONSTRAINT; Schema: public
--

ALTER TABLE ONLY public.hibernate_sequences
    ADD CONSTRAINT hibernate_sequences_pkey PRIMARY KEY (sequence_name);


--
-- Name: role_model role_model_pkey; Type: CONSTRAINT; Schema: public
--

ALTER TABLE ONLY public.role_model
    ADD CONSTRAINT role_model_pkey PRIMARY KEY (id);


--
-- Name: server_settings_model server_settings_model_pkey; Type: CONSTRAINT; Schema: public
--

ALTER TABLE ONLY public.server_settings_model
    ADD CONSTRAINT server_settings_model_pkey PRIMARY KEY (id);


--
-- Name: server_settings_model_sb_event_prize_map server_settings_model_sb_event_prize_map_pkey; Type: CONSTRAINT; Schema: public
--

ALTER TABLE ONLY public.server_settings_model_sb_event_prize_map
    ADD CONSTRAINT server_settings_model_sb_event_prize_map_pkey PRIMARY KEY (server_settings_model_id, prize_map_key);


--
-- Name: apply_requirement_requirements fk3pnowfox24h3fpak6hm91usq0; Type: FK CONSTRAINT; Schema: public
--

ALTER TABLE ONLY public.apply_requirement_requirements
    ADD CONSTRAINT fk3pnowfox24h3fpak6hm91usq0 FOREIGN KEY (apply_requirement_id) REFERENCES public.apply_requirement(id);


--
-- Name: server_settings_model_blacklist_blacklist fk4v1wg3gehyvt0apdmvrfrl7jv; Type: FK CONSTRAINT; Schema: public
--

ALTER TABLE ONLY public.server_settings_model_blacklist_blacklist
    ADD CONSTRAINT fk4v1wg3gehyvt0apdmvrfrl7jv FOREIGN KEY (server_settings_model_id) REFERENCES public.server_settings_model(id);


--
-- Name: role_model fk6882lq77kkfrifts5gs1xoq7b; Type: FK CONSTRAINT; Schema: public
--

ALTER TABLE ONLY public.role_model
    ADD CONSTRAINT fk6882lq77kkfrifts5gs1xoq7b FOREIGN KEY (server_settings_id) REFERENCES public.server_settings_model(id);


--
-- Name: apply_requirement fk6cvgixuuxwvo0we9tw5w0c05a; Type: FK CONSTRAINT; Schema: public
--

ALTER TABLE ONLY public.apply_requirement
    ADD CONSTRAINT fk6cvgixuuxwvo0we9tw5w0c05a FOREIGN KEY (automated_guild_id) REFERENCES public.automated_guild(id);


--
-- Name: server_settings_model_blacklist_is_using fk6q3p6k50ss9pmmbo35o5uyd4l; Type: FK CONSTRAINT; Schema: public
--

ALTER TABLE ONLY public.server_settings_model_blacklist_is_using
    ADD CONSTRAINT fk6q3p6k50ss9pmmbo35o5uyd4l FOREIGN KEY (server_settings_model_id) REFERENCES public.server_settings_model(id);


--
-- Name: automated_guild_apply_staff_roles fk6yg3lgsowe4b2etli1a9e44q4; Type: FK CONSTRAINT; Schema: public
--

ALTER TABLE ONLY public.automated_guild_apply_staff_roles
    ADD CONSTRAINT fk6yg3lgsowe4b2etli1a9e44q4 FOREIGN KEY (automated_guild_id) REFERENCES public.automated_guild(id);


--
-- Name: server_settings_model_log_events fk9hkhi1g0wpgs9ntu3ygma85rq; Type: FK CONSTRAINT; Schema: public
--

ALTER TABLE ONLY public.server_settings_model_log_events
    ADD CONSTRAINT fk9hkhi1g0wpgs9ntu3ygma85rq FOREIGN KEY (server_settings_model_id) REFERENCES public.server_settings_model(id);


--
-- Name: server_settings_model_event_notif_events fkboq3k0qq548jibo6fxlaq2wtg; Type: FK CONSTRAINT; Schema: public
--

ALTER TABLE ONLY public.server_settings_model_event_notif_events
    ADD CONSTRAINT fkboq3k0qq548jibo6fxlaq2wtg FOREIGN KEY (server_settings_model_id) REFERENCES public.server_settings_model(id);


--
-- Name: server_settings_model_sb_event_members_list fkekrl0m5bceww9g96yo7tj5h4l; Type: FK CONSTRAINT; Schema: public
--

ALTER TABLE ONLY public.server_settings_model_sb_event_members_list
    ADD CONSTRAINT fkekrl0m5bceww9g96yo7tj5h4l FOREIGN KEY (server_settings_model_id) REFERENCES public.server_settings_model(id);


--
-- Name: server_settings_model_bot_manager_roles fkex5o4t0ps3ylyl3igywpt1m19; Type: FK CONSTRAINT; Schema: public
--

ALTER TABLE ONLY public.server_settings_model_bot_manager_roles
    ADD CONSTRAINT fkex5o4t0ps3ylyl3igywpt1m19 FOREIGN KEY (server_settings_model_id) REFERENCES public.server_settings_model(id);


--
-- Name: server_settings_model_blacklist_can_use fkf85sv09mud1s084wyghnan5y3; Type: FK CONSTRAINT; Schema: public
--

ALTER TABLE ONLY public.server_settings_model_blacklist_can_use
    ADD CONSTRAINT fkf85sv09mud1s084wyghnan5y3 FOREIGN KEY (server_settings_model_id) REFERENCES public.server_settings_model(id);


--
-- Name: role_model_levels fkkm75mav7u6sumwijtlal5m4yo; Type: FK CONSTRAINT; Schema: public
--

ALTER TABLE ONLY public.role_model_levels
    ADD CONSTRAINT fkkm75mav7u6sumwijtlal5m4yo FOREIGN KEY (role_model_id) REFERENCES public.role_model(id);


--
-- Name: server_settings_model_automated_verify_verified_roles fkkphac3ya6m60aw9jmb0or5dg1; Type: FK CONSTRAINT; Schema: public
--

ALTER TABLE ONLY public.server_settings_model_automated_verify_verified_roles
    ADD CONSTRAINT fkkphac3ya6m60aw9jmb0or5dg1 FOREIGN KEY (server_settings_model_id) REFERENCES public.server_settings_model(id);


--
-- Name: automated_guild_guild_ranks fkmg2b6wjjbf9kluv5vbrff3uuq; Type: FK CONSTRAINT; Schema: public
--

ALTER TABLE ONLY public.automated_guild_guild_ranks
    ADD CONSTRAINT fkmg2b6wjjbf9kluv5vbrff3uuq FOREIGN KEY (automated_guild_id) REFERENCES public.automated_guild(id);


--
-- Name: server_settings_model_jacob_settings_crops fkoslo8rg4iltnt5obds5u8hxpj; Type: FK CONSTRAINT; Schema: public
--

ALTER TABLE ONLY public.server_settings_model_jacob_settings_crops
    ADD CONSTRAINT fkoslo8rg4iltnt5obds5u8hxpj FOREIGN KEY (server_settings_model_id) REFERENCES public.server_settings_model(id);


--
-- Name: automated_guild fkqfrsgorlwe3x0bi18oww4t4v; Type: FK CONSTRAINT; Schema: public
--

ALTER TABLE ONLY public.automated_guild
    ADD CONSTRAINT fkqfrsgorlwe3x0bi18oww4t4v FOREIGN KEY (server_settings_id) REFERENCES public.server_settings_model(id);


--
-- Name: server_settings_model_sb_event_prize_map fkqp14wvlfwmkbr6349jawwrw4h; Type: FK CONSTRAINT; Schema: public
--

ALTER TABLE ONLY public.server_settings_model_sb_event_prize_map
    ADD CONSTRAINT fkqp14wvlfwmkbr6349jawwrw4h FOREIGN KEY (server_settings_model_id) REFERENCES public.server_settings_model(id);


--
-- Name: server_settings_model_blacklist_features fks41nargnw7tiwub9mfpubpxb5; Type: FK CONSTRAINT; Schema: public
--

ALTER TABLE ONLY public.server_settings_model_blacklist_features
    ADD CONSTRAINT fks41nargnw7tiwub9mfpubpxb5 FOREIGN KEY (server_settings_model_id) REFERENCES public.server_settings_model(id);