package com.skyblockplus.utils.slashcommand;

import static com.skyblockplus.utils.Utils.executor;
import static com.skyblockplus.utils.Utils.globalCooldown;

public abstract class SlashCommand {

	protected final int cooldown = globalCooldown;
	protected final CooldownScope cooldownScope = CooldownScope.USER;
	protected String name = "null";

	protected abstract void execute(SlashCommandExecutedEvent event);

	protected void _execute(SlashCommandExecutedEvent event) {
		executor.submit(() -> execute(event));
	}

	public String getName() {
		return name;
	}

	public int getRemainingCooldown(SlashCommandExecutedEvent event) {
		String key = cooldownScope.genKey(name, event.getUser().getIdLong());
		int remaining = event.getSlashCommandClient().getRemainingCooldown(key);
		if (remaining > 0) {
			return remaining;
		} else {
			event.getSlashCommandClient().applyCooldown(key, cooldown);
		}

		return 0;
	}

	public void replyCooldown(SlashCommandExecutedEvent event, int remainingCooldown) {
		event.getHook().editOriginal("⚠️ That command is on cooldown for " + remainingCooldown + " more seconds").queue();
	}

	public enum CooldownScope {
		USER("U:%d");

		private final String format;

		CooldownScope(String format) {
			this.format = format;
		}

		String genKey(String name, long id) {
			return name + "|" + String.format(format, id);
		}
	}
}
