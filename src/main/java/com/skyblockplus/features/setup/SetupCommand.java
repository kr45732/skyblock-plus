package com.skyblockplus.features.setup;

import static com.skyblockplus.features.listeners.AutomaticGuild.getGuildPrefix;
import static com.skyblockplus.utils.Utils.*;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.interactions.components.Button;

public class SetupCommand extends Command {

	public SetupCommand() {
		this.name = "setup";
		this.cooldown = globalCooldown;
		this.userPermissions = new Permission[] { Permission.ADMINISTRATOR };
	}

	@Override
	protected void execute(CommandEvent event) {
		executor.submit(
			() -> {
				logCommand(event.getGuild(), event.getAuthor(), getGuildPrefix(event.getGuild().getId()) + "setup");

				event
					.getChannel()
					.sendMessageEmbeds(
						defaultEmbed("Setup").setDescription("Choose one of the buttons below to setup the corresponding feature").build()
					)
					.setActionRow(
						Button.primary("setup_command_verify", "Verification"),
						Button.primary("setup_command_apply", "Application"),
						Button.primary("setup_command_guild", "Guild Roles & Ranks"),
						Button.primary("setup_command_roles", "Automatic Roles"),
						Button.primary("setup_command_prefix", "Prefix")
					)
					.queue();
			}
		);
	}
}
