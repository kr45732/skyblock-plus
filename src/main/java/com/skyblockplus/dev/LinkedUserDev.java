package com.skyblockplus.dev;

import static com.skyblockplus.Main.database;
import static com.skyblockplus.utils.Utils.defaultEmbed;
import static com.skyblockplus.utils.Utils.makeHastePost;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.skyblockplus.utils.command.CommandExecute;

public class LinkedUserDev extends Command {

	public LinkedUserDev() {
		this.name = "d-linked";
		this.ownerCommand = true;
	}

	@Override
	protected void execute(CommandEvent event) {
		new CommandExecute(this, event) {
			@Override
			protected void execute() {
				logCommand();

				if (args.length == 4) {
					if (args[1].equals("delete")) {
						switch (args[2]) {
							case "discordId":
								database.deleteLinkedUserByDiscordId(args[3]);
								embed(defaultEmbed("Done"));
								return;
							case "username":
								database.deleteLinkedUserByMinecraftUsername(args[3]);
								embed(defaultEmbed("Done"));
								return;
							case "uuid":
								database.deleteLinkedUserByMinecraftUuid(args[3]);
								embed(defaultEmbed("Done"));
								return;
						}
					}
				} else if (args.length == 2) {
					if (args[1].equals("all")) {
						if (getAllLinkedUsers(event)) {
							ebMessage.delete().queue();
							return;
						}
					}
				}

				sendErrorEmbed();
			}
		}
			.submit();
	}

	private boolean getAllLinkedUsers(CommandEvent event) {
		JsonElement allSettings = new Gson().toJsonTree(database.getLinkedUsers());
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
}
