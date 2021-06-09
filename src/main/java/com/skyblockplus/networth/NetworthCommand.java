package com.skyblockplus.networth;

import static com.skyblockplus.utils.Utils.globalCooldown;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;

public class NetworthCommand extends Command {

	public NetworthCommand() {
		this.name = "networth";
		this.cooldown = globalCooldown + 1;
		this.aliases = new String[] { "nw" };
	}

	@Override
	protected void execute(CommandEvent event) {
		new NetworthExecute().execute(event);
	}
}
