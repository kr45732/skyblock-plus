package com.skyblockplus.slayer;

import static com.skyblockplus.utils.Utils.*;

import com.google.gson.JsonElement;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.skyblockplus.utils.Player;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;

public class SlayerCommand extends Command {

	public SlayerCommand() {
		this.name = "slayer";
		this.aliases = new String[] { "slayers" };
		this.cooldown = globalCooldown;
	}

	public static EmbedBuilder getPlayerSlayer(String username, String profileName) {
		Player player = profileName == null ? new Player(username) : new Player(username, profileName);
		if (player.isValid()) {
			EmbedBuilder eb = player.defaultPlayerEmbed();

			JsonElement slayer = higherDepth(player.getProfileJson(), "slayer_bosses");

			int svenOneKills = player.getSlayerBossKills("wolf", 0);
			int svenTwoKills = player.getSlayerBossKills("wolf", 1);
			int svenThreeKills = player.getSlayerBossKills("wolf", 2);
			int svenFourKills = player.getSlayerBossKills("wolf", 3);

			int revOneKills = player.getSlayerBossKills("zombie", 0);
			int revTwoKills = player.getSlayerBossKills("zombie", 1);
			int revThreeKills = player.getSlayerBossKills("zombie", 2);
			int revFourKills = player.getSlayerBossKills("zombie", 3);
			int revFiveKills = player.getSlayerBossKills("zombie", 4);

			int taraOneKills = player.getSlayerBossKills("spider", 0);
			int taraTwoKills = player.getSlayerBossKills("spider", 1);
			int taraThreeKills = player.getSlayerBossKills("spider", 2);
			int taraFourKills = player.getSlayerBossKills("spider", 3);

			int endermanOneKills = player.getSlayerBossKills("enderman", 0);
			int endermanTwoKills = player.getSlayerBossKills("enderman", 1);
			int endermanThreeKills = player.getSlayerBossKills("enderman", 2);
			int endermanFourKills = player.getSlayerBossKills("enderman", 3);

			String svenKills =
				"**Tier 1:** " +
				svenOneKills +
				"\n**Tier 2:** " +
				svenTwoKills +
				"\n**Tier 3:** " +
				svenThreeKills +
				"\n**Tier 4:** " +
				svenFourKills;

			String revKills =
				"**Tier 1:** " +
				revOneKills +
				"\n**Tier 2:** " +
				revTwoKills +
				"\n**Tier 3:** " +
				revThreeKills +
				"\n**Tier 4:** " +
				revFourKills +
				"\n**Tier 5:** " +
				revFiveKills;

			String taraKills =
				"**Tier 1:** " +
				taraOneKills +
				"\n**Tier 2:** " +
				taraTwoKills +
				"\n**Tier 3:** " +
				taraThreeKills +
				"\n**Tier 4:** " +
				taraFourKills;

			String endermanKills =
				"**Tier 1:** " +
				endermanOneKills +
				"\n**Tier 2:** " +
				endermanTwoKills +
				"\n**Tier 3:** " +
				endermanThreeKills +
				"\n**Tier 4:** " +
				endermanFourKills;

			long coinsSpentOnSlayers =
				100L *
				(svenOneKills + revOneKills + taraOneKills) +
				2000L *
				(svenTwoKills + revTwoKills + taraTwoKills) +
				10000L *
				(svenThreeKills + revThreeKills + taraThreeKills) +
				50000L *
				(svenFourKills + revFourKills + taraFourKills) +
				100000L *
				revFiveKills +
				2000L *
				endermanOneKills +
				7500L *
				endermanTwoKills +
				20000L *
				endermanThreeKills +
				50000L *
				endermanFourKills;
			eb.setDescription(
				"**Total slayer:** " +
				formatNumber(player.getTotalSlayer()) +
				" XP\n**Total coins spent:** " +
				simplifyNumber(coinsSpentOnSlayers)
			);
			eb.addField(
				"<:sven_packmaster:800002277648891914> Wolf (" + player.getSlayerLevel("sven") + ")",
				simplifyNumber(player.getSlayer("sven")) + " XP",
				true
			);
			eb.addField(
				"<:revenant_horror:800002290987302943> Zombie (" + player.getSlayerLevel("rev") + ")",
				simplifyNumber(player.getSlayer("rev")) + " XP",
				true
			);
			eb.addField(
				"<:tarantula_broodfather:800002277262884874> Spider (" + player.getSlayerLevel("tara") + ")",
				simplifyNumber(player.getSlayer("tara")) + " XP",
				true
			);

			eb.addField("Boss Kills", svenKills, true);
			eb.addField("Boss Kills", revKills, true);
			eb.addField("Boss Kills", taraKills, true);

			eb.addField(
				"<:voidgloom_seraph:849280131281059881> Enderman (" + player.getSlayerLevel("enderman") + ")",
				simplifyNumber(player.getSlayer("enderman")) + " XP",
				true
			);
			eb.addBlankField(true);
			eb.addBlankField(true);
			eb.addField("Boss Kills", endermanKills, true);

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
					ebMessage.editMessageEmbeds(getPlayerSlayer(args[1], args[2]).build()).queue();
					return;
				} else if (args.length == 2) {
					ebMessage.editMessageEmbeds(getPlayerSlayer(args[1], null).build()).queue();
					return;
				}

				ebMessage.editMessageEmbeds(errorEmbed(this.name).build()).queue();
			}
		);
	}
}
