package com.skyblockplus.inventory;

import static com.skyblockplus.Main.waiter;
import static com.skyblockplus.utils.Utils.*;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.skyblockplus.utils.Player;
import com.skyblockplus.utils.command.CommandExecute;
import com.skyblockplus.utils.command.CustomPaginator;
import com.skyblockplus.utils.structs.PaginatorExtras;
import java.util.Map;
import net.dv8tion.jda.api.EmbedBuilder;

public class SacksCommand extends Command {

	public SacksCommand() {
		this.name = "sacks";
		this.cooldown = globalCooldown;
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

					paginate(getPlayerSacks(username, args.length == 3 ? args[2] : null, event));
					return;
				}

				sendErrorEmbed();
			}
		}
			.submit();
	}

	private EmbedBuilder getPlayerSacks(String username, String profileName, CommandEvent event) {
		Player player = profileName == null ? new Player(username) : new Player(username, profileName);
		if (player.isValid()) {
			Map<String, Integer> sacksMap = player.getPlayerSacks();
			if (sacksMap != null) {
				CustomPaginator.Builder paginateBuilder = defaultPaginator(waiter, event.getAuthor()).setColumns(1).setItemsPerPage(20);

				for (Map.Entry<String, Integer> currentSack : sacksMap.entrySet()) {
					paginateBuilder.addItems("**" + convertSkyblockIdName(currentSack.getKey()) + "**: " + currentSack.getValue());
				}
				paginateBuilder.setPaginatorExtras(
					new PaginatorExtras()
						.setEveryPageTitle(player.getUsername())
						.setEveryPageThumbnail(player.getThumbnailUrl())
						.setEveryPageTitleUrl(player.skyblockStatsLink())
				);
				paginateBuilder.build().paginate(event.getChannel(), 0);
				return null;
			}
		}
		return invalidEmbed(player.getFailCause());
	}
}
