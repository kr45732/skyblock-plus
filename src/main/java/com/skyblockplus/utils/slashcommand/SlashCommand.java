package com.skyblockplus.utils.slashcommand;

import static com.skyblockplus.utils.Utils.globalCooldown;

public abstract class SlashCommand {

	protected final int cooldown = globalCooldown;
	protected final CooldownScope cooldownScope = CooldownScope.USER;
	protected String name = "null";

	protected abstract void execute(SlashCommandExecutedEvent event);

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
		USER("U:%d", ""),
		GLOBAL("Global", "globally");

		final String errorSpecification;
		private final String format;

		CooldownScope(String format, String errorSpecification) {
			this.format = format;
			this.errorSpecification = errorSpecification;
		}

		String genKey(String name, long id) {
			return genKey(name, id, -1);
		}

		String genKey(String name, long idOne, long idTwo) {
			if (this.equals(GLOBAL)) return name + "|" + format; else if (idTwo == -1) return (
				name + "|" + String.format(format, idOne)
			); else return name + "|" + String.format(format, idOne, idTwo);
		}
	}
}
