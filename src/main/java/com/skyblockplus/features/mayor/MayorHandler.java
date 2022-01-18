/*
 * Skyblock Plus - A Skyblock focused Discord bot with many commands and customizable features to improve the experience of Skyblock players and guild staff!
 * Copyright (c) 2022 kr45732
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

package com.skyblockplus.features.mayor;

import static com.skyblockplus.Main.jda;
import static com.skyblockplus.features.listeners.MainListener.guildMap;
import static com.skyblockplus.utils.Utils.*;

import com.google.gson.JsonElement;
import com.skyblockplus.features.listeners.AutomaticGuild;
import java.util.concurrent.TimeUnit;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;

public class MayorHandler {

	public static void initialize() {
		scheduler.scheduleAtFixedRate(MayorHandler::updateMayor, 0, 30, TimeUnit.MINUTES);
	}

	private static void updateMayor() {
		try {
			JsonElement mayorJson = getJson("https://whoknew.sbe-stole-skytils.design/api/mayor");
			String curMayorName = higherDepth(mayorJson, "name", null);
			if (curMayorName == null) {
				return;
			}

			if (curMayorName.equals("Jerry")) {
				mayorJson = higherDepth(getJson("https://whoknew.sbe-stole-skytils.design/api/mayor/jerry"), "mayor");
				curMayorName = "Jerry | " + higherDepth(mayorJson, "name").getAsString();
			}

			if (
				curMayorName.equals(
					jda
						.getTextChannelById("932484216179011604")
						.getHistory()
						.retrievePast(1)
						.complete()
						.get(0)
						.getEmbeds()
						.get(0)
						.getTitle()
				)
			) {
				return;
			}

			EmbedBuilder eb = defaultEmbed(curMayorName);
			for (JsonElement perk : higherDepth(mayorJson, "perks").getAsJsonArray()) {
				eb.addField(higherDepth(perk, "name").getAsString(), higherDepth(perk, "description").getAsString(), false);
			}
			MessageEmbed embed = eb.build();

			for (AutomaticGuild guild : guildMap.values()) {
				guild.onMayor(embed);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
