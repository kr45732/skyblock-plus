/*
 * Skyblock Plus - A Skyblock focused Discord bot with many commands and customizable features to improve the experience of Skyblock players and guild staff!
 * Copyright (c) 2022 kr45732
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

import static com.skyblockplus.features.listeners.MainListener.guildMap;
import static com.skyblockplus.utils.ApiHandler.*;
import static com.skyblockplus.utils.Utils.*;
import static com.skyblockplus.utils.structs.HypixelGuildCache.types;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import com.skyblockplus.features.jacob.JacobData;
import com.skyblockplus.features.jacob.JacobHandler;
import com.skyblockplus.features.party.Party;
import com.skyblockplus.price.AuctionTracker;
import com.skyblockplus.utils.structs.HypixelResponse;
import com.skyblockplus.utils.structs.UsernameUuidStruct;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.lang.reflect.Type;
import java.sql.*;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import net.dv8tion.jda.api.utils.data.DataObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CacheDatabase {

	private static final Logger log = LoggerFactory.getLogger(CacheDatabase.class);

	private final HikariDataSource dataSource;
	private final ConcurrentHashMap<String, Instant> uuidToTimeSkyblockProfiles = new ConcurrentHashMap<>();
	private final List<Player.Gamemode> leaderboardGamemodes = Arrays.asList(
		Player.Gamemode.ALL,
		Player.Gamemode.IRONMAN,
		Player.Gamemode.STRANDED
	);
	public int guildCount = 2;

	public CacheDatabase() {
		HikariConfig config = new HikariConfig();
		config.setJdbcUrl(PLANET_SCALE_URL);
		config.setUsername(PLANET_SCALE_USERNAME);
		config.setPassword(PLANET_SCALE_PASSWORD);
		dataSource = new HikariDataSource(config);

		if (isMainBot()) {
			scheduler.scheduleAtFixedRate(this::updateLeaderboard, 1, 1, TimeUnit.MINUTES);
		}
	}

	public Connection getConnection() throws SQLException {
		return dataSource.getConnection();
	}

	public void cacheJson(String uuid, JsonElement json) {
		executor.submit(() -> {
			try (Connection connection = getConnection(); Statement statement = connection.createStatement()) {
				Instant now = Instant.now();
				uuidToTimeSkyblockProfiles.put(uuid, now);

				statement.executeUpdate(
					"INSERT INTO profiles VALUES ('" +
					uuid +
					"', " +
					now.toEpochMilli() +
					", '" +
					json +
					"') ON DUPLICATE KEY UPDATE uuid = VALUES(uuid), time = VALUES(time), data = VALUES(data)"
				);
			} catch (Exception ignored) {}
		});
	}

	public JsonElement getCachedJson(String uuid) {
		Instant lastUpdated = uuidToTimeSkyblockProfiles.getOrDefault(uuid, null);
		if (lastUpdated != null && Duration.between(lastUpdated, Instant.now()).toMillis() > 90000) {
			deleteCachedJson(uuid);
		} else {
			try (
				Connection connection = getConnection();
				PreparedStatement statement = connection.prepareStatement("SELECT * FROM profiles where uuid = ?")
			) {
				statement.setString(1, uuid);
				try (ResultSet response = statement.executeQuery()) {
					if (response.next()) {
						Instant lastUpdatedResponse = Instant.ofEpochMilli(response.getLong("time"));
						if (Duration.between(lastUpdatedResponse, Instant.now()).toMillis() > 90000) {
							deleteCachedJson(uuid);
						} else {
							uuidToTimeSkyblockProfiles.put(uuid, lastUpdatedResponse);
							return JsonParser.parseString(response.getString("data"));
						}
					}
				}
			} catch (Exception ignored) {}
		}
		return null;
	}

	public void deleteCachedJson(String... uuids) {
		if (uuids.length == 0) {
			return;
		}

		executor.submit(() -> {
			StringBuilder query = new StringBuilder();
			for (String uuid : uuids) {
				uuidToTimeSkyblockProfiles.remove(uuid);
				query.append("'").append(uuid).append("',");
			}
			if (query.charAt(query.length() - 1) == ',') {
				query.deleteCharAt(query.length() - 1);
			}

			try (Connection connection = getConnection(); Statement statement = connection.createStatement()) {
				statement.executeUpdate("DELETE FROM profiles WHERE uuid IN (" + query + ")");
			} catch (Exception ignored) {}
		});
	}

	public void updateCache() {
		long now = Instant.now().minusSeconds(90).toEpochMilli();
		try (
			Connection connection = getConnection();
			PreparedStatement statement = connection.prepareStatement("SELECT uuid FROM profiles WHERE time < ?")
		) {
			statement.setLong(1, now);
			try (ResultSet response = statement.executeQuery()) {
				List<String> expiredCacheUuidList = new ArrayList<>();
				while (response.next()) {
					expiredCacheUuidList.add(response.getString("uuid"));
				}
				deleteCachedJson(expiredCacheUuidList.toArray(new String[0]));
			}
		} catch (Exception ignored) {}

		try (
			Connection connection = getConnection();
			PreparedStatement statement = connection.prepareStatement("DELETE FROM profiles WHERE time < ?")
		) {
			statement.setLong(1, now);
			statement.executeUpdate();
		} catch (Exception ignored) {}
	}

	public boolean cachePartyData(String guildId, String json) {
		try (Connection connection = getConnection(); Statement statement = connection.createStatement()) {
			statement.executeUpdate(
				"INSERT INTO party VALUES ('" +
				guildId +
				"', '" +
				json +
				"') ON DUPLICATE KEY UPDATE guild_id = VALUES(guild_id), data = VALUES(data)"
			);
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	public boolean cacheCommandUsage(String json) {
		try (Connection connection = getConnection(); Statement statement = connection.createStatement()) {
			statement.executeUpdate("INSERT INTO commands VALUES (0, '" + json + "') ON DUPLICATE KEY UPDATE data = VALUES(data)");
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	public boolean cacheAhTracker(String json) {
		try (Connection connection = getConnection(); Statement statement = connection.createStatement()) {
			statement.executeUpdate("INSERT INTO ah_track VALUES (0, '" + json + "') ON DUPLICATE KEY UPDATE data = VALUES(data)");
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	public boolean cacheJacobData(String json) {
		try (Connection connection = getConnection(); Statement statement = connection.createStatement()) {
			statement.executeUpdate("INSERT INTO jacob VALUES (0, '" + json + "') ON DUPLICATE KEY UPDATE data = VALUES(data)");
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	public void initializeAhTracker() {
		if (!isMainBot()) {
			return;
		}

		try (Connection connection = getConnection(); PreparedStatement statement = connection.prepareStatement("SELECT * FROM ah_track")) {
			try (ResultSet response = statement.executeQuery()) {
				response.next();
				JsonObject data = JsonParser.parseString(response.getString("data")).getAsJsonObject();
				for (Map.Entry<String, JsonElement> entry : data.entrySet()) {
					AuctionTracker.insertAhTrack(
						entry.getKey(),
						new UsernameUuidStruct(
							higherDepth(entry.getValue(), "username").getAsString(),
							higherDepth(entry.getValue(), "uuid").getAsString()
						)
					);
				}
				log.info("Retrieved auction tracker | " + data.size());
			}
		} catch (Exception e) {
			log.error("", e);
		}
	}

	public void initializeCommandUses() {
		if (!isMainBot()) {
			return;
		}

		try (Connection connection = getConnection(); PreparedStatement statement = connection.prepareStatement("SELECT * FROM commands")) {
			try (ResultSet response = statement.executeQuery()) {
				response.next();
				Map<String, Integer> commandUsage = gson.fromJson(
					response.getString("data"),
					new TypeToken<Map<String, Integer>>() {}.getType()
				);
				slashCommandClient.setCommandUses(commandUsage);
				log.info("Retrieved command uses");
			}
		} catch (Exception e) {
			log.error("", e);
		}
	}

	public void initializeJacobData() {
		if (!isMainBot()) {
			return;
		}

		try (Connection connection = getConnection(); PreparedStatement statement = connection.prepareStatement("SELECT * FROM jacob")) {
			try (ResultSet response = statement.executeQuery()) {
				response.next();
				JacobHandler.setJacobData(gson.fromJson(response.getString("data"), JacobData.class));
				log.info("Retrieved jacob data");
			}
		} catch (Exception e) {
			log.error("", e);
		}
	}

	public void initializeParties() {
		if (!isMainBot()) {
			return;
		}

		List<String> toDeleteIds = new ArrayList<>();
		try (Connection connection = getConnection(); PreparedStatement statement = connection.prepareStatement("SELECT * FROM party")) {
			try (ResultSet response = statement.executeQuery()) {
				Type partyListType = new TypeToken<List<Party>>() {}.getType();
				while (response.next()) {
					String guildId = null;
					try {
						guildId = response.getString("guild_id");
						toDeleteIds.add(guildId);
						List<Party> partyList = gson.fromJson(response.getString("data"), partyListType);
						guildMap.get(guildId).setPartyList(partyList);
						log.info("Retrieved party cache (" + partyList.size() + ") - guildId={" + guildId + "}");
					} catch (Exception e) {
						log.error("guildId={" + guildId + "}", e);
					}
				}
			}
		} catch (Exception ignored) {}

		try (Connection connection = getConnection(); Statement statement = connection.createStatement()) {
			statement.executeUpdate("DELETE FROM party WHERE guild_id IN (" + String.join(",", toDeleteIds) + ")");
		} catch (Exception ignored) {}
	}

	public void insertIntoLeaderboard(Player player) {
		insertIntoLeaderboard(player, true);
	}

	public void insertIntoLeaderboard(Player player, boolean makeCopy) {
		executor.submit(() -> {
			Player finalPlayer = makeCopy ? player.copy() : player;
			for (Player.Gamemode gamemode : leaderboardGamemodes) {
				if (player.isValid()) {
					insertIntoLeaderboard(finalPlayer, gamemode);
				} else {
					deleteFromLeaderboard(finalPlayer.getUuid(), gamemode);
				}
			}
		});
	}

	private void insertIntoLeaderboard(Player player, Player.Gamemode gamemode) {
		try (
			Connection connection = getConnection();
			PreparedStatement statement = connection.prepareStatement(
				"INSERT INTO " +
				gamemode.toCacheType() +
				" (last_updated, " +
				String.join(", ", types) +
				") VALUES (" +
				String.join(", ", Collections.nCopies(types.size() + 1, "?")) +
				") ON DUPLICATE KEY UPDATE last_updated = VALUES(last_updated), " +
				types.stream().map(type -> type + " = VALUES(" + type + ")").collect(Collectors.joining(", "))
			)
		) {
			statement.setLong(1, Instant.now().toEpochMilli());
			statement.setString(2, player.getUsername());
			statement.setString(3, player.getUuid());
			for (int i = 2; i < types.size(); i++) {
				String type = types.get(i);
				statement.setDouble(
					i + 2,
					player.getHighestAmount(
						type +
						switch (type) {
							case "catacombs",
								"alchemy",
								"combat",
								"fishing",
								"farming",
								"foraging",
								"carpentry",
								"mining",
								"taming",
								"enchanting" -> "_xp";
							default -> "";
						},
						gamemode,
						true
					)
				);
			}
			statement.executeUpdate();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void deleteFromLeaderboard(String uuid, Player.Gamemode gamemode) {
		try (
			Connection connection = getConnection();
			PreparedStatement statement = connection.prepareStatement("DELETE FROM " + gamemode.toCacheType() + " WHERE uuid = ?")
		) {
			statement.setString(1, uuid);
			statement.executeUpdate();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public List<DataObject> getLeaderboard(String lbType, Player.Gamemode mode) {
		try (
			Connection connection = getConnection();
			PreparedStatement statement = connection.prepareStatement(
				"SELECT username, " + lbType + " FROM " + mode.toCacheType() + " WHERE " + lbType + " >= 0 ORDER BY " + lbType + " DESC"
			)
		) {
			try (ResultSet response = statement.executeQuery()) {
				List<DataObject> out = new ArrayList<>();
				while (response.next()) {
					try {
						out.add(DataObject.empty().put("username", response.getString("username")).put("data", response.getString(lbType)));
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
				return out;
			}
		} catch (Exception ignored) {}
		return null;
	}

	public void updateLeaderboard() {
		try (
			Connection connection = getConnection();
			PreparedStatement statement = connection.prepareStatement(
				"SELECT uuid FROM all_lb WHERE last_updated < " + Instant.now().minus(5, ChronoUnit.DAYS).toEpochMilli() + " LIMIT 180"
			)
		) {
			try (ResultSet response = statement.executeQuery()) {
				int count = 0;
				long start = System.currentTimeMillis();
				while (response.next() && count < 90) {
					String uuid = response.getString("uuid");
					UsernameUuidStruct usernameUuidStruct = uuidToUsername(uuid);
					if (usernameUuidStruct.isNotValid()) {
						executor.submit(() -> {
							for (Player.Gamemode gamemode : leaderboardGamemodes) {
								cacheDatabase.deleteFromLeaderboard(uuid, gamemode);
							}
						});
					} else {
						asyncSkyblockProfilesFromUuid(
							usernameUuidStruct.uuid(),
							count < 45 ? "c0cc68fc-a82a-462f-96ef-a060c22465fa" : "4991bfe2-d7aa-446a-b310-c7a70690927c",
							false
						)
							.whenComplete((r, e) ->
								insertIntoLeaderboard(new Player(usernameUuidStruct.uuid(), usernameUuidStruct.username(), r, true), false)
							);
					}
					count++;
				}

				if (count > 0) {
					log.info("Updated " + count + " leaderboard players in " + (System.currentTimeMillis() - start) + "ms");
				}

				if (count <= 5 && guildCount != -1) {
					count = 0;
					String guildId = higherDepth(
						getJson("https://raw.githubusercontent.com/kr45732/skyblock-plus-data/main/guilds.json"),
						"[" + guildCount + "]",
						null
					);
					if (guildId == null) {
						guildCount = -1;
						log.info("All guilds added");
						return;
					}
					HypixelResponse guildResponse = getGuildFromId(guildId);
					if (!guildResponse.isNotValid()) {
						JsonArray members = guildResponse.get("members").getAsJsonArray();
						for (JsonElement member : members) {
							String uuid = higherDepth(member, "uuid").getAsString();
							UsernameUuidStruct usernameUuidStruct = uuidToUsername(uuid);
							if (usernameUuidStruct.isNotValid()) {
								executor.submit(() -> {
									for (Player.Gamemode gamemode : leaderboardGamemodes) {
										cacheDatabase.deleteFromLeaderboard(uuid, gamemode);
									}
								});
							} else {
								asyncSkyblockProfilesFromUuid(
									usernameUuidStruct.uuid(),
									count < (members.size() / 2)
										? "c0cc68fc-a82a-462f-96ef-a060c22465fa"
										: "4991bfe2-d7aa-446a-b310-c7a70690927c",
									false
								)
									.whenComplete((r, e) ->
										insertIntoLeaderboard(
											new Player(usernameUuidStruct.uuid(), usernameUuidStruct.username(), r, true),
											false
										)
									);
							}
							count++;
						}
					}

					log.info("Finished guild count: " + guildCount);
					guildCount++;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
