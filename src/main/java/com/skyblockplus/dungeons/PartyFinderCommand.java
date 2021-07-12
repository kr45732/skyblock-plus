package com.skyblockplus.dungeons;

import static com.skyblockplus.utils.Utils.*;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.skyblockplus.utils.Player;
import java.util.Arrays;
import java.util.Set;
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
			eb.appendDescription(player.getFastestF7Time());
			Set<String> necronBlade = player.getItemsPlayerHas(
				Arrays.asList("HYPERION", "VALKYRIE", "SCYLLA", "AXE_OF_THE_SHREDDED", "JUJU_SHORTBOW", "TERMINATOR")
			);
			eb.appendDescription(
				"\n**Meta Items player has:** " +
				(necronBlade != null ? (necronBlade.size() > 0 ? String.join(", ", necronBlade) : "None") : "Inventory API disabled")
			);
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

				if (args.length == 2 || args.length == 3) {
					if (args.length == 3) {
						ebMessage.editMessageEmbeds(getPlayerDungeonInfo(args[1], args[2]).build()).queue();
					} else {
						ebMessage.editMessageEmbeds(getPlayerDungeonInfo(args[1], null).build()).queue();
					}
					return;
				}

				ebMessage.editMessageEmbeds(errorEmbed(this.name).build()).queue();
			}
		);
	}
}
