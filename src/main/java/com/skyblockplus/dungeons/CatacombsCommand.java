package com.skyblockplus.dungeons;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.skyblockplus.skills.SkillsStruct;
import com.skyblockplus.utils.Player;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;

import static com.skyblockplus.utils.BotUtils.*;

public class CatacombsCommand extends Command {

    public CatacombsCommand() {
        this.name = "dungeons";
        this.cooldown = globalCooldown;
        this.aliases = new String[]{"cata", "catacombs"};
    }

    @Override
    protected void execute(CommandEvent event) {
        EmbedBuilder eb = defaultEmbed("Loading...");

        String content = event.getMessage().getContentRaw();
        Message ebMessage = event.getChannel().sendMessage(eb.build()).complete();

        String[] args = content.split(" ");
        if (args.length <= 2 || args.length > 4) {
            eb.setTitle(errorMessage(this.name));
            ebMessage.editMessage(eb.build()).queue();
            return;
        }

        System.out.println(content);

        if (args[1].equals("player")) {
            if (args.length == 4) {
                eb = getPlayerCatacombs(args[2], args[3]);
            } else {
                eb = getPlayerCatacombs(args[2], null);
            }
            ebMessage.editMessage(eb.build()).queue();
            return;
        }
        eb.setTitle(errorMessage(this.name));
        ebMessage.editMessage(eb.build()).queue();
    }

    private EmbedBuilder getPlayerCatacombs(String username, String profileName) {
        Player player = profileName == null ? new Player(username) : new Player(username, profileName);
        if (player.isValid()) {
            EmbedBuilder eb = defaultEmbed("Dungeons for " + player.getUsername(),
                    skyblockStatsLink(player.getUsername(), player.getProfileName()));
            try {
                SkillsStruct skillInfo = player.getCatacombsSkill();
                eb.addField(capitalizeString(skillInfo.skillName) + " (" + skillInfo.skillLevel + ")",
                        simplifyNumber(skillInfo.expCurrent) + " / " + simplifyNumber(skillInfo.expForNext)
                                + "\nTotal XP: " + simplifyNumber(skillInfo.totalSkillExp) + "\nProgress: "
                                + roundProgress(skillInfo.progressToNext),
                        false);
                eb.setDescription("True catacombs level: " + skillInfo.skillLevel + "\nProgress catacombs level: "
                        + roundSkillAverage(skillInfo.skillLevel + skillInfo.progressToNext));

                for (String className : new String[]{"healer", "mage", "berserk", "archer", "tank"}) {
                    skillInfo = player.getDungeonClass(className);
                    eb.addField(capitalizeString(className) + " (" + skillInfo.skillLevel + ")",
                            simplifyNumber(skillInfo.expCurrent) + " / " + simplifyNumber(skillInfo.expForNext)
                                    + "\nTotal XP: " + simplifyNumber(skillInfo.totalSkillExp) + "\nProgress: "
                                    + roundProgress(skillInfo.progressToNext),
                            true);
                }
                eb.addBlankField(true);

                return eb;
            } catch (NullPointerException e) {
                return defaultEmbed("Error fetching player dungeons data");
            }
        }
        return defaultEmbed("Unable to fetch player data");
    }
}
