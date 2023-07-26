/*
 * Skyblock Plus - A Skyblock focused Discord bot with many commands and customizable features to improve the experience of Skyblock players and guild staff!
 * Copyright (c) 2021-2023 kr45732
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

package com.skyblockplus.price;

import static com.skyblockplus.utils.ApiHandler.queryLowestBin;
import static com.skyblockplus.utils.ApiHandler.uuidToUsername;
import static com.skyblockplus.utils.utils.JsonUtils.*;
import static com.skyblockplus.utils.utils.StringUtils.*;
import static com.skyblockplus.utils.utils.Utils.*;

import com.google.gson.JsonObject;
import com.skyblockplus.utils.command.SlashCommand;
import com.skyblockplus.utils.command.SlashCommandEvent;
import com.skyblockplus.utils.rendering.LoreRenderer;
import com.skyblockplus.utils.structs.AutoCompleteEvent;
import com.skyblockplus.utils.utils.StringUtils;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Collectors;
import javax.imageio.ImageIO;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.utils.FileUpload;
import net.dv8tion.jda.api.utils.messages.MessageEditBuilder;
import org.springframework.stereotype.Component;

@Component
public class BinSlashCommand extends SlashCommand {

	public BinSlashCommand() {
		this.name = "bin";
	}

	public static Object getLowestBin(String item) {
		String itemId = nameToId(item, true);
		if (itemId == null) {
			itemId = getClosestMatchFromIds(item, getInternalJsonMappings().keySet());
		}

		JsonObject lowestBinJson = queryLowestBin(itemId);
		if (lowestBinJson == null) {
			return errorEmbed("No bins found for " + idToName(item));
		}

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try {
			BufferedImage loreRender = LoreRenderer.renderLore(
				Arrays.stream(higherDepth(lowestBinJson, "lore").getAsString().split("\n")).toList()
			);
			ImageIO.write(loreRender, "png", baos);
		} catch (Exception ignored) {}

		return new MessageEditBuilder()
			.setEmbeds(
				defaultEmbed(idToName(itemId))
					.setDescription(
						"**Price:** " +
						formatNumber(higherDepth(lowestBinJson, "starting_bid").getAsLong()) +
						"\n**Seller:** " +
						uuidToUsername(higherDepth(lowestBinJson, "auctioneer").getAsString()).username() +
						"\n**Ends:** " +
						getRelativeTimestamp(higherDepth(lowestBinJson, "end_t").getAsLong()) +
						"\n**Command:** `/viewauction " +
						higherDepth(lowestBinJson, "uuid").getAsString() +
						"`"
					)
					.setThumbnail(getItemThumbnail(itemId))
					.setImage("attachment://lore.png")
					.build()
			)
			.setFiles(FileUpload.fromData(baos.toByteArray(), "lore.png"));
	}

	@Override
	protected void execute(SlashCommandEvent event) {
		event.embed(getLowestBin(event.getOptionStr("item")));
	}

	@Override
	public SlashCommandData getCommandData() {
		return Commands.slash(name, "Get the lowest bin of an item").addOption(OptionType.STRING, "item", "Item name", true, true);
	}

	@Override
	public void onAutoComplete(AutoCompleteEvent event) {
		if (event.getFocusedOption().getName().equals("item")) {
			event.replyClosestMatch(
				event.getFocusedOption().getValue(),
				getInternalJsonMappings()
					.keySet()
					.stream()
					.map(StringUtils::idToName)
					.distinct()
					.collect(Collectors.toCollection(ArrayList::new))
			);
		}
	}
}
