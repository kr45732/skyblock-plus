package com.skyblockplus.api.security;

import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import static com.skyblockplus.utils.BotUtils.API_PASSWORD;
import static com.skyblockplus.utils.BotUtils.API_USERNAME;

@Service
public class AuthDiscordConfig implements CommandLineRunner {
    final AuthDiscordUserRepository discordUserRepository;
    final PasswordEncoder passwordEncoder;

    public AuthDiscordConfig(AuthDiscordUserRepository discordUserRepository, PasswordEncoder passwordEncoder) {
        this.discordUserRepository = discordUserRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        this.discordUserRepository.deleteAll();

        discordUserRepository.save(new AuthDiscordUser(API_USERNAME, passwordEncoder.encode(API_PASSWORD)));
    }

}
