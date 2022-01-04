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

import static com.skyblockplus.Main.database;
import static com.skyblockplus.features.listeners.MainListener.guildMap;
import static com.skyblockplus.utils.Utils.*;

import com.google.gson.JsonElement;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.skyblockplus.utils.Player;
import com.skyblockplus.utils.command.CommandExecute;
import com.skyblockplus.utils.command.PaginatorEvent;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
import net.dv8tion.jda.api.interactions.components.buttons.Button;

public class PartyCommand extends Command {

	public PartyCommand() {
		this.name = "party";
		this.cooldown = globalCooldown;
		this.botPermissions = defaultPerms();
	}

	public static EmbedBuilder getCurrentParty(PaginatorEvent event) {
		Party party = guildMap
			.get(event.getGuild().getId())
			.partyList.stream()
			.filter(p ->
				p.getPartyLeaderId().equals(event.getUser().getId()) ||
				p.getPartyMembers().stream().anyMatch(pm -> pm.getDiscordId().equalsIgnoreCase(event.getUser().getId()))
			)
			.findFirst()
			.orElse(null);
		if (party == null) {
			return invalidEmbed("Your are not in a party");
		}

		return defaultEmbed("Party Finder")
			.addField(
				party.getPartyLeaderUsername() + "'s party",
				"Floor: " +
				party.getFloor().replace("_", " ") +
				"\nCapacity: " +
				party.getPartyMembers().size() +
				"/5\nMembers: <@" +
				party.getPartyLeaderId() +
				"> " +
				party
					.getPartyMembers()
					.stream()
					.map(m -> "<@" + m.getDiscordId() + "> (" + m.getClassName() + ")")
					.collect(Collectors.joining(" ")),
				false
			);
	}

	public static EmbedBuilder kickMemberFromParty(String username, PaginatorEvent event) {
		Party party = guildMap
			.get(event.getGuild().getId())
			.partyList.stream()
			.filter(p -> p.getPartyLeaderId().equals(event.getUser().getId()))
			.findFirst()
			.orElse(null);
		if (party == null) {
			return invalidEmbed("You are not the leader of a party");
		}

		String kickedUsername = party.kickFromParty(username);
		if (kickedUsername != null) {
			return defaultEmbed("Party Finder").setDescription("Kicked " + kickedUsername + " from the party");
		} else {
			return invalidEmbed(username + " is not in your party");
		}
	}

	public static EmbedBuilder leaveParty(PaginatorEvent event) {
		List<Party> partyList = guildMap.get(event.getGuild().getId()).partyList;
		for (Party party : partyList) {
			if (party.leaveParty(event.getUser().getId())) {
				return defaultEmbed("Party Finder").setDescription("Left " + party.getPartyLeaderUsername() + "'s party");
			}
		}

		return invalidEmbed("You are not in a party");
	}

	public static EmbedBuilder disbandParty(PaginatorEvent event) {
		List<Party> partyList = guildMap.get(event.getGuild().getId()).partyList;
		Party party = partyList.stream().filter(p -> p.getPartyLeaderId().equals(event.getUser().getId())).findFirst().orElse(null);
		if (party == null) {
			return invalidEmbed("You are not the leader of a party");
		}

		partyList.remove(party);
		return defaultEmbed("Party Finder").setDescription("Disbanded the party");
	}

	public static EmbedBuilder createParty(PaginatorEvent event) {
		if (
			guildMap
				.get(event.getGuild().getId())
				.partyList.stream()
				.anyMatch(p ->
					p.getPartyLeaderId().equals(event.getUser().getId()) ||
					p.getPartyMembers().stream().anyMatch(pm -> pm.getDiscordId().equals(event.getUser().getId()))
				)
		) {
			return invalidEmbed("You are already a party leader or in a party");
		}

		String username = higherDepth(database.getLinkedUserByDiscordId(event.getUser().getId()), "minecraftUsername", null);
		if (username == null) {
			return invalidEmbed("You must be linked to run this command. Use `/link <player>` to link");
		}

		new PartyHandler(username, event);
		return null;
	}

	public static EmbedBuilder getPartyList(String guildId) {
		List<Party> partyList = guildMap.get(guildId).partyList;
		if (partyList.size() == 0) {
			return invalidEmbed("No active parties");
		}

		EmbedBuilder eb = defaultEmbed("Party List");
		for (Party party : partyList) {
			eb.addField(
				party.getPartyLeaderUsername() + "'s party",
				"Join: `/party join " +
				party.getPartyLeaderUsername() +
				"`\nFloor: " +
				party.getFloor().replace("_", " ") +
				"\nRequested classes: " +
				String.join(", ", party.getRequestedClasses()) +
				"\nMembers: <@" +
				party.getPartyLeaderId() +
				"> " +
				party.getPartyMembers().stream().map(m -> "<@" + m.getDiscordId() + ">").collect(Collectors.joining(" ")),
				false
			);
		}
		return eb;
	}

	public static EmbedBuilder joinParty(String id, PaginatorEvent event) {
		List<Party> partyList = guildMap.get(event.getGuild().getId()).partyList;
		if (
			partyList
				.stream()
				.anyMatch(p ->
					p.getPartyLeaderId().equals(event.getUser().getId()) ||
					p.getPartyMembers().stream().anyMatch(pm -> pm.getDiscordId().equals(event.getUser().getId()))
				)
		) {
			return invalidEmbed("You are already a party leader or in a party");
		}

		Party party = partyList.stream().filter(p -> p.getPartyLeaderUsername().equalsIgnoreCase(id)).findFirst().orElse(null);
		if (party == null) {
			return invalidEmbed("Invalid party id. You can get a list of all parties using `/party list`");
		}

		JsonElement linkedUser = database.getLinkedUserByDiscordId(event.getUser().getId());
		if (linkedUser.isJsonNull()) {
			return invalidEmbed("You must be linked to run this command. Use `/link <player>` to link");
		}

		Player player = new Player(higherDepth(linkedUser, "minecraftUuid").getAsString());
		if (player.getHighestPlayedDungeonFloor() + 1 < party.getFloorInt()) {
			return invalidEmbed("You have not unlocked this floor");
		}

		String selectedClass = player.getSelectedDungeonClass();
		if (party.getMissingClasses().contains(selectedClass) || party.getMissingClasses().contains("any")) {
			party.joinParty(
				player.getUsername(),
				event.getUser().getId(),
				selectedClass,
				!party.getMissingClasses().contains(selectedClass)
			);
			TextChannel channel = event.getGuild().getTextChannelById(party.getMessageChannelId());
			channel
				.sendMessage("<@" + party.getPartyLeaderId() + "> ")
				.setEmbeds(
					defaultEmbed("Party Finder")
						.setDescription(
							"**" +
							player.getUsername() +
							" joined your party**\nCatacombs: " +
							roundAndFormat(player.getCatacombs().getProgressLevel()) +
							"\nSecrets: " +
							formatNumber(player.getDungeonSecrets()) +
							"\nClass: " +
							selectedClass
						)
						.build()
				)
				.queueAfter(1, TimeUnit.SECONDS);
			if (party.getPartyMembers().size() == 4) {
				try {
					channel
						.createThreadChannel("pf-" + party.getPartyLeaderUsername())
						.queue(threadChannel ->
							threadChannel
								.sendMessage(
									"<@" +
									party.getPartyLeaderId() +
									"> " +
									party
										.getPartyMembers()
										.stream()
										.map(m -> "<@" + m.getDiscordId() + ">")
										.collect(Collectors.joining(" "))
								)
								.setEmbeds(
									defaultEmbed("Party Finder")
										.setDescription(
											"Your party has reached 5/5 players and has been unlisted. The party leader can click the button below to close this channel."
										)
										.build()
								)
								.setActionRow(Button.danger("party_finder_channel_close_" + party.getPartyLeaderId(), "Archive Thraed"))
								.queueAfter(1, TimeUnit.SECONDS)
						);
				} catch (InsufficientPermissionException e) {
					channel
						.sendMessage(
							"<@" +
							party.getPartyLeaderId() +
							"> " +
							party.getPartyMembers().stream().map(m -> "<@" + m.getDiscordId() + ">").collect(Collectors.joining(" "))
						)
						.setEmbeds(
							defaultEmbed("Party Finder")
								.setDescription(
									"Your party has reached 5/5 players and has been unlisted. Missing permissions to create a thread: `" +
									e.getMessage().split("Missing permission: ")[1] +
									"`"
								)
								.build()
						)
						.queueAfter(1, TimeUnit.SECONDS);
				}

				partyList.remove(party);
			}

			return defaultEmbed("Party Finder")
				.setDescription(
					"Joined " +
					party.getPartyLeaderUsername() +
					"'s party. The party is at " +
					(party.getPartyMembers().size() + 1) +
					"/5 members"
				);
		} else {
			return invalidEmbed(
				"The party needs a " +
				String.join(", or ", new HashSet<>(party.getMissingClasses())) +
				", however, your selected class is a " +
				selectedClass
			);
		}
	}

	@Override
	protected void execute(CommandEvent event) {
		new CommandExecute(this, event) {
			@Override
			protected void execute() {
				logCommand();

				if (args.length == 2) {
					switch (args[1]) {
						case "create" -> {
							paginate(createParty(new PaginatorEvent(event)));
							return;
						}
						case "list" -> {
							embed(getPartyList(event.getGuild().getId()));
							return;
						}
						case "leave" -> {
							embed(leaveParty(new PaginatorEvent(event)));
							return;
						}
						case "disband" -> {
							embed(disbandParty(new PaginatorEvent(event)));
							return;
						}
						case "current" -> {
							embed(getCurrentParty(new PaginatorEvent(event)));
							return;
						}
					}
				} else if (args.length == 3) {
					switch (args[1]) {
						case "join" -> {
							embed(joinParty(args[2], new PaginatorEvent(event)));
							return;
						}
						case "kick" -> {
							embed(kickMemberFromParty(args[2], new PaginatorEvent(event)));
							return;
						}
					}
				}

				sendErrorEmbed();
			}
		}
			.queue();
	}
}
