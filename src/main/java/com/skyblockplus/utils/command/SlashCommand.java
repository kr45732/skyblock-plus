/*
 * Skyblock Plus - A Skyblock focused Discord bot with many commands and customizable features to improve the experience of Skyblock players and guild staff!
 * Copyright (c) 2021-2022 kr45732
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

package com.skyblockplus.utils.command;

import com.skyblockplus.utils.structs.AutoCompleteEvent;
import java.util.ArrayList;
import java.util.List;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;

public abstract class SlashCommand extends AbstractSlashCommand {

	protected final List<Subcommand> subcommands = new ArrayList<>();

	protected void run(SlashCommandEvent event) {
		if (!subcommands.isEmpty() && event.getSubcommandName() != null) {
			Subcommand subcommand = getSubcommand(event.getSubcommandName());
			if (subcommand != null) {
				subcommand.run(event);
			} else {
				event.embed(event.invalidCommandMessage());
			}
			return;
		}

		super.run(event);
	}

	@Override
	protected String getFullName() {
		return name;
	}

	protected abstract SlashCommandData getCommandData();

	protected void addSubcommand(Subcommand subcommand) {
		if (subcommands.stream().anyMatch(cmd -> cmd.getName().equalsIgnoreCase(subcommand.getName()))) {
			throw new IllegalArgumentException("Tried to add a subcommand name that has already been indexed: " + subcommand.getName());
		}

		subcommand.superCommand = this;
		subcommands.add(subcommand);
	}

	private Subcommand getSubcommand(String subcommandName) {
		return subcommands.stream().filter(s -> s.name.equals(subcommandName)).findAny().orElse(null);
	}

	protected void onAutoCompleteInternal(AutoCompleteEvent event) {
		if (!subcommands.isEmpty() && event.getSubcommandName() != null) {
			Subcommand subcommand = getSubcommand(event.getSubcommandName());
			if (subcommand != null) {
				subcommand.onAutoComplete(event);
			}
		}

		onAutoComplete(event);
	}

	public SlashCommandData getFullCommandData() {
		return getCommandData()
			.addSubcommands(subcommands.stream().map(Subcommand::getCommandData).collect(Collectors.toCollection(ArrayList::new)));
	}
}
