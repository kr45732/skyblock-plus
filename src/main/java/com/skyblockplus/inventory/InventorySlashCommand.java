package com.skyblockplus.inventory;

import static com.skyblockplus.utils.Utils.defaultEmbed;
import static com.skyblockplus.utils.Utils.invalidEmbed;

import com.skyblockplus.utils.slashcommand.SlashCommand;
import com.skyblockplus.utils.slashcommand.SlashCommandExecutedEvent;

public class InventorySlashCommand extends SlashCommand {

	public InventorySlashCommand() {
		this.name = "inventory";
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
					InventoryCommand.getPlayerInventoryList(
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
				String[] playerInventory = InventoryCommand.getPlayerInventory(event.player, event.getOptionStr("profile"));
				if (playerInventory != null) {
					event.getHook().deleteOriginal().queue();
					event.getChannel().sendMessage(playerInventory[0]).complete();
					event.getChannel().sendMessage(playerInventory[1]).queue();
					if (playerInventory[2].length() > 0) {
						event
							.getChannel()
							.sendMessageEmbeds(defaultEmbed("Missing emojis").setDescription(playerInventory[2]).build())
							.queue();
					}
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
