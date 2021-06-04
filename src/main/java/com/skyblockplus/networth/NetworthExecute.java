package com.skyblockplus.networth;

import static com.skyblockplus.utils.Utils.*;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.skyblockplus.utils.Player;
import com.skyblockplus.utils.structs.InvItem;
import com.skyblockplus.utils.structs.NwItemPrice;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.*;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;

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
	private JsonElement lowestBinJson;
	private JsonElement averageAuctionJson;
	private JsonElement bazaarJson;
	private JsonArray sbzPrices;
	private int failedCount = 0;
	private double enderChestTotal = 0;
	private double petsTotal = 0;
	private double invTotal = 0;
	private double bankBalance = 0;
	private double purseCoins = 0;
	private double wardrobeTotal = 0;
	private double talismanTotal = 0;
	private double invArmor = 0;
	private double storageTotal = 0;
	private String calcItemsJsonStr = "[";
	private boolean verbose = false;

	public void execute(CommandEvent event) {
		new Thread(
			() -> {
				EmbedBuilder eb = loadingEmbed();
				Message ebMessage = event.getChannel().sendMessage(eb.build()).complete();
				String content = event.getMessage().getContentRaw();
				logCommand(event.getGuild(), event.getAuthor(), content);

				if (content.contains("verbose:true")) {
					verbose = true;
					content = content.replace("verbose:true", "").trim();
				}

				String[] args = content.split(" ");

				if (args.length == 2) {
					EmbedBuilder nwEb = getPlayerNetworth(args[1], null);

					if (verbose) {
						try {
							if (calcItemsJsonStr.endsWith(",")) {
								calcItemsJsonStr = calcItemsJsonStr.substring(0, calcItemsJsonStr.length() - 1);
							}
							calcItemsJsonStr += "]";

							nwEb.appendDescription(
								"\nVerbose JSON: " +
								makeHastePost(
									new GsonBuilder().setPrettyPrinting().create().toJson(JsonParser.parseString(calcItemsJsonStr))
								) +
								".json"
							);
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
					ebMessage.editMessage(nwEb.build()).queue();
					return;
				} else if (args.length == 3) {
					EmbedBuilder nwEb = getPlayerNetworth(args[1], args[2]);

					if (verbose) {
						try {
							if (calcItemsJsonStr.endsWith(",")) {
								calcItemsJsonStr = calcItemsJsonStr.substring(0, calcItemsJsonStr.length() - 1);
							}
							calcItemsJsonStr += "]";

							nwEb.appendDescription(
								"\nVerbose JSON: " +
								makeHastePost(
									new GsonBuilder().setPrettyPrinting().create().toJson(JsonParser.parseString(calcItemsJsonStr))
								) +
								".json"
							);
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
					ebMessage.editMessage(nwEb.build()).queue();
					return;
				}

				ebMessage.editMessage(errorMessage("networth").build()).queue();
			}
		)
			.start();
	}

	private EmbedBuilder getPlayerNetworth(String username, String profileName) {
		Player player = profileName == null ? new Player(username) : new Player(username, profileName);
		if (player.isValid()) {
			EmbedBuilder eb = player.defaultPlayerEmbed();

			lowestBinJson = getJson("https://moulberry.codes/lowestbin.json");
			averageAuctionJson = getJson("https://moulberry.codes/auction_averages/3day.json");
			bazaarJson = higherDepth(getJson("https://api.hypixel.net/skyblock/bazaar"), "products");
			sbzPrices = getJson("https://raw.githubusercontent.com/skyblockz/pricecheckbot/master/data.json").getAsJsonArray();

			bankBalance = player.getBankBalance();
			purseCoins = player.getPurseCoins();

			Map<Integer, InvItem> playerInventory = player.getInventoryMap();
			if (playerInventory == null) {
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
			for (InvItem item : playerTalismans.values()) {
				double itemPrice = calculateItemPrice(item);
				talismanTotal += itemPrice;
				if (item != null) {
					talismanItems.add(addItemStr(item, itemPrice));
				}
			}

			Map<Integer, InvItem> invArmorMap = player.getInventoryArmorMap();
			for (InvItem item : invArmorMap.values()) {
				double itemPrice = calculateItemPrice(item);
				invArmor += itemPrice;
				if (item != null) {
					armorItems.add(addItemStr(item, itemPrice));
				}
			}

			Map<Integer, InvItem> wardrobeMap = player.getWardrobeMap();
			for (InvItem item : wardrobeMap.values()) {
				double itemPrice = calculateItemPrice(item);
				wardrobeTotal += itemPrice;
				if (item != null) {
					wardrobeItems.add(addItemStr(item, itemPrice));
				}
			}

			List<InvItem> petsMap = player.getPetsMapNames();
			for (InvItem item : petsMap) {
				petsTotal += calculateItemPrice(item, "pets");
			}

			Map<Integer, InvItem> enderChest = player.getEnderChestMap();
			for (InvItem item : enderChest.values()) {
				double itemPrice = calculateItemPrice(item, "enderchest");
				enderChestTotal += itemPrice;
				if (item != null) {
					enderChestItems.add(addItemStr(item, itemPrice));
				}
			}

			try {
				Map<Integer, InvItem> storageMap = player.getStorageMap();
				for (InvItem item : storageMap.values()) {
					double itemPrice = calculateItemPrice(item, "storage");
					storageTotal += itemPrice;
					if (item != null) {
						storageItems.add(addItemStr(item, itemPrice));
					}
				}
			} catch (Exception ignored) {}

			calculateAllPetsPrice();

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
					echestStr.append("• And more...");
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
					storageStr.append("• And more...");
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
					invStr.append("• And more...");
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
					wardrobeStr.append("• And more...");
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
					petsStr.append("• And more...");
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
					talismanStr.append("• And more...");
					break;
				}
			}

			double totalNetworth =
				bankBalance + purseCoins + invTotal + talismanTotal + invArmor + wardrobeTotal + petsTotal + enderChestTotal + storageTotal;

			eb.setDescription("Total Networth: " + simplifyNumber(totalNetworth) + " (" + formatNumber(totalNetworth) + ")");
			eb.addField("Purse", simplifyNumber(purseCoins), true);
			eb.addField("Bank", (bankBalance == -1 ? "Private" : simplifyNumber(bankBalance)), true);
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
			eb.addField("Talisman | " + simplifyNumber(talismanTotal), talismanStr.length() == 0 ? "Empty" : talismanStr.toString(), false);

			if (failedCount != 0) {
				eb.appendDescription("\nUnable to get " + failedCount + " items");
			}

			tempSet.forEach(System.out::println);

			return eb;
		}
		return defaultEmbed("Unable to fetch player data");
	}

	private void calculateAllPetsPrice() {
		StringBuilder queryStr = new StringBuilder();
		for (InvItem item : invPets) {
			String petName = capitalizeString(item.getName()).replace("lvl", "Lvl");
			queryStr.append("\"").append(petName).append("\",");
		}
		for (InvItem item : petsPets) {
			String petName = capitalizeString(item.getName()).replace("lvl", "Lvl");
			queryStr.append("\"").append(petName).append("\",");
		}
		for (InvItem item : enderChestPets) {
			String petName = capitalizeString(item.getName()).replace("lvl", "Lvl");
			queryStr.append("\"").append(petName).append("\",");
		}

		for (InvItem item : storagePets) {
			String petName = capitalizeString(item.getName()).replace("lvl", "Lvl");
			queryStr.append("\"").append(petName).append("\",");
		}

		if (queryStr.length() == 0) {
			return;
		}

		queryStr = new StringBuilder(queryStr.substring(0, queryStr.length() - 1));

		JsonArray ahQuery = queryAhApi(queryStr.toString());

		if (ahQuery != null) {
			for (JsonElement auction : ahQuery) {
				String auctionName = higherDepth(auction, "item_name").getAsString();
				double auctionPrice = higherDepth(auction, "starting_bid").getAsDouble();
				String auctionRarity = higherDepth(auction, "tier").getAsString();

				for (Iterator<InvItem> iterator = invPets.iterator(); iterator.hasNext();) {
					InvItem item = iterator.next();
					if (item.getName().equalsIgnoreCase(auctionName) && item.getRarity().equalsIgnoreCase(auctionRarity)) {
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
							calcItemsJsonStr +=
								"{\"total\":\"" +
								simplifyNumber(auctionPrice + miscExtras) +
								"\",\"name\":\"" +
								item.getName() +
								"\",\"base_cost\":\"" +
								simplifyNumber(auctionPrice) +
								"\",\"misc\":{\"total\":\"" +
								simplifyNumber(miscExtras) +
								"\",\"miscs\":" +
								miscStr +
								"}},";
						}
						iterator.remove();
					}
				}

				for (Iterator<InvItem> iterator = petsPets.iterator(); iterator.hasNext();) {
					InvItem item = iterator.next();
					if (item.getName().equalsIgnoreCase(auctionName) && item.getRarity().equalsIgnoreCase(auctionRarity)) {
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
							calcItemsJsonStr +=
								"{\"total\":\"" +
								simplifyNumber(auctionPrice + miscExtras) +
								"\",\"name\":\"" +
								item.getName() +
								"\",\"base_cost\":\"" +
								simplifyNumber(auctionPrice) +
								"\",\"misc\":{\"total\":\"" +
								simplifyNumber(miscExtras) +
								"\",\"miscs\":" +
								miscStr +
								"}},";
						}
						iterator.remove();
					}
				}

				for (Iterator<InvItem> iterator = enderChestPets.iterator(); iterator.hasNext();) {
					InvItem item = iterator.next();
					if (item.getName().equalsIgnoreCase(auctionName) && item.getRarity().equalsIgnoreCase(auctionRarity)) {
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
							calcItemsJsonStr +=
								"{\"total\":\"" +
								simplifyNumber(auctionPrice + miscExtras) +
								"\",\"name\":\"" +
								item.getName() +
								"\",\"base_cost\":\"" +
								simplifyNumber(auctionPrice) +
								"\",\"misc\":{\"total\":\"" +
								simplifyNumber(miscExtras) +
								"\",\"miscs\":" +
								miscStr +
								"}},";
						}
						iterator.remove();
					}
				}

				for (Iterator<InvItem> iterator = storagePets.iterator(); iterator.hasNext();) {
					InvItem item = iterator.next();
					if (item.getName().equalsIgnoreCase(auctionName) && item.getRarity().equalsIgnoreCase(auctionRarity)) {
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
							calcItemsJsonStr +=
								"{\"total\":\"" +
								simplifyNumber(auctionPrice + miscExtras) +
								"\",\"name\":\"" +
								item.getName() +
								"\",\"base_cost\":\"" +
								simplifyNumber(auctionPrice) +
								"\",\"misc\":{\"total\":\"" +
								simplifyNumber(miscExtras) +
								"\",\"miscs\":" +
								miscStr +
								"}},";
						}
						iterator.remove();
					}
				}
			}
		}

		Map<String, String> rarityMap = new HashMap<>();
		rarityMap.put("LEGENDARY", ";4");
		rarityMap.put("EPIC", ";3");
		rarityMap.put("RARE", ";2");
		rarityMap.put("UNCOMMON", ";1");
		rarityMap.put("COMMON", ";0");

		for (InvItem item : invPets) {
			try {
				double auctionPrice = higherDepth(
					lowestBinJson,
					item.getName().split("] ")[1].toLowerCase().trim() + rarityMap.get(item.getRarity())
				)
					.getAsDouble();

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
					calcItemsJsonStr +=
						"{\"total\":\"" +
						simplifyNumber(auctionPrice + miscExtras) +
						"\",\"name\":\"" +
						item.getName() +
						"\",\"base_cost\":\"" +
						simplifyNumber(auctionPrice) +
						"\",\"misc\":{\"total\":\"" +
						simplifyNumber(miscExtras) +
						"\",\"miscs\":" +
						miscStr +
						"},\"fail_calc_lvl_cost\":true},";
				}
			} catch (Exception ignored) {}
		}

		for (InvItem item : petsPets) {
			try {
				double auctionPrice = higherDepth(
					lowestBinJson,
					item.getName().split("] ")[1].toLowerCase().trim() + rarityMap.get(item.getRarity())
				)
					.getAsDouble();
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
					calcItemsJsonStr +=
						"{\"total\":\"" +
						simplifyNumber(auctionPrice + miscExtras) +
						"\",\"name\":\"" +
						item.getName() +
						"\",\"base_cost\":\"" +
						simplifyNumber(auctionPrice) +
						"\",\"misc\":{\"total\":\"" +
						simplifyNumber(miscExtras) +
						"\",\"miscs\":" +
						miscStr +
						"},\"fail_calc_lvl_cost\":true},";
				}
			} catch (Exception ignored) {}
		}

		for (InvItem item : enderChestPets) {
			try {
				double auctionPrice = higherDepth(
					lowestBinJson,
					item.getName().split("] ")[1].toLowerCase().trim() + rarityMap.get(item.getRarity())
				)
					.getAsDouble();
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
					calcItemsJsonStr +=
						"{\"total\":\"" +
						simplifyNumber(auctionPrice + miscExtras) +
						"\",\"name\":\"" +
						item.getName() +
						"\",\"base_cost\":\"" +
						simplifyNumber(auctionPrice) +
						"\",\"misc\":{\"total\":\"" +
						simplifyNumber(miscExtras) +
						"\",\"miscs\":" +
						miscStr +
						"},\"fail_calc_lvl_cost\":true},";
				}
			} catch (Exception ignored) {}
		}

		for (InvItem item : storagePets) {
			try {
				double auctionPrice = higherDepth(
					lowestBinJson,
					item.getName().split("] ")[1].toLowerCase().trim() + rarityMap.get(item.getRarity())
				)
					.getAsDouble();
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
					calcItemsJsonStr +=
						"{\"total\":\"" +
						simplifyNumber(auctionPrice + miscExtras) +
						"\",\"name\":\"" +
						item.getName() +
						"\",\"base_cost\":\"" +
						simplifyNumber(auctionPrice) +
						"\",\"misc\":{\"total\":\"" +
						simplifyNumber(miscExtras) +
						"\",\"miscs\":" +
						miscStr +
						"},\"fail_calc_lvl_cost\":true},";
				}
			} catch (Exception ignored) {}
		}
	}

	private String addItemStr(InvItem item, double itemPrice) {
		return (
			(item.getCount() != 1 ? item.getCount() + "x " : "") +
			item.getName() +
			(item.isRecombobulated() ? " <:recombobulator_3000:841689479621378139>" : "") +
			"@split@" +
			itemPrice
		);
	}

	private double calculateItemPrice(InvItem item) {
		return calculateItemPrice(item, null);
	}

	private double calculateItemPrice(InvItem item, String location) {
		if (item == null) {
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
			if (
				item.isRecombobulated() &&
				(
					itemCost *
					2 >=
					higherDepth(higherDepth(bazaarJson, "RECOMBOBULATOR_3000.sell_summary").getAsJsonArray().get(0), "pricePerUnit")
						.getAsDouble()
				)
			) {
				recombobulatedExtra =
					higherDepth(higherDepth(bazaarJson, "RECOMBOBULATOR_3000.sell_summary").getAsJsonArray().get(0), "pricePerUnit")
						.getAsDouble();
			}
		} catch (Exception ignored) {}

		try {
			hbpExtras =
				item.getHbpCount() *
				higherDepth(higherDepth(bazaarJson, "HOT_POTATO_BOOK.sell_summary").getAsJsonArray().get(0), "pricePerUnit").getAsDouble();
		} catch (Exception ignored) {}

		try {
			fumingExtras =
				item.getFumingCount() *
				higherDepth(higherDepth(bazaarJson, "FUMING_POTATO_BOOK.sell_summary").getAsJsonArray().get(0), "pricePerUnit")
					.getAsDouble();
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
				backpackExtras += bpItemPrice.price;
				bpStr.append(bpItemPrice.json != null ? bpItemPrice.json : "");
			}
		} catch (Exception ignored) {}
		bpStr.append("]");

		double totalPrice =
			itemCount *
			(itemCost + recombobulatedExtra + hbpExtras + enchantsExtras + fumingExtras + reforgeExtras + miscExtras + backpackExtras);

		if (verbose) {
			calcItemsJsonStr += "{";
			calcItemsJsonStr += "\"total\":\"" + simplifyNumber(totalPrice) + "\"";
			calcItemsJsonStr += ",\"name\":\"" + item.getName() + "\"";
			calcItemsJsonStr += ",\"id\":\"" + item.getId() + "\"";
			calcItemsJsonStr += ",\"count\":" + itemCount;
			calcItemsJsonStr += ",\"base_cost\":\"" + simplifyNumber(itemCost) + "\"";
			calcItemsJsonStr += recombobulatedExtra > 0 ? ",\"recomb\":\"" + simplifyNumber(recombobulatedExtra) + "\"" : "";
			calcItemsJsonStr += (hbpExtras > 0 ? ",\"hbp\":\"" + simplifyNumber(hbpExtras) + "\"" : "");
			calcItemsJsonStr +=
				(
					enchantsExtras > 0
						? ",\"enchants\":{\"total\":\"" + simplifyNumber(enchantsExtras) + "\",\"enchants\":" + enchStr + "}"
						: ""
				);
			calcItemsJsonStr += fumingExtras > 0 ? ",\"fuming\":\"" + simplifyNumber(fumingExtras) + "\"" : "";
			calcItemsJsonStr +=
				reforgeExtras > 0
					? ",\"reforge\":{\"cost\":\"" + simplifyNumber(reforgeExtras) + "\",\"name\":\"" + item.getModifier() + "\"}"
					: "";
			calcItemsJsonStr +=
				miscExtras > 0 ? ",\"misc\":{\"total\":\"" + simplifyNumber(miscExtras) + "\",\"miscs\":" + miscStr + "}" : "";
			calcItemsJsonStr +=
				backpackExtras > 0 ? ",\"bp\":{\"cost\":\"" + simplifyNumber(backpackExtras) + "\",\"bp\":" + bpStr + "}" : "";

			calcItemsJsonStr += ",\"nbt_tag\":\"" + parseMcCodes(item.getNbtTag().replace("\"", "\\\"")) + "\"";
			calcItemsJsonStr += "},";
		}

		return totalPrice;
	}

	private NwItemPrice calculateBpItemPrice(InvItem item, String location) {
		if (item == null) {
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
			if (
				item.isRecombobulated() &&
				(
					itemCost *
					2 >=
					higherDepth(higherDepth(bazaarJson, "RECOMBOBULATOR_3000.sell_summary").getAsJsonArray().get(0), "pricePerUnit")
						.getAsDouble()
				)
			) {
				recombobulatedExtra =
					higherDepth(higherDepth(bazaarJson, "RECOMBOBULATOR_3000.sell_summary").getAsJsonArray().get(0), "pricePerUnit")
						.getAsDouble();
			}
		} catch (Exception ignored) {}

		try {
			hbpExtras =
				item.getHbpCount() *
				higherDepth(higherDepth(bazaarJson, "HOT_POTATO_BOOK.sell_summary").getAsJsonArray().get(0), "pricePerUnit").getAsDouble();
		} catch (Exception ignored) {}

		try {
			fumingExtras =
				item.getFumingCount() *
				higherDepth(higherDepth(bazaarJson, "FUMING_POTATO_BOOK.sell_summary").getAsJsonArray().get(0), "pricePerUnit")
					.getAsDouble();
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

		double totalPrice =
			itemCount * (itemCost + recombobulatedExtra + hbpExtras + enchantsExtras + fumingExtras + reforgeExtras + miscExtras);

		String jsonStr = "";
		if (verbose) {
			jsonStr += "{";
			jsonStr += "\"total\":\"" + simplifyNumber(totalPrice) + "\"";
			jsonStr += ",\"name\":\"" + item.getName() + "\"";
			jsonStr += ",\"id\":\"" + item.getId() + "\"";
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
		List<String> reforgeStones = getJsonKeys(reforgesStonesJson);

		for (String reforgeStone : reforgeStones) {
			JsonElement reforgeStoneInfo = higherDepth(reforgesStonesJson, reforgeStone);
			if (higherDepth(reforgeStoneInfo, "reforgeName").getAsString().equalsIgnoreCase(reforgeName)) {
				String reforgeStoneName = higherDepth(reforgeStoneInfo, "internalName").getAsString();
				double reforgeStoneCost = getLowestPrice(reforgeStoneName, " ");
				double reforgeApplyCost = higherDepth(reforgeStoneInfo, "reforgeCosts." + itemRarity.toUpperCase()).getAsDouble();
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
				averageAuction =
					higherDepth(avgInfo, "clean_price") != null
						? higherDepth(avgInfo, "clean_price").getAsDouble()
						: higherDepth(avgInfo, "price").getAsDouble();
			} catch (Exception ignored) {}

			if (lowestBin == -1 && averageAuction != -1) {
				return Math.pow(2, enchantLevel - i) * averageAuction;
			} else if (lowestBin != -1 && averageAuction == -1) {
				return Math.pow(2, enchantLevel - i) * lowestBin;
			} else if (lowestBin != -1 && averageAuction != -1) {
				return (Math.pow(2, enchantLevel - i) * Math.min(lowestBin, averageAuction));
			}
		}

		if (higherDepth(sbzPrices, enchantName + "_1") != null) {
			return (Math.pow(2, enchantLevel - 1) * higherDepth(sbzPrices, enchantName + "_1").getAsDouble());
		}

		if (higherDepth(sbzPrices, enchantName + "_i") != null) {
			return (Math.pow(2, enchantLevel - 1) * higherDepth(sbzPrices, enchantName + "_i").getAsDouble());
		}

		tempSet.add(enchantId);
		return 0;
	}

	private double getLowestPrice(String itemId, String tempName) {
		if (isIgnoredItem(itemId)) {
			return 0;
		}

		double lowestBin = -1;
		double averageAuction = -1;

		try {
			lowestBin = higherDepth(lowestBinJson, itemId).getAsDouble();
		} catch (Exception ignored) {}

		try {
			JsonElement avgInfo = higherDepth(averageAuctionJson, itemId);
			averageAuction =
				higherDepth(avgInfo, "clean_price") != null
					? higherDepth(avgInfo, "clean_price").getAsDouble()
					: higherDepth(avgInfo, "price").getAsDouble();
		} catch (Exception ignored) {}

		if (lowestBin == -1 && averageAuction != -1) {
			return averageAuction;
		} else if (lowestBin != -1 && averageAuction == -1) {
			return lowestBin;
		} else if (lowestBin != -1 && averageAuction != -1) {
			return Math.min(lowestBin, averageAuction);
		}

		try {
			return higherDepth(higherDepth(bazaarJson, itemId + "sell_summary").getAsJsonArray().get(0), "pricePerUnit").getAsDouble();
		} catch (Exception ignored) {}

		try {
			itemId = itemId.toLowerCase();
			if (itemId.contains("generator")) {
				String minionName = itemId.split("_generator_")[0];
				int level = Integer.parseInt(itemId.split("_generator_")[1]);

				itemId = minionName + "_minion_" + toRomanNumerals(level);
			} else if (itemId.equals("magic_mushroom_soup")) {
				itemId = "magical_mushroom_soup";
			} else if (itemId.startsWith("theoretical_hoe_")) {
				String parseHoe = itemId.split("theoretical_hoe_")[1];
				String hoeType = parseHoe.split("_")[0];
				int hoeLevel = Integer.parseInt(parseHoe.split("_")[1]);

				for (JsonElement itemPrice : sbzPrices) {
					String itemNamePrice = higherDepth(itemPrice, "name").getAsString();
					if (itemNamePrice.startsWith("tier_" + hoeLevel) && itemNamePrice.endsWith(hoeType + "_hoe")) {
						return higherDepth(itemPrice, "low").getAsDouble();
					}
				}
			} else if (itemId.equals("mine_talisman")) {
				itemId = "mine_affinity_talisman";
			} else if (itemId.equals("village_talisman")) {
				itemId = "village_affinity_talisman";
			} else if (itemId.equals("coin_talisman")) {
				itemId = "talisman_of_coins";
			} else if (itemId.equals("melody_hair")) {
				itemId = "melodys_hair";
			} else if (itemId.equals("theoretical_hoe")) {
				itemId = "mathematical_hoe_blueprint";
			} else if (itemId.equals("dctr_space_helm")) {
				itemId = "dctrs_space_helmet";
			}

			for (JsonElement itemPrice : sbzPrices) {
				if (higherDepth(itemPrice, "name").getAsString().equalsIgnoreCase(itemId)) {
					return higherDepth(itemPrice, "low").getAsDouble();
				}
			}
		} catch (Exception ignored) {}

		tempSet.add(itemId + " - " + tempName);
		failedCount++;
		return 0;
	}

	private boolean isIgnoredItem(String s) {
		if (s.equalsIgnoreCase("none")) {
			return true;
		}

		if (s.startsWith("stained_glass")) {
			return true;
		}

		if (s.equalsIgnoreCase("redstone_torch_on")) {
			return true;
		}

		if (s.equalsIgnoreCase("book")) {
			return true;
		}

		if (s.equalsIgnoreCase("smooth_brick")) {
			return true;
		}

		if (s.equalsIgnoreCase("zombie_talisman")) {
			return true;
		}

		if (s.equalsIgnoreCase("zombie_commander_whip")) {
			return true;
		}

		return s.equals("skyblock_menu");
	}

	private JsonArray queryAhApi(String query) {
		CloseableHttpClient httpclient = HttpClientBuilder.create().build();
		try {
			HttpGet httpget = new HttpGet("https://api.eastarcti.ca/auctions/");
			httpget.addHeader("content-type", "application/json; charset=UTF-8");

			URI uri = new URIBuilder(httpget.getURI())
				.addParameter("query", "{\"item_name\":{\"$in\":[" + query + "]},\"bin\":true}")
				.addParameter("sort", "{\"starting_bid\":1}")
				.build();
			httpget.setURI(uri);

			HttpResponse httpresponse = httpclient.execute(httpget);
			return JsonParser.parseReader(new InputStreamReader(httpresponse.getEntity().getContent())).getAsJsonArray();
		} catch (Exception ignored) {} finally {
			try {
				httpclient.close();
			} catch (Exception e) {
				System.out.println("== Stack Trace (Nw Query Close Http Client) ==");
				e.printStackTrace();
			}
		}
		return null;
	}
}
