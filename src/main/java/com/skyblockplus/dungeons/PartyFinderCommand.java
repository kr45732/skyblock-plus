package com.skyblockplus.dungeons;

import static com.skyblockplus.utils.Utils.*;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.skyblockplus.utils.Player;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;

public class PartyFinderCommand extends Command {

	public PartyFinderCommand() {
		this.name = "partyfinder";
		this.cooldown = globalCooldown;
		this.aliases = new String[] { "pf" };
	}

	public static EmbedBuilder getPlayerDungeonInfo(String username, String profileName) {
		Player player = profileName == null ? new Player(username) : new Player(username, profileName);
		if (player.isValid()) {
			EmbedBuilder eb = player.defaultPlayerEmbed();
			eb.setDescription("**Catacombs Level:** " + roundAndFormat(player.getCatacombsLevel()));
			eb.appendDescription("\n**Secrets:** " + formatNumber(player.getDungeonSecrets()));
			eb.appendDescription("\n**Selected Class:** " + player.getSelectedDungeonClass());
			eb.appendDescription(player.getNecronBlade());
			eb.appendDescription(player.getFastestF7Time());
			eb.appendDescription(
				player.getBonemerang() != -1
					? ((player.getBonemerang() == 1 ? "\n**Bonemerang:** " : "\n**Bonemerangs:** ") + player.getBonemerang())
					: ""
			);
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
		)
			.start();
	}
}