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
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.skyblockplus.utils.command.CommandExecute;
import com.skyblockplus.utils.command.CustomPaginator;
import com.skyblockplus.utils.command.PaginatorEvent;
import com.skyblockplus.utils.command.PaginatorExtras;
import java.util.Comparator;
import java.util.Map;
import java.util.stream.Collectors;
import net.dv8tion.jda.api.EmbedBuilder;
import org.springframework.stereotype.Component;

@Component
public class CalcDragsCommand extends Command {

	public CalcDragsCommand() {
		this.name = "calcdrags";
		this.cooldown = globalCooldown;
		this.aliases = new String[] { "drags" };
		this.botPermissions = defaultPerms();
	}

	public static EmbedBuilder getCalcDrags(int position, double damageRatio, int eyesPlaced, PaginatorEvent event) {
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
				.collect(Collectors.toList())) {
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

	@Override
	protected void execute(CommandEvent event) {
		new CommandExecute(this, event) {
			@Override
			protected void execute() {
				logCommand();

				int eyesPlaced = getIntOption("eyes", 8);
				int position = getIntOption("position", 1);
				double damageRatio = getDoubleOption("ratio", 1);

				paginate(getCalcDrags(position, damageRatio, eyesPlaced, getPaginatorEvent()));
			}
		}
			.queue();
	}
}
