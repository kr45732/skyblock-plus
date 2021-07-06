package com.skyblockplus.guilds;

import static com.skyblockplus.Main.database;
import static com.skyblockplus.utils.Utils.*;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.skyblockplus.api.discordserversettings.automatedapplication.AutomatedApplication;
import java.util.List;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;

public class GuildRequirementsCommand extends Command {

	public GuildRequirementsCommand() {
		this.name = "guild-requirements";
		this.cooldown = globalCooldown;
		this.aliases = new String[] { "g-reqs", "guild-reqs" };
	}

	@Override
	protected void execute(CommandEvent event) {
		executor.submit(
			() -> {
				EmbedBuilder eb = loadingEmbed();
				Message ebMessage = event.getChannel().sendMessage(eb.build()).complete();
				String content = event.getMessage().getContentRaw();
				String[] args = content.split(" ");

				logCommand(event.getGuild(), event.getAuthor(), content);
				if (args.length != 2) {
					ebMessage.editMessage(errorEmbed(name).build()).queue();
					return;
				}

				JsonArray guildReqs = null;
				try {
					guildReqs = database.getApplyReqs(event.getGuild().getId(), args[1]).getAsJsonArray();
				} catch (Exception ignored) {}

				if (guildReqs == null) {
					List<AutomatedApplication> allGuilds = database.getAllApplySettings(event.getGuild().getId());

					String ebStr = null;
					try {
						ebStr = allGuilds.get(0).getName() + " or " + allGuilds.get(1).getName();
					} catch (Exception ignored) {}

					ebMessage
						.editMessage(
							defaultEmbed("Error")
								.setDescription(
									(
										ebStr != null
											? args[1] + " is an invalid name\nValid options are: " + ebStr
											: "No requirements set for " + args[1]
									)
								)
								.build()
						)
						.queue();
					return;
				}

				if (guildReqs.size() == 0) {
					ebMessage.editMessage(defaultEmbed("Error").setDescription("No requirements set for " + args[1]).build()).queue();
					return;
				}

				eb = defaultEmbed("Guild Requirements");
				for (JsonElement req : guildReqs) {
					eb.addField(
						"Requirement",
						"Slayer: " +
						formatNumber(higherDepth(req, "slayerReq").getAsInt()) +
						"\nSkill Average: " +
						formatNumber(higherDepth(req, "skillsReq").getAsInt()) +
						"\nCatacombs: " +
						formatNumber(higherDepth(req, "catacombsReq").getAsInt()) +
						"\nWeight: " +
						formatNumber(higherDepth(req, "weightReq").getAsInt()),
						false
					);
				}

				ebMessage.editMessage(eb.build()).queue();
			}
		);
	}
}
