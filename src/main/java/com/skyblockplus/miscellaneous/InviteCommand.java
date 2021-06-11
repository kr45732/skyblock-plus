package com.skyblockplus.miscellaneous;

import static com.skyblockplus.utils.Utils.*;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import net.dv8tion.jda.api.EmbedBuilder;

public class InviteCommand extends Command {

	public InviteCommand() {
		this.name = "invite";
		this.cooldown = globalCooldown;
	}

	public static EmbedBuilder getInvite() {
		EmbedBuilder eb = defaultEmbed("Invite Skyblock Plus");
		eb.addField(
			"Invite me to your server",
			"[Click here](https://discord.com/api/oauth2/authorize?client_id=796791167366594592&permissions=403040368&scope=bot)",
			false
		);
		eb.addField("Join my server", "[Click here](" + DISCORD_SERVER_INVITE_LINK + ")", false);
		eb.setThumbnail("https://cdn.discordapp.com/attachments/803419567958392832/825768516636508160/sb_loading.gif");
		return eb;
	}

	@Override
	protected void execute(CommandEvent event) {
		new Thread(
			() -> {
				logCommand(event.getGuild(), event.getAuthor(), event.getMessage().getContentRaw());

				event.getChannel().sendMessage(getInvite().build()).queue();
			}
		)
			.start();
	}
}
