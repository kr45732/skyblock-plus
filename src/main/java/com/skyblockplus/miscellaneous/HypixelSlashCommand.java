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
import static com.skyblockplus.utils.Utils.*;

import com.google.gson.JsonElement;
import com.skyblockplus.utils.HypixelPlayer;
import com.skyblockplus.utils.command.SlashCommand;
import com.skyblockplus.utils.command.SlashCommandEvent;
import com.skyblockplus.utils.structs.AutoCompleteEvent;
import com.skyblockplus.utils.structs.HypixelResponse;
import java.time.Instant;
import java.util.Map;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import org.springframework.stereotype.Component;

@Component
public class HypixelSlashCommand extends SlashCommand {

	public HypixelSlashCommand() {
		this.name = "hypixel";
	}

	@Override
	protected void execute(SlashCommandEvent event) {
		if (event.invalidPlayerOption()) {
			return;
		}

		event.embed(getHypixelStats(event.player));
	}

	@Override
	public CommandData getCommandData() {
		return Commands
			.slash(name, "Get Hypixel information about a player")
			.addOption(OptionType.STRING, "player", "Player username or mention", false, true);
	}

	@Override
	public void onAutoComplete(AutoCompleteEvent event) {
		if (event.getFocusedOption().getName().equals("player")) {
			event.replyClosestPlayer();
		}
	}

	public static EmbedBuilder getHypixelStats(String username) {
		HypixelPlayer player = new HypixelPlayer(username);
		if (!player.isValid()) {
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
		if (guildResponse.isValid()) {
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
				"• Scorpius Bribe (96): " +
				"<t:" +
				Instant.ofEpochMilli(player.get("scorpius_bribe_96").getAsLong()).getEpochSecond() +
				":d>" +
				"\n";
		}

		if (player.get("scorpius_bribe_120") != null) {
			skyblockItems +=
				"• Scorpius Bribe (120): " +
				"<t:" +
				Instant.ofEpochMilli(player.get("scorpius_bribe_120").getAsLong()).getEpochSecond() +
				":d>" +
				"\n";
		}

		if (player.get("scorpius_bribe_144") != null) {
			skyblockItems +=
				"• Scorpius Bribe (144): " +
				"<t:" +
				Instant.ofEpochMilli(player.get("scorpius_bribe_144").getAsLong()).getEpochSecond() +
				":d>" +
				"\n";
		}

		if (player.get("scorpius_bribe_168") != null) {
			skyblockItems +=
				"• Scorpius Bribe (168): " +
				"<t:" +
				Instant.ofEpochMilli(player.get("scorpius_bribe_168").getAsLong()).getEpochSecond() +
				":d>" +
				"\n";
		}

		if (player.get("scorpius_bribe_192") != null) {
			skyblockItems +=
				"• Scorpius Bribe (192): " +
				"<t:" +
				Instant.ofEpochMilli(player.get("scorpius_bribe_192").getAsLong()).getEpochSecond() +
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
}
