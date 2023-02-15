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

package com.skyblockplus.miscellaneous;

import static com.skyblockplus.utils.Utils.*;

import com.google.gson.JsonElement;
import com.skyblockplus.utils.command.CustomPaginator;
import com.skyblockplus.utils.command.PaginatorExtras;
import com.skyblockplus.utils.command.SlashCommand;
import com.skyblockplus.utils.command.SlashCommandEvent;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Map;
import java.util.stream.Collectors;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import org.springframework.stereotype.Component;

@Component
public class CalcDragsSlashCommand extends SlashCommand {

	public CalcDragsSlashCommand() {
		this.name = "calcdrags";
	}

	@Override
	protected void execute(SlashCommandEvent event) {
		event.paginate(
			getCalcDrags(event.getOptionInt("position", 1), event.getOptionDouble("ratio", 1), event.getOptionInt("eyes", 8), event)
		);
	}

	@Override
	public SlashCommandData getCommandData() {
		return Commands
			.slash(name, "Calculate your loot quality and loot from dragons in the end")
			.addOptions(
				new OptionData(OptionType.INTEGER, "position", "Your position on damage dealt").setRequiredRange(1, 25),
				new OptionData(OptionType.NUMBER, "ratio", "Ratio create your damage to the 1st place's damage").setRequiredRange(0.0, 1.0),
				new OptionData(OptionType.INTEGER, "eyes", "Number create eyes you placed").setRequiredRange(0, 8)
			);
	}

	public static EmbedBuilder getCalcDrags(int position, double damageRatio, int eyesPlaced, SlashCommandEvent event) {
		position = damageRatio == 1 ? 1 : position;
		damageRatio = position == 1 ? 1 : damageRatio;
		int estimatedQuality = (int) (
			switch (position) {
				case 1 -> 200;
				case 2 -> 175;
				case 3 -> 150;
				case 4 -> 125;
				case 5 -> 110;
				case 6, 7, 8 -> 100;
				case 9, 10 -> 90;
				case 11, 12 -> 80;
				default -> 70;
			} +
			100 *
			damageRatio +
			100 *
			eyesPlaced
		);

		CustomPaginator.Builder paginateBuilder = event.getPaginator().setItemsPerPage(10);
		PaginatorExtras extras = new PaginatorExtras(PaginatorExtras.PaginatorType.EMBED_PAGES);
		for (Map.Entry<String, JsonElement> dragon : getDragonLootJson().getAsJsonObject().entrySet()) {
			EmbedBuilder eb = defaultEmbed(capitalizeString(dragon.getKey()) + " Dragon Loot");
			eb.setDescription(
				"**Your Estimated Quality:** " +
				estimatedQuality +
				"\n**Eyes Placed:** " +
				eyesPlaced +
				"\n**Position:** " +
				position +
				"\n**Damage Ratio:** " +
				formatNumber(damageRatio)
			);
			eb.setThumbnail("https://wiki.hypixel.net/images/6/60/Minecraft_entities_ender_dragon.gif");
			for (Map.Entry<String, JsonElement> entry : dragon
				.getValue()
				.getAsJsonObject()
				.entrySet()
				.stream()
				.sorted(Comparator.comparingInt(e -> -higherDepth(e.getValue(), "quality", 0)))
				.collect(Collectors.toCollection(ArrayList::new))) {
				eb.addField(
					getEmoji(nameToId(entry.getKey())) +
					" " +
					entry.getKey() +
					(higherDepth(entry.getValue(), "unique", false) ? " (Unique)" : ""),
					"➜ Quality: " +
					higherDepth(entry.getValue(), "quality", 0) +
					"\n➜ Drop Chance: " +
					(
						higherDepth(entry.getValue(), "eye", false)
							? formatNumber(
								Double.parseDouble(higherDepth(entry.getValue(), "drop_chance", "").replace("%", "")) * eyesPlaced
							) +
							"%"
							: higherDepth(entry.getValue(), "drop_chance", "")
					),
					true
				);
			}
			for (int i = 0; i < 3 - eb.getFields().size() % 3; i++) {
				eb.addBlankField(true);
			}
			extras.addEmbedPage(eb);
		}

		event.paginate(paginateBuilder.setPaginatorExtras(extras));
		return null;
	}
}
