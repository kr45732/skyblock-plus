package com.skyblockplus.slayer;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.skyblockplus.utils.Player;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;

import static com.skyblockplus.utils.Utils.*;

public class SlayerCommands extends Command {

    public SlayerCommands() {
        this.name = "slayer";
        this.cooldown = globalCooldown;
    }

    @Override
    protected void execute(CommandEvent event) {
        EmbedBuilder eb = loadingEmbed();
        Message ebMessage = event.getChannel().sendMessage(eb.build()).complete();
        String content = event.getMessage().getContentRaw();
        String[] args = content.split(" ");

        logCommand(event.getGuild(), event.getAuthor(), content);

        if (args.length == 3) {
            ebMessage.editMessage(getPlayerSlayer(args[1], args[2]).build()).queue();
            return;
        } else if (args.length == 2) {
            ebMessage.editMessage(getPlayerSlayer(args[1], null).build()).queue();
            return;
        }

        ebMessage.editMessage(errorMessage(this.name).build()).queue();
    }

    private EmbedBuilder getPlayerSlayer(String username, String profileName) {
        Player player = profileName == null ? new Player(username) : new Player(username, profileName);
        if (player.isValid()) {
            EmbedBuilder eb = player.defaultPlayerEmbed();
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
