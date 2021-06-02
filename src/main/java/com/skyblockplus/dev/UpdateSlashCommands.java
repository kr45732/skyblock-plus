package com.skyblockplus.dev;

import static com.skyblockplus.Main.jda;
import static com.skyblockplus.utils.Utils.*;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.requests.restaction.CommandListUpdateAction;

public class UpdateSlashCommands extends Command {

	public UpdateSlashCommands() {
		this.name = "d-slash";
		this.ownerCommand = true;
	}

	@Override
	protected void execute(CommandEvent event) {
		new Thread(
			() -> {
				String content = event.getMessage().getContentRaw();

				logCommand(event.getGuild(), event.getAuthor(), content);

				CommandListUpdateAction slashCommands = jda.getGuildById("796790757947867156").updateCommands();

				slashCommands.addCommands(
					new CommandData("information", "Get information about this bot"),
					new CommandData("invite", "Invite this bot to your server"),
					new CommandData("link", "Get what Hypixel account you are linked to")
						.addOption(OptionType.STRING, "player", "Link your Hypixel account to this bot"),
					new CommandData("unlink", "Unlink your account from this bot"),
					new CommandData("slayer", "Get the slayer data of a player")
						.addOption(OptionType.STRING, "player", "Player username", true)
						.addOption(OptionType.STRING, "profile", "Profile name"),
					new CommandData("skills", "Get the skills data of a player")
						.addOption(OptionType.STRING, "player", "Player username", true)
						.addOption(OptionType.STRING, "profile", "Profile name")
				);

				int size = slashCommands.complete().size();
				event.getChannel().sendMessage(defaultEmbed("Success - total of " + size + " commands").build()).queue();
			}
		)
			.start();
	}
}
