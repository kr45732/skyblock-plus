package com.skyblockplus.inventory;

import static com.skyblockplus.Main.waiter;
import static com.skyblockplus.utils.Utils.*;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.skyblockplus.utils.Player;
import com.skyblockplus.utils.command.CommandExecute;
import com.skyblockplus.utils.command.CustomPaginator;
import com.skyblockplus.utils.structs.InvItem;
import com.skyblockplus.utils.structs.PaginatorExtras;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.dv8tion.jda.api.EmbedBuilder;

public class ArmorCommand extends Command {

	public ArmorCommand() {
		this.name = "armor";
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

					paginate(getPlayerEquippedArmor(username, args.length == 3 ? args[2] : null, event));
					return;
				}

				sendErrorEmbed();
			}
		}
			.submit();
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
						pageThumbnails.add("https://sky.shiiyu.moe/item.gif/" + currentInvStruct.getId());
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
		return invalidEmbed(player.getFailCause());
	}
}
