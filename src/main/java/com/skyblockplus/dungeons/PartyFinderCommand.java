package com.skyblockplus.dungeons;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.skyblockplus.utils.Player;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;

import static com.skyblockplus.utils.Utils.*;

public class PartyFinderCommand extends Command {
    public PartyFinderCommand() {
        this.name = "partyfinder";
        this.cooldown = globalCooldown;
        this.aliases = new String[]{"pf"};
    }

    @Override
    protected void execute(CommandEvent event) {
        EmbedBuilder eb = loadingEmbed();
        Message ebMessage = event.getChannel().sendMessage(eb.build()).complete();
        String content = event.getMessage().getContentRaw();
        String[] args = content.split(" ");

        if (args.length == 2 || args.length == 3) {
            if (args.length == 3) {
                ebMessage.editMessage(getPlayerDungeonInfo(args[1], args[2]).build()).queue();
            } else {
                ebMessage.editMessage(getPlayerDungeonInfo(args[1], null).build()).queue();
            }
            return;
        }

        ebMessage.editMessage(errorMessage(this.name).build()).queue();
    }

    private EmbedBuilder getPlayerDungeonInfo(String username, String profileName) {
        Player player = profileName == null ? new Player(username) : new Player(username, profileName);
        if (player.isValid()) {
            EmbedBuilder eb = defaultEmbed("Dungeon stats for " + player.getUsername());
            eb.setDescription("**Catacombs Level:** " + roundSkillAverage(player.getCatacombsLevel()));
            eb.appendDescription("\n**Secrets:** " + formatNumber(player.getDungeonSecrets()));
            eb.appendDescription("\n**Selected Class:** " + player.getSelectedDungeonClass());
            eb.appendDescription("\n**Hyperion:** " + player.getHyperion());
            eb.appendDescription((player.getBonemerang() == 1 ? "\n**Bonemerang:** " : "\n**Bonemerangs:** ") + player.getBonemerang());
            return eb;
        }
        return defaultEmbed("Unable to fetch player data");
    }

}
