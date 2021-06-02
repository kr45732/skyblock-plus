package com.skyblockplus.utils.slashcommands;

import static com.skyblockplus.utils.Utils.*;

public abstract class SlashCommand {

	protected String name = "null";
	protected int cooldown = globalCooldown;
	protected CooldownScope cooldownScope = CooldownScope.USER;

	protected abstract void execute(SlashCommandExecutedEvent event);

	public String getName() {
		return name;
	}

	public int getRemainingCooldown(SlashCommandExecutedEvent event) {
		if (cooldown > 0) {
			String key = cooldownScope.genKey(name, event.getUser().getIdLong());
			int remaining = event.getSlashCommandImpl().getRemainingCooldown(key);
			if (remaining > 0) {
				return remaining;
			} else {
				event.getSlashCommandImpl().applyCooldown(key, cooldown);
			}
		}

		return 0;
	}

	public void replyCooldown(SlashCommandExecutedEvent event, int remainingCooldown) {
		event.getHook().editOriginal("⚠️ That command is on cooldown for " + remainingCooldown + " more seconds").queue();
	}

	public enum CooldownScope {
		USER("U:%d", ""),
		CHANNEL("C:%d", "in this channel"),
		USER_CHANNEL("U:%d|C:%d", "in this channel"),
		GUILD("G:%d", "in this server"),
		USER_GUILD("U:%d|G:%d", "in this server"),
		SHARD("S:%d", "on this shard"),
		USER_SHARD("U:%d|S:%d", "on this shard"),
		GLOBAL("Global", "globally");

		private final String format;
		final String errorSpecification;

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
