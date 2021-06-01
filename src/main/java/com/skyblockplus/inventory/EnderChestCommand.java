package com.skyblockplus.inventory;

import static com.skyblockplus.Main.jda;
import static com.skyblockplus.utils.Utils.*;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.skyblockplus.utils.Player;
import java.util.List;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;

public class EnderChestCommand extends Command {

	private String missingEmoji;

	public EnderChestCommand() {
		this.name = "enderchest";
		this.cooldown = globalCooldown;
		this.aliases = new String[] { "echest" };
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
					List<String[]> playerEnderChest;
					if (args.length == 3) {
						playerEnderChest = getPlayerEnderChest(args[1], args[2]);
					} else {
						playerEnderChest = getPlayerEnderChest(args[1], null);
					}

					if (playerEnderChest != null) {
						ebMessage.delete().queue();
						if (missingEmoji.length() > 0) {
							ebMessage.getChannel().sendMessage(defaultEmbed("Missing Items").setDescription(missingEmoji).build()).queue();
						}

						jda.addEventListener(new InventoryPaginator(playerEnderChest, ebMessage.getChannel(), event.getAuthor()));
					} else {
						ebMessage.editMessage(defaultEmbed("Error").setDescription("Unable to fetch player data").build()).queue();
					}
					return;
				}

				ebMessage.editMessage(errorMessage(this.name).build()).queue();
			}
		)
			.start();
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
