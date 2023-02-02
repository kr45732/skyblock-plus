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

package com.skyblockplus.guild;

import static com.skyblockplus.utils.ApiHandler.*;
import static com.skyblockplus.utils.Utils.*;
import static com.skyblockplus.utils.database.LeaderboardDatabase.formattedTypesSubList;
import static com.skyblockplus.utils.database.LeaderboardDatabase.getType;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.skyblockplus.utils.Player;
import com.skyblockplus.utils.command.*;
import com.skyblockplus.utils.structs.AutoCompleteEvent;
import com.skyblockplus.utils.structs.HypixelResponse;
import com.skyblockplus.utils.structs.UsernameUuidStruct;
import java.io.FileReader;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.utils.data.DataObject;
import org.springframework.stereotype.Component;

@Component
public class GuildSlashCommand extends SlashCommand {

	public GuildSlashCommand() {
		this.name = "guild";
	}

	public static class InformationSubcommand extends Subcommand {

		public InformationSubcommand() {
			this.name = "information";
		}

		@Override
		protected void execute(SlashCommandEvent event) {
			String guild = event.getOptionStr("guild");
			if (guild != null) {
				event.embed(getGuildInformation(null, guild));
				return;
			}

			if (event.invalidPlayerOption()) {
				return;
			}

			event.embed(getGuildInformation(event.player, null));
		}

		@Override
		protected SubcommandData getCommandData() {
			return new SubcommandData("information", "Get information and statistics about a guild")
				.addOption(OptionType.STRING, "player", "Player username or mention", false, true)
				.addOption(OptionType.STRING, "guild", "Guild name", false);
		}

		public static EmbedBuilder getGuildInformation(String username, String guildName) {
			HypixelResponse hypixelResponse;
			if (guildName != null) {
				hypixelResponse = getGuildFromName(guildName);
			} else {
				UsernameUuidStruct usernameUuid = usernameToUuid(username);
				if (!usernameUuid.isValid()) {
					return invalidEmbed(usernameUuid.failCause());
				}

				hypixelResponse = getGuildFromPlayer(usernameUuid.uuid());
			}

			if (!hypixelResponse.isValid()) {
				return invalidEmbed(hypixelResponse.failCause());
			}

			String guildInfo = "";
			guildName = hypixelResponse.get("name").getAsString();

			guildInfo +=
				"• " +
				guildName +
				" was created on <t:" +
				Instant.ofEpochMilli(hypixelResponse.get("created").getAsLong()).getEpochSecond() +
				":D>\n";

			JsonArray guildMembers = hypixelResponse.get("members").getAsJsonArray();
			for (JsonElement currentMember : guildMembers) {
				if (higherDepth(currentMember, "rank").getAsString().equals("Guild Master")) {
					guildInfo +=
						"• " +
						guildName +
						"'s guild master is " +
						uuidToUsername(higherDepth(currentMember, "uuid").getAsString()).username() +
						"\n";
					break;
				}
			}

			guildInfo += "• " + guildName + " has " + guildMembers.size() + " members\n";

			JsonElement preferredGames = hypixelResponse.get("preferredGames");
			if (preferredGames != null) {
				List<String> preferedGames = streamJsonArray(preferredGames)
					.map(e -> capitalizeString(e.getAsString().replace("_", " ")))
					.toList();
				if (!preferedGames.isEmpty()) {
					guildInfo +=
						"• " +
						guildName +
						"'s preferred " +
						(preferedGames.size() == 1 ? "game is " : "games are ") +
						String.join(", ", preferedGames) +
						"\n";
				}
			}

			guildInfo += "• " + guildName + " is guild level " + guildExpToLevel(higherDepth(hypixelResponse.response(), "exp", 0)) + "\n";

			EmbedBuilder eb = defaultEmbed(guildName);
			eb.addField("Guild statistics:", guildInfo, false);
			return eb;
		}
	}

	public static class MembersSubcommand extends Subcommand {

		public MembersSubcommand() {
			this.name = "members";
		}

		@Override
		protected void execute(SlashCommandEvent event) {
			String guild = event.getOptionStr("guild");
			if (guild != null) {
				event.paginate(getGuildMembers(null, guild, event));
				return;
			}

			if (event.invalidPlayerOption()) {
				return;
			}

			event.paginate(getGuildMembers(event.player, null, event));
		}

		@Override
		protected SubcommandData getCommandData() {
			return new SubcommandData("members", "Get a list of all members in a player's guild")
				.addOption(OptionType.STRING, "player", "Player username or mention", false, true)
				.addOption(OptionType.STRING, "guild", "Guild name", false);
		}

		public static EmbedBuilder getGuildMembers(String username, String guildName, SlashCommandEvent event) {
			HypixelResponse hypixelResponse;
			if (guildName != null) {
				hypixelResponse = getGuildFromName(guildName);
			} else {
				UsernameUuidStruct usernameUuid = usernameToUuid(username);
				if (!usernameUuid.isValid()) {
					return invalidEmbed(usernameUuid.failCause());
				}

				hypixelResponse = getGuildFromPlayer(usernameUuid.uuid());
			}

			if (!hypixelResponse.isValid()) {
				return invalidEmbed(hypixelResponse.failCause());
			}

			JsonElement guildJson = hypixelResponse.response();

			JsonArray membersArr = higherDepth(guildJson, "members").getAsJsonArray();
			Map<CompletableFuture<String>, Integer> futures = new HashMap<>();
			Map<String, Integer> guildMembers = new HashMap<>();
			Map<String, Integer> ranksMap = streamJsonArray(higherDepth(guildJson, "ranks"))
				.collect(Collectors.toMap(m -> higherDepth(m, "name").getAsString(), m -> higherDepth(m, "priority", 0)));
			for (JsonElement member : membersArr) {
				String rank = higherDepth(member, "rank").getAsString();
				futures.put(
					asyncUuidToUsername(higherDepth(member, "uuid").getAsString()),
					rank.equals("Guild Master") ? 50 : ranksMap.getOrDefault(higherDepth(member, "rank").getAsString(), 0)
				);
			}

			for (Map.Entry<CompletableFuture<String>, Integer> future : futures.entrySet()) {
				try {
					guildMembers.put(future.getKey().get(), future.getValue());
				} catch (Exception ignored) {}
			}

			CustomPaginator.Builder paginateBuilder = event.getPaginator().setColumns(3).setItemsPerPage(33);

			paginateBuilder
				.getExtras()
				.setEveryPageText("**Size:** " + membersArr.size())
				.setEveryPageTitle(higherDepth(guildJson, "name").getAsString() + " Members")
				.setEveryPageFirstFieldTitle("Members:");

			for (String member : guildMembers
				.entrySet()
				.stream()
				.sorted(Comparator.comparingInt(m -> -m.getValue()))
				.map(Map.Entry::getKey)
				.toList()) {
				if (member != null) {
					paginateBuilder.addItems("• [" + fixUsername(member) + "](" + skyblockStatsLink(member, null) + ")  ");
				}
			}

			event.paginate(paginateBuilder);
			return null;
		}
	}

	public static class ExperienceSubcommand extends Subcommand {

		public ExperienceSubcommand() {
			this.name = "experience";
		}

		@Override
		protected void execute(SlashCommandEvent event) {
			int numDays = event.getOptionInt("days", 7);

			String guild = event.getOptionStr("guild");
			if (guild != null) {
				event.paginate(getGuildExperience(null, guild, numDays, event));
				return;
			}

			if (event.invalidPlayerOption()) {
				return;
			}

			event.paginate(getGuildExperience(event.player, null, numDays, event));
		}

		@Override
		protected SubcommandData getCommandData() {
			return new SubcommandData("experience", "Get the experience leaderboard for a player's guild")
				.addOption(OptionType.STRING, "player", "Player username or mention", false, true)
				.addOption(OptionType.STRING, "guild", "Guild name", false)
				.addOptions(new OptionData(OptionType.INTEGER, "days", "Number of days").setRequiredRange(1, 7));
		}

		public static EmbedBuilder getGuildExperience(String username, String guildName, long days, SlashCommandEvent event) {
			if (days < 1 || days > 7) {
				return invalidEmbed("Days must be between 1 to 7");
			}

			UsernameUuidStruct usernameUuid = null;
			HypixelResponse hypixelResponse;
			if (guildName != null) {
				hypixelResponse = getGuildFromName(guildName);
			} else {
				usernameUuid = usernameToUuid(username);
				if (!usernameUuid.isValid()) {
					return invalidEmbed(usernameUuid.failCause());
				}

				hypixelResponse = getGuildFromPlayer(usernameUuid.uuid());
			}

			if (!hypixelResponse.isValid()) {
				return invalidEmbed(hypixelResponse.failCause());
			}

			JsonElement members = hypixelResponse.get("members");
			JsonArray membersArr = members.getAsJsonArray();
			List<String> guildExpList = new ArrayList<>();
			List<CompletableFuture<String>> futures = new ArrayList<>();
			for (int i = 0; i < membersArr.size(); i++) {
				int finalI = i;
				futures.add(
					asyncUuidToUsername(higherDepth(membersArr.get(i), "uuid").getAsString())
						.thenApplyAsync(
							currentUsername -> {
								JsonElement expHistory = higherDepth(membersArr.get(finalI), "expHistory");
								List<String> keys = getJsonKeys(expHistory);
								int totalPlayerExp = 0;

								for (int j = 0; j < days; j++) {
									String value = keys.get(j);
									totalPlayerExp += higherDepth(expHistory, value, 0);
								}
								return currentUsername + "=:=" + totalPlayerExp;
							},
							executor
						)
				);
			}

			for (CompletableFuture<String> future : futures) {
				try {
					String futureResponse = future.get();
					if (futureResponse != null) {
						guildExpList.add(futureResponse);
					}
				} catch (Exception ignored) {}
			}

			guildExpList.sort(Comparator.comparingInt(o1 -> -Integer.parseInt(o1.split("=:=")[1])));

			CustomPaginator.Builder paginateBuilder = event.getPaginator().setColumns(2).setItemsPerPage(20);
			PaginatorExtras extras = paginateBuilder.getExtras().setEveryPageTitle(hypixelResponse.get("name").getAsString());

			if (usernameUuid != null) {
				int guildRank = -2;
				int guildExp = -1;
				for (int i = 0; i < guildExpList.size(); i++) {
					String[] curGuildRank = guildExpList.get(i).split("=:=");
					if (curGuildRank[0].equals(usernameUuid.username())) {
						guildRank = i;
						guildExp = Integer.parseInt(curGuildRank[1]);
						break;
					}
				}
				extras.setEveryPageText(
					"**Player:** " +
					usernameUuid.username() +
					"\n**Guild Rank:** #" +
					(guildRank + 1) +
					"\n**Exp:** " +
					formatNumber(guildExp)
				);
			}

			for (int i = 0; i < guildExpList.size(); i++) {
				String[] curG = guildExpList.get(i).split("=:=");
				paginateBuilder.addItems(
					"`" + (i + 1) + ")` " + fixUsername(curG[0]) + ": " + formatNumber(Integer.parseInt(curG[1])) + " EXP  "
				);
			}

			event.paginate(paginateBuilder);

			return null;
		}
	}

	public static class LeaderboardSubcommand extends Subcommand {

		public LeaderboardSubcommand() {
			this.name = "leaderboard";
			this.cooldown = globalCooldown + 2;
		}

		@Override
		protected void execute(SlashCommandEvent event) {
			String guild = event.getOptionStr("guild");
			if (guild != null) {
				event.paginate(
					getLeaderboard(
						event.getOptionStr("type"),
						null,
						guild,
						Player.Gamemode.of(event.getOptionStr("gamemode", "all")),
						event.getOptionBoolean("key", false),
						event
					)
				);
				return;
			}

			if (event.invalidPlayerOption()) {
				return;
			}

			event.paginate(
				getLeaderboard(
					event.getOptionStr("type"),
					event.player,
					null,
					Player.Gamemode.of(event.getOptionStr("gamemode", "all")),
					event.getOptionBoolean("key", false),
					event
				)
			);
		}

		@Override
		protected SubcommandData getCommandData() {
			return new SubcommandData(name, "Get a leaderboard for a guild. The API key must be set for this server.")
				.addOptions(new OptionData(OptionType.STRING, "type", "Leaderboard type", true, true))
				.addOption(OptionType.STRING, "player", "Player username or mention", false, true)
				.addOption(OptionType.STRING, "guild", "Guild name", false)
				.addOptions(
					new OptionData(OptionType.STRING, "gamemode", "Gamemode type")
						.addChoice("All", "all")
						.addChoice("Ironman", "ironman")
						.addChoice("Stranded", "stranded")
				)
				.addOption(OptionType.BOOLEAN, "key", "If the API key for this server should be used for more updated results");
		}

		public static EmbedBuilder getLeaderboard(
			String lbType,
			String username,
			String guildName,
			Player.Gamemode gamemode,
			boolean useKey,
			SlashCommandEvent event
		) {
			String hypixelKey = null;
			if (useKey) {
				hypixelKey = database.getServerHypixelApiKey(event.getGuild().getId());

				EmbedBuilder eb = checkHypixelKey(hypixelKey);
				if (eb != null) {
					return eb;
				}
			}

			lbType = getType(lbType);

			UsernameUuidStruct usernameUuidStruct = null;
			HypixelResponse guildResponse;
			if (username != null) {
				usernameUuidStruct = usernameToUuid(username);
				if (!usernameUuidStruct.isValid()) {
					return invalidEmbed(usernameUuidStruct.failCause());
				}
				guildResponse = getGuildFromPlayer(usernameUuidStruct.uuid());
			} else {
				guildResponse = getGuildFromName(guildName);
			}
			if (!guildResponse.isValid()) {
				return invalidEmbed(guildResponse.failCause());
			}

			JsonElement guildJson = guildResponse.response();
			guildName = higherDepth(guildJson, "name").getAsString();
			String guildId = higherDepth(guildJson, "_id").getAsString();

			if (hypixelGuildQueue.contains(guildId)) {
				return invalidEmbed("This guild is currently updating, please try again in a few seconds");
			}
			hypixelGuildQueue.add(guildId);
			List<DataObject> playerList = leaderboardDatabase.getCachedPlayers(
				lbType,
				gamemode,
				streamJsonArray(higherDepth(guildJson, "members"))
					.map(u -> higherDepth(u, "uuid", ""))
					.collect(Collectors.toCollection(ArrayList::new)),
				hypixelKey,
				event
			);
			hypixelGuildQueue.remove(guildId);

			String finalLbType = lbType;
			playerList.sort(Comparator.comparingDouble(cache -> -cache.getDouble(finalLbType)));

			CustomPaginator.Builder paginateBuilder = event.getPaginator().setColumns(2).setItemsPerPage(20);

			long total = 0;
			int guildRank = -1;
			String amt = "?";
			for (int i = 0, guildMemberPlayersListSize = playerList.size(); i < guildMemberPlayersListSize; i++) {
				DataObject player = playerList.get(i);
				double amount = player.getDouble(lbType);
				amount = lbType.equals("networth") ? (long) amount : amount;
				String formattedAmt = amount == -1 ? "?" : roundAndFormat(amount);
				String playerUsername = player.getString("username");

				paginateBuilder.addItems("`" + (i + 1) + ")` " + fixUsername(playerUsername) + ": " + formattedAmt);
				total += Math.max(0, amount);

				if (username != null && playerUsername.equals(usernameUuidStruct.username())) {
					guildRank = i;
					amt = formattedAmt;
				}
			}

			String ebStr =
				"**Total " +
				capitalizeString(lbType.replace("_", " ")) +
				":** " +
				formatNumber(total) +
				(
					username != null
						? "\n**Player:** " +
						usernameUuidStruct.username() +
						"\n**Guild Rank:** #" +
						(guildRank + 1) +
						"\n**" +
						capitalizeString(lbType.replace("_", " ")) +
						":** " +
						amt
						: ""
				);

			paginateBuilder.getExtras().setEveryPageTitle(guildName).setEveryPageText(ebStr);
			event.paginate(paginateBuilder, (guildRank / 20) + 1);

			return null;
		}
	}

	public static class KickerSubcommand extends Subcommand {

		public KickerSubcommand() {
			this.name = "kicker";
			this.cooldown = globalCooldown + 2;
		}

		@Override
		protected void execute(SlashCommandEvent event) {
			if (event.invalidPlayerOption()) {
				return;
			}

			event.paginate(
				getGuildKicker(
					event.player,
					event.getOptionStr("requirements"),
					Player.Gamemode.of(event.getOptionStr("gamemode", "all")),
					event.getOptionBoolean("key", false),
					event
				)
			);
		}

		@Override
		protected SubcommandData getCommandData() {
			return new SubcommandData(name, "Get helper which shows who to promote or demote in your guild")
				.addOption(OptionType.STRING, "requirements", "The requirements a player must meet", true)
				.addOption(OptionType.STRING, "player", "Player username or mention", false, true)
				.addOptions(
					new OptionData(OptionType.STRING, "gamemode", "Gamemode type")
						.addChoice("All", "all")
						.addChoice("Ironman", "ironman")
						.addChoice("Stranded", "stranded")
				)
				.addOption(OptionType.BOOLEAN, "key", "If the API key for this server should be used for more updated results");
		}

		public static EmbedBuilder getGuildKicker(
			String username,
			String reqs,
			Player.Gamemode gamemode,
			boolean useKey,
			SlashCommandEvent event
		) {
			String[] reqsArr = reqs.split("] \\[");
			if (reqsArr.length > 5) {
				return invalidEmbed("You can only enter a maximum of 5 sets of requirements");
			}
			for (int i = 0; i < reqsArr.length; i++) {
				String[] indvReqs = reqsArr[i].replace("[", "").replace("]", "").split("\\s+");
				for (String indvReq : indvReqs) {
					String[] reqDashSplit = indvReq.split(":");
					if (reqDashSplit.length != 2) {
						return invalidEmbed(indvReq + " is an invalid requirement format");
					}

					if (
						!reqDashSplit[0].equals("slayer") &&
						!reqDashSplit[0].equals("skills") &&
						!reqDashSplit[0].equals("catacombs") &&
						!reqDashSplit[0].equals("weight")
					) {
						return invalidEmbed(indvReq + " is an invalid requirement type");
					}

					try {
						Double.parseDouble(reqDashSplit[1]);
					} catch (Exception e) {
						return invalidEmbed(indvReq + " is an invalid requirement value");
					}
				}

				reqsArr[i] = reqsArr[i].replace("[", "").replace("]", "");
			}

			String hypixelKey = null;
			if (useKey) {
				hypixelKey = database.getServerHypixelApiKey(event.getGuild().getId());

				EmbedBuilder eb = checkHypixelKey(hypixelKey);
				if (eb != null) {
					return eb;
				}
			}

			UsernameUuidStruct usernameUuidStruct = usernameToUuid(username);
			if (!usernameUuidStruct.isValid()) {
				return invalidEmbed(usernameUuidStruct.failCause());
			}
			HypixelResponse guildResponse = getGuildFromPlayer(usernameUuidStruct.uuid());
			if (!guildResponse.isValid()) {
				return invalidEmbed(guildResponse.failCause());
			}
			JsonElement guildJson = guildResponse.response();
			String guildId = higherDepth(guildJson, "_id").getAsString();

			if (hypixelGuildQueue.contains(guildId)) {
				return invalidEmbed("This guild is currently updating, please try again in a few seconds");
			}
			hypixelGuildQueue.add(guildId);
			List<DataObject> playerList = leaderboardDatabase.getCachedPlayers(
				List.of("slayer", "skills", "catacombs", "weight"),
				gamemode,
				streamJsonArray(higherDepth(guildJson, "members"))
					.map(u -> higherDepth(u, "uuid", ""))
					.collect(Collectors.toCollection(ArrayList::new)),
				hypixelKey,
				event
			);
			hypixelGuildQueue.remove(guildId);

			CustomPaginator.Builder paginateBuilder = event.getPaginator().setItemsPerPage(20);

			for (DataObject guildMember : playerList) {
				double slayer = guildMember.getDouble("slayer");
				double skills = guildMember.getDouble("skills");
				double catacombs = guildMember.getDouble("catacombs");
				double weight = guildMember.getDouble("weight");

				boolean meetsReqs = false;

				for (String req : reqsArr) {
					String[] reqSplit = req.split("\\s+");
					double slayerReq = 0;
					double skillsReq = 0;
					double catacombsReq = 0;
					double weightReq = 0;
					for (String reqIndividual : reqSplit) {
						switch (reqIndividual.split(":")[0]) {
							case "slayer" -> slayerReq = Double.parseDouble(reqIndividual.split(":")[1]);
							case "skills" -> skillsReq = Double.parseDouble(reqIndividual.split(":")[1]);
							case "catacombs" -> catacombsReq = Double.parseDouble(reqIndividual.split(":")[1]);
							case "weight" -> weightReq = Double.parseDouble(reqIndividual.split(":")[1]);
						}
					}

					if (slayer >= slayerReq && Math.max(0, skills) >= skillsReq && catacombs >= catacombsReq && weight >= weightReq) {
						meetsReqs = true;
						break;
					}
				}

				if (!meetsReqs) {
					paginateBuilder.addItems(
						"• **" +
						guildMember.getString("username") +
						"** | Slayer: " +
						formatNumber(slayer) +
						" | Skills: " +
						roundAndFormat(skills) +
						" | Cata: " +
						roundAndFormat(catacombs) +
						" | Weight: " +
						roundAndFormat(weight)
					);
				}
			}

			paginateBuilder
				.getExtras()
				.setEveryPageTitle("Guild Kick Helper")
				.setEveryPageText("**Total missing requirements:** " + paginateBuilder.size());

			event.paginate(paginateBuilder);
			return null;
		}
	}

	public static class RanksSubcommand extends Subcommand {

		public RanksSubcommand() {
			this.name = "ranks";
			this.cooldown = globalCooldown + 2;
		}

		@Override
		protected void execute(SlashCommandEvent event) {
			if (event.invalidPlayerOption()) {
				return;
			}

			event.paginate(
				getRanks(
					event.player,
					Player.Gamemode.of(event.getOptionStr("gamemode", "all")),
					event.getOptionBoolean("key", false),
					event
				)
			);
		}

		@Override
		protected SubcommandData getCommandData() {
			return new SubcommandData(name, "Get helper which shows who to promote or demote in your guild")
				.addOption(OptionType.STRING, "player", "Player username or mention", false, true)
				.addOptions(
					new OptionData(OptionType.STRING, "gamemode", "Gamemode type")
						.addChoice("All", "all")
						.addChoice("Ironman", "ironman")
						.addChoice("Stranded", "stranded")
				)
				.addOption(OptionType.BOOLEAN, "key", "If the API key for this server should be used for more accurate results");
		}

		public static EmbedBuilder getRanks(String username, Player.Gamemode gamemode, boolean useKey, SlashCommandEvent event) {
			String hypixelKey = null;
			if (useKey) {
				hypixelKey = database.getServerHypixelApiKey(event.getGuild().getId());

				EmbedBuilder eb = checkHypixelKey(hypixelKey);
				if (eb != null) {
					return eb;
				}
			}

			UsernameUuidStruct usernameUuid = usernameToUuid(username);
			if (!usernameUuid.isValid()) {
				return invalidEmbed(usernameUuid.failCause());
			}

			HypixelResponse guildResponse = getGuildFromPlayer(usernameUuid.uuid());
			if (!guildResponse.isValid()) {
				return invalidEmbed(guildResponse.failCause());
			}

			JsonElement guildJson = guildResponse.response();
			String guildId = higherDepth(guildJson, "_id").getAsString();
			String guildName = higherDepth(guildJson, "name").getAsString();
			JsonArray guildMembers = higherDepth(guildJson, "members").getAsJsonArray();

			JsonElement lbSettings;
			try {
				lbSettings =
					higherDepth(
						JsonParser.parseReader(new FileReader("src/main/java/com/skyblockplus/json/GuildSettings.json")),
						guildId + ".guild_leaderboard"
					)
						.getAsJsonObject();
			} catch (Exception e) {
				return invalidEmbed(
					guildName +
					"'s rank settings are not setup. Please join the [Skyblock Plus Discord](" +
					DISCORD_SERVER_INVITE_LINK +
					") and mention CrypticPlasma to setup this for your guild."
				);
			}

			String lbType = higherDepth(lbSettings, "lb_type").getAsString();
			List<String> ignoredRanks = streamJsonArray(higherDepth(lbSettings, "ignored_ranks"))
				.map(e -> e.getAsString().toLowerCase())
				.collect(Collectors.toCollection(ArrayList::new));
			ignoredRanks.add("guild master");
			List<String> rankTypes = new ArrayList<>();
			if (lbType.equals("position")) {
				for (JsonElement i : higherDepth(lbSettings, "types").getAsJsonArray()) {
					rankTypes.add(i.getAsString().toLowerCase());
				}
			}

			if (hypixelGuildQueue.contains(guildId)) {
				return invalidEmbed("This guild is currently updating, please try again in a few seconds");
			}
			hypixelGuildQueue.add(guildId);
			List<DataObject> playerList = leaderboardDatabase.getCachedPlayers(
				List.of("slayer", "skills", "catacombs", "weight", "networth"),
				gamemode,
				streamJsonArray(guildMembers).map(u -> higherDepth(u, "uuid", "")).collect(Collectors.toCollection(ArrayList::new)),
				hypixelKey,
				event
			);
			hypixelGuildQueue.remove(guildId);

			List<String> uniqueGuildName = new ArrayList<>();
			for (int i = playerList.size() - 1; i >= 0; i--) {
				DataObject lbM = playerList.get(i);
				String gMemUuid = lbM.getString("uuid");
				String gMemUsername = lbM.getString("username");
				JsonElement gMemJson = streamJsonArray(guildMembers)
					.filter(m -> higherDepth(m, "uuid", "").equals(gMemUuid))
					.findFirst()
					.orElse(null);
				String curRank = higherDepth(gMemJson, "rank", null);

				if (curRank == null || ignoredRanks.contains(curRank.toLowerCase())) {
					playerList.remove(i);
				} else {
					playerList
						.get(i)
						.put("rank", curRank.toLowerCase())
						.put(
							"gxp",
							higherDepth(gMemJson, "expHistory")
								.getAsJsonObject()
								.entrySet()
								.stream()
								.mapToDouble(g -> g.getValue().getAsDouble())
								.sum()
						)
						.put(
							"duration",
							Duration
								.between(Instant.now(), Instant.ofEpochMilli(higherDepth(gMemJson, "joined").getAsLong()))
								.abs()
								.toSeconds()
						);
					uniqueGuildName.add(gMemUsername);
				}
			}

			if (lbType.equals("position")) {
				List<DataObject> guildSlayer = playerList
					.stream()
					.sorted(Comparator.comparingDouble(o1 -> -o1.getDouble("slayer")))
					.collect(Collectors.toCollection(ArrayList::new));
				List<DataObject> guildSkills = playerList
					.stream()
					.sorted(Comparator.comparingDouble(o1 -> -o1.getDouble("skills")))
					.collect(Collectors.toCollection(ArrayList::new));
				List<DataObject> guildCatacombs = playerList
					.stream()
					.sorted(Comparator.comparingDouble(o1 -> -o1.getDouble("catacombs")))
					.collect(Collectors.toCollection(ArrayList::new));
				List<DataObject> guildWeight = playerList
					.stream()
					.sorted(Comparator.comparingDouble(o1 -> -o1.getDouble("weight")))
					.collect(Collectors.toCollection(ArrayList::new));

				for (String s : uniqueGuildName) {
					int slayerRank = -1;
					int skillsRank = -1;
					int catacombsRank = -1;
					int weightRank = -1;

					if (rankTypes.contains("slayer")) {
						for (int j = 0; j < guildSlayer.size(); j++) {
							try {
								if (s.equals(guildSlayer.get(j).getString("username"))) {
									slayerRank = j;
									break;
								}
							} catch (NullPointerException ignored) {}
						}
					}

					if (rankTypes.contains("skills")) {
						for (int j = 0; j < guildSkills.size(); j++) {
							try {
								if (s.equals(guildSkills.get(j).getString("username"))) {
									skillsRank = j;
									break;
								}
							} catch (NullPointerException ignored) {}
						}
					}

					if (rankTypes.contains("catacombs")) {
						for (int j = 0; j < guildCatacombs.size(); j++) {
							try {
								if (s.equals(guildCatacombs.get(j).getString("username"))) {
									catacombsRank = j;
									break;
								}
							} catch (NullPointerException ignored) {}
						}
					}

					if (rankTypes.contains("weight")) {
						for (int j = 0; j < guildWeight.size(); j++) {
							try {
								if (s.equals(guildWeight.get(j).getString("username"))) {
									weightRank = j;
									break;
								}
							} catch (NullPointerException ignored) {}
						}
					}

					if (guildName.equals("Skyblock Forceful")) {
						if (slayerRank < skillsRank) {
							guildSkills.set(skillsRank, null);
							if (slayerRank < catacombsRank) {
								guildCatacombs.set(catacombsRank, null);
							} else {
								guildSlayer.set(slayerRank, null);
							}
						} else {
							guildSlayer.set(slayerRank, null);
							if (skillsRank < catacombsRank) {
								guildCatacombs.set(catacombsRank, null);
							} else {
								guildSkills.set(skillsRank, null);
							}
						}
					}
				}

				List<List<DataObject>> guildLeaderboards = new ArrayList<>();

				if (rankTypes.contains("slayer")) {
					guildLeaderboards.add(guildSlayer);
				}
				if (rankTypes.contains("skills")) {
					guildLeaderboards.add(guildSkills);
				}
				if (rankTypes.contains("catacombs")) {
					guildLeaderboards.add(guildCatacombs);
				}
				if (rankTypes.contains("weight")) {
					guildLeaderboards.add(guildWeight);
				}

				JsonArray ranksArr = higherDepth(lbSettings, "ranks").getAsJsonArray();

				CustomPaginator.Builder paginateBuilder = event.getPaginator().setItemsPerPage(20);
				int totalChange = 0;
				for (List<DataObject> currentLeaderboard : guildLeaderboards) {
					for (int i = 0; i < currentLeaderboard.size(); i++) {
						DataObject currentPlayer = currentLeaderboard.get(i);
						if (currentPlayer == null) {
							continue;
						}

						if (ignoredRanks.contains(currentPlayer.getString("rank"))) {
							continue;
						}

						String playerRank = currentPlayer.getString("rank");
						String playerUsername = currentPlayer.getString("username");

						for (JsonElement rank : ranksArr) {
							if (i <= higherDepth(rank, "range", 0) - 1) {
								JsonArray rankNames = higherDepth(rank, "names").getAsJsonArray();
								List<String> rankNamesList = new ArrayList<>();
								for (JsonElement rankName : rankNames) {
									rankNamesList.add(rankName.getAsString());
								}

								if (!rankNamesList.contains(playerRank.toLowerCase())) {
									paginateBuilder.addItems(("- /g setrank " + fixUsername(playerUsername) + " " + rankNamesList.get(0)));
									totalChange++;
								}
								break;
							}
						}
					}
				}

				if (paginateBuilder.size() == 0) {
					return defaultEmbed("No rank changes");
				}

				paginateBuilder
					.getExtras()
					.setEveryPageTitle("Rank changes for " + guildName)
					.setEveryPageText("**Total rank changes:** " + totalChange);
				event.paginate(paginateBuilder);
			} else {
				List<String> pbItems = new ArrayList<>();
				int totalChange = 0;
				JsonObject defaultRankObj = higherDepth(lbSettings, "default_rank").getAsJsonObject();
				List<String> defaultRank = streamJsonArray(higherDepth(defaultRankObj, "names")).map(JsonElement::getAsString).toList();
				JsonArray defaultRanksArr = higherDepth(defaultRankObj, "requirements").getAsJsonArray();

				for (DataObject gMember : playerList) {
					if (!defaultRanksArr.isEmpty()) {
						boolean meetsReqOr = false;
						for (JsonElement reqOr : defaultRanksArr) {
							boolean meetsReqAnd = true;
							for (JsonElement reqAnd : reqOr.getAsJsonArray()) {
								String type = higherDepth(reqAnd, "type").getAsString();
								double amount = gMember.getDouble(type);

								double reqAmount = higherDepth(reqAnd, "amount").getAsDouble();
								if (higherDepth(reqAnd, "convert_from_level", false)) {
									reqAmount =
										levelingInfoFromLevel(
											(int) reqAmount,
											type,
											type.equals("farming") ? 60 : higherDepth(getLevelingJson(), "leveling_caps." + type, 0)
										)
											.totalExp();
								}

								if (amount < reqAmount) {
									meetsReqAnd = false;
									break;
								}
							}
							meetsReqOr = meetsReqAnd;
							if (meetsReqAnd) {
								break;
							}
						}

						if (!meetsReqOr) {
							pbItems.add("- /g kick " + fixUsername(gMember.getString("username")) + " doesn't meet reqs");
							totalChange++;
							continue;
						}
					}

					int highestRankMet = -1;
					JsonArray gRanks = higherDepth(lbSettings, "ranks").getAsJsonArray();
					for (int i = 0; i < gRanks.size(); i++) {
						JsonElement rank = gRanks.get(i); // e.g. [[a && b] || [c && d]]
						boolean meetsReqOr = false;

						for (JsonElement reqOr : higherDepth(rank, "requirements").getAsJsonArray()) {
							boolean meetsReqAnd = true;

							for (JsonElement reqAnd : reqOr.getAsJsonArray()) {
								String type = higherDepth(reqAnd, "type").getAsString();
								double amount = gMember.getDouble(type);

								double reqAmount = higherDepth(reqAnd, "amount").getAsDouble();
								if (higherDepth(reqAnd, "convert_from_level", false)) {
									reqAmount =
										levelingInfoFromLevel(
											(int) reqAmount,
											type,
											type.equals("farming") ? 60 : higherDepth(getLevelingJson(), "leveling_caps." + type).getAsInt()
										)
											.totalExp();
								}

								if (amount < reqAmount) {
									meetsReqAnd = false;
									break;
								}
							}

							if (meetsReqAnd) {
								meetsReqOr = true;
								break;
							}
						}

						if (meetsReqOr) {
							highestRankMet = Math.max(i, highestRankMet);
						}
					}

					if (highestRankMet != -1) {
						List<String> rankNamesList = streamJsonArray(higherDepth(gRanks.get(highestRankMet), "names"))
							.map(JsonElement::getAsString)
							.toList();
						if (!rankNamesList.contains(gMember.getString("rank").toLowerCase())) {
							pbItems.add(("- /g setrank " + fixUsername(gMember.getString("username")) + " " + rankNamesList.get(0)));
							totalChange++;
						}
					} else {
						if (!defaultRank.contains(gMember.getString("rank"))) {
							pbItems.add(("- /g setrank " + fixUsername(gMember.getString("username")) + " " + defaultRank.get(0)));
							totalChange++;
						}
					}
				}

				if (pbItems.isEmpty()) {
					return defaultEmbed("No rank changes");
				}

				CustomPaginator.Builder paginateBuilder = event
					.getPaginator()
					.setItemsPerPage(20)
					.addItems(pbItems.stream().sorted().toList());
				paginateBuilder
					.getExtras()
					.setEveryPageTitle("Rank changes for " + guildName)
					.setEveryPageText("**Total rank changes:** " + totalChange);

				event.paginate(paginateBuilder);
			}

			return null;
		}
	}

	public static class StatisticsSubcommand extends Subcommand {

		public StatisticsSubcommand() {
			this.name = "statistics";
			this.cooldown = globalCooldown + 2;
		}

		@Override
		protected void execute(SlashCommandEvent event) {
			String guild = event.getOptionStr("guild");
			if (guild != null) {
				event.embed(
					getStatistics(
						null,
						guild,
						event.getOptionBoolean("key", false),
						Player.Gamemode.of(event.getOptionStr("gamemode", "all")),
						event
					)
				);
				return;
			}

			if (event.invalidPlayerOption()) {
				return;
			}

			event.embed(
				getStatistics(
					event.player,
					null,
					event.getOptionBoolean("key", false),
					Player.Gamemode.of(event.getOptionStr("gamemode", "all")),
					event
				)
			);
		}

		@Override
		protected SubcommandData getCommandData() {
			return new SubcommandData(name, "Get a guild's Skyblock statistics of slayer, skills, catacombs, and weight")
				.addOption(OptionType.STRING, "player", "Player username or mention", false, true)
				.addOption(OptionType.STRING, "guild", "Guild name", false)
				.addOptions(
					new OptionData(OptionType.STRING, "gamemode", "Gamemode type")
						.addChoice("All", "all")
						.addChoice("Ironman", "ironman")
						.addChoice("Stranded", "stranded")
				)
				.addOption(OptionType.BOOLEAN, "key", "If the API key for this server should be used for more updated results");
		}

		public static EmbedBuilder getStatistics(
			String username,
			String guildName,
			boolean useKey,
			Player.Gamemode gamemode,
			SlashCommandEvent event
		) {
			String hypixelKey = null;
			if (useKey) {
				hypixelKey = database.getServerHypixelApiKey(event.getGuild().getId());

				EmbedBuilder eb = checkHypixelKey(hypixelKey);
				if (eb != null) {
					return eb;
				}
			}

			HypixelResponse guildResponse;
			UsernameUuidStruct usernameUuidStruct = null;
			if (username != null) {
				usernameUuidStruct = usernameToUuid(username);
				if (!usernameUuidStruct.isValid()) {
					return invalidEmbed(usernameUuidStruct.failCause());
				}

				guildResponse = getGuildFromPlayer(usernameUuidStruct.uuid());
			} else {
				guildResponse = getGuildFromName(guildName);
			}
			if (!guildResponse.isValid()) {
				return invalidEmbed(guildResponse.failCause());
			}

			JsonElement guildJson = guildResponse.response();
			guildName = higherDepth(guildJson, "name").getAsString();
			String guildId = higherDepth(guildJson, "_id").getAsString();

			if (hypixelGuildQueue.contains(guildId)) {
				return invalidEmbed("This guild is currently updating, please try again in a few seconds");
			}

			hypixelGuildQueue.add(guildId);
			List<DataObject> playerList = leaderboardDatabase.getCachedPlayers(
				List.of("networth", "level", "slayer", "skills", "catacombs", "weight"),
				gamemode,
				streamJsonArray(higherDepth(guildJson, "members"))
					.map(u -> higherDepth(u, "uuid", ""))
					.collect(Collectors.toCollection(ArrayList::new)),
				hypixelKey,
				event
			);
			hypixelGuildQueue.remove(guildId);

			String levelStr = getLeaderboardTop(playerList, "level", usernameUuidStruct);
			String networthStr = getLeaderboardTop(playerList, "networth", usernameUuidStruct);
			String slayerStr = getLeaderboardTop(playerList, "slayer", usernameUuidStruct);
			String skillsStr = getLeaderboardTop(playerList, "skills", usernameUuidStruct);
			String cataStr = getLeaderboardTop(playerList, "catacombs", usernameUuidStruct);
			String weightStr = getLeaderboardTop(playerList, "weight", usernameUuidStruct);

			double averageLevel = playerList.stream().mapToDouble(m -> m.getDouble("level")).average().orElse(0);
			double averageNetworth = (long) playerList.stream().mapToDouble(m -> m.getDouble("networth")).average().orElse(0);
			double averageSlayer = playerList.stream().mapToDouble(m -> m.getDouble("slayer")).average().orElse(0);
			double averageSkills = playerList.stream().mapToDouble(m -> m.getDouble("skills")).average().orElse(0);
			double averageCata = playerList.stream().mapToDouble(m -> m.getDouble("catacombs")).average().orElse(0);
			double averageWeight = playerList.stream().mapToDouble(m -> m.getDouble("weight")).average().orElse(0);

			return defaultEmbed(guildName)
				.setDescription(
					"**Average Skyblock Level:** " +
					roundAndFormat(averageLevel) +
					"\n**Average Networth** " +
					roundAndFormat(averageNetworth) +
					"\n**Average Slayer XP:** " +
					roundAndFormat(averageSlayer) +
					"\n**Average Skills Level:** " +
					roundAndFormat(averageSkills) +
					"\n**Average Catacombs XP:** " +
					roundAndFormat(averageCata) +
					"\n**Average Weight:** " +
					roundAndFormat(averageWeight)
				)
				.addField("Top 5 Skyblock Level", levelStr, true)
				.addField("Top 5 Networth", networthStr, true)
				.addBlankField(true)
				.addField("Top 5 Slayer", slayerStr, true)
				.addField("Top 5 Skills", skillsStr, true)
				.addBlankField(true)
				.addField("Top 5 Catacombs", cataStr, true)
				.addField("Top 5 Weight", weightStr, true)
				.addBlankField(true);
		}

		private static String getLeaderboardTop(List<DataObject> playerList, String lbType, UsernameUuidStruct usernameUuidStruct) {
			List<DataObject> lb = playerList.stream().sorted(Comparator.comparingDouble(m -> -m.getDouble(lbType))).toList();

			int pos = -1;
			if (usernameUuidStruct != null) {
				for (int i = 0; i < lb.size(); i++) {
					if (lb.get(i).getString("uuid").equals(usernameUuidStruct.uuid())) {
						pos = i;
					}
				}
			}

			StringBuilder str = new StringBuilder();
			for (int i = 0; i < Math.min(5, lb.size()); i++) {
				if (pos > 5 && i == 3) {
					DataObject cur = lb.get(pos);
					str
						.append("...\n`")
						.append(pos + 1)
						.append(")` ")
						.append(fixUsername(cur.getString("username")))
						.append(": ")
						.append(roundAndFormat(cur.getDouble(lbType)))
						.append("\n");
					break;
				}
				DataObject cur = lb.get(i);
				str
					.append("`")
					.append(i + 1)
					.append(")` ")
					.append(fixUsername(cur.getString("username")))
					.append(": ")
					.append(roundAndFormat(cur.getDouble(lbType)))
					.append("\n");
			}
			return str.toString();
		}
	}

	public static class ApiSubcommand extends Subcommand {

		public ApiSubcommand() {
			this.name = "api";
			this.cooldown = globalCooldown + 2;
		}

		@Override
		protected void execute(SlashCommandEvent event) {
			if (event.invalidPlayerOption()) {
				return;
			}

			event.paginate(getGuildCheckApi(event.player, event.getOptionStr("exclude", ""), event));
		}

		@Override
		protected SubcommandData getCommandData() {
			return new SubcommandData(name, "Get which Skyblock APIs players have enabled or disabled for a guild")
				.addOption(OptionType.STRING, "player", "Player username or mention", false, true)
				.addOption(OptionType.STRING, "exclude", "Exclude certain APIs from being checked (comma separated)", false);
		}

		public static EmbedBuilder getGuildCheckApi(String username, String exclude, SlashCommandEvent event) {
			List<String> excludeArr = new ArrayList<>();
			if (!exclude.isEmpty()) {
				excludeArr.addAll(List.of(exclude.toLowerCase().split(",")));
				for (String s : excludeArr) {
					if (!List.of("inventory", "bank", "collections", "vault", "skills").contains(s)) {
						return invalidEmbed("Invalid exclude type: " + s);
					}
				}
			}

			String hypixelKey = database.getServerHypixelApiKey(event.getGuild().getId());

			EmbedBuilder eb = checkHypixelKey(hypixelKey);
			if (eb != null) {
				return eb;
			}

			UsernameUuidStruct usernameUuid = usernameToUuid(username);
			if (!usernameUuid.isValid()) {
				return invalidEmbed(usernameUuid.failCause());
			}

			HypixelResponse guildResponse = getGuildFromPlayer(usernameUuid.uuid());
			if (!guildResponse.isValid()) {
				return invalidEmbed(guildResponse.failCause());
			}

			JsonArray guildMembers = guildResponse.get("members").getAsJsonArray();
			List<CompletableFuture<String>> futuresList = new ArrayList<>();
			List<Player> players = new ArrayList<>();

			for (JsonElement guildMember : guildMembers) {
				String guildMemberUuid = higherDepth(guildMember, "uuid").getAsString();

				try {
					if (keyCooldownMap.get(hypixelKey).isRateLimited()) {
						System.out.println("Sleeping for " + keyCooldownMap.get(hypixelKey).getTimeTillReset() + " seconds");
						TimeUnit.SECONDS.sleep(keyCooldownMap.get(hypixelKey).getTimeTillReset());
					}
				} catch (Exception ignored) {}

				futuresList.add(
					asyncSkyblockProfilesFromUuid(guildMemberUuid, hypixelKey)
						.thenApplyAsync(
							guildMemberProfileJsonResponse -> {
								Player player = new Player(
									guildMemberUuid,
									uuidToUsername(guildMemberUuid).username(),
									guildMemberProfileJsonResponse,
									false
								);

								if (player.isValid()) {
									boolean invEnabled = excludeArr.contains("inventory") || player.isInventoryApiEnabled();
									boolean bankEnabled = excludeArr.contains("bank") || player.isBankApiEnabled();
									boolean collectionsEnabled = excludeArr.contains("collections") || player.isCollectionsApiEnabled();
									boolean vaultEnabled = excludeArr.contains("vault") || player.isVaultApiEnabled();
									boolean skillsEnabled = excludeArr.contains("skills") || player.isSkillsApiEnabled();

									if (invEnabled && bankEnabled && collectionsEnabled && vaultEnabled && skillsEnabled) {
										return client.getSuccess() + " **" + player.getUsernameFixed() + ":** all APIs enabled";
									} else {
										String out =
											(invEnabled ? "" : "Inventory API, ") +
											(bankEnabled ? "" : "Bank API, ") +
											(collectionsEnabled ? "" : "Collections API, ") +
											(vaultEnabled ? "" : "Vault API, ") +
											(skillsEnabled ? "" : "Skills API, ");

										return (
											client.getError() +
											" **" +
											player.getUsernameFixed() +
											":** " +
											out.substring(0, out.length() - 2)
										);
									}
								}
								return client.getError() + " **" + player.getUsernameFixed() + ":** unable to get data";
							},
							executor
						)
				);
			}

			List<String> out = new ArrayList<>();
			for (CompletableFuture<String> future : futuresList) {
				try {
					out.add(future.get());
				} catch (Exception e) {
					e.printStackTrace();
				}
			}

			leaderboardDatabase.insertIntoLeaderboard(players);

			out.sort(Comparator.comparing(o -> !o.contains(client.getError())));
			CustomPaginator.Builder paginator = event.getPaginator().setItemsPerPage(20);
			paginator.addItems(out);
			event.paginate(
				paginator.updateExtras(extra ->
					extra
						.setEveryPageTitle(guildResponse.get("name").getAsString())
						.setEveryPageText(
							"**API Disabled Count:** " +
							out.stream().filter(o -> o.contains(client.getError())).count() +
							"\n" +
							(!excludeArr.isEmpty() ? "**Excluded APIs:** " + String.join(", ", excludeArr) + "\n" : "")
						)
				)
			);
			return null;
		}
	}

	@Override
	public SlashCommandData getCommandData() {
		return Commands.slash(name, "Main guild command");
	}

	@Override
	public void onAutoComplete(AutoCompleteEvent event) {
		if (event.getFocusedOption().getName().equals("player")) {
			event.replyClosestPlayer();
		} else if (event.getFocusedOption().getName().equals("type")) {
			event.replyClosestMatch(event.getFocusedOption().getValue(), formattedTypesSubList);
		}
	}
}
