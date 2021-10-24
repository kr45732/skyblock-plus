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

package com.skyblockplus.skills;

import static com.skyblockplus.utils.Constants.HOTM_PERK_ID_TO_NAME;
import static com.skyblockplus.utils.Constants.HOTM_PERK_MAX_LEVEL;
import static com.skyblockplus.utils.Utils.*;

import com.google.gson.JsonElement;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.skyblockplus.utils.Player;
import com.skyblockplus.utils.command.CommandExecute;
import com.skyblockplus.utils.structs.SkillsStruct;
import java.util.Map.Entry;
import net.dv8tion.jda.api.EmbedBuilder;

public class HotmCommand extends Command {

	public HotmCommand() {
		this.name = "hotm";
		this.cooldown = globalCooldown;
		this.botPermissions = defaultPerms();
	}

	public static EmbedBuilder getHotm(String username, String profileName) {
		Player player = profileName == null ? new Player(username) : new Player(username, profileName);
		if (player.isValid()) {
			SkillsStruct skillInfo = player.getHOTM();
			if (skillInfo == null) {
				return invalidEmbed("Player has not unlocked heart of the mountain");
			}
			EmbedBuilder eb = player.defaultPlayerEmbed();
			JsonElement miningJson = higherDepth(player.profileJson(), "mining_core");

			eb.addField(
				"Statistics",
				"• **HOTM level:** " +
				skillInfo.getCurrentLevel() +
				" (**Progress:** " +
				roundProgress(skillInfo.getProgressToNext()) +
				")\n• **Tokens:** " +
				higherDepth(miningJson, "tokens", 0) +
				" (**Spent:** " +
				higherDepth(miningJson, "tokens_spent", 0) +
				")\n• **Mithril Powder:** " +
				formatNumber(higherDepth(miningJson, "powder_mithril", 0)) +
				" (**Spent:** " +
				formatNumber(higherDepth(miningJson, "powder_spent_mithril", 0)) +
				")\n• **Gemstone Powder:** " +
				formatNumber(higherDepth(miningJson, "powder_gemstone", 0)) +
				" (**Spent:** " +
				formatNumber(higherDepth(miningJson, "powder_spent_gemstone", 0)) +
				")\n• **Selected ability:** " +
				capitalizeString(higherDepth(miningJson, "selected_pickaxe_ability", "none").replace("_", " ")),
				false
			);

			StringBuilder perksStr = new StringBuilder();
			for (Entry<String, JsonElement> perk : higherDepth(miningJson, "nodes").getAsJsonObject().entrySet()) {
				if (!perk.getValue().getAsJsonPrimitive().isNumber()) {
					continue;
				}
				perksStr
					.append("• **")
					.append(capitalizeString(HOTM_PERK_ID_TO_NAME.getOrDefault(perk.getKey(), perk.getKey().replace("_", " "))))
					.append(":** ")
					.append(perk.getValue().getAsInt())
					.append("/")
					.append(HOTM_PERK_MAX_LEVEL.getOrDefault(perk.getKey(), 50))
					.append("\n");
			}
			eb.addField("Perks", perksStr.toString(), false);
			return eb;
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

					embed(getHotm(username, args.length == 3 ? args[2] : null));
					return;
				}

				sendErrorEmbed();
			}
		}
			.submit();
	}
}
