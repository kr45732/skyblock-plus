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

package com.skyblockplus.features.party;

import com.skyblockplus.utils.command.PaginatorEvent;
import com.skyblockplus.utils.command.SlashCommand;
import com.skyblockplus.utils.command.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import org.springframework.stereotype.Component;

@Component
public class PartySlashCommand extends SlashCommand {

	public PartySlashCommand() {
		this.name = "party";
	}

	@Override
	protected void execute(SlashCommandEvent event) {
		event.logCommand();

		switch (event.getSubcommandName()) {
			case "create" -> event.paginate(PartyCommand.createParty(new PaginatorEvent(event)), true);
			case "list" -> event.embed(PartyCommand.getPartyList(event.getGuild().getId()));
			case "leave" -> event.embed(PartyCommand.leaveParty(new PaginatorEvent(event)));
			case "disband" -> event.embed(PartyCommand.disbandParty(new PaginatorEvent(event)));
			case "join" -> event.embed(PartyCommand.joinParty(event.getOptionStr("username"), new PaginatorEvent(event)));
			case "kick" -> event.embed(PartyCommand.kickMemberFromParty(event.getOptionStr("username"), new PaginatorEvent(event)));
			case "current" -> event.embed(PartyCommand.getCurrentParty(new PaginatorEvent(event)));
			default -> event.embed(event.invalidCommandMessage());
		}
	}

	@Override
	public CommandData getCommandData() {
		return Commands
			.slash(name, "Main party command")
			.addSubcommands(
				new SubcommandData("create", "Interactive message to create a new party"),
				new SubcommandData("list", "List all active parties"),
				new SubcommandData("leave", "Leave your current party"),
				new SubcommandData("disband", "Disband your current party"),
				new SubcommandData("current", "Get information about the party you are currently in"),
				new SubcommandData("join", "Join a party").addOption(OptionType.STRING, "username", "The party leader's username", true),
				new SubcommandData("kick", "Kick a member from your party")
					.addOption(OptionType.STRING, "username", "The party member's username", true)
			);
	}
}
