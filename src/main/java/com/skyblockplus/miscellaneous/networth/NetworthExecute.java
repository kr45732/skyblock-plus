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
import static com.skyblockplus.utils.ApiHandler.leaderboardDatabase;
import static com.skyblockplus.utils.Constants.*;
import static com.skyblockplus.utils.Utils.*;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.skyblockplus.utils.Player;
import com.skyblockplus.utils.command.*;
import com.skyblockplus.utils.structs.InvItem;
import java.util.*;
import java.util.stream.Collectors;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.selections.SelectOption;
import org.apache.groovy.util.Maps;

public class NetworthExecute {

	private static final Map<String, Integer> essencePrices = Maps.of(
		"WITHER",
		3500,
		"UNDEAD",
		800,
		"DIAMOND",
		4000,
		"GOLD",
		3500,
		"ICE",
		4000,
		"SPIDER",
		3000,
		"DRAGON",
		750
	);
	//	private final Set<String> tempSet = new HashSet<>();
	private final List<InvItem> invPets = new ArrayList<>();
	private final List<InvItem> petsPets = new ArrayList<>();
	private final List<InvItem> enderChestPets = new ArrayList<>();
	private final List<InvItem> storagePets = new ArrayList<>();
	private final List<InvItem> personalVaultPets = new ArrayList<>();
	private final List<String> enderChestItems = new ArrayList<>();
	private final List<String> petsItems = new ArrayList<>();
	private final List<String> invItems = new ArrayList<>();
	private final List<String> wardrobeItems = new ArrayList<>();
	private final List<String> talismanItems = new ArrayList<>();
	private final List<String> armorItems = new ArrayList<>();
	private final List<String> storageItems = new ArrayList<>();
	private final List<String> personalVaultItems = new ArrayList<>();
	private final List<String> sacksItems = new ArrayList<>();
	private StringBuilder calcItemsJsonStr = new StringBuilder("[");
	private JsonElement lowestBinJson;
	private JsonElement averageAuctionJson;
	private JsonElement bazaarJson;
	private JsonArray sbzPrices;
	private double enderChestTotal = 0;
	private double personalVaultTotal = 0;
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

	public static double getTotalNetworth(String username, String profileName) {
		NetworthExecute calc = new NetworthExecute().setOnlyTotal(true);
		calc.getPlayerNetworth(username, profileName);
		return calc.getTotalCalculatedNetworth();
	}

	public static double getTotalNetworth(Player player) {
		NetworthExecute calc = new NetworthExecute().setOnlyTotal(true);
		calc.getPlayerNetworth(player);
		return calc.getTotalCalculatedNetworth();
	}

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
				verbose = getBooleanOption("--verbose");

				if (args.length == 3 || args.length == 2 || args.length == 1) {
					if (getMentionedUsername(args.length == 1 ? -1 : 1)) {
						return;
					}

					paginate(getPlayerNetworth(player, args.length == 3 ? args[2] : null, getPaginatorEvent()));
					return;
				}

				sendErrorEmbed();
			}
		}
			.queue();
	}

	public void getPlayerNetworth(String username, String profileName) {
		getPlayerNetworth(username, profileName, null);
	}

	public Object getPlayerNetworth(String username, String profileName, PaginatorEvent event) {
		return getPlayerNetworth(profileName == null ? new Player(username) : new Player(username, profileName), event);
	}

	public void getPlayerNetworth(Player player) {
		getPlayerNetworth(player, null);
	}

	public Object getPlayerNetworth(Player player, PaginatorEvent event) {
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
				return defaultEmbed(player.getUsernameFixed() + "'s inventory API is disabled");
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

			Map<Integer, InvItem> invArmorMap = player.getArmorMap();
			if (invArmorMap != null) {
				for (InvItem item : invArmorMap.values()) {
					double itemPrice = calculateItemPrice(item);
					invArmor += itemPrice;
					if (item != null) {
						armorItems.add(addItemStr(item, itemPrice));
					}
				}
			}
			Map<Integer, InvItem> equipmentMap = player.getEquipmentMap();
			if (equipmentMap != null) {
				for (InvItem item : equipmentMap.values()) {
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

			List<InvItem> petsMap = player.getPetsMap();
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

			Map<Integer, InvItem> personalVault = player.getPersonalVaultMap();
			if (personalVault != null) {
				for (InvItem item : personalVault.values()) {
					double itemPrice = calculateItemPrice(item, "personal_vault");
					personalVaultTotal += itemPrice;
					if (item != null) {
						personalVaultItems.add(addItemStr(item, itemPrice));
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
						double itemPrice = getLowestPrice(sackEntry.getKey(), true) * sackEntry.getValue();
						sacksTotal += itemPrice;
						String emoji = higherDepth(getEmojiMap(), sackEntry.getKey(), null);
						sacksItems.add(
							(emoji == null ? "" : emoji + " ") +
							(sackEntry.getValue() != 1 ? sackEntry.getValue() + "x " : "") +
							idToName(sackEntry.getKey()) +
							"=:=" +
							itemPrice
						);
					}
				}
			}

			calculatePetPrices();

			if (onlyTotal) {
				return invalidEmbed("Only total is enabled");
			}

			enderChestItems.sort(Comparator.comparingDouble(item -> -Double.parseDouble(item.split("=:=")[1])));
			StringBuilder echestStr = new StringBuilder();
			for (int i = 0; i < enderChestItems.size(); i++) {
				String item = enderChestItems.get(i);
				echestStr
					.append(item.split("=:=")[0])
					.append(" ➜ ")
					.append(simplifyNumber(Double.parseDouble(item.split("=:=")[1])))
					.append("\n");
				if (i == 4) {
					echestStr.append("\n");
				} else if (i == 24) {
					int moreItems = enderChestItems.size() - 25;
					if (moreItems > 0) {
						echestStr.append("... ").append(moreItems).append(" more item").append(moreItems > 1 ? "s" : "");
					}
					break;
				}
			}

			sacksItems.sort(Comparator.comparingDouble(item -> -Double.parseDouble(item.split("=:=")[1])));
			StringBuilder sacksStr = new StringBuilder();
			for (int i = 0; i < sacksItems.size(); i++) {
				String item = sacksItems.get(i);
				sacksStr
					.append(item.split("=:=")[0])
					.append(" ➜ ")
					.append(simplifyNumber(Double.parseDouble(item.split("=:=")[1])))
					.append("\n");
				if (i == 24) {
					int moreItems = sacksItems.size() - 25;
					if (moreItems > 0) {
						sacksStr.append("... ").append(moreItems).append(" more item").append(moreItems > 1 ? "s" : "");
					}
					break;
				}
			}

			personalVaultItems.sort(Comparator.comparingDouble(item -> -Double.parseDouble(item.split("=:=")[1])));
			StringBuilder personalVaultStr = new StringBuilder();
			for (int i = 0; i < personalVaultItems.size(); i++) {
				String item = personalVaultItems.get(i);
				personalVaultStr
					.append(item.split("=:=")[0])
					.append(" ➜ ")
					.append(simplifyNumber(Double.parseDouble(item.split("=:=")[1])))
					.append("\n");
				if (i == 4) {
					personalVaultStr.append("\n");
				} else if (i == 24) {
					int moreItems = personalVaultItems.size() - 25;
					if (moreItems > 0) {
						echestStr.append("... ").append(moreItems).append(" more item").append(moreItems > 1 ? "s" : "");
					}
					break;
				}
			}

			storageItems.sort(Comparator.comparingDouble(item -> -Double.parseDouble(item.split("=:=")[1])));
			StringBuilder storageStr = new StringBuilder();
			for (int i = 0; i < storageItems.size(); i++) {
				String item = storageItems.get(i);
				storageStr
					.append(item.split("=:=")[0])
					.append(" ➜ ")
					.append(simplifyNumber(Double.parseDouble(item.split("=:=")[1])))
					.append("\n");
				if (i == 4) {
					storageStr.append("\n");
				} else if (i == 24) {
					int moreItems = storageItems.size() - 25;
					if (moreItems > 0) {
						storageStr.append("... ").append(moreItems).append(" more item").append(moreItems > 1 ? "s" : "");
					}
					break;
				}
			}

			invItems.sort(Comparator.comparingDouble(item -> -Double.parseDouble(item.split("=:=")[1])));
			StringBuilder invStr = new StringBuilder();
			for (int i = 0; i < invItems.size(); i++) {
				String item = invItems.get(i);
				invStr
					.append(item.split("=:=")[0])
					.append(" ➜ ")
					.append(simplifyNumber(Double.parseDouble(item.split("=:=")[1])))
					.append("\n");
				if (i == 4) {
					invStr.append("\n");
				} else if (i == 24) {
					int moreItems = invItems.size() - 25;
					if (moreItems > 0) {
						invStr.append("... ").append(moreItems).append(" more item").append(moreItems > 1 ? "s" : "");
					}
					break;
				}
			}

			armorItems.sort(Comparator.comparingDouble(item -> -Double.parseDouble(item.split("=:=")[1])));
			StringBuilder armorStr = new StringBuilder();
			for (int i = 0; i < armorItems.size(); i++) {
				String item = armorItems.get(i);
				armorStr
					.append(item.split("=:=")[0])
					.append(" ➜ ")
					.append(simplifyNumber(Double.parseDouble(item.split("=:=")[1])))
					.append("\n");
				if (i == 4) {
					armorStr.append("\n");
				} else if (i == 24) {
					int moreItems = armorItems.size() - 25;
					if (moreItems > 0) {
						armorStr.append("... ").append(moreItems).append(" more item").append(moreItems > 1 ? "s" : "");
					}
					break;
				}
			}

			wardrobeItems.sort(Comparator.comparingDouble(item -> -Double.parseDouble(item.split("=:=")[1])));
			StringBuilder wardrobeStr = new StringBuilder();
			for (int i = 0; i < wardrobeItems.size(); i++) {
				String item = wardrobeItems.get(i);
				wardrobeStr
					.append(item.split("=:=")[0])
					.append(" ➜ ")
					.append(simplifyNumber(Double.parseDouble(item.split("=:=")[1])))
					.append("\n");
				if (i == 4) {
					wardrobeStr.append("\n");
				} else if (i == 24) {
					int moreItems = wardrobeItems.size() - 25;
					if (moreItems > 0) {
						wardrobeStr.append("... ").append(moreItems).append(" more item").append(moreItems > 1 ? "s" : "");
					}
					break;
				}
			}

			petsItems.sort(Comparator.comparingDouble(item -> -Double.parseDouble(item.split("=:=")[1])));
			StringBuilder petsStr = new StringBuilder();
			for (int i = 0; i < petsItems.size(); i++) {
				String item = petsItems.get(i);
				petsStr
					.append(item.split("=:=")[0])
					.append(" ➜ ")
					.append(simplifyNumber(Double.parseDouble(item.split("=:=")[1])))
					.append("\n");
				if (i == 4) {
					petsStr.append("\n");
				} else if (i == 24) {
					int moreItems = petsItems.size() - 25;
					if (moreItems > 0) {
						petsStr.append("... ").append(moreItems).append(" more item").append(moreItems > 1 ? "s" : "");
					}
					break;
				}
			}

			talismanItems.sort(Comparator.comparingDouble(item -> -Double.parseDouble(item.split("=:=")[1])));
			StringBuilder talismanStr = new StringBuilder();
			for (int i = 0; i < talismanItems.size(); i++) {
				String item = talismanItems.get(i);
				talismanStr
					.append(item.split("=:=")[0])
					.append(" ➜ ")
					.append(simplifyNumber(Double.parseDouble(item.split("=:=")[1])))
					.append("\n");
				if (i == 4) {
					talismanStr.append("\n");
				} else if (i == 24) {
					int moreItems = talismanItems.size() - 25;
					if (moreItems > 0) {
						talismanStr.append("... ").append(moreItems).append(" more item").append(moreItems > 1 ? "s" : "");
					}
					break;
				}
			}

			if (event == null) {
				return null;
			}

			double totalNetworth = getTotalCalculatedNetworth();
			//			int position = leaderboardDatabase.getNetworthPosition(player.getGamemode(), player.getUuid());
			String ebDesc = "**Total Networth:** " + simplifyNumber(totalNetworth) + " (" + formatNumber(totalNetworth) + ")";
			//			\n**" +
			//				(
			//					Player.Gamemode.IRONMAN_STRANDED.isGamemode(player.getGamemode())
			//						? capitalizeString(player.getGamemode().toString()) + " "
			//						: ""
			//				)+"Leaderboard Position:** " + (position != -1 ? formatNumber(position) : "Not on leaderboard");
			eb.setDescription(ebDesc);
			eb.addField("Purse", simplifyNumber(purseCoins), true);
			eb.addField("Bank", (bankBalance == -1 ? "Private" : simplifyNumber(bankBalance)), true);
			eb.addField("Sacks", simplifyNumber(sacksTotal), true);
			if (!echestStr.isEmpty()) {
				eb.addField("Ender Chest | " + simplifyNumber(enderChestTotal), echestStr.toString().split("\n\n")[0], false);
			}
			if (!storageStr.isEmpty()) {
				eb.addField("Storage | " + simplifyNumber(storageTotal), storageStr.toString().split("\n\n")[0], false);
			}
			if (!invStr.isEmpty()) {
				eb.addField("Inventory | " + simplifyNumber(invTotal), invStr.toString().split("\n\n")[0], false);
			}
			if (!armorStr.isEmpty()) {
				eb.addField("Armor & Equipment | " + simplifyNumber(invArmor), armorStr.toString().split("\n\n")[0], false);
			}
			if (!wardrobeStr.isEmpty()) {
				eb.addField("Wardrobe | " + simplifyNumber(wardrobeTotal), wardrobeStr.toString().split("\n\n")[0], false);
			}
			if (!petsStr.isEmpty()) {
				eb.addField("Pets | " + simplifyNumber(petsTotal), petsStr.toString().split("\n\n")[0], false);
			}
			if (!talismanStr.isEmpty()) {
				eb.addField("Accessories | " + simplifyNumber(talismanTotal), talismanStr.toString().split("\n\n")[0], false);
			}
			if (!personalVaultStr.isEmpty()) {
				eb.addField("Personal Vault | " + simplifyNumber(personalVaultTotal), personalVaultStr.toString().split("\n\n")[0], false);
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
							simplifyNumber(enderChestTotal) +
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
							ebDesc + "\n**Storage:** " + simplifyNumber(storageTotal) + "\n\n" + storageStr.toString().replace("\n\n", "\n")
						)
				);
			}
			if (!invStr.isEmpty()) {
				pages.put(
					SelectOption.of("Inventory", "inventory").withEmoji(getEmojiObj("CHEST")),
					player
						.defaultPlayerEmbed(" | Inventory")
						.setDescription(
							ebDesc + "\n**Inventory:** " + simplifyNumber(invTotal) + "\n\n" + invStr.toString().replace("\n\n", "\n")
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
							simplifyNumber(invArmor) +
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
							simplifyNumber(wardrobeTotal) +
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
							ebDesc + "\n**Pets:** " + simplifyNumber(petsTotal) + "\n\n" + petsStr.toString().replace("\n\n", "\n")
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
							simplifyNumber(talismanTotal) +
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
							simplifyNumber(personalVaultTotal) +
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
						.setDescription(ebDesc + "\n**Sacks:** " + simplifyNumber(sacksTotal) + "\n\n" + sacksStr)
				);
			}

			//			JsonArray missing = collectJsonArray(
			//				tempSet.stream().filter(str -> !str.toLowerCase().startsWith("rune_")).map(JsonPrimitive::new)
			//			);
			//			if (!missing.isEmpty()) {
			//				System.out.println(missing);
			//			}

			if (verbose) {
				try {
					long startTime = System.currentTimeMillis();
					String verboseLink = makeHastePost(formattedGson.toJson(getVerboseJson())) + ".json";
					System.out.println("Verbose time: " + (System.currentTimeMillis() - startTime) + " ms");

					extras.addButton(Button.link(verboseLink, "Verbose JSON"));
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			extras.addButton(Button.link("https://forms.gle/RBmN2AFBLafGyx5E7", "Bug In Calculations?"));
			new SelectMenuPaginator(pages, "overview", extras, event);
			//			event.paginate(paginateBuilder.setPaginatorExtras(extras));
			return null;
		}
		return player.getFailEmbed();
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
			sacksTotal +
			personalVaultTotal;
	}

	private void calculatePetPrices() {
		StringBuilder queryStr = new StringBuilder();
		for (InvItem item : invPets) {
			queryStr.append(item.getPetApiName()).append(",");
		}
		for (InvItem item : petsPets) {
			queryStr.append(item.getPetApiName()).append(",");
		}
		for (InvItem item : enderChestPets) {
			queryStr.append(item.getPetApiName()).append(",");
		}
		for (InvItem item : storagePets) {
			queryStr.append(item.getPetApiName()).append(",");
		}
		for (InvItem item : personalVaultPets) {
			queryStr.append(item.getPetApiName()).append(",");
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

				for (Iterator<InvItem> iterator = personalVaultPets.iterator(); iterator.hasNext();) {
					InvItem item = iterator.next();
					if (item.getPetApiName().equals(auctionName)) {
						StringBuilder miscStr = new StringBuilder("[");
						double miscExtras = 0;
						try {
							List<String> extraStats = item.getExtraStats();
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
						if (miscStr.toString().endsWith(",")) {
							miscStr = new StringBuilder(miscStr.substring(0, miscStr.length() - 1));
						}
						miscStr.append("]");

						personalVaultItems.add(addItemStr(item, auctionPrice + miscExtras));
						personalVaultTotal += auctionPrice + miscExtras;
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

		for (InvItem item : personalVaultPets) {
			double auctionPrice = getMinBinAvg(
				item.getName().split("] ")[1].toUpperCase().replace(" ", "_") + RARITY_TO_NUMBER_MAP.get(item.getRarity())
			);
			if (auctionPrice != -1) {
				StringBuilder miscStr = new StringBuilder("[");
				double miscExtras = 0;
				try {
					List<String> extraStats = item.getExtraStats();
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
				if (miscStr.toString().endsWith(",")) {
					miscStr = new StringBuilder(miscStr.substring(0, miscStr.length() - 1));
				}
				miscStr.append("]");

				personalVaultItems.add(addItemStr(item, auctionPrice + miscExtras));
				personalVaultTotal += auctionPrice + miscExtras;
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

	private double calculateItemPrice(InvItem item, String location, StringBuilder out) {
		if (item == null || item.getName().equalsIgnoreCase("null") || item.getId().equals("None")) {
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
		double essenceExtras = 0;

		try {
			if (item.getId().equals("PET") && location != null) {
				if (!item.getName().startsWith("Mystery ") && !item.getName().equals("Unknown Pet")) {
					switch (location) {
						case "inventory" -> invPets.add(item);
						case "pets" -> petsPets.add(item);
						case "enderchest" -> enderChestPets.add(item);
						case "storage" -> storagePets.add(item);
						case "personal_vault" -> personalVaultPets.add(item);
					}
				}
				return 0;
			} else {
				itemCost = item.getDarkAuctionPrice() != -1 ? item.getDarkAuctionPrice() : getLowestPrice(item.getId().toUpperCase());
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
			fumingExtras = item.getFumingCount() * fumingPrice * 0.66;
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
					if (!item.getId().equals("ENCHANTED_BOOK")) {
						enchantPrice *= enchant.startsWith("ULTIMATE_SOUL_EATER") || enchant.startsWith("OVERLOAD") ? 0.40 : 0.90;
					}
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

		try {
			essenceExtras = item.getEssenceCount() * essencePrices.get(item.getEssenceType());
		} catch (Exception ignored) {}

		StringBuilder miscStr = new StringBuilder("[");
		try {
			List<String> extraStats = item.getExtraStats();
			for (String extraItem : extraStats) {
				double miscPrice = getLowestPrice(extraItem);
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
				backpackExtras += calculateItemPrice(backpackItem, location, bpStr);
			}
		} catch (Exception ignored) {}
		if (bpStr.toString().endsWith(",")) {
			bpStr = new StringBuilder(bpStr.substring(0, bpStr.length() - 1));
		}
		bpStr.append("]");

		double totalPrice =
			itemCount *
			(itemCost + recombobulatedExtra + hbpExtras + enchantsExtras + fumingExtras + reforgeExtras + miscExtras + backpackExtras);

		if (verbose) {
			out.append("{");
			out.append("\"name\":\"").append(item.getName()).append("\"");
			out.append(",\"id\":\"").append(item.getId()).append("\"");
			out.append(",\"total\":\"").append(simplifyNumber(totalPrice)).append("\"");
			out.append(",\"count\":").append(itemCount);
			out.append(",\"base_cost\":\"").append(simplifyNumber(itemCost)).append("\"");
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

	private double calculateReforgePrice(String reforgeName, String itemRarity) {
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

	private double getLowestPriceEnchant(String enchantId) {
		double lowestBin = -1;
		double averageAuction = -1;
		String enchantName = enchantId.split(";")[0];
		int enchantLevel = Integer.parseInt(enchantId.split(";")[1]);
		if (enchantLevel > 10) {
			return 0;
		}

		if (enchantLevel <= IGNORED_ENCHANTS.getOrDefault(enchantName, 0)) {
			return 0;
		}

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

		//		tempSet.add(enchantId);
		return 0;
	}

	public double getLowestPrice(String itemId) {
		return getLowestPrice(itemId, false, true);
	}

	public double getLowestPrice(String itemId, boolean onlyBazaar) {
		return getLowestPrice(itemId, onlyBazaar, true);
	}

	public double getLowestPrice(String itemId, boolean onlyBazaar, boolean useRecipe) {
		double priceOverride = getPriceOverride(itemId);
		if (priceOverride != -1) {
			return priceOverride;
		}
		String iName = idToName(itemId);
		String origItemId = itemId;

		try {
			return Math.max(higherDepth(bazaarJson, itemId + ".sell_summary.[0].pricePerUnit").getAsDouble(), getNpcSellPrice(itemId));
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
							int index = itemId.lastIndexOf("_");
							return getMinionCost(itemId.substring(0, index).toUpperCase(), Integer.parseInt(itemId.substring(index + 1)));
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
					if (itemName.equalsIgnoreCase(itemId) || itemName.equalsIgnoreCase(iName.replace(" ", "_"))) {
						return Math.max(higherDepth(itemPrice, "low").getAsDouble(), 0);
					}
				}
			} catch (Exception ignored) {}
		}

		if (useRecipe && higherDepth(getInternalJsonMappings(), origItemId + ".recipe") != null) {
			double cost = 0;
			for (String item : higherDepth(getInternalJsonMappings(), origItemId + ".recipe")
				.getAsJsonObject()
				.entrySet()
				.stream()
				.map(e -> e.getValue().getAsString())
				.filter(e -> !e.isEmpty())
				.collect(Collectors.toList())) {
				String[] idCountSplit = item.split(":");
				cost += getLowestPrice(idCountSplit[0].replace("-", ":")) * Integer.parseInt(idCountSplit[1]);
			}
			return cost;
		}

		//		tempSet.add(itemId + " - " + iName);
		return Math.max(getNpcSellPrice(itemId), 0);
	}

	public double getMinionCost(String id, int tier) {
		return getMinionCost(id, tier, -1);
	}

	public double getMinionCost(String id, int tier, int depth) {
		if (
			(tier == 1 && (id.equals("FLOWER_GENERATOR") || id.equals("SNOW_GENERATOR"))) || (tier == 12 && !id.equals("FLOWER_GENERATOR"))
		) {
			String finalId = id.split("GENERATOR")[0].toLowerCase() + "minion_" + (tier == 1 ? "i" : "xii");
			return streamJsonArray(sbzPrices)
				.filter(i -> higherDepth(i, "name", "").equals(finalId))
				.map(i -> higherDepth(i, "low", 0))
				.findFirst()
				.orElse(0);
		}

		double cost = 0;
		for (String material : higherDepth(getInternalJsonMappings(), id + "_" + tier + ".recipe")
			.getAsJsonObject()
			.entrySet()
			.stream()
			.map(e -> e.getValue().getAsString())
			.filter(e -> !e.isEmpty())
			.collect(Collectors.toList())) {
			String[] idCountSplit = material.split(":");
			if (idCountSplit[0].contains("GENERATOR")) {
				if (depth - 1 != 0) {
					cost += getMinionCost(idCountSplit[0].substring(0, idCountSplit[0].lastIndexOf("_")), tier - 1, depth - 1);
				}
			} else {
				cost += getLowestPrice(idCountSplit[0].replace("-", ":"), false, false) * Integer.parseInt(idCountSplit[1]);
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
}
