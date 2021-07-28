package com.skyblockplus.settings;

import static com.skyblockplus.utils.Utils.globalCooldown;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import net.dv8tion.jda.api.Permission;

public class SettingsCommand extends Command {

	public SettingsCommand() {
		this.name = "settings";
		this.cooldown = globalCooldown + 1;
		this.userPermissions = new Permission[] { Permission.ADMINISTRATOR };
	}

	@Override
	protected void execute(CommandEvent event) {
		new SettingsExecute(event).execute();
	}
}
