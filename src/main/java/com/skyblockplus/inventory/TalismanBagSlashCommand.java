package com.skyblockplus.inventory;

import static com.skyblockplus.Main.jda;
import static com.skyblockplus.utils.Utils.defaultEmbed;
import static com.skyblockplus.utils.Utils.invalidEmbed;

import com.skyblockplus.utils.Player;
import com.skyblockplus.utils.slashcommand.SlashCommand;
import com.skyblockplus.utils.slashcommand.SlashCommandExecutedEvent;
import java.util.List;

public class TalismanBagSlashCommand extends SlashCommand {

	public TalismanBagSlashCommand() {
		this.name = "talisman";
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
					TalismanBagCommand.getPlayerTalismansList(
						event.player,
						event.getOptionStr("profile"),
						event.getOptionInt("slot", 0),
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

				List<String[]> talismanBagPages = player.getTalismanBag();
				if (talismanBagPages != null) {
					event.getHook().deleteOriginal().queue();
					if (player.invMissing.length() > 0) {
						event
							.getChannel()
							.sendMessageEmbeds(defaultEmbed("Missing emojis").setDescription(player.invMissing).build())
							.queue();
					}

					jda.addEventListener(new InventoryPaginator(talismanBagPages, event.getChannel(), event.getUser()));
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
