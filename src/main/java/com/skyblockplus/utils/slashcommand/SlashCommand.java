/*
 * Skyblock Plus - A Skyblock focused Discord bot with many commands and customizable features to improve the experience of Skyblock players and guild staff!
 * Copyright (c) 2021 kr45732
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

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
