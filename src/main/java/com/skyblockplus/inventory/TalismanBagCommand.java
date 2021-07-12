package com.skyblockplus.inventory;

import static com.skyblockplus.Main.jda;
import static com.skyblockplus.Main.waiter;
import static com.skyblockplus.utils.Utils.*;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.skyblockplus.utils.CustomPaginator;
import com.skyblockplus.utils.Player;
import com.skyblockplus.utils.structs.InvItem;
import com.skyblockplus.utils.structs.PaginatorExtras;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;

public class TalismanBagCommand extends Command {

	private String missingEmoji;

	public TalismanBagCommand() {
		this.name = "talisman";
		this.cooldown = globalCooldown;
		this.aliases = new String[] { "talismans" };
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

				if (((args.length == 3) && args[2].startsWith("slot")) || ((args.length == 4) && args[3].startsWith("slot"))) {
					if (args.length == 4) {
						eb = getPlayerTalismansList(args[1], args[2], args[3], event);
					} else {
						eb = getPlayerTalismansList(args[1], null, args[2], event);
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
						playerEnderChest = getPlayerTalismansEmoji(args[1], args[2]);
					} else {
						playerEnderChest = getPlayerTalismansEmoji(args[1], null);
					}

					if (playerEnderChest != null) {
						ebMessage.delete().queue();
						if (missingEmoji.length() > 0) {
							ebMessage
								.getChannel()
								.sendMessageEmbeds(defaultEmbed("Missing Items").setDescription(missingEmoji).build())
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

	private List<String[]> getPlayerTalismansEmoji(String username, String profileName) {
		Player player = profileName == null ? new Player(username) : new Player(username, profileName);
		if (player.isValid()) {
			List<String[]> talismanBagPages = player.getTalismanBag();

			if (talismanBagPages != null) {
				this.missingEmoji = player.invMissing;
				return talismanBagPages;
			}
		}
		return null;
	}

	private EmbedBuilder getPlayerTalismansList(String username, String profileName, String slotNum, CommandEvent event) {
		Player player = profileName == null ? new Player(username) : new Player(username, profileName);
		if (player.isValid()) {
			Map<Integer, InvItem> talismanBagMap = player.getTalismanBagMap();
			if (talismanBagMap != null) {
				List<String> pageTitles = new ArrayList<>();
				List<String> pageThumbnails = new ArrayList<>();

				CustomPaginator.Builder paginateBuilder = defaultPaginator(waiter, event.getAuthor()).setColumns(1).setItemsPerPage(1);

				for (Map.Entry<Integer, InvItem> currentTalisman : talismanBagMap.entrySet()) {
					InvItem currentTalismanStruct = currentTalisman.getValue();

					if (currentTalismanStruct == null) {
						pageTitles.add("Empty");
						pageThumbnails.add(null);
						paginateBuilder.addItems("**Slot:** " + (currentTalisman.getKey() + 1));
					} else {
						pageTitles.add(currentTalismanStruct.getName() + " x" + currentTalismanStruct.getCount());
						pageThumbnails.add("https://sky.shiiyu.moe/item.gif/" + currentTalismanStruct.getId());
						String itemString = "";
						itemString += "**Slot:** " + (currentTalisman.getKey() + 1);
						itemString += "\n\n**Lore:**\n" + currentTalismanStruct.getLore();
						if (currentTalismanStruct.isRecombobulated()) {
							itemString += "\n(Recombobulated)";
						}

						itemString += "\n\n**Item Creation:** " + currentTalismanStruct.getCreationTimestamp();
						paginateBuilder.addItems(itemString);
					}
				}
				paginateBuilder.setPaginatorExtras(new PaginatorExtras().setTitles(pageTitles).setThumbnails(pageThumbnails));

				int slotNumber = 1;
				try {
					slotNumber = Integer.parseInt(slotNum.replace("slot:", ""));
				} catch (Exception ignored) {}
				paginateBuilder.build().paginate(event.getChannel(), slotNumber);
				return null;
			}
		}
		return invalidEmbed(player.getFailCause());
	}
}
