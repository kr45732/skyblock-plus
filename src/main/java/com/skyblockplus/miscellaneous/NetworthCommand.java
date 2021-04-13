package com.skyblockplus.miscellaneous;

import com.google.gson.JsonElement;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.skyblockplus.inventory.InvItemStruct;
import com.skyblockplus.utils.Player;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static com.skyblockplus.utils.Utils.*;

public class NetworthCommand extends Command {
    private JsonElement lowestBinJson;
    private JsonElement averageAuctionJson;
    private JsonElement bazaarJson;
    private Map<Double, String> test = new TreeMap<>();

    public NetworthCommand() {
        this.name = "networth";
        this.cooldown = globalCooldown;
        this.aliases = new String[]{"nw"};
    }

    @Override
    protected void execute(CommandEvent event) {
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

            double bankBalance = player.getBankBalance();
            double purseCoins = player.getPurseCoins();

            double invTotal = 0;
            Map<Integer, InvItemStruct> playerInventory = player.getInventoryMap();
            for (InvItemStruct item : playerInventory.values()) {
                invTotal += calculateItemPrice(item);
            }

            double talismanTotal = 0;
            Map<Integer, InvItemStruct> playerTalismans = player.getTalismanBagMap();
            for (InvItemStruct item : playerTalismans.values()) {
                talismanTotal += calculateItemPrice(item);
            }

            double invArmor = 0;
            Map<Integer, InvItemStruct> invArmorMap = player.getInventoryArmorMap();
            for (InvItemStruct item : invArmorMap.values()) {
                invArmor += calculateItemPrice(item);
            }

            double wardrobeTotal = 0;
            Map<Integer, InvItemStruct> wardrobeMap = player.getWardrobeMap();
            for (InvItemStruct item : wardrobeMap.values()) {
                wardrobeTotal += calculateItemPrice(item);
            }

            double petsTotal = 0;
            List<InvItemStruct> petsMap = player.getPetsMapFormatted();
            for (InvItemStruct item : petsMap) {
                petsTotal += calculateItemPrice(item);
            }

            double enderChestTotal = 0;
            Map<Integer, InvItemStruct> enderChest = player.getEnderChestMap();
            for (InvItemStruct item : enderChest.values()) {
                enderChestTotal += calculateItemPrice(item);
            }

            for(Map.Entry<Double, String> test1:test.entrySet()){
                System.out.println(simplifyNumber(test1.getKey()) + " - " + test1.getValue());
            }

            double totalNetworth = bankBalance + purseCoins + invTotal + talismanTotal + invArmor + wardrobeTotal + petsTotal + enderChestTotal;

            eb.setDescription("Total Networth: " + simplifyNumber(totalNetworth));
            eb.addField("Bank", simplifyNumber(bankBalance), true);
            eb.addField("Purse", simplifyNumber(purseCoins), true);
            eb.addField("Inventory", simplifyNumber(invTotal), true);
            eb.addField("Talisman", simplifyNumber(talismanTotal), true);
            eb.addField("Armor", simplifyNumber(invArmor), true);
            eb.addField("Wardrobe", simplifyNumber(wardrobeTotal), true);
            eb.addField("Pets", simplifyNumber(petsTotal), true);
            eb.addField("Ender Chest", simplifyNumber(enderChestTotal), true);

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
            itemCost = getLowestPrice(item.getId().toUpperCase());
        } catch (Exception ignored) {
        }

        try {
            itemCount = item.getCount();
        } catch (Exception ignored) {
        }

        try {
            if (item.isRecombobulated()) {
                recombobulatedExtra = higherDepth(higherDepth(higherDepth(bazaarJson, "RECOMBOBULATOR_3000"), "quick_status"), "sellPrice").getAsDouble();
            }
        } catch (Exception ignored) {
        }

        try {
            hbpExtras = item.getHbpCount() * higherDepth(higherDepth(higherDepth(bazaarJson, "HOT_POTATO_BOOK"), "quick_status"), "sellPrice").getAsDouble();
        } catch (Exception ignored) {
        }

        try {
            fumingExtras = item.getFumingCount() * higherDepth(higherDepth(higherDepth(bazaarJson, "FUMING_POTATO_BOOK"), "quick_status"), "sellPrice").getAsDouble();
        } catch (Exception ignored) {
        }

        try {
            List<String> enchants = item.getEnchantsFormatted();
            for (String enchant : enchants) {
                try {
                    enchantsExtras += getLowestPrice(enchant.toUpperCase());
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
                miscExtras += getLowestPrice(extraItem);
            }
        } catch (Exception ignored) {
        }

        try{
            for (InvItemStruct backpackItem : backpackItems) {
                backpackExtras += calculateItemPrice(backpackItem);
            }
        }catch (Exception ignored){
        }

        return itemCount * (itemCost + recombobulatedExtra + hbpExtras + enchantsExtras + fumingExtras + reforgeExtras + miscExtras + backpackExtras);
    }

    private double calculateReforgePrice(String reforgeName, String itemRarity) {
        JsonElement reforgesStonesJson = getReforgeStonesJson();
        List<String> reforgeStones = getJsonKeys(reforgesStonesJson);

        for (String reforgeStone : reforgeStones) {
            JsonElement reforgeStoneInfo = higherDepth(reforgesStonesJson, reforgeStone);
            if (higherDepth(reforgeStoneInfo, "reforgeName").getAsString().equalsIgnoreCase(reforgeName)) {
                String reforgeStoneName = higherDepth(reforgeStoneInfo, "internalName").getAsString();
                double reforgeStoneCost = getLowestPrice(reforgeStoneName);
                double reforgeApplyCost = higherDepth(higherDepth(reforgeStoneInfo, "reforgeCosts"), itemRarity.toUpperCase()).getAsDouble();
                return reforgeStoneCost + reforgeApplyCost;
            }
        }

        return 0;
    }

    public double getLowestPrice(String itemId) {
        double lowestBin = -1;
        double averageAuction = -1;

        try {
            lowestBin = higherDepth(lowestBinJson, itemId).getAsDouble();
        } catch (Exception ignored) {
        }

        try {
            JsonElement avgInfo = higherDepth(averageAuctionJson, itemId);
            averageAuction = higherDepth(avgInfo, "clean_price") != null ? higherDepth(avgInfo, "clean_price").getAsDouble() : higherDepth(avgInfo, "price").getAsDouble();
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
            return higherDepth(bazaarJson, itemId).getAsDouble();
        } catch (Exception ignored) {
        }

        return 0;
    }
}
