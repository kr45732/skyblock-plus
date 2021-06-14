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
			"[Normal Invite](" +
			BOT_INVITE_LINK_REQUIRED_NO_SLASH +
			")\n" +
			"[Slash Commands Invite](" +
			BOT_INVITE_LINK_REQUIRED_SLASH +
			")",
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
