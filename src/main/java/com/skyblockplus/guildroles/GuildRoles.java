package com.skyblockplus.guildroles;

import net.dv8tion.jda.api.events.guild.GuildReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import static com.skyblockplus.Main.jda;

public class GuildRoles extends ListenerAdapter {
    @Override
    public void onGuildReady(@NotNull GuildReadyEvent event) {
        jda.addEventListener(new GuildRolesGuild(event.getGuild()));
    }
}
