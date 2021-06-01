package com.skyblockplus.dev;

import static com.skyblockplus.Main.database;
import static com.skyblockplus.utils.Utils.*;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.skyblockplus.api.discordserversettings.automatedroles.RoleModel;
import com.skyblockplus.api.discordserversettings.settingsmanagers.ServerSettingsModel;
import com.skyblockplus.api.discordserversettings.skyblockevent.SbEvent;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;

public class QuickSetupTestCommand extends Command {

	public QuickSetupTestCommand() {
		this.name = "d-settings";
		this.ownerCommand = true;
	}

	@Override
	protected void execute(CommandEvent event) {
		new Thread(
			() -> {
				EmbedBuilder eb = loadingEmbed();
				Message ebMessage = event.getChannel().sendMessage(eb.build()).complete();
				String content = event.getMessage().getContentRaw();
				String[] args = content.split(" ", 4);

				logCommand(event.getGuild(), event.getAuthor(), content);

				if (args.length >= 4) {
					if (args[1].equals("roles")) {
						ebMessage.editMessage(setRoleSettings(args[2], args[3], event).build()).queue();
						return;
					} else if (args[1].equals("delete")) {
						switch (args[2]) {
							case "server":
								ebMessage.editMessage(deleteServer(args[3]).build()).queue();
								return;
							case "apply_cache":
								ebMessage.editMessage(deleteServerApplyCache(args[3], args[4]).build()).queue();
								return;
							case "skyblock_event":
								ebMessage.editMessage(deleteSkyblockEvent(args[3]).build()).queue();
								return;
						}
					}
				}

				ebMessage.editMessage(defaultEmbed("Invalid input").build()).queue();
			}
		)
			.start();
	}

	private EmbedBuilder deleteSkyblockEvent(String serverId) {
		return defaultEmbed("API returned response code " + database.updateSkyblockEventSettings(serverId, new SbEvent()));
	}

	private EmbedBuilder deleteServerApplyCache(String serverId, String name) {
		if (database.getServerSettings(serverId) != null) {
			return defaultEmbed("API returned response code " + database.deleteApplyCacheSettings(serverId, name));
		}
		return defaultEmbed("Error updating settings");
	}

	private EmbedBuilder deleteServer(String serverId) {
		if (database.getServerSettings(serverId) != null) {
			return defaultEmbed("API returned response code " + database.deleteServerSettings(serverId));
		}
		return defaultEmbed("Error updating settings");
	}

	private EmbedBuilder setRoleSettings(String roleName, String json, CommandEvent event) {
		try {
			JsonElement jsonElement = new Gson().toJsonTree(new Gson().fromJson(json, RoleModel.class));
			if (higherDepth(database.getServerSettings(event.getGuild().getId()), "serverId") == null) {
				database.addNewServerSettings(
					event.getGuild().getId(),
					new ServerSettingsModel(event.getGuild().getName(), event.getGuild().getId())
				);
			}

			int responseCode = database.updateRoleSettings(event.getGuild().getId(), roleName, jsonElement);
			return defaultEmbed("API returned response code: " + responseCode);
		} catch (Exception ignored) {}
		return defaultEmbed("Error updating settings");
	}
}
