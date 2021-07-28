package com.skyblockplus.inventory;

import static com.skyblockplus.Main.jda;
import static com.skyblockplus.Main.waiter;
import static com.skyblockplus.utils.Utils.*;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.skyblockplus.utils.CustomPaginator;
import com.skyblockplus.utils.Player;
import com.skyblockplus.utils.structs.ArmorStruct;
import com.skyblockplus.utils.structs.PaginatorExtras;
import java.util.List;
import java.util.Map;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;

public class WardrobeCommand extends Command {

	private String missingEmoji;

	public WardrobeCommand() {
		this.name = "wardrobe";
		this.cooldown = globalCooldown;
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

				if ((args.length == 3 || args.length == 4) && args[1].equals("list")) {
					if (args.length == 4) {
						eb = getPlayerWardrobeList(args[2], args[3], event);
					} else {
						eb = getPlayerWardrobeList(args[2], null, event);
					}

					if (eb == null) {
						ebMessage.delete().queue();
					} else {
						ebMessage.editMessageEmbeds(eb.build()).queue();
					}
					return;
				} else if (args.length == 2 || args.length == 3) {
					List<String[]> playerEnderChest;
					if (args.length == 3) {
						playerEnderChest = getPlayerWardrobe(args[1], args[2]);
					} else {
						playerEnderChest = getPlayerWardrobe(args[1], null);
					}

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
						ebMessage.editMessageEmbeds(invalidEmbed("Unable to fetch player data").build()).queue();
					}
					return;
				}

				ebMessage.editMessageEmbeds(errorEmbed(this.name).build()).queue();
			}
		);
	}

	private List<String[]> getPlayerWardrobe(String username, String profileName) {
		Player player = profileName == null ? new Player(username) : new Player(username, profileName);
		if (player.isValid()) {
			List<String[]> talismanBagPages = player.getWardrobe();

			if (talismanBagPages != null) {
				this.missingEmoji = player.invMissing;
				return talismanBagPages;
			}
		}
		return null;
	}

	private EmbedBuilder getPlayerWardrobeList(String username, String profileName, CommandEvent event) {
		Player player = profileName == null ? new Player(username) : new Player(username, profileName);
		if (player.isValid()) {
			Map<Integer, ArmorStruct> armorStructMap = player.getWardrobeList();
			if (armorStructMap != null) {
				CustomPaginator.Builder paginateBuilder = defaultPaginator(waiter, event.getAuthor()).setColumns(1).setItemsPerPage(4);

				for (Map.Entry<Integer, ArmorStruct> currentArmour : armorStructMap.entrySet()) {
					paginateBuilder.addItems(
						"**__Slot " +
						(currentArmour.getKey() + 1) +
						"__**\n" +
						currentArmour.getValue().getHelmet() +
						"\n" +
						currentArmour.getValue().getChestplate() +
						"\n" +
						currentArmour.getValue().getLeggings() +
						"\n" +
						currentArmour.getValue().getBoots() +
						"\n"
					);
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
