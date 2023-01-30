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
import com.skyblockplus.utils.command.CustomPaginator;
import com.skyblockplus.utils.command.PaginatorExtras;
import com.skyblockplus.utils.command.SlashCommand;
import com.skyblockplus.utils.command.SlashCommandEvent;
import java.util.Arrays;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import org.springframework.stereotype.Component;

@Component
public class CalcDropsSlashCommand extends SlashCommand {

	public CalcDropsSlashCommand() {
		this.name = "calcdrops";
	}

	@Override
	protected void execute(SlashCommandEvent event) {
		event.paginate(
			getCalcDrops(event.getOptionInt("floor", 1), event.getOptionInt("luck", 1), event.getOptionStr("accessory", "A"), event)
		);
	}

	@Override
	public SlashCommandData getCommandData() {
		return Commands
			.slash(name, "Calculate the drop rate and cost of all chests for a floor")
			.addOptions(
				new OptionData(OptionType.INTEGER, "floor", "Catacombs or master catacombs floor", true)
					.addChoice("Floor 1", 1)
					.addChoice("Floor 2", 2)
					.addChoice("Floor 3", 3)
					.addChoice("Floor 4", 4)
					.addChoice("Floor 5", 5)
					.addChoice("Floor 6", 6)
					.addChoice("Floor 7", 7)
					.addChoice("Master Floor 1", 8)
					.addChoice("Master Floor 2", 9)
					.addChoice("Master Floor 3", 10)
					.addChoice("Master Floor 4", 11)
					.addChoice("Master Floor 5", 12)
					.addChoice("Master Floor 6", 13)
					.addChoice("Master Floor 7", 14),
				new OptionData(OptionType.STRING, "accessory", "Catacombs accessory")
					.addChoice("None", "A")
					.addChoice("Talisman", "B")
					.addChoice("Ring", "C")
					.addChoice("Artifact", "D"),
				new OptionData(OptionType.INTEGER, "luck", "Boss luck level").setRequiredRange(1, 5)
			);
	}

	public static EmbedBuilder getCalcDrops(int floor, int bossLuck, String talisman, SlashCommandEvent event) {
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
			)
				.setDescription("**Chest:** " + chest + "\nDrop chances listed as `not S+, S+`\n");
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
}
