package com.skyblockplus.guilds;

import static com.skyblockplus.Main.database;
import static com.skyblockplus.utils.Utils.*;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.skyblockplus.api.serversettings.automatedapply.AutomatedApply;
import com.skyblockplus.utils.command.CommandExecute;
import java.util.List;

public class GuildRequirementsCommand extends Command {

	public GuildRequirementsCommand() {
		this.name = "guild-requirements";
		this.cooldown = globalCooldown;
		this.aliases = new String[] { "g-reqs", "guild-reqs" };
	}

	@Override
	protected void execute(CommandEvent event) {
		new CommandExecute(this, event) {
			@Override
			protected void execute() {
				logCommand();

				if (args.length == 2) {
					JsonArray guildReqs = null;
					try {
						guildReqs = database.getApplyReqs(event.getGuild().getId(), args[1]).getAsJsonArray();
					} catch (Exception ignored) {}

					if (guildReqs == null) {
						List<AutomatedApply> allGuilds = database.getAllApplySettings(event.getGuild().getId());

						String ebStr = null;
						try {
							ebStr = allGuilds.get(0).getName() + " or " + allGuilds.get(1).getName();
						} catch (Exception ignored) {}

						embed(
							invalidEmbed(
								(
									ebStr != null
										? args[1] + " is an invalid name\nValid options are: " + ebStr
										: "No requirements set for " + args[1]
								)
							)
						);
						return;
					}

					if (guildReqs.size() == 0) {
						embed(invalidEmbed("No requirements set for " + args[1]));
						return;
					}

					eb = defaultEmbed("Guild Requirements");
					for (JsonElement req : guildReqs) {
						eb.addField(
							"Requirement",
							"Slayer: " +
							formatNumber(higherDepth(req, "slayerReq", 0)) +
							"\nSkill Average: " +
							formatNumber(higherDepth(req, "skillsReq", 0)) +
							"\nCatacombs: " +
							formatNumber(higherDepth(req, "catacombsReq", 0)) +
							"\nWeight: " +
							formatNumber(higherDepth(req, "weightReq", 0)),
							false
						);
					}

					embed(eb);
					return;
				}

				sendErrorEmbed();
			}
		}
			.submit();
	}
}
