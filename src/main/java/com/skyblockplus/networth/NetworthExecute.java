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

package com.skyblockplus.networth;

import static com.skyblockplus.utils.ApiHandler.getAuctionPetsByName;
import static com.skyblockplus.utils.Constants.*;
import static com.skyblockplus.utils.Utils.*;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.skyblockplus.utils.Player;
import com.skyblockplus.utils.command.CommandExecute;
import com.skyblockplus.utils.structs.InvItem;
import com.skyblockplus.utils.structs.NwItemPrice;
import java.util.*;
import net.dv8tion.jda.api.EmbedBuilder;

public class NetworthExecute {

	private final Set<String> tempSet = new HashSet<>();
	private final List<InvItem> invPets = new ArrayList<>();
	private final List<InvItem> petsPets = new ArrayList<>();
	private final List<InvItem> enderChestPets = new ArrayList<>();
	private final List<InvItem> storagePets = new ArrayList<>();
	private final List<String> enderChestItems = new ArrayList<>();
	private final List<String> petsItems = new ArrayList<>();
	private final List<String> invItems = new ArrayList<>();
	private final List<String> wardrobeItems = new ArrayList<>();
	private final List<String> talismanItems = new ArrayList<>();
	private final List<String> armorItems = new ArrayList<>();
	private final List<String> storageItems = new ArrayList<>();
	private StringBuilder calcItemsJsonStr = new StringBuilder("[");
	private JsonElement lowestBinJson;
	private JsonElement averageAuctionJson;
	private JsonElement bazaarJson;
	private JsonArray sbzPrices;
	private double enderChestTotal = 0;
	private double petsTotal = 0;
	private double invTotal = 0;
	private double bankBalance = 0;
	private double purseCoins = 0;
	private double wardrobeTotal = 0;
	private double talismanTotal = 0;
	private double invArmor = 0;
	private double storageTotal = 0;
	private double sacksTotal = 0;
	private boolean verbose = false;
	private double recombPrice;
	private double fumingPrice;
	private double hbpPrice;
	private boolean onlyTotal = false;

	public NetworthExecute setVerbose(boolean verbose) {
		this.verbose = verbose;
		return this;
	}

	public NetworthExecute setOnlyTotal(boolean onlyTotal) {
		this.onlyTotal = onlyTotal;
		return this;
	}

	public NetworthExecute initPrices() {
		lowestBinJson = getLowestBinJson();
		averageAuctionJson = getAverageAuctionJson();
		bazaarJson = higherDepth(getBazaarJson(), "products");
		sbzPrices = getSbzPricesJson();

		recombPrice = higherDepth(bazaarJson, "RECOMBOBULATOR_3000.sell_summary.[0].pricePerUnit", 0.0);
		hbpPrice = higherDepth(bazaarJson, "HOT_POTATO_BOOK.sell_summary.[0].pricePerUnit", 0.0);
		fumingPrice = higherDepth(bazaarJson, "FUMING_POTATO_BOOK.sell_summary.[0].pricePerUnit", 0.0);
		return this;
	}

	public void execute(Command command, CommandEvent event) {
		new CommandExecute(command, event) {
			@Override
			protected void execute() {
				logCommand();
				verbose = getBooleanArg("--verbose");

				if (args.length == 3 || args.length == 2 || args.length == 1) {
					if (getMentionedUsername(args.length == 1 ? -1 : 1)) {
						return;
					}

					embed(getPlayerNetworth(username, args.length == 3 ? args[2] : null));
					return;
				}

				sendErrorEmbed();
			}
		}
			.queue();
	}

	public EmbedBuilder getPlayerNetworth(String username, String profileName) {
		Player player = profileName == null ? new Player(username) : new Player(username, profileName);
		if (player.isValid()) {
			EmbedBuilder eb = player.defaultPlayerEmbed();

			lowestBinJson = getLowestBinJson();
			averageAuctionJson = getAverageAuctionJson();
			bazaarJson = higherDepth(getBazaarJson(), "products");
			sbzPrices = getSbzPricesJson();

			recombPrice = higherDepth(bazaarJson, "RECOMBOBULATOR_3000.sell_summary.[0].pricePerUnit", 0.0);
			hbpPrice = higherDepth(bazaarJson, "HOT_POTATO_BOOK.sell_summary.[0].pricePerUnit", 0.0);
			fumingPrice = higherDepth(bazaarJson, "FUMING_POTATO_BOOK.sell_summary.[0].pricePerUnit", 0.0);

			bankBalance = player.getBankBalance();
			purseCoins = player.getPurseCoins();

			Map<Integer, InvItem> playerInventory = player.getInventoryMap();
			if (playerInventory == null) {
				invTotal = -1;
				return defaultEmbed(player.getUsername() + "'s inventory API is disabled");
			}
			for (InvItem item : playerInventory.values()) {
				double itemPrice = calculateItemPrice(item, "inventory");
				invTotal += itemPrice;
				if (item != null) {
					invItems.add(addItemStr(item, itemPrice));
				}
			}

			Map<Integer, InvItem> playerTalismans = player.getTalismanBagMap();
			if (playerTalismans != null) {
				for (InvItem item : playerTalismans.values()) {
					double itemPrice = calculateItemPrice(item);
					talismanTotal += itemPrice;
					if (item != null) {
						talismanItems.add(addItemStr(item, itemPrice));
					}
				}
			}

			Map<Integer, InvItem> invArmorMap = player.getInventoryArmorMap();
			if (invArmorMap != null) {
				for (InvItem item : invArmorMap.values()) {
					double itemPrice = calculateItemPrice(item);
					invArmor += itemPrice;
					if (item != null) {
						armorItems.add(addItemStr(item, itemPrice));
					}
				}
			}

			Map<Integer, InvItem> wardrobeMap = player.getWardrobeMap();
			if (wardrobeMap != null) {
				for (InvItem item : wardrobeMap.values()) {
					double itemPrice = calculateItemPrice(item);
					wardrobeTotal += itemPrice;
					if (item != null) {
						wardrobeItems.add(addItemStr(item, itemPrice));
					}
				}
			}

			List<InvItem> petsMap = player.getPetsMapNames();
			if (petsMap != null) {
				for (InvItem item : petsMap) {
					petsTotal += calculateItemPrice(item, "pets");
				}
			}

			Map<Integer, InvItem> enderChest = player.getEnderChestMap();
			if (enderChest != null) {
				for (InvItem item : enderChest.values()) {
					double itemPrice = calculateItemPrice(item, "enderchest");
					enderChestTotal += itemPrice;
					if (item != null) {
						enderChestItems.add(addItemStr(item, itemPrice));
					}
				}
			}

			Map<Integer, InvItem> storageMap = player.getStorageMap();
			if (storageMap != null) {
				for (InvItem item : storageMap.values()) {
					double itemPrice = calculateItemPrice(item, "storage");
					storageTotal += itemPrice;
					if (item != null) {
						storageItems.add(addItemStr(item, itemPrice));
					}
				}
			}

			Map<String, Integer> sacksMap = player.getPlayerSacks();
			if (sacksMap != null) {
				for (Map.Entry<String, Integer> sackEntry : sacksMap.entrySet()) {
					if (sackEntry.getValue() > 0) {
						sacksTotal += getLowestPrice(sackEntry.getKey(), sackEntry.getKey(), true) * sackEntry.getValue();
					}
				}
			}

			calculateAllPetsPrice();

			if (onlyTotal) {
				return invalidEmbed("Only total is enabled");
			}

			enderChestItems.sort(Comparator.comparingDouble(item -> -Double.parseDouble(item.split("@split@")[1])));
			StringBuilder echestStr = new StringBuilder();
			for (int i = 0; i < enderChestItems.size(); i++) {
				String item = enderChestItems.get(i);
				echestStr
					.append("• ")
					.append(item.split("@split@")[0])
					.append(" ➜ ")
					.append(simplifyNumber(Double.parseDouble(item.split("@split@")[1])))
					.append("\n");
				if (i == 4) {
					break;
				}
			}

			storageItems.sort(Comparator.comparingDouble(item -> -Double.parseDouble(item.split("@split@")[1])));
			StringBuilder storageStr = new StringBuilder();
			for (int i = 0; i < storageItems.size(); i++) {
				String item = storageItems.get(i);
				storageStr
					.append("• ")
					.append(item.split("@split@")[0])
					.append(" ➜ ")
					.append(simplifyNumber(Double.parseDouble(item.split("@split@")[1])))
					.append("\n");
				if (i == 4) {
					break;
				}
			}

			invItems.sort(Comparator.comparingDouble(item -> -Double.parseDouble(item.split("@split@")[1])));
			StringBuilder invStr = new StringBuilder();
			for (int i = 0; i < invItems.size(); i++) {
				String item = invItems.get(i);
				invStr
					.append("• ")
					.append(item.split("@split@")[0])
					.append(" ➜ ")
					.append(simplifyNumber(Double.parseDouble(item.split("@split@")[1])))
					.append("\n");
				if (i == 4) {
					break;
				}
			}

			armorItems.sort(Comparator.comparingDouble(item -> -Double.parseDouble(item.split("@split@")[1])));
			StringBuilder armorStr = new StringBuilder();
			for (int i = 0; i < armorItems.size(); i++) {
				String item = armorItems.get(i);
				armorStr
					.append("• ")
					.append(item.split("@split@")[0])
					.append(" ➜ ")
					.append(simplifyNumber(Double.parseDouble(item.split("@split@")[1])))
					.append("\n");
				if (i == 4) {
					break;
				}
			}

			wardrobeItems.sort(Comparator.comparingDouble(item -> -Double.parseDouble(item.split("@split@")[1])));
			StringBuilder wardrobeStr = new StringBuilder();
			for (int i = 0; i < wardrobeItems.size(); i++) {
				String item = wardrobeItems.get(i);
				wardrobeStr
					.append("• ")
					.append(item.split("@split@")[0])
					.append(" ➜ ")
					.append(simplifyNumber(Double.parseDouble(item.split("@split@")[1])))
					.append("\n");
				if (i == 4) {
					break;
				}
			}

			petsItems.sort(Comparator.comparingDouble(item -> -Double.parseDouble(item.split("@split@")[1])));
			StringBuilder petsStr = new StringBuilder();
			for (int i = 0; i < petsItems.size(); i++) {
				String item = petsItems.get(i);
				petsStr
					.append("• ")
					.append(item.split("@split@")[0])
					.append(" ➜ ")
					.append(simplifyNumber(Double.parseDouble(item.split("@split@")[1])))
					.append("\n");
				if (i == 4) {
					break;
				}
			}

			talismanItems.sort(Comparator.comparingDouble(item -> -Double.parseDouble(item.split("@split@")[1])));
			StringBuilder talismanStr = new StringBuilder();
			for (int i = 0; i < talismanItems.size(); i++) {
				String item = talismanItems.get(i);
				talismanStr
					.append("• ")
					.append(item.split("@split@")[0])
					.append(" ➜ ")
					.append(simplifyNumber(Double.parseDouble(item.split("@split@")[1])))
					.append("\n");
				if (i == 4) {
					break;
				}
			}

			double totalNetworth = getTotalCalculatedNetworth();

			eb.setDescription("Total Networth: " + simplifyNumber(totalNetworth) + " (" + formatNumber(totalNetworth) + ")");
			eb.addField("Purse", simplifyNumber(purseCoins), true);
			eb.addField("Bank", (bankBalance == -1 ? "Private" : simplifyNumber(bankBalance)), true);
			eb.addField("Sacks", simplifyNumber(sacksTotal), true);
			eb.addField(
				"Ender Chest | " + simplifyNumber(enderChestTotal),
				echestStr.length() == 0 ? "Empty" : echestStr.toString(),
				false
			);
			eb.addField("Storage | " + simplifyNumber(storageTotal), storageStr.length() == 0 ? "Empty" : storageStr.toString(), false);
			eb.addField("Inventory | " + simplifyNumber(invTotal), invStr.length() == 0 ? "Empty" : invStr.toString(), false);
			eb.addField("Armor | " + simplifyNumber(invArmor), armorStr.length() == 0 ? "Empty" : armorStr.toString(), false);
			eb.addField("Wardrobe | " + simplifyNumber(wardrobeTotal), wardrobeStr.length() == 0 ? "Empty" : wardrobeStr.toString(), false);
			eb.addField("Pets | " + simplifyNumber(petsTotal), petsStr.length() == 0 ? "Empty" : petsStr.toString(), false);
			eb.addField(
				"Accessories | " + simplifyNumber(talismanTotal),
				talismanStr.length() == 0 ? "Empty" : talismanStr.toString(),
				false
			);
			eb.addField("Bug in the price calculation?", "[Please submit a bug report here!](https://forms.gle/RBmN2AFBLafGyx5E7)", false);

			if (verbose) {
				try {
					long startTime = System.currentTimeMillis();

					eb.appendDescription("\nVerbose JSON: " + makeHastePost(formattedGson.toJson(getVerboseJson())) + ".json");
					System.out.println("Verbose time: " + (System.currentTimeMillis() - startTime) + " ms");
				} catch (Exception e) {
					e.printStackTrace();
				}
			}

			JsonArray missing = collectJsonArray(
				tempSet.stream().filter(str -> !str.toLowerCase().startsWith("rune_")).map(JsonPrimitive::new)
			);
			if (!missing.isEmpty()) {
				System.out.println(missing);
			}

			return eb;
		}
		return player.getFailEmbed();
	}

	public static double getTotalNetworth(String username, String profileName) {
		NetworthExecute calc = new NetworthExecute().setOnlyTotal(true);
		calc.getPlayerNetworth(username, profileName);
		return calc.getTotalCalculatedNetworth();
	}

	public double getTotalCalculatedNetworth() {
		return invTotal == -1
			? -1
			: bankBalance +
			purseCoins +
			invTotal +
			talismanTotal +
			invArmor +
			wardrobeTotal +
			petsTotal +
			enderChestTotal +
			storageTotal +
			sacksTotal;
	}

	private void calculateAllPetsPrice() {
		StringBuilder queryStr = new StringBuilder();
		for (InvItem item : invPets) {
			queryStr.append("'").append(item.getPetApiName()).append("',");
		}
		for (InvItem item : petsPets) {
			queryStr.append("'").append(item.getPetApiName()).append("',");
		}
		for (InvItem item : enderChestPets) {
			queryStr.append("'").append(item.getPetApiName()).append("',");
		}

		for (InvItem item : storagePets) {
			queryStr.append("'").append(item.getPetApiName()).append("',");
		}

		if (queryStr.length() == 0) {
			return;
		}

		queryStr = new StringBuilder(queryStr.substring(0, queryStr.length() - 1));
		JsonArray ahQuery = getAuctionPetsByName(queryStr.toString());

		if (ahQuery != null) {
			for (JsonElement auction : ahQuery) {
				String auctionName = higherDepth(auction, "name").getAsString();
				double auctionPrice = higherDepth(auction, "price").getAsDouble();

				for (Iterator<InvItem> iterator = invPets.iterator(); iterator.hasNext();) {
					InvItem item = iterator.next();
					if (item.getPetApiName().equals(auctionName)) {
						StringBuilder miscStr = new StringBuilder("[");
						double miscExtras = 0;
						try {
							List<String> extraStats = item.getExtraStats();
							for (String extraItem : extraStats) {
								double miscPrice = getLowestPrice(extraItem, " ");
								miscExtras += miscPrice;
								miscStr
									.append("{\"name\":\"")
									.append(extraItem)
									.append("\",\"price\":\"")
									.append(simplifyNumber(miscPrice))
									.append("\"},");
							}
						} catch (Exception ignored) {}
						if (miscStr.toString().endsWith(",")) {
							miscStr = new StringBuilder(miscStr.substring(0, miscStr.length() - 1));
						}
						miscStr.append("]");

						invItems.add(addItemStr(item, auctionPrice + miscExtras));
						invTotal += auctionPrice + miscExtras;
						if (verbose) {
							calcItemsJsonStr
								.append("{\"name\":\"")
								.append(item.getName())
								.append("\",\"total\":\"")
								.append(simplifyNumber(auctionPrice + miscExtras))
								.append("\",\"base_cost\":\"")
								.append(simplifyNumber(auctionPrice))
								.append("\"")
								.append(
									miscExtras > 0
										? ",\"misc\":{\"total\":\"" + simplifyNumber(miscExtras) + "\",\"miscs\":" + miscStr + "}"
										: ""
								)
								.append("},");
						}
						iterator.remove();
					}
				}

				for (Iterator<InvItem> iterator = petsPets.iterator(); iterator.hasNext();) {
					InvItem item = iterator.next();
					if (item.getPetApiName().equals(auctionName)) {
						StringBuilder miscStr = new StringBuilder("[");
						double miscExtras = 0;
						try {
							List<String> extraStats = item.getExtraStats();
							for (String extraItem : extraStats) {
								double miscPrice = getLowestPrice(extraItem, " ");
								miscExtras += miscPrice;
								miscStr
									.append("{\"name\":\"")
									.append(extraItem)
									.append("\",\"price\":\"")
									.append(simplifyNumber(miscPrice))
									.append("\"},");
							}
						} catch (Exception ignored) {}
						if (miscStr.toString().endsWith(",")) {
							miscStr = new StringBuilder(miscStr.substring(0, miscStr.length() - 1));
						}
						miscStr.append("]");

						petsItems.add(addItemStr(item, auctionPrice + miscExtras));
						petsTotal += auctionPrice + miscExtras;
						if (verbose) {
							calcItemsJsonStr
								.append("{\"name\":\"")
								.append(item.getName())
								.append("\",\"total\":\"")
								.append(simplifyNumber(auctionPrice + miscExtras))
								.append("\",\"base_cost\":\"")
								.append(simplifyNumber(auctionPrice))
								.append("\"")
								.append(
									miscExtras > 0
										? ",\"misc\":{\"total\":\"" + simplifyNumber(miscExtras) + "\",\"miscs\":" + miscStr + "}"
										: ""
								)
								.append("},");
						}
						iterator.remove();
					}
				}

				for (Iterator<InvItem> iterator = enderChestPets.iterator(); iterator.hasNext();) {
					InvItem item = iterator.next();
					if (item.getPetApiName().equals(auctionName)) {
						StringBuilder miscStr = new StringBuilder("[");
						double miscExtras = 0;
						try {
							List<String> extraStats = item.getExtraStats();
							for (String extraItem : extraStats) {
								double miscPrice = getLowestPrice(extraItem, " ");
								miscExtras += miscPrice;
								miscStr
									.append("{\"name\":\"")
									.append(extraItem)
									.append("\",\"price\":\"")
									.append(simplifyNumber(miscPrice))
									.append("\"},");
							}
						} catch (Exception ignored) {}
						if (miscStr.toString().endsWith(",")) {
							miscStr = new StringBuilder(miscStr.substring(0, miscStr.length() - 1));
						}
						miscStr.append("]");

						enderChestItems.add(addItemStr(item, auctionPrice + miscExtras));
						enderChestTotal += auctionPrice + miscExtras;
						if (verbose) {
							calcItemsJsonStr
								.append("{\"name\":\"")
								.append(item.getName())
								.append("\",\"total\":\"")
								.append(simplifyNumber(auctionPrice + miscExtras))
								.append("\",\"base_cost\":\"")
								.append(simplifyNumber(auctionPrice))
								.append("\"")
								.append(
									miscExtras > 0
										? ",\"misc\":{\"total\":\"" + simplifyNumber(miscExtras) + "\",\"miscs\":" + miscStr + "}"
										: ""
								)
								.append("},");
						}
						iterator.remove();
					}
				}

				for (Iterator<InvItem> iterator = storagePets.iterator(); iterator.hasNext();) {
					InvItem item = iterator.next();
					if (item.getPetApiName().equals(auctionName)) {
						StringBuilder miscStr = new StringBuilder("[");
						double miscExtras = 0;
						try {
							List<String> extraStats = item.getExtraStats();
							for (String extraItem : extraStats) {
								double miscPrice = getLowestPrice(extraItem, " ");
								miscExtras += miscPrice;
								miscStr
									.append("{\"name\":\"")
									.append(extraItem)
									.append("\",\"price\":\"")
									.append(simplifyNumber(miscPrice))
									.append("\"},");
							}
						} catch (Exception ignored) {}
						if (miscStr.toString().endsWith(",")) {
							miscStr = new StringBuilder(miscStr.substring(0, miscStr.length() - 1));
						}
						miscStr.append("]");

						storageItems.add(addItemStr(item, auctionPrice + miscExtras));
						storageTotal += auctionPrice + miscExtras;
						if (verbose) {
							calcItemsJsonStr
								.append("{\"name\":\"")
								.append(item.getName())
								.append("\",\"total\":\"")
								.append(simplifyNumber(auctionPrice + miscExtras))
								.append("\",\"base_cost\":\"")
								.append(simplifyNumber(auctionPrice))
								.append("\"")
								.append(
									miscExtras > 0
										? ",\"misc\":{\"total\":\"" + simplifyNumber(miscExtras) + "\",\"miscs\":" + miscStr + "}"
										: ""
								)
								.append("},");
						}
						iterator.remove();
					}
				}
			}
		}

		for (InvItem item : invPets) {
			double auctionPrice = getMinBinAvg(
				item.getName().split("] ")[1].toUpperCase().replace(" ", "_") + RARITY_TO_NUMBER_MAP.get(item.getRarity())
			);
			if (auctionPrice != -1) {
				StringBuilder miscStr = new StringBuilder("[");
				double miscExtras = 0;
				try {
					List<String> extraStats = item.getExtraStats();
					for (String extraItem : extraStats) {
						double miscPrice = getLowestPrice(extraItem, " ");
						miscExtras += miscPrice;
						miscStr
							.append("{\"name\":\"")
							.append(extraItem)
							.append("\",\"price\":\"")
							.append(simplifyNumber(miscPrice))
							.append("\"},");
					}
				} catch (Exception ignored) {}
				if (miscStr.toString().endsWith(",")) {
					miscStr = new StringBuilder(miscStr.substring(0, miscStr.length() - 1));
				}
				miscStr.append("]");

				invItems.add(addItemStr(item, auctionPrice + miscExtras));
				invTotal += auctionPrice + miscExtras;
				if (verbose) {
					calcItemsJsonStr
						.append("{\"name\":\"")
						.append(item.getName())
						.append("\",\"total\":\"")
						.append(simplifyNumber(auctionPrice + miscExtras))
						.append("\",\"base_cost\":\"")
						.append(simplifyNumber(auctionPrice))
						.append("\",")
						.append(
							miscExtras > 0 ? "\"misc\":{\"total\":\"" + simplifyNumber(miscExtras) + "\",\"miscs\":" + miscStr + "}," : ""
						)
						.append("\"fail_calc_lvl_cost\":true},");
				}
			}
		}

		for (InvItem item : petsPets) {
			double auctionPrice = getMinBinAvg(
				item.getName().split("] ")[1].toUpperCase().replace(" ", "_") + RARITY_TO_NUMBER_MAP.get(item.getRarity())
			);
			if (auctionPrice != -1) {
				StringBuilder miscStr = new StringBuilder("[");
				double miscExtras = 0;
				try {
					List<String> extraStats = item.getExtraStats();
					for (String extraItem : extraStats) {
						double miscPrice = getLowestPrice(extraItem, " ");
						miscExtras += miscPrice;
						miscStr
							.append("{\"name\":\"")
							.append(extraItem)
							.append("\",\"price\":\"")
							.append(simplifyNumber(miscPrice))
							.append("\"},");
					}
				} catch (Exception ignored) {}
				if (miscStr.toString().endsWith(",")) {
					miscStr = new StringBuilder(miscStr.substring(0, miscStr.length() - 1));
				}
				miscStr.append("]");

				petsItems.add(addItemStr(item, auctionPrice + miscExtras));
				petsTotal += auctionPrice + miscExtras;
				if (verbose) {
					calcItemsJsonStr
						.append("{\"name\":\"")
						.append(item.getName())
						.append("\",\"total\":\"")
						.append(simplifyNumber(auctionPrice + miscExtras))
						.append("\",\"base_cost\":\"")
						.append(simplifyNumber(auctionPrice))
						.append("\",")
						.append(
							miscExtras > 0 ? "\"misc\":{\"total\":\"" + simplifyNumber(miscExtras) + "\",\"miscs\":" + miscStr + "}," : ""
						)
						.append("\"fail_calc_lvl_cost\":true},");
				}
			}
		}

		for (InvItem item : enderChestPets) {
			double auctionPrice = getMinBinAvg(
				item.getName().split("] ")[1].toUpperCase().replace(" ", "_") + RARITY_TO_NUMBER_MAP.get(item.getRarity())
			);
			if (auctionPrice != -1) {
				StringBuilder miscStr = new StringBuilder("[");
				double miscExtras = 0;
				try {
					List<String> extraStats = item.getExtraStats();
					for (String extraItem : extraStats) {
						double miscPrice = getLowestPrice(extraItem, " ");
						miscExtras += miscPrice;
						miscStr
							.append("{\"name\":\"")
							.append(extraItem)
							.append("\",\"price\":\"")
							.append(simplifyNumber(miscPrice))
							.append("\"},");
					}
				} catch (Exception ignored) {}
				if (miscStr.toString().endsWith(",")) {
					miscStr = new StringBuilder(miscStr.substring(0, miscStr.length() - 1));
				}
				miscStr.append("]");

				enderChestItems.add(addItemStr(item, auctionPrice + miscExtras));
				enderChestTotal += auctionPrice + miscExtras;
				if (verbose) {
					calcItemsJsonStr
						.append("{\"name\":\"")
						.append(item.getName())
						.append("\",\"total\":\"")
						.append(simplifyNumber(auctionPrice + miscExtras))
						.append("\",\"base_cost\":\"")
						.append(simplifyNumber(auctionPrice))
						.append("\",")
						.append(
							miscExtras > 0 ? "\"misc\":{\"total\":\"" + simplifyNumber(miscExtras) + "\",\"miscs\":" + miscStr + "}," : ""
						)
						.append("\"fail_calc_lvl_cost\":true},");
				}
			}
		}

		for (InvItem item : storagePets) {
			double auctionPrice = getMinBinAvg(
				item.getName().split("] ")[1].toUpperCase().replace(" ", "_") + RARITY_TO_NUMBER_MAP.get(item.getRarity())
			);
			if (auctionPrice != -1) {
				StringBuilder miscStr = new StringBuilder("[");
				double miscExtras = 0;
				try {
					List<String> extraStats = item.getExtraStats();
					for (String extraItem : extraStats) {
						double miscPrice = getLowestPrice(extraItem, " ");
						miscExtras += miscPrice;
						miscStr
							.append("{\"name\":\"")
							.append(extraItem)
							.append("\",\"price\":\"")
							.append(simplifyNumber(miscPrice))
							.append("\"},");
					}
				} catch (Exception ignored) {}
				if (miscStr.toString().endsWith(",")) {
					miscStr = new StringBuilder(miscStr.substring(0, miscStr.length() - 1));
				}
				miscStr.append("]");

				storageItems.add(addItemStr(item, auctionPrice + miscExtras));
				storageTotal += auctionPrice + miscExtras;
				if (verbose) {
					calcItemsJsonStr
						.append("{\"name\":\"")
						.append(item.getName())
						.append("\",\"total\":\"")
						.append(simplifyNumber(auctionPrice + miscExtras))
						.append("\",\"base_cost\":\"")
						.append(simplifyNumber(auctionPrice))
						.append("\",")
						.append(
							miscExtras > 0 ? "\"misc\":{\"total\":\"" + simplifyNumber(miscExtras) + "\",\"miscs\":" + miscStr + "}," : ""
						)
						.append("\"fail_calc_lvl_cost\":true},");
				}
			}
		}
	}

	private double getMinBinAvg(String id) {
		return getMin(
			higherDepth(lowestBinJson, id, -1.0),
			getMin(higherDepth(averageAuctionJson, id + ".clean_price", -1.0), higherDepth(averageAuctionJson, id + ".price", -1.0))
		);
	}

	private String addItemStr(InvItem item, double itemPrice) {
		String formattedStr =
			(item.getCount() != 1 ? item.getCount() + "x " : "") +
			(item.getId().equals("PET") ? capitalizeString(item.getPetRarity()) + " " : "") +
			item.getName();

		if (item.getPetItem() != null) {
			JsonElement petItemEmoji = getEmojiMap().get(item.getPetItem());
			if (petItemEmoji != null) {
				formattedStr += " " + petItemEmoji.getAsString();
			}
		}

		formattedStr += (item.isRecombobulated() ? " <:recombobulator_3000:852647805813784597>" : "") + "@split@" + itemPrice;

		return formattedStr;
	}

	public double calculateItemPrice(InvItem item) {
		return calculateItemPrice(item, null);
	}

	private double calculateItemPrice(InvItem item, String location) {
		if (item == null || item.getId().equals("None")) {
			return 0;
		}

		if (getPriceOverride(item.getId()) == 0) {
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

		try {
			if (item.getId().equals("PET") && location != null) {
				switch (location) {
					case "inventory":
						invPets.add(item);
						break;
					case "pets":
						petsPets.add(item);
						break;
					case "enderchest":
						enderChestPets.add(item);
						break;
					case "storage":
						storagePets.add(item);
						break;
				}
				return 0;
			} else {
				itemCost = getLowestPrice(item.getId().toUpperCase(), item.getName());
			}
		} catch (Exception ignored) {}

		try {
			itemCount = item.getCount();
		} catch (Exception ignored) {}

		try {
			if (item.isRecombobulated() && (itemCost * 2 >= recombPrice)) {
				recombobulatedExtra = recombPrice;
			}
		} catch (Exception ignored) {}

		try {
			hbpExtras = item.getHbpCount() * hbpPrice;
		} catch (Exception ignored) {}

		try {
			fumingExtras = item.getFumingCount() * fumingPrice;
		} catch (Exception ignored) {}

		StringBuilder enchStr = new StringBuilder("[");
		try {
			List<String> enchants = item.getEnchantsFormatted();
			for (String enchant : enchants) {
				try {
					if (item.getDungeonFloor() != -1 && enchant.equalsIgnoreCase("scavenger;5")) {
						continue;
					}

					double enchantPrice = getLowestPriceEnchant(enchant.toUpperCase());
					enchantsExtras += enchantPrice;
					enchStr
						.append("{\"type\":\"")
						.append(enchant)
						.append("\",\"price\":\"")
						.append(simplifyNumber(enchantPrice))
						.append("\"},");
				} catch (Exception ignored) {}
			}
		} catch (Exception ignored) {}

		if (enchStr.toString().endsWith(",")) {
			enchStr = new StringBuilder(enchStr.substring(0, enchStr.length() - 1));
		}
		enchStr.append("]");

		try {
			reforgeExtras = calculateReforgePrice(item.getModifier(), item.getRarity());
		} catch (Exception ignored) {}

		StringBuilder miscStr = new StringBuilder("[");
		try {
			List<String> extraStats = item.getExtraStats();
			for (String extraItem : extraStats) {
				double miscPrice = getLowestPrice(extraItem, " ");
				miscExtras += miscPrice;
				miscStr.append("{\"name\":\"").append(extraItem).append("\",\"price\":\"").append(simplifyNumber(miscPrice)).append("\"},");
			}
		} catch (Exception ignored) {}

		if (miscStr.toString().endsWith(",")) {
			miscStr = new StringBuilder(miscStr.substring(0, miscStr.length() - 1));
		}
		miscStr.append("]");

		StringBuilder bpStr = new StringBuilder("[");
		try {
			List<InvItem> backpackItems = item.getBackpackItems();
			for (InvItem backpackItem : backpackItems) {
				NwItemPrice bpItemPrice = calculateBpItemPrice(backpackItem, location);
				backpackExtras += bpItemPrice.getPrice();
				bpStr.append(bpItemPrice.getJson() != null ? bpItemPrice.getJson() : "");
			}
		} catch (Exception ignored) {}
		bpStr.append("]");

		double totalPrice =
			itemCount *
			(itemCost + recombobulatedExtra + hbpExtras + enchantsExtras + fumingExtras + reforgeExtras + miscExtras + backpackExtras);

		if (verbose) {
			calcItemsJsonStr.append("{");
			calcItemsJsonStr.append("\"name\":\"").append(item.getName()).append("\"");
			calcItemsJsonStr.append(",\"id\":\"").append(item.getId()).append("\"");
			calcItemsJsonStr.append(",\"total\":\"").append(simplifyNumber(totalPrice)).append("\"");
			calcItemsJsonStr.append(",\"count\":").append(itemCount);
			calcItemsJsonStr.append(",\"base_cost\":\"").append(simplifyNumber(itemCost)).append("\"");
			calcItemsJsonStr.append(recombobulatedExtra > 0 ? ",\"recomb\":\"" + simplifyNumber(recombobulatedExtra) + "\"" : "");
			calcItemsJsonStr.append(hbpExtras > 0 ? ",\"hbp\":\"" + simplifyNumber(hbpExtras) + "\"" : "");
			calcItemsJsonStr.append(
				enchantsExtras > 0
					? ",\"enchants\":{\"total\":\"" + simplifyNumber(enchantsExtras) + "\",\"enchants\":" + enchStr + "}"
					: ""
			);
			calcItemsJsonStr.append(fumingExtras > 0 ? ",\"fuming\":\"" + simplifyNumber(fumingExtras) + "\"" : "");
			calcItemsJsonStr.append(
				reforgeExtras > 0
					? ",\"reforge\":{\"cost\":\"" + simplifyNumber(reforgeExtras) + "\",\"name\":\"" + item.getModifier() + "\"}"
					: ""
			);
			calcItemsJsonStr.append(
				miscExtras > 0 ? ",\"misc\":{\"total\":\"" + simplifyNumber(miscExtras) + "\",\"miscs\":" + miscStr + "}" : ""
			);
			calcItemsJsonStr.append(
				backpackExtras > 0 ? ",\"bp\":{\"cost\":\"" + simplifyNumber(backpackExtras) + "\",\"bp\":" + bpStr + "}" : ""
			);

			calcItemsJsonStr.append(",\"nbt_tag\":\"").append(parseMcCodes(item.getNbtTag().replace("\"", "\\\""))).append("\"");
			calcItemsJsonStr.append("},");
		}

		return totalPrice;
	}

	private NwItemPrice calculateBpItemPrice(InvItem item, String location) {
		if (item == null || item.getId().equals("None")) {
			return new NwItemPrice(0, null);
		}

		if (getPriceOverride(item.getId()) == 0) {
			return new NwItemPrice(0, null);
		}

		double itemCost = 0;
		double itemCount = 1;
		double recombobulatedExtra = 0;
		double hbpExtras = 0;
		double enchantsExtras = 0;
		double fumingExtras = 0;
		double reforgeExtras = 0;
		double miscExtras = 0;

		try {
			if (item.getId().equals("PET") && location != null) {
				switch (location) {
					case "inventory":
						invPets.add(item);
						break;
					case "pets":
						petsPets.add(item);
						break;
					case "enderchest":
						enderChestPets.add(item);
						break;
					case "storage":
						storagePets.add(item);
						break;
				}
				return new NwItemPrice(0, null);
			} else {
				itemCost = getLowestPrice(item.getId().toUpperCase(), item.getName());
			}
		} catch (Exception ignored) {}

		try {
			itemCount = item.getCount();
		} catch (Exception ignored) {}

		try {
			if (item.isRecombobulated() && (itemCost * 2 >= recombPrice)) {
				recombobulatedExtra = recombPrice;
			}
		} catch (Exception ignored) {}

		try {
			hbpExtras = item.getHbpCount() * hbpPrice;
		} catch (Exception ignored) {}

		try {
			fumingExtras = item.getFumingCount() * fumingPrice;
		} catch (Exception ignored) {}

		StringBuilder enchStr = new StringBuilder("[");
		try {
			List<String> enchants = item.getEnchantsFormatted();
			for (String enchant : enchants) {
				try {
					if (item.getDungeonFloor() != -1 && enchant.equalsIgnoreCase("scavenger;5")) {
						continue;
					}

					double enchantPrice = getLowestPriceEnchant(enchant.toUpperCase());
					enchantsExtras += enchantPrice;
					enchStr
						.append("{\"type\":\"")
						.append(enchant)
						.append("\",\"price\":\"")
						.append(simplifyNumber(enchantPrice))
						.append("\"},");
				} catch (Exception ignored) {}
			}
		} catch (Exception ignored) {}

		if (enchStr.toString().endsWith(",")) {
			enchStr = new StringBuilder(enchStr.substring(0, enchStr.length() - 1));
		}
		enchStr.append("]");

		try {
			reforgeExtras = calculateReforgePrice(item.getModifier(), item.getRarity());
		} catch (Exception ignored) {}

		StringBuilder miscStr = new StringBuilder("[");
		try {
			List<String> extraStats = item.getExtraStats();
			for (String extraItem : extraStats) {
				double miscPrice = getLowestPrice(extraItem, "");
				miscExtras += miscPrice;
				miscStr.append("{\"name\":\"").append(extraItem).append("\",\"price\":\"").append(simplifyNumber(miscPrice)).append("\"},");
			}
		} catch (Exception ignored) {}

		if (miscStr.toString().endsWith(",")) {
			miscStr = new StringBuilder(miscStr.substring(0, miscStr.length() - 1));
		}
		miscStr.append("]");

		double totalPrice =
			itemCount * (itemCost + recombobulatedExtra + hbpExtras + enchantsExtras + fumingExtras + reforgeExtras + miscExtras);

		String jsonStr = "";
		if (verbose) {
			jsonStr += "{";
			jsonStr += "\"name\":\"" + item.getName() + "\"";
			jsonStr += ",\"id\":\"" + item.getId() + "\"";
			jsonStr += ",\"total\":\"" + simplifyNumber(totalPrice) + "\"";
			jsonStr += ",\"count\":" + itemCount;
			jsonStr += ",\"base_cost\":\"" + simplifyNumber(itemCost) + "\"";
			jsonStr += recombobulatedExtra > 0 ? ",\"recomb\":\"" + simplifyNumber(recombobulatedExtra) + "\"" : "";
			jsonStr += (hbpExtras > 0 ? ",\"hbp\":\"" + simplifyNumber(hbpExtras) + "\"" : "");
			jsonStr +=
				(
					enchantsExtras > 0
						? ",\"enchants\":{\"total\":\"" + simplifyNumber(enchantsExtras) + "\",\"enchants\":" + enchStr + "}"
						: ""
				);
			jsonStr += fumingExtras > 0 ? ",\"fuming\":\"" + simplifyNumber(fumingExtras) + "\"" : "";
			jsonStr +=
				reforgeExtras > 0
					? ",\"reforge\":{\"cost\":\"" + simplifyNumber(reforgeExtras) + "\",\"name\":\"" + item.getModifier() + "\"}"
					: "";
			jsonStr += miscExtras > 0 ? ",\"misc\":{\"total\":\"" + simplifyNumber(miscExtras) + "\",\"miscs\":" + miscStr + "}" : "";

			jsonStr += ",\"nbt_tag\":\"" + parseMcCodes(item.getNbtTag().replace("\"", "\\\"")) + "\"";
			jsonStr += "},";
		}

		return new NwItemPrice(totalPrice, jsonStr);
	}

	private double calculateReforgePrice(String reforgeName, String itemRarity) {
		JsonElement reforgesStonesJson = getReforgeStonesJson();

		for (String reforgeStone : REFORGE_STONE_NAMES) {
			JsonElement reforgeStoneInfo = higherDepth(reforgesStonesJson, reforgeStone);
			if (higherDepth(reforgeStoneInfo, "reforgeName").getAsString().equalsIgnoreCase(reforgeName)) {
				String reforgeStoneId = higherDepth(reforgeStoneInfo, "internalName").getAsString();
				double reforgeStoneCost = getLowestPrice(reforgeStoneId, idToName(reforgeStoneId));
				double reforgeApplyCost = higherDepth(reforgeStoneInfo, "reforgeCosts." + itemRarity.toUpperCase()).getAsLong();
				return reforgeStoneCost + reforgeApplyCost;
			}
		}

		return 0;
	}

	private double getLowestPriceEnchant(String enchantId) {
		double lowestBin = -1;
		double averageAuction = -1;
		String enchantName = enchantId.split(";")[0];
		int enchantLevel = Integer.parseInt(enchantId.split(";")[1]);

		if (
			enchantName.equalsIgnoreCase("compact") ||
			enchantName.equalsIgnoreCase("expertise") ||
			enchantName.equalsIgnoreCase("cultivating")
		) {
			enchantLevel = 1;
		}

		for (int i = enchantLevel; i >= 1; i--) {
			try {
				lowestBin = higherDepth(lowestBinJson, enchantName + ";" + i).getAsDouble();
			} catch (Exception ignored) {}

			try {
				JsonElement avgInfo = higherDepth(averageAuctionJson, enchantName + ";" + i);
				averageAuction = getMin(higherDepth(avgInfo, "clean_price", -1.0), higherDepth(avgInfo, "price", -1.0));
			} catch (Exception ignored) {}

			double min = getMin(Math.pow(2, enchantLevel - i) * lowestBin, Math.pow(2, enchantLevel - i) * averageAuction);
			if (min != -1) {
				return min;
			}
		}

		for (JsonElement sbzPrice : sbzPrices) {
			String sbzItemName = higherDepth(sbzPrice, "name").getAsString();
			if (
				sbzItemName.equalsIgnoreCase(enchantName + "_" + enchantLevel) ||
				sbzItemName.equalsIgnoreCase(enchantName + "_" + toRomanNumerals(enchantLevel))
			) {
				return higherDepth(sbzPrice, "low").getAsLong();
			}
		}

		tempSet.add(enchantId);
		return 0;
	}

	private double getLowestPrice(String itemId, String iName) {
		return getLowestPrice(itemId, iName, false);
	}

	private double getLowestPrice(String itemId, String iName, boolean onlyBazaar) {
		double priceOverride = getPriceOverride(itemId);
		if (priceOverride != -1) {
			return priceOverride;
		}

		try {
			return Math.max(higherDepth(bazaarJson, itemId + ".sell_summary.[0].pricePerUnit").getAsDouble(), 0);
		} catch (Exception ignored) {}

		if (!onlyBazaar) {
			double lowestBin = -1;
			double averageAuction = -1;

			try {
				lowestBin = higherDepth(lowestBinJson, itemId).getAsDouble();
			} catch (Exception ignored) {}

			try {
				JsonElement avgInfo = higherDepth(averageAuctionJson, itemId);
				averageAuction = getMin(higherDepth(avgInfo, "clean_price", -1.0), higherDepth(avgInfo, "price", -1.0));
			} catch (Exception ignored) {}

			double minBinAverage = getMin(lowestBin, averageAuction);
			if (minBinAverage != -1) {
				return minBinAverage;
			}

			try {
				itemId = itemId.toLowerCase();
				switch (itemId) {
					case "magic_mushroom_soup":
						itemId = "magical_mushroom_soup";
						break;
					case "mine_talisman":
						itemId = "mine_affinity_talisman";
						break;
					case "village_talisman":
						itemId = "village_affinity_talisman";
						break;
					case "coin_talisman":
						itemId = "talisman_of_coins";
						break;
					case "melody_hair":
						itemId = "melodys_hair";
						break;
					case "theoretical_hoe":
						itemId = "mathematical_hoe_blueprint";
						break;
					case "dctr_space_helm":
						itemId = "dctrs_space_helmet";
						break;
					default:
						if (itemId.contains("generator")) {
							String minionName = itemId.split("_generator_")[0];
							int level = Integer.parseInt(itemId.split("_generator_")[1]);
							itemId = minionName + "_minion_" + toRomanNumerals(level);
						} else if (itemId.startsWith("theoretical_hoe_")) {
							String parseHoe = itemId.split("theoretical_hoe_")[1];
							String hoeType = parseHoe.split("_")[0];
							int hoeLevel = Integer.parseInt(parseHoe.split("_")[1]);

							for (JsonElement itemPrice : sbzPrices) {
								String itemNamePrice = higherDepth(itemPrice, "name").getAsString();
								if (itemNamePrice.startsWith("tier_" + hoeLevel) && itemNamePrice.endsWith(hoeType + "_hoe")) {
									return Math.max(higherDepth(itemPrice, "low").getAsDouble(), 0);
								}
							}
						}
						break;
				}

				for (JsonElement itemPrice : sbzPrices) {
					String itemName = higherDepth(itemPrice, "name").getAsString();
					if (itemName.equalsIgnoreCase(itemId) || itemName.equalsIgnoreCase(itemId.toLowerCase().replace(" ", "_"))) {
						return Math.max(higherDepth(itemPrice, "low").getAsDouble(), 0);
					}
				}
			} catch (Exception ignored) {}
		}

		tempSet.add(itemId + " - " + iName);
		return 0;
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
}
