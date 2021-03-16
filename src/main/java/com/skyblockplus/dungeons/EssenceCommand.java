package com.skyblockplus.dungeons;

import com.google.gson.JsonElement;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;

import java.util.Locale;

import static com.skyblockplus.Main.jda;
import static com.skyblockplus.utils.Utils.*;

public class EssenceCommand extends Command {

    public EssenceCommand() {
        this.name = "essence";
        this.cooldown = globalCooldown;
    }

    @Override
    protected void execute(CommandEvent event) {
        EmbedBuilder eb = loadingEmbed();
        Message ebMessage = event.getChannel().sendMessage(eb.build()).complete();
        String content = event.getMessage().getContentRaw();
        String[] args = content.split(" ");

        logCommand(event.getGuild(), event.getAuthor(), content);

        JsonElement essenceCostsJson = getJson(
                "https://raw.githubusercontent.com/Moulberry/NotEnoughUpdates-REPO/master/constants/essencecosts.json");

        if (args.length >= 3 && args[1].equals("upgrade")) {
            String itemName = content.split(" ", 3)[2].replace(" ", "_").toUpperCase();

            JsonElement itemJson = higherDepth(essenceCostsJson, itemName);
            if (itemJson != null) {
                jda.addEventListener(new EssenceWaiter(itemName, itemJson, ebMessage, event.getAuthor()));
            } else {
                eb = defaultEmbed("Invalid item name");
                ebMessage.editMessage(eb.build()).queue();
            }
            return;
        } else if (args.length >= 3 && (args[1].equals("info") || args[1].equals("information"))) {
            String itemName = content.split(" ", 3)[2].replace(" ", "_").toUpperCase();

            eb = getEssenceInformation(itemName, essenceCostsJson);
            if (eb == null) {
                eb = defaultEmbed("Invalid item name");
            }
            ebMessage.editMessage(eb.build()).queue();
            return;
        }

        ebMessage.editMessage(errorMessage(this.name).build()).queue();
    }

    private EmbedBuilder getEssenceInformation(String itemName, JsonElement essenceCostsJson) {
        JsonElement itemJson = higherDepth(essenceCostsJson, itemName);
        EmbedBuilder eb = defaultEmbed("Essence information for " + itemName.toLowerCase().replace("_", " "));
        if (itemJson != null) {
            String essenceType = higherDepth(itemJson, "type").getAsString().toLowerCase(Locale.ROOT);
            for (String level : getJsonKeys(itemJson)) {
                switch (level) {
                    case "type":
                        eb.setDescription("**Essence Type**: " + capitalizeString(essenceType) + " essence");
                        break;
                    case "dungeonize":
                        eb.addField("Dungeonize item",
                                higherDepth(itemJson, level).getAsString() + " " + essenceType + " essence", false);
                        break;
                    case "1":
                        eb.addField(level + " star",
                                higherDepth(itemJson, level).getAsString() + " " + essenceType + " essence", false);
                        break;
                    default:
                        eb.addField(level + " stars",
                                higherDepth(itemJson, level).getAsString() + " " + essenceType + " essence", false);
                        break;
                }
            }
            eb.setThumbnail("https://sky.lea.moe/item.gif/" + itemName);
            return eb;
        }
        return null;
    }

}
