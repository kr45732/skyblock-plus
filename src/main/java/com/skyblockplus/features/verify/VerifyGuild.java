package com.skyblockplus.features.verify;

import static com.skyblockplus.Main.jda;
import static com.skyblockplus.features.listeners.AutomaticGuild.getGuildPrefix;

import java.util.concurrent.TimeUnit;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;

public class VerifyGuild {

	public TextChannel messageChannel;
	public Message originalMessage;
	public boolean enable = true;

	public VerifyGuild(TextChannel messageChannel, Message originalMessage) {
		this.messageChannel = messageChannel;
		this.originalMessage = originalMessage;
	}

	public VerifyGuild() {
		this.enable = false;
	}

	public boolean onGuildMessageReceived(GuildMessageReceivedEvent event) {
		if (!enable) {
			return false;
		}

		if (!event.getChannel().getId().equals(messageChannel.getId())) {
			return false;
		}

		if (event.getMessage().getId().equals(originalMessage.getId())) {
			return false;
		}

		if (!event.getAuthor().getId().equals(jda.getSelfUser().getId())) {
			if (event.getAuthor().isBot()) {
				return false;
			}

			if (!event.getMessage().getContentRaw().startsWith(getGuildPrefix(event.getGuild().getId()) + "link ")) {
				event.getMessage().delete().queue();
				return true;
			}
		}

		event.getMessage().delete().queueAfter(7, TimeUnit.SECONDS);
		return true;
	}
}
