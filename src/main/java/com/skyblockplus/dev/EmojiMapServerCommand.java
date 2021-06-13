package com.skyblockplus.dev;

import static com.skyblockplus.utils.Utils.BOT_PREFIX;
import static com.skyblockplus.utils.Utils.logCommand;

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

				StringBuilder ebString = new StringBuilder();
				for (Emote emote : event.getGuild().getEmotes()) {
					ebString
						.append("emojiMap.put(\"")
						.append(emote.getName())
						.append("\", \"\\")
						.append(emote.getAsMention())
						.append("\");")
						.append("\n");
				}
				event.reply(ebString.toString());
			}
		)
			.start();
	}
}