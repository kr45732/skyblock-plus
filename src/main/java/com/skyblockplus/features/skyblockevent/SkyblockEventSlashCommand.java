package com.skyblockplus.features.skyblockevent;

import static com.skyblockplus.Main.database;
import static com.skyblockplus.utils.Utils.defaultEmbed;

import com.skyblockplus.utils.slashcommand.SlashCommand;
import com.skyblockplus.utils.slashcommand.SlashCommandExecutedEvent;
import net.dv8tion.jda.api.Permission;

public class SkyblockEventSlashCommand extends SlashCommand {

	public SkyblockEventSlashCommand() {
		this.name = "event";
	}

	@Override
	protected void execute(SlashCommandExecutedEvent event) {
		event.logCommand();

		String subcommandName = event.getSubcommandName();
		if (
			(subcommandName.equals("create") || subcommandName.equals("cancel") || subcommandName.equals("end")) &&
			!event.getMember().hasPermission(Permission.ADMINISTRATOR)
		) {
			event.string("❌ You must have the Administrator permission in this Guild to use that!");
			return;
		}

		switch (subcommandName) {
			case "create":
				event.paginate(SkyblockEventCommand.createSkyblockEvent(event.getChannel(), event.getUser(), event.getGuild()));
				return;
			case "current":
				event.embed(SkyblockEventCommand.getCurrentSkyblockEvent(event.getGuild().getId()));
				return;
			case "cancel":
				event.embed(SkyblockEventCommand.cancelSkyblockEvent(event.getGuild().getId()));
				return;
			case "join":
				event.embed(SkyblockEventCommand.joinSkyblockEvent(event.getGuild().getId(), event.getUser().getId(), new String[] {}));
				return;
			case "leave":
				event.embed(SkyblockEventCommand.leaveSkyblockEvent(event.getGuild().getId(), event.getUser().getId()));
				return;
			case "leaderboard":
			case "lb":
				event.paginate(SkyblockEventCommand.getEventLeaderboard(event.getGuild().getId(), event.getUser(), null, event.getHook()));
				return;
			case "end":
				if (database.getSkyblockEventActive(event.getGuild().getId())) {
					SkyblockEventCommand.endSkyblockEvent(event.getGuild().getId());
					event.embed(defaultEmbed("Success").setDescription("Event Ended"));
				} else {
					event.embed(defaultEmbed("No event running"));
				}
			default:
				event.embed(event.invalidCommandMessage());
		}
	}
}
