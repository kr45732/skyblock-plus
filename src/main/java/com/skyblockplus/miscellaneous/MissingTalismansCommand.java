package com.skyblockplus.miscellaneous;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.skyblockplus.utils.Player;
import com.skyblockplus.utils.structs.InvItem;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;

import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import static com.skyblockplus.utils.Utils.*;

public class MissingTalismansCommand extends Command {

    public MissingTalismansCommand() {
        this.name = "missing";
        this.cooldown = globalCooldown;
    }

    public static EmbedBuilder getMissingTalismans(String username, String profileName) {
        Player player = profileName == null ? new Player(username) : new Player(username, profileName);
        if (player.isValid()) {
            Set<String> playerItems;
            try {
                playerItems =
                        player.getInventoryMap().values().stream().filter(Objects::nonNull).map(InvItem::getId).collect(Collectors.toSet());
                playerItems.addAll(
                        player.getEnderChestMap().values().stream().filter(Objects::nonNull).map(InvItem::getId).collect(Collectors.toSet())
                );
                playerItems.addAll(
                        player.getStorageMap().values().stream().filter(Objects::nonNull).map(InvItem::getId).collect(Collectors.toSet())
                );
                playerItems.addAll(
                        player.getTalismanBagMap().values().stream().filter(Objects::nonNull).map(InvItem::getId).collect(Collectors.toSet())
                );
            } catch (Exception e) {
                return defaultEmbed("Error").setDescription("Inventory API is disabled");
            }

            JsonObject talismanUpgrades = higherDepth(getMiscJson(), "talisman_upgrades").getAsJsonObject();
            JsonObject allTalismans = higherDepth(getTalismanJson(), "talismans").getAsJsonObject();

            Set<String> missingInternal = new HashSet<>();
            for (Entry<String, JsonElement> talismanUpgrade : allTalismans.entrySet()) {
                missingInternal.add(talismanUpgrade.getKey());
                if (higherDepth(getTalismanJson(), "talisman_duplicates." + talismanUpgrade.getKey()) != null) {
                    JsonArray duplicates = higherDepth(getTalismanJson(), "talisman_duplicates." + talismanUpgrade.getKey())
                            .getAsJsonArray();
                    for (JsonElement duplicate : duplicates) {
                        missingInternal.add(duplicate.getAsString());
                    }
                }
            }

            for (String playerItem : playerItems) {
                missingInternal.remove(playerItem);
                for (Map.Entry<String, JsonElement> talismanUpgradesElement : talismanUpgrades.entrySet()) {
                    JsonArray upgrades = talismanUpgradesElement.getValue().getAsJsonArray();
                    for (int j = 0; j < upgrades.size(); j++) {
                        String upgrade = upgrades.get(j).getAsString();
                        if (playerItem.equals(upgrade)) {
                            missingInternal.remove(talismanUpgradesElement.getKey());
                            break;
                        }
                    }
                }
            }

            List<String> missingInternalArr = new ArrayList<>(missingInternal);
            List<String> missingInternalArrCopy = new ArrayList<>(missingInternalArr);

            missingInternalArrCopy.forEach(
                    o1 -> {
                        if (higherDepth(talismanUpgrades, o1) != null) {
                            JsonArray curUpgrades = higherDepth(talismanUpgrades, o1).getAsJsonArray();
                            for (JsonElement curUpgrade : curUpgrades) {
                                missingInternalArr.remove(curUpgrade.getAsString());
                            }
                        }
                    }
            );

            JsonElement lowestBinJson = getLowestBinJson();
            missingInternalArr.sort(
                    Comparator.comparingDouble(o1 -> higherDepth(lowestBinJson, o1) != null ? higherDepth(lowestBinJson, o1).getAsDouble() : 0)
            );

            StringBuilder ebStr = new StringBuilder(
                    "Sorted roughly from the least to greatest cost. Talismans with a `*` have higher tiers.\n\n"
            );
            for (String i : missingInternalArr) {
                ebStr
                        .append("• ")
                        .append(convertFromInternalName(i))
                        .append(higherDepth(talismanUpgrades, i) != null ? "**\\***" : "")
                        .append("\n");
            }
            return player.defaultPlayerEmbed().setDescription(ebStr.toString());
        }
        return defaultEmbed("Unable to fetch player data");
    }

    @Override
    protected void execute(CommandEvent event) {
        new Thread(
                () -> {
                    EmbedBuilder eb = loadingEmbed();
                    Message ebMessage = event.getChannel().sendMessage(eb.build()).complete();
                    String content = event.getMessage().getContentRaw();
                    String[] args = content.split(" ");

                    logCommand(event.getGuild(), event.getAuthor(), content);

                    if (args.length == 3) {
                        ebMessage.editMessage(getMissingTalismans(args[1], args[2]).build()).queue();
                        return;
                    } else if (args.length == 2) {
                        ebMessage.editMessage(getMissingTalismans(args[1], null).build()).queue();
                        return;
                    }

                    ebMessage.editMessage(errorMessage(this.name).build()).queue();
                }
        )
                .start();
    }
}
