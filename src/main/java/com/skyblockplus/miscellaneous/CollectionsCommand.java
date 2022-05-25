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

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.skyblockplus.utils.Player;
import com.skyblockplus.utils.command.CommandExecute;
import com.skyblockplus.utils.command.PaginatorEvent;
import com.skyblockplus.utils.command.PaginatorExtras;
import java.util.Map;
import net.dv8tion.jda.api.EmbedBuilder;
import org.springframework.stereotype.Component;

@Component
public class CollectionsCommand extends Command {

	public CollectionsCommand() {
		this.name = "collections";
		this.cooldown = globalCooldown;
		this.botPermissions = defaultPerms();
	}

	public static EmbedBuilder getCollections(String username, String profileName, PaginatorEvent event) {
		Player player = profileName == null ? new Player(username) : new Player(username, profileName);
		if (player.isValid()) {
			EmbedBuilder eb = player.defaultPlayerEmbed();

			PaginatorExtras extras = new PaginatorExtras(PaginatorExtras.PaginatorType.EMBED_PAGES);
			int maxedCount = player.getNumMaxedCollections();
			int maxCountType = 0;
			int totalCountType = 0;
			String collectionType = null;
			for (Map.Entry<String, JsonElement> entry : getCollectionsJson().entrySet()) {
				String curCollectionType = higherDepth(entry.getValue(), "type").getAsString();
				if (collectionType == null) {
					collectionType = curCollectionType;
				}
				if (!curCollectionType.equals(collectionType)) {
					extras.addEmbedPage(
						eb.setDescription(
							"**Total Maxed Collections:** " +
							maxedCount +
							"/" +
							getCollectionsJson().size() +
							"\n**Maxed " +
							capitalizeString(collectionType) +
							" Collections:** " +
							maxCountType +
							"/" +
							totalCountType +
							"\n" +
							eb.getDescriptionBuilder()
						)
					);
					eb = player.defaultPlayerEmbed();
					collectionType = curCollectionType;
					maxCountType = totalCountType = 0;
				}

				JsonArray tiers = higherDepth(entry.getValue(), "tiers").getAsJsonArray();
				long amt = player.getCollection(entry.getKey());
				int level = 0;
				for (int i = 0; i < tiers.size(); i++) {
					if (amt >= tiers.get(i).getAsLong()) {
						level = i + 1;
					} else {
						break;
					}
				}
				if (level == tiers.size()) {
					maxCountType++;
				}

				eb.appendDescription(
					"\n" +
					getEmoji(entry.getKey().equals("MUSHROOM_COLLECTION") ? "RED_MUSHROOM" : entry.getKey()) +
					" " +
					idToName(entry.getKey()) +
					": " +
					level +
					"/" +
					tiers.size() +
					" (" +
					simplifyNumber(amt) +
					")"
				);
				totalCountType++;
			}

			event.paginate(defaultPaginator(event.getUser()).setPaginatorExtras(extras));
			return null;
		}
		return player.getFailEmbed();
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

					paginate(getCollections(player, args.length == 3 ? args[2] : null, getPaginatorEvent()));
					return;
				}

				sendErrorEmbed();
			}
		}
			.queue();
	}
}