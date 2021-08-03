package com.skyblockplus.dev;

import static com.skyblockplus.utils.Hypixel.usernameToUuid;
import static com.skyblockplus.utils.Utils.defaultEmbed;
import static com.skyblockplus.utils.Utils.invalidEmbed;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.skyblockplus.utils.command.CommandExecute;
import com.skyblockplus.utils.structs.UsernameUuidStruct;
import net.dv8tion.jda.api.EmbedBuilder;

public class UuidCommand extends Command {

	public UuidCommand() {
		this.name = "d-uuid";
		this.ownerCommand = true;
	}

	@Override
	protected void execute(CommandEvent event) {
		new CommandExecute(this, event) {
			@Override
			protected void execute() {
				logCommand();

				if (args.length == 2) {
					embed(getUuidPlayer(args[1]));
					return;
				}

				sendErrorEmbed();
			}
		}
			.submit();
	}

	private EmbedBuilder getUuidPlayer(String username) {
		UsernameUuidStruct usernameUuid = usernameToUuid(username);
		if (usernameUuid.isNotValid()) {
			return invalidEmbed(usernameUuid.failCause);
		}
		return defaultEmbed("Uuid for " + usernameUuid.playerUsername).setDescription("**Uuid:** " + usernameUuid.playerUuid);
	}
}
