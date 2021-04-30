package com.skyblockplus.networth;

import static com.skyblockplus.utils.Utils.capitalizeString;
import static com.skyblockplus.utils.Utils.defaultEmbed;
import static com.skyblockplus.utils.Utils.errorMessage;
import static com.skyblockplus.utils.Utils.getJson;
import static com.skyblockplus.utils.Utils.getJsonKeys;
import static com.skyblockplus.utils.Utils.getReforgeStonesJson;
import static com.skyblockplus.utils.Utils.higherDepth;
import static com.skyblockplus.utils.Utils.loadingEmbed;
import static com.skyblockplus.utils.Utils.logCommand;
import static com.skyblockplus.utils.Utils.*;

import java.io.InputStreamReader;
import java.net.URI;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.skyblockplus.utils.Player;
import com.skyblockplus.utils.structs.InvItem;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;

public class NetworthExecute {
    private JsonElement lowestBinJson;
    private JsonElement averageAuctionJson;
    private JsonElement bazaarJson;
    private JsonArray sbzPrices;
    private int failedCount = 0;
    private Set<String> tempSet = new HashSet<>();
    private List<InvItem> invPets = new ArrayList<>();
    private List<InvItem> petsPets = new ArrayList<>();
    private List<InvItem> enderChestPets = new ArrayList<>();
    private double enderChestTotal = 0;
    private double petsTotal = 0;
    private double invTotal = 0;
    private double bankBalance = 0;
    private double purseCoins = 0;
    private double wardrobeTotal = 0;
    private double talismanTotal = 0;
    private double invArmor = 0;

    private List<String> enderChestItems = new ArrayList<>();
    private List<String> petsItems = new ArrayList<>();
    private List<String> invItems = new ArrayList<>();
    private List<String> wardrobeItems = new ArrayList<>();
    private List<String> talismanItems = new ArrayList<>();
    private List<String> armorItems = new ArrayList<>();

    public void execute(CommandEvent event) {
        new Thread(() -> {
            EmbedBuilder eb = loadingEmbed();
            Message ebMessage = event.getChannel().sendMessage(eb.build()).complete();
            String content = event.getMessage().getContentRaw();
            String[] args = content.split(" ");

            logCommand(event.getGuild(), event.getAuthor(), content);

            if (args.length == 2) {
                ebMessage.editMessage(getPlayerNetworth(args[1], null).build()).queue();
                return;
            } else if (args.length == 3) {
                ebMessage.editMessage(getPlayerNetworth(args[1], args[2]).build()).queue();
                return;
            }

            ebMessage.editMessage(errorMessage("networth").build()).queue();
        }).start();
    }

    private EmbedBuilder getPlayerNetworth(String username, String profileName) {
        Player player = profileName == null ? new Player(username) : new Player(username, profileName);
        if (player.isValid()) {
            EmbedBuilder eb = player.defaultPlayerEmbed();
            eb.setThumbnail(player.getThumbnailUrl());

            lowestBinJson = getJson("https://moulberry.codes/lowestbin.json");
            averageAuctionJson = getJson("https://moulberry.codes/auction_averages/3day.json");
            bazaarJson = higherDepth(getJson("https://api.hypixel.net/skyblock/bazaar"), "products");
            sbzPrices = getJson("https://raw.githubusercontent.com/skyblockz/pricecheckbot/master/data.json")
                    .getAsJsonArray();

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
                    invItems.add(item.getName() + "@split@" + itemPrice);
                }

            }

            Map<Integer, InvItem> playerTalismans = player.getTalismanBagMap();
            for (InvItem item : playerTalismans.values()) {
                double itemPrice = calculateItemPrice(item);
                talismanTotal += itemPrice;
                if (item != null) {
                    talismanItems.add(item.getName() + "@split@" + itemPrice);
                }
            }

            Map<Integer, InvItem> invArmorMap = player.getInventoryArmorMap();
            for (InvItem item : invArmorMap.values()) {
                double itemPrice = calculateItemPrice(item);
                invArmor += itemPrice;
                if (item != null) {
                    armorItems.add(item.getName() + "@split@" + itemPrice);
                }
            }

            Map<Integer, InvItem> wardrobeMap = player.getWardrobeMap();
            for (InvItem item : wardrobeMap.values()) {
                double itemPrice = calculateItemPrice(item);
                wardrobeTotal += itemPrice;
                if (item != null) {
                    wardrobeItems.add(item.getName() + "@split@" + itemPrice);
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
                    enderChestItems.add(item.getName() + "@split@" + itemPrice);
                }
            }

            calculateAllPetsPrice();

            enderChestItems.sort(Comparator.comparingDouble(item -> -Double.parseDouble(item.split("@split@")[1])));
            String echestStr = "";
            for (int i = 0; i < enderChestItems.size(); i++) {
                String item = enderChestItems.get(i);
                echestStr += "• " + item.split("@split@")[0] + " ➜ "
                        + simplifyNumber(Double.parseDouble(item.split("@split@")[1])) + "\n";
                if (i == 4) {
                    echestStr += "• And more...";
                    break;
                }
            }

            invItems.sort(Comparator.comparingDouble(item -> -Double.parseDouble(item.split("@split@")[1])));
            String invStr = "";
            for (int i = 0; i < invItems.size(); i++) {
                String item = invItems.get(i);
                invStr += "• " + item.split("@split@")[0] + " ➜ "
                        + simplifyNumber(Double.parseDouble(item.split("@split@")[1])) + "\n";
                if (i == 4) {
                    invStr += "• And more...";
                    break;
                }
            }

            armorItems.sort(Comparator.comparingDouble(item -> -Double.parseDouble(item.split("@split@")[1])));
            String armorStr = "";
            for (int i = 0; i < armorItems.size(); i++) {
                String item = armorItems.get(i);
                armorStr += "• " + item.split("@split@")[0] + " ➜ "
                        + simplifyNumber(Double.parseDouble(item.split("@split@")[1])) + "\n";
                if (i == 4) {
                    break;
                }
            }

            wardrobeItems.sort(Comparator.comparingDouble(item -> -Double.parseDouble(item.split("@split@")[1])));
            String wardrobeStr = "";
            for (int i = 0; i < wardrobeItems.size(); i++) {
                String item = wardrobeItems.get(i);
                wardrobeStr += "• " + item.split("@split@")[0] + " ➜ "
                        + simplifyNumber(Double.parseDouble(item.split("@split@")[1])) + "\n";
                if (i == 4) {
                    wardrobeStr += "• And more...";
                    break;
                }
            }

            petsItems.sort(Comparator.comparingDouble(item -> -Double.parseDouble(item.split("@split@")[1])));
            String petsStr = "";
            for (int i = 0; i < petsItems.size(); i++) {
                String item = petsItems.get(i);
                petsStr += "• " + item.split("@split@")[0] + " ➜ "
                        + simplifyNumber(Double.parseDouble(item.split("@split@")[1])) + "\n";
                if (i == 4) {
                    petsStr += "• And more...";
                    break;
                }
            }

            talismanItems.sort(Comparator.comparingDouble(item -> -Double.parseDouble(item.split("@split@")[1])));
            String talismanStr = "";
            for (int i = 0; i < talismanItems.size(); i++) {
                String item = talismanItems.get(i);
                talismanStr += "• " + item.split("@split@")[0] + " ➜ "
                        + simplifyNumber(Double.parseDouble(item.split("@split@")[1])) + "\n";
                if (i == 4) {
                    talismanStr = "• And more...";
                    break;
                }
            }

            double totalNetworth = bankBalance + purseCoins + invTotal + talismanTotal + invArmor + wardrobeTotal
                    + petsTotal + enderChestTotal;

            eb.setDescription(
                    "Total Networth: " + simplifyNumber(totalNetworth) + " (" + formatNumber(totalNetworth) + ")");
            eb.addField("Purse", simplifyNumber(purseCoins), true);
            eb.addField("Bank", (bankBalance == -1 ? "Private" : simplifyNumber(bankBalance)), true);
            eb.addField("Ender Chest | " + simplifyNumber(enderChestTotal), echestStr, false);
            eb.addField("Inventory | " + simplifyNumber(invTotal), invStr, false);
            eb.addField("Armor | " + simplifyNumber(invArmor), armorStr, false);
            eb.addField("Wardrobe | " + simplifyNumber(wardrobeTotal), wardrobeStr, false);
            eb.addField("Pets | " + simplifyNumber(petsTotal), petsStr, false);
            eb.addField("Talisman | " + simplifyNumber(talismanTotal), talismanStr, false);

            if (failedCount != 0) {
                eb.appendDescription("\nUnable to get " + failedCount + " items");
            }

            tempSet.forEach(System.out::println);

            return eb;
        }
        return

        defaultEmbed("Unable to fetch player data");
    }

    private static JsonArray queryAhApi(String query) {
        CloseableHttpClient httpclient = HttpClientBuilder.create().build();
        try {
            HttpGet httpget = new HttpGet("https://api.eastarctica.tk/auctions/");
            httpget.addHeader("content-type", "application/json; charset=UTF-8");

            URI uri = new URIBuilder(httpget.getURI())
                    .addParameter("query", "{\"item_name\":{\"$in\":[" + query + "]},\"bin\":true}")
                    .addParameter("sort", "{\"starting_bid\":1}").build();
            httpget.setURI(uri);

            HttpResponse httpresponse = httpclient.execute(httpget);
            return JsonParser.parseReader(new InputStreamReader(httpresponse.getEntity().getContent()))
                    .getAsJsonArray();
        } catch (Exception ignored) {
        } finally {
            try {
                httpclient.close();
            } catch (Exception e) {
                System.out.println("== Stack Trace (Nw Query Close Http Client) ==");
                e.printStackTrace();
            }
        }
        return null;
    }

    private void calculateAllPetsPrice() {
        String queryStr = "";
        for (InvItem item : invPets) {
            String petName = capitalizeString(item.getName()).replace("lvl", "Lvl");
            queryStr += "\"" + petName + "\",";
        }
        for (InvItem item : petsPets) {
            String petName = capitalizeString(item.getName()).replace("lvl", "Lvl");
            queryStr += "\"" + petName + "\",";
        }
        for (InvItem item : enderChestPets) {
            String petName = capitalizeString(item.getName()).replace("lvl", "Lvl");
            queryStr += "\"" + petName + "\",";
        }

        if (queryStr.length() == 0) {
            return;
        }

        queryStr = queryStr.substring(0, queryStr.length() - 1);

        JsonArray ahQuery = queryAhApi(queryStr);

        if (ahQuery != null) {
            for (JsonElement auction : ahQuery) {
                String auctionName = higherDepth(auction, "item_name").getAsString();
                double auctionPrice = higherDepth(auction, "starting_bid").getAsDouble();
                String auctionRarity = higherDepth(auction, "tier").getAsString();

                for (Iterator<InvItem> iterator = invPets.iterator(); iterator.hasNext();) {
                    InvItem item = iterator.next();
                    if (item.getName().equalsIgnoreCase(auctionName)
                            && item.getRarity().equalsIgnoreCase(auctionRarity)) {
                        double itemPrice = auctionPrice
                                + (item.getExtraStats().size() == 1 ? getLowestPrice(item.getExtraStats().get(0), " ")
                                        : 0);
                        invItems.add(item.getName() + "@split@" + itemPrice);
                        invTotal += itemPrice;
                        iterator.remove();
                    }
                }

                for (Iterator<InvItem> iterator = petsPets.iterator(); iterator.hasNext();) {
                    InvItem item = iterator.next();
                    if (item.getName().equalsIgnoreCase(auctionName)
                            && item.getRarity().equalsIgnoreCase(auctionRarity)) {
                        double itemPrice = auctionPrice
                                + (item.getExtraStats().size() == 1 ? getLowestPrice(item.getExtraStats().get(0), " ")
                                        : 0);
                        petsItems.add(item.getName() + "@split@" + itemPrice);
                        petsTotal += itemPrice;
                        iterator.remove();
                    }
                }

                for (Iterator<InvItem> iterator = enderChestPets.iterator(); iterator.hasNext();) {
                    InvItem item = iterator.next();
                    if (item.getName().equalsIgnoreCase(auctionName)
                            && item.getRarity().equalsIgnoreCase(auctionRarity)) {
                        double itemPrice = auctionPrice
                                + (item.getExtraStats().size() == 1 ? getLowestPrice(item.getExtraStats().get(0), " ")
                                        : 0);
                        enderChestItems.add(item.getName() + "@split@" + itemPrice);
                        enderChestTotal += itemPrice;
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
                double itemPrice = higherDepth(lowestBinJson,
                        item.getName().split("] ")[1].toLowerCase().trim() + rarityMap.get(item.getRarity()))
                                .getAsDouble()
                        + (item.getExtraStats().size() == 1 ? getLowestPrice(item.getExtraStats().get(0), " ") : 0);
                invItems.add(item.getName() + "@split@" + itemPrice);
                invTotal += itemPrice;
            } catch (Exception ignored) {
            }
        }

        for (InvItem item : petsPets) {
            try {
                double itemPrice = higherDepth(lowestBinJson,
                        item.getName().split("] ")[1].toLowerCase().trim() + rarityMap.get(item.getRarity()))
                                .getAsDouble()
                        + (item.getExtraStats().size() == 1 ? getLowestPrice(item.getExtraStats().get(0), " ") : 0);
                petsItems.add(item.getName() + "@split@" + itemPrice);
                petsTotal += itemPrice;
            } catch (Exception ignored) {
            }
        }

        for (InvItem item : enderChestPets) {
            try {
                double itemPrice = higherDepth(lowestBinJson,
                        item.getName().split("] ")[1].toLowerCase().trim() + rarityMap.get(item.getRarity()))
                                .getAsDouble()
                        + (item.getExtraStats().size() == 1 ? getLowestPrice(item.getExtraStats().get(0), " ") : 0);
                enderChestItems.add(item.getName() + "@split@" + itemPrice);
                enderChestTotal += itemPrice;
            } catch (Exception ignored) {
            }
        }

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
                }
                return 0;
            } else {
                itemCost = getLowestPrice(item.getId().toUpperCase(), item.getName());
            }
        } catch (Exception ignored) {
        }

        try {
            itemCount = item.getCount();
        } catch (Exception ignored) {
        }

        try {
            if (item.isRecombobulated()) {
                recombobulatedExtra = higherDepth(
                        higherDepth(higherDepth(bazaarJson, "RECOMBOBULATOR_3000"), "quick_status"), "sellPrice")
                                .getAsDouble();
            }
        } catch (Exception ignored) {
        }

        try {
            hbpExtras = item.getHbpCount()
                    * higherDepth(higherDepth(higherDepth(bazaarJson, "HOT_POTATO_BOOK"), "quick_status"), "sellPrice")
                            .getAsDouble();
        } catch (Exception ignored) {
        }

        try {
            fumingExtras = item.getFumingCount()
                    * higherDepth(higherDepth(higherDepth(bazaarJson, "FUMING_POTATO_BOOK"), "quick_status"),
                            "sellPrice").getAsDouble();
        } catch (Exception ignored) {
        }

        try {
            List<String> enchants = item.getEnchantsFormatted();
            for (String enchant : enchants) {
                try {
                    enchantsExtras += getLowestPriceEnchant(enchant.toUpperCase());
                } catch (Exception ignored) {
                }
            }
        } catch (Exception ignored) {
        }

        try {
            reforgeExtras = calculateReforgePrice(item.getModifier(), item.getRarity());
        } catch (Exception ignored) {

        }

        try {
            List<String> extraStats = item.getExtraStats();
            for (String extraItem : extraStats) {
                miscExtras += getLowestPrice(extraItem, " ");
            }
        } catch (Exception ignored) {
        }

        try {
            List<InvItem> backpackItems = item.getBackpackItems();
            for (InvItem backpackItem : backpackItems) {
                backpackExtras += calculateItemPrice(backpackItem);
            }
        } catch (Exception ignored) {
        }

        return itemCount * (itemCost + recombobulatedExtra + hbpExtras + enchantsExtras + fumingExtras + reforgeExtras
                + miscExtras + backpackExtras);
    }

    private double calculateReforgePrice(String reforgeName, String itemRarity) {
        JsonElement reforgesStonesJson = getReforgeStonesJson();
        List<String> reforgeStones = getJsonKeys(reforgesStonesJson);

        for (String reforgeStone : reforgeStones) {
            JsonElement reforgeStoneInfo = higherDepth(reforgesStonesJson, reforgeStone);
            if (higherDepth(reforgeStoneInfo, "reforgeName").getAsString().equalsIgnoreCase(reforgeName)) {
                String reforgeStoneName = higherDepth(reforgeStoneInfo, "internalName").getAsString();
                double reforgeStoneCost = getLowestPrice(reforgeStoneName, " ");
                double reforgeApplyCost = higherDepth(higherDepth(reforgeStoneInfo, "reforgeCosts"),
                        itemRarity.toUpperCase()).getAsDouble();
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

        for (int i = enchantLevel; i >= 1; i--) {
            try {
                lowestBin = higherDepth(lowestBinJson, enchantName + ";" + i).getAsDouble();
            } catch (Exception ignored) {
            }

            try {
                JsonElement avgInfo = higherDepth(averageAuctionJson, enchantName + ";" + i);
                averageAuction = higherDepth(avgInfo, "clean_price") != null
                        ? higherDepth(avgInfo, "clean_price").getAsDouble()
                        : higherDepth(avgInfo, "price").getAsDouble();
            } catch (Exception ignored) {
            }

            if (lowestBin == -1 && averageAuction != -1) {
                return Math.pow(2, enchantLevel - i) * averageAuction;
            } else if (lowestBin != -1 && averageAuction == -1) {
                return Math.pow(2, enchantLevel - i) * lowestBin;
            } else if (lowestBin != -1 && averageAuction != -1) {
                return Math.pow(2, enchantLevel - i) * Math.min(lowestBin, averageAuction);
            }
        }

        if (higherDepth(sbzPrices, enchantName + "_1") != null) {
            return Math.pow(2, enchantLevel - 1) * higherDepth(sbzPrices, enchantName + "_1").getAsDouble();
        }

        if (higherDepth(sbzPrices, enchantName + "_i") != null) {
            return Math.pow(2, enchantLevel - 1) * higherDepth(sbzPrices, enchantName + "_i").getAsDouble();
        }

        tempSet.add(enchantId);
        return 0;
    }

    private double getLowestPrice(String itemId, String tempName) {
        double lowestBin = -1;
        double averageAuction = -1;

        try {
            lowestBin = higherDepth(lowestBinJson, itemId).getAsDouble();
        } catch (Exception ignored) {
        }

        try {
            JsonElement avgInfo = higherDepth(averageAuctionJson, itemId);
            averageAuction = higherDepth(avgInfo, "clean_price") != null
                    ? higherDepth(avgInfo, "clean_price").getAsDouble()
                    : higherDepth(avgInfo, "price").getAsDouble();
        } catch (Exception ignored) {
        }

        if (lowestBin == -1 && averageAuction != -1) {
            return averageAuction;
        } else if (lowestBin != -1 && averageAuction == -1) {
            return lowestBin;
        } else if (lowestBin != -1 && averageAuction != -1) {
            return Math.min(lowestBin, averageAuction);
        }

        try {
            return higherDepth(higherDepth(higherDepth(bazaarJson, itemId), "quick_status"), "sellPrice").getAsDouble();
        } catch (Exception ignored) {
        }

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
        } catch (Exception ignored) {
        }

        if (isIgnoredItem(itemId)) {
            return 0;
        }

        tempSet.add(itemId + " - " + tempName);
        failedCount++;
        return 0;
    }

    private boolean isIgnoredItem(String s) {
        if (s.equalsIgnoreCase("none")) {
            return true;
        }

        if (s.startsWith("stained_glass_pane")) {
            return true;
        }

        if (s.equals("skyblock_menu")) {
            return true;
        }

        return false;
    }
}
