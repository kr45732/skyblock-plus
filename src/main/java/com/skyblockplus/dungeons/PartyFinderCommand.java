package com.skyblockplus.dungeons;

import static com.skyblockplus.utils.Constants.DUNGEON_META_ITEMS;
import static com.skyblockplus.utils.Utils.*;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.skyblockplus.utils.Player;
import com.skyblockplus.utils.command.CommandExecute;
import java.util.Set;
import net.dv8tion.jda.api.EmbedBuilder;

public class PartyFinderCommand extends Command {

	public PartyFinderCommand() {
		this.name = "partyfinder";
		this.cooldown = globalCooldown;
		this.aliases = new String[] { "pf" };
	}

	public static EmbedBuilder getPartyFinderInfo(String username, String profileName) {
		Player player = profileName == null ? new Player(username) : new Player(username, profileName);
		if (player.isValid()) {
			EmbedBuilder eb = player.defaultPlayerEmbed();
			eb.setDescription("**Catacombs Level:** " + roundAndFormat(player.getCatacombsLevel()));
			eb.appendDescription("\n**Secrets:** " + formatNumber(player.getDungeonSecrets()));
			eb.appendDescription("\n**Selected Class:** " + player.getSelectedDungeonClass());
			eb.appendDescription(player.getFastestF7Time());
			Set<String> necronBlade = player.getItemsPlayerHas(DUNGEON_META_ITEMS);
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
		new CommandExecute(this, event) {
			@Override
			protected void execute() {
				logCommand();

				if (args.length == 3 || args.length == 2 || args.length == 1) {
					if (getMentionedUsername(args.length == 1 ? -1 : 1)) {
						return;
					}

					embed(getPartyFinderInfo(username, args.length == 3 ? args[2] : null));
					return;
				}

				sendErrorEmbed();
			}
		}
			.submit();
	}
}
