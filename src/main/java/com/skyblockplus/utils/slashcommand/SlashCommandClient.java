package com.skyblockplus.utils.slashcommand;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class SlashCommandClient extends ListenerAdapter {

	private final List<SlashCommand> slashCommands;
	private final HashMap<String, OffsetDateTime> cooldowns;
	private final OffsetDateTime startTime;

	public SlashCommandClient() {
		this.slashCommands = new ArrayList<>();
		this.cooldowns = new HashMap<>();
		this.startTime = OffsetDateTime.now();
	}

	public void addSlashCommands(SlashCommand... commands) {
		slashCommands.addAll(Arrays.asList(commands));
	}

	@Override
	public void onSlashCommand(SlashCommandEvent event) {
		if (event.getGuild() == null) {
			event.reply("❌ This command cannot be used in Direct messages").queue();
			return;
		}

		event.deferReply().complete();

		SlashCommandExecutedEvent slashCommandExecutedEvent = new SlashCommandExecutedEvent(event, this);
		for (SlashCommand command : slashCommands) {
			if (command.getName().equals(event.getName())) {
				int remainingCooldown = command.getRemainingCooldown(slashCommandExecutedEvent);
				if (remainingCooldown > 0) {
					command.replyCooldown(slashCommandExecutedEvent, remainingCooldown);
				} else {
					command._execute(slashCommandExecutedEvent);
				}

				return;
			}
		}

		slashCommandExecutedEvent.getHook().editOriginalEmbeds(slashCommandExecutedEvent.invalidCommandMessage().build()).queue();
	}

	public int getRemainingCooldown(String name) {
		if (cooldowns.containsKey(name)) {
			int time = (int) Math.ceil(OffsetDateTime.now().until(cooldowns.get(name), ChronoUnit.MILLIS) / 1000D);
			if (time <= 0) {
				cooldowns.remove(name);
				return 0;
			}
			return time;
		}
		return 0;
	}

	public void applyCooldown(String name, int seconds) {
		cooldowns.put(name, OffsetDateTime.now().plusSeconds(seconds));
	}

	public OffsetDateTime getStartTime() {
		return startTime;
	}
}
