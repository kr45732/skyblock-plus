package com.skyblockplus.miscellaneous;

import static com.skyblockplus.auctionbaz.QueryAuctionCommand.queryAhApi;
import static com.skyblockplus.utils.Player.getGenericInventoryMap;
import static com.skyblockplus.utils.Utils.defaultEmbed;
import static com.skyblockplus.utils.Utils.errorMessage;
import static com.skyblockplus.utils.Utils.getJson;
import static com.skyblockplus.utils.Utils.getJsonKeys;
import static com.skyblockplus.utils.Utils.getReforgeStonesJson;
import static com.skyblockplus.utils.Utils.globalCooldown;
import static com.skyblockplus.utils.Utils.higherDepth;
import static com.skyblockplus.utils.Utils.loadingEmbed;
import static com.skyblockplus.utils.Utils.logCommand;
import static com.skyblockplus.utils.Utils.simplifyNumber;
import static java.lang.String.join;
import static java.util.Collections.nCopies;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.skyblockplus.utils.Player;
import com.skyblockplus.utils.structs.InvItemStruct;

import me.nullicorn.nedit.NBTReader;
import me.nullicorn.nedit.type.NBTCompound;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;

public class NetworthCommand extends Command {
    private JsonElement lowestBinJson;
    private JsonElement averageAuctionJson;
    private JsonElement bazaarJson;
    private JsonArray sbzPrices;
    private int failedCount;
    private Set<String> tempSet;

    public NetworthCommand() {
        this.name = "networth";
        this.cooldown = globalCooldown;
        this.aliases = new String[] { "nw" };
    }

    @Override
    protected void execute(CommandEvent event) {
        new Thread(() -> {
            EmbedBuilder eb = loadingEmbed();
            Message ebMessage = event.getChannel().sendMessage(eb.build()).complete();
            String content = event.getMessage().getContentRaw();
            String[] args = content.split(" ");
            failedCount = 0;
            tempSet = new HashSet<String>();

            logCommand(event.getGuild(), event.getAuthor(), content);

            if (args.length == 2) {
                ebMessage.editMessage(getPlayerNetworth(args[1], null).build()).queue();
                return;
            } else if (args.length == 3) {
                ebMessage.editMessage(getPlayerNetworth(args[1], args[2]).build()).queue();
                return;
            }

            ebMessage.editMessage(errorMessage(this.name).build()).queue();
        }).start();
    }

    private EmbedBuilder getPlayerNetworth(String username, String profileName) {
        Player player = profileName == null ? new Player(username) : new Player(username, profileName);
        if (player.isValid()) {
            EmbedBuilder eb = player.defaultPlayerEmbed();
            eb.setThumbnail(player.getThumbnailUrl());

            lowestBinJson = getJson("https://moulberry.codes/lowestbin.json");
            averageAuctionJson = getJson("http://moulberry.codes/auction_averages/3day.json");
            bazaarJson = higherDepth(getJson("https://api.hypixel.net/skyblock/bazaar"), "products");
            sbzPrices = getJson("https://raw.githubusercontent.com/skyblockz/pricecheckbot/master/data.json")
                    .getAsJsonArray();
            failedCount = 0;

            double bankBalance = player.getBankBalance();
            double purseCoins = player.getPurseCoins();

            double invTotal = 0;
            Map<Integer, InvItemStruct> playerInventory = player.getInventoryMap();

            if (playerInventory == null) {
                return defaultEmbed(player.getUsername() + "'s inventory API is disabled");
            }
            for (InvItemStruct item : playerInventory.values()) {
                invTotal += calculateItemPrice(item);
            }

            double talismanTotal = 0;
            Map<Integer, InvItemStruct> playerTalismans = player.getTalismanBagMap();
            if (playerTalismans == null) {
                return defaultEmbed(player.getUsername() + "'s talisman API is disabled");
            }
            for (InvItemStruct item : playerTalismans.values()) {
                talismanTotal += calculateItemPrice(item);
            }

            double invArmor = 0;
            Map<Integer, InvItemStruct> invArmorMap = player.getInventoryArmorMap();
            if (invArmorMap == null) {
                return defaultEmbed(player.getUsername() + "'s equipped armor API is disabled");
            }
            for (InvItemStruct item : invArmorMap.values()) {
                invArmor += calculateItemPrice(item);
            }

            double wardrobeTotal = 0;
            Map<Integer, InvItemStruct> wardrobeMap = player.getWardrobeMap();
            if (wardrobeMap == null) {
                return defaultEmbed(player.getUsername() + "'s wardrobe API is disabled");
            }
            for (InvItemStruct item : wardrobeMap.values()) {
                wardrobeTotal += calculateItemPrice(item);
            }

            // double petsTotal = 0;
            // List<InvItemStruct> petsMap = player.getPetsMapFormatted();
            // if (petsMap == null) {
            // return defaultEmbed(player.getUsername() + "'s pets API is disabled");
            // }
            // for (InvItemStruct item : petsMap) {
            // petsTotal += calculateItemPrice(item);
            // }
            double petsTotal = 0;
            List<InvItemStruct> petsMap = player.getPetsMapNames();
            if (petsMap == null) {
                return defaultEmbed(player.getUsername() + "'s pets API is disabled");
            }
            for (InvItemStruct item : petsMap) {
                petsTotal += calculateItemPrice(item);
            }

            double enderChestTotal = 0;
            Map<Integer, InvItemStruct> enderChest = player.getEnderChestMap();
            if (enderChest == null) {
                return defaultEmbed(player.getUsername() + "'s enderchest API is disabled");
            }
            for (InvItemStruct item : enderChest.values()) {
                enderChestTotal += calculateItemPrice(item);
            }

            double totalNetworth = bankBalance + purseCoins + invTotal + talismanTotal + invArmor + wardrobeTotal
                    + petsTotal + enderChestTotal;

            eb.setDescription("Total Networth: " + simplifyNumber(totalNetworth));
            eb.addField("Bank", simplifyNumber(bankBalance), true);
            eb.addField("Purse", simplifyNumber(purseCoins), true);
            eb.addField("Inventory", simplifyNumber(invTotal), true);
            eb.addField("Talisman", simplifyNumber(talismanTotal), true);
            eb.addField("Armor", simplifyNumber(invArmor), true);
            eb.addField("Wardrobe", simplifyNumber(wardrobeTotal), true);
            eb.addField("Pets", simplifyNumber(petsTotal), true);
            eb.addField("Ender Chest", simplifyNumber(enderChestTotal), true);
            if (failedCount != 0) {
                eb.appendDescription("\nUnable to get " + failedCount + " items");
            }

            tempSet.forEach(System.out::println);

            return eb;
        }
        return defaultEmbed("Unable to fetch player data");
    }

    public double calculateItemPrice(InvItemStruct item) {
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
            if (item.getId().equals("PET")) {
                itemCost = calculatePetPrice(item.getName(), item.getRarity());
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
            List<InvItemStruct> backpackItems = item.getBackpackItems();
            for (InvItemStruct backpackItem : backpackItems) {
                backpackExtras += calculateItemPrice(backpackItem);
            }
        } catch (Exception ignored) {
        }

        return itemCount * (itemCost + recombobulatedExtra + hbpExtras + enchantsExtras + fumingExtras + reforgeExtras
                + miscExtras + backpackExtras);
    }

    private double calculatePetPrice(String petName, String rarity) {
        JsonArray petQuery = queryAhApi(petName);

        if (petQuery == null || petQuery.size() <= 0) {
            return 0;
        }

        for (JsonElement lowestBinAh : petQuery) {

            Instant endingAt = Instant.ofEpochMilli(higherDepth(lowestBinAh, "end").getAsLong());
            Duration duration = Duration.between(Instant.now(), endingAt);
            if (duration.getSeconds() <= 0) {
                continue;
            }

            if (!higherDepth(lowestBinAh, "tier").getAsString().equals(rarity)) {
                continue;
            }

            if (higherDepth(lowestBinAh, "item_lore").getAsString()
                    .contains("Right-click to add this pet to\n§eyour pet menu!")) {
                System.out.println("SUCCESS - " + petName + " - "
                        + simplifyNumber(higherDepth(lowestBinAh, "starting_bid").getAsDouble()));
                return higherDepth(lowestBinAh, "starting_bid").getAsDouble();
            }
        }

        tempSet.add(petName);

        try {
            Map<String, String> rarityMap = new HashMap<>();
            rarityMap.put("LEGENDARY", ";4");
            rarityMap.put("EPIC", ";3");
            rarityMap.put("RARE", ";2");
            rarityMap.put("UNCOMMON", ";1");
            rarityMap.put("COMMON", ";0");

            petName = petName.split("] ")[1].toLowerCase().trim() + rarityMap.get(rarity);
            return higherDepth(lowestBinJson, petName).getAsDouble();
        } catch (Exception ignored) {
        }

        return 0;
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

    public double getLowestPriceEnchant(String enchantId) {
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

        return 0;
    }

    public double getLowestPrice(String itemId, String tempName) {
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

                itemId = minionName + "_minion_" + join("", nCopies(level, "i")).replace("iiiii", "v")
                        .replace("iiii", "iv").replace("vv", "x").replace("viv", "ix");
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
