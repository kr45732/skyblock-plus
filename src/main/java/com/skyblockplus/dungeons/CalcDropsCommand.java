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

package com.skyblockplus.dungeons;

import static com.skyblockplus.utils.Utils.*;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.skyblockplus.utils.command.CommandExecute;
import com.skyblockplus.utils.command.CustomPaginator;
import com.skyblockplus.utils.command.PaginatorEvent;
import com.skyblockplus.utils.structs.PaginatorExtras;
import java.util.Arrays;
import net.dv8tion.jda.api.EmbedBuilder;
import org.springframework.stereotype.Component;

@Component
public class CalcDropsCommand extends Command {

	public CalcDropsCommand() {
		this.name = "calcdrops";
		this.cooldown = globalCooldown;
		this.aliases = new String[] { "drops" };
		this.botPermissions = defaultPerms();
	}

	public static EmbedBuilder getCalcDrops(int floor, int bossLuck, String talisman, PaginatorEvent event) {
		if (floor < 0 || floor > 14) {
			return invalidEmbed("Invalid floor");
		}
		if (bossLuck < 1 || bossLuck > 5) {
			return invalidEmbed("Invalid boss luck level");
		}
		boolean isMaster = floor > 7;

		CustomPaginator.Builder paginateBuilder = defaultPaginator(event.getUser()).setColumns(1).setItemsPerPage(10);
		PaginatorExtras extras = new PaginatorExtras(PaginatorExtras.PaginatorType.EMBED_PAGES);

		JsonObject data = getDungeonLootJson().get("" + floor).getAsJsonObject();
		String combo = talisman + bossLuck;
		for (String chest : Arrays.asList("Wood", "Gold", "Diamond", "Emerald", "Obsidian", "Bedrock")) {
			if (higherDepth(data, chest) == null) {
				continue;
			}
			EmbedBuilder eb = defaultEmbed(
				(isMaster ? "Master " : "") + "Floor " + (isMaster ? floor - 7 : floor) + " Loot",
				"https://wiki.hypixel.net/Catacombs_Floor_" + toRomanNumerals(isMaster ? floor - 7 : floor).toUpperCase()
			);
			eb.setDescription("**Chest:** " + chest + "\nDrop chances listed as `not S+, S+`\n");
			for (JsonElement itemData : higherDepth(data, chest).getAsJsonArray()) {
				String name = higherDepth(itemData, "item").getAsString();
				eb.appendDescription(
					"\n" +
					higherDepth(getEmojiMap(), nameToId(name), "") +
					" **" +
					name +
					"**: " +
					higherDepth(itemData, "drop_chances.S" + combo).getAsString() +
					", " +
					higherDepth(itemData, "drop_chances.S+" + combo).getAsString() +
					" ($" +
					formatNumber(higherDepth(itemData, "cost").getAsLong()) +
					")"
				);
			}
			extras.addEmbedPage(eb);
		}
		event.paginate(paginateBuilder.setPaginatorExtras(extras));
		return null;
	}

	@Override
	protected void execute(CommandEvent event) {
		new CommandExecute(this, event) {
			@Override
			protected void execute() {
				logCommand();

				int bossLuck = getIntOption("luck", 1);
				String talisman =
					switch (getStringOption("accessory", "")) {
						case "talisman" -> "B";
						case "ring" -> "C";
						case "artifact" -> "D";
						default -> "A";
					};

				if (args.length == 2) {
					int floorInt = -1;
					String floor = args[1];
					try {
						if (floor.contains("m")) {
							floorInt = 7 + Integer.parseInt(floor.split("m")[1]);
							if (floorInt <= 7) {
								floorInt = -1;
							}
						} else {
							floorInt = Integer.parseInt(floor.split("f")[1]);
							if (floorInt > 7) {
								floorInt = -1;
							}
						}
					} catch (Exception ignored) {}

					paginate(getCalcDrops(floorInt, bossLuck, talisman, getPaginatorEvent()));
					return;
				}

				sendErrorEmbed();
			}
		}
			.queue();
	}
}
