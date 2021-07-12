package com.skyblockplus.inventory;

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

public class InventoryCommand extends Command {

	public InventoryCommand() {
		this.name = "inventory";
		this.cooldown = globalCooldown;
		this.aliases = new String[] { "inv" };
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
						eb = getPlayerInventoryList(args[1], args[2], args[3], event);
					} else {
						eb = getPlayerInventoryList(args[1], null, args[2], event);
					}

					if (eb == null) {
						ebMessage.delete().queue();
					} else {
						ebMessage.editMessageEmbeds(eb.build()).queue();
					}
					return;
				} else if (args.length == 2 || args.length == 3) {
					String[] playerInventory;
					if (args.length == 3) {
						playerInventory = getPlayerInventory(args[1], args[2]);
					} else {
						playerInventory = getPlayerInventory(args[1], null);
					}

					if (playerInventory != null) {
						ebMessage.delete().queue();
						ebMessage.getChannel().sendMessage(playerInventory[0]).queue();
						ebMessage.getChannel().sendMessage(playerInventory[1]).queue();
						if (playerInventory[2].length() > 0) {
							ebMessage
								.getChannel()
								.sendMessageEmbeds(defaultEmbed("Missing Items").setDescription(playerInventory[2]).build())
								.queue();
						}
					} else {
						ebMessage.editMessageEmbeds(invalidEmbed("Unable to fetch player data").build()).queue();
					}
					return;
				}

				ebMessage.editMessageEmbeds(errorEmbed(this.name).build()).queue();
			}
		);
	}

	private EmbedBuilder getPlayerInventoryList(String username, String profileName, String slotNum, CommandEvent event) {
		Player player = profileName == null ? new Player(username) : new Player(username, profileName);
		if (player.isValid()) {
			Map<Integer, InvItem> inventoryMap = player.getInventoryMap();
			if (inventoryMap != null) {
				List<String> pageTitles = new ArrayList<>();
				List<String> pageThumbnails = new ArrayList<>();

				CustomPaginator.Builder paginateBuilder = defaultPaginator(waiter, event.getAuthor()).setColumns(1).setItemsPerPage(1);

				for (Map.Entry<Integer, InvItem> currentInvSlot : inventoryMap.entrySet()) {
					InvItem currentInvStruct = currentInvSlot.getValue();

					if (currentInvStruct == null) {
						pageTitles.add("Empty");
						pageThumbnails.add(null);
						paginateBuilder.addItems("**Slot:** " + (currentInvSlot.getKey() + 1));
					} else {
						pageTitles.add(currentInvStruct.getName() + " x" + currentInvStruct.getCount());
						pageThumbnails.add("https://sky.shiiyu.moe/item.gif/" + currentInvStruct.getId());
						String itemString = "";
						itemString += "**Slot:** " + (currentInvSlot.getKey() + 1);
						itemString += "\n\n**Lore:**\n" + currentInvStruct.getLore();
						if (currentInvStruct.isRecombobulated()) {
							itemString += "\n(Recombobulated)";
						}

						itemString += "\n\n**Item Creation:** " + currentInvStruct.getCreationTimestamp();
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

	private String[] getPlayerInventory(String username, String profileName) {
		Player player = profileName == null ? new Player(username) : new Player(username, profileName);
		if (player.isValid()) {
			String[] temp = player.getInventory();
			if (temp != null) {
				return new String[] { temp[0], temp[1], player.invMissing };
			}
		}
		return null;
	}
}
