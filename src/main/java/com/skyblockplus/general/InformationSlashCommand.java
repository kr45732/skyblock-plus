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

package com.skyblockplus.general;

import static com.skyblockplus.utils.utils.StringUtils.formatNumber;
import static com.skyblockplus.utils.utils.StringUtils.roundAndFormat;
import static com.skyblockplus.utils.utils.Utils.*;

import com.skyblockplus.utils.command.SlashCommand;
import com.skyblockplus.utils.command.SlashCommandEvent;
import java.util.Comparator;
import java.util.stream.Collectors;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.utils.messages.MessageEditBuilder;
import org.springframework.stereotype.Component;

@Component
public class InformationSlashCommand extends SlashCommand {

	public InformationSlashCommand() {
		this.name = "information";
	}

	public static void getInformation(SlashCommandEvent event) {
		event
			.getJDA()
			.getRestPing()
			.queue(ping ->
				event
					.getHook()
					.editOriginal(
						new MessageEditBuilder()
							.setEmbeds(
								defaultEmbed("Skyblock Plus")
									.setDescription(
										"Skyblock Plus is a Skyblock focused Discord bot that has" +
										" many commands to help Skyblock players and guild" +
										" staff! It allows for quick retrieval of Skyblock" +
										" stats plus customizable features for a better" +
										" Skyblock experience."
									)
									.addField(
										"Statistics",
										"**Servers:** " +
										formatNumber(jda.getGuildCache().size()) +
										"\n**Users:** " +
										formatNumber(getUserCount()) +
										"\n**Ping:** " +
										formatNumber(ping) +
										"ms\n**Websocket:** " +
										formatNumber((long) jda.getAverageGatewayPing()) +
										"ms",
										true
									)
									.addField(
										"Shards",
										jda
											.getShardCache()
											.stream()
											.sorted(Comparator.comparingInt(s -> s.getShardInfo().getShardId()))
											.map(s ->
												"**Shard " +
												(s.getShardInfo().getShardId() + 1) +
												":** " +
												s.getGuildCache().size() +
												" servers"
											)
											.collect(Collectors.joining("\n")),
										true
									)
									.addField(
										"Usage",
										"**Memory:** " +
										roundAndFormat(
											(100.0 * (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory())) /
											(Runtime.getRuntime().maxMemory())
										) +
										"%",
										true
									)
									.setThumbnail(
										"https://cdn.discordapp.com/attachments/803419567958392832/825768516636508160/sb_loading.gif"
									)
									.setFooter("SB+ is open source • sbplus.codes/gh • Last restart")
									.setTimestamp(client.getStartTime())
									.build()
							)
							.setComponents(
								ActionRow.of(
									Button.link(BOT_INVITE_LINK, "Invite Link"),
									Button.link(DISCORD_SERVER_INVITE_LINK, "Discord Server"),
									Button.link(FORUM_POST_LINK, "Forum Post"),
									Button.link("https://www.patreon.com/skyblock_plus", "Patreon")
								),
								ActionRow.of(
									Button.link(WEBSITE_LINK, "Website"),
									Button.link(WEBSITE_LINK + "/tos", "Terms of Service"),
									Button.link(WEBSITE_LINK + "/privacy-policy", "Privacy Policy"),
									Button.link("https://stats.uptimerobot.com/z4On2FGnOo", "Uptime")
								)
							)
							.build()
					)
					.queue()
			);
	}

	@Override
	protected void execute(SlashCommandEvent event) {
		getInformation(event);
	}

	@Override
	public SlashCommandData getCommandData() {
		return Commands.slash(name, "Get information about this bot");
	}
}
