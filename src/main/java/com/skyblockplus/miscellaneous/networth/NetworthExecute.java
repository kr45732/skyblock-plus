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

package com.skyblockplus.miscellaneous.networth;

import static com.skyblockplus.utils.ApiHandler.getAuctionPetsByName;
import static com.skyblockplus.utils.Constants.*;
import static com.skyblockplus.utils.utils.HypixelUtils.*;
import static com.skyblockplus.utils.utils.JsonUtils.*;
import static com.skyblockplus.utils.utils.StringUtils.*;
import static com.skyblockplus.utils.utils.Utils.*;

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
import java.util.stream.Stream;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.GenericInteractionCreateEvent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.selections.SelectOption;
import org.apache.groovy.util.Maps;

public class NetworthExecute {

	private static final List<String> allowedRecombCategories = List.of("ACCESSORY", "NECKLACE", "GLOVES", "BRACELET", "BELT", "CLOAK");
	private static final Map<String, String> attributesBaseCosts = Maps.of(
		"GLOWSTONE_GAUNTLET",
		"GLOWSTONE_GAUNTLET",
		"VANQUISHED_GLOWSTONE_GAUNTLET",
		"GLOWSTONE_GAUNTLET",
		"BLAZE_BELT",
		"BLAZE_BELT",
		"VANQUISHED_BLAZE_BELT",
		"BLAZE_BELT",
		"MAGMA_NECKLACE",
		"MAGMA_NECKLACE",
		"VANQUISHED_MAGMA_NECKLACE",
		"MAGMA_NECKLACE",
		"MAGMA_ROD",
		"MAGMA_ROD",
		"INFERNO_ROD",
		"MAGMA_ROD",
		"HELLFIRE_ROD",
		"MAGMA_ROD"
	);
	private static final Map<String, Double> enchantWorth = Maps.of(
		"COUNTER_STRIKE",
		0.25,
		"BIG_BRAIN",
		0.40,
		"ULTIMATE_INFERNO",
		0.40,
		"OVERLOAD",
		0.40,
		"ULTIMATE_SOUL_EATER",
		0.40,
		"ULTIMATE_FATAL_TEMPO",
		0.7
	);
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
	private final Map<String, List<String>> soulboundIgnoredItems = new HashMap<>();
	private final Map<String, Double> totals = new HashMap<>();
	private final Map<String, Double> soulboundIgnoredTotals = new HashMap<>();
	private StringBuilder calcItemsJsonStr = new StringBuilder("[");
	private JsonElement lowestBinJson;
	private JsonElement averageAuctionJson;
	private JsonElement bazaarJson;
	private JsonObject extraPrices;
	private double recombPrice;
	private double fumingPrice;
	private double hpbPrice;
	private boolean verbose = false;

	public static double getNetworth(String username, String profileName) {
		NetworthExecute calc = new NetworthExecute();
		calc.getPlayerNetworth(username, profileName, null);
		return calc.getNetworth();
	}

	public static double getNetworth(Player.Profile player) {
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

		recombPrice = higherDepth(bazaarJson, "RECOMBOBULATOR_3000.sell_summary", 0.0);
		hpbPrice = higherDepth(bazaarJson, "HOT_POTATO_BOOK.sell_summary", 0.0);
		fumingPrice = higherDepth(bazaarJson, "FUMING_POTATO_BOOK.sell_summary", 0.0);
		return this;
	}

	public EmbedBuilder getPlayerNetworth(String username, String profileName, GenericInteractionCreateEvent event) {
		return getPlayerNetworth(Player.create(username, profileName), event);
	}

	public EmbedBuilder getPlayerNetworth(Player.Profile player, GenericInteractionCreateEvent event) {
		if (!player.isValid()) {
			return player.getErrorEmbed();
		}

		Map<Integer, InvItem> playerInventory = player.getInventoryMap();
		if (playerInventory == null) {
			addTotal("inventory", -1.0);
			return defaultEmbed(player.getUsernameFixed() + "'s inventory API is disabled");
		}

		initPrices();

		addTotal("bank", player.getBankBalance());
		addTotal("purse", player.getPurseCoins());

		for (String essence : essenceTypes) {
			addTotal(
				"essence",
				getLowestPrice("ESSENCE_" + essence.toUpperCase()) * higherDepth(player.profileJson(), "essence_" + essence, 0)
			);
		}

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
							// If event is null, no need to update the soulbound fields even if soulbound
							addTotal(location, itemPrice, event != null && item.isSoulbound());
							if (event != null) {
								addItem(location, addItemStr(item, itemPrice), item.isSoulbound());
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
					double itemPrice = getLowestPrice(sackEntry.getKey(), true, null, false) * sackEntry.getValue();
					addTotal("sacks", itemPrice);
					if (event != null) {
						String emoji = getEmoji(sackEntry.getKey(), null);
						addItem(
							"sacks",
							(emoji == null ? "" : emoji + " ") +
							(sackEntry.getValue() != 1 ? formatNumber(sackEntry.getValue()) + "x " : "") +
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
			return errorEmbed("Was not triggered by command");
		}

		player.getProfileToNetworth().put(player.getProfileIndex(), getNetworth());

		PaginatorExtras extras = new PaginatorExtras(PaginatorExtras.PaginatorType.EMBED_PAGES);
		Map<SelectOption, EmbedBuilder> pages = getPages(player, false);
		Map<SelectOption, EmbedBuilder> soulboundIgnoredPages = getPages(player, true);

		String verboseLink = null;
		if (verbose) {
			verboseLink = makeHastePost(formattedGson.toJson(getVerboseJson()));
			extras.addButton(Button.link(verboseLink, "Verbose JSON"));
		}
		// Init last updated to 0
		extras
			.addButton(
				Button.danger(
					"nw_" +
					player.getUuid() +
					"_" +
					player.getProfileName() +
					"_0" +
					(verboseLink != null ? "_" + verboseLink.split(getHasteUrl())[1] : ""),
					"Report Incorrect Calculations"
				)
			)
			.setSelectPages(pages)
			.addReactiveButtons(
				new PaginatorExtras.ReactiveButton(
					Button.primary("reactive_nw_ignore_soulbound", "Hide Soulbound"),
					paginator ->
						extras
							.setSelectPages(soulboundIgnoredPages)
							.toggleReactiveButton("reactive_nw_ignore_soulbound", false)
							.toggleReactiveButton("reactive_nw_show_soulbound", true),
					true
				),
				new PaginatorExtras.ReactiveButton(
					Button.primary("reactive_nw_show_soulbound", "Show Soulbound"),
					paginator ->
						extras
							.setSelectPages(pages)
							.toggleReactiveButton("reactive_nw_ignore_soulbound", true)
							.toggleReactiveButton("reactive_nw_show_soulbound", false),
					false
				)
			);
		new SelectMenuPaginator("overview", extras, event);
		return null;
	}

	private Map<SelectOption, EmbedBuilder> getPages(Player.Profile player, boolean ignoreSoulbound) {
		StringBuilder echestStr = getSectionString(getItems("enderchest", ignoreSoulbound));
		StringBuilder sacksStr = getSectionString(getItems("sacks", ignoreSoulbound));
		StringBuilder personalVaultStr = getSectionString(getItems("personal_vault", ignoreSoulbound));
		StringBuilder storageStr = getSectionString(getItems("storage", ignoreSoulbound));
		StringBuilder invStr = getSectionString(getItems("inventory", ignoreSoulbound));
		StringBuilder armorStr = getSectionString(getItems("armor", ignoreSoulbound));
		StringBuilder petsStr = getSectionString(getItems("pets", ignoreSoulbound));
		StringBuilder talismanStr = getSectionString(getItems("talisman", ignoreSoulbound));

		double totalNetworth = getNetworth(ignoreSoulbound);
		//			int position = leaderboardDatabase.getNetworthPosition(player.getGamemode(), player.getUuid());
		String ebDesc = "**Total Networth:** " + simplifyNumber(totalNetworth) + " (" + roundAndFormat(totalNetworth) + ")";
		//			\n**" +
		//				(
		//					Player.Gamemode.IRONMAN_STRANDED.isGamemode(player.getGamemode())
		//						? capitalizeString(player.getGamemode().toString()) + " "
		//						: ""
		//				)+"Leaderboard Position:** " + (position != -1 ? formatNumber(position) : "Not on leaderboard");
		EmbedBuilder eb = player.defaultPlayerEmbed().setDescription(ebDesc);
		eb.addField(
			"Purse & Bank",
			simplifyNumber(getTotal("purse", ignoreSoulbound)) +
			" & " +
			(getTotal("bank", ignoreSoulbound) == -1 ? "Private" : simplifyNumber(getTotal("bank", ignoreSoulbound))),
			true
		);
		eb.addField("Sacks", simplifyNumber(getTotal("sacks", ignoreSoulbound)), true);
		eb.addField("Essence", simplifyNumber(getTotal("essence", ignoreSoulbound)), true);
		if (!echestStr.isEmpty()) {
			eb.addField(
				"Ender Chest | " + simplifyNumber(getTotal("enderchest", ignoreSoulbound)),
				echestStr.toString().split("\n\n")[0],
				false
			);
		}
		if (!storageStr.isEmpty()) {
			eb.addField("Storage | " + simplifyNumber(getTotal("storage", ignoreSoulbound)), storageStr.toString().split("\n\n")[0], false);
		}
		if (!invStr.isEmpty()) {
			eb.addField("Inventory | " + simplifyNumber(getTotal("inventory", ignoreSoulbound)), invStr.toString().split("\n\n")[0], false);
		}
		if (!armorStr.isEmpty()) {
			eb.addField("Armor | " + simplifyNumber(getTotal("armor", ignoreSoulbound)), armorStr.toString().split("\n\n")[0], false);
		}
		if (!petsStr.isEmpty()) {
			eb.addField("Pets | " + simplifyNumber(getTotal("pets", ignoreSoulbound)), petsStr.toString().split("\n\n")[0], false);
		}
		if (!talismanStr.isEmpty()) {
			eb.addField(
				"Accessories | " + simplifyNumber(getTotal("talisman", ignoreSoulbound)),
				talismanStr.toString().split("\n\n")[0],
				false
			);
		}
		if (!personalVaultStr.isEmpty()) {
			eb.addField(
				"Personal Vault | " + simplifyNumber(getTotal("personal_vault", ignoreSoulbound)),
				personalVaultStr.toString().split("\n\n")[0],
				false
			);
		}

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
						simplifyNumber(getTotal("enderchest", ignoreSoulbound)) +
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
						simplifyNumber(getTotal("storage", ignoreSoulbound)) +
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
						simplifyNumber(getTotal("inventory", ignoreSoulbound)) +
						"\n\n" +
						invStr.toString().replace("\n\n", "\n")
					)
			);
		}
		if (!armorStr.isEmpty()) {
			pages.put(
				SelectOption.of("Armor", "armor").withEmoji(getEmojiObj("GOLD_CHESTPLATE")),
				player
					.defaultPlayerEmbed(" | Armor")
					.setDescription(
						ebDesc +
						"\n**Armor:** " +
						simplifyNumber(getTotal("armor", ignoreSoulbound)) +
						"\n\n" +
						armorStr.toString().replace("\n\n", "\n")
					)
			);
		}
		if (!petsStr.isEmpty()) {
			pages.put(
				SelectOption.of("Pets", "pets").withEmoji(getEmojiObj("ENDER_DRAGON;4")),
				player
					.defaultPlayerEmbed(" | Pets")
					.setDescription(
						ebDesc +
						"\n**Pets:** " +
						simplifyNumber(getTotal("pets", ignoreSoulbound)) +
						"\n\n" +
						petsStr.toString().replace("\n\n", "\n")
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
						simplifyNumber(getTotal("talisman", ignoreSoulbound)) +
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
						simplifyNumber(getTotal("personal_vault", ignoreSoulbound)) +
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
						ebDesc +
						"\n**Sacks:** " +
						simplifyNumber(getTotal("sacks", ignoreSoulbound)) +
						"\n\n" +
						sacksStr.toString().replace("\n\n", "\n")
					)
			);
		}

		return pages;
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
		return getNetworth(false);
	}

	public double getNetworth(boolean ignoreSoulbound) {
		return totals.getOrDefault("inventory", -1.0) == -1
			? -1
			: (ignoreSoulbound ? soulboundIgnoredTotals : totals).values().stream().mapToDouble(i -> i).sum();
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
						if (Objects.equals(extraItem, item.getPetItem()) && item.isTierBoosted()) {
							continue;
						}

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
							miscExtras > 0 ? ",\"misc\":{\"total\":\"" + simplifyNumber(miscExtras) + "\",\"data\":" + miscStr + "}" : ""
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
							miscExtras > 0 ? "\"misc\":{\"total\":\"" + simplifyNumber(miscExtras) + "\",\"data\":" + miscStr + "}," : ""
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
		String emoji = getEmoji(item.getFormattedId(), null);

		if (item.getSkin() != null && getEmoji(item.getSkin(), null) != null) {
			emoji = getEmoji(item.getSkin(), null);
		}

		String formattedStr =
			(emoji == null ? "" : emoji + " ") +
			(item.getCount() != 1 ? formatNumber(item.getCount()) + "x " : "") +
			(item.getId().equals("PET") ? capitalizeString(item.getPetRarity()) + " " : "") +
			item.getNameFormatted();

		if (item.getPetItem() != null) {
			String petItemEmoji = getEmoji(item.getPetItem(), null);
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
		double hpbExtras = 0;
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
					long maxBid = item.getId().equals("MIDAS_SWORD") ? 50000000 : 100000000;

					if (item.getDarkAuctionPrice() >= maxBid) {
						itemCost = getLowestPrice(item.getId() + "_" + maxBid, false, source, false);
						if (itemCost == 0) {
							itemCost = getLowestPrice(item.getId(), false, source, false);
						}
					} else {
						itemCost = item.getDarkAuctionPrice();
					}

					if (verbose) {
						source.append("dark auction price paid");
					}
				} else {
					itemCost = getLowestPrice(item.getId().toUpperCase(), false, source, false);

					if (item.getShensAuctionPrice() != -1 && item.getShensAuctionPrice() * 0.9 > itemCost) {
						itemCost = item.getShensAuctionPrice() * 0.9;
						if (verbose) {
							source.setLength(0);
							source.append("shens auction price paid");
						}
					}
				}
			}
		} catch (Exception ignored) {}

		try {
			itemCount = item.getCount();
		} catch (Exception ignored) {}

		try {
			if (
				item.isRecombobulated() &&
				item.getDungeonFloor() == -1 &&
				(
					!item.getEnchantsFormatted().isEmpty() ||
					allowedRecombCategories.contains(higherDepth(getSkyblockItemsJson().get(item.getId()), "category", ""))
				)
			) {
				recombobulatedExtra = recombPrice * 0.9;
			}
		} catch (Exception ignored) {}

		try {
			hpbExtras = item.getHpbCount() * hpbPrice;
		} catch (Exception ignored) {}

		try {
			fumingExtras = item.getFumingCount() * fumingPrice * 0.7;
		} catch (Exception ignored) {}

		StringBuilder enchStr = verbose ? new StringBuilder("{") : null;
		try {
			List<String> enchants = item.getEnchantsFormatted();
			for (String enchant : enchants) {
				try {
					if (!item.getId().equals("ENCHANTED_BOOK") && enchant.equalsIgnoreCase("SCAVENGER;5")) {
						continue;
					}

					double enchantPrice = getLowestPriceEnchant(enchant.toUpperCase());
					if (!item.getId().equals("ENCHANTED_BOOK")) {
						enchantPrice *= enchantWorth.getOrDefault(enchant.split(";")[0], 0.9);
					}
					enchantsExtras += enchantPrice;
					if (verbose) {
						enchStr.append("\"").append(enchant).append("\":\"").append(simplifyNumber(enchantPrice)).append("\",");
					}
				} catch (Exception ignored) {}
			}
		} catch (Exception ignored) {}
		if (verbose) {
			if (enchStr.charAt(enchStr.length() - 1) == ',') {
				enchStr.deleteCharAt(enchStr.length() - 1);
			}
			enchStr.append("}");
		}

		try {
			reforgeExtras = getLowestPriceModifier(item.getModifier(), item.getRarity());
		} catch (Exception ignored) {}

		try {
			essenceExtras =
				item.getEssenceCount() * higherDepth(bazaarJson, "ESSENCE_" + item.getEssenceType() + ".sell_summary", 0.0) * 0.9;
		} catch (Exception ignored) {}

		StringBuilder miscStr = verbose ? new StringBuilder("[") : null;
		try {
			for (Map.Entry<String, Integer> extraItem : item.getExtraStats().entrySet()) {
				double miscPrice = 0;

				if (extraItem.getKey().startsWith("ATTRIBUTE_SHARD_")) {
					if (extraItem.getValue() == 1) {
						continue;
					}

					double baseAttributePrice = getLowestPrice(extraItem.getKey());
					if (attributesBaseCosts.containsKey(item.getId())) {
						double calcBasePrice = getLowestPrice(attributesBaseCosts.get(item.getId()));
						if (calcBasePrice > 0) {
							baseAttributePrice = Math.min(baseAttributePrice, calcBasePrice);
						}
					} else if (
						item
							.getId()
							.matches(
								"(|HOT_|BURNING_|FIERY_|INFERNAL_)(CRIMSON|FERVOR|HOLLOW|TERROR|AURORA)_(HELMET|CHESTPLATE|LEGGINGS|BOOTS)"
							)
					) {
						double calcBasePrice = Stream
							.of("HELMET", "CHESTPLATE", "LEGGINGS", "BOOTS")
							.map(a ->
								higherDepth(
									extraPrices,
									("KUUDRA_" + a + "_" + extraItem.getKey().split("ATTRIBUTE_SHARD_")[1]).toLowerCase()
								)
							)
							.filter(Objects::nonNull)
							.mapToDouble(JsonElement::getAsDouble)
							.average()
							.orElse(0);
						if (calcBasePrice > 0) {
							baseAttributePrice = Math.min(baseAttributePrice, calcBasePrice);
						}
					}

					if (baseAttributePrice > 0) {
						miscPrice = baseAttributePrice;
					}
				}
				if (miscPrice == 0) {
					miscPrice = getLowestPrice(extraItem.getKey());
				}
				miscPrice *= 0.95;

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
			(
				itemCost +
				recombobulatedExtra +
				hpbExtras +
				enchantsExtras +
				fumingExtras +
				reforgeExtras +
				miscExtras +
				backpackExtras +
				essenceExtras
			);

		if (verbose) {
			out.append("{");
			out.append("\"name\":\"").append(item.getName()).append("\"");
			out.append(",\"id\":\"").append(item.getId()).append("\"");
			out.append(",\"total\":\"").append(simplifyNumber(totalPrice)).append("\"");
			out.append(",\"count\":").append(itemCount);
			out
				.append(",\"base_cost\":{\"cost\":\"")
				.append(simplifyNumber(itemCost))
				.append("\",\"source\":\"")
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
			out.append(hpbExtras > 0 ? ",\"hpb\":\"" + simplifyNumber(hpbExtras) + "\"" : "");
			out.append(
				enchantsExtras > 0 ? ",\"enchants\":{\"total\":\"" + simplifyNumber(enchantsExtras) + "\",\"data\":" + enchStr + "}" : ""
			);
			out.append(fumingExtras > 0 ? ",\"fuming\":\"" + simplifyNumber(fumingExtras) + "\"" : "");
			out.append(
				reforgeExtras > 0
					? ",\"reforge\":{\"cost\":\"" + simplifyNumber(reforgeExtras) + "\",\"name\":\"" + item.getModifier() + "\"}"
					: ""
			);
			out.append(miscExtras > 0 ? ",\"misc\":{\"total\":\"" + simplifyNumber(miscExtras) + "\",\"data\":" + miscStr + "}" : "");
			out.append(backpackExtras > 0 ? ",\"bp\":{\"cost\":\"" + simplifyNumber(backpackExtras) + "\",\"data\":" + bpStr + "}" : "");
			out.append(",\"nbt_tag\":\"").append(parseMcCodes(item.getNbtTag().toString().replace("\"", "\\\""))).append("\"");
			out.append("},");
		}

		return totalPrice;
	}

	public double getLowestPriceModifier(String reforgeName, String itemRarity) {
		for (Map.Entry<String, JsonElement> reforge : getReforgeStonesJson().entrySet()) {
			if (
				higherDepth(reforge.getValue(), "reforgeName").getAsString().equalsIgnoreCase(reforgeName) ||
				reforge.getKey().equalsIgnoreCase(reforgeName)
			) {
				String reforgeStoneId = higherDepth(reforge.getValue(), "internalName").getAsString();
				double reforgeStoneCost = getLowestPrice(reforgeStoneId);
				double reforgeApplyCost = higherDepth(reforge.getValue(), "reforgeCosts." + itemRarity.toUpperCase()).getAsLong();
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
				return (Math.pow(2, enchantLevel - i) * higherDepth(bazaarJson, enchantName + ";" + i + ".buy_summary").getAsDouble());
			} catch (Exception ignored) {}
		}

		//		tempSet.add(enchantId);
		return 0;
	}

	public double getLowestPrice(String itemId) {
		return getLowestPrice(itemId, false, null, false);
	}

	public double getLowestPrice(String itemId, boolean ignoreAh, StringBuilder source, boolean onlyFullCraft) {
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

		if (higherDepth(bazaarJson, itemId) != null) {
			double bazaarPrice = higherDepth(bazaarJson, itemId + ".sell_summary", 0.0);
			if (source != null) {
				source.append("bazaar");
			}
			return bazaarPrice;
		}

		List<String> recipe = getRecipe(itemId);
		double craftCost = 0;
		if (recipe != null) {
			for (String item : recipe) {
				String[] idCountSplit = item.split(":");

				double itemLowestPrice = getLowestPrice(idCountSplit[0].replace("-", ":"), false, null, true);
				if (itemLowestPrice == 0) {
					craftCost = 0;
					break;
				}
				craftCost += itemLowestPrice * Integer.parseInt(idCountSplit[1]);
			}
			craftCost /= getRecipeCount(itemId);
		}

		if (!ignoreAh && !SOULBOUND_ITEMS.contains(itemId)) {
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
			if (minBinAverage != -1 && (craftCost == 0 || minBinAverage <= craftCost)) {
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

		if (!onlyFullCraft) {
			if (recipe != null) {
				double partialCraftCost = 0;
				for (String item : recipe) {
					String[] idCountSplit = item.split(":");
					partialCraftCost += getLowestPrice(idCountSplit[0].replace("-", ":")) * Integer.parseInt(idCountSplit[1]);
				}
				partialCraftCost /= getRecipeCount(itemId);

				if (partialCraftCost > 0) {
					if (source != null) {
						if (higherDepth(getInternalJsonMappings(), itemId + ".recipe") != null) {
							source.append("craft");
						} else {
							source.append("npc buy");
						}
					}
					return partialCraftCost;
				}
			}

			double npcPrice = getNpcSellPrice(itemId);
			if (npcPrice != -1) {
				if (source != null) {
					source.append("npc sell");
				}
				return npcPrice;
			}
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
		addTotal(location, total, false);
	}

	public void addTotal(String location, double total, boolean isSoulbound) {
		location = location.equals("equipment") || location.equals("wardrobe") ? "armor" : location;
		totals.compute(location, (k, v) -> (v == null ? 0 : v) + total);
		if (!isSoulbound) {
			soulboundIgnoredTotals.compute(location, (k, v) -> (v == null ? 0 : v) + total);
		}
	}

	public void addItem(String location, String item) {
		addItem(location, item, false);
	}

	public void addItem(String location, String item, boolean isSoulbound) {
		location = location.equals("equipment") || location.equals("wardrobe") ? "armor" : location;
		items.compute(
			location,
			(k, v) -> {
				(v = (v == null ? new ArrayList<>() : v)).add(item);
				return v;
			}
		);
		if (!isSoulbound) {
			soulboundIgnoredItems.compute(
				location,
				(k, v) -> {
					(v = (v == null ? new ArrayList<>() : v)).add(item);
					return v;
				}
			);
		}
	}

	public double getTotal(String location, boolean ignoreSoulbound) {
		return (ignoreSoulbound ? soulboundIgnoredTotals : totals).getOrDefault(location, 0.0);
	}

	public List<String> getItems(String location, boolean ignoreSoulbound) {
		return (ignoreSoulbound ? soulboundIgnoredItems : items).getOrDefault(location, new ArrayList<>());
	}
}
