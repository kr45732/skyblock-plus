package com.skyblockplus.utils.slashcommands;

import static com.skyblockplus.utils.Utils.logCommand;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;

public class SlashCommandExecutedEvent {

	private SlashCommandEvent event;
	private InteractionHook hook;
	private SlashCommandImpl slashCommandImpl;

	public SlashCommandExecutedEvent(SlashCommandEvent event, SlashCommandImpl slashCommandImpl) {
		this.event = event;
		this.hook = event.getHook();
		this.slashCommandImpl = slashCommandImpl;
	}

	public SlashCommandImpl getSlashCommandImpl() {
		return slashCommandImpl;
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
}
