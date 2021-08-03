package com.skyblockplus.dev;

import static com.skyblockplus.Main.database;
import static com.skyblockplus.utils.Utils.defaultEmbed;
import static com.skyblockplus.utils.Utils.higherDepth;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.skyblockplus.api.serversettings.automatedroles.RoleModel;
import com.skyblockplus.api.serversettings.managers.ServerSettingsModel;
import com.skyblockplus.api.serversettings.skyblockevent.SbEvent;
import com.skyblockplus.utils.command.CommandExecute;
import net.dv8tion.jda.api.EmbedBuilder;

public class QuickSetupTestCommand extends Command {

	public QuickSetupTestCommand() {
		this.name = "d-settings";
		this.ownerCommand = true;
	}

	@Override
	protected void execute(CommandEvent event) {
		new CommandExecute(this, event) {
			@Override
			protected void execute() {
				logCommand();
				setArgs(4);

				if (args.length == 4) {
					if (args[1].equals("roles")) {
						embed(setRoleSettings(args[2], args[3], event));
						return;
					} else if (args[1].equals("delete")) {
						switch (args[2]) {
							case "server":
								embed(deleteServer(args[3]));
								return;
							case "apply_cache":
								setArgs(5);
								embed(deleteServerApplyCache(args[3], args[4]));
								return;
							case "skyblock_event":
								embed(deleteSkyblockEvent(args[3]));
								return;
						}
					}
				}

				sendErrorEmbed();
			}
		}
			.submit();
	}

	private EmbedBuilder deleteSkyblockEvent(String serverId) {
		return defaultEmbed("API returned response code " + database.setSkyblockEventSettings(serverId, new SbEvent()));
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

			int responseCode = database.setRoleSettings(event.getGuild().getId(), roleName, jsonElement);
			return defaultEmbed("API returned response code: " + responseCode);
		} catch (Exception ignored) {}
		return defaultEmbed("Error updating settings");
	}
}
