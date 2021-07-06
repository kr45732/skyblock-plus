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

public class ArmorCommand extends Command {

	public ArmorCommand() {
		this.name = "armor";
		this.cooldown = globalCooldown;
	}

	@Override
	protected void execute(CommandEvent event) {
		executor.submit(
			() -> {
				EmbedBuilder eb = loadingEmbed();
				Message ebMessage = event.getChannel().sendMessage(eb.build()).complete();
				String content = event.getMessage().getContentRaw();
				String[] args = content.split(" ");

				logCommand(event.getGuild(), event.getAuthor(), content);

				if (args.length == 2 || args.length == 3) {
					if (args.length == 3) {
						eb = getPlayerEquippedArmor(args[1], args[2], event);
					} else {
						eb = getPlayerEquippedArmor(args[1], null, event);
					}

					if (eb == null) {
						ebMessage.delete().queue();
					} else {
						ebMessage.editMessage(eb.build()).queue();
					}
					return;
				}

				ebMessage.editMessage(errorEmbed(this.name).build()).queue();
			}
		);
	}

	private EmbedBuilder getPlayerEquippedArmor(String username, String profileName, CommandEvent event) {
		Player player = profileName == null ? new Player(username) : new Player(username, profileName);
		if (player.isValid()) {
			Map<Integer, InvItem> inventoryMap = player.getInventoryArmorMap();
			if (inventoryMap != null) {
				List<String> pageTitles = new ArrayList<>();
				List<String> pageThumbnails = new ArrayList<>();

				CustomPaginator.Builder paginateBuilder = defaultPaginator(waiter, event.getAuthor()).setColumns(1).setItemsPerPage(1);

				for (Map.Entry<Integer, InvItem> currentInvSlot : inventoryMap.entrySet()) {
					InvItem currentInvStruct = currentInvSlot.getValue();

					if (currentInvStruct == null) {
						pageTitles.add("Empty");
						pageThumbnails.add(null);

						String slotName = "";
						switch ((currentInvSlot.getKey())) {
							case 4:
								slotName = "Boots";
								break;
							case 3:
								slotName = "Leggings";
								break;
							case 2:
								slotName = "Chestplate";
								break;
							case 1:
								slotName = "Helmet";
								break;
						}

						paginateBuilder.addItems("**Slot:** " + slotName);
					} else {
						pageTitles.add(currentInvStruct.getName() + " x" + currentInvStruct.getCount());
						pageThumbnails.add("https://sky.lea.moe/item.gif/" + currentInvStruct.getId());
						String itemString = "";

						String slotName = "";
						switch ((currentInvSlot.getKey())) {
							case 4:
								slotName = "Boots";
								break;
							case 3:
								slotName = "Leggings";
								break;
							case 2:
								slotName = "Chestplate";
								break;
							case 1:
								slotName = "Helmet";
								break;
						}

						itemString += "**Slot:** " + slotName;
						itemString += "\n\n**Lore:**\n" + currentInvStruct.getLore();
						if (currentInvStruct.isRecombobulated()) {
							itemString += "\n(Recombobulated)";
						}

						itemString += "\n\n**Item Creation:** " + currentInvStruct.getCreationTimestamp();
						paginateBuilder.addItems(itemString);
					}
				}
				paginateBuilder.setPaginatorExtras(new PaginatorExtras().setTitles(pageTitles).setThumbnails(pageThumbnails));

				paginateBuilder.build().paginate(event.getChannel(), 0);
				return null;
			}
		}
		return defaultEmbed("Unable to fetch player data");
	}
}
