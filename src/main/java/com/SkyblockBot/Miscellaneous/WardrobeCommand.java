package com.SkyblockBot.Miscellaneous;

import com.SkyblockBot.Utils.ArmorStruct;
import com.SkyblockBot.Utils.Player;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;

import java.util.Map;

import static com.SkyblockBot.Utils.BotUtils.*;

public class WardrobeCommand extends Command {
    Message ebMessage;

    public WardrobeCommand() {
        this.name = "wardrobe";
        this.guildOnly = false;
        this.cooldown = globalCooldown;
    }

    @Override
    protected void execute(CommandEvent event) {
        EmbedBuilder eb = defaultEmbed("Loading player data...", null);
        this.ebMessage = event.getChannel().sendMessage(eb.build()).complete();

        Message message = event.getMessage();
        String content = message.getContentRaw();

        String[] args = content.split(" ");
        if (args.length <= 2 || args.length > 4) {
            eb = defaultEmbed(errorMessage(this.name), null);
            ebMessage.editMessage(eb.build()).queue();
            return;
        }

        for (String value : args) {
            System.out.print(value + " ");
        }
        System.out.println();

        if (args[1].equals("player")) {
            if (args.length == 4) {
                eb = getPlayerStats(args[2], args[3]);
            } else
                eb = getPlayerStats(args[2], null);
        } else {
            eb = defaultEmbed(errorMessage(this.name), null);
            ebMessage.editMessage(eb.build()).queue();
            return;
        }

        ebMessage.editMessage(eb.build()).queue();
    }

    public EmbedBuilder getPlayerStats(String username, String profileName) {
        Player player = profileName == null ? new Player(username) : new Player(username, profileName);
        if (player.isValid()) {
            Map<Integer, ArmorStruct> armorStructMap = player.getWardrobe();
            EmbedBuilder eb = defaultEmbed("Player wardrobe for " + player.getUsername(), skyblockStatsLink(player.getUsername(), player.getProfileName()));

            for (Map.Entry<Integer, ArmorStruct> currentArmour : armorStructMap.entrySet()) {
                eb.addField("Slot " + currentArmour.getKey(), currentArmour.getValue().getHelmet() + "\n" + currentArmour.getValue().getChestplate() + "\n" + currentArmour.getValue().getLeggings() + "\n" + currentArmour.getValue().getBoots(), true);
            }
            return eb;
        }
        return defaultEmbed("Unable to fetch player data", null);
    }
}
