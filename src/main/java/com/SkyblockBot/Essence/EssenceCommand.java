package com.SkyblockBot.Essence;

import com.google.gson.JsonElement;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;

import java.util.Locale;

import static com.SkyblockBot.Miscellaneous.BotUtils.*;

public class EssenceCommand extends Command {
    Message ebMessage;
    JDA jda;
    User user;

    public EssenceCommand() {
        this.name = "essence";
        this.guildOnly = false;
        this.cooldown = globalCooldown;
    }

    @Override
    protected void execute(CommandEvent event) {
        EmbedBuilder eb = defaultEmbed("Loading essence data...", null);
        this.ebMessage = event.getChannel().sendMessage(eb.build()).complete();

        Message message = event.getMessage();
        String content = message.getContentRaw();

        String[] args = content.split(" ");
        if (args.length < 3) {
            eb = defaultEmbed(errorMessage(this.name), null);
            ebMessage.editMessage(eb.build()).queue();
            return;
        }

        for (String value : args) {
            System.out.print(value + " ");
        }
        System.out.println();


        String itemName = content.split(" ", 3)[2].replace(" ", "_").toUpperCase();
        if (args[1].equals("upgrade")) {
            jda = event.getJDA();
            user = event.getAuthor();

            JsonElement essenceCostsJson = getJson("https://raw.githubusercontent.com/Moulberry/NotEnoughUpdates-REPO/master/constants/essencecosts.json");
            JsonElement itemJson = higherDepth(essenceCostsJson, itemName);
            if (itemJson != null) {
                jda.addEventListener(new EssenceWaiter(itemName, itemJson, ebMessage, user));
            } else {
                eb = defaultEmbed("Invalid item name", null);
                ebMessage.editMessage(eb.build()).queue();
            }
        } else if (args[1].equals("info") || args[1].equals("information")) {
            eb = getEssenceInformation(itemName);
            if (eb == null) {
                eb = defaultEmbed("Invalid item name", null);
            }
            ebMessage.editMessage(eb.build()).queue();
        } else {
            eb = defaultEmbed(errorMessage(this.name), null);
            ebMessage.editMessage(eb.build()).queue();
        }
    }

    public EmbedBuilder getEssenceInformation(String itemName) {
        JsonElement essenceCostsJson = getJson("https://raw.githubusercontent.com/Moulberry/NotEnoughUpdates-REPO/master/constants/essencecosts.json");
        JsonElement itemJson = higherDepth(essenceCostsJson, itemName);
        EmbedBuilder eb = defaultEmbed("Essence information for " + itemName.toLowerCase().replace("_", " "), null);
        if (itemJson != null) {
            String essenceType = higherDepth(itemJson, "type").getAsString().toLowerCase(Locale.ROOT);
            for (String level : getJsonKeys(itemJson)) {
                if (level.equals("type")) {
                    eb.setDescription("**Essence Type**: " + capitalizeString(essenceType) + " essence");
                } else if (level.equals("dungeonize")) {
                    eb.addField("Dungeonize item", higherDepth(itemJson, level).getAsString() + " " + essenceType + " essence", false);
                } else if (level.equals("1")) {
                    eb.addField(level + " star", higherDepth(itemJson, level).getAsString() + " " + essenceType + " essence", false);
                } else {
                    eb.addField(level + " stars", higherDepth(itemJson, level).getAsString() + " " + essenceType + " essence", false);
                }
            }
            return eb;
        }
        return null;
    }

}
