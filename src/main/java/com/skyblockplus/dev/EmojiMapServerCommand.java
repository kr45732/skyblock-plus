package com.skyblockplus.dev;

import static com.skyblockplus.utils.Utils.makeHastePost;

import com.google.gson.JsonObject;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.skyblockplus.utils.command.CommandExecute;
import net.dv8tion.jda.api.entities.Emote;

public class EmojiMapServerCommand extends Command {

	public EmojiMapServerCommand() {
		this.name = "d-emojis";
		this.ownerCommand = true;
		this.aliases = new String[] { "d-emoji" };
	}

	@Override
	protected void execute(CommandEvent event) {
		new CommandExecute(this, event, false) {
			@Override
			protected void execute() {
				logCommand();

				JsonObject toAdd = new JsonObject();
				for (Emote emote : event.getGuild().getEmotes()) {
					toAdd.addProperty(emote.getName().toUpperCase(), emote.getAsMention());
				}
				event.reply(makeHastePost(toAdd.toString()));
			}
		}
			.submit();
	}
}
