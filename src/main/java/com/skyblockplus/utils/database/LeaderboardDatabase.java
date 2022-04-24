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

import static com.skyblockplus.utils.ApiHandler.*;
import static com.skyblockplus.utils.Player.COLLECTION_NAME_TO_ID;
import static com.skyblockplus.utils.Utils.*;
import static com.skyblockplus.utils.database.LeaderboardDatabase.getTypes;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.*;
import com.skyblockplus.utils.Player;
import com.skyblockplus.utils.structs.HypixelGuildCache;
import com.skyblockplus.utils.structs.HypixelResponse;
import com.skyblockplus.utils.structs.UsernameUuidStruct;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.apache.commons.collections4.ListUtils;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LeaderboardDatabase {

	public static final List<String> types = ListUtils.union(
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
			"enchanting",
			"networth",
			"blaze"
		),
		COLLECTION_NAME_TO_ID.keySet().stream().toList()
	);
	private static final Logger log = LoggerFactory.getLogger(LeaderboardDatabase.class);

	private final MongoClient dataSource;
	private final List<Player.Gamemode> leaderboardGamemodes = Arrays.asList(
		Player.Gamemode.ALL,
		Player.Gamemode.IRONMAN,
		Player.Gamemode.STRANDED
	);
	public int guildCount = -1;

	public LeaderboardDatabase() {
		dataSource = new MongoClient(new MongoClientURI(LEADERBOARD_DB_URL));

		if (isMainBot()) {
			scheduler.scheduleAtFixedRate(this::updateLeaderboard, 1, 1, TimeUnit.MINUTES);
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
		try {
			List<Bson> updates = new ArrayList<>();
			updates.add(Updates.set("last_updated", Instant.now().toEpochMilli()));
			updates.add(Updates.set("username", player.getUsername()));
			updates.add(Updates.set("uuid", player.getUuid()));
			for (String type : getTypes()) {
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
									"enchanting" -> "_xp";
								default -> "";
							},
							gamemode,
							true
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

	private void deleteFromLeaderboard(String uuid, Player.Gamemode gamemode) {
		try {
			getConnection().getCollection(gamemode.toCacheType()).deleteOne(Filters.eq("uuid", uuid));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public Map<String, Double> getLeaderboard(String lbType, Player.Gamemode mode) {
		try {
			MongoCollection<Document> lbCollection = getConnection().getCollection(mode.toCacheType());
			FindIterable<Document> response = lbCollection
				.find()
				.projection(Projections.include("username", lbType))
				.sort(Sorts.descending(lbType));

			Map<String, Double> out = new LinkedHashMap<>();
			for (Document document : response) {
				try {
					Double value = document.getDouble(lbType);
					out.put(document.getString("username"), value == null ? 0 : value);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			return out;
		} catch (Exception ignored) {}
		return null;
	}

	public void a() {
		MongoCollection<Document> lbCollection = getConnection().getCollection("all_lb");
		lbCollection.updateMany(
			Filters.ne("username", "CrypticPlasma"),
			Updates.set("last_updated", Instant.now().minus(6, ChronoUnit.DAYS).toEpochMilli())
		);
		FindIterable<Document> response = lbCollection.find();

		int count = 0;
		for (Document document : response) {
			long lastUpdated;
			try {
				lastUpdated = Long.parseLong(document.getString("last_updated"));
			} catch (Exception e) {
				continue;
			}
			lbCollection.updateOne(Filters.eq("uuid", document.getString("uuid")), Updates.set("last_updated", lastUpdated));
			count++;

			if (count % 500 == 0) {
				System.out.println(count);
			}
		}
	}

	public void updateLeaderboard() {
		try {
			FindIterable<Document> response = getConnection()
				.getCollection("all_lb")
				.find(Filters.lt("last_updated", Instant.now().minus(5, ChronoUnit.DAYS)))
				.projection(Projections.include("uuid"))
				.limit(180);
			int count = 0;
			long start = System.currentTimeMillis();
			for (Document document : response) {
				String uuid = document.getString("uuid");
				UsernameUuidStruct usernameUuidStruct = uuidToUsername(uuid);
				if (usernameUuidStruct.isNotValid()) {
					executor.submit(() -> {
						for (Player.Gamemode gamemode : leaderboardGamemodes) {
							leaderboardDatabase.deleteFromLeaderboard(uuid, gamemode);
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
									leaderboardDatabase.deleteFromLeaderboard(uuid, gamemode);
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
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static List<String> getTypes() {
		return getTypes(false);
	}

	/**
	 * @return Only stats (no uuid or username)
	 */
	public static List<String> getTypes(boolean formatted) {
		List<String> typesSubList = types.subList(2, types.size());
		return formatted
			? typesSubList.stream().map(t -> capitalizeString(t.replace("_", " "))).collect(Collectors.toList())
			: typesSubList;
	}

	public static String getType(String lbType) {
		lbType =
			switch (lbType = lbType.replace(" ", "_").toLowerCase()) {
				case "nw" -> "networth";
				case "wolf" -> "sven";
				case "spider" -> "tara";
				case "zombie" -> "rev";
				case "eman" -> "enderman";
				default -> lbType;
			};

		if (!isValidType(lbType)) {
			lbType = getClosestMatch(lbType, getTypes());
		}

		return lbType;
	}

	public static boolean isValidType(String type) {
		return HypixelGuildCache.typeToIndex(type.toLowerCase()) >= 2;
	}
}
