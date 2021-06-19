package com.skyblockplus.dev;

import static com.skyblockplus.utils.Utils.*;

import com.google.gson.JsonObject;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import net.dv8tion.jda.api.entities.Emote;

public class EmojiMapServerCommand extends Command {

	public EmojiMapServerCommand() {
		this.name = "d-emojis";
		this.ownerCommand = true;
		this.aliases = new String[] { "d-emoji" };
	}

	@Override
	protected void execute(CommandEvent event) {
		new Thread(
			() -> {
				logCommand(event.getGuild(), event.getAuthor(), BOT_PREFIX + "d-emojis");

				JsonObject toAdd = new JsonObject();
				for (Emote emote : event.getGuild().getEmotes()) {
					toAdd.addProperty(emote.getName().toUpperCase(), emote.getAsMention());
				}
				event.reply(makeHastePost(toAdd.toString()));
			}
		)
			.start();
	}
}
