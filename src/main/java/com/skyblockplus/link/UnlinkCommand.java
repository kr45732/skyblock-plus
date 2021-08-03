package com.skyblockplus.link;

import static com.skyblockplus.Main.database;
import static com.skyblockplus.utils.Utils.defaultEmbed;
import static com.skyblockplus.utils.Utils.globalCooldown;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.skyblockplus.utils.command.CommandExecute;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.User;

public class UnlinkCommand extends Command {

	public UnlinkCommand() {
		this.name = "unlink";
		this.cooldown = globalCooldown;
	}

	public static EmbedBuilder unlinkAccount(User user) {
		database.deleteLinkedUserByDiscordId(user.getId());
		return defaultEmbed("Success").setDescription("You were unlinked");
	}

	@Override
	protected void execute(CommandEvent event) {
		new CommandExecute(this, event) {
			@Override
			protected void execute() {
				logCommand();

				embed(unlinkAccount(event.getAuthor()));
			}
		}
			.submit();
	}
}
