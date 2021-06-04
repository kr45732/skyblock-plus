package com.skyblockplus.dev;

import static com.skyblockplus.Main.database;
import static com.skyblockplus.Main.jda;
import static com.skyblockplus.utils.Utils.*;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.skyblockplus.api.discordserversettings.settingsmanagers.ServerSettingsModel;
import java.util.List;
import net.dv8tion.jda.api.entities.Guild;

public class GetSettingsFile extends Command {

	public GetSettingsFile() {
		this.name = "d-getsettings";
		this.ownerCommand = true;
	}

	@Override
	protected void execute(CommandEvent event) {
		new Thread(
			() -> {
				String content = event.getMessage().getContentRaw();
				String[] args = content.split(" ");

				logCommand(event.getGuild(), event.getAuthor(), content);

				if (args.length == 2) {
					if (args[1].equals("current")) {
						if (getCurrentServerSettings(event.getGuild().getId(), event)) {
							return;
						}
					} else if (args[1].equals("all")) {
						if (getAllServerSettings(event)) {
							return;
						}
					} else {
						if (getCurrentServerSettings(args[1], event)) {
							return;
						}
					}
				}

				event.getChannel().sendMessage(errorMessage(this.name).build()).queue();
			}
		)
			.start();
	}

	private boolean getAllServerSettings(CommandEvent event) {
		List<ServerSettingsModel> allSettings = database.getAllServerSettings();
		if (allSettings == null) {
			return false;
		}

		try {
			event
				.getChannel()
				.sendMessage(makeHastePost(new GsonBuilder().setPrettyPrinting().create().toJson(allSettings)) + ".json")
				.queue();
			return true;
		} catch (Exception ignored) {}
		return false;
	}

	private boolean getCurrentServerSettings(String guildId, CommandEvent event) {
		Guild guild = jda.getGuildById(guildId);
		JsonElement currentSettings = database.getServerSettings(guildId);
		if (currentSettings == null || guild == null) {
			return false;
		}

		try {
			event
				.getChannel()
				.sendMessage(makeHastePost(new GsonBuilder().setPrettyPrinting().create().toJson(currentSettings)) + ".json")
				.queue();
			return true;
		} catch (Exception ignored) {}

		return false;
	}
}
