package com.skyblockplus.slayer;

import static com.skyblockplus.utils.BotUtils.defaultEmbed;
import static com.skyblockplus.utils.BotUtils.errorMessage;
import static com.skyblockplus.utils.BotUtils.formatNumber;
import static com.skyblockplus.utils.BotUtils.globalCooldown;
import static com.skyblockplus.utils.BotUtils.simplifyNumber;
import static com.skyblockplus.utils.BotUtils.skyblockStatsLink;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.skyblockplus.utils.Player;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;

public class SlayerCommands extends Command {

    public SlayerCommands() {
        this.name = "slayer";
        this.cooldown = globalCooldown;
    }

    @Override
    protected void execute(CommandEvent event) {
        EmbedBuilder eb = defaultEmbed("Loading...");
        Message ebMessage = event.getChannel().sendMessage(eb.build()).complete();

        String content = event.getMessage().getContentRaw();

        String[] args = content.split(" ");
        if (args.length <= 2 || args.length > 4) {
            eb = defaultEmbed(errorMessage(this.name));
            ebMessage.editMessage(eb.build()).queue();
            return;
        }

        System.out.println(content);

        if (args[1].equals("player")) {
            if (args.length == 4) {
                eb = getPlayerSlayer(args[2], args[3]);
            } else
                eb = getPlayerSlayer(args[2], null);
        } else {
            eb = defaultEmbed(errorMessage(this.name));
            ebMessage.editMessage(eb.build()).queue();
            return;
        }

        ebMessage.editMessage(eb.build()).queue();
    }

    private EmbedBuilder getPlayerSlayer(String username, String profileName) {
        Player player = profileName == null ? new Player(username) : new Player(username, profileName);
        if (player.isValid()) {
            EmbedBuilder eb = defaultEmbed("Slayer for " + player.getUsername(),
                    skyblockStatsLink(player.getUsername(), player.getProfileName()));
            eb.setDescription("**Total slayer:** " + formatNumber(player.getSlayer()) + " XP");
            eb.addField("<:sven_packmaster:800002277648891914> Wolf", simplifyNumber(player.getWolfXp()) + " XP", true);
            eb.addField("<:revenant_horror:800002290987302943> Zombie", simplifyNumber(player.getZombieXp()) + " XP",
                    true);
            eb.addField("<:tarantula_broodfather:800002277262884874> Spider",
                    simplifyNumber(player.getSpiderXp()) + " XP", true);
            return eb;
        }
        return defaultEmbed("Unable to fetch player data");
    }
}
