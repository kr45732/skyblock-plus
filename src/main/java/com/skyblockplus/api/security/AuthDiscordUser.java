package com.skyblockplus.api.security;

import lombok.Data;

import javax.persistence.*;

@Entity
@Data
public class AuthDiscordUser {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long id;

    @Column(nullable = false)
    private String discordId;

    @Column(nullable = false)
    private String discordToken;

    private String roles = "DEFAULT";

    public AuthDiscordUser() {
    }

    public AuthDiscordUser(String discordId, String discordToken) {
        this.discordId = discordId;
        this.discordToken = discordToken;
    }

}
