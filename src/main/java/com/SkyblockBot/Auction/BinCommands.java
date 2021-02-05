package com.SkyblockBot.Auction;

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

import static com.SkyblockBot.Utils.BotUtils.*;

public class BinCommands extends Command {
    Message ebMessage;

    public BinCommands() {
        this.name = "bin";
        this.guildOnly = false;
        this.cooldown = globalCooldown;
    }

    @Override
    protected void execute(CommandEvent event) {
        EmbedBuilder eb = defaultEmbed("Loading lowest bin data...", null);

        Message message = event.getMessage();
        String args = message.getContentRaw();
        this.ebMessage = event.getChannel().sendMessage(eb.build()).complete();

        if (args.split(" ").length < 2) { // Not enough args are given
            eb.setTitle(errorMessage(this.name));
            ebMessage.editMessage(eb.build()).queue();
            return;
        }

        System.out.println(args);

        eb = getLowestBin(args.replace(botPrefix + "bin ", ""));

        ebMessage.editMessage(eb.build()).queue();
    }

    public EmbedBuilder getLowestBin(String item) {
        String preFormattedItem = item.trim().toUpperCase().replace(" ", "_").replace("'S", "").replace("FRAG", "FRAGMENT").replace(".", "");

        JsonElement petJson = getJson(
                "https://raw.githubusercontent.com/Moulberry/NotEnoughUpdates-REPO/master/constants/petnums.json");

        List<String> petNames = petJson.getAsJsonObject().entrySet().stream().map(Entry::getKey)
                .collect(Collectors.toCollection(ArrayList::new));

        if (preFormattedItem.equals("BONEMERANG")) {
            preFormattedItem = "BONE_BOOMERANG";
        } else if (preFormattedItem.equals("GOD_POT")) {
            preFormattedItem = "GOD_POTION";
        } else if (preFormattedItem.equals("AOTD")) {
            preFormattedItem = "ASPECT_OF_THE_DRAGON";
        } else if (preFormattedItem.equals("AOTE")) {
            preFormattedItem = "ASPECT_OF_THE_END";
        } else if (preFormattedItem.equals("ROD_OF_CHAMPIONS")) {
            preFormattedItem = "CHAMP_ROD";
        } else if (preFormattedItem.equals("ROD_OF_LEGENDS")) {
            preFormattedItem = "LEGEND_ROD";
        } else if (preFormattedItem.equals("CHALLENGING_ROD")) {
            preFormattedItem = "CHALLENGE_ROD";
        } else if (preFormattedItem.equals("LASR_EYE")) {
            preFormattedItem = "GIANT_FRAGMENT_LASER";
        } else if (preFormattedItem.equals("DIAMANTE_HANDLE")) {
            preFormattedItem = "GIANT_FRAGMENT_DIAMOND";
        } else if (preFormattedItem.equals("BIGFOOT_LASSO")) {
            preFormattedItem = "GIANT_FRAGMENT_BIGFOOT";
        } else if (preFormattedItem.equals("JOLLY_PINK_ROCK")) {
            preFormattedItem = "GIANT_FRAGMENT_BOULDER";
        } else if (preFormattedItem.equals("HYPER_CATALYST")) {
            preFormattedItem = "HYPER_CATALYST_UPGRADE";
        } else if (preFormattedItem.equals("ENDER_HELMET")) {
            preFormattedItem = "END_HELMET";
        } else if (preFormattedItem.equals("ENDER_CHESTPLATE")) {
            preFormattedItem = "END_CHESTPLATE";
        } else if (preFormattedItem.equals("ENDER_LEGGINGS")) {
            preFormattedItem = "END_LEGGINGS";
        } else if (preFormattedItem.equals("ENDER_BOOTS")) {
            preFormattedItem = "END_BOOTS";
        } else if (preFormattedItem.equals("EMPEROR_SKULL")) {
            preFormattedItem = "DIVER_FRAGMENT";
        } else if (preFormattedItem.contains("GOLDEN") && preFormattedItem.contains("HEAD")) {
            preFormattedItem = preFormattedItem.replace("GOLDEN", "GOLD");
        } else if (preFormattedItem.equals("COLOSSAL_EXP_BOTTLE")) {
            preFormattedItem = "COLOSSAL_EXP_BOTTLE_UPGRADE";
        } else if (preFormattedItem.equals("FLYCATCHER")) {
            preFormattedItem = "FLYCATCHER_UPGRADE";
        } else if (preFormattedItem.contains("PET_SKIN")) {
            for (String curPet : petNames) {
                if (preFormattedItem.contains(curPet)) {
                    preFormattedItem = "PET_SKIN_" + curPet;
                    break;
                }
            }
        }

        JsonElement lowestBinJson = getJson("https://moulberry.codes/lowestbin.json");
        if (higherDepth(lowestBinJson, preFormattedItem) != null) {
            EmbedBuilder eb = defaultEmbed("Lowest bin", null);
            eb.addField(capitalizeString(item.toLowerCase()), formatNumber(higherDepth(lowestBinJson, preFormattedItem).getAsLong()),
                    false);
            return eb;
        }


        JsonElement enchantsJson = higherDepth(getJson(
                "https://raw.githubusercontent.com/Moulberry/NotEnoughUpdates-REPO/master/constants/enchants.json"),
                "enchants_min_level");

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
                    EmbedBuilder eb = defaultEmbed("Lowest bin", null);
                    eb.addField(capitalizeString(enchantName), formatNumber(higherDepth(lowestBinJson, formattedName).getAsLong()),
                            false);
                    return eb;
                } catch (NumberFormatException e) {
                    try {
                        EmbedBuilder eb = defaultEmbed("/Lowest bin", null);
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
                            return defaultEmbed("No bin found for " + capitalizeString(item.toLowerCase()), null);
                        }
                        return eb;
                    } catch (NullPointerException ex) {
                        return defaultEmbed("No bin found for " + capitalizeString(item.toLowerCase()), null);
                    }
                } catch (NullPointerException e) {
                    return defaultEmbed("No bin found for " + capitalizeString(item.toLowerCase()), null);
                }
            }
        }

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
                EmbedBuilder eb = defaultEmbed("Lowest bin", null);
                eb.addField(capitalizeString(petName) + " pet", formatNumber(higherDepth(lowestBinJson, formattedName).getAsLong()),
                        false);
                return eb;
            }
        }

        return defaultEmbed("No bin found for " + capitalizeString(item.toLowerCase()), null);
    }
}
