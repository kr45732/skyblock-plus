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

package com.skyblockplus.miscellaneous.networth;

import static com.skyblockplus.utils.ApiHandler.getAuctionPetsByName;
import static com.skyblockplus.utils.Constants.*;
import static com.skyblockplus.utils.Utils.*;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.skyblockplus.utils.Player;
import com.skyblockplus.utils.command.PaginatorExtras;
import com.skyblockplus.utils.command.SelectMenuPaginator;
import com.skyblockplus.utils.structs.InvItem;
import java.util.*;
import java.util.stream.Collectors;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.GenericInteractionCreateEvent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.selections.SelectOption;

public class NetworthExecute {

	private static final List<String> locations = List.of(
		"inventory",
		"talisman",
		"armor",
		"wardrobe",
		"pets",
		"enderchest",
		"equipment",
		"personal_vault",
		"storage"
	);
	//	private final Set<String> tempSet = new HashSet<>();
	private final Map<String, List<InvItem>> pets = new HashMap<>();
	private final Map<String, List<String>> items = new HashMap<>();
	private final Map<String, Double> totals = new HashMap<>();
	private StringBuilder calcItemsJsonStr = new StringBuilder("[");
	private JsonElement lowestBinJson;
	private JsonElement averageAuctionJson;
	private JsonElement bazaarJson;
	private JsonObject extraPrices;
	private double recombPrice;
	private double fumingPrice;
	private double hbpPrice;
	private boolean verbose = false;

	public static double getNetworth(String username, String profileName) {
		NetworthExecute calc = new NetworthExecute();
		calc.getPlayerNetworth(username, profileName, null);
		return calc.getNetworth();
	}

	public static double getNetworth(Player player) {
		NetworthExecute calc = new NetworthExecute();
		calc.getPlayerNetworth(player, null);
		return calc.getNetworth();
	}

	public NetworthExecute setVerbose(boolean verbose) {
		this.verbose = verbose;
		return this;
	}

	public NetworthExecute initPrices() {
		lowestBinJson = getLowestBinJson();
		averageAuctionJson = getAverageAuctionJson();
		bazaarJson = getBazaarJson();
		extraPrices = getExtraPricesJson();

		recombPrice = higherDepth(bazaarJson, "RECOMBOBULATOR_3000.sell_summary.[0].pricePerUnit", 0.0);
		hbpPrice = higherDepth(bazaarJson, "HOT_POTATO_BOOK.sell_summary.[0].pricePerUnit", 0.0);
		fumingPrice = higherDepth(bazaarJson, "FUMING_POTATO_BOOK.sell_summary.[0].pricePerUnit", 0.0);
		return this;
	}

	public EmbedBuilder getPlayerNetworth(String username, String profileName, GenericInteractionCreateEvent event) {
		return getPlayerNetworth(profileName == null ? new Player(username) : new Player(username, profileName), event);
	}

	public EmbedBuilder getPlayerNetworth(Player player, GenericInteractionCreateEvent event) {
		if (!player.isValid()) {
			return player.getFailEmbed();
		}

		Map<Integer, InvItem> playerInventory = player.getInventoryMap();
		if (playerInventory == null) {
			addTotal("inventory", -1.0);
			return defaultEmbed(player.getUsernameFixed() + "'s inventory API is disabled");
		}

		initPrices();

		addTotal("bank", player.getBankBalance());
		addTotal("purse", player.getPurseCoins());

		for (String location : locations) {
			Map<Integer, InvItem> curItems =
				switch (location) {
					case "inventory" -> playerInventory;
					case "talisman" -> player.getTalismanBagMap();
					case "armor" -> player.getArmorMap();
					case "equipment" -> player.getEquipmentMap();
					case "wardrobe" -> player.getWardrobeMap();
					case "pets" -> player.getPetsMap();
					case "enderchest" -> player.getEnderChestMap();
					case "personal_vault" -> player.getPersonalVaultMap();
					case "storage" -> player.getStorageMap();
					default -> throw new IllegalStateException("Unexpected value: " + location);
				};

			if (curItems != null) {
				for (InvItem item : curItems.values()) {
					if (item != null) {
						double itemPrice = calculateItemPrice(item, location);
						if (itemPrice >= 0) { // -1 if pet
							addTotal(location, itemPrice);
							if (event != null) {
								addItem(location, addItemStr(item, itemPrice));
							}
						}
					}
				}
			}
		}

		Map<String, Integer> sacksMap = player.getPlayerSacks();
		if (sacksMap != null) {
			for (Map.Entry<String, Integer> sackEntry : sacksMap.entrySet()) {
				if (sackEntry.getValue() > 0) {
					double itemPrice = getLowestPrice(sackEntry.getKey(), true, null) * sackEntry.getValue();
					addTotal("sacks", itemPrice);
					if (event != null) {
						String emoji = getEmojiOr(sackEntry.getKey(), null);
						addItem(
							"sacks",
							(emoji == null ? "" : emoji + " ") +
							(sackEntry.getValue() != 1 ? sackEntry.getValue() + "x " : "") +
							idToName(sackEntry.getKey()) +
							"=:=" +
							itemPrice
						);
					}
				}
			}
		}

		calculatePetPrices();

		if (event == null) {
			return invalidEmbed("Was not triggered by command");
		}

		StringBuilder echestStr = getSectionString(getItems("enderchest"));
		StringBuilder sacksStr = getSectionString(getItems("sacks"));
		StringBuilder personalVaultStr = getSectionString(getItems("personal_vault"));
		StringBuilder storageStr = getSectionString(getItems("storage"));
		StringBuilder invStr = getSectionString(getItems("inventory"));
		StringBuilder armorStr = getSectionString(getItems("armor"));
		StringBuilder wardrobeStr = getSectionString(getItems("wardrobe"));
		StringBuilder petsStr = getSectionString(getItems("pets"));
		StringBuilder talismanStr = getSectionString(getItems("talisman"));

		double totalNetworth = getNetworth();
		//			int position = leaderboardDatabase.getNetworthPosition(player.getGamemode(), player.getUuid());
		String ebDesc = "**Total Networth:** " + simplifyNumber(totalNetworth) + " (" + roundAndFormat(totalNetworth) + ")";
		//			\n**" +
		//				(
		//					Player.Gamemode.IRONMAN_STRANDED.isGamemode(player.getGamemode())
		//						? capitalizeString(player.getGamemode().toString()) + " "
		//						: ""
		//				)+"Leaderboard Position:** " + (position != -1 ? formatNumber(position) : "Not on leaderboard");
		EmbedBuilder eb = player.defaultPlayerEmbed().setDescription(ebDesc);
		eb.addField("Purse", simplifyNumber(getTotal("purse")), true);
		eb.addField("Bank", (getTotal("bank") == -1 ? "Private" : simplifyNumber(getTotal("bank"))), true);
		eb.addField("Sacks", simplifyNumber(getTotal("sacks")), true);
		if (!echestStr.isEmpty()) {
			eb.addField("Ender Chest | " + simplifyNumber(getTotal("enderchest")), echestStr.toString().split("\n\n")[0], false);
		}
		if (!storageStr.isEmpty()) {
			eb.addField("Storage | " + simplifyNumber(getTotal("storage")), storageStr.toString().split("\n\n")[0], false);
		}
		if (!invStr.isEmpty()) {
			eb.addField("Inventory | " + simplifyNumber(getTotal("inventory")), invStr.toString().split("\n\n")[0], false);
		}
		if (!armorStr.isEmpty()) {
			eb.addField("Armor & Equipment | " + simplifyNumber(getTotal("armor")), armorStr.toString().split("\n\n")[0], false);
		}
		if (!wardrobeStr.isEmpty()) {
			eb.addField("Wardrobe | " + simplifyNumber(getTotal("wardrobe")), wardrobeStr.toString().split("\n\n")[0], false);
		}
		if (!petsStr.isEmpty()) {
			eb.addField("Pets | " + simplifyNumber(getTotal("pets")), petsStr.toString().split("\n\n")[0], false);
		}
		if (!talismanStr.isEmpty()) {
			eb.addField("Accessories | " + simplifyNumber(getTotal("talisman")), talismanStr.toString().split("\n\n")[0], false);
		}
		if (!personalVaultStr.isEmpty()) {
			eb.addField(
				"Personal Vault | " + simplifyNumber(getTotal("personal_vault")),
				personalVaultStr.toString().split("\n\n")[0],
				false
			);
		}

		PaginatorExtras extras = new PaginatorExtras(PaginatorExtras.PaginatorType.EMBED_PAGES);
		Map<SelectOption, EmbedBuilder> pages = new LinkedHashMap<>();
		pages.put(SelectOption.of("Overview", "overview").withEmoji(getEmojiObj("SKYBLOCK_MENU")), eb);

		if (!echestStr.isEmpty()) {
			pages.put(
				SelectOption.of("Ender Chest", "ender_chest").withEmoji(getEmojiObj("ENDER_CHEST")),
				player
					.defaultPlayerEmbed(" | Ender Chest")
					.setDescription(
						ebDesc +
						"\n**Ender Chest:** " +
						simplifyNumber(getTotal("enderchest")) +
						"\n\n" +
						echestStr.toString().replace("\n\n", "\n")
					)
			);
		}
		if (!storageStr.isEmpty()) {
			pages.put(
				SelectOption.of("Storage", "storage").withEmoji(getEmojiObj("SMALL_BACKPACK")),
				player
					.defaultPlayerEmbed(" | Storage")
					.setDescription(
						ebDesc +
						"\n**Storage:** " +
						simplifyNumber(getTotal("storage")) +
						"\n\n" +
						storageStr.toString().replace("\n\n", "\n")
					)
			);
		}
		if (!invStr.isEmpty()) {
			pages.put(
				SelectOption.of("Inventory", "inventory").withEmoji(getEmojiObj("CHEST")),
				player
					.defaultPlayerEmbed(" | Inventory")
					.setDescription(
						ebDesc +
						"\n**Inventory:** " +
						simplifyNumber(getTotal("inventory")) +
						"\n\n" +
						invStr.toString().replace("\n\n", "\n")
					)
			);
		}
		if (!armorStr.isEmpty()) {
			pages.put(
				SelectOption.of("Armor & Equipment", "armor_equipment").withEmoji(getEmojiObj("GOLD_CHESTPLATE")),
				player
					.defaultPlayerEmbed(" | Armor & Equipment")
					.setDescription(
						ebDesc +
						"\n**Armor & Equipment:** " +
						simplifyNumber(getTotal("armor")) +
						"\n\n" +
						armorStr.toString().replace("\n\n", "\n")
					)
			);
		}
		if (!wardrobeStr.isEmpty()) {
			pages.put(
				SelectOption.of("Wardrobe", "wardrobe").withEmoji(getEmojiObj("ARMOR_STAND")),
				player
					.defaultPlayerEmbed(" | Wardrobe")
					.setDescription(
						ebDesc +
						"\n**Wardrobe:** " +
						simplifyNumber(getTotal("wardrobe")) +
						"\n\n" +
						wardrobeStr.toString().replace("\n\n", "\n")
					)
			);
		}
		if (!petsStr.isEmpty()) {
			pages.put(
				SelectOption.of("Pets", "pets").withEmoji(getEmojiObj("ENDER_DRAGON;4")),
				player
					.defaultPlayerEmbed(" | Pets")
					.setDescription(
						ebDesc + "\n**Pets:** " + simplifyNumber(getTotal("pets")) + "\n\n" + petsStr.toString().replace("\n\n", "\n")
					)
			);
		}
		if (!talismanStr.isEmpty()) {
			pages.put(
				SelectOption.of("Accessories", "accessories").withEmoji(getEmojiObj("MASTER_SKULL_TIER_7")),
				player
					.defaultPlayerEmbed(" | Accessories")
					.setDescription(
						ebDesc +
						"\n**Accessories:** " +
						simplifyNumber(getTotal("talisman")) +
						"\n\n" +
						talismanStr.toString().replace("\n\n", "\n")
					)
			);
		}
		if (!personalVaultStr.isEmpty()) {
			pages.put(
				SelectOption.of("Personal Vault", "personal_vault").withEmoji(getEmojiObj("IRON_CHEST")),
				player
					.defaultPlayerEmbed(" | Personal Vault")
					.setDescription(
						ebDesc +
						"\n**Personal Vault:** " +
						simplifyNumber(getTotal("personal_vault")) +
						"\n\n" +
						personalVaultStr.toString().replace("\n\n", "\n")
					)
			);
		}
		if (!sacksStr.isEmpty()) {
			pages.put(
				SelectOption.of("Sacks", "sacks").withEmoji(getEmojiObj("RUNE_SACK")),
				player
					.defaultPlayerEmbed(" | Sacks")
					.setDescription(
						ebDesc + "\n**Sacks:** " + simplifyNumber(getTotal("sacks")) + "\n\n" + sacksStr.toString().replace("\n\n", "\n")
					)
			);
		}

		//			JsonArray missing = collectJsonArray(
		//				tempSet.stream().filter(str -> !str.toLowerCase().startsWith("rune_")).map(JsonPrimitive::new)
		//			);
		//			if (!missing.isEmpty()) {
		//				System.out.println(missing);
		//			}

		String verboseLink = null;
		if (verbose) {
			verboseLink = makeHastePost(formattedGson.toJson(getVerboseJson()));
			extras.addButton(Button.link(verboseLink + ".json", "Verbose JSON"));
		}
		// Init last updated to 0
		extras.addButton(
			Button.danger(
				"nw_" + player.getUuid() + "_" + player.getProfileName() + "_0" + (verboseLink != null ? "_" + verboseLink : ""),
				"Report Incorrect Calculations"
			)
		);
		new SelectMenuPaginator(pages, "overview", extras, event);
		return null;
	}

	public StringBuilder getSectionString(List<String> items) {
		items.sort(Comparator.comparingDouble(item -> -Double.parseDouble(item.split("=:=")[1])));
		StringBuilder str = new StringBuilder();

		for (int i = 0; i < items.size(); i++) {
			String item = items.get(i);
			str.append(item.split("=:=")[0]).append(" ➜ ").append(simplifyNumber(Double.parseDouble(item.split("=:=")[1]))).append("\n");
			if (i == 4) {
				str.append("\n");
			} else if (i == 24) {
				int moreItems = items.size() - 25;
				if (moreItems > 0) {
					str.append("... ").append(moreItems).append(" more item").append(moreItems > 1 ? "s" : "");
				}
				break;
			}
		}

		return str;
	}

	public double getNetworth() {
		return totals.getOrDefault("inventory", -1.0) == -1 ? -1 : totals.values().stream().mapToDouble(i -> i).sum();
	}

	public void calculatePetPrice(String location, String auctionName, double auctionPrice) {
		List<InvItem> petsList = pets.getOrDefault(location, new ArrayList<>());

		for (Iterator<InvItem> iterator = petsList.iterator(); iterator.hasNext();) {
			InvItem item = iterator.next();
			if (item.getPetApiName().equals(auctionName)) {
				StringBuilder miscStr = new StringBuilder("[");
				double miscExtras = 0;
				try {
					Set<String> extraStats = item.getExtraStats().keySet();
					for (String extraItem : extraStats) {
						double miscPrice = getLowestPrice(extraItem);
						miscExtras += miscPrice;
						miscStr
							.append("{\"name\":\"")
							.append(extraItem)
							.append("\",\"price\":\"")
							.append(simplifyNumber(miscPrice))
							.append("\"},");
					}
				} catch (Exception ignored) {}
				if (miscStr.charAt(miscStr.length() - 1) == ',') {
					miscStr.deleteCharAt(miscStr.length() - 1);
				}
				miscStr.append("]");

				String itemStr = addItemStr(item, auctionPrice + miscExtras);
				double totalPrice = auctionPrice + miscExtras;
				addItem(location, itemStr);
				addTotal(location, totalPrice);

				if (verbose) {
					calcItemsJsonStr
						.append("{\"name\":\"")
						.append(item.getName())
						.append("\",\"rarity\":\"")
						.append(item.getRarity())
						.append("\",\"total\":\"")
						.append(simplifyNumber(auctionPrice + miscExtras))
						.append("\",\"base_cost\":\"")
						.append(simplifyNumber(auctionPrice))
						.append("\"")
						.append(
							miscExtras > 0 ? ",\"misc\":{\"total\":\"" + simplifyNumber(miscExtras) + "\",\"miscs\":" + miscStr + "}" : ""
						)
						.append("},");
				}

				iterator.remove();
			}
		}
	}

	public void calculateDefaultPetPrices(String location) {
		List<InvItem> petsList = pets.getOrDefault(location, new ArrayList<>());

		for (InvItem item : petsList) {
			double auctionPrice = getMinBinAvg(
				item.getName().split("] ")[1].toUpperCase().replace(" ", "_") + RARITY_TO_NUMBER_MAP.get(item.getRarity())
			);
			if (auctionPrice != -1) {
				StringBuilder miscStr = new StringBuilder("[");
				double miscExtras = 0;
				try {
					Set<String> extraStats = item.getExtraStats().keySet();
					for (String extraItem : extraStats) {
						double miscPrice = getLowestPrice(extraItem);
						miscExtras += miscPrice;
						miscStr
							.append("{\"name\":\"")
							.append(extraItem)
							.append("\",\"price\":\"")
							.append(simplifyNumber(miscPrice))
							.append("\"},");
					}
				} catch (Exception ignored) {}
				if (miscStr.charAt(miscStr.length() - 1) == ',') {
					miscStr.deleteCharAt(miscStr.length() - 1);
				}
				miscStr.append("]");

				addItem(location, addItemStr(item, auctionPrice + miscExtras));
				addTotal(location, auctionPrice + miscExtras);

				if (verbose) {
					calcItemsJsonStr
						.append("{\"name\":\"")
						.append(item.getName())
						.append("\",\"rarity\":\"")
						.append(item.getRarity())
						.append("\",\"total\":\"")
						.append(simplifyNumber(auctionPrice + miscExtras))
						.append("\",\"base_cost\":\"")
						.append(simplifyNumber(auctionPrice))
						.append("\",")
						.append(
							miscExtras > 0 ? "\"misc\":{\"total\":\"" + simplifyNumber(miscExtras) + "\",\"miscs\":" + miscStr + "}," : ""
						)
						.append("\"fail_find_lvl\":true},");
				}
			}
		}
	}

	public void calculatePetPrices() {
		String queryStr = pets.values().stream().flatMap(Collection::stream).map(InvItem::getPetApiName).collect(Collectors.joining(","));

		if (queryStr.isEmpty()) {
			return;
		}

		JsonArray ahQuery = getAuctionPetsByName(queryStr);
		if (ahQuery != null) {
			for (JsonElement auction : ahQuery) {
				String auctionName = higherDepth(auction, "name").getAsString();
				double auctionPrice = higherDepth(auction, "price").getAsDouble();

				for (String location : locations) {
					calculatePetPrice(location, auctionName, auctionPrice);
				}
			}
		}

		for (String location : locations) {
			calculateDefaultPetPrices(location);
		}
	}

	public double getMinBinAvg(String id) {
		return getMin(
			higherDepth(lowestBinJson, id, -1.0),
			getMin(higherDepth(averageAuctionJson, id + ".clean_price", -1.0), higherDepth(averageAuctionJson, id + ".price", -1.0))
		);
	}

	public String addItemStr(InvItem item, double itemPrice) {
		String emoji = higherDepth(getEmojiMap(), item.getFormattedId(), null);
		String formattedStr =
			(emoji == null ? "" : emoji + " ") +
			(item.getCount() != 1 ? item.getCount() + "x " : "") +
			(item.getId().equals("PET") ? capitalizeString(item.getPetRarity()) + " " : "") +
			item.getNameFormatted();

		if (item.getPetItem() != null) {
			String petItemEmoji = higherDepth(getEmojiMap(), item.getPetItem(), null);
			if (petItemEmoji != null) {
				formattedStr += " " + petItemEmoji;
			}
		}

		formattedStr += (item.isRecombobulated() ? " " + getEmoji("RECOMBOBULATOR_3000") : "") + "=:=" + itemPrice;

		return formattedStr;
	}

	public double calculateItemPrice(InvItem item) {
		return calculateItemPrice(item, null);
	}

	public double calculateItemPrice(InvItem item, String location) {
		return calculateItemPrice(item, location, calcItemsJsonStr);
	}

	public double calculateItemPrice(InvItem item, String location, StringBuilder out) {
		if (item == null || item.getName().equalsIgnoreCase("null") || item.getId().equals("None") || getPriceOverride(item.getId()) == 0) {
			return 0;
		}

		double itemCost = 0;
		double itemCount = 1;
		double recombobulatedExtra = 0;
		double hbpExtras = 0;
		double enchantsExtras = 0;
		double fumingExtras = 0;
		double reforgeExtras = 0;
		double miscExtras = 0;
		double backpackExtras = 0;
		double essenceExtras = 0;

		StringBuilder source = verbose ? new StringBuilder() : null;
		try {
			if (item.getId().equals("PET") && location != null) {
				if (!item.getName().startsWith("Mystery ") && !item.getName().equals("Unknown Pet")) {
					pets.compute(
						location,
						(k, v) -> {
							(v = (v == null ? new ArrayList<>() : v)).add(item);
							return v;
						}
					);
				}
				return -1;
			} else {
				if (item.getDarkAuctionPrice() != -1) {
					itemCost = item.getDarkAuctionPrice();
					if (verbose) {
						source.append("dark auction price paid");
					}
				} else {
					itemCost = getLowestPrice(item.getId().toUpperCase(), false, source);
				}
			}
		} catch (Exception ignored) {}

		try {
			itemCount = item.getCount();
		} catch (Exception ignored) {}

		try {
			if (item.isRecombobulated() && (ALL_TALISMANS.contains(item.getId()) || itemCost * 2 >= recombPrice)) {
				recombobulatedExtra = recombPrice;
			}
		} catch (Exception ignored) {}

		try {
			hbpExtras = item.getHbpCount() * hbpPrice;
		} catch (Exception ignored) {}

		try {
			fumingExtras = item.getFumingCount() * fumingPrice * 0.66;
		} catch (Exception ignored) {}

		StringBuilder enchStr = verbose ? new StringBuilder("[") : null;
		try {
			List<String> enchants = item.getEnchantsFormatted();
			for (String enchant : enchants) {
				try {
					if (item.getDungeonFloor() != -1 && enchant.equalsIgnoreCase("scavenger;5")) {
						continue;
					}

					double enchantPrice = getLowestPriceEnchant(enchant.toUpperCase());
					if (!item.getId().equals("ENCHANTED_BOOK")) {
						enchantPrice *= enchant.startsWith("ULTIMATE_SOUL_EATER") || enchant.startsWith("OVERLOAD") ? 0.40 : 0.90;
					}
					enchantsExtras += enchantPrice;
					if (verbose) {
						enchStr
							.append("{\"type\":\"")
							.append(enchant)
							.append("\",\"price\":\"")
							.append(simplifyNumber(enchantPrice))
							.append("\"},");
					}
				} catch (Exception ignored) {}
			}
		} catch (Exception ignored) {}
		if (verbose) {
			if (enchStr.charAt(enchStr.length() - 1) == ',') {
				enchStr.deleteCharAt(enchStr.length() - 1);
			}
			enchStr.append("]");
		}

		try {
			reforgeExtras = getLowestPriceModifier(item.getModifier(), item.getRarity());
		} catch (Exception ignored) {}

		try {
			essenceExtras =
				item.getEssenceCount() *
				higherDepth(bazaarJson, "ESSENCE_" + item.getEssenceType() + ".sell_summary.[0].pricePerUnit", 0.0);
		} catch (Exception ignored) {}

		StringBuilder miscStr = verbose ? new StringBuilder("[") : null;
		try {
			for (Map.Entry<String, Integer> extraItem : item.getExtraStats().entrySet()) {
				double miscPrice = getLowestPrice(extraItem.getKey());
				miscExtras += miscPrice * extraItem.getValue();
				if (verbose) {
					miscStr
						.append("{\"name\":\"")
						.append(extraItem.getKey())
						.append("\",\"total\":\"")
						.append(simplifyNumber(miscPrice * extraItem.getValue()))
						.append("\",\"count\":")
						.append(extraItem.getValue())
						.append(",\"cost\":\"")
						.append(simplifyNumber(miscPrice))
						.append("\"},");
				}
			}
		} catch (Exception ignored) {}
		if (verbose) {
			if (miscStr.charAt(miscStr.length() - 1) == ',') {
				miscStr.deleteCharAt(miscStr.length() - 1);
			}
			miscStr.append("]");
		}

		StringBuilder bpStr = verbose ? new StringBuilder("[") : null;
		try {
			List<InvItem> backpackItems = item.getBackpackItems();
			for (InvItem backpackItem : backpackItems) {
				backpackExtras += calculateItemPrice(backpackItem, location, bpStr);
			}
		} catch (Exception ignored) {}
		if (verbose) {
			if (bpStr.charAt(bpStr.length() - 1) == ',') {
				bpStr.deleteCharAt(bpStr.length() - 1);
			}
			bpStr.append("]");
		}

		double totalPrice =
			itemCount *
			(itemCost + recombobulatedExtra + hbpExtras + enchantsExtras + fumingExtras + reforgeExtras + miscExtras + backpackExtras);

		if (verbose) {
			out.append("{");
			out.append("\"name\":\"").append(item.getName()).append("\"");
			out.append(",\"id\":\"").append(item.getId()).append("\"");
			out.append(",\"total\":\"").append(simplifyNumber(totalPrice)).append("\"");
			out.append(",\"count\":").append(itemCount);
			out
				.append(",\"base_cost\":{\"cost\":\"")
				.append(simplifyNumber(itemCost))
				.append("\",\"location\":\"")
				.append(source)
				.append("\"}");
			out.append(recombobulatedExtra > 0 ? ",\"recomb\":\"" + simplifyNumber(recombobulatedExtra) + "\"" : "");
			out.append(
				essenceExtras > 0
					? ",\"essence\":{\"total\":\"" +
					simplifyNumber(essenceExtras) +
					"\",\"amount\":" +
					item.getEssenceCount() +
					",\"type\":\"" +
					item.getEssenceType() +
					"\"}"
					: ""
			);
			out.append(hbpExtras > 0 ? ",\"hbp\":\"" + simplifyNumber(hbpExtras) + "\"" : "");
			out.append(
				enchantsExtras > 0
					? ",\"enchants\":{\"total\":\"" + simplifyNumber(enchantsExtras) + "\",\"enchants\":" + enchStr + "}"
					: ""
			);
			out.append(fumingExtras > 0 ? ",\"fuming\":\"" + simplifyNumber(fumingExtras) + "\"" : "");
			out.append(
				reforgeExtras > 0
					? ",\"reforge\":{\"cost\":\"" + simplifyNumber(reforgeExtras) + "\",\"name\":\"" + item.getModifier() + "\"}"
					: ""
			);
			out.append(miscExtras > 0 ? ",\"misc\":{\"total\":\"" + simplifyNumber(miscExtras) + "\",\"miscs\":" + miscStr + "}" : "");
			out.append(backpackExtras > 0 ? ",\"bp\":{\"cost\":\"" + simplifyNumber(backpackExtras) + "\",\"bp\":" + bpStr + "}" : "");
			out.append(",\"nbt_tag\":\"").append(parseMcCodes(item.getNbtTag().toString().replace("\"", "\\\""))).append("\"");
			out.append("},");
		}

		return totalPrice;
	}

	public double getLowestPriceModifier(String reforgeName, String itemRarity) {
		JsonElement reforgesStonesJson = getReforgeStonesJson();

		for (String reforgeStone : REFORGE_STONE_NAMES) {
			JsonElement reforgeStoneInfo = higherDepth(reforgesStonesJson, reforgeStone);
			if (
				higherDepth(reforgeStoneInfo, "reforgeName").getAsString().equalsIgnoreCase(reforgeName) ||
				reforgeStone.equalsIgnoreCase(reforgeName)
			) {
				String reforgeStoneId = higherDepth(reforgeStoneInfo, "internalName").getAsString();
				double reforgeStoneCost = getLowestPrice(reforgeStoneId);
				double reforgeApplyCost = higherDepth(reforgeStoneInfo, "reforgeCosts." + itemRarity.toUpperCase()).getAsLong();
				return reforgeStoneCost + reforgeApplyCost;
			}
		}

		return 0;
	}

	public double getLowestPriceEnchant(String enchantId) {
		String enchantName = enchantId.split(";")[0];
		int enchantLevel = Integer.parseInt(enchantId.split(";")[1]);

		if (enchantLevel > 10) { // Admin enchant
			return 0;
		}

		int ignoredLevels = IGNORED_ENCHANTS.getOrDefault(enchantName, 0);
		if (enchantLevel <= ignoredLevels) {
			return 0;
		}

		if (
			enchantName.equalsIgnoreCase("compact") ||
			enchantName.equalsIgnoreCase("expertise") ||
			enchantName.equalsIgnoreCase("cultivating") ||
			enchantName.equalsIgnoreCase("champion") ||
			enchantName.equalsIgnoreCase("hecatomb")
		) {
			enchantLevel = 1;
		}

		try {
			return higherDepth(extraPrices, "enchantment_" + enchantName.toLowerCase() + "_" + enchantLevel).getAsDouble();
		} catch (Exception ignored) {}

		for (int i = enchantLevel; i >= Math.max(1, ignoredLevels); i--) {
			try {
				return (
					Math.pow(2, enchantLevel - i) *
					higherDepth(bazaarJson, enchantName + ";" + i + ".buy_summary.[0].pricePerUnit").getAsDouble()
				);
			} catch (Exception ignored) {}
		}

		//		tempSet.add(enchantId);
		return 0;
	}

	public double getLowestPrice(String itemId) {
		return getLowestPrice(itemId, false, null);
	}

	public double getLowestPrice(String itemId, boolean ignoreAh, StringBuilder source) {
		double priceOverride = getPriceOverride(itemId);
		if (priceOverride != -1) {
			if (source != null) {
				source.append("price override");
			}
			return priceOverride;
		}

		if (itemId.equals("SKYBLOCK_COIN")) {
			return 1; // 1 * count
		}

		if (itemId.contains("GENERATOR")) {
			if (source != null) {
				source.append("craft");
			}
			int index = itemId.lastIndexOf("_");
			return getMinionCost(itemId.substring(0, index), Integer.parseInt(itemId.substring(index + 1)));
		}

		List<String> recipe = getRecipe(itemId);
		double craftCost = 0;
		if (recipe != null) {
			for (String item : recipe) {
				String[] idCountSplit = item.split(":");
				craftCost += getLowestPrice(idCountSplit[0].replace("-", ":")) * Integer.parseInt(idCountSplit[1]);
			}
		}

		try {
			double bazaarPrice = higherDepth(bazaarJson, itemId + ".sell_summary.[0].pricePerUnit").getAsDouble();
			if (recipe == null || bazaarPrice <= craftCost) {
				if (source != null) {
					source.append("bazaar");
				}
				return bazaarPrice;
			}
		} catch (Exception ignored) {}

		if (!ignoreAh) {
			double lowestBin = -1;
			double averageAuction = -1;

			try {
				lowestBin = higherDepth(lowestBinJson, itemId).getAsDouble();
			} catch (Exception ignored) {}

			try {
				averageAuction =
					getMin(
						higherDepth(averageAuctionJson, itemId + ".clean_price", -1.0),
						higherDepth(averageAuctionJson, itemId + ".price", -1.0)
					);
			} catch (Exception ignored) {}

			double minBinAverage = getMin(lowestBin, averageAuction);
			if (minBinAverage != -1 && (recipe == null || minBinAverage <= craftCost)) {
				if (source != null) {
					if (minBinAverage == lowestBin) {
						source.append("lowest BIN");
					} else {
						source.append("average auction");
					}
				}
				return minBinAverage;
			}
		}

		if (recipe != null) {
			if (source != null) {
				if (higherDepth(getInternalJsonMappings(), itemId + ".recipe") != null) {
					source.append("craft");
				} else {
					source.append("npc buy");
				}
			}
			return craftCost;
		}

		double npcPrice = getNpcSellPrice(itemId);
		if (source != null && npcPrice != -1) {
			source.append("npc sell");
			return npcPrice;
		}

		//		tempSet.add(itemId + " - " + iName);
		return 0;
	}

	public double getMinionCost(String id, int tier) {
		return getMinionCost(id, tier, -1);
	}

	public double getMinionCost(String id, int tier, int depth) {
		double priceOverride = getPriceOverride(id + "_" + tier);
		if (priceOverride != -1) {
			return priceOverride;
		}

		List<String> recipe = getRecipe(id + "_" + tier);
		if (recipe == null) {
			return 0;
		}

		double cost = 0;
		for (String material : recipe) {
			String[] idCountSplit = material.split(":");
			if (idCountSplit[0].contains("GENERATOR")) {
				if (depth - 1 != 0) {
					cost += getMinionCost(idCountSplit[0].substring(0, idCountSplit[0].lastIndexOf("_")), tier - 1, depth - 1);
				}
			} else {
				cost += getLowestPrice(idCountSplit[0].replace("-", ":")) * Integer.parseInt(idCountSplit[1]);
			}
		}
		return cost;
	}

	public JsonElement getVerboseJson() {
		if (!verbose) {
			return null;
		}

		if (calcItemsJsonStr.charAt(calcItemsJsonStr.length() - 1) == ',') {
			calcItemsJsonStr.deleteCharAt(calcItemsJsonStr.length() - 1);
		}
		calcItemsJsonStr.append("]");

		return JsonParser.parseString(calcItemsJsonStr.toString());
	}

	public void resetVerboseJson() {
		calcItemsJsonStr = new StringBuilder("[");
	}

	public void addTotal(String location, double total) {
		totals.compute(location.equals("equipment") ? "armor" : location, (k, v) -> (v == null ? 0 : v) + total);
	}

	public void addItem(String location, String item) {
		items.compute(
			location.equals("equipment") ? "armor" : location,
			(k, v) -> {
				(v = (v == null ? new ArrayList<>() : v)).add(item);
				return v;
			}
		);
	}

	public double getTotal(String location) {
		return totals.getOrDefault(location, 0.0);
	}

	public List<String> getItems(String location) {
		return items.getOrDefault(location, new ArrayList<>());
	}
}
