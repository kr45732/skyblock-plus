package com.skyblockplus.miscellaneous;

import static com.skyblockplus.Main.jda;
import static com.skyblockplus.features.listeners.AutomaticGuild.getGuildPrefix;
import static com.skyblockplus.utils.Utils.*;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import java.time.OffsetDateTime;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.Button;

public class InformationCommand extends Command {

	public InformationCommand() {
		this.name = "information";
		this.cooldown = globalCooldown;
		this.aliases = new String[] { "info", "about" };
	}

	public static ActionRow getInformationActionRow() {
		return ActionRow.of(
			Button.link(BOT_INVITE_LINK_REQUIRED_NO_SLASH, "Normal Invite"),
			Button.link(BOT_INVITE_LINK_REQUIRED_SLASH, "Slash Commands Invite"),
			Button.link(DISCORD_SERVER_INVITE_LINK, "Discord Server"),
			Button.link(FORUM_POST_LINK, "Forum Post")
		);
	}

	public static EmbedBuilder getInformation(OffsetDateTime starTime) {
		EmbedBuilder eb = defaultEmbed("Skyblock Plus");

		eb.setDescription(
			"Skyblock Plus is a Skyblock focused Discord bot that has many commands to help Skyblock players and guild staff! It allows for quick retrieval of Skyblock stats plus customizable features for a better Skyblock experience."
		);
		eb.addField(
			"Statistics",
			"**Servers:** " +
			jda.getGuilds().size() +
			"\n**Ping:** " +
			jda.getRestPing().complete() +
			"ms\n**Websocket:** " +
			jda.getGatewayPing() +
			"ms",
			true
		);
		eb.addField(
			"Usage",
			"**Memory:** " +
			roundAndFormat(
				100.0 * (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / (Runtime.getRuntime().totalMemory())
			) +
			"%",
			true
		);

		eb.setThumbnail("https://cdn.discordapp.com/attachments/803419567958392832/825768516636508160/sb_loading.gif");

		eb.setFooter("Last restart").setTimestamp(starTime);

		return eb;
	}

	@Override
	protected void execute(CommandEvent event) {
		executor.submit(
			() -> {
				logCommand(event.getGuild(), event.getAuthor(), getGuildPrefix(event.getGuild().getId()) + "information");

				event
					.getChannel()
					.sendMessageEmbeds(getInformation(event.getClient().getStartTime()).build())
					.setActionRows(getInformationActionRow())
					.queue();
			}
		);
	}
}
