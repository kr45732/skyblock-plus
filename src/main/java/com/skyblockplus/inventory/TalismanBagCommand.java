/*
 * Skyblock Plus - A Skyblock focused Discord bot with many commands and customizable features to improve the experience of Skyblock players and guild staff!
 * Copyright (c) 2021 kr45732
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.skyblockplus.inventory;

import static com.skyblockplus.utils.Utils.*;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.skyblockplus.utils.Player;
import com.skyblockplus.utils.command.CommandExecute;
import com.skyblockplus.utils.command.PaginatorEvent;
import com.skyblockplus.utils.structs.InvItem;
import java.util.List;
import java.util.Map;
import net.dv8tion.jda.api.EmbedBuilder;
import org.springframework.stereotype.Component;

@Component
public class TalismanBagCommand extends Command {

	public TalismanBagCommand() {
		this.name = "talisman";
		this.cooldown = globalCooldown;
		this.aliases = new String[] { "talismans" };
		this.botPermissions = defaultPerms();
	}

	public static EmbedBuilder getPlayerTalismansList(String username, String profileName, int slotNum, PaginatorEvent event) {
		Player player = profileName == null ? new Player(username) : new Player(username, profileName);
		if (player.isValid()) {
			Map<Integer, InvItem> talismanBagMap = player.getTalismanBagMap();
			if (talismanBagMap != null) {
				new InventoryListPaginator(player, talismanBagMap, slotNum, event);
				return null;
			}
		}
		return player.getFailEmbed();
	}

	@Override
	protected void execute(CommandEvent event) {
		new CommandExecute(this, event) {
			@Override
			protected void execute() {
				logCommand();

				int slotNumber = -1;
				for (int i = 0; i < args.length; i++) {
					if (args[i].startsWith("slot:")) {
						try {
							slotNumber = Math.max(0, Integer.parseInt(args[i].split("slot:")[1]));
							removeArg(i);
						} catch (Exception ignored) {}
					}
				}

				if (slotNumber != -1 && (args.length == 3 || args.length == 2 || args.length == 1)) {
					if (getMentionedUsername(args.length == 1 ? -1 : 1)) {
						return;
					}

					paginate(getPlayerTalismansList(player, args.length == 3 ? args[2] : null, slotNumber, getPaginatorEvent()));
					return;
				} else if (args.length == 3 || args.length == 2 || args.length == 1) {
					if (getMentionedUsername(args.length == 1 ? -1 : 1)) {
						return;
					}

					paginate(getPlayerTalismansEmoji(player, args.length == 3 ? args[2] : null, getPaginatorEvent()), true);
					return;
				}

				sendErrorEmbed();
			}
		}
			.queue();
	}

	public static EmbedBuilder getPlayerTalismansEmoji(String username, String profileName, PaginatorEvent event) {
		Player player = profileName == null ? new Player(username) : new Player(username, profileName);
		if (player.isValid()) {
			List<String[]> talismanBag = player.getTalismanBag();
			if (talismanBag != null) {
				if (player.invMissing.length() > 0) {
					event.getChannel().sendMessageEmbeds(defaultEmbed("Missing emojis").setDescription(player.invMissing).build()).queue();
				}

				new InventoryPaginator(talismanBag, "Talisman Bag", player, event);
				return null;
			}
			return invalidEmbed(player.getUsernameFixed() + "'s inventory API is disabled");
		}
		return player.getFailEmbed();
	}
}
