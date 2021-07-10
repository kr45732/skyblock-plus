package com.skyblockplus.skills;

import static com.skyblockplus.utils.Constants.*;
import static com.skyblockplus.utils.Utils.*;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.skyblockplus.utils.Player;
import com.skyblockplus.utils.structs.SkillsStruct;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;

public class SkillsCommand extends Command {

	public SkillsCommand() {
		this.name = "skills";
		this.cooldown = globalCooldown;
		this.aliases = new String[] { "skill" };
	}

	public static EmbedBuilder getPlayerSkill(String username, String profileName) {
		Player player = profileName == null ? new Player(username) : new Player(username, profileName);

		if (player.isValid()) {
			double trueSA = 0;
			double progressSA = 0;
			EmbedBuilder eb = player.defaultPlayerEmbed();

			for (String skillName : allSkillNames) {
				SkillsStruct skillInfo = player.getSkill(skillName);
				if (skillInfo != null) {
					eb.addField(
						skillsEmojiMap.get(skillName) + " " + capitalizeString(skillInfo.skillName) + " (" + skillInfo.skillLevel + ")",
						simplifyNumber(skillInfo.expCurrent) +
						" / " +
						simplifyNumber(skillInfo.expForNext) +
						"\nTotal XP: " +
						simplifyNumber(skillInfo.totalSkillExp) +
						"\nProgress: " +
						(skillInfo.skillLevel == skillInfo.maxSkillLevel ? "MAX" : roundProgress(skillInfo.progressToNext)),
						true
					);
					if (!cosmeticSkillNames.contains(skillName)) {
						trueSA += skillInfo.skillLevel;
						progressSA += skillInfo.skillLevel + skillInfo.progressToNext;
					}
				} else {
					eb.addField(skillsEmojiMap.get(skillName) + " " + capitalizeString(skillName) + " (?) ", "Unable to retrieve", true);
				}
			}
			trueSA /= skillNames.size();
			progressSA /= skillNames.size();
			eb.setDescription("True skill average: " + roundAndFormat(trueSA) + "\nProgress skill average: " + roundAndFormat(progressSA));
			return eb;
		}
		return defaultEmbed("Unable to fetch player data");
	}

	@Override
	protected void execute(CommandEvent event) {
		executor.submit(
			() -> {
				EmbedBuilder eb = loadingEmbed();
				Message ebMessage = event.getChannel().sendMessageEmbeds(eb.build()).complete();
				String content = event.getMessage().getContentRaw();
				String[] args = content.split(" ");

				logCommand(event.getGuild(), event.getAuthor(), content);

				if (args.length == 3) {
					ebMessage.editMessageEmbeds(getPlayerSkill(args[1], args[2]).build()).queue();
					return;
				} else if (args.length == 2) {
					ebMessage.editMessageEmbeds(getPlayerSkill(args[1], null).build()).queue();
					return;
				}

				ebMessage.editMessageEmbeds(errorEmbed(this.name).build()).queue();
			}
		);
	}
}
