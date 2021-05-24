package com.skyblockplus.miscellaneous;

import static com.skyblockplus.utils.Utils.capitalizeString;
import static com.skyblockplus.utils.Utils.defaultEmbed;
import static com.skyblockplus.utils.Utils.errorMessage;
import static com.skyblockplus.utils.Utils.getJson;
import static com.skyblockplus.utils.Utils.getMiscJson;
import static com.skyblockplus.utils.Utils.getTalismanJson;
import static com.skyblockplus.utils.Utils.globalCooldown;
import static com.skyblockplus.utils.Utils.higherDepth;
import static com.skyblockplus.utils.Utils.loadingEmbed;
import static com.skyblockplus.utils.Utils.logCommand;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.skyblockplus.utils.Player;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;

public class MissingTalismansCommand extends Command {

    public MissingTalismansCommand() {
        this.name = "missing";
        this.cooldown = globalCooldown;
    }

    @Override
    protected void execute(CommandEvent event) {
        new Thread(() -> {
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
        }).start();
    }

    private EmbedBuilder getMissingTalismans(String username, String profileName) {
        Player player = profileName == null ? new Player(username) : new Player(username, profileName);
        if (player.isValid()) {
            Set<String> playerItems;
            try {
                playerItems = player.getInventoryMap().values().stream().filter(o1 -> o1 != null).map(o1 -> o1.getId())
                        .collect(Collectors.toSet());
                playerItems.addAll(player.getEnderChestMap().values().stream().filter(o1 -> o1 != null)
                        .map(o1 -> o1.getId()).collect(Collectors.toSet()));
                playerItems.addAll(player.getStorageMap().values().stream().filter(o1 -> o1 != null)
                        .map(o1 -> o1.getId()).collect(Collectors.toSet()));
                playerItems.addAll(player.getTalismanBagMap().values().stream().filter(o1 -> o1 != null)
                        .map(o1 -> o1.getId()).collect(Collectors.toSet()));
            } catch (Exception e) {
                return defaultEmbed("Error").setDescription("Inventory API is disabled");
            }

            JsonObject talismanUpgrades = higherDepth(getMiscJson(), "talisman_upgrades").getAsJsonObject();
            JsonObject allTalismans = higherDepth(getTalismanJson(), "talismans").getAsJsonObject();

            Set<String> missingInternal = new HashSet<>();
            for (Entry<String, JsonElement> talismanUpgrade : allTalismans.entrySet()) {
                missingInternal.add(talismanUpgrade.getKey());
                if (higherDepth(getTalismanJson(), "talisman_duplicates." + talismanUpgrade.getKey()) != null) {
                    JsonArray duplicates = higherDepth(getTalismanJson(),
                            "talisman_duplicates." + talismanUpgrade.getKey()).getAsJsonArray();
                    for (JsonElement dupliate : duplicates) {
                        missingInternal.add(dupliate.getAsString());
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

            missingInternalArrCopy.forEach(o1 -> {
                if (higherDepth(talismanUpgrades, o1) != null) {
                    JsonArray curUpgrades = higherDepth(talismanUpgrades, o1).getAsJsonArray();
                    for (JsonElement curUpgrade : curUpgrades) {
                        missingInternalArr.remove(curUpgrade.getAsString());
                    }
                }
            });

            JsonElement lowestBinJson = getJson("https://moulberry.codes/lowestbin.json");
            missingInternalArr.sort(Comparator.comparingDouble(
                    o1 -> higherDepth(lowestBinJson, o1) != null ? higherDepth(lowestBinJson, o1).getAsDouble() : 0));

            String ebStr = "Sorted roughly from the least to greatest cost. Talismans with a `*` have higher tiers.\n\n";
            for (String i : missingInternalArr) {
                ebStr += "• " + capitalizeString(i.toLowerCase().replace("_", " "))
                        + (higherDepth(talismanUpgrades, i) != null ? "*" : "") + "\n";
            }
            return player.defaultPlayerEmbed().setDescription(ebStr);
        }
        return defaultEmbed("Unable to fetch player data");
    }

}
