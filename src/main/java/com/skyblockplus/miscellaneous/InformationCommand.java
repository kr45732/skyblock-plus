package com.skyblockplus.miscellaneous;

import static com.skyblockplus.Main.jda;
import static com.skyblockplus.utils.Utils.*;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import net.dv8tion.jda.api.EmbedBuilder;

public class InformationCommand extends Command {

	public InformationCommand() {
		this.name = "information";
		this.cooldown = globalCooldown;
		this.aliases = new String[] { "info" };
	}

	public static EmbedBuilder getInformation() {
		EmbedBuilder eb = defaultEmbed("Skyblock Plus");

		eb.setDescription(
			"Skyblock Plus is a Skyblock focused discord bot that has many commands to help Skyblock players and guild staff! It allows for quick retrieval of Skyblock stats plus customizable features for a better Skyblock experience."
		);
		eb.addField(
			"Stats",
			"**Servers:** " +
			jda.getGuilds().size() +
			"\n**Members:** " +
			jda.getUsers().size() +
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

		eb.addField(
			"Links",
			"[**Invite Link**](https://discord.com/api/oauth2/authorize?client_id=796791167366594592&permissions=403040368&scope=bot)\n[**Discord Link**](https://discord.gg/DpcCAwMXwp)\n[**Forum Post**](https://hypixel.net/threads/3980092)",
			true
		);
		eb.setFooter("Last restart");

		return eb;
	}

	@Override
	protected void execute(CommandEvent event) {
		new Thread(
			() -> {
				logCommand(event.getGuild(), event.getAuthor(), BOT_PREFIX + "information");

				event.getChannel().sendMessage(getInformation().setTimestamp(event.getClient().getStartTime()).build()).queue();
			}
		)
			.start();
	}
}
