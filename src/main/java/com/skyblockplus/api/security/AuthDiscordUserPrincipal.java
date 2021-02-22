package com.skyblockplus.api.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class AuthDiscordUserPrincipal implements UserDetails {
    private static final long serialVersionUID = 7639400431520003150L;
    private final AuthDiscordUser discordUserModel;

    public AuthDiscordUserPrincipal(AuthDiscordUser discordUserModel) {
        this.discordUserModel = discordUserModel;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        List<GrantedAuthority> authorities = new ArrayList<>();

        authorities.add(new SimpleGrantedAuthority(this.discordUserModel.getRoles()));

        return authorities;
    }

    @Override
    public String getPassword() {
        try {
            return this.discordUserModel.getDiscordToken();
        } catch (Exception e) {
            return "";
        }

    }

    @Override
    public String getUsername() {
        return this.discordUserModel.getDiscordId();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

}