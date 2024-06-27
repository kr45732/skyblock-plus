/*
 * Skyblock Plus - A Skyblock focused Discord bot with many commands and customizable features to improve the experience of Skyblock players and guild staff!
 * Copyright (c) 2021-2024 kr45732
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

import static com.skyblockplus.features.listeners.MainListener.guildMap;
import static com.skyblockplus.utils.Constants.mayorNameToEmoji;
import static com.skyblockplus.utils.utils.StringUtils.getRelativeTimestamp;
import static com.skyblockplus.utils.utils.Utils.PRIMARY_GUILD_ID;
import static com.skyblockplus.utils.utils.Utils.defaultEmbed;

import com.skyblockplus.features.listeners.AutomaticGuild;
import com.skyblockplus.utils.command.SlashCommand;
import com.skyblockplus.utils.command.SlashCommandEvent;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.utils.messages.MessageEditBuilder;
import org.springframework.stereotype.Component;

@Component
public class MayorSlashCommand extends SlashCommand {

	public MayorSlashCommand() {
		this.name = "mayor";
	}

	public static MessageEditBuilder getMayor() {
		AutomaticGuild automaticGuild = guildMap.get(PRIMARY_GUILD_ID);

		List<Button> buttons = new ArrayList<>(automaticGuild.lastMayorElectedMessage.getButtons());
		if (automaticGuild.lastMayorElectionOpenMessage != null) {
			buttons.add(Button.primary("mayor_current_election_button", "Current Election"));
		}
		buttons.add(Button.primary("mayor_special_button", "Special Mayors"));

		EmbedBuilder eb = new EmbedBuilder();
		eb.copyFrom(automaticGuild.lastMayorElectedMessage.getEmbeds().get(0));
		try {
			for (int i = eb.getFields().size() - 1; i >= 0; i--) {
				MessageEmbed.Field field = eb.getFields().get(i);
				if (
					field.getName().equals("Next Election") &&
					(Instant.now().getEpochSecond() > Long.parseLong(field.getValue().split("Opens <t:")[1].split(":R>")[0]))
				) {
					eb.getFields().remove(i);
				}
			}
		} catch (Exception ignored) {}
		return new MessageEditBuilder().setEmbeds(eb.build()).setActionRow(buttons);
	}

	public static EmbedBuilder getSpecialMayors() {
		long newYearToElectionOpen = 217200000;
		long newYearToElectionClose = 105600000;
		int year = CalendarSlashCommand.getSkyblockYear();
		int nextSpecial = year % 8 == 0 ? year : ((year + 8) - (year % 8));

		String[] mayorNames = new String[] { "Scorpius", "Derpy", "Jerry" };
		EmbedBuilder eb = defaultEmbed("Next Special Mayors");
		for (int i = nextSpecial; i < nextSpecial + 24; i += 8) {
			int mayorIndex = 0;
			if ((i - 8) % 24 == 0) {
				mayorIndex = 1;
			} else if ((i - 16) % 24 == 0) {
				mayorIndex = 2;
			}
			eb.addField(
				mayorNameToEmoji.get(mayorNames[mayorIndex].toUpperCase()) + " " + mayorNames[mayorIndex],
				"Election Opens: " +
				getRelativeTimestamp((CalendarSlashCommand.YEAR_0 + CalendarSlashCommand.YEAR_MS * (i - 1)) + newYearToElectionOpen) +
				"\nElection Closes: " +
				getRelativeTimestamp((CalendarSlashCommand.YEAR_0 + CalendarSlashCommand.YEAR_MS * (i)) + newYearToElectionClose),
				false
			);
		}
		return eb;
	}

	@Override
	protected void execute(SlashCommandEvent event) {
		event.embed(getMayor());
	}

	@Override
	public SlashCommandData getCommandData() {
		return Commands.slash(name, "Get the current mayor and their perks");
	}
}
