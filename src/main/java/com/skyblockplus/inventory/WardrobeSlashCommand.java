package com.skyblockplus.inventory;

import static com.skyblockplus.Main.jda;
import static com.skyblockplus.utils.Utils.defaultEmbed;
import static com.skyblockplus.utils.Utils.invalidEmbed;

import com.skyblockplus.utils.Player;
import com.skyblockplus.utils.slashcommand.SlashCommand;
import com.skyblockplus.utils.slashcommand.SlashCommandExecutedEvent;
import java.util.List;

public class WardrobeSlashCommand extends SlashCommand {

	public WardrobeSlashCommand() {
		this.name = "wardrobe";
	}

	@Override
	protected void execute(SlashCommandExecutedEvent event) {
		event.logCommand();

		if (event.invalidPlayerOption()) {
			return;
		}

		switch (event.getSubcommandName()) {
			case "list":
				event.paginate(
					WardrobeCommand.getPlayerWardrobeList(
						event.player,
						event.getOptionStr("profile"),
						event.getUser(),
						null,
						event.getHook()
					)
				);
				break;
			case "emoji":
				Player player = event.getOptionStr("profile") == null
					? new Player(event.player)
					: new Player(event.player, event.getOptionStr("profile"));
				if (!player.isValid()) {
					event.embed(invalidEmbed(player.getFailCause()));
					return;
				}

				List<String[]> wardrobePages = player.getWardrobe();
				if (wardrobePages != null) {
					event.getHook().deleteOriginal().queue();
					if (player.invMissing.length() > 0) {
						event
							.getChannel()
							.sendMessageEmbeds(defaultEmbed("Missing emojis").setDescription(player.invMissing).build())
							.queue();
					}

					jda.addEventListener(new InventoryPaginator(wardrobePages, event.getChannel(), event.getUser()));
				} else {
					event.embed(invalidEmbed("Inventory API disabled"));
				}
				break;
			default:
				event.embed(event.invalidCommandMessage());
				break;
		}
	}
}
