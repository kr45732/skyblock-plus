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

package com.skyblockplus.utils.database;

import com.google.gson.JsonArray;
import com.skyblockplus.utils.Player;
import com.skyblockplus.utils.structs.HypixelResponse;
import com.skyblockplus.utils.structs.UsernameUuidStruct;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import net.dv8tion.jda.api.utils.data.DataObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.skyblockplus.utils.ApiHandler.skyblockProfilesFromUuid;
import static com.skyblockplus.utils.ApiHandler.uuidToUsername;
import static com.skyblockplus.utils.Player.COLLECTION_NAME_TO_ID;
import static com.skyblockplus.utils.Player.STATS_LIST;
import static com.skyblockplus.utils.Utils.*;

public class LeaderboardDatabase {

	public static final List<String> types = new ArrayList<>(
		List.of(
			"username",
			"uuid",
			"slayer",
			"skills",
			"catacombs",
			"weight",
			"sven",
			"rev",
			"tara",
			"enderman",
			"alchemy",
			"combat",
			"fishing",
			"farming",
			"foraging",
			"carpentry",
			"mining",
			"taming",
			"social",
			"enchanting",
			"networth",
			"blaze",
			"lily_weight",
			"coins"
		)
	);
	public static final List<String> typesSubList = new ArrayList<>();
	public static final List<String> formattedTypesSubList = new ArrayList<>();

	public static final List<String> guildTypes = new ArrayList<>();
	public static final List<String> guildTypesSubList = new ArrayList<>();
	public static final List<String> formattedGuildTypesSubList = new ArrayList<>();

	static {
		types.addAll(COLLECTION_NAME_TO_ID.keySet());
		guildTypes.addAll(types);
		guildTypes.add("lily_slayer_weight");
		types.addAll(STATS_LIST);

		typesSubList.addAll(types.subList(2, types.size()));
		formattedTypesSubList.addAll(typesSubList.stream().map(t -> capitalizeString(t.replace("_", " "))).toList());

		guildTypesSubList.addAll(guildTypes.subList(2, guildTypes.size()));
		formattedGuildTypesSubList.addAll(guildTypesSubList.stream().map(t -> capitalizeString(t.replace("_", " "))).toList());
	}

	private static final Logger log = LoggerFactory.getLogger(LeaderboardDatabase.class);

	private final HikariDataSource dataSource;
	private final List<Player.Gamemode> leaderboardGamemodes = Arrays.asList(
		Player.Gamemode.ALL,
		Player.Gamemode.IRONMAN,
		Player.Gamemode.STRANDED
	);
	public int userCount = -1;
	public ScheduledFuture<?> updateTask;

	public LeaderboardDatabase() {
		HikariConfig config = new HikariConfig();
		config.setJdbcUrl(LEADERBOARD_DB_URL);
		dataSource = new HikariDataSource(config);

		if (isMainBot()) {
			updateTask = scheduler.scheduleAtFixedRate(this::updateLeaderboard, 1, 1, TimeUnit.MINUTES);
		}
	}

	public Connection getConnection() throws SQLException {
		return dataSource.getConnection();
	}

	public void insertIntoLeaderboard(Player player) {
		insertIntoLeaderboard(player, true);
	}

	public void insertIntoLeaderboard(Player player, boolean makeCopy) {
		executor.submit(() -> {
			if (player.isValid()) {
				Player finalPlayer = makeCopy ? player.copy() : player;
				for (Player.Gamemode gamemode : leaderboardGamemodes) {
					insertIntoLeaderboard(finalPlayer, gamemode);
				}
			}
		});
	}

	private void insertIntoLeaderboard(Player player, Player.Gamemode gamemode) {
		try {
			String paramStr = "?,".repeat(types.size() + 1); // Add 1 for last_updated
			paramStr = paramStr.substring(0, paramStr.length() - 1);

			try (
				Connection connection = getConnection();
				PreparedStatement statement = connection.prepareStatement(
					"INSERT INTO " +
					gamemode.toCacheType() +
					" VALUES (" +
					paramStr +
					") ON CONFLICT (uuid) DO UPDATE SET " +
					typesSubList
						.stream()
						.map(t -> t + "=EXCLUDED." + t)
						.collect(Collectors.joining(",", "username=EXCLUDED.username,last_updated=EXCLUDED.last_updated,", ""))
				)
			) {
				statement.setString(1, player.getUuid());
				statement.setString(2, player.getUsername());
				statement.setLong(3, Instant.now().toEpochMilli());
				for (int i = 0; i < typesSubList.size(); i++) {
					String type = typesSubList.get(i);
					statement.setDouble(
						i + 4,
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
									"social",
									"enchanting" -> "_xp";
								default -> "";
							},
							gamemode
						)
					);
				}
				statement.executeUpdate();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Sync insert into requestedGamemode and async insert for other gamemodes (makes copy of player)
	 */
	public void insertIntoLeaderboardSync(Player player, Player.Gamemode requestedGamemode) {
		Player finalPlayer = player.copy();

		if (player.isValid()) {
			insertIntoLeaderboard(finalPlayer, requestedGamemode);
		}

		executor.submit(() -> {
			for (Player.Gamemode gamemode : leaderboardGamemodes) {
				if (gamemode != requestedGamemode) {
					if (player.isValid()) {
						insertIntoLeaderboard(finalPlayer, gamemode);
					}
				}
			}
		});
	}

	public void deleteFromLeaderboard(String uuid, Player.Gamemode gamemode) {
		try (
				Connection connection = getConnection();
				PreparedStatement statement = connection.prepareStatement("DELETE FROM " + gamemode.toCacheType() + " WHERE uuid = ?")
		) {
			statement.setString(1, uuid);
		} catch (SQLException e) {
			e.printStackTrace();
		}
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
				mode.toCacheType() +
				" OFFSET " +
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
						DataObject
							.empty()
							.put("rank", response.getInt("rank"))
							.put("username", response.getString("username"))
							.put(lbType, response.getDouble(lbType))
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
				mode.toCacheType() +
				") s WHERE uuid=?"
			)
		) {
			int rank = 0;
			statement.setString(1, uuid);
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
		Map<Integer, DataObject> out = new TreeMap<>();
		try (
			Connection connection = getConnection();
			PreparedStatement statement = connection.prepareStatement(
				"SELECT * FROM (SELECT username, " +
				lbType +
				", ROW_NUMBER() OVER(ORDER BY " +
				lbType +
				" DESC) AS rank FROM " +
				mode.toCacheType() +
				") s WHERE " +
				lbType +
				" > " +
				amount +
				" ORDER BY rank DESC LIMIT 200"
			)
		) {
			try (ResultSet response = statement.executeQuery()) {
				while (response.next()) {
					out.put(
						response.getInt("rank"),
						DataObject
							.empty()
							.put("rank", response.getInt("rank"))
							.put("username", response.getString("username"))
							.put(lbType, response.getDouble(lbType))
					);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		try (
			Connection connection = getConnection();
			PreparedStatement statement = connection.prepareStatement(
				"SELECT * FROM (SELECT username, " +
				lbType +
				", ROW_NUMBER() OVER(ORDER BY " +
				lbType +
				" DESC) AS rank FROM " +
				mode.toCacheType() +
				") s WHERE " +
				lbType +
				" <= " +
				amount +
				" LIMIT 200"
			)
		) {
			try (ResultSet response = statement.executeQuery()) {
				while (response.next()) {
					out.put(
						response.getInt("rank"),
						DataObject
							.empty()
							.put("rank", response.getInt("rank"))
							.put("username", response.getString("username"))
							.put(lbType, response.getDouble(lbType))
					);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		return out;
	}

	public int getNetworthPosition(Player.Gamemode gamemode, String uuid) {
		try (
				Connection connection = getConnection();
				PreparedStatement statement = connection.prepareStatement(
						"SELECT rank FROM (SELECT uuid, ROW_NUMBER() OVER(ORDER BY networth DESC) AS rank FROM " +
								gamemode.toCacheType() +
								") s WHERE uuid=?"
				)
		) {
			statement.setString(1, uuid);
			try (ResultSet response = statement.executeQuery()) {
				if (response.next()) {
					return response.getInt("rank");
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		return -1;
	}

	public void updateLeaderboard() {
		try {
			int count = 0;
			long start = System.currentTimeMillis();

			if (userCount != -1) {
				JsonArray members = getJson("https://raw.githubusercontent.com/kr45732/skyblock-plus-data/main/users.json")
					.getAsJsonArray();
				if (userCount >= members.size()) {
					log.info("Finished updating all users: " + userCount);
					userCount = -1;
				} else {
					for (count = 0; count <= 90 && userCount < members.size() && System.currentTimeMillis() - start < 57000; userCount++) {
						UsernameUuidStruct usernameUuidStruct = uuidToUsername(members.get(userCount).getAsString());
						if (usernameUuidStruct.isValid()) {
							count++;
							HypixelResponse profileResponse = skyblockProfilesFromUuid(
								usernameUuidStruct.uuid(),
								count < 45 ? "9312794c-8ed1-4350-968a-dedf71601e90" : "4991bfe2-d7aa-446a-b310-c7a70690927c",
								true,
								false
							);
							if (profileResponse.isValid()) {
								insertIntoLeaderboard(
									new Player(usernameUuidStruct.uuid(), usernameUuidStruct.username(), profileResponse.response(), true),
									false
								);
							}
						}
					}
					System.out.println("Finished up to user count: " + userCount);
				}
				return;
			}

			List<String> out = new ArrayList<>();
			try (
					Connection connection = getConnection();
					PreparedStatement statement = connection.prepareStatement(
							"SELECT uuid FROM all_lb WHERE last_updated < " + Instant.now().minus(5, ChronoUnit.DAYS).toEpochMilli() + " LIMIT 180"
					)
			) {
				try (ResultSet response = statement.executeQuery()) {
					while (response.next()) {
						out.add(
								response.getString("uuid")
						);
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
				return;
			}

			for (String uuid : out) {
				if (count == 90 || System.currentTimeMillis() - start >= 57000) {
					break;
				}

				UsernameUuidStruct usernameUuidStruct = uuidToUsername(uuid);
				if (usernameUuidStruct.isValid()) {
					count++;
					HypixelResponse profileResponse = skyblockProfilesFromUuid(
						usernameUuidStruct.uuid(),
						count < 45 ? "9312794c-8ed1-4350-968a-dedf71601e90" : "4991bfe2-d7aa-446a-b310-c7a70690927c",
						true,
						false
					);
					if (profileResponse.isValid()) {
						insertIntoLeaderboard(
							new Player(usernameUuidStruct.uuid(), usernameUuidStruct.username(), profileResponse.response(), true),
							false
						);
					}
				}
			}
			//			System.out.println("Updated " + count + " users in " + (System.currentTimeMillis() - start) + "ms");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static String getType(String lbType, boolean allLb) {
		lbType =
			switch (lbType = lbType.replace(" ", "_").toLowerCase()) {
				case "nw" -> "networth";
				case "wolf" -> "sven";
				case "spider" -> "tara";
				case "zombie" -> "rev";
				case "eman" -> "enderman";
				default -> lbType;
			};

		if (!(allLb ? typesSubList : guildTypesSubList).contains(lbType)) {
			lbType = getClosestMatch(lbType, allLb ? typesSubList : guildTypesSubList);
		}

		return lbType;
	}

	public void close() {
		dataSource.close();
	}
}
