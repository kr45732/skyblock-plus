package com.skyblockplus.features.setup;

import static com.skyblockplus.utils.Utils.defaultEmbed;
import static com.skyblockplus.utils.Utils.globalCooldown;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.skyblockplus.utils.command.CommandExecute;
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
		new CommandExecute(this, event) {
			@Override
			protected void execute() {
				logCommand();

				ebMessage
					.editMessageEmbeds(
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
		}
			.submit();
	}
}
