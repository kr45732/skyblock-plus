package com.skyblockplus.api.security;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AuthDiscordUserRepository extends JpaRepository<AuthDiscordUser, Long> {
    AuthDiscordUser findByDiscordId(String discordId);

    void deleteByDiscordId(String discordId);
}