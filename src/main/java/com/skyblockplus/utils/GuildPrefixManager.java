package com.skyblockplus.utils;

import static com.skyblockplus.features.listeners.AutomaticGuild.getGuildPrefix;

import com.jagrosh.jdautilities.command.GuildSettingsProvider;
import java.util.Collection;
import java.util.Collections;
import net.dv8tion.jda.api.entities.Guild;
import org.jetbrains.annotations.Nullable;

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
