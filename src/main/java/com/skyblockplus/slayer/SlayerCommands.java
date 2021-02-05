package com.skyblockplus.slayer;

import com.skyblockplus.utils.Player;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;

import static com.skyblockplus.utils.BotUtils.*;

public class SlayerCommands extends Command {
    Message ebMessage;

    public SlayerCommands() {
        this.name = "slayer";
        this.guildOnly = false;
        this.cooldown = globalCooldown;
    }

    @Override
    protected void execute(CommandEvent event) {
        EmbedBuilder eb = defaultEmbed("Loading slayer data...", null);
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
                eb = getPlayerSlayer(args[2], args[3]);
            } else
                eb = getPlayerSlayer(args[2], null);
        } else {
            eb = defaultEmbed(errorMessage(this.name), null);
            ebMessage.editMessage(eb.build()).queue();
            return;
        }

        ebMessage.editMessage(eb.build()).queue();
    }

    public EmbedBuilder getPlayerSlayer(String username, String profileName) {
        Player player = profileName == null ? new Player(username) : new Player(username, profileName);
        if (player.isValid()) {
            EmbedBuilder eb = defaultEmbed("Slayer for " + player.getUsername(), skyblockStatsLink(player.getUsername(), player.getProfileName()));
            eb.setDescription("**Total slayer:** " + formatNumber(player.getSlayer()) + " XP");
            eb.addField("<:sven_packmaster:800002277648891914> Wolf", simplifyNumber(player.getWolfXp()) + " XP", true);
            eb.addField("<:revenant_horror:800002290987302943> Zombie", simplifyNumber(player.getZombieXp()) + " XP", true);
            eb.addField("<:tarantula_broodfather:800002277262884874> Spider", simplifyNumber(player.getSpiderXp()) + " XP", true);
//            eb.setThumbnail("https://cravatar.eu/helmhead/" + player.getPlayerUuid());
            return eb;
        }
        return defaultEmbed("Unable to fetch player data", null);
    }
}
