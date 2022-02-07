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

import com.google.gson.JsonElement;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.skyblockplus.utils.command.CommandExecute;
import net.dv8tion.jda.api.EmbedBuilder;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.skyblockplus.utils.Utils.*;

public class RecipeCommand extends Command {
	public static List<String> allRecipeIds;

	public RecipeCommand() {
		this.name = "recipe";
		this.cooldown = globalCooldown;
		this.botPermissions = defaultPerms();

		allRecipeIds = getInternalJsonMappings().entrySet().stream().filter(e -> higherDepth(e.getValue(), "recipe") != null).map(Map.Entry::getKey).collect(Collectors.toList());
	}

	public static EmbedBuilder getRecipe(String item) {
		String id = nameToId(item);

		if (higherDepth(getInternalJsonMappings(), id) == null) {
			id = getClosestMatchFromIds(item, allRecipeIds);
		}
		String name = idToName(id);

		JsonElement infoJson = getInternalJsonMappings().get(id);
		if (higherDepth(infoJson, "recipe") == null) {
			return invalidEmbed("No recipe found for " + name);
		}

		EmbedBuilder eb = defaultEmbed("Recipe of " + name);
		for (Map.Entry<String, JsonElement> entry : higherDepth(infoJson, "recipe").getAsJsonObject().entrySet()) {
			String[] idCountSplit = entry.getValue().getAsString().split(":");
			String entryId = idCountSplit[0].replace("-", ":");
			if (entry.getKey().equals("B1") || entry.getKey().equals("C1")) {
				eb.appendDescription("\n");
			}
			eb.appendDescription(higherDepth(getEmojiMap(), entryId, "") + " ");
			eb.addField(idToName(entryId), idCountSplit[1], true);
		}
		eb.setThumbnail(getItemThumbnail(id));
		return eb;
	}

	@Override
	protected void execute(CommandEvent event) {
		new CommandExecute(this, event) {
			@Override
			protected void execute() {
				logCommand();

				setArgs(2);
				if (args.length == 2) {
					embed(getRecipe(args[1]));
					return;
				}

				sendErrorEmbed();
			}
		}
			.queue();
	}
}
