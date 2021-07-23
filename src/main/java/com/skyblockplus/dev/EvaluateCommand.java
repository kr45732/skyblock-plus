package com.skyblockplus.dev;

import static com.skyblockplus.Main.database;
import static com.skyblockplus.features.listeners.MainListener.guildMap;
import static com.skyblockplus.utils.Utils.*;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import groovy.lang.GroovyShell;
import java.util.Arrays;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public class EvaluateCommand extends Command {

	private final StringBuilder importString = new StringBuilder();
	private boolean inSession = false;
	private GroovyShell shell = new GroovyShell();

	public EvaluateCommand() {
		this.name = "evaluate";
		this.ownerCommand = true;
		this.aliases = new String[] { "eval", "ev" };

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
			"com.skyblockplus",
			"com.google.gson",
			"com.skyblockplus.utils.structs",
		};

		// import [name]
		String[] classImports = { "com.skyblockplus.utils.Player", "me.nullicorn.nedit.NBTReader" };

		// import static [name]
		String[] staticImports = { "com.skyblockplus.utils.Utils.*", "com.skyblockplus.Main.*", "com.skyblockplus.utils.Hypixel.*" };

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
		executor.submit(
			() -> {
				Message ebMessage = event.getChannel().sendMessage("Loading").complete();
				String content = event.getMessage().getContentRaw();
				String[] args = content.split("\\s+", 2);

				logCommand(event.getGuild(), event.getAuthor(), content);

				if (args.length < 2) {
					ebMessage.editMessage("Invalid Input").queue();
					return;
				}

				switch (args[1]) {
					case "start_session()":
						inSession = true;
						shell = new GroovyShell();
						ebMessage.editMessage("Session started with " + shell).queue();
						return;
					case "end_session()":
						inSession = false;
						shell = new GroovyShell();
						ebMessage.editMessage("Session ended with " + shell).queue();
						return;
					case "get_session()":
						ebMessage.editMessage(inSession ? "Session running with " + shell : "No session running").queue();
						return;
				}

				if (!inSession) {
					shell = new GroovyShell();
				}

				String arg = args[1];

				if (arg.startsWith("```") && arg.endsWith("```")) {
					arg = arg.replaceAll("```(.*)\n", "").replaceAll("\n?```", "");
				}

				MessageReceivedEvent jdaEvent = event.getEvent();

				try {
					shell.setProperty("event", jdaEvent);
					shell.setProperty("message", jdaEvent.getMessage());
					shell.setProperty("channel", jdaEvent.getChannel());
					shell.setProperty("args", args);
					shell.setProperty("jda", jdaEvent.getJDA());
					shell.setProperty("guilds", guildMap);
					shell.setProperty("db", database);
					if (jdaEvent.isFromType(ChannelType.TEXT)) {
						shell.setProperty("guild", jdaEvent.getGuild());
						shell.setProperty("member", jdaEvent.getMember());
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
		);
	}
}
