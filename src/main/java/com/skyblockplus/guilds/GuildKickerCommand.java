package com.skyblockplus.guilds;

import static com.skyblockplus.Main.waiter;
import static com.skyblockplus.utils.Utils.*;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.skyblockplus.utils.CustomPaginator;
import com.skyblockplus.utils.structs.PaginatorExtras;
import com.skyblockplus.utils.structs.UsernameUuidStruct;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;

public class GuildKickerCommand extends Command {

	public GuildKickerCommand() {
		this.name = "guild-kicker";
		this.cooldown = globalCooldown;
		this.aliases = new String[] { "g-kicker" };
	}

	@Override
	protected void execute(CommandEvent event) {
		new Thread(
			() -> {
				EmbedBuilder eb = loadingEmbed();
				Message ebMessage = event.getChannel().sendMessage(eb.build()).complete();

				String content = event.getMessage().getContentRaw();

				String[] args = content.split(" ", 3);

				logCommand(event.getGuild(), event.getAuthor(), content);

				if (args.length == 3 && args[1].toLowerCase().startsWith("u:")) {
					eb = getGuildKicker(args[1].split(":")[1], args[2], event);

					if (eb == null) {
						ebMessage.delete().queue();
					} else {
						ebMessage.editMessage(eb.build()).queue();
					}
					return;
				}

				ebMessage.editMessage(errorMessage(this.name).build()).queue();
			}
		)
			.start();
	}

	private EmbedBuilder getGuildKicker(String username, String reqs, CommandEvent event) {
		String[] reqsArr = reqs.split("] \\[");
		if (reqsArr.length > 3) {
			return defaultEmbed("Error").setDescription("You can only enter a maximum of 3 sets of requirements");
		}
		for (int i = 0; i < reqsArr.length; i++) {
			String[] indvReqs = reqsArr[i].replace("[", "").replace("]", "").split(" ");
			for (String indvReq : indvReqs) {
				String[] reqDashSplit = indvReq.split(":");
				if (reqDashSplit.length != 2) {
					return defaultEmbed("Error").setDescription(indvReq + " is an invalid requirement format");
				}

				if (
					!reqDashSplit[0].equals("slayer") &&
					!reqDashSplit[0].equals("skills") &&
					!reqDashSplit[0].equals("catacombs") &&
					!reqDashSplit[0].equals("weight")
				) {
					return defaultEmbed("Error").setDescription(indvReq + " is an invalid requirement type");
				}

				try {
					Double.parseDouble(reqDashSplit[1]);
				} catch (Exception e) {
					return defaultEmbed("Error").setDescription(indvReq + " is an invalid requirement value");
				}
			}

			reqsArr[i] = reqsArr[i].replace("[", "").replace("]", "");
		}

		UsernameUuidStruct usernameUuidStruct = usernameToUuid(username);
		JsonElement guildJson = getJson(
			"https://api.hypixel.net/findGuild?key=" + HYPIXEL_API_KEY + "&byUuid=" + usernameUuidStruct.playerUuid
		);
		if (guildJson != null && !higherDepth(guildJson, "guild").isJsonNull()) {
			String guildId = higherDepth(guildJson, "guild").getAsString();
			JsonElement guildLbJson = getJson("https://hypixel-app-api.senither.com/leaderboard/players/" + guildId);
			if (higherDepth(guildLbJson, "data") != null) {
				JsonArray guildMembers = higherDepth(guildLbJson, "data").getAsJsonArray();

				CustomPaginator.Builder paginateBuilder = defaultPaginator(waiter, event.getAuthor()).setColumns(1).setItemsPerPage(20);
				int missingReqsCount = 0;
				for (JsonElement guildMember : guildMembers) {
					double slayer = higherDepth(guildMember, "total_slayer").getAsDouble();
					double skills = higherDepth(guildMember, "average_skill_progress").getAsDouble();
					double catacombs = higherDepth(guildMember, "catacomb").getAsDouble();
					double weight = higherDepth(guildMember, "raw_weight.total").getAsDouble();

					boolean meetsReqs = false;

					for (String req : reqsArr) {
						String[] reqSplit = req.split(" ");
						double slayerReq = 0;
						double skillsReq = 0;
						double catacombsReq = 0;
						double weightReq = 0;
						for (String reqIndividual : reqSplit) {
							switch (reqIndividual.split(":")[0]) {
								case "slayer":
									slayerReq = Double.parseDouble(reqIndividual.split(":")[1]);
									break;
								case "skills":
									skillsReq = Double.parseDouble(reqIndividual.split(":")[1]);
									break;
								case "catacombs":
									catacombsReq = Double.parseDouble(reqIndividual.split(":")[1]);
									break;
								case "weight":
									weightReq = Double.parseDouble(reqIndividual.split(":")[1]);
									break;
							}
						}

						if (slayer >= slayerReq && skills >= skillsReq && catacombs >= catacombsReq && weight >= weightReq) {
							meetsReqs = true;
							break;
						}
					}

					if (!meetsReqs) {
						paginateBuilder.addItems(
							"• **" +
							higherDepth(guildMember, "username").getAsString() +
							"** | Slayer: " +
							formatNumber(slayer) +
							" | Skills: " +
							roundAndFormat(skills) +
							" | Cata: " +
							roundAndFormat(catacombs) +
							" | Weight: " +
							roundAndFormat(weight)
						);
						missingReqsCount++;
					}
				}

				paginateBuilder
					.setPaginatorExtras(
						new PaginatorExtras()
							.setEveryPageTitle("Guild Kick Helper")
							.setEveryPageText("**Total missing requirements: " + missingReqsCount + "**\n")
					)
					.build()
					.paginate(event.getChannel(), 0);

				return null;
			}
		}

		return defaultEmbed("Unable to fetch guild data");
	}
}
