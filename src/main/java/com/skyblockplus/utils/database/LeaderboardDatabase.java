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
import static com.skyblockplus.utils.ApiHandler.*;
import static com.skyblockplus.utils.Constants.collectionNameToId;
import static com.skyblockplus.utils.Constants.skyblockStats;
import static com.skyblockplus.utils.utils.HypixelUtils.levelingInfoFromExp;
import static com.skyblockplus.utils.utils.JsonUtils.getLevelingJson;
import static com.skyblockplus.utils.utils.JsonUtils.higherDepth;
import static com.skyblockplus.utils.utils.StringUtils.*;
import static com.skyblockplus.utils.utils.Utils.*;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import com.skyblockplus.api.linkedaccounts.LinkedAccount;
import com.skyblockplus.features.jacob.JacobData;
import com.skyblockplus.features.jacob.JacobHandler;
import com.skyblockplus.features.listeners.AutomaticGuild;
import com.skyblockplus.features.party.Party;
import com.skyblockplus.price.AuctionTracker;
import com.skyblockplus.utils.Player;
import com.skyblockplus.utils.command.SlashCommandEvent;
import com.skyblockplus.utils.oauth.TokenData;
import com.skyblockplus.utils.structs.HypixelResponse;
import com.skyblockplus.utils.structs.UsernameUuidStruct;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import groovy.lang.Tuple2;
import java.sql.*;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import lombok.Getter;
import net.dv8tion.jda.api.utils.data.DataObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LeaderboardDatabase {

	public static final Map<String, String> typeToNameSubMap = new HashMap<>();
	private static final List<String> ignoredTypes = List.of("selected_class", "gamemode", "emblem", "farming_cap");
	private static final List<String> types = new ArrayList<>(
		List.of(
			"username",
			"uuid",
			"catacombs_xp",
			"networth",
			"museum",
			"museum_hypixel",
			"level",
			"slayer",
			"skills",
			"skills_xp",
			"weight",
			"wolf",
			"zombie",
			"spider",
			"enderman",
			"blaze",
			"vampire",
			"alchemy_xp",
			"combat_xp",
			"fishing_xp",
			"farming_xp",
			"foraging_xp",
			"carpentry_xp",
			"mining_xp",
			"taming_xp",
			"social_xp",
			"enchanting_xp",
			"runecrafting_xp",
			"lily_weight",
			"coins",
			"hotm",
			"pet_score",
			"fairy_souls",
			"minion_slots",
			"maxed_slayers",
			"maxed_collections",
			"healer_xp",
			"mage_xp",
			"berserk_xp",
			"archer_xp",
			"tank_xp",
			"class_average",
			"mage_reputation",
			"barbarian_reputation",
			"lily_slayer_weight",
			"selected_class",
			"cole_weight",
			"bestiary",
			"gamemode",
			"emblem",
			"farming_cap",
			"dungeon_secrets"
		)
	);
	private static final List<String> typesSubList = new ArrayList<>();
	private static final Logger log = LoggerFactory.getLogger(LeaderboardDatabase.class);

	static {
		types.addAll(collectionNameToId.keySet());
		types.addAll(skyblockStats);

		typesSubList.addAll(types.subList(2, types.size()));
		for (String type : typesSubList) {
			if (!ignoredTypes.contains(type)) {
				typeToNameSubMap.put(type, capitalizeString(type.replace("_", " ")));
			}
		}
	}

	private final HikariDataSource dataSource;
	private final List<Player.Gamemode> leaderboardGamemodes = List.of(
		Player.Gamemode.ALL,
		Player.Gamemode.IRONMAN,
		Player.Gamemode.STRANDED,
		Player.Gamemode.SELECTED
	);
	private ScheduledFuture<?> leaderboardUpdateTask;
	private int numLeaderboardUpdates = 0;
	private final Cache<String, Boolean> failedUpdates = Caffeine.newBuilder().expireAfterWrite(12, TimeUnit.HOURS).build();
	private final Map<CacheId, Long> cacheIdToExpiry = new ConcurrentHashMap<>();
	public final Map<String, List<Party>> parties = new HashMap<>();

	public LeaderboardDatabase() {
		HikariConfig config = new HikariConfig();
		config.setJdbcUrl(LEADERBOARD_DB_URL);
		config.setMaximumPoolSize(20);
		config.setPoolName("Leaderbord Database Pool");
		dataSource = new HikariDataSource(config);

		if (!IS_DEV) {
			leaderboardUpdateTask = scheduler.scheduleAtFixedRate(this::updateLeaderboard, 1, 1, TimeUnit.MINUTES);
		}
	}

	public static String getType(String lbType) {
		return lbType.equalsIgnoreCase("nw") ? "networth" : getClosestMatch(lbType, typeToNameSubMap);
	}

	private Connection getConnection() throws SQLException {
		return dataSource.getConnection();
	}

	public void insertIfNotExist(String uuid) {
		if (failedUpdates.getIfPresent(uuid) != null) {
			return;
		}

		try (
			Connection connection = getConnection();
			PreparedStatement statement = connection.prepareStatement("SELECT uuid FROM stranded_lb WHERE uuid = ? LIMIT 1")
		) {
			statement.setObject(1, stringToUuid(uuid));
			try (ResultSet response = statement.executeQuery()) {
				if (!response.next()) {
					playerRequestExecutor.submit(() -> {
						if (!Player.create(uuid).isValid()) {
							failedUpdates.put(uuid, true);
						}
					});
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void insertIntoLeaderboard(Player.Profile player) {
		if (player.isValid()) {
			List<Player.Profile> players = new ArrayList<>();
			players.add(player);
			insertIntoLeaderboard(players);
		}
	}

	public void insertIntoLeaderboard(List<Player.Profile> players) {
		players.removeIf(p -> !p.isValid());
		if (!players.isEmpty()) {
			for (Player.Gamemode gamemode : leaderboardGamemodes) {
				leaderboardDbInsertQueue.submit(() -> insertIntoLeaderboard(players, gamemode));
			}
		}
	}

	private void insertIntoLeaderboard(List<Player.Profile> players, Player.Gamemode gamemode) {
		try {
			String paramStr = "?,".repeat(types.size() + 1); // Add 1 for last_updated
			paramStr = paramStr.substring(0, paramStr.length() - 1);

			boolean updateNetworth = players.size() == 1 || players.stream().noneMatch(p -> p.getProfileToNetworth().isEmpty());

			try (
				Connection connection = getConnection();
				PreparedStatement statement = connection.prepareStatement(
					"INSERT INTO " +
					gamemode.toLeaderboardName() +
					"(uuid,username,last_updated," +
					String.join(",", typesSubList) +
					") SELECT * FROM unnest(" +
					paramStr +
					") ON CONFLICT (uuid) DO UPDATE SET " +
					typesSubList
						.stream()
						.map(t ->
							t.equals("networth") && !updateNetworth
								? ("networth=" + gamemode.toLeaderboardName() + ".networth")
								: (t + "=EXCLUDED." + t)
						)
						.collect(Collectors.joining(",", "username=EXCLUDED.username,last_updated=EXCLUDED.last_updated,", ""))
				)
			) {
				Object[][] params = new Object[types.size() + 1][players.size()];
				for (int playerIndex = 0; playerIndex < players.size(); playerIndex++) {
					Player.Profile player = players.get(playerIndex);

					params[0][playerIndex] = stringToUuid(player.getUuid());
					params[1][playerIndex] = player.getUsername();
					params[2][playerIndex] = Instant.now().toEpochMilli();

					for (int paramIndex = 0; paramIndex < typesSubList.size(); paramIndex++) {
						String type = typesSubList.get(paramIndex);
						double value = type.equals("networth") && !updateNetworth ? 0 : player.getHighestAmount(type, gamemode, true);

						params[paramIndex + 3][playerIndex] = value < 0 ? null : value;
					}

					if (gamemode == Player.Gamemode.ALL) {
						LinkedAccount linkedAccount = database.getByUuid(player.getUuid());
						if (linkedAccount != null) {
							TokenData.updateLinkedRolesMetadata(linkedAccount.discord(), linkedAccount, player, true);
						}
					}
				}

				for (int i = 0; i < params.length; i++) {
					statement.setArray(
						i + 1,
						connection.createArrayOf(
							switch (i) {
								case 0 -> "uuid";
								case 1 -> "text";
								case 2 -> "bigint";
								default -> {
									String type = typesSubList.get(i - 3);
									yield type.equals("highest_critical_damage") || type.equals("highest_damage")
										? "double precision"
										: "real";
								}
							},
							params[i]
						)
					);
				}

				statement.executeUpdate();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/** Sync insert into requestedGamemode and async insert for other gamemodes */
	public void insertIntoLeaderboardSync(Player.Profile player, Player.Gamemode requestedGamemode) {
		if (!player.isValid()) {
			return;
		}

		List<Player.Profile> players = List.of(player);
		insertIntoLeaderboard(players, requestedGamemode);
		leaderboardDbInsertQueue.submit(() -> {
			for (Player.Gamemode gamemode : leaderboardGamemodes) {
				if (gamemode != requestedGamemode) {
					insertIntoLeaderboard(players, gamemode);
				}
			}
		});
	}

	public void deleteFromLeaderboard(String uuid, Player.Gamemode gamemode) {
		try (
			Connection connection = getConnection();
			PreparedStatement statement = connection.prepareStatement("DELETE FROM " + gamemode.toLeaderboardName() + " WHERE uuid = ?")
		) {
			statement.setObject(1, stringToUuid(uuid));
			statement.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	private double getDouble(ResultSet row, String lbType) throws SQLException {
		return switch (lbType) {
			case "alchemy",
				"combat",
				"fishing",
				"farming",
				"foraging",
				"carpentry",
				"mining",
				"taming",
				"enchanting",
				"social",
				"runecrafting",
				"healer",
				"mage",
				"berserk",
				"archer",
				"tank",
				"catacombs" -> levelingInfoFromExp(
				(long) row.getDouble(lbType + "_xp"),
				lbType,
				higherDepth(getLevelingJson(), "leveling_caps." + lbType, 50) +
				(lbType.equals("farming") ? (int) row.getDouble("farming_cap") : 0)
			)
				.getProgressLevel();
			case "selected_class", "emblem" -> {
				double value = row.getDouble(lbType);
				yield row.wasNull() ? -1 : value;
			}
			default -> row.getDouble(lbType);
		};
	}

	/**
	 * @param rankStart Exclusive
	 * @param rankEnd Inclusive
	 */
	public Map<Integer, DataObject> getLeaderboard(String lbType, Player.Gamemode mode, int rankStart, int rankEnd) {
		rankStart = Math.max(0, rankStart);
		rankEnd = Math.max(rankStart, rankEnd);
		try (
			Connection connection = getConnection();
			PreparedStatement statement = connection.prepareStatement(
				"SELECT username, " +
				lbType +
				", ROW_NUMBER() OVER(ORDER BY " +
				lbType +
				" DESC) AS rank FROM " +
				mode.toLeaderboardName() +
				" WHERE " +
				lbType +
				" IS NOT NULL OFFSET " +
				rankStart +
				" LIMIT " +
				(rankEnd - rankStart)
			)
		) {
			Map<Integer, DataObject> out = new TreeMap<>();
			try (ResultSet response = statement.executeQuery()) {
				while (response.next()) {
					out.put(
						response.getInt("rank"),
						DataObject.empty().put("username", response.getString("username")).put(lbType, getDouble(response, lbType))
					);
				}
			}
			return out;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public Map<Integer, DataObject> getLeaderboard(String lbType, Player.Gamemode mode, String uuid) {
		try (
			Connection connection = getConnection();
			PreparedStatement statement = connection.prepareStatement(
				"SELECT rank FROM (SELECT uuid, ROW_NUMBER() OVER(ORDER BY " +
				lbType +
				" DESC) AS rank FROM " +
				mode.toLeaderboardName() +
				" WHERE " +
				lbType +
				" IS NOT NULL) s WHERE uuid = ? LIMIT 1"
			)
		) {
			int rank = 0;
			statement.setObject(1, stringToUuid(uuid));
			try (ResultSet response = statement.executeQuery()) {
				if (response.next()) {
					rank = response.getInt("rank");
				}
			}

			return getLeaderboard(lbType, mode, rank - 200, rank + 200);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public Map<Integer, DataObject> getLeaderboard(String lbType, Player.Gamemode mode, double amount) {
		try (
			Connection connection = getConnection();
			PreparedStatement statement = connection.prepareStatement(
				"WITH s AS (SELECT username, " +
				lbType +
				", ROW_NUMBER() OVER(ORDER BY " +
				lbType +
				" DESC) AS rank FROM " +
				mode.toLeaderboardName() +
				" WHERE " +
				lbType +
				" IS NOT NULL) (SELECT * FROM s WHERE " +
				lbType +
				" > " +
				amount +
				" ORDER BY rank DESC LIMIT 200) UNION ALL (SELECT * FROM s WHERE " +
				lbType +
				" <= " +
				amount +
				" LIMIT 200)"
			)
		) {
			Map<Integer, DataObject> out = new TreeMap<>();
			try (ResultSet response = statement.executeQuery()) {
				while (response.next()) {
					out.put(
						response.getInt("rank"),
						DataObject.empty().put("username", response.getString("username")).put(lbType, getDouble(response, lbType))
					);
				}
				return out;
			}
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	public List<DataObject> getPlayers(List<String> lbTypes, Player.Gamemode mode, List<String> uuids, SlashCommandEvent event) {
		if (uuids.isEmpty()) {
			return new ArrayList<>();
		}

		List<DataObject> out = getCachedPlayers(lbTypes, mode, uuids);

		if (out.size() != uuids.size()) {
			List<String> loadedUuids = out.stream().map(e -> e.getString("uuid")).toList();
			List<String> remainingUuids = uuids.stream().filter(e -> !loadedUuids.contains(e)).toList();

			event.embed(
				defaultEmbed("Loading")
					.setDescription("Retrieving an additional " + remainingUuids.size() + " players. This may take some time.")
			);

			out.addAll(fetchPlayers(lbTypes, mode, remainingUuids));
		}

		return out;
	}

	public List<DataObject> getCachedPlayers(List<String> lbTypes, Player.Gamemode mode, List<String> uuids) {
		if (uuids.isEmpty()) {
			return new ArrayList<>();
		}

		List<DataObject> out = new ArrayList<>();
		String paramsStr = "?,".repeat(uuids.size());
		paramsStr = paramsStr.endsWith(",") ? paramsStr.substring(0, paramsStr.length() - 1) : paramsStr;

		try (
			Connection connection = getConnection();
			PreparedStatement statement = connection.prepareStatement(
				"SELECT username, uuid, last_updated, " +
				lbTypes
					.stream()
					.map(e ->
						switch (e) {
							case "alchemy",
								"combat",
								"fishing",
								"foraging",
								"carpentry",
								"mining",
								"taming",
								"enchanting",
								"social",
								"runecrafting",
								"healer",
								"mage",
								"berserk",
								"archer",
								"tank",
								"catacombs" -> e + "_xp";
							case "farming" -> "farming_xp, farming_cap";
							default -> e;
						}
					)
					.collect(Collectors.joining(", ")) +
				" FROM " +
				mode.toLeaderboardName() +
				" WHERE uuid IN (" +
				paramsStr +
				")"
			)
		) {
			for (int i = 0; i < uuids.size(); i++) {
				statement.setObject(i + 1, stringToUuid(uuids.get(i)));
			}

			try (ResultSet response = statement.executeQuery()) {
				while (response.next()) {
					String uuid = response.getString("uuid").replace("-", "");

					DataObject playerObj = DataObject
						.empty()
						.put("username", response.getString("username"))
						.put("uuid", uuid)
						.put("last_updated", response.getLong("last_updated"));
					for (String lbType : lbTypes) {
						playerObj.put(lbType, getDouble(response, lbType));
					}

					out.add(playerObj);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return out;
	}

	public DataObject getCachedPlayer(List<String> lbTypes, Player.Gamemode mode, String uuid) {
		List<DataObject> cachedPlayers = getCachedPlayers(lbTypes, mode, List.of(uuid));
		return cachedPlayers.isEmpty() ? null : cachedPlayers.get(0);
	}

	public List<DataObject> fetchPlayers(List<String> lbTypes, Player.Gamemode mode, List<String> uuids) {
		if (uuids.isEmpty()) {
			return new ArrayList<>();
		}

		List<DataObject> out = new ArrayList<>();
		List<Player.Profile> players = new ArrayList<>();
		List<CompletableFuture<DataObject>> futuresList = new ArrayList<>();
		for (String uuid : uuids) {
			futuresList.add(
				CompletableFuture.supplyAsync(
					() -> {
						Player.Profile player = new Player(uuid, false).getSelectedProfile();
						if (player.isValid()) {
							players.add(player);

							DataObject playerObj = DataObject
								.empty()
								.put("username", player.getUsername())
								.put("uuid", uuid)
								.put("last_updated", Instant.now().toEpochMilli());
							for (String lbType : lbTypes) {
								playerObj.put(lbType, player.getHighestAmount(lbType, mode));
							}
							return playerObj;
						}
						return null;
					},
					playerRequestExecutor
				)
			);
		}

		for (CompletableFuture<DataObject> future : futuresList) {
			try {
				DataObject getFuture = future.get();
				if (getFuture != null) {
					out.add(getFuture);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		insertIntoLeaderboard(players);

		return out;
	}

	public Future<Integer> getNetworthPosition(Player.Gamemode gamemode, String uuid) {
		return executor.submit(() -> {
			try (
				Connection connection = getConnection();
				PreparedStatement statement = connection.prepareStatement(
					"SELECT rank FROM (SELECT uuid, ROW_NUMBER() OVER(ORDER BY networth DESC) AS" +
					" rank FROM " +
					gamemode.toLeaderboardName() +
					" WHERE networth IS NOT NULL) s WHERE uuid = ? LIMIT 1"
				)
			) {
				statement.setObject(1, stringToUuid(uuid));
				try (ResultSet response = statement.executeQuery()) {
					if (response.next()) {
						return response.getInt("rank");
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			return -1;
		});
	}

	public UsernameUuidStruct usernameToUuid(String username) {
		try (
			Connection connection = getConnection();
			PreparedStatement statement = connection.prepareStatement(
				"SELECT username, uuid FROM stranded_lb WHERE username ILIKE ? LIMIT 1"
			)
		) {
			statement.setString(1, username);
			try (ResultSet response = statement.executeQuery()) {
				if (response.next()) {
					return new UsernameUuidStruct(response.getString("username"), response.getString("uuid"));
				}
			}
		} catch (Exception ignored) {}
		return null;
	}

	public List<String> getClosestPlayers(String toMatch) {
		try (
			Connection connection = getConnection();
			PreparedStatement statement = connection.prepareStatement("SELECT username FROM stranded_lb ORDER BY username <-> ? LIMIT 25")
		) {
			statement.setString(1, toMatch);
			try (ResultSet response = statement.executeQuery()) {
				List<String> usernames = new ArrayList<>();
				while (response.next()) {
					usernames.add(response.getString("username"));
				}
				return usernames;
			}
		} catch (Exception ignored) {}
		return null;
	}

	private void updateLeaderboard() {
		try {
			long start = System.currentTimeMillis();

			List<String> out = new ArrayList<>();
			try (
				Connection connection = getConnection();
				PreparedStatement statement = connection.prepareStatement(
					"SELECT uuid FROM stranded_lb WHERE last_updated < " +
					Instant.now().minus(3, ChronoUnit.DAYS).toEpochMilli() +
					" ORDER BY RANDOM() LIMIT 180"
				)
			) {
				try (ResultSet response = statement.executeQuery()) {
					while (response.next()) {
						out.add(response.getString("uuid").replace("-", ""));
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
				return;
			}

			int count = 0;
			List<Player.Profile> players = new ArrayList<>();
			for (String uuid : out) {
				if (count >= 90 || System.currentTimeMillis() - start >= 55000) {
					break;
				}

				if (failedUpdates.getIfPresent(uuid) != null) {
					continue;
				}

				UsernameUuidStruct usernameUuidStruct = uuidToUsername(uuid);
				if (usernameUuidStruct.isValid()) {
					HypixelResponse profileResponse = skyblockProfilesFromUuid(usernameUuidStruct.uuid(), true, false);
					count++;

					if (profileResponse.isValid()) {
						Player player = new Player(
							usernameUuidStruct.username(),
							usernameUuidStruct.uuid(),
							profileResponse.response(),
							false
						);

						if (player.isValid()) {
							Player.Profile profile = player.getSelectedProfile();
							profile.getHighestAmount("networth");
							players.add(profile);
							continue;
						}
					}
				}

				failedUpdates.put(uuid, true);
			}

			insertIntoLeaderboard(players);
			numLeaderboardUpdates++;

			if (numLeaderboardUpdates % 10 == 0) {
				System.out.println("Update Leaderboard | Time (" + (System.currentTimeMillis() - start) + "ms) | Users (" + count + ")");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/* JSON storage */
	public void cacheCommandUses() {
		if (IS_DEV) {
			return;
		}

		insertIntoJsonStorage(JsonStorageId.COMMAND_USES, getCommandUses());
	}

	public void cacheAuctionTracker() {
		if (IS_DEV) {
			return;
		}

		insertIntoJsonStorage(JsonStorageId.AUCTION_TRACKER, AuctionTracker.commandAuthorToTrackingUser);
	}

	public void cacheJacob() {
		if (IS_DEV) {
			return;
		}

		insertIntoJsonStorage(JsonStorageId.JACOB, JacobHandler.getJacobData());
	}

	public void cacheTokens() {
		if (IS_DEV) {
			return;
		}

		insertIntoJsonStorage(JsonStorageId.TOKENS, oAuthClient.getTokensMap());
	}

	public void cacheParties() {
		if (IS_DEV) {
			return;
		}

		Map<String, List<Party>> partyMap = new HashMap<>();
		for (Map.Entry<String, AutomaticGuild> automaticGuild : guildMap.entrySet()) {
			if (!automaticGuild.getValue().partyList.isEmpty()) {
				partyMap.put(automaticGuild.getValue().guildId, automaticGuild.getValue().partyList);
			}
		}

		insertIntoJsonStorage(JsonStorageId.PARTIES, partyMap);
	}

	private void insertIntoJsonStorage(JsonStorageId id, Object json) {
		try (
			Connection connection = getConnection();
			PreparedStatement statement = connection.prepareStatement(
				"INSERT INTO json_storage (id, data) VALUES (?, ?) ON CONFLICT (id) DO UPDATE SET data = EXCLUDED.data"
			)
		) {
			statement.setInt(1, id.id);
			statement.setObject(2, gson.toJsonTree(json), Types.OTHER);

			statement.executeUpdate();

			log.info("Cached " + id.name());
		} catch (Exception e) {
			log.error("Failed to cache " + id.name(), e);
		}
	}

	public void initializeCommandUses() {
		if (IS_DEV) {
			return;
		}

		try {
			slashCommandClient.setCommandUses(gson.fromJson(selectFromJsonStorage(JsonStorageId.COMMAND_USES), new TypeToken<>() {}));
		} catch (Exception e) {
			log.error("", e);
		}
	}

	public void initializeAuctionTracker() {
		if (IS_DEV) {
			return;
		}

		try {
			AuctionTracker.commandAuthorToTrackingUser.putAll(
				gson.fromJson(selectFromJsonStorage(JsonStorageId.AUCTION_TRACKER), new TypeToken<Map<String, UsernameUuidStruct>>() {})
			);
		} catch (Exception e) {
			log.error("", e);
		}
	}

	public void initializeJacob() {
		try {
			JacobData jacobData = gson.fromJson(selectFromJsonStorage(JsonStorageId.JACOB), JacobData.class);
			if (jacobData.getYear() == getSkyblockYear()) {
				JacobHandler.setJacobData(jacobData);
			}
		} catch (Exception e) {
			log.error("", e);
		}

		if (JacobHandler.getJacobData() == null) {
			try {
				JacobHandler.setJacobDataFromApi();
				log.info("Fetched jacob data from API");
			} catch (Exception e) {
				log.error("", e);
			}
		}
	}

	public void initializeTokens() {
		if (IS_DEV) {
			return;
		}

		try {
			oAuthClient
				.getTokensMap()
				.putAll(gson.fromJson(selectFromJsonStorage(JsonStorageId.TOKENS), new TypeToken<Map<String, TokenData>>() {}));
		} catch (Exception e) {
			log.error("", e);
		}
	}

	public void initializeParties() {
		if (IS_DEV) {
			return;
		}

		try {
			parties.putAll(gson.fromJson(selectFromJsonStorage(JsonStorageId.PARTIES), new TypeToken<Map<String, List<Party>>>() {}));
		} catch (Exception e) {
			log.error("", e);
		}
	}

	private String selectFromJsonStorage(JsonStorageId id) throws SQLException {
		try (
			Connection connection = getConnection();
			PreparedStatement statement = connection.prepareStatement("SELECT data FROM json_storage WHERE id = ?")
		) {
			statement.setInt(1, id.id);

			try (ResultSet response = statement.executeQuery()) {
				response.next();
				String json = response.getString("data");
				log.info("Retrieved " + id.name());
				return json;
			}
		}
	}

	private enum JsonStorageId {
		COMMAND_USES(0),
		AUCTION_TRACKER(1),
		JACOB(2),
		TOKENS(3),
		PARTIES(4);

		private final int id;

		JsonStorageId(int id) {
			this.id = id;
		}
	}

	/* Guild cache */
	private void cacheGuild(String guildId, String guildName, JsonArray members, String discordId) {
		try (
			Connection connection = getConnection();
			PreparedStatement statement = connection.prepareStatement(
				"INSERT INTO guild (guild_id, guild_name, request_time, members, request_discord)" +
				" VALUES (?, ?, ?, ?, ?) ON CONFLICT (guild_id) DO UPDATE SET guild_name = EXCLUDED.guild_name, request_time = EXCLUDED.request_time, members = EXCLUDED.members, request_discord = EXCLUDED.request_discord"
			)
		) {
			statement.setString(1, guildId);
			statement.setString(2, guildName);
			statement.setLong(3, Instant.now().toEpochMilli());
			statement.setObject(4, members, Types.OTHER);
			statement.setString(5, discordId);

			statement.executeUpdate();
		} catch (Exception ignored) {}
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

	/* JSON cache */
	public void cacheJson(CacheId id, JsonElement json) {
		long expiry = Instant.now().plus(90, ChronoUnit.SECONDS).toEpochMilli();
		executor.submit(() -> {
			try (
				Connection connection = getConnection();
				PreparedStatement statement = connection.prepareStatement(
					"INSERT INTO json_cache (id, expiry, data) VALUES (?, ?, ?) ON CONFLICT (id) DO UPDATE SET expiry = EXCLUDED.expiry, data = EXCLUDED.data"
				)
			) {
				statement.setString(1, id.getGeneratedId());
				statement.setLong(2, expiry);
				statement.setObject(3, json, Types.OTHER);

				statement.executeUpdate();

				cacheIdToExpiry.put(id, expiry);
			} catch (Exception ignored) {}
		});
	}

	public JsonElement getCachedJson(CacheType cacheType, String id) {
		Optional<Map.Entry<CacheId, Long>> cacheEntry = cacheIdToExpiry
			.entrySet()
			.stream()
			.filter(e -> e.getKey().matches(cacheType, id))
			.findAny();
		if (cacheEntry.isPresent() && cacheEntry.get().getValue() > Instant.now().toEpochMilli()) {
			try (
				Connection connection = getConnection();
				PreparedStatement statement = connection.prepareStatement("SELECT data FROM json_cache WHERE id = ? LIMIT 1")
			) {
				statement.setString(1, cacheEntry.get().getKey().getGeneratedId());

				try (ResultSet response = statement.executeQuery()) {
					if (response.next()) {
						return JsonParser.parseString(response.getString("data"));
					}
				}
			} catch (Exception ignored) {}
		}
		return null;
	}

	public void updateJsonCache() {
		long now = Instant.now().toEpochMilli();
		List<String> expiredCacheIds = new ArrayList<>();

		try (
			Connection connection = getConnection();
			PreparedStatement statement = connection.prepareStatement("SELECT id FROM json_cache WHERE expiry <= ?")
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
					"DELETE FROM json_cache WHERE id IN (" + expiredCacheIds.stream().collect(Collectors.joining("','", "'", "'")) + ")"
				);
			} catch (Exception ignored) {}
		}
	}

	public static class CacheId {

		private final CacheType cacheType;
		private final List<String> ids = new ArrayList<>();

		@Getter
		private final String generatedId;

		public CacheId(CacheType cacheType, String... ids) {
			this.cacheType = cacheType;

			for (String id : ids) {
				this.ids.add(id.toLowerCase());
			}

			String generatedId;
			while (true) {
				String finalGeneratedId = generatedId = UUID.randomUUID().toString();
				if (leaderboardDatabase.cacheIdToExpiry.keySet().stream().noneMatch(c -> c.generatedId.equals(finalGeneratedId))) {
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

	public enum CacheType {
		SKYBLOCK_PROFILES,
		GUILD,
		PLAYER,
		SKYBLOCK_MUSEUM,
		SKYBLOCK_BINGO,
	}

	public void close() {
		log.info("Closing leaderboard database");
		if (leaderboardUpdateTask != null) {
			leaderboardUpdateTask.cancel(true);
		}
		dataSource.close();
		log.info("Successfully closed leaderboard database");
	}
}
