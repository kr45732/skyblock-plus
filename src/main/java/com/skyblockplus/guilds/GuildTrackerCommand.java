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

package com.skyblockplus.guilds;

import static com.skyblockplus.Main.database;
import static com.skyblockplus.utils.ApiHandler.*;
import static com.skyblockplus.utils.Utils.*;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.skyblockplus.utils.Player;
import com.skyblockplus.utils.command.CommandExecute;
import com.skyblockplus.utils.command.CustomPaginator;
import com.skyblockplus.utils.command.PaginatorEvent;
import com.skyblockplus.utils.structs.HypixelGuildCache;
import com.skyblockplus.utils.structs.HypixelResponse;
import com.skyblockplus.utils.structs.PaginatorExtras;
import com.skyblockplus.utils.structs.UsernameUuidStruct;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;

public class GuildTrackerCommand extends Command {

	private static final Pattern removeZeroProfilesPattern = Pattern.compile(".*[1-9].*");

	public GuildTrackerCommand() {
		this.name = "guild-tracker";
		this.cooldown = globalCooldown + 1;
		this.aliases = new String[] { "g-tracker" };
		this.botPermissions = defaultPerms();
	}

	public static EmbedBuilder startGuildTracker(String username, Guild guild) {
		String hypixelKey = database.getServerHypixelApiKey(guild.getId());

		EmbedBuilder eb = checkHypixelKey(hypixelKey);
		if (eb != null) {
			return eb;
		}

		UsernameUuidStruct usernameUuidStruct = usernameToUuid(username);
		if (usernameUuidStruct.isNotValid()) {
			return invalidEmbed(usernameUuidStruct.getFailCause());
		}

		HypixelResponse guildResponse = getGuildFromPlayer(usernameUuidStruct.getUuid());
		if (guildResponse.isNotValid()) {
			return invalidEmbed(guildResponse.getFailCause());
		}

		return addTrackingGuild(guild.getId(), guildResponse.get("_id").getAsString())
			? defaultEmbed("Success").setDescription("Now tracking " + guildResponse.get("name").getAsString())
			: invalidEmbed("You are tracking 3/3 guilds");
	}

	public static EmbedBuilder stopGuildTracker(String username, Guild guild) {
		UsernameUuidStruct usernameUuidStruct = usernameToUuid(username);
		if (usernameUuidStruct.isNotValid()) {
			return invalidEmbed(usernameUuidStruct.getFailCause());
		}

		HypixelResponse guildResponse = getGuildFromPlayer(usernameUuidStruct.getUuid());
		if (guildResponse.isNotValid()) {
			return invalidEmbed(guildResponse.getFailCause());
		}

		return removeTrackingGuild(guild.getId(), guildResponse.get("_id").getAsString())
			? defaultEmbed("Success").setDescription("Stopped tracking " + guildResponse.get("name"))
			: invalidEmbed("You are not tracking " + guildResponse.get("name").getAsString());
	}

	public static EmbedBuilder getGuildTracker(String username, PaginatorEvent event) {
		UsernameUuidStruct usernameUuidStruct = usernameToUuid(username);
		if (usernameUuidStruct.isNotValid()) {
			return invalidEmbed(usernameUuidStruct.getFailCause());
		}

		HypixelResponse guildResponse = getGuildFromPlayer(usernameUuidStruct.getUuid());
		if (guildResponse.isNotValid()) {
			return invalidEmbed(guildResponse.getFailCause());
		}

		for (JsonElement guildObject : getTrackingGuilds()) {
			if (higherDepth(guildObject, "guild_id").getAsString().equals(guildResponse.get("_id").getAsString())) {
				CustomPaginator.Builder paginateBuilder = defaultPaginator(event.getUser()).setColumns(1).setItemsPerPage(10);
				PaginatorExtras extras = new PaginatorExtras(PaginatorExtras.PaginatorType.EMBED_FIELDS)
					.setEveryPageTitle(guildResponse.get("name").getAsString())
					.setEveryPageTitleUrl("https://hypixel-leaderboard.senither.com/guilds/" + guildResponse.get("_id").getAsString());

				JsonArray days = higherDepth(guildObject, "days").getAsJsonArray();
				if (higherDepth(days, "[0].members") == null) {
					return defaultEmbed("No data currently for this guild. Data is added at midnight EST every day");
				}
				JsonArray dayMembers = higherDepth(days, "[0].members").getAsJsonArray();
				for (JsonElement dayMember : dayMembers) {
					StringBuilder dayMemberStr = new StringBuilder();
					for (JsonElement dayProfile : higherDepth(dayMember, "profiles").getAsJsonArray()) {
						StringBuilder curProfileFarming = new StringBuilder(
							"Farming: " + simplifyNumber(higherDepth(dayProfile, "farming", 0L))
						);
						StringBuilder curProfileMining = new StringBuilder(
							"Mining: " + simplifyNumber(higherDepth(dayProfile, "mining", 0L))
						);
						for (int i = 1; i < days.size(); i++) {
							for (JsonElement otherDayMembers : higherDepth(days, "[" + i + "].members").getAsJsonArray()) {
								if (
									higherDepth(otherDayMembers, "uuid").getAsString().equals(higherDepth(dayMember, "uuid").getAsString())
								) {
									for (JsonElement otherDayProfile : higherDepth(otherDayMembers, "profiles").getAsJsonArray()) {
										if (
											higherDepth(otherDayProfile, "name")
												.getAsString()
												.equals(higherDepth(dayProfile, "name").getAsString())
										) {
											curProfileFarming
												.append(" ➜ ")
												.append(simplifyNumber(higherDepth(otherDayProfile, "farming", 0L)));
											curProfileMining
												.append(" ➜ ")
												.append(simplifyNumber(higherDepth(otherDayProfile, "mining", 0L)));
											break;
										}
									}
									break;
								}
							}
						}
						dayMemberStr
							.append(higherDepth(dayProfile, "name").getAsString())
							.append(" | ")
							.append(curProfileFarming)
							.append(" | ")
							.append(curProfileMining)
							.append("\n");
					}
					Matcher matcher = removeZeroProfilesPattern.matcher(dayMemberStr.toString().trim());
					dayMemberStr = new StringBuilder();
					while (matcher.find()) {
						dayMemberStr.append(matcher.group()).append("\n");
					}
					if (dayMemberStr.length() > 0) {
						extras.addEmbedField(higherDepth(dayMember, "username").getAsString(), dayMemberStr.toString().trim(), false);
					}
				}
				if (extras.getEmbedFields().size() == 0) {
					return defaultEmbed("No data currently for this guild. Data is added at midnight EST every day");
				}
				event.paginate(paginateBuilder.setPaginatorExtras(extras));
				return null;
			}
		}
		return invalidEmbed(guildResponse.get("name").getAsString() + " is not currently being tracked");
	}

	public static void updateTrackingGuilds() {
		try {
			JsonArray trackingArray = getTrackingGuilds();

			for (int i = trackingArray.size() - 1; i >= 0; i--) {
				try {
					JsonObject guildObject = trackingArray.get(i).getAsJsonObject();

					String hypixelKey = null;
					for (JsonElement requestedBy : higherDepth(guildObject, "requested_by").getAsJsonArray()) {
						hypixelKey = database.getServerHypixelApiKey(requestedBy.getAsString());
						if (hypixelKey != null) {
							break;
						}
					}
					if (hypixelKey == null) {
						trackingArray.remove(i);
						continue;
					}
					String finalHypixelKey = hypixelKey;

					HypixelResponse guildResponse = getGuildFromId(higherDepth(guildObject, "guild_id").getAsString());
					if (guildResponse.isNotValid()) {
						trackingArray.remove(i);
						continue;
					}

					JsonElement guildJson = guildResponse.getResponse();
					String guildId = higherDepth(guildJson, "_id").getAsString();
					HypixelGuildCache newGuildCache = new HypixelGuildCache();
					JsonArray guildMembers = higherDepth(guildJson, "members").getAsJsonArray();
					List<CompletableFuture<String>> futuresList = new ArrayList<>();

					JsonArray newMembersArray = new JsonArray();
					for (JsonElement guildMember : guildMembers) {
						String guildMemberUuid = higherDepth(guildMember, "uuid").getAsString();

						CompletableFuture<String> guildMemberUsername = asyncUuidToUsername(guildMemberUuid);
						futuresList.add(
							guildMemberUsername.thenApply(guildMemberUsernameResponse -> {
								try {
									if (keyCooldownMap.get(finalHypixelKey).getRemainingLimit().get() < 5) {
										System.out.println(
											"Sleeping for " + keyCooldownMap.get(finalHypixelKey).getTimeTillReset().get() + " seconds"
										);
										TimeUnit.SECONDS.sleep(keyCooldownMap.get(finalHypixelKey).getTimeTillReset().get());
									}
								} catch (Exception ignored) {}

								// TODO: move skyblockProfilesFromUuid back to async if better hosting service
								Player guildMemberPlayer = new Player(
									guildMemberUuid,
									guildMemberUsernameResponse,
									skyblockProfilesFromUuid(guildMemberUuid, finalHypixelKey).getResponse()
								);

								JsonObject memberObj = new JsonObject();
								memberObj.addProperty("username", guildMemberPlayer.getUsername());
								memberObj.addProperty("uuid", guildMemberPlayer.getUuid());
								JsonArray profilesObject = new JsonArray();
								for (JsonElement profile : guildMemberPlayer.getProfileArray()) {
									JsonObject profileObj = new JsonObject();
									profileObj.add("name", higherDepth(profile, "cute_name"));
									int scale = Math.min(higherDepth(profile, "members").getAsJsonObject().size(), 3);
									JsonElement col = higherDepth(profile, "members." + guildMemberPlayer.getUuid() + ".collection");
									profileObj.addProperty(
										"farming",
										scale *
										(
											higherDepth(col, "POTATO_ITEM", 0L) +
											higherDepth(col, "CARROT_ITEM", 0L) +
											higherDepth(col, "WHEAT", 0L) +
											higherDepth(col, "INK_SACK:3", 0L) +
											higherDepth(col, "SUGAR_CANE", 0L) +
											higherDepth(col, "NETHER_STALK", 0L)
										)
									);
									profileObj.addProperty(
										"mining",
										scale *
										(
											higherDepth(col, "DIAMOND", 0L) +
											higherDepth(col, "GOLD_INGOT", 0L) +
											higherDepth(col, "IRON_INGOT", 0L) +
											higherDepth(col, "COAL", 0L) +
											higherDepth(col, "REDSTONE", 0L) +
											higherDepth(col, "QUARTZ", 0L) +
											higherDepth(col, "EMERALD", 0L)
										)
									);
									profilesObject.add(profileObj);
								}
								memberObj.add("profiles", profilesObject);
								newMembersArray.add(memberObj);

								if (guildMemberPlayer.isValid()) {
									newGuildCache.addPlayer(guildMemberPlayer);
								}
								return null;
							})
						);
					}

					for (CompletableFuture<String> future : futuresList) {
						try {
							future.get();
						} catch (Exception ignored) {}
					}

					hypixelGuildsCacheMap.put(guildId, newGuildCache.setLastUpdated());

					JsonObject dayObject = new JsonObject();
					dayObject.addProperty("time", Instant.now().toEpochMilli());
					dayObject.add("members", newMembersArray);
					JsonArray daysObject = higherDepth(guildObject, "days").getAsJsonArray();
					if (daysObject.size() >= 3) {
						daysObject.remove(
							streamJsonArray(daysObject)
								.min(Comparator.comparingLong(day -> higherDepth(day, "time").getAsLong()))
								.orElse(null)
						);
					}
					daysObject.add(dayObject);
					guildObject.add("days", daysObject);
					trackingArray.set(i, guildObject);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}

			setTrackingJson(trackingArray);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void initialize() {
		ZoneId z = ZoneId.of("America/New_York");
		long minutesTillMidnight = Duration
			.between(Instant.now(), ZonedDateTime.now(z).toLocalDate().plusDays(1).atStartOfDay(z))
			.toMinutes();
		scheduler.scheduleAtFixedRate(
			GuildTrackerCommand::updateTrackingGuilds,
			minutesTillMidnight,
			TimeUnit.DAYS.toMinutes(1),
			TimeUnit.MINUTES
		);
	}

	@Override
	protected void execute(CommandEvent event) {
		new CommandExecute(this, event) {
			@Override
			protected void execute() {
				logCommand();

				if (args.length == 3 && args[1].equals("start") && args[2].startsWith("u:")) {
					embed(startGuildTracker(args[2].split("u:")[1], event.getGuild()));
					return;
				} else if (args.length == 3 && args[1].equals("stop") && args[2].startsWith("u:")) {
					embed(stopGuildTracker(args[2].split("u:")[1], event.getGuild()));
					return;
				} else if (args.length == 3 && args[1].equals("get") && args[2].startsWith("u:")) {
					paginate(getGuildTracker(args[2].split("u:")[1], new PaginatorEvent(event)));
					return;
				}

				sendErrorEmbed();
			}
		}
			.submit();
	}

	public static boolean addTrackingGuild(String serverId, String guildId) {
		JsonArray trackingArray = getTrackingGuilds();

		int numTracking = 0;
		for (int i = trackingArray.size() - 1; i >= 0; i--) {
			JsonObject guildObject = trackingArray.get(i).getAsJsonObject();
			if (higherDepth(guildObject, "guild_id", "").equals(guildId)) {
				JsonArray requestedBy = higherDepth(guildObject, "requested_by").getAsJsonArray();
				for (JsonElement req : requestedBy) {
					if (req.getAsString().equals(serverId)) {
						return true;
					}
				}
				requestedBy.add(serverId);
				guildObject.add("requested_by", requestedBy);
				trackingArray.set(i, guildObject);
				setTrackingJson(trackingArray);
				return true;
			}

			if (streamJsonArray(guildObject.get("requested_by").getAsJsonArray()).anyMatch(g -> g.getAsString().equals(serverId))) {
				numTracking++;
			}
		}

		if (numTracking == 3) {
			return false;
		}

		JsonObject guildObject = new JsonObject();
		guildObject.addProperty("guild_id", guildId);
		JsonArray requestedBy = new JsonArray();
		requestedBy.add(serverId);
		guildObject.add("requested_by", requestedBy);
		guildObject.add("days", new JsonArray());
		trackingArray.add(guildObject);

		setTrackingJson(trackingArray);
		return true;
	}

	public static void setTrackingJson(JsonElement data) {
		try (Statement statement = cacheDatabaseConnection.createStatement()) {
			statement.executeUpdate("INSERT INTO tracking VALUES (1, '" + data + "') ON DUPLICATE KEY UPDATE data = VALUES(data)");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static JsonArray getTrackingGuilds() {
		try (Statement statement = getCacheDatabaseConnection().createStatement()) {
			try (ResultSet response = statement.executeQuery("SELECT * FROM tracking")) {
				response.next();
				return JsonParser.parseString(response.getString("data")).getAsJsonArray();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public static boolean removeTrackingGuild(String serverId, String guildId) {
		JsonArray trackingGuilds = getTrackingGuilds();

		for (int i = trackingGuilds.size() - 1; i >= 0; i--) {
			JsonObject guildObject = trackingGuilds.get(i).getAsJsonObject();
			if (higherDepth(guildObject, "guild_id", "").equals(guildId)) {
				JsonArray requestedBy = higherDepth(guildObject, "requested_by").getAsJsonArray();
				for (JsonElement requested : requestedBy) {
					if (requested.getAsString().equals(serverId)) {
						requestedBy.remove(requested);
						if (requestedBy.size() == 0) {
							trackingGuilds.remove(i);
						} else {
							guildObject.add("requested_by", requestedBy);
							trackingGuilds.set(i, guildObject);
						}
						setTrackingJson(trackingGuilds);
						return true;
					}
				}
			}
		}

		return false;
	}
}
