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

import static com.skyblockplus.utils.ApiHandler.*;
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
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.GenericInteractionCreateEvent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.selections.SelectOption;
import net.dv8tion.jda.api.utils.messages.MessageEditBuilder;
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
		"storage",
		"museum"
	);
	//	private final Set<String> tempSet = new HashSet<>();
	private final List<InvItem> pets = new ArrayList<>();
	private final Map<String, List<NetworthItem>> items = new HashMap<>();
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

	public NetworthExecute setVerbose(boolean verbose) {
		this.verbose = verbose;
		return this;
	}

	public NetworthExecute initPrices() {
		lowestBinJson = getLowestBinJson();
		averageAuctionJson = getAveragePriceJson();
		bazaarJson = getBazaarJson();
		extraPrices = getExtraPricesJson();

		recombPrice = higherDepth(bazaarJson, "RECOMBOBULATOR_3000.sell_summary", 0.0);
		hpbPrice = higherDepth(bazaarJson, "HOT_POTATO_BOOK.sell_summary", 0.0);
		fumingPrice = higherDepth(bazaarJson, "FUMING_POTATO_BOOK.sell_summary", 0.0);
		return this;
	}

	public MessageEditBuilder getPlayerNetworth(String username, String profileName, GenericInteractionCreateEvent event) {
		return getPlayerNetworth(Player.create(username, profileName), event);
	}

	public MessageEditBuilder getPlayerNetworth(Player.Profile player, GenericInteractionCreateEvent event) {
		if (!player.isValid()) {
			return new MessageEditBuilder().setEmbeds(player.getErrorEmbed().build());
		}

		Map<Integer, InvItem> playerInventory = player.getInventoryMap();
		if (playerInventory == null) {
			addTotal("items", -1.0);
			return withApiHelpButton(defaultEmbed(player.getEscapedUsername() + "'s inventory API is disabled"));
		}

		Future<Integer> networthPositionFuture = null;
		if (event != null) {
			networthPositionFuture = leaderboardDatabase.getNetworthPosition(player.getGamemode(), player.getUuid());
		}

		initPrices();

		addTotal("bank", player.getBankBalance());
		addTotal("purse", player.getPurseCoins());

		for (String essence : essenceTypes) {
			addTotal(
				"essence",
				getLowestPrice("ESSENCE_" + essence.toUpperCase()) *
				higherDepth(player.profileJson(), "currencies.essence." + essence.toUpperCase() + ".current", 0)
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
					case "museum" -> player.getMuseumMap();
					default -> throw new IllegalStateException("Unexpected value: " + location);
				};

			if (curItems != null) {
				for (InvItem item : curItems.values()) {
					if (item != null) {
						double itemPrice = calculateItemPrice(item, location);
						if (itemPrice >= 0) { // -1 if pet
							// No need to update the soulbound values if event is null
							addTotal(location, itemPrice, event != null && item.isSoulbound());
							if (event != null) {
								addItem(location, addItemStr(item), itemPrice, item.isSoulbound());
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
					double itemPrice;
					String itemId = sackEntry.getKey();
					if (itemId.startsWith("RUNE_")) {
						String rune = itemId.split("RUNE_")[1];
						int idx = rune.lastIndexOf("_");
						itemId = rune.substring(0, idx) + "_RUNE;" + rune.substring(idx + 1);
						if (!networthRunes.contains(itemId)) {
							continue;
						}
						itemPrice = getLowestPrice(itemId);
					} else {
						itemPrice = getLowestPrice(itemId, true, false, null);
					}
					itemPrice *= sackEntry.getValue();

					addTotal("sacks", itemPrice);
					if (event != null) {
						String emoji = getEmoji(itemId, null);
						addItem(
							"sacks",
							(emoji == null ? "" : emoji + " ") +
							(sackEntry.getValue() != 1 ? formatNumber(sackEntry.getValue()) + "x " : "") +
							idToName(itemId),
							itemPrice
						);
					}
				}
			}
		}

		calculatePetPrices();

		if (event == null) {
			return new MessageEditBuilder().setEmbeds(errorEmbed("Not triggered by command").build());
		}

		player.getProfileToNetworth().put(player.getProfileIndex(), getNetworth());

		int networthPosition = -1;
		try {
			networthPosition = networthPositionFuture.get();
		} catch (Exception ignored) {}

		PaginatorExtras extras = new PaginatorExtras(PaginatorExtras.PaginatorType.EMBED_PAGES);
		Map<SelectOption, EmbedBuilder> pages = getPages(player, false, networthPosition);
		Map<SelectOption, EmbedBuilder> soulboundIgnoredPages = getPages(player, true, networthPosition);

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
					ignored -> {
						extras
							.setSelectPages(soulboundIgnoredPages)
							.toggleReactiveButton("reactive_nw_ignore_soulbound", false)
							.toggleReactiveButton("reactive_nw_show_soulbound", true);
					},
					true
				),
				new PaginatorExtras.ReactiveButton(
					Button.primary("reactive_nw_show_soulbound", "Show Soulbound"),
					ignored -> {
						extras
							.setSelectPages(pages)
							.toggleReactiveButton("reactive_nw_ignore_soulbound", true)
							.toggleReactiveButton("reactive_nw_show_soulbound", false);
					},
					false
				)
			);
		new SelectMenuPaginator("overview", extras, event);
		return null;
	}

	private Map<SelectOption, EmbedBuilder> getPages(Player.Profile player, boolean ignoreSoulbound, int networthPosition) {
		StringBuilder sacksStr = getSectionString("sacks", ignoreSoulbound);
		StringBuilder itemsStr = getSectionString("items", ignoreSoulbound);
		StringBuilder armorStr = getSectionString("armor", ignoreSoulbound);
		StringBuilder petsStr = getSectionString("pets", ignoreSoulbound);
		StringBuilder talismanStr = getSectionString("talisman", ignoreSoulbound);
		StringBuilder museumStr = getSectionString("museum", ignoreSoulbound);

		double totalNetworth = getNetworth(ignoreSoulbound);
		String ebDesc =
			"**Total Networth:** " +
			simplifyNumber(totalNetworth) +
			" (" +
			roundAndFormat(totalNetworth) +
			")\n**" +
			(Player.Gamemode.IRONMAN_STRANDED.isGamemode(player.getGamemode())
					? capitalizeString(player.getGamemode().toString()) + " "
					: "") +
			"Leaderboard Position:** " +
			(networthPosition != -1 ? formatNumber(networthPosition) : "Not on leaderboard");
		EmbedBuilder eb = player
			.defaultPlayerEmbed()
			.setDescription(ebDesc)
			.addField(
				"Purse & Bank",
				simplifyNumber(getTotal("purse", ignoreSoulbound)) +
				" & " +
				(getTotal("bank", ignoreSoulbound) == -1 ? "Private" : simplifyNumber(getTotal("bank", ignoreSoulbound))),
				true
			)
			.addField("Sacks", simplifyNumber(getTotal("sacks", ignoreSoulbound)), true)
			.addField("Essence", simplifyNumber(getTotal("essence", ignoreSoulbound)), true);
		if (!museumStr.isEmpty()) {
			eb.addField("Museum | " + simplifyNumber(getTotal("museum", ignoreSoulbound)), museumStr.toString().split("\n\n")[0], false);
		} else if (!player.getMuseum().isValid()) {
			eb.addField("Museum", "Museum API is disabled ([**help enabling**](https://i.imgur.com/h2ybRgU.mp4))", false);
		} else {
			eb.addField("Museum", "Museum empty or all items are being borrowed", false);
		}
		if (!itemsStr.isEmpty()) {
			eb.addField("Items | " + simplifyNumber(getTotal("items", ignoreSoulbound)), itemsStr.toString().split("\n\n")[0], false);
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

		Map<SelectOption, EmbedBuilder> pages = new LinkedHashMap<>();
		pages.put(SelectOption.of("Overview", "overview").withEmoji(getEmojiObj("SKYBLOCK_MENU")), eb);
		if (!museumStr.isEmpty()) {
			pages.put(
				SelectOption.of("Museum", "museum").withEmoji(getEmojiObj("MUSEUM_PORTAL")),
				player
					.defaultPlayerEmbed(" | Museum")
					.setDescription(
						ebDesc +
						"\n**Museum:** " +
						simplifyNumber(getTotal("museum", ignoreSoulbound)) +
						"\n\n" +
						museumStr.toString().replace("\n\n", "\n")
					)
			);
		}
		if (!itemsStr.isEmpty()) {
			pages.put(
				SelectOption.of("Items", "items").withEmoji(getEmojiObj("CHEST")),
				player
					.defaultPlayerEmbed(" | Items")
					.setDescription(
						ebDesc +
						"\n**Items:** " +
						simplifyNumber(getTotal("items", ignoreSoulbound)) +
						"\n\n" +
						itemsStr.toString().replace("\n\n", "\n")
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

	public StringBuilder getSectionString(String location, boolean ignoreSoulbound) {
		List<NetworthItem> items = getItems(location);
		if (items.isEmpty()) {
			return new StringBuilder();
		}

		items.sort(Comparator.comparingDouble(item -> -item.price()));
		StringBuilder str = new StringBuilder();

		int i = 0;
		for (NetworthItem item : items) {
			if (ignoreSoulbound && item.soulbound()) {
				continue;
			}

			str.append(item.item()).append(" ➜ ").append(simplifyNumber(item.price())).append("\n");
			if (i == 4) {
				str.append("\n");
			} else if (i == 24) {
				int moreItems = items.size() - 25;
				if (moreItems > 0) {
					str.append("... ").append(moreItems).append(" more item").append(moreItems > 1 ? "s" : "");
				}
				break;
			}
			i++;
		}

		return str;
	}

	public double getNetworth() {
		return getNetworth(false);
	}

	public double getNetworth(boolean ignoreSoulbound) {
		return totals.getOrDefault("items", -1.0) == -1
			? -1
			: (ignoreSoulbound ? soulboundIgnoredTotals : totals).values().stream().mapToDouble(i -> i).sum();
	}

	public void calculatePetPrice(String auctionName, double auctionPrice) {
		for (Iterator<InvItem> iterator = pets.iterator(); iterator.hasNext();) {
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

				double totalPrice = auctionPrice + miscExtras;
				addItem("pets", addItemStr(item), totalPrice);
				addTotal("pets", totalPrice);

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

	public void calculateDefaultPetPrices() {
		for (InvItem item : pets) {
			double auctionPrice = getMinBinAvg(
				item.getName().split("] ")[1].toUpperCase().replace(" ", "_") + ";" + RARITY_TO_NUMBER_MAP.get(item.getRarity())
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

				double itemPrice = auctionPrice + miscExtras;
				addItem("pets", addItemStr(item), itemPrice);
				addTotal("pets", itemPrice);

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
		String queryStr = pets.stream().map(InvItem::getPetApiName).distinct().collect(Collectors.joining(","));
		if (queryStr.isEmpty()) {
			return;
		}

		JsonArray ahQuery = getAuctionPetsByName(queryStr);
		if (ahQuery != null) {
			for (JsonElement auction : ahQuery) {
				String auctionName = higherDepth(auction, "name").getAsString();
				double auctionPrice = higherDepth(auction, "price").getAsDouble();
				calculatePetPrice(auctionName, auctionPrice);
			}
		}

		calculateDefaultPetPrices();
	}

	public double getMinBinAvg(String id) {
		return getMin(
			higherDepth(lowestBinJson, id, -1.0),
			getMin(higherDepth(averageAuctionJson, id + ".clean_price", -1.0), higherDepth(averageAuctionJson, id + ".price", -1.0))
		);
	}

	public String addItemStr(InvItem item) {
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

		formattedStr += (item.isRecombobulated() ? " " + getEmoji("RECOMBOBULATOR_3000") : "");

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

		double itemCount = 1;
		double itemCost = 0;
		double recombobulatedExtra = 0;
		double hpbExtras = 0;
		double enchantsExtras = 0;
		double fumingExtras = 0;
		double reforgeExtras = 0;
		double miscExtras = 0;
		double backpackExtras = 0;
		double essenceExtras = 0;
		double runesExtras = 0;

		StringBuilder source = verbose ? new StringBuilder() : null;
		try {
			if (item.getId().equals("PET") && location != null) {
				if (!item.getName().startsWith("Mystery ") && !item.getName().equals("Unknown Pet")) {
					pets.add(item);
				}
				return -1;
			} else {
				if (item.getDarkAuctionPrice() != -1) {
					long maxBid = item.getId().equals("MIDAS_SWORD") ? 50000000 : 100000000;

					if (item.getDarkAuctionPrice() >= maxBid) {
						itemCost = getLowestPrice(item.getId() + "_" + maxBid, false, false, source);
						if (itemCost == 0) {
							itemCost = getLowestPrice(item.getId(), false, false, source);
						}
					} else {
						itemCost = item.getDarkAuctionPrice();
					}

					if (verbose) {
						source.append("dark auction price paid");
					}
				} else {
					String itemId = item.getId();

					// Prestiged crimson armor is soulbound (prestige costs are accounted for in misc)
					Matcher matcher = crimsonArmorRegex.matcher(item.getId());
					if (matcher.matches()) {
						itemId = matcher.group(2) + "_" + matcher.group(3);
					}

					if (!item.getAttributes().isEmpty()) {
						String attributeItemId = itemId + "+" + String.join("+", item.getAttributes().keySet());
						double attributeItemCost = getLowestPrice(attributeItemId, false, false, source);
						if (attributeItemCost > 0) {
							itemCost = attributeItemCost;
							if (verbose) {
								source.append(" - " + String.join(" & ", item.getAttributes().keySet()));
							}
						}
					}

					if (itemCost == 0) {
						if (item.isShiny()) {
							itemCost = getLowestPrice(itemId + "_SHINY", false, false, source);
							if (itemCost > 0 && verbose) {
								source.append(" - shiny");
							}
						}

						if (itemCost == 0) {
							itemCost = getLowestPrice(itemId, false, false, source);
						}
					}

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
				(!item.getEnchantsFormatted().isEmpty() ||
					allowedRecombCategories.contains(higherDepth(getSkyblockItemsJson().get(item.getId()), "category", "")))
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
					if (!item.getId().equals("ENCHANTED_BOOK") && enchant.equals("SCAVENGER;5")) {
						continue;
					}

					double enchantPrice = getLowestPriceEnchant(enchant);
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

		StringBuilder runesStr = verbose ? new StringBuilder("{") : null;
		try {
			List<String> runes = item.getRunesFormatted();
			for (String rune : runes) {
				try {
					if (!networthRunes.contains(rune)) {
						continue;
					}

					double runePrice = getLowestPrice(rune);
					if (!item.getId().equals("RUNE")) {
						runePrice *= 0.65;
					}
					runesExtras += runePrice;
					if (verbose) {
						runesStr.append("\"").append(rune).append("\":\"").append(simplifyNumber(runePrice)).append("\",");
					}
				} catch (Exception ignored) {}
			}
		} catch (Exception ignored) {}
		if (verbose) {
			if (runesStr.charAt(runesStr.length() - 1) == ',') {
				runesStr.deleteCharAt(runesStr.length() - 1);
			}
			runesStr.append("}");
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
			for (Map.Entry<String, Integer> attribute : item.getAttributes().entrySet()) {
				// Second -1 is since level 1 is accounted for by the base calculation
				int count = (int) Math.pow(2, attribute.getValue() - 1) - 1;
				if (count == 0) {
					continue;
				}

				double miscPrice = getLowestPrice(attribute.getKey());
				// TODO: check this
				//				if (attributesBaseCosts.containsKey(item.getId())) {
				//					double calcBasePrice = getLowestPrice(attributesBaseCosts.get(item.getId()));
				//					if (calcBasePrice > 0) {
				//						miscPrice = Math.min(miscPrice, calcBasePrice);
				//					}
				//				} else
				if (isCrimsonArmor(item.getId(), false)) {
					double calcBasePrice = Stream
						.of("HELMET", "CHESTPLATE", "LEGGINGS", "BOOTS")
						.map(a ->
							higherDepth(extraPrices, ("KUUDRA_" + a + "_" + attribute.getKey().split("ATTRIBUTE_SHARD_")[1]).toLowerCase())
						)
						.filter(Objects::nonNull)
						.mapToDouble(JsonElement::getAsDouble)
						.average()
						.orElse(0);
					if (calcBasePrice > 0) {
						miscPrice = Math.min(miscPrice, calcBasePrice);
					}
				}

				miscPrice *= 0.95;
				miscExtras += miscPrice * count;
				if (verbose) {
					miscStr
						.append("{\"name\":\"")
						.append(attribute.getKey() + ";" + attribute.getValue())
						.append("\",\"total\":\"")
						.append(simplifyNumber(miscPrice * count))
						.append("\",\"count\":")
						.append(count)
						.append(",\"cost\":\"")
						.append(simplifyNumber(miscPrice))
						.append("\"},");
				}
			}

			for (Map.Entry<String, Integer> extraItem : item.getExtraStats().entrySet()) {
				double miscPrice = getLowestPrice(extraItem.getKey());
				if (!extraItem.getKey().equals("SKYBLOCK_COIN")) {
					miscPrice *= 0.95;
				}

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
			(itemCost +
				recombobulatedExtra +
				hpbExtras +
				enchantsExtras +
				fumingExtras +
				reforgeExtras +
				miscExtras +
				backpackExtras +
				essenceExtras +
				runesExtras);

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
			out.append(fumingExtras > 0 ? ",\"fuming\":\"" + simplifyNumber(fumingExtras) + "\"" : "");
			out.append(
				enchantsExtras > 0 ? ",\"enchants\":{\"total\":\"" + simplifyNumber(enchantsExtras) + "\",\"data\":" + enchStr + "}" : ""
			);
			out.append(runesExtras > 0 ? ",\"runes\":{\"total\":\"" + simplifyNumber(runesExtras) + "\",\"data\":" + runesStr + "}" : "");
			out.append(
				reforgeExtras > 0
					? ",\"reforge\":{\"cost\":\"" + simplifyNumber(reforgeExtras) + "\",\"name\":\"" + item.getModifier() + "\"}"
					: ""
			);
			out.append(miscExtras > 0 ? ",\"misc\":{\"total\":\"" + simplifyNumber(miscExtras) + "\",\"data\":" + miscStr + "}" : "");
			out.append(backpackExtras > 0 ? ",\"bp\":{\"total\":\"" + simplifyNumber(backpackExtras) + "\",\"data\":" + bpStr + "}" : "");
			out.append(",\"nbt\":\"").append(cleanMcCodes(item.getNbtTag().toString().replace("\"", "\\\""))).append("\"");
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
		return getLowestPrice(itemId, false, false, null);
	}

	public double getLowestPrice(String itemId, boolean ignoreAh, boolean onlyFullCraft, StringBuilder source) {
		if (!itemId.equals("NEW_YEAR_CAKE_BAG") && itemId.startsWith("NEW_YEAR_CAKE_")) {
			if (source != null) {
				source.append("price override");
			}
			return 0;
		}

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

				double itemLowestPrice = getLowestPrice(idCountSplit[0].replace("-", ":"), false, true, null);
				if (itemLowestPrice == 0) {
					craftCost = 0;
					break;
				}
				craftCost += itemLowestPrice * Double.parseDouble(idCountSplit[1]);
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

		// If it's positive, it is a full craft
		if (craftCost > 0) {
			if (source != null) {
				if (higherDepth(getInternalJsonMappings(), itemId + ".recipe") != null) {
					source.append("craft");
				} else {
					source.append("npc buy");
				}
			}
			return craftCost;
		}

		if (!onlyFullCraft) {
			if (recipe != null) {
				double partialCraftCost = 0;
				for (String item : recipe) {
					String[] idCountSplit = item.split(":");
					partialCraftCost += getLowestPrice(idCountSplit[0].replace("-", ":")) * Double.parseDouble(idCountSplit[1]);
				}
				partialCraftCost /= getRecipeCount(itemId);

				if (partialCraftCost > 0) {
					if (source != null) {
						if (higherDepth(getInternalJsonMappings(), itemId + ".recipe") != null) {
							source.append("partial craft");
						} else {
							source.append("partial npc buy");
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
		location =
			switch (location) {
				case "equipment", "wardrobe" -> "armor";
				case "inventory", "enderchest", "personal_vault", "storage" -> "items";
				default -> location;
			};
		totals.compute(location, (k, v) -> (v == null ? 0 : v) + total);
		if (!isSoulbound) {
			soulboundIgnoredTotals.compute(location, (k, v) -> (v == null ? 0 : v) + total);
		}
	}

	public void addItem(String location, String item, double price) {
		addItem(location, item, price, false);
	}

	public void addItem(String location, String item, double price, boolean isSoulbound) {
		location =
			switch (location) {
				case "equipment", "wardrobe" -> "armor";
				case "inventory", "enderchest", "personal_vault", "storage" -> "items";
				default -> location;
			};
		items.compute(
			location,
			(k, v) -> {
				(v = (v == null ? new ArrayList<>() : v)).add(new NetworthItem(item, price, isSoulbound));
				return v;
			}
		);
	}

	public double getTotal(String location, boolean ignoreSoulbound) {
		// soulboundIgnoredTotals does not include souldbound items
		return (ignoreSoulbound ? soulboundIgnoredTotals : totals).getOrDefault(location, 0.0);
	}

	public List<NetworthItem> getItems(String location) {
		return items.getOrDefault(location, new ArrayList<>());
	}

	record NetworthItem(String item, double price, boolean soulbound) {}
}
