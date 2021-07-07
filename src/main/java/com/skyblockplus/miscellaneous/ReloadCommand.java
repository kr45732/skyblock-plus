package com.skyblockplus.miscellaneous;

import static com.skyblockplus.eventlisteners.MainListener.*;
import static com.skyblockplus.utils.Utils.*;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;

public class ReloadCommand extends Command {

	public ReloadCommand() {
		this.name = "reload";
		this.cooldown = 45;
		this.userPermissions = new Permission[] { Permission.ADMINISTRATOR };
	}

	@Override
	protected void execute(CommandEvent event) {
		executor.submit(
			() -> {
				logCommand(event.getGuild(), event.getAuthor(), event.getMessage().getContentRaw());

				EmbedBuilder eb = loadingEmbed();
				Message ebMessage = event.getChannel().sendMessageEmbeds(eb.build()).complete();

				eb = defaultEmbed("Reload Settings for " + event.getGuild().getName());
				eb.addField("Apply settings reload status", onApplyReload(event.getGuild().getId()), false);
				eb.addField("Verify settings reload status", onVerifyReload(event.getGuild().getId()), false);
				eb.addField("Mee6 bypasser reload status", onMee6Reload(event.getGuild().getId()), false);
				ebMessage.editMessageEmbeds(eb.build()).queue();
			}
		);
	}
}
