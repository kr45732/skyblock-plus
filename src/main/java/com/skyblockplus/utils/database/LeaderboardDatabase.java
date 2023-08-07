/*
 * Skyblock Plus - A Skyblock focused Discord bot with many commands and customizable features to improve the experience of Skyblock players and guild staff!
 * Copyright (c) 2021-2023 kr45732
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

import static com.skyblockplus.utils.ApiHandler.skyblockProfilesFromUuid;
import static com.skyblockplus.utils.ApiHandler.uuidToUsername;
import static com.skyblockplus.utils.Constants.collectionNameToId;
import static com.skyblockplus.utils.Constants.skyblockStats;
import static com.skyblockplus.utils.utils.HypixelUtils.levelingInfoFromExp;
import static com.skyblockplus.utils.utils.StringUtils.*;
import static com.skyblockplus.utils.utils.Utils.*;

import com.skyblockplus.api.linkedaccounts.LinkedAccount;
import com.skyblockplus.utils.Player;
import com.skyblockplus.utils.command.SlashCommandEvent;
import com.skyblockplus.utils.oauth.TokenData;
import com.skyblockplus.utils.structs.HypixelResponse;
import com.skyblockplus.utils.structs.UsernameUuidStruct;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.*;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import net.dv8tion.jda.api.utils.data.DataObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LeaderboardDatabase {

	public static final Map<String, String> typeToNameSubMap = new HashMap<>();
	private static final int MAX_INSERT_COUNT = 60;
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
			"mage_reputation",
			"barbarian_reputation",
			"lily_slayer_weight",
			"selected_class",
			"cole_weight",
			"bestiary"
		)
	);
	private static final List<String> typesSubList = new ArrayList<>();
	private static final Logger log = LoggerFactory.getLogger(LeaderboardDatabase.class);

	static {
		types.addAll(collectionNameToId.keySet());
		types.addAll(skyblockStats);

		typesSubList.addAll(types.subList(2, types.size()));
		for (String type : typesSubList) {
			if (!type.equals("selected_class")) {
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

	public LeaderboardDatabase() {
		HikariConfig config = new HikariConfig();
		config.setJdbcUrl(LEADERBOARD_DB_URL);
		config.setMaximumPoolSize(MAX_INSERT_COUNT);
		config.setPoolName("Leaderbord Database Pool");
		dataSource = new HikariDataSource(config);

		if (isMainBot()) {
			leaderboardUpdateTask = scheduler.scheduleAtFixedRate(this::updateLeaderboard, 1, 1, TimeUnit.MINUTES);
		}
	}

	public static String getType(String lbType) {
		return lbType.equalsIgnoreCase("nw") ? "networth" : getClosestMatch(lbType, typeToNameSubMap);
	}

	private Connection getConnection() throws SQLException {
		return dataSource.getConnection();
	}

	public void insertIntoLeaderboard(Player.Profile player) {
		if (player.isValid()) {
			List<Player.Profile> players = List.of(player);
			for (Player.Gamemode gamemode : leaderboardGamemodes) {
				leaderboardDbInsertQueue.submit(() -> insertIntoLeaderboard(players, 0, gamemode));
			}
		}
	}

	/**
	 * Once called, these players should not be used again to access stats (does not copy)
	 */
	public void insertIntoLeaderboard(List<Player.Profile> players) {
		players.removeIf(p -> !p.isValid());
		if (!players.isEmpty()) {
			for (int i = 0; i < players.size(); i += MAX_INSERT_COUNT) {
				int finalI = i;
				for (Player.Gamemode gamemode : leaderboardGamemodes) {
					leaderboardDbInsertQueue.submit(() -> insertIntoLeaderboard(players, finalI, gamemode));
				}
			}
		}
	}

	private void insertIntoLeaderboard(List<Player.Profile> players, int startingIndex, Player.Gamemode gamemode) {
		try {
			String paramStr = "?,".repeat(types.size() + 1); // Add 1 for last_updated
			paramStr = paramStr.substring(0, paramStr.length() - 1);
			String multiParamsStr =
				("(" + paramStr + "),").repeat(
						startingIndex + MAX_INSERT_COUNT <= players.size() ? MAX_INSERT_COUNT : (players.size() - startingIndex)
					);
			multiParamsStr = multiParamsStr.substring(0, multiParamsStr.length() - 1);

			boolean updateNetworth = players.size() == 1 || players.stream().noneMatch(p -> p.getProfileToNetworth().isEmpty());

			try (
				Connection connection = getConnection();
				PreparedStatement statement = connection.prepareStatement(
					"INSERT INTO " +
					gamemode.toLeaderboardName() +
					"(uuid,username,last_updated," +
					String.join(",", typesSubList) +
					") VALUES " +
					multiParamsStr +
					" ON CONFLICT (uuid) DO UPDATE SET " +
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
				for (int j = startingIndex; j < Math.min(startingIndex + MAX_INSERT_COUNT, players.size()); j++) {
					Player.Profile player = players.get(j);
					int offset = (j - startingIndex) * (3 + typesSubList.size());

					statement.setObject(1 + offset, stringToUuid(player.getUuid()));
					statement.setString(2 + offset, player.getUsername());
					statement.setLong(3 + offset, Instant.now().toEpochMilli());
					for (int i = 0; i < typesSubList.size(); i++) {
						String type = typesSubList.get(i);
						double value = type.equals("networth") && !updateNetworth ? 0 : player.getHighestAmount(type, gamemode);

						if (type.equals("highest_critical_damage") || type.equals("highest_damage")) {
							if (value < 0) {
								statement.setNull(i + 4 + offset, Types.DOUBLE);
							} else {
								statement.setDouble(i + 4 + offset, value);
							}
						} else {
							if (value < 0) {
								statement.setNull(i + 4 + offset, Types.FLOAT);
							} else {
								statement.setFloat(i + 4 + offset, (float) value);
							}
						}
					}

					if (gamemode == Player.Gamemode.ALL) {
						LinkedAccount linkedAccount = database.getByUuid(player.getUuid());
						if (linkedAccount != null) {
							TokenData.updateLinkedRolesMetadata(linkedAccount.discord(), linkedAccount, player, true);
						}
					}
				}
				statement.executeUpdate();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Sync insert into requestedGamemode and async insert for other gamemodes
	 */
	public void insertIntoLeaderboardSync(Player.Profile player, Player.Gamemode requestedGamemode) {
		if (!player.isValid()) {
			return;
		}

		List<Player.Profile> players = List.of(player);
		insertIntoLeaderboard(players, 0, requestedGamemode);
		leaderboardDbInsertQueue.submit(() -> {
			for (Player.Gamemode gamemode : leaderboardGamemodes) {
				if (gamemode != requestedGamemode) {
					insertIntoLeaderboard(players, 0, gamemode);
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
				"healer",
				"mage",
				"berserk",
				"archer",
				"tank",
				"catacombs" -> levelingInfoFromExp((long) row.getDouble(lbType + "_xp"), lbType).getProgressLevel();
			case "selected_class" -> {
				double value = row.getDouble(lbType);
				boolean isNull = row.wasNull();
				yield isNull ? -1 : value;
			}
			default -> row.getDouble(lbType);
		};
	}

	/**
	 * @param rankStart Exclusive
	 * @param rankEnd   Inclusive
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
				"SELECT username, uuid, " +
				lbTypes
					.stream()
					.map(e ->
						switch (e) {
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
								"healer",
								"mage",
								"berserk",
								"archer",
								"tank",
								"catacombs" -> e + "_xp";
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

					DataObject playerObj = DataObject.empty().put("username", response.getString("username")).put("uuid", uuid);
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

							DataObject playerObj = DataObject.empty().put("username", player.getUsername()).put("uuid", uuid);
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
					"SELECT rank FROM (SELECT uuid, ROW_NUMBER() OVER(ORDER BY networth DESC) AS rank FROM " +
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

				UsernameUuidStruct usernameUuidStruct = uuidToUsername(uuid);
				if (usernameUuidStruct.isValid()) {
					HypixelResponse profileResponse = skyblockProfilesFromUuid(usernameUuidStruct.uuid(), HYPIXEL_API_KEY, true, false);
					count++;

					if (profileResponse.isValid()) {
						Player.Profile player = new Player(
							usernameUuidStruct.username(),
							usernameUuidStruct.uuid(),
							profileResponse.response(),
							false
						)
							.getSelectedProfile();
						player.getHighestAmount("networth");
						players.add(player);
					}
				}
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

	public void close() {
		log.info("Closing leaderboard database");
		if (leaderboardUpdateTask != null) {
			leaderboardUpdateTask.cancel(true);
		}
		dataSource.close();
		log.info("Successfully closed leaderboard database");
	}
}
