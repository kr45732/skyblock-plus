package com.skyblockplus.utils.slashcommands;

import static com.skyblockplus.utils.Utils.defaultEmbed;
import static com.skyblockplus.utils.Utils.logCommand;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;

public class SlashCommandExecutedEvent {

	private final SlashCommandEvent event;
	private final InteractionHook hook;
	private final SlashCommandClient slashCommandClient;

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

	public void logCommandGuildUserCommand() {
		logCommand(event.getGuild(), event.getUser(), "/" + event.getCommandPath().replace("/", " "));
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

	public String getOptionStr(String name) {
		OptionMapping option = event.getOption(name);
		return option == null ? null : option.getAsString();
	}

	public String getOptionStrNotNull(String name) {
		OptionMapping option = event.getOption(name);
		return option == null ? "" : option.getAsString();
	}

	public EmbedBuilder disabledCommandMessage() {
		return defaultEmbed("Error").setDescription("❌ This command is disabled");
	}

	public EmbedBuilder invalidCommandMessage() {
		return defaultEmbed("Error").setDescription("❌ Invalid Command");
	}

	public String getSubcommandName() {
		return event.getSubcommandName();
	}
}
