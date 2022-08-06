/*
 * Skyblock Plus - A Skyblock focused Discord bot with many commands and customizable features to improve the experience of Skyblock players and guild staff!
 * Copyright (c) 2021-2022 kr45732
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

import static com.skyblockplus.utils.Utils.*;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.skyblockplus.utils.command.CommandExecute;
import java.util.Comparator;
import java.util.stream.Collectors;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import org.springframework.stereotype.Component;

@Component
public class InformationCommand extends Command {

	public InformationCommand() {
		this.name = "information";
		this.cooldown = globalCooldown;
		this.aliases = new String[] { "info", "about", "invite" };
		this.botPermissions = defaultPerms();
	}

	public static MessageBuilder getInformation() {
		return new MessageBuilder()
			.setEmbeds(
				defaultEmbed("Skyblock Plus")
					.setDescription(
						"Skyblock Plus is a Skyblock focused Discord bot that has many commands to help Skyblock players and guild staff! It allows for quick retrieval of Skyblock stats plus customizable features for a better Skyblock experience."
					)
					.addField(
						"Statistics",
						"**Servers:** " +
						formatNumber(jda.getGuildCache().size()) +
						"\n**Users:** " +
						formatNumber(getUserCount()) +
						"\n**Avg Ping:** " +
								formatNumber(
										(long) jda.getShardCache().stream().map(s -> s.getRestPing().complete()).mapToLong(i -> i).average().orElse(0.0)
						) +
						"ms\n**Avg Websocket:** " +
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
							.map(s -> "**Shard " + (s.getShardInfo().getShardId() + 1) + ":** " + s.getGuildCache().size() + " servers")
							.collect(Collectors.joining("\n")),
						true
					)
					.addField(
						"Usage",
						"**Memory:** " +
						roundAndFormat(
							100.0 *
							(Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) /
							(Runtime.getRuntime().maxMemory())
						) +
						"%",
						true
					)
					.setThumbnail("https://cdn.discordapp.com/attachments/803419567958392832/825768516636508160/sb_loading.gif")
					.setFooter("dsc.gg/sb+ • Last restart")
					.setTimestamp(client.getStartTime())
					.build()
			)
			.setActionRows(
				ActionRow.of(
					Button.link(BOT_INVITE_LINK, "Invite Link"),
					Button.link(DISCORD_SERVER_INVITE_LINK, "Discord Server"),
					Button.link(FORUM_POST_LINK, "Forum Post"),
					Button.link("https://www.patreon.com/skyblock_plus", "Patreon")
				),
					ActionRow.of(
							Button.link("https://skyblock-plus.vercel.app", "Website Link"),
							Button.link("https://skyblock-plus.vercel.app/tos", "Terms of Service"),
							Button.link("https://skyblock-plus.vercel.app/privacy-policy", "Privacy Policy")
					)
			);
	}

	@Override
	protected void execute(CommandEvent event) {
		new CommandExecute(this, event) {
			@Override
			protected void execute() {
				logCommand();

				embed(getInformation());
			}
		}
			.queue();
	}
}
