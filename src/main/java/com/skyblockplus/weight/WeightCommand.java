package com.skyblockplus.weight;

import static com.skyblockplus.utils.Constants.*;
import static com.skyblockplus.utils.Utils.*;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.skyblockplus.utils.Player;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;

public class WeightCommand extends Command {

	public WeightCommand() {
		this.name = "weight";
		this.cooldown = globalCooldown;
	}

	public static EmbedBuilder calculateWeight(String skillAverage, String slayer, String catacombs, String averageDungeonClass) {
		try {
			double skillAverageD = Double.parseDouble(skillAverage);
			double slayerD = Double.parseDouble(slayer);
			double catacombsD = Double.parseDouble(catacombs);
			double averageDungeonClassD = Double.parseDouble(averageDungeonClass);
			EmbedBuilder eb = defaultEmbed("Weight Calculator");
			eb.setDescription("**Total Weight**: " + Weight.of(skillAverageD, slayerD, catacombsD, averageDungeonClassD));
			eb.addField("Slayer Weight", roundAndFormat(Weight.calculateSkillsWeight(skillAverageD)), false);
			eb.addField("Skills Weight", roundAndFormat(Weight.calculateSlayerWeight(slayerD)), false);
			eb.addField("Dungeons Weight", roundAndFormat(Weight.calculateDungeonsWeight(catacombsD, averageDungeonClassD)), false);
			return eb;
		} catch (NumberFormatException e) {
			return defaultEmbed("Invalid input");
		}
	}

	public static EmbedBuilder getPlayerWeight(String username, String profileName) {
		Player player = profileName == null ? new Player(username) : new Player(username, profileName);
		if (player.isValid()) {
			Weight weight = new Weight(player);
			EmbedBuilder eb = player.defaultPlayerEmbed();
			String slayerStr = "";
			for (String slayerName : slayerNames) {
				slayerStr += capitalizeString(slayerName) + ": " + weight.getSlayerWeight().getSlayerWeight(slayerName).get() + "\n";
			}
			String skillsStr = "";
			for (String skillName : skillNames) {
				skillsStr += capitalizeString(skillName) + ": " + weight.getSkillsWeight().getSkillsWeight(skillName).get() + "\n";
			}
			String dungeonsStr = "";
			dungeonsStr += capitalizeString("catacombs") + ": " + weight.getDungeonsWeight().getDungeonWeight("catacombs").get() + "\n";
			for (String dungeonClassName : dungeonClassNames) {
				dungeonsStr +=
					capitalizeString(dungeonClassName) + ": " + weight.getDungeonsWeight().getClassWeight(dungeonClassName).get() + "\n";
			}

			eb.addField("Slayer | " + weight.getSlayerWeight().getWeightStruct().get(), slayerStr, false);
			eb.addField("Skills | " + weight.getSkillsWeight().getWeightStruct().get(), skillsStr, false);
			eb.addField("Dungeons | " + weight.getDungeonsWeight().getWeightStruct().get(), dungeonsStr, false);
			eb.setDescription("**Total Weight:** " + weight.getTotalWeight(false).get());
			return eb;
		}
		return invalidEmbed(player.getFailCause());
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

				if (args.length == 6 && args[1].equals("calculate")) {
					try {
						ebMessage.editMessageEmbeds(calculateWeight(args[2], args[3], args[4], args[5]).build()).queue();
						return;
					} catch (Exception ignored) {}
				} else if (args.length == 3) {
					ebMessage.editMessageEmbeds(getPlayerWeight(args[1], args[2]).build()).queue();
					return;
				} else if (args.length == 2) {
					ebMessage.editMessageEmbeds(getPlayerWeight(args[1], null).build()).queue();
					return;
				}

				ebMessage.editMessageEmbeds(errorEmbed(this.name).build()).queue();
			}
		);
	}
}
