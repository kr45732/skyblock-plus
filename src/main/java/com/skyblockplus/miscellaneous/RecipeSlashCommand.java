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
import com.skyblockplus.utils.Utils;
import com.skyblockplus.utils.command.SlashCommand;
import com.skyblockplus.utils.command.SlashCommandEvent;
import com.skyblockplus.utils.structs.AutoCompleteEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import org.springframework.stereotype.Component;

@Component
public class RecipeSlashCommand extends SlashCommand {

	public static List<String> allRecipeIds;

	public RecipeSlashCommand() {
		this.name = "recipe";
		allRecipeIds =
			getInternalJsonMappings()
				.entrySet()
				.stream()
				.filter(e -> higherDepth(e.getValue(), "recipe") != null)
				.map(Map.Entry::getKey)
				.collect(Collectors.toCollection(ArrayList::new));
	}

	@Override
	protected void execute(SlashCommandEvent event) {
		event.embed(getRecipe(event.getOptionStr("item")));
	}

	@Override
	public SlashCommandData getCommandData() {
		return Commands.slash(name, "Get the crafting recipe create an item").addOption(OptionType.STRING, "item", "Item name", true, true);
	}

	@Override
	public void onAutoComplete(AutoCompleteEvent event) {
		if (event.getFocusedOption().getName().equals("item")) {
			event.replyClosestMatch(
				event.getFocusedOption().getValue(),
				allRecipeIds.stream().map(Utils::idToName).collect(Collectors.toCollection(ArrayList::new))
			);
		}
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

		EmbedBuilder eb = defaultEmbed("Recipe create " + name);
		for (Map.Entry<String, JsonElement> entry : higherDepth(infoJson, "recipe").getAsJsonObject().entrySet()) {
			if (entry.getKey().equals("count")) {
				continue;
			}
			String[] idCountSplit = entry.getValue().getAsString().split(":");
			if (entry.getKey().equals("B1") || entry.getKey().equals("C1")) {
				eb.appendDescription("\n");
			}
			if (idCountSplit.length == 1) {
				eb.appendDescription(getEmojiOr("EMPTY", "❓"));
				eb.addBlankField(true);
			} else {
				String entryId = idCountSplit[0].replace("-", ":");
				eb.appendDescription(getEmojiOr(entryId, "❓"));
				eb.addField(idToName(entryId), idCountSplit[1], true);
			}
		}
		eb.setThumbnail(getItemThumbnail(id));
		return eb;
	}
}
