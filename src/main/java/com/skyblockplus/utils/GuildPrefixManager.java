package com.skyblockplus.utils;

import com.jagrosh.jdautilities.command.GuildSettingsProvider;
import net.dv8tion.jda.api.entities.Guild;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;

import static com.skyblockplus.eventlisteners.AutomaticGuild.getGuildPrefix;

public class GuildPrefixManager implements GuildSettingsProvider {
        private final Guild guild;

        public GuildPrefixManager(Guild guild) {
            this.guild = guild;
        }

        @Nullable
        @Override
        public Collection<String> getPrefixes() {
            return Collections.singletonList(getGuildPrefix(guild.getId()));
        }
}
