package com.SkyblockBot.Auction;

import static com.SkyblockBot.Miscellaneous.BotUtils.botPrefix;
import static com.SkyblockBot.Miscellaneous.BotUtils.defaultEmbed;
import static com.SkyblockBot.Miscellaneous.BotUtils.errorMessage;
import static com.SkyblockBot.Miscellaneous.BotUtils.formatNumber;
import static com.SkyblockBot.Miscellaneous.BotUtils.getJson;
import static com.SkyblockBot.Miscellaneous.BotUtils.globalCooldown;
import static com.SkyblockBot.Miscellaneous.BotUtils.higherDepth;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import com.google.gson.JsonElement;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;

public class BinCommands extends Command {
    public BinCommands() {
        this.name = "bin";
        this.guildOnly = false;
        this.cooldown = globalCooldown;
    }

    @Override
    protected void execute(CommandEvent event) {
        final EmbedBuilder[] eb = { defaultEmbed("Loading auction data...", null) };

        Message message = event.getMessage();
        String args = message.getContentRaw();

        if (args.split(" ").length < 2) { // Not enough args are given
            eb[0].setTitle(errorMessage(this.name));
            event.reply(eb[0].build(), m -> m.editMessage(eb[0].build()).queue());
            return;
        }

        System.out.println(args);

        eb[0] = getLowestBin(args.replace(botPrefix + "bin ", ""));

        event.reply(eb[0].build(), m -> m.editMessage(eb[0].build()).queue());

    }

    public EmbedBuilder getLowestBin(String item) {
        String preFormattedItem = item.trim().toUpperCase().replace(" ", "_");

        JsonElement lowestBinJson = getJson("https://moulberry.codes/lowestbin.json");

        JsonElement petJson = getJson(
                "https://raw.githubusercontent.com/Moulberry/NotEnoughUpdates-REPO/master/constants/petnums.json");

        JsonElement enchantsJson = higherDepth(getJson(
                "https://raw.githubusercontent.com/Moulberry/NotEnoughUpdates-REPO/master/constants/enchants.json"),
                "enchants_min_level");

        List<String> petNames = petJson.getAsJsonObject().entrySet().stream().map(i -> i.getKey())
                .collect(Collectors.toCollection(ArrayList::new));

        List<String> enchantNames = enchantsJson.getAsJsonObject().entrySet().stream()
                .map(i -> i.getKey().toUpperCase()).collect(Collectors.toCollection(ArrayList::new));
        enchantNames.add("ULTIMATE_JERRY");

        Map<String, String> rarityMap = new HashMap<>();
        rarityMap.put("LEGENDARY", ";4");
        rarityMap.put("EPIC", ";3");
        rarityMap.put("RARE", ";2");
        rarityMap.put("UNCOMMON", ";1");
        rarityMap.put("COMMON", ";0");

        String formattedName = "";
        for (String i : enchantNames) {
            if (preFormattedItem.contains(i)) {
                String enchantName = "";
                try {
                    int enchantLevel = Integer.parseInt(preFormattedItem.replaceAll("\\D+", ""));
                    enchantName = i.toLowerCase().replace("_", " ") + " " + enchantLevel;
                    formattedName = i + ";" + enchantLevel;
                    EmbedBuilder eb = defaultEmbed("Lowest bin", null);
                    eb.addField(enchantName, formatNumber(higherDepth(lowestBinJson, formattedName).getAsLong()),
                            false);
                    return eb;
                } catch (NumberFormatException e) {
                    try {
                        EmbedBuilder eb = defaultEmbed("Lowest bin", null);
                        for (int j = 10; j > 0; j--) {
                            try {
                                formattedName = i + ";" + j;
                                enchantName = i.toLowerCase().replace("_", " ") + " " + j;
                                eb.addField(enchantName,
                                        formatNumber(higherDepth(lowestBinJson, formattedName).getAsLong()), false);

                            } catch (NullPointerException ex) {

                            }
                        }
                        if (eb.getFields().size() == 0) {
                            return defaultEmbed("No bin found for " + item.toLowerCase(), null);
                        }
                        return eb;
                    } catch (NullPointerException ex) {
                        return defaultEmbed("No bin found for " + item.toLowerCase(), null);
                    }
                } catch (NullPointerException e) {
                    return defaultEmbed("No bin found for " + item.toLowerCase(), null);
                }
            }
        }

        for (String i : petNames) {
            if (preFormattedItem.contains("JERRY") && !preFormattedItem.contains("ULTIMATE")) {
                break;
            }
            if (preFormattedItem.contains(i)) {
                String petName = "";
                formattedName = i;
                boolean raritySpecified = false;
                for (Entry<String, String> j : rarityMap.entrySet()) {
                    if (preFormattedItem.contains(j.getKey())) {
                        petName = j.getKey().toLowerCase() + " " + formattedName.toLowerCase().replace("_", " ");
                        formattedName += j.getValue();
                        raritySpecified = true;
                        break;
                    }
                }

                if (!raritySpecified) {
                    List<String> petRarities = higherDepth(petJson, formattedName).getAsJsonObject().entrySet().stream()
                            .map(j -> j.getKey().toUpperCase()).collect(Collectors.toCollection(ArrayList::new));

                    for (String j : petRarities) {
                        if (higherDepth(lowestBinJson, formattedName + rarityMap.get(j)) != null) {
                            petName = j.toLowerCase() + " " + formattedName.toLowerCase().replace("_", " ");
                            formattedName += rarityMap.get(j);
                            break;
                        }
                    }
                }
                EmbedBuilder eb = defaultEmbed("Lowest bin", null);
                eb.addField(petName + " pet", formatNumber(higherDepth(lowestBinJson, formattedName).getAsLong()),
                        false);
                return eb;
            }
        }

        try {
            EmbedBuilder eb = defaultEmbed("Lowest bin", null);
            eb.addField(item.toLowerCase(), formatNumber(higherDepth(lowestBinJson, preFormattedItem).getAsLong()),
                    false);
            return eb;
        } catch (NullPointerException e) {
            return defaultEmbed("No bin found for " + item.toLowerCase(), null);
        }
    }
}
