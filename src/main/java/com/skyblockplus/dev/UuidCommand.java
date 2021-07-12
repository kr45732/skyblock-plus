package com.skyblockplus.dev;

import static com.skyblockplus.utils.Hypixel.usernameToUuid;
import static com.skyblockplus.utils.Utils.*;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.skyblockplus.utils.structs.UsernameUuidStruct;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;

public class UuidCommand extends Command {

	public UuidCommand() {
		this.name = "d-uuid";
		this.ownerCommand = true;
	}

	@Override
	protected void execute(CommandEvent event) {
		executor.submit(
			() -> {
				EmbedBuilder eb = loadingEmbed();
				Message ebMessage = event.getChannel().sendMessageEmbeds(eb.build()).complete();
				String content = event.getMessage().getContentRaw();
				String[] args = content.split(" ");

				logCommand(event.getGuild(), event.getAuthor(), content);

				if (args.length == 2) {
					ebMessage.editMessageEmbeds(getUuidPlayer(args[1]).build()).queue();
					return;
				}

				ebMessage.editMessageEmbeds(errorEmbed(this.name).build()).queue();
			}
		);
	}

	private EmbedBuilder getUuidPlayer(String username) {
		UsernameUuidStruct usernameUuid = usernameToUuid(username);
		if (usernameUuid.isNotValid()) {
			return invalidEmbed(usernameUuid.failCause);
		}
		return defaultEmbed("Uuid for " + usernameUuid.playerUsername).setDescription("**Uuid:** " + usernameUuid.playerUuid);
	}
}
