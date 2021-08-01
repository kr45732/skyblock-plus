package com.skyblockplus.utils.command;

import static com.skyblockplus.Main.database;
import static com.skyblockplus.utils.Utils.*;

import com.google.gson.JsonElement;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import java.util.regex.Matcher;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;

public abstract class CommandBase extends Command {

	/* Config */
	public boolean sendLoadingEmbed = true;

	/* Execution */
	private CommandEvent event;
	public Message ebMessage;
	public String[] args;
	public String linkedUser;

	@Override
	protected void execute(CommandEvent event) {
		executor.submit(
			() -> {
				this.event = event;
				if (this.sendLoadingEmbed) {
					this.ebMessage = event.getChannel().sendMessageEmbeds(loadingEmbed().build()).complete();
				}
				this.args = event.getMessage().getContentRaw().split(" ", 0);
				onExecute(event);
			}
		);
	}

	protected abstract void onExecute(CommandEvent event);

	public void logCommand() {
		com.skyblockplus.utils.Utils.logCommand(event.getGuild(), event.getAuthor(), event.getMessage().getContentRaw());
	}

	public void setArgs(int limit) {
		args = event.getMessage().getContentRaw().split(" ", limit);
	}

	public void paginate(EmbedBuilder embedBuilder) {
		if (embedBuilder == null) {
			ebMessage.delete().queue();
		} else {
			ebMessage.editMessageEmbeds(embedBuilder.build()).queue();
		}
	}

	public void sendNotLinkedEmbed() {
		sendNotLinkedEmbed(event.getAuthor().getId());
	}

	public void sendNotLinkedEmbed(String id) {
		ebMessage.editMessageEmbeds(invalidEmbed("<@" + id + "> is not linked to the bot.").build()).queue();
	}

	public void sendErrorEmbed() {
		ebMessage.editMessageEmbeds(errorEmbed(this.name).build()).queue();
	}

	public boolean getLinkedUser() {
		return getLinkedUser(event.getAuthor().getId());
	}

	private boolean getLinkedUser(String userId) {
		JsonElement linkedUserUsername = higherDepth(database.getLinkedUserByDiscordId(userId), "minecraftUsername");
		if (linkedUserUsername != null) {
			linkedUser = linkedUserUsername.getAsString();
			return true;
		}

		return false;
	}

	public LinkedStatus getMentionedUserId(int index) {
		Matcher matcher = Message.MentionType.USER.getPattern().matcher(args[index]);
		if (!matcher.matches()) {
			return LinkedStatus.NO_MENTION;
		}

		return getLinkedUser(matcher.group(1)) ? LinkedStatus.LINKED : LinkedStatus.NOT_LINKED.setId(matcher.group(1));
	}
}
