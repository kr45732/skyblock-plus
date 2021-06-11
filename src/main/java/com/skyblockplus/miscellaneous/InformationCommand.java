package com.skyblockplus.miscellaneous;

import static com.skyblockplus.Main.jda;
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
		this.aliases = new String[] { "info" };
	}

	public static ActionRow getInformationActionRow() {
		return ActionRow.of(
			Button.link("https://discord.com/api/oauth2/authorize?client_id=796791167366594592&permissions=403040368&scope=bot", "Invite"),
			Button.link(DISCORD_SERVER_INVITE_LINK, "Discord Link"),
			Button.link("https://hypixel.net/threads/3980092", "Forum Post")
		);
	}

	public static EmbedBuilder getInformation(OffsetDateTime starTime) {
		EmbedBuilder eb = defaultEmbed("Skyblock Plus");

		eb.setDescription(
			"Skyblock Plus is a Skyblock focused discord bot that has many commands to help Skyblock players and guild staff! It allows for quick retrieval of Skyblock stats plus customizable features for a better Skyblock experience."
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

		eb.setFooter("Last restart").setTimestamp(starTime);

		return eb;
	}

	@Override
	protected void execute(CommandEvent event) {
		new Thread(
			() -> {
				logCommand(event.getGuild(), event.getAuthor(), BOT_PREFIX + "information");

				event
					.getChannel()
					.sendMessage(getInformation(event.getClient().getStartTime()).build())
					.setActionRows(getInformationActionRow())
					.queue();
			}
		)
			.start();
	}
}
