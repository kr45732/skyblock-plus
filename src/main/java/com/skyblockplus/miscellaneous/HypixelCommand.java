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

package com.skyblockplus.miscellaneous;

import static com.skyblockplus.utils.ApiHandler.getGuildFromPlayer;
import static com.skyblockplus.utils.ApiHandler.getNameHistory;
import static com.skyblockplus.utils.Utils.*;

import com.google.gson.JsonElement;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.skyblockplus.utils.HypixelPlayer;
import com.skyblockplus.utils.command.CommandExecute;
import com.skyblockplus.utils.structs.HypixelResponse;
import java.time.Instant;
import java.util.Map;
import net.dv8tion.jda.api.EmbedBuilder;

public class HypixelCommand extends Command {

	public HypixelCommand() {
		this.name = "hypixel";
		this.cooldown = globalCooldown;
		this.botPermissions = defaultPerms();
	}

	public static EmbedBuilder getParkourStats(String username) {
		HypixelPlayer player = new HypixelPlayer(username);
		if (player.isNotValid()) {
			return invalidEmbed(player.getFailCause());
		}

		try {
			EmbedBuilder eb = player.getDefaultEmbed();
			StringBuilder parkourCompletionString = new StringBuilder();
			for (String parkourLocation : getJsonKeys(player.get("parkourCompletions"))) {
				int fastestTime = -1;
				for (JsonElement parkourTime : player.get("parkourCompletions." + parkourLocation).getAsJsonArray()) {
					fastestTime = Math.max(higherDepth(parkourTime, "timeTook", -1), fastestTime);
				}

				if (fastestTime != -1) {
					parkourCompletionString.append("• ").append(parkourLocation).append(": ").append(fastestTime / 1000).append("s\n");
				}
			}

			if (parkourCompletionString.length() > 0) {
				eb.setDescription("**Fastest Parkour Times:**\n" + parkourCompletionString);
				return eb;
			}
		} catch (Exception ignored) {}
		return invalidEmbed("Player has no completed parkours");
	}

	public static EmbedBuilder getHypixelStats(String username) {
		HypixelPlayer player = new HypixelPlayer(username);
		if (player.isNotValid()) {
			return invalidEmbed(player.getFailCause());
		}

		EmbedBuilder eb = player.getDefaultEmbed();
		if (player.isOnline()) {
			eb.addField("Status", "🟢", true);
		} else {
			eb.addField("Status", "🔴", true);

			if (player.getLastLogin() != null) {
				eb.addField("Last Login", "<t:" + player.getLastLogin().getEpochSecond() + ">", true);
			}
		}

		if (player.getFirstLogin() != null) {
			eb.addField("First Login", "<t:" + player.getFirstLogin().getEpochSecond() + ":D>", true);
		}

		eb.addField("Hypixel Level", roundAndFormat(player.getHypixelLevel()), true);
		eb.addField("Hypixel Rank", player.getRank(), true);

		try {
			for (Map.Entry<String, JsonElement> socialMedia : player.getSocialMediaLinks().entrySet()) {
				eb.addField(
					socialMedia.getKey().equals("HYPIXEL") ? "Hypixel Forums" : capitalizeString(socialMedia.getKey()),
					socialMedia.getValue().getAsString().contains("http")
						? "[Link](" + socialMedia.getValue().getAsString() + ")"
						: socialMedia.getValue().getAsString(),
					true
				);
			}
		} catch (Exception ignored) {}

		try {
			eb.addField("Most Recent Lobby", capitalizeString(player.get("mostRecentGameType").getAsString().toLowerCase()), true);
		} catch (Exception ignored) {}

		HypixelResponse guildResponse = getGuildFromPlayer(player.getUuid());
		if (!guildResponse.isNotValid()) {
			eb.addField("Guild", guildResponse.get("name").getAsString(), true);

			for (JsonElement member : guildResponse.get("members").getAsJsonArray()) {
				if (higherDepth(member, "uuid").getAsString().equals(player.getUuid())) {
					eb.addField(
						"Guild Rank",
						higherDepth(member, "rank").getAsString().equals("GUILDMASTER")
							? "Guild Master"
							: higherDepth(member, "rank").getAsString(),
						true
					);
					eb.addField(
						"Joined Guild",
						"<t:" + Instant.ofEpochMilli(higherDepth(member, "joined").getAsLong()).getEpochSecond() + ":D>",
						true
					);
					break;
				}
			}
		}

		eb.addField("Karma", formatNumber(player.getKarma()), true);
		eb.addField("Achievement Points", formatNumber(player.getAchievementPoints()), true);

		StringBuilder namesString = new StringBuilder();
		for (String name : getNameHistory(player.getUuid())) {
			namesString.append("• ").append(name).append("\n");
		}
		if (namesString.length() > 0) {
			eb.addField("Aliases", namesString.toString(), true);
		}

		String skyblockItems = "";
		if (player.get("skyblock_free_cookie") != null) {
			skyblockItems +=
				"• Free booster cookie: " +
				"<t:" +
				Instant.ofEpochMilli(player.get("skyblock_free_cookie").getAsLong()).getEpochSecond() +
				":d>" +
				"\n";
		}

		if (player.get("scorpius_bribe_96") != null) {
			skyblockItems +=
				"• Scorpius Bribe (Year 96): " +
				"<t:" +
				Instant.ofEpochMilli(player.get("scorpius_bribe_96").getAsLong()).getEpochSecond() +
				":d>" +
				"\n";
		}

		if (player.get("scorpius_bribe_120") != null) {
			skyblockItems +=
				"• Scorpius Bribe (Year 120): " +
				"<t:" +
				Instant.ofEpochMilli(player.get("scorpius_bribe_120").getAsLong()).getEpochSecond() +
				":d>" +
				"\n";
		}

		if (player.get("scorpius_bribe_144") != null) {
			skyblockItems +=
				"• Scorpius Bribe (Year 144): " +
				"<t:" +
				Instant.ofEpochMilli(player.get("scorpius_bribe_144").getAsLong()).getEpochSecond() +
				":d>" +
				"\n";
		}

		if (player.get("claimed_potato_talisman") != null) {
			skyblockItems +=
				"• Potato Talisman: " +
				"<t:" +
				Instant.ofEpochMilli(player.get("claimed_potato_talisman").getAsLong()).getEpochSecond() +
				":d>" +
				"\n";
		}

		if (player.get("claimed_potato_basket") != null) {
			skyblockItems +=
				"• Potato Basket: " +
				"<t:" +
				Instant.ofEpochMilli(player.get("claimed_potato_basket").getAsLong()).getEpochSecond() +
				":d>" +
				"\n";
		}

		if (player.get("claim_potato_war_crown") != null) {
			skyblockItems +=
				"• Potato War Crown: " +
				"<t:" +
				Instant.ofEpochMilli(player.get("claim_potato_war_crown").getAsLong()).getEpochSecond() +
				":d>" +
				"\n";
		}

		if (skyblockItems.length() > 0) {
			eb.addField("Skyblock", skyblockItems, true);
		}

		int fillGap = 3 - ((eb.getFields().size() % 3) == 0 ? 3 : (eb.getFields().size() % 3));
		for (int i = 0; i < fillGap; i++) {
			eb.addBlankField(true);
		}
		return eb;
	}

	@Override
	protected void execute(CommandEvent event) {
		new CommandExecute(this, event) {
			@Override
			protected void execute() {
				if ((args.length == 3 || args.length == 2) && args[1].equals("parkour")) {
					if (getMentionedUsername(args.length == 2 ? -1 : 1)) {
						return;
					}

					embed(getParkourStats(username));
					return;
				} else if (args.length == 2 || args.length == 1) {
					if (getMentionedUsername(args.length == 1 ? -1 : 1)) {
						return;
					}

					embed(getHypixelStats(username));
					return;
				}

				sendErrorEmbed();
			}
		}
			.submit();
	}
}
