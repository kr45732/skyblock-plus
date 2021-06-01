package com.skyblockplus.dungeons;

import static com.skyblockplus.utils.Utils.*;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.skyblockplus.utils.Player;
import com.skyblockplus.utils.structs.SkillsStruct;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;

public class CatacombsCommand extends Command {

  public CatacombsCommand() {
    this.name = "dungeons";
    this.cooldown = globalCooldown;
    this.aliases = new String[] { "cata", "catacombs" };
  }

  @Override
  protected void execute(CommandEvent event) {
    new Thread(
      () -> {
        EmbedBuilder eb = loadingEmbed();
        Message ebMessage = event
          .getChannel()
          .sendMessage(eb.build())
          .complete();
        String content = event.getMessage().getContentRaw();
        String[] args = content.split(" ");

        logCommand(event.getGuild(), event.getAuthor(), content);

        if (args.length == 3) {
          ebMessage
            .editMessage(getPlayerCatacombs(args[1], args[2]).build())
            .queue();
          return;
        } else if (args.length == 2) {
          ebMessage
            .editMessage(getPlayerCatacombs(args[1], null).build())
            .queue();
          return;
        }
        ebMessage.editMessage(errorMessage(this.name).build()).queue();
      }
    )
      .start();
  }

  private EmbedBuilder getPlayerCatacombs(String username, String profileName) {
    Player player = profileName == null
      ? new Player(username)
      : new Player(username, profileName);
    if (player.isValid()) {
      EmbedBuilder eb = player.defaultPlayerEmbed();
      try {
        SkillsStruct skillInfo = player.getCatacombsSkill();
        eb.addField(
          capitalizeString(skillInfo.skillName) +
          " (" +
          skillInfo.skillLevel +
          ")",
          simplifyNumber(skillInfo.expCurrent) +
          " / " +
          simplifyNumber(skillInfo.expForNext) +
          "\nTotal XP: " +
          simplifyNumber(skillInfo.totalSkillExp) +
          "\nProgress: " +
          roundProgress(skillInfo.progressToNext),
          false
        );
        eb.setDescription(
          "True catacombs level: " +
          skillInfo.skillLevel +
          "\nProgress catacombs level: " +
          roundAndFormat(skillInfo.skillLevel + skillInfo.progressToNext)
        );

        for (String className : new String[] {
          "healer",
          "mage",
          "berserk",
          "archer",
          "tank",
        }) {
          skillInfo = player.getDungeonClass(className);
          eb.addField(
            capitalizeString(className) + " (" + skillInfo.skillLevel + ")",
            simplifyNumber(skillInfo.expCurrent) +
            " / " +
            simplifyNumber(skillInfo.expForNext) +
            "\nTotal XP: " +
            simplifyNumber(skillInfo.totalSkillExp) +
            "\nProgress: " +
            roundProgress(skillInfo.progressToNext),
            true
          );
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
