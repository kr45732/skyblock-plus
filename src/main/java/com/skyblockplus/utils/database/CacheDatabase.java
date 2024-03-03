/*
 * Skyblock Plus - A Skyblock focused Discord bot with many commands and customizable features to improve the experience of Skyblock players and guild staff!
 * Copyright (c) 2021-2024 kr45732
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

package com.skyblockplus.utils.database;

import static com.skyblockplus.features.listeners.MainListener.guildMap;
import static com.skyblockplus.miscellaneous.CalendarSlashCommand.getSkyblockYear;
import static com.skyblockplus.utils.ApiHandler.cacheDatabase;
import static com.skyblockplus.utils.ApiHandler.leaderboardDatabase;
import static com.skyblockplus.utils.utils.JsonUtils.higherDepth;
import static com.skyblockplus.utils.utils.StringUtils.roundAndFormat;
import static com.skyblockplus.utils.utils.Utils.*;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import com.skyblockplus.features.jacob.JacobData;
import com.skyblockplus.features.jacob.JacobHandler;
import com.skyblockplus.features.listeners.AutomaticGuild;
import com.skyblockplus.features.party.Party;
import com.skyblockplus.price.AuctionTracker;
import com.skyblockplus.utils.Player;
import com.skyblockplus.utils.oauth.TokenData;
import com.skyblockplus.utils.structs.UsernameUuidStruct;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import groovy.lang.Tuple2;
import java.lang.reflect.Type;
import java.sql.*;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import net.dv8tion.jda.api.utils.data.DataObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CacheDatabase {

	private static final Logger log = LoggerFactory.getLogger(CacheDatabase.class);
	private final HikariDataSource dataSource;
	public final Map<String, List<Party>> partyCaches = new HashMap<>();
	private final Map<CacheId, Long> cacheIdToExpiry = new ConcurrentHashMap<>();

	public CacheDatabase() {
		HikariConfig config = new HikariConfig();
		config.setJdbcUrl(PLANET_SCALE_URL);
		config.setPoolName("Cache Database Pool");
		dataSource = new HikariDataSource(config);
	}

	public Connection getConnection() throws SQLException {
		return dataSource.getConnection();
	}

	public void cacheJson(CacheId id, JsonElement json) {
		long expiry = Instant.now().plus(90, ChronoUnit.SECONDS).toEpochMilli();
		executor.submit(() -> {
			try (
				Connection connection = getConnection();
				PreparedStatement statement = connection.prepareStatement(
					"INSERT INTO cache VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE id = VALUES(id)," +
					" expiry = VALUES(expiry), data = VALUES(data)"
				)
			) {
				statement.setString(1, id.getGeneratedId());
				statement.setLong(2, expiry);
				statement.setString(3, json.toString());
				statement.executeUpdate();

				cacheIdToExpiry.put(id, expiry);
			} catch (Exception ignored) {}
		});
	}

	public JsonElement getCachedJson(CacheType cacheType, String id) {
		Map.Entry<CacheId, Long> cacheEntry = cacheIdToExpiry
			.entrySet()
			.stream()
			.filter(e -> e.getKey().matches(cacheType, id))
			.findAny()
			.orElse(null);
		if (cacheEntry != null && cacheEntry.getValue() > Instant.now().toEpochMilli()) {
			try (
				Connection connection = getConnection();
				PreparedStatement statement = connection.prepareStatement("SELECT data FROM cache WHERE id = ? LIMIT 1")
			) {
				statement.setString(1, cacheEntry.getKey().getGeneratedId());
				try (ResultSet response = statement.executeQuery()) {
					if (response.next()) {
						return JsonParser.parseString(response.getString("data"));
					}
				}
			} catch (Exception ignored) {}
		}
		return null;
	}

	public void updateCache() {
		long now = Instant.now().toEpochMilli();
		List<String> expiredCacheIds = new ArrayList<>();

		try (
			Connection connection = getConnection();
			PreparedStatement statement = connection.prepareStatement("SELECT id FROM cache WHERE expiry <= ?")
		) {
			statement.setLong(1, now);
			try (ResultSet response = statement.executeQuery()) {
				while (response.next()) {
					expiredCacheIds.add(response.getString("id"));
				}
			}
		} catch (Exception ignored) {}

		if (!expiredCacheIds.isEmpty()) {
			cacheIdToExpiry.keySet().removeIf(cacheId -> expiredCacheIds.contains(cacheId.getGeneratedId()));

			try (Connection connection = getConnection(); Statement statement = connection.createStatement()) {
				statement.executeUpdate(
					"DELETE FROM cache WHERE id IN (" + expiredCacheIds.stream().collect(Collectors.joining("','", "'", "'")) + ")"
				);
			} catch (Exception ignored) {}
		}
	}

	public void cachePartyData() {
		if (!isMainBot()) {
			return;
		}

		log.info("Caching Parties");
		long startTime = System.currentTimeMillis();
		for (Map.Entry<String, AutomaticGuild> automaticGuild : guildMap.entrySet()) {
			try {
				List<Party> partyList = automaticGuild.getValue().partyList;
				if (!partyList.isEmpty()) {
					String partySettingsJson = gson.toJson(partyList);
					try (Connection connection = getConnection(); Statement statement = connection.createStatement()) {
						statement.executeUpdate(
							"INSERT INTO party VALUES ('" +
							automaticGuild.getValue().guildId +
							"', '" +
							partySettingsJson +
							"') ON DUPLICATE KEY UPDATE guild_id = VALUES(guild_id), data =" +
							" VALUES(data)"
						);
						log.info("Successfully cached PartyList | " + automaticGuild.getKey() + " | " + partyList.size());
					}
				}
			} catch (Exception e) {
				log.error(automaticGuild.getKey(), e);
			}
		}

		log.info("Cached parties in " + roundAndFormat((System.currentTimeMillis() - startTime) / 1000.0) + "s");
	}

	public void cacheCommandUsesData() {
		if (!isMainBot()) {
			return;
		}

		log.info("Caching Command Uses");
		long startTime = System.currentTimeMillis();
		String json = gson.toJson(getCommandUses());
		try (Connection connection = getConnection(); Statement statement = connection.createStatement()) {
			statement.executeUpdate("INSERT INTO commands VALUES (0, '" + json + "') ON DUPLICATE KEY UPDATE data = VALUES(data)");
			log.info("Cached command uses in " + roundAndFormat((System.currentTimeMillis() - startTime) / 1000.0) + "s");
		} catch (Exception e) {
			log.error("Failed to cache command uses in " + roundAndFormat((System.currentTimeMillis() - startTime) / 1000.0) + "s");
		}
	}

	public void cacheAhTrackerData() {
		if (!isMainBot()) {
			return;
		}

		log.info("Caching Auction Tracker");
		long startTime = System.currentTimeMillis();
		String json = gson.toJson(AuctionTracker.commandAuthorToTrackingUser);
		try (Connection connection = getConnection(); Statement statement = connection.createStatement()) {
			statement.executeUpdate("INSERT INTO ah_track VALUES (0, '" + json + "') ON DUPLICATE KEY UPDATE data = VALUES(data)");
			log.info(
				"Cached auction tracker in " +
				roundAndFormat((System.currentTimeMillis() - startTime) / 1000.0) +
				"s | " +
				AuctionTracker.commandAuthorToTrackingUser.size()
			);
		} catch (Exception e) {
			log.error("Failed to cache auction tracker in " + roundAndFormat((System.currentTimeMillis() - startTime) / 1000.0) + "s");
		}
	}

	public void cacheJacobData() {
		if (!isMainBot()) {
			return;
		}

		long startTime = System.currentTimeMillis();
		String json = gson.toJson(JacobHandler.getJacobData());
		try (Connection connection = getConnection(); Statement statement = connection.createStatement()) {
			statement.executeUpdate("INSERT INTO jacob VALUES (0, '" + json + "') ON DUPLICATE KEY UPDATE data = VALUES(data)");
			log.info("Cached jacob data in " + roundAndFormat((System.currentTimeMillis() - startTime) / 1000.0) + "s");
		} catch (Exception e) {
			log.error("Failed to cache jacob data in " + roundAndFormat((System.currentTimeMillis() - startTime) / 1000.0) + "s");
		}
	}

	public void cacheTokensData() {
		if (!isMainBot()) {
			return;
		}

		long startTime = System.currentTimeMillis();
		String json = gson.toJson(oAuthClient.getTokensMap());
		try (Connection connection = getConnection(); Statement statement = connection.createStatement()) {
			statement.executeUpdate("INSERT INTO tokens VALUES (0, '" + json + "') ON DUPLICATE KEY UPDATE data = VALUES(data)");
			log.info("Cached token data in " + roundAndFormat((System.currentTimeMillis() - startTime) / 1000.0) + "s");
		} catch (Exception e) {
			log.error("Failed to cache token data in " + roundAndFormat((System.currentTimeMillis() - startTime) / 1000.0) + "s");
		}
	}

	public void initializeAhTracker() {
		if (!isMainBot()) {
			return;
		}

		try (
			Connection connection = getConnection();
			PreparedStatement statement = connection.prepareStatement("SELECT data FROM ah_track LIMIT 1")
		) {
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

		try (
			Connection connection = getConnection();
			PreparedStatement statement = connection.prepareStatement("SELECT data FROM commands LIMIT 1")
		) {
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
		try (
			Connection connection = getConnection();
			PreparedStatement statement = connection.prepareStatement("SELECT data FROM jacob LIMIT 1")
		) {
			try (ResultSet response = statement.executeQuery()) {
				response.next();
				JacobData jacobData = gson.fromJson(response.getString("data"), JacobData.class);
				if (jacobData.getYear() == getSkyblockYear()) {
					JacobHandler.setJacobData(jacobData);
					log.info("Retrieved jacob data");
				}
			}
		} catch (Exception e) {
			log.error("", e);
		}

		if (JacobHandler.getJacobData() == null) {
			try {
				JacobHandler.setJacobDataFromApi();
				log.info("Fetched jacob data");
			} catch (Exception e) {
				log.error("", e);
			}
		}
	}

	public void initializeTokens() {
		if (!isMainBot()) {
			return;
		}

		try (
			Connection connection = getConnection();
			PreparedStatement statement = connection.prepareStatement("SELECT data FROM tokens LIMIT 1")
		) {
			try (ResultSet response = statement.executeQuery()) {
				response.next();
				oAuthClient
					.getTokensMap()
					.putAll(gson.fromJson(response.getString("data"), new TypeToken<Map<String, TokenData>>() {}.getType()));
				log.info("Retrieved tokens data");
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
		try (
			Connection connection = getConnection();
			PreparedStatement statement = connection.prepareStatement("SELECT guild_id, data FROM party")
		) {
			try (ResultSet response = statement.executeQuery()) {
				Type partyListType = new TypeToken<List<Party>>() {}.getType();
				while (response.next()) {
					String guildId = null;
					try {
						guildId = response.getString("guild_id");
						toDeleteIds.add(guildId);
						List<Party> partyList = gson.fromJson(response.getString("data"), partyListType);
						partyCaches.put(guildId, partyList);
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

	public static class CacheId {

		private final CacheType cacheType;
		private final List<String> ids = new ArrayList<>();

		@lombok.Getter
		private final String generatedId;

		public CacheId(CacheType cacheType, String... ids) {
			this.cacheType = cacheType;

			for (String id : ids) {
				this.ids.add(id.toLowerCase());
			}

			String generatedId;
			while (true) {
				String finalGeneratedId = generatedId = UUID.randomUUID().toString();
				if (cacheDatabase.cacheIdToExpiry.keySet().stream().noneMatch(c -> c.generatedId.equals(finalGeneratedId))) {
					break;
				}
			}
			this.generatedId = generatedId;
		}

		public CacheId addIds(List<String> ids) {
			this.ids.addAll(ids);
			return this;
		}

		public boolean matches(CacheType cacheType, String id) {
			return this.cacheType == cacheType && this.ids.contains(id.toLowerCase());
		}
	}

	/**
	 * @return timestamp of when guild was last cached or null if not present
	 */
	public Instant getGuildCacheRequestTime(String guildId) {
		try (
			Connection connection = getConnection();
			PreparedStatement statement = connection.prepareStatement("SELECT request_time FROM guild WHERE guild_id = ? LIMIT 1")
		) {
			statement.setObject(1, guildId);
			try (ResultSet response = statement.executeQuery()) {
				if (response.next()) {
					return Instant.ofEpochMilli(response.getLong("request_time"));
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return null;
	}

	/**
	 * @return timestamp of last guild update made by user or null if not present
	 */
	public Instant getGuildCacheLastRequest(String discordId) {
		try (
			Connection connection = getConnection();
			PreparedStatement statement = connection.prepareStatement(
				"SELECT request_time FROM guild WHERE request_discord = ? ORDER BY request_time DESC LIMIT 1"
			)
		) {
			statement.setObject(1, discordId);
			try (ResultSet response = statement.executeQuery()) {
				if (response.next()) {
					return Instant.ofEpochMilli(response.getLong("request_time"));
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return null;
	}

	public void cacheGuild(String guildId, String guildName, JsonArray members, String discordId) {
		try (
			Connection connection = getConnection();
			PreparedStatement statement = connection.prepareStatement(
				"INSERT INTO guild (guild_id, guild_name, request_time, members, request_discord)" +
				" VALUES (?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE guild_name =" +
				" VALUES(guild_name), request_time = VALUES(request_time), members =" +
				" VALUES(members), request_discord = VALUES(request_discord)"
			)
		) {
			statement.setString(1, guildId);
			statement.setString(2, guildName);
			statement.setLong(3, Instant.now().toEpochMilli());
			statement.setString(4, members.toString());
			statement.setString(5, discordId);
			statement.executeUpdate();
		} catch (Exception ignored) {}
	}

	public List<DataObject> fetchGuild(
		String guildId,
		String guildName,
		List<String> members,
		String discordId,
		List<String> lbTypes,
		Player.Gamemode gamemode
	) {
		if (!hypixelGuildFetchQueue.contains(guildId)) {
			hypixelGuildFetchQueue.add(guildId);
		}

		List<DataObject> out = null;
		try {
			out = guildRequestExecutor.submit(() -> leaderboardDatabase.fetchPlayers(lbTypes, gamemode, members)).get();
		} catch (Exception ignored) {}

		hypixelGuildFetchQueue.remove(guildId);

		if (out != null) {
			cacheGuild(guildId, guildName, gson.toJsonTree(members).getAsJsonArray(), discordId);
		}

		return out;
	}

	public Map<Tuple2<String, String>, List<String>> getGuildCaches() {
		Map<Tuple2<String, String>, List<String>> out = new HashMap<>();

		try (
			Connection connection = getConnection();
			PreparedStatement statement = connection.prepareStatement("SELECT guild_id, guild_name, members FROM guild")
		) {
			try (ResultSet response = statement.executeQuery()) {
				while (response.next()) {
					out.put(
						new Tuple2<>(response.getString("guild_id"), response.getString("guild_name")),
						gson.fromJson(response.getString("members"), new TypeToken<List<String>>() {}.getType())
					);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return out;
	}

	public enum CacheType {
		SKYBLOCK_PROFILES,
		GUILD,
		PLAYER,
		SKYBLOCK_MUSEUM,
		SKYBLOCK_BINGO,
	}
}
