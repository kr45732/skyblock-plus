package com.skyblockplus.inventory;

import static com.skyblockplus.Main.jda;
import static com.skyblockplus.utils.Utils.*;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.skyblockplus.utils.Player;
import com.skyblockplus.utils.command.CommandExecute;
import java.util.List;

public class EnderChestCommand extends Command {

	private String missingEmoji;

	public EnderChestCommand() {
		this.name = "enderchest";
		this.cooldown = globalCooldown;
		this.aliases = new String[] { "echest", "ec" };
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

					List<String[]> playerEnderChest = getPlayerEnderChest(username, args.length == 3 ? args[2] : null);
					if (playerEnderChest != null) {
						ebMessage.delete().queue();
						if (missingEmoji.length() > 0) {
							ebMessage
								.getChannel()
								.sendMessageEmbeds(defaultEmbed("Missing emojis").setDescription(missingEmoji).build())
								.queue();
						}

						jda.addEventListener(new InventoryPaginator(playerEnderChest, ebMessage.getChannel(), event.getAuthor()));
					} else {
						embed(invalidEmbed("Unable to fetch player data"));
					}
					return;
				}

				sendErrorEmbed();
			}
		}
			.submit();
	}

	private List<String[]> getPlayerEnderChest(String username, String profileName) {
		Player player = profileName == null ? new Player(username) : new Player(username, profileName);
		if (player.isValid()) {
			List<String[]> enderChestPages = player.getEnderChest();
			if (enderChestPages != null) {
				this.missingEmoji = player.invMissing;
				return enderChestPages;
			}
		}
		return null;
	}
}
