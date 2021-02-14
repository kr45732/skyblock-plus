package com.skyblockplus.weight;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.skyblockplus.utils.Player;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;

import static com.skyblockplus.utils.BotUtils.*;

public class WeightCommand extends Command {
    Message ebMessage;

    public WeightCommand() {
        this.name = "weight";
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
                eb = getPlayerWeight(args[2], args[3]);
            } else
                eb = getPlayerWeight(args[2], null);
        } else {
            eb = defaultEmbed(errorMessage(this.name), null);
            ebMessage.editMessage(eb.build()).queue();
            return;
        }

        ebMessage.editMessage(eb.build()).queue();

    }

    public EmbedBuilder getPlayerWeight(String username, String profileName) {
        Player player = profileName == null ? new Player(username) : new Player(username, profileName);
        if (player.isValid()) {
            Weight playerWeight = new Weight(player);
            EmbedBuilder eb = defaultEmbed("Weight for " + player.getUsername(),
                    skyblockStatsLink(player.getUsername(), player.getProfileName()));
            eb.setDescription("**Total Weight**: " + roundSkillAverage(playerWeight.getPlayerWeight()));
            eb.addField("Slayer Weight", roundSkillAverage(playerWeight.getSlayerWeight()), false);
            eb.addField("Skills Weight", roundSkillAverage(playerWeight.getSkillsWeight()), false);
            eb.addField("Dungeons Weight", roundSkillAverage(playerWeight.getDungeonsWeight()), false);
            return eb;
        }
        return defaultEmbed("Unable to fetch player data", null);
    }
}
