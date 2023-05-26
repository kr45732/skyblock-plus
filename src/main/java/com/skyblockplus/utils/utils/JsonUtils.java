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

package com.skyblockplus.utils.utils;

import static com.skyblockplus.features.mayor.MayorHandler.currentMayor;
import static com.skyblockplus.utils.ApiHandler.getNeuBranch;
import static com.skyblockplus.utils.ApiHandler.getQueryApiUrl;
import static com.skyblockplus.utils.Constants.getConstant;
import static com.skyblockplus.utils.utils.HttpUtils.*;
import static com.skyblockplus.utils.utils.StringUtils.nameToId;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.FileReader;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.http.client.utils.URIBuilder;

public class JsonUtils {

	private static final Pattern bazaarEnchantPattern = Pattern.compile("ENCHANTMENT_(\\D*)_(\\d+)");
	private static List<String> queryItems = new ArrayList<>();
	public static final List<String> vanillaItems = new ArrayList<>();
	private static final Set<String> dungeonLootItems = new HashSet<>();
	private static Instant lowestBinJsonLastUpdated = Instant.now();
	private static Instant averagePriceJsonLastUpdated = Instant.now();
	private static Instant averageAuctionJsonLastUpdated = Instant.now();
	private static Instant averageBinJsonLastUpdated = Instant.now();
	private static Instant bingoJsonLastUpdated = Instant.now();
	private static Instant bazaarJsonLastUpdated = Instant.now();
	private static Instant extraPricesJsonLastUpdated = Instant.now();
	private static JsonObject essenceCostsJson;
	private static JsonObject levelingJson;
	private static JsonObject collectionsJson;
	private static JsonObject enchantsJson;
	private static JsonObject petNumsJson;
	private static JsonObject petsJson;
	private static JsonObject parentsJson;
	private static JsonObject reforgeStonesJson;
	private static JsonObject bitsJson;
	private static JsonObject copperJson;
	private static JsonObject miscJson;
	private static JsonObject lowestBinJson;
	private static JsonObject averagePriceJson;
	private static JsonObject averageAuctionJson;
	private static JsonObject averageBinJson;
	private static JsonObject bazaarJson;
	private static JsonObject extraPricesJson;
	private static JsonObject emojiMap;
	private static Map<String, JsonElement> skyblockItemsJson;
	private static JsonObject internalJsonMappings;
	private static JsonObject priceOverrideJson;
	private static JsonObject bingoInfoJson;
	private static JsonObject dungeonLootJson;
	private static JsonObject dragonLootJson;
	private static JsonObject weightJson;
	private static JsonObject sbLevelsJson;
	private static JsonObject essenceShopsJson;

	public static JsonObject getLowestBinJson() {
		if (lowestBinJson == null || Duration.between(lowestBinJsonLastUpdated, Instant.now()).toMinutes() >= 1) {
			lowestBinJson = getJsonObject(getQueryApiUrl("lowestbin").toString());

			if (lowestBinJson == null || !higherDepth(lowestBinJson, "success", true)) {
				lowestBinJson = getJsonObject("https://moulberry.codes/lowestbin.json");
			}
			lowestBinJsonLastUpdated = Instant.now();
		}

		return lowestBinJson;
	}

	public static JsonObject getInternalJsonMappings() {
		if (internalJsonMappings == null) {
			internalJsonMappings =
				getJsonObject("https://raw.githubusercontent.com/kr45732/skyblock-plus-data/main/InternalNameMappings.json");
		}

		return internalJsonMappings;
	}

	public static JsonObject getEmojiMap() {
		if (emojiMap == null) {
			try {
				emojiMap =
					JsonParser.parseReader(new FileReader("src/main/java/com/skyblockplus/json/IdToEmojiMappings.json")).getAsJsonObject();
			} catch (Exception ignored) {}
		}

		return emojiMap;
	}

	public static JsonObject getAveragePriceJson() {
		if (averagePriceJson == null || Duration.between(averagePriceJsonLastUpdated, Instant.now()).toMinutes() >= 1) {
			// Other requests will use the old json while the new json is being fetched (since it takes a few seconds)
			averagePriceJsonLastUpdated = Instant.now();

			URIBuilder uriBuilder = getQueryApiUrl("average");
			if (!currentMayor.equals("Derpy")) {
				uriBuilder.addParameter("time", "" + Instant.now().minus(4, ChronoUnit.DAYS).toEpochMilli()).addParameter("step", "60");
			}
			averagePriceJson = getJsonObject(uriBuilder.toString());

			if (averagePriceJson == null) {
				averagePriceJson = getJsonObject("https://moulberry.codes/auction_averages/3day.json");
			}
		}

		return averagePriceJson;
	}

	public static JsonObject getAverageAuctionJson() {
		if (averageAuctionJson == null || Duration.between(averageAuctionJsonLastUpdated, Instant.now()).toMinutes() >= 1) {
			// Other requests will use the old json while the new json is being fetched (since it takes a few seconds)
			averageAuctionJsonLastUpdated = Instant.now();

			URIBuilder uriBuilder = getQueryApiUrl("average_auction");
			if (!currentMayor.equals("Derpy")) {
				uriBuilder.addParameter("time", "" + Instant.now().minus(4, ChronoUnit.DAYS).toEpochMilli()).addParameter("step", "60");
			}
			averageAuctionJson = getJsonObject(uriBuilder.toString());

			if (averageAuctionJson == null) {
				averageAuctionJson = getJsonObject("https://moulberry.codes/auction_averages/3day.json");
			}
		}

		return averageAuctionJson;
	}

	public static JsonObject getAverageBinJson() {
		if (averageBinJson == null || Duration.between(averageBinJsonLastUpdated, Instant.now()).toMinutes() >= 1) {
			// Other requests will use the old json while the new json is being fetched (since it takes a few seconds)
			averageBinJsonLastUpdated = Instant.now();

			URIBuilder uriBuilder = getQueryApiUrl("average_bin");
			if (!currentMayor.equals("Derpy")) {
				uriBuilder.addParameter("time", "" + Instant.now().minus(4, ChronoUnit.DAYS).toEpochMilli()).addParameter("step", "60");
			}
			averageBinJson = getJsonObject(uriBuilder.toString());

			if (averageBinJson == null) {
				averageBinJson = getJsonObject("https://moulberry.codes/auction_averages/3day.json");
			}
		}

		return averageBinJson;
	}

	public static JsonObject getBingoInfoJson() {
		if (bingoInfoJson == null || Duration.between(bingoJsonLastUpdated, Instant.now()).toMinutes() >= 5) {
			bingoInfoJson = getJsonObject("https://api.hypixel.net/resources/skyblock/bingo");
			bingoJsonLastUpdated = Instant.now();
		}

		return bingoInfoJson;
	}

	public static JsonObject getBazaarJson() {
		if (bazaarJson == null || Duration.between(bazaarJsonLastUpdated, Instant.now()).toMinutes() >= 1) {
			try {
				JsonObject tempBazaarJson = new JsonObject();
				for (Map.Entry<String, JsonElement> entry : getJsonObject("https://api.hypixel.net/skyblock/bazaar")
					.get("products")
					.getAsJsonObject()
					.entrySet()) {
					String id = entry.getKey();

					Matcher matcher = bazaarEnchantPattern.matcher(entry.getKey());
					if (matcher.matches()) {
						id = matcher.group(1) + ";" + matcher.group(2);
					}

					if (!getInternalJsonMappings().has(id)) {
						continue;
					}

					JsonObject j = new JsonObject();
					j.addProperty("buy_summary", higherDepth(entry.getValue(), "buy_summary.[0].pricePerUnit", 0.0));
					j.addProperty("sell_summary", higherDepth(entry.getValue(), "sell_summary.[0].pricePerUnit", 0.0));
					j.addProperty("buyVolume", higherDepth(entry.getValue(), "quick_status.buyVolume", 0L));
					j.addProperty("sellVolume", higherDepth(entry.getValue(), "quick_status.sellVolume", 0L));
					tempBazaarJson.add(id, j);
				}
				bazaarJson = tempBazaarJson;
				bazaarJsonLastUpdated = Instant.now();
			} catch (Exception ignored) {}
		}

		return bazaarJson;
	}

	public static List<String> getQueryItems() {
		if (queryItems == null) {
			try {
				queryItems =
					streamJsonArray(getJson(getQueryApiUrl("query_items").toString()))
						.map(JsonElement::getAsString)
						.collect(Collectors.toCollection(ArrayList::new));
			} catch (Exception ignored) {}
		}

		return queryItems;
	}

	public static Set<String> getDungeonLootItems() {
		if (dungeonLootItems.isEmpty()) {
			getDungeonLootJson();
		}

		return dungeonLootItems;
	}

	public static Map<String, JsonElement> getSkyblockItemsJson() {
		if (skyblockItemsJson == null) {
			skyblockItemsJson = new HashMap<>();
			for (JsonElement item : higherDepth(getJson("https://api.hypixel.net/resources/skyblock/items"), "items").getAsJsonArray()) {
				if (higherDepth(item, "gemstone_slots") != null) {
					Map<String, Integer> count = new HashMap<>();
					for (JsonElement slot : higherDepth(item, "gemstone_slots").getAsJsonArray()) {
						String slotName = higherDepth(slot, "slot_type").getAsString();
						slot
							.getAsJsonObject()
							.addProperty(
								"formatted_slot_type",
								slotName + "_" + count.compute(slotName, (k, v) -> (v == null ? 0 : v + 1))
							);
					}
				}
				skyblockItemsJson.put(higherDepth(item, "id").getAsString(), item);
			}
		}

		return skyblockItemsJson;
	}

	public static JsonObject getExtraPricesJson() {
		if (extraPricesJson == null || Duration.between(extraPricesJsonLastUpdated, Instant.now()).toMinutes() >= 15) {
			extraPricesJson = getJson("https://raw.githubusercontent.com/SkyHelperBot/Prices/main/prices.json").getAsJsonObject();
			extraPricesJsonLastUpdated = Instant.now();
		}

		return extraPricesJson;
	}

	public static JsonObject getMiscJson() {
		if (miscJson == null) {
			miscJson =
				getJsonObject(
					"https://raw.githubusercontent.com/NotEnoughUpdates/NotEnoughUpdates-REPO/" + getNeuBranch() + "/constants/misc.json"
				);
		}

		return miscJson;
	}

	public static JsonObject getDungeonLootJson() {
		if (dungeonLootJson == null) {
			dungeonLootJson = getJsonObject("https://raw.githubusercontent.com/kr45732/skyblock-plus-data/main/DungeonLoot.json");
			for (Map.Entry<String, JsonElement> floor : dungeonLootJson.entrySet()) {
				for (Map.Entry<String, JsonElement> chest : floor.getValue().getAsJsonObject().entrySet()) {
					for (JsonElement item : chest.getValue().getAsJsonArray()) {
						String itemId = nameToId(higherDepth(item, "item").getAsString());
						item.getAsJsonObject().addProperty("id", itemId);
						dungeonLootItems.add(itemId);
					}
				}
			}
		}

		return dungeonLootJson;
	}

	public static JsonObject getDragonLootJson() {
		if (dragonLootJson == null) {
			dragonLootJson = getJsonObject("https://raw.githubusercontent.com/kr45732/skyblock-plus-data/main/DragonLoot.json");
		}

		return dragonLootJson;
	}

	public static JsonObject getWeightJson() {
		if (weightJson == null) {
			weightJson =
				getJsonObject(
					"https://raw.githubusercontent.com/NotEnoughUpdates/NotEnoughUpdates-REPO/" + getNeuBranch() + "/constants/weight.json"
				);
		}

		return weightJson;
	}

	public static JsonObject getSbLevelsJson() {
		if (sbLevelsJson == null) {
			sbLevelsJson =
				getJsonObject(
					"https://github.com/NotEnoughUpdates/NotEnoughUpdates-REPO/raw/" + getNeuBranch() + "/constants/sblevels.json"
				);
		}

		return sbLevelsJson;
	}

	public static JsonObject getEssenceShopsJson() {
		if (essenceShopsJson == null) {
			essenceShopsJson =
				getJsonObject(
					"https://raw.githubusercontent.com/NotEnoughUpdates/NotEnoughUpdates-REPO/" +
					getNeuBranch() +
					"/constants/essenceshops.json"
				);
		}

		return essenceShopsJson;
	}

	public static JsonObject getBitsJson() {
		if (bitsJson == null) {
			bitsJson = getJsonObject("https://raw.githubusercontent.com/kr45732/skyblock-plus-data/main/BitPrices.json");
		}

		return bitsJson;
	}

	public static JsonObject getCopperJson() {
		if (copperJson == null) {
			copperJson = getJsonObject("https://raw.githubusercontent.com/kr45732/skyblock-plus-data/main/CopperPrices.json");
		}

		return copperJson;
	}

	public static JsonObject getReforgeStonesJson() {
		if (reforgeStonesJson == null) {
			reforgeStonesJson =
				getJsonObject(
					"https://raw.githubusercontent.com/NotEnoughUpdates/NotEnoughUpdates-REPO/" +
					getNeuBranch() +
					"/constants/reforgestones.json"
				);
		}

		return reforgeStonesJson;
	}

	public static JsonObject getPetJson() {
		if (petsJson == null) {
			petsJson =
				getJsonObject(
					"https://raw.githubusercontent.com/NotEnoughUpdates/NotEnoughUpdates-REPO/" + getNeuBranch() + "/constants/pets.json"
				);
		}
		return petsJson;
	}

	public static JsonObject getParentsJson() {
		if (parentsJson == null) {
			parentsJson =
				getJsonObject(
					"https://raw.githubusercontent.com/NotEnoughUpdates/NotEnoughUpdates-REPO/" + getNeuBranch() + "/constants/parents.json"
				);
		}
		return parentsJson;
	}

	public static JsonObject getPetNumsJson() {
		if (petNumsJson == null) {
			petNumsJson =
				getJsonObject(
					"https://raw.githubusercontent.com/NotEnoughUpdates/NotEnoughUpdates-REPO/" + getNeuBranch() + "/constants/petnums.json"
				);
		}
		return petNumsJson;
	}

	public static JsonObject getEnchantsJson() {
		if (enchantsJson == null) {
			enchantsJson =
				getJsonObject(
					"https://raw.githubusercontent.com/NotEnoughUpdates/NotEnoughUpdates-REPO/" +
					getNeuBranch() +
					"/constants/enchants.json"
				);
		}
		return enchantsJson;
	}

	public static JsonObject getLevelingJson() {
		if (levelingJson == null) {
			levelingJson =
				getJsonObject(
					"https://raw.githubusercontent.com/NotEnoughUpdates/NotEnoughUpdates-REPO/" +
					getNeuBranch() +
					"/constants/leveling.json"
				);
		}
		return levelingJson;
	}

	public static JsonObject getEssenceCostsJson() {
		if (essenceCostsJson == null) {
			essenceCostsJson =
				getJsonObject(
					"https://raw.githubusercontent.com/NotEnoughUpdates/NotEnoughUpdates-REPO/" +
					getNeuBranch() +
					"/constants/essencecosts.json"
				);
			for (Map.Entry<String, JsonElement> entry : getConstant("ADDITIONAL_ESSENCE_UPGRADE_DATA").getAsJsonObject().entrySet()) {
				essenceCostsJson.add(entry.getKey(), entry.getValue());
			}
		}
		return essenceCostsJson;
	}

	public static JsonObject getCollectionsJson() {
		if (collectionsJson == null) {
			collectionsJson = new JsonObject();
			JsonObject hypixelCollectionsJson = higherDepth(
				getJson("https://api.hypixel.net/resources/skyblock/collections"),
				"collections"
			)
				.getAsJsonObject();
			for (Map.Entry<String, JsonElement> collectionType : hypixelCollectionsJson.entrySet()) {
				JsonObject collectionItems = higherDepth(collectionType.getValue(), "items").getAsJsonObject();
				for (Map.Entry<String, JsonElement> item : collectionItems.entrySet()) {
					JsonArray tierAmounts = new JsonArray();
					for (JsonElement tierAmount : higherDepth(item.getValue(), "tiers").getAsJsonArray()) {
						tierAmounts.add(higherDepth(tierAmount, "amountRequired"));
					}
					JsonObject idAndTier = new JsonObject();
					idAndTier.add("name", higherDepth(item.getValue(), "name"));
					idAndTier.addProperty("type", collectionType.getKey().toLowerCase());
					idAndTier.add("tiers", tierAmounts);
					collectionsJson.add(item.getKey(), idAndTier);
				}
			}
		}

		return collectionsJson;
	}

	public static JsonObject getPriceOverrideJson() {
		if (priceOverrideJson == null) {
			JsonElement splitPriceOverrides = getJson(
				"https://raw.githubusercontent.com/kr45732/skyblock-plus-data/main/PriceOverrides.json"
			)
				.getAsJsonObject();
			priceOverrideJson = higherDepth(splitPriceOverrides, "automatic").getAsJsonObject();
			vanillaItems.addAll(priceOverrideJson.keySet());
			for (Map.Entry<String, JsonElement> manualOverride : higherDepth(splitPriceOverrides, "manual").getAsJsonObject().entrySet()) {
				priceOverrideJson.add(manualOverride.getKey(), manualOverride.getValue());
			}
		}

		return priceOverrideJson;
	}

	public static void resetSbPlusData() {
		internalJsonMappings = null;
		priceOverrideJson = null;
	}

	public static void resetQueryItems() {
		queryItems = null;
	}

	/* Miscellaneous */
	public static JsonElement higherDepth(JsonElement element, String path) {
		String[] paths = path.split("\\.");

		try {
			for (String key : paths) {
				if (key.length() >= 3 && key.startsWith("[") && key.endsWith("]")) {
					int idx = Integer.parseInt(key.substring(1, key.length() - 1));
					element = element.getAsJsonArray().get(idx == -1 ? element.getAsJsonArray().size() - 1 : idx);
				} else {
					element = element.getAsJsonObject().get(key);
				}
			}
			return element;
		} catch (Exception e) {
			return null;
		}
	}

	public static String higherDepth(JsonElement element, String path, String defaultValue) {
		try {
			return higherDepth(element, path).getAsString();
		} catch (Exception e) {
			return defaultValue;
		}
	}

	public static boolean higherDepth(JsonElement element, String path, boolean defaultValue) {
		try {
			return higherDepth(element, path).getAsBoolean();
		} catch (Exception e) {
			return defaultValue;
		}
	}

	public static long higherDepth(JsonElement element, String path, long defaultValue) {
		try {
			return higherDepth(element, path).getAsLong();
		} catch (Exception e) {
			return defaultValue;
		}
	}

	public static int higherDepth(JsonElement element, String path, int defaultValue) {
		try {
			return higherDepth(element, path).getAsInt();
		} catch (Exception e) {
			return defaultValue;
		}
	}

	public static double higherDepth(JsonElement element, String path, double defaultValue) {
		try {
			return higherDepth(element, path).getAsDouble();
		} catch (Exception e) {
			return defaultValue;
		}
	}

	public static ArrayList<String> getJsonKeys(JsonElement jsonElement) {
		return jsonElement != null && jsonElement.isJsonObject()
			? new ArrayList<>(jsonElement.getAsJsonObject().keySet())
			: new ArrayList<>();
	}

	public static Stream<JsonElement> streamJsonArray(JsonElement array) {
		List<JsonElement> list = new ArrayList<>();
		for (JsonElement element : array.getAsJsonArray()) {
			list.add(element);
		}
		return list.stream();
	}

	public static JsonArray collectJsonArray(Stream<JsonElement> list) {
		JsonArray array = new JsonArray();
		list.forEach(array::add);
		return array;
	}
}
