package com.skyblockplus.auctionbaz;

import com.google.gson.JsonElement;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import static com.skyblockplus.utils.Utils.*;

public class BinCommand extends Command {
    public BinCommand() {
        this.name = "bin";
        this.cooldown = globalCooldown;
        this.aliases = new String[] { "lbin" };
    }

    @Override
    protected void execute(CommandEvent event) {
        new Thread(() -> {
            EmbedBuilder eb = loadingEmbed();
            Message ebMessage = event.getChannel().sendMessage(eb.build()).complete();
            String args = event.getMessage().getContentRaw();

            logCommand(event.getGuild(), event.getAuthor(), args);

            if (args.split(" ").length >= 2) {
                ebMessage.editMessage(getLowestBin(args.replace(BOT_PREFIX + "bin ", "")).build()).queue();
                return;
            }

            ebMessage.editMessage(errorMessage(this.name).build()).queue();
        }).start();
    }

    private EmbedBuilder getLowestBin(String item) {
        JsonElement lowestBinJson = getJson("https://moulberry.codes/lowestbin.json");
        if (higherDepth(lowestBinJson, item) != null) {
            EmbedBuilder eb = defaultEmbed("Lowest bin");
            eb.addField(capitalizeString(item.toLowerCase()),
                    formatNumber(higherDepth(lowestBinJson, item).getAsLong()), false);
            eb.setThumbnail("https://sky.lea.moe/item.gif/" + item);
            return eb;
        }

        String preFormattedItem = convertToInternalName(item);

        if (higherDepth(lowestBinJson, preFormattedItem) != null) {
            EmbedBuilder eb = defaultEmbed("Lowest bin");
            eb.addField(capitalizeString(item.toLowerCase()),
                    formatNumber(higherDepth(lowestBinJson, preFormattedItem).getAsLong()), false);
            eb.setThumbnail("https://sky.lea.moe/item.gif/" + preFormattedItem);
            return eb;
        }

        JsonElement enchantsJson = higherDepth(getEnchantsJson(), "enchants_min_level");

        List<String> enchantNames = enchantsJson.getAsJsonObject().entrySet().stream()
                .map(i -> i.getKey().toUpperCase()).collect(Collectors.toCollection(ArrayList::new));
        enchantNames.add("ULTIMATE_JERRY");

        Map<String, String> rarityMap = new HashMap<>();
        rarityMap.put("LEGENDARY", ";4");
        rarityMap.put("EPIC", ";3");
        rarityMap.put("RARE", ";2");
        rarityMap.put("UNCOMMON", ";1");
        rarityMap.put("COMMON", ";0");

        String formattedName;
        for (String i : enchantNames) {
            if (preFormattedItem.contains(i)) {
                String enchantName;
                try {
                    int enchantLevel = Integer.parseInt(preFormattedItem.replaceAll("\\D+", ""));
                    enchantName = i.toLowerCase().replace("_", " ") + " " + enchantLevel;
                    formattedName = i + ";" + enchantLevel;
                    EmbedBuilder eb = defaultEmbed("Lowest bin");
                    eb.addField(capitalizeString(enchantName),
                            formatNumber(higherDepth(lowestBinJson, formattedName).getAsLong()), false);
                    eb.setThumbnail("https://sky.lea.moe/item.gif/ENCHANTED_BOOK");
                    return eb;
                } catch (NumberFormatException e) {
                    try {
                        EmbedBuilder eb = defaultEmbed("Lowest bin");
                        for (int j = 10; j > 0; j--) {
                            try {
                                formattedName = i + ";" + j;
                                enchantName = i.toLowerCase().replace("_", " ") + " " + j;
                                eb.addField(capitalizeString(enchantName),
                                        formatNumber(higherDepth(lowestBinJson, formattedName).getAsLong()), false);

                            } catch (NullPointerException ignored) {

                            }
                        }
                        if (eb.getFields().size() == 0) {
                            return defaultEmbed("No bin found for " + capitalizeString(item.toLowerCase()));
                        }
                        eb.setThumbnail("https://sky.lea.moe/item.gif/ENCHANTED_BOOK");
                        return eb;
                    } catch (NullPointerException ex) {
                        return defaultEmbed("No bin found for " + capitalizeString(item.toLowerCase()));
                    }
                } catch (NullPointerException e) {
                    return defaultEmbed("No bin found for " + capitalizeString(item.toLowerCase()));
                }
            }
        }

        JsonElement petJson = getPetNumsJson();

        List<String> petNames = petJson.getAsJsonObject().entrySet().stream().map(Entry::getKey)
                .collect(Collectors.toCollection(ArrayList::new));

        for (String i : petNames) {
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
                EmbedBuilder eb = defaultEmbed("Lowest bin");

                try {
                    eb.addField(capitalizeString(petName) + " pet",
                            formatNumber(higherDepth(lowestBinJson, formattedName).getAsLong()), false);
                    eb.setThumbnail(getPetUrl(formattedName.split(";")[0]));
                    return eb;
                } catch (Exception ignored) {

                }
            }
        }

        return defaultEmbed("No bin found for " + capitalizeString(item.toLowerCase()));
    }
}
