package com.skyblockplus.utils.slashcommand;

import static com.skyblockplus.Main.database;
import static com.skyblockplus.utils.Utils.*;

import com.google.gson.JsonElement;
import com.skyblockplus.utils.Utils;
import java.util.regex.Matcher;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;

public class SlashCommandExecutedEvent {

	private final SlashCommandEvent event;
	private final InteractionHook hook;
	private final SlashCommandClient slashCommandClient;
	public String player;

	public SlashCommandExecutedEvent(SlashCommandEvent event, SlashCommandClient slashCommandClient) {
		this.event = event;
		this.slashCommandClient = slashCommandClient;
		this.hook = event.getHook();
	}

	public SlashCommandClient getSlashCommandClient() {
		return slashCommandClient;
	}

	public InteractionHook getHook() {
		return hook;
	}

	public SlashCommandEvent getEvent() {
		return event;
	}

	public void logCommand() {
		StringBuilder options = new StringBuilder();
		for (OptionMapping option : event.getOptions()) {
			options.append(" ").append(option.getAsString());
		}
		Utils.logCommand(event.getGuild(), event.getUser(), ("/" + event.getCommandPath().replace("/", " ") + options));
	}

	public User getUser() {
		return event.getUser();
	}

	public Member getMember() {
		return event.getMember();
	}

	public Guild getGuild() {
		return event.getGuild();
	}

	public MessageChannel getChannel() {
		return event.getChannel();
	}

	public String getOptionStr(String name) {
		OptionMapping option = event.getOption(name);
		return option == null ? null : option.getAsString();
	}

	public EmbedBuilder disabledCommandMessage() {
		return invalidEmbed("❌ This command is disabled");
	}

	public EmbedBuilder invalidCommandMessage() {
		return invalidEmbed("❌ Invalid Command");
	}

	public String getSubcommandName() {
		return event.getSubcommandName();
	}

	public void embed(EmbedBuilder eb) {
		event.getHook().editOriginalEmbeds(eb.build()).queue();
	}

	private boolean getLinkedUser(String id) {
		JsonElement linkedUserUsername = higherDepth(database.getLinkedUserByDiscordId(id), "minecraftUsername");
		if (linkedUserUsername != null) {
			player = linkedUserUsername.getAsString();
			return false;
		}

		embed(invalidEmbed("<@" + id + "> is not linked to the bot."));
		return true;
	}

	public boolean invalidPlayerOption() {
		OptionMapping option = event.getOption("player");

		if (option == null) {
			return getLinkedUser(event.getUser().getId());
		}

		player = option.getAsString();

		Matcher matcher = Message.MentionType.USER.getPattern().matcher(option.getAsString());
		if (matcher.matches()) {
			return getLinkedUser(matcher.group(1));
		}

		return false;
	}

	public void paginate(EmbedBuilder failEmbed) {
		if (failEmbed != null) {
			event.getHook().editOriginalEmbeds(failEmbed.build()).queue();
		}
	}

	public void string(String string) {
		event.getHook().editOriginal(string).queue();
	}
}
