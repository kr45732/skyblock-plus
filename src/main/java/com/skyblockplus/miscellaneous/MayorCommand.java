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

import static com.skyblockplus.features.event.CalendarCommand.*;
import static com.skyblockplus.features.mayor.MayorHandler.mayorNameToEmoji;
import static com.skyblockplus.utils.Utils.*;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.skyblockplus.utils.command.CommandExecute;
import java.time.Instant;
import java.util.List;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import org.springframework.stereotype.Component;

@Component
public class MayorCommand extends Command {

	public MayorCommand() {
		this.name = "mayor";
		this.cooldown = globalCooldown;
		this.botPermissions = defaultPerms();
	}

	public static MessageBuilder getMayor() {
		Message message = jda.getTextChannelById("932484216179011604").getHistory().retrievePast(1).complete().get(0);
		List<Button> buttons = message.getButtons();
		buttons.add(Button.primary("mayor_special_button", "Special Mayors"));
		return new MessageBuilder().setEmbeds(message.getEmbeds()).setActionRows(ActionRow.of(buttons));
	}

	public static EmbedBuilder getSpecialMayors() {
		long newYearToElectionOpen = 217200000;
		long newYearToElectionClose = 105600000;
		int year = getSkyblockYear();
		int nextSpecial = year % 8 == 0 ? year : ((year + 8) - (year % 8));

		String[] mayorNames = new String[] { "Scorpius", "Derpy", "Jerry" };
		EmbedBuilder eb = defaultEmbed("Special Mayors");
		for (int i = nextSpecial; i < nextSpecial + 24; i += 8) {
			int mayorIndex = 0;
			if ((i - 8) % 24 == 0) {
				mayorIndex = 1;
			} else if ((i - 16) % 24 == 0) {
				mayorIndex = 2;
			}
			eb.addField(
				mayorNameToEmoji.get(mayorNames[mayorIndex].toUpperCase()) + " " + mayorNames[mayorIndex],
				"Election Opens: <t:" +
				Instant.ofEpochMilli((YEAR_0 + YEAR_MS * (i - 1)) + newYearToElectionOpen).getEpochSecond() +
				":R>\nElection Closes: <t:" +
				Instant.ofEpochMilli((YEAR_0 + YEAR_MS * (i)) + newYearToElectionClose).getEpochSecond() +
				":R>",
				false
			);
		}
		return eb;
	}

	@Override
	protected void execute(CommandEvent event) {
		new CommandExecute(this, event) {
			@Override
			protected void execute() {
				logCommand();

				embed(getMayor());
			}
		}
			.queue();
	}
}
