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

package com.skyblockplus.dev;

import static com.skyblockplus.features.listeners.MainListener.guildMap;
import static com.skyblockplus.utils.Utils.*;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.skyblockplus.utils.command.CommandExecute;
import groovy.lang.GroovyShell;
import java.util.Arrays;
import net.dv8tion.jda.api.entities.ChannelType;
import org.springframework.stereotype.Component;

@Component
public class EvaluateCommand extends Command {

	private final StringBuilder importString = new StringBuilder();
	private boolean inSession = false;
	private GroovyShell shell = new GroovyShell();

	public EvaluateCommand() {
		this.name = "d-evaluate";
		this.ownerCommand = true;
		this.aliases = new String[] { "evaluate", "eval", "ev" };
		this.botPermissions = defaultPerms();

		// import [name].*
		String[] packageImports = {
			"java.io",
			"java.lang",
			"java.math",
			"java.time",
			"java.util",
			"java.util.concurrent",
			"java.util.stream",
			"net.dv8tion.jda.api",
			"net.dv8tion.jda.api.entities",
			"net.dv8tion.jda.api.entities.impl",
			"net.dv8tion.jda.api.managers",
			"net.dv8tion.jda.api.managers.impl",
			"net.dv8tion.jda.api.utils",
			"net.dv8tion.jda.api.interactions",
			"com.skyblockplus",
			"com.google.gson",
		};

		// import [name]
		String[] classImports = { "com.skyblockplus.utils.Player", "me.nullicorn.nedit.NBTReader" };

		// import static [name]
		String[] staticImports = {
			"com.skyblockplus.utils.Utils.*",
			"com.skyblockplus.Main.*",
			"com.skyblockplus.utils.ApiHandler.*",
			"com.skyblockplus.utils.Constants.*",
		};

		for (String packageImport : packageImports) {
			importString.append("import ").append(packageImport).append(".*\n");
		}

		for (String classImport : classImports) {
			importString.append("import ").append(classImport).append("\n");
		}

		for (String staticImport : staticImports) {
			importString.append("import static ").append(staticImport).append("\n");
		}
	}

	@Override
	protected void execute(CommandEvent event) {
		new CommandExecute(this, event, false) {
			@Override
			protected void execute() {
				setArgs(2, true);
				ebMessage = event.getChannel().sendMessage("Loading").complete();

				if (args.length < 2) {
					ebMessage.editMessage("Invalid Input").queue();
					return;
				}

				switch (args[1]) {
					case "start_session()" -> {
						inSession = true;
						shell = new GroovyShell();
						ebMessage.editMessage("Session started with " + shell).queue();
						return;
					}
					case "end_session()" -> {
						inSession = false;
						shell = new GroovyShell();
						ebMessage.editMessage("Session ended with " + shell).queue();
						return;
					}
					case "get_session()" -> {
						ebMessage.editMessage(inSession ? "Session running with " + shell : "No session running").queue();
						return;
					}
				}

				if (!inSession) {
					shell = new GroovyShell();
				}

				String arg = args[1].trim();
				if (arg.startsWith("```") && arg.endsWith("```")) {
					arg = arg.replaceAll("```(.*)\n", "").replaceAll("\n?```", "");
				}

				try {
					shell.setProperty("event", event.getEvent());
					shell.setProperty("cmdEvent", event);
					shell.setProperty("message", event.getMessage());
					shell.setProperty("channel", event.getChannel());
					shell.setProperty("jda", event.getJDA());
					shell.setProperty("guilds", guildMap);
					shell.setProperty("db", database);
					if (event.isFromType(ChannelType.TEXT)) {
						shell.setProperty("guild", event.getGuild());
						shell.setProperty("member", event.getMember());
					}

					String script = importString + arg;
					Object out = shell.evaluate(script);

					if (out == null) {
						ebMessage.editMessage("Success (null output)").queue();
					} else if (out.toString().length() >= 2000) {
						ebMessage.editMessage(makeHastePost(out.toString()) + ".json").queue();
					} else {
						ebMessage.editMessage(out.toString()).queue();
					}
				} catch (Exception e) {
					String msg = e.getMessage() != null ? e.getMessage() : Arrays.toString(e.getStackTrace());
					ebMessage.editMessage("" + (msg.length() >= 2000 ? makeHastePost(msg) : msg)).queue();
				}
			}
		}
			.queue();
	}
}
