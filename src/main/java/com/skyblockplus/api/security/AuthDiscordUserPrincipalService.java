package com.skyblockplus.api.security;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class AuthDiscordUserPrincipalService implements UserDetailsService {
    private final AuthDiscordUserRepository discordUserRepository;

    public AuthDiscordUserPrincipalService(AuthDiscordUserRepository discordUserRepository) {
        this.discordUserRepository = discordUserRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        AuthDiscordUser user = this.discordUserRepository.findByDiscordId(username);
        return new AuthDiscordUserPrincipal(user);
    }

}