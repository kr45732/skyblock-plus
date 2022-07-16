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

import static com.skyblockplus.utils.ApiHandler.asyncSkyblockProfilesFromUuid;
import static com.skyblockplus.utils.ApiHandler.uuidToUsername;
import static com.skyblockplus.utils.Player.COLLECTION_NAME_TO_ID;
import static com.skyblockplus.utils.Player.STATS_LIST;
import static com.skyblockplus.utils.Utils.*;

import com.google.gson.JsonArray;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.AggregateIterable;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Projections;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.Updates;
import com.skyblockplus.utils.Player;
import com.skyblockplus.utils.structs.UsernameUuidStruct;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
		types.addAll(STATS_LIST);

		typesSubList.addAll(types.subList(2, types.size()));
		formattedTypesSubList.addAll(typesSubList.stream().map(t -> capitalizeString(t.replace("_", " "))).toList());

		guildTypesSubList.addAll(guildTypes.subList(2, guildTypes.size()));
		formattedGuildTypesSubList.addAll(guildTypesSubList.stream().map(t -> capitalizeString(t.replace("_", " "))).toList());
	}

	private static final Logger log = LoggerFactory.getLogger(LeaderboardDatabase.class);

	public final MongoClient dataSource;
	private final List<Player.Gamemode> leaderboardGamemodes = Arrays.asList(
		Player.Gamemode.ALL,
		Player.Gamemode.IRONMAN,
		Player.Gamemode.STRANDED
	);
	public int userCount = -1;
	public ScheduledFuture<?> updateTask;

	public LeaderboardDatabase() {
		dataSource = new MongoClient(new MongoClientURI(LEADERBOARD_DB_URL));

		if (isMainBot()) {
			updateTask = scheduler.scheduleAtFixedRate(this::updateLeaderboard, 1, 1, TimeUnit.MINUTES);
		}
	}

	public MongoDatabase getConnection() {
		return dataSource.getDatabase("skyblock-plus");
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
			List<Bson> updates = new ArrayList<>();
			updates.add(Updates.set("last_updated", Instant.now().toEpochMilli()));
			updates.add(Updates.set("username", player.getUsername()));
			updates.add(Updates.set("uuid", player.getUuid()));
			for (String type : typesSubList) {
				updates.add(
					Updates.set(
						type,
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
					)
				);
			}

			getConnection()
				.getCollection(gamemode.toCacheType())
				.updateOne(Filters.eq("uuid", player.getUuid()), Updates.combine(updates), new UpdateOptions().upsert(true));
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
		try {
			getConnection().getCollection(gamemode.toCacheType()).deleteOne(Filters.eq("uuid", uuid));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * @param rankStart Exclusive
	 * @param rankEnd Inclusive
	 */
	public Map<Integer, Document> getLeaderboard(String lbType, Player.Gamemode mode, int rankStart, int rankEnd) {
		try {
			MongoCollection<Document> lbCollection = getConnection().getCollection(mode.toCacheType());

			Map<Integer, Document> out = new TreeMap<>();
			AggregateIterable<Document> leaderboard = lbCollection.aggregate(
				List.of(
					Document.parse("{$project: {\"_id\": 0,\"username\":1,\"" + lbType + "\": 1}}"),
					Document.parse("{$match: {" + lbType + ": {$gte: 0}}}"),
					Document.parse("{$setWindowFields: {sortBy: { " + lbType + ": -1 }, output: {rank: {$documentNumber: {}}}}}"),
					Document.parse("{$match: {rank : {$gt: " + rankStart + ", $lte: " + rankEnd + "}}}")
				)
			);

			for (Document player : leaderboard) {
				out.put(player.getInteger("rank"), player);
			}
			return out;
		} catch (Exception ignored) {}
		return null;
	}

	public Map<Integer, Document> getLeaderboard(String lbType, Player.Gamemode mode, String uuid) {
		try {
			MongoCollection<Document> lbCollection = getConnection().getCollection(mode.toCacheType());

			Document playerPos = lbCollection
				.aggregate(
					List.of(
						Document.parse("{$project: {\"_id\": 0,\"uuid\":1,\"" + lbType + "\": 1}}"),
						Document.parse("{$match: {" + lbType + ": {$gte: 0}}}"),
						Document.parse("{$setWindowFields: {sortBy: { " + lbType + ": -1 }, output: {rank: {$documentNumber: {}}}}}"),
						Document.parse("{$match : {uuid : { $eq : \"" + uuid + "\"}}}")
					)
				)
				.first();
			int rank = playerPos != null ? playerPos.getInteger("rank") : 0;

			return getLeaderboard(lbType, mode, rank - 200, rank + 200);
		} catch (Exception ignored) {}
		return null;
	}

	public Map<Integer, Document> getLeaderboard(String lbType, Player.Gamemode mode, double amount) {
		try {
			MongoCollection<Document> lbCollection = getConnection().getCollection(mode.toCacheType());

			Document amountPos = lbCollection
				.aggregate(
					List.of(
						Document.parse("{$project: {\"_id\": 0,\"" + lbType + "\": 1}}"),
						Document.parse("{$match: {" + lbType + ": {$gte: 0}}}"),
						Document.parse("{$setWindowFields: {sortBy: { " + lbType + ": -1 }, output: {rank: {$documentNumber: {}}}}}"),
						Document.parse("{$project: {diff: {$abs: {$subtract: [" + amount + ", \"$" + lbType + "\"]}}, rank: \"$rank\"}}"),
						Document.parse("{$sort: {diff: 1}}"),
						Document.parse("{$limit: 1}")
					)
				)
				.first();
			int rank = amountPos != null ? amountPos.getInteger("rank") : 0;

			return getLeaderboard(lbType, mode, rank - 200, rank + 200);
		} catch (Exception ignored) {}
		return null;
	}

	public int getNetworthPosition(Player.Gamemode gamemode, String uuid) {
		try {
			MongoCollection<Document> lbCollection = getConnection().getCollection(gamemode.toCacheType());

			Document playerPos = lbCollection
				.aggregate(
					List.of(
						Document.parse("{$project: {\"_id\": 0,\"uuid\":1,\"networth\": 1}}"),
						Document.parse("{$setWindowFields: {sortBy: { networth: -1 }, output: {rank: {$documentNumber: {}}}}}"),
						Document.parse("{$match : {uuid : { $eq : \"" + uuid + "\"}}}")
					)
				)
				.first();
			return playerPos != null ? playerPos.getInteger("rank") : -1;
		} catch (Exception ignored) {}
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
					for (count = 0; count <= 90 && userCount < members.size() && System.currentTimeMillis() - start < 60000; userCount++) {
						UsernameUuidStruct usernameUuidStruct = uuidToUsername(members.get(userCount).getAsString());
						if (!usernameUuidStruct.isNotValid()) {
							count++;
							asyncSkyblockProfilesFromUuid(
								usernameUuidStruct.uuid(),
								count < 45 ? "9312794c-8ed1-4350-968a-dedf71601e90" : "4991bfe2-d7aa-446a-b310-c7a70690927c",
								false
							)
								.whenComplete((r, e) ->
									insertIntoLeaderboard(
										new Player(usernameUuidStruct.uuid(), usernameUuidStruct.username(), r, true),
										false
									)
								);
						}
					}
					System.out.println("Finished up to user count: " + userCount);
				}
				return;
			}

			FindIterable<Document> response = getConnection()
				.getCollection("all_lb")
				.find(Filters.lt("last_updated", Instant.now().minus(5, ChronoUnit.DAYS).toEpochMilli()))
				.projection(Projections.include("uuid"))
				.limit(180);

			for (Document document : response) {
				if (count == 90 || System.currentTimeMillis() - start >= 60000) {
					break;
				}

				UsernameUuidStruct usernameUuidStruct = uuidToUsername(document.getString("uuid"));
				if (!usernameUuidStruct.isNotValid()) {
					asyncSkyblockProfilesFromUuid(
						usernameUuidStruct.uuid(),
						count < 45 ? "9312794c-8ed1-4350-968a-dedf71601e90" : "4991bfe2-d7aa-446a-b310-c7a70690927c",
						false
					)
						.whenComplete((r, e) ->
							insertIntoLeaderboard(new Player(usernameUuidStruct.uuid(), usernameUuidStruct.username(), r, true), false)
						);
				}
				count++;
			}
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
