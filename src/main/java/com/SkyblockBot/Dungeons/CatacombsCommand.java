package com.SkyblockBot.Dungeons;

import com.SkyblockBot.Miscellaneous.Player;
import com.SkyblockBot.Skills.SkillsStruct;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;

import static com.SkyblockBot.Miscellaneous.BotUtils.*;

public class CatacombsCommand extends Command {
    Message ebMessage;

    public CatacombsCommand() {
        this.name = "catacombs";
        this.guildOnly = false;
        this.cooldown = globalCooldown;
        this.aliases = new String[]{"cata"};
    }

    @Override
    protected void execute(CommandEvent event) {
        EmbedBuilder eb = defaultEmbed("Loading catacombs data...", null);

        Message message = event.getMessage();
        String content = message.getContentRaw();
        this.ebMessage = event.getChannel().sendMessage(eb.build()).complete();

        String[] args = content.split(" ");
        if (args.length <= 1 || args.length > 4) {
            eb.setTitle(errorMessage(this.name));
            ebMessage.editMessage(eb.build()).queue();
            return;
        }

        for (String value : args) {
            System.out.print(value + " ");
        }
        System.out.println();

        if (args[1].equals("player")) {
            if (args.length == 4) {
                eb = getPlayerCatacombs(args[2], args[3]);
            } else {
                eb = getPlayerCatacombs(args[2], null);
            }
        } else {
            eb.setTitle(errorMessage(this.name));
            ebMessage.editMessage(eb.build()).queue();
            return;
        }

        ebMessage.editMessage(eb.build()).queue();

    }

    public EmbedBuilder getPlayerCatacombs(String username, String profileName) {
        Player player = profileName == null ? new Player(username) : new Player(username, profileName);
        if (player.isValidPlayer()) {
            EmbedBuilder eb = defaultEmbed("Dungeons for " + player.getUsername(), skyblockStatsLink(player.getUsername(), player.getProfileName()));
            try {

                SkillsStruct skillInfo = player.getCatacombsSkill();
                eb.addField(capitalizeString(skillInfo.skillName) + " (" + skillInfo.skillLevel + ")",
                        simplifyNumber(skillInfo.expCurrent) + " / " + simplifyNumber(skillInfo.expForNext) + "\nTotal XP: "
                                + simplifyNumber(skillInfo.totalSkillExp) + "\nProgress: "
                                + roundProgress(skillInfo.progressToNext),
                        false);

                eb.setDescription("True catacombs level: " + skillInfo.skillLevel + "\nProgress catacombs level: "
                        + roundSkillAverage(skillInfo.skillLevel + skillInfo.progressToNext));
                return eb;
            } catch (NullPointerException e) {
                return defaultEmbed("Error fetching player catacombs data", null);
            }
        }
        return defaultEmbed("Unable to fetch player data", null);
    }
}
