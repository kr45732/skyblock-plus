package com.skyblockplus.weight;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.skyblockplus.utils.Player;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;

import static com.skyblockplus.utils.BotUtils.*;

public class WeightCommand extends Command {

    public WeightCommand() {
        this.name = "weight";
        this.cooldown = globalCooldown;
    }

    @Override
    protected void execute(CommandEvent event) {
        EmbedBuilder eb = defaultEmbed("Loading...", null);
        Message ebMessage = event.getChannel().sendMessage(eb.build()).complete();
        String content = event.getMessage().getContentRaw();
        String[] args = content.split(" ");

        System.out.println(content);

        if (args.length == 6 && args[1].equals("calculate")) {
            try {
                ebMessage.editMessage(calculateWeight(args[2], args[3], args[4], args[5]).build()).queue();
                return;
            } catch (Exception ignored) {
            }
        } else if (args.length == 3) {
            ebMessage.editMessage(getPlayerWeight(args[1], args[2]).build()).queue();
            return;
        } else if (args.length == 2) {
            ebMessage.editMessage(getPlayerWeight(args[1], null).build()).queue();
            return;
        }

        ebMessage.editMessage(errorMessage(this.name).build()).queue();
    }

    private EmbedBuilder calculateWeight(String skillAverage, String slayer, String catacombs,
                                         String averageDungeonClass) {
        try {
            double skillAverageD = Double.parseDouble(skillAverage);
            double slayerD = Double.parseDouble(slayer);
            double catacombsD = Double.parseDouble(catacombs);
            double averageDungeonClassD = Double.parseDouble(averageDungeonClass);
            Weight calculatedWeight = new Weight(skillAverageD, slayerD, catacombsD, averageDungeonClassD);
            EmbedBuilder eb = defaultEmbed("Weight Calculator");
            eb.setDescription("**Total Weight**: " + roundSkillAverage(calculatedWeight.calculateTotalWeight()));
            eb.addField("Slayer Weight", roundSkillAverage(calculatedWeight.calculateSlayerWeight()), false);
            eb.addField("Skills Weight", roundSkillAverage(calculatedWeight.calculateSkillsWeight()), false);
            eb.addField("Dungeons Weight", roundSkillAverage(calculatedWeight.calculateDungeonsWeight()), false);
            return eb;
        } catch (NumberFormatException e) {
            return defaultEmbed("Invalid input");
        }
    }

    private EmbedBuilder getPlayerWeight(String username, String profileName) {
        Player player = profileName == null ? new Player(username) : new Player(username, profileName);
        if (player.isValid()) {
            Weight playerWeight = new Weight(player);
            EmbedBuilder eb = defaultEmbed("Weight for " + player.getUsername(),
                    skyblockStatsLink(player.getUsername(), player.getProfileName()));
            eb.setDescription("**Total Weight**: " + roundSkillAverage(playerWeight.getTotalWeight()));
            eb.addField("Slayer Weight", roundSkillAverage(playerWeight.getSlayerWeight()), false);
            eb.addField("Skills Weight", roundSkillAverage(playerWeight.getSkillsWeight()), false);
            eb.addField("Dungeons Weight", roundSkillAverage(playerWeight.getDungeonsWeight()), false);
            return eb;
        }
        return defaultEmbed("Unable to fetch player data");
    }
}
