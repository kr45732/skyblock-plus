package com.skyblockplus.weight;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.skyblockplus.utils.Player;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;

import static com.skyblockplus.utils.Utils.*;

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
            Weight calculatedWeight = new Weight(skillAverageD, slayerD, catacombsD, averageDungeonClassD);
            EmbedBuilder eb = defaultEmbed("Weight Calculator");
            eb.setDescription("**Total Weight**: " + roundAndFormat(calculatedWeight.calculateTotalWeight()));
            eb.addField("Slayer Weight", roundAndFormat(calculatedWeight.calculateSlayerWeight()), false);
            eb.addField("Skills Weight", roundAndFormat(calculatedWeight.calculateSkillsWeight()), false);
            eb.addField("Dungeons Weight", roundAndFormat(calculatedWeight.calculateDungeonsWeight()), false);
            return eb;
        } catch (NumberFormatException e) {
            return defaultEmbed("Invalid input");
        }
    }

    public static EmbedBuilder getPlayerWeight(String username, String profileName) {
        Player player = profileName == null ? new Player(username) : new Player(username, profileName);
        if (player.isValid()) {
            Weight playerWeight = new Weight(player);
            EmbedBuilder eb = player.defaultPlayerEmbed();
            eb.setDescription("**Total Weight**: " + roundAndFormat(playerWeight.getTotalWeight()));
            eb.addField("Slayer Weight", roundAndFormat(playerWeight.getSlayerWeight()), false);
            eb.addField("Skills Weight", roundAndFormat(playerWeight.getSkillsWeight()), false);
            eb.addField("Dungeons Weight", roundAndFormat(playerWeight.getDungeonsWeight()), false);
            return eb;
        }
        return defaultEmbed("Unable to fetch player data");
    }

    @Override
    protected void execute(CommandEvent event) {
        new Thread(
                () -> {
                    EmbedBuilder eb = loadingEmbed();
                    Message ebMessage = event.getChannel().sendMessage(eb.build()).complete();
                    String content = event.getMessage().getContentRaw();
                    String[] args = content.split(" ");

                    logCommand(event.getGuild(), event.getAuthor(), content);

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
        )
                .start();
    }
}
