/*
 * Skyblock Plus - A Skyblock focused Discord bot with many commands and customizable features to improve the experience of Skyblock players and guild staff!
 * Copyright (c) 2021 kr45732
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.skyblockplus.utils;

import static com.skyblockplus.features.listeners.AutomaticGuild.getGuildPrefix;
import static com.skyblockplus.utils.Utils.DEFAULT_PREFIX;
import static com.skyblockplus.utils.Utils.IS_API;

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
		if (IS_API) {
			return Collections.singletonList(DEFAULT_PREFIX);
		}

		return Collections.singletonList(getGuildPrefix(guild.getId()));
	}
}
