package com.skyblockplus.miscellaneous;

import static com.skyblockplus.utils.Hypixel.*;
import static com.skyblockplus.utils.Utils.*;

import com.google.gson.JsonElement;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.skyblockplus.utils.command.CommandExecute;
import com.skyblockplus.utils.structs.HypixelResponse;
import com.skyblockplus.utils.structs.UsernameUuidStruct;
import java.time.Instant;
import net.dv8tion.jda.api.EmbedBuilder;

public class HypixelCommand extends Command {

	public HypixelCommand() {
		this.name = "hypixel";
		this.cooldown = globalCooldown;
	}

	public static EmbedBuilder getParkourStats(String username) {
		UsernameUuidStruct usernameUuid = usernameToUuid(username);
		if (usernameUuid.isNotValid()) {
			return invalidEmbed(usernameUuid.failCause);
		}

		HypixelResponse hypixelJson = playerFromUuid(usernameUuid.playerUuid);
		if (hypixelJson.isNotValid()) {
			return invalidEmbed(hypixelJson.failCause);
		}

		try {
			EmbedBuilder eb = defaultEmbed(
				usernameUuid.playerUsername,
				"https://plancke.io/hypixel/player/stats/" + usernameUuid.playerUuid
			);
			StringBuilder parkourCompletionString = new StringBuilder();
			for (String parkourLocation : getJsonKeys(hypixelJson.get("parkourCompletions"))) {
				int fastestTime = -1;
				for (JsonElement parkourTime : hypixelJson.get("parkourCompletions." + parkourLocation).getAsJsonArray()) {
					if (higherDepth(parkourTime, "timeTook").getAsInt() > fastestTime) {
						fastestTime = higherDepth(parkourTime, "timeTook").getAsInt();
					}
				}
				if (fastestTime != -1) {
					parkourCompletionString.append("• ").append(parkourLocation).append(": ").append(fastestTime / 1000).append("s\n");
				}
			}

			if (parkourCompletionString.length() > 0) {
				eb.setDescription("**Fastest Parkour Times:**\n" + parkourCompletionString);
				return eb;
			}
		} catch (Exception ignored) {}
		return invalidEmbed("Player has no completed parkours");
	}

	public static EmbedBuilder getHypixelStats(String username) {
		UsernameUuidStruct usernameUuid = usernameToUuid(username);
		if (usernameUuid.isNotValid()) {
			return invalidEmbed(usernameUuid.failCause);
		}

		HypixelResponse hypixelResponse = playerFromUuid(usernameUuid.playerUuid);
		if (hypixelResponse.isNotValid()) {
			return invalidEmbed(hypixelResponse.failCause);
		}
		EmbedBuilder eb = defaultEmbed(usernameUuid.playerUsername, "https://plancke.io/hypixel/player/stats/" + usernameUuid.playerUuid);

		JsonElement hypixelJson = hypixelResponse.response;
		try {
			if (higherDepth(hypixelJson, "lastLogin").getAsInt() > higherDepth(hypixelJson, "lastLogout").getAsInt()) {
				eb.addField("Online", "🟢", true);
			} else {
				eb.addField("Status", "🔴", true);

				eb.addField(
					"Last Updated",
					"<t:" + Instant.ofEpochMilli(higherDepth(hypixelJson, "lastLogout").getAsLong()).getEpochSecond() + ">",
					true
				);
			}
		} catch (Exception ignored) {}

		try {
			eb.addField(
				"First Login",
				"<t:" + Instant.ofEpochMilli(higherDepth(hypixelJson, "firstLogin").getAsLong()).getEpochSecond() + ":D>",
				true
			);
		} catch (Exception ignored) {}

		double networkLevel = (Math.sqrt((2 * higherDepth(hypixelJson, "networkExp").getAsLong()) + 30625) / 50) - 2.5;
		eb.addField("Hypixel Level", roundAndFormat(networkLevel), true);

		String hypixelRank = "None";
		if (higherDepth(hypixelJson, "rank") != null && !higherDepth(hypixelJson, "rank").getAsString().equals("NORMAL")) {
			hypixelRank = capitalizeString(higherDepth(hypixelJson, "rank").getAsString());
		} else if (
			higherDepth(hypixelJson, "monthlyPackageRank") != null &&
			higherDepth(hypixelJson, "monthlyPackageRank").getAsString().equals("SUPERSTAR")
		) {
			hypixelRank = "MVP++";
		} else if (
			higherDepth(hypixelJson, "newPackageRank") != null && !higherDepth(hypixelJson, "newPackageRank").getAsString().equals("NONE")
		) {
			hypixelRank = higherDepth(hypixelJson, "newPackageRank").getAsString().replace("PLUS", "+").replace("_", "");
		} else if (
			higherDepth(hypixelJson, "packageRank") != null && !higherDepth(hypixelJson, "packageRank").getAsString().equals("NONE")
		) {
			hypixelRank = higherDepth(hypixelJson, "packageRank").getAsString().replace("PLUS", "+").replace("_", "");
		}

		eb.addField("Hypixel Rank", hypixelRank, true);

		try {
			for (String socialMedia : getJsonKeys(higherDepth(hypixelJson, "socialMedia.links"))) {
				String currentSocialMediaLink = higherDepth(higherDepth(hypixelJson, "socialMedia.links"), socialMedia).getAsString();
				eb.addField(
					socialMedia.equals("HYPIXEL") ? "Hypixel Forums" : capitalizeString(socialMedia.toLowerCase()),
					currentSocialMediaLink.contains("http") ? "[Link](" + currentSocialMediaLink + ")" : currentSocialMediaLink,
					true
				);
			}
		} catch (Exception ignored) {}

		try {
			eb.addField(
				"Most Recent Lobby",
				capitalizeString(higherDepth(hypixelJson, "mostRecentGameType").getAsString().toLowerCase()),
				true
			);
		} catch (Exception ignored) {}

		try {
			HypixelResponse guildResponse = getGuildFromPlayer(usernameUuid.playerUuid);
			if (!guildResponse.isNotValid()) {
				eb.addField("Guild", guildResponse.get("name").getAsString(), true);

				for (JsonElement member : guildResponse.get("members").getAsJsonArray()) {
					if (higherDepth(member, "uuid").getAsString().equals(usernameUuid.playerUuid)) {
						eb.addField(
							"Guild Rank",
							higherDepth(member, "rank").getAsString().equals("GUILDMASTER")
								? "Guild Master"
								: higherDepth(member, "rank").getAsString(),
							true
						);
						eb.addField(
							"Joined Guild",
							"<t:" + Instant.ofEpochMilli(higherDepth(member, "joined").getAsLong()).getEpochSecond() + ":D>",
							true
						);
					}
				}
			}
		} catch (Exception ignored) {}

		try {
			eb.addField("Karma", formatNumber(higherDepth(hypixelJson, "karma").getAsLong()), true);
		} catch (Exception ignored) {}

		try {
			eb.addField("Achievement Points", formatNumber(higherDepth(hypixelJson, "achievementPoints").getAsLong()), true);
		} catch (Exception ignored) {}

		StringBuilder namesString = new StringBuilder();
		for (JsonElement name : higherDepth(getJson("https://api.ashcon.app/mojang/v2/user/" + usernameUuid.playerUuid), "username_history")
			.getAsJsonArray()) {
			if (!higherDepth(name, "username").getAsString().equals(usernameUuid.playerUsername)) {
				namesString.append("• ").append(higherDepth(name, "username").getAsString()).append("\n");
			}
		}
		if (namesString.length() > 0) {
			eb.addField("Aliases", namesString.toString(), true);
		}

		String skyblockItems = "";
		if (higherDepth(hypixelJson, "skyblock_free_cookie") != null) {
			skyblockItems +=
				"• Free booster cookie: " +
				"<t:" +
				Instant.ofEpochMilli(higherDepth(hypixelJson, "skyblock_free_cookie").getAsLong()).getEpochSecond() +
				":d>" +
				"\n";
		}

		if (higherDepth(hypixelJson, "scorpius_bribe_96") != null) {
			skyblockItems +=
				"• Scorpius Bribe (Year 96): " +
				"<t:" +
				Instant.ofEpochMilli(higherDepth(hypixelJson, "scorpius_bribe_96").getAsLong()).getEpochSecond() +
				":d>" +
				"\n";
		}

		if (higherDepth(hypixelJson, "scorpius_bribe_120") != null) {
			skyblockItems +=
				"• Scorpius Bribe (Year 120): " +
				"<t:" +
				Instant.ofEpochMilli(higherDepth(hypixelJson, "scorpius_bribe_120").getAsLong()).getEpochSecond() +
				":d>" +
				"\n";
		}

		if (higherDepth(hypixelJson, "claimed_potato_talisman") != null) {
			skyblockItems +=
				"• Potato Talisman: " +
				"<t:" +
				Instant.ofEpochMilli(higherDepth(hypixelJson, "claimed_potato_talisman").getAsLong()).getEpochSecond() +
				":d>" +
				"\n";
		}

		if (higherDepth(hypixelJson, "claimed_potato_basket") != null) {
			skyblockItems +=
				"• Potato Basket: " +
				"<t:" +
				Instant.ofEpochMilli(higherDepth(hypixelJson, "claimed_potato_basket").getAsLong()).getEpochSecond() +
				":d>" +
				"\n";
		}

		if (higherDepth(hypixelJson, "claim_potato_war_crown") != null) {
			skyblockItems +=
				"• Potato War Crown: " +
				"<t:" +
				Instant.ofEpochMilli(higherDepth(hypixelJson, "claim_potato_war_crown").getAsLong()).getEpochSecond() +
				":d>" +
				"\n";
		}

		if (skyblockItems.length() > 0) {
			eb.addField("Skyblock Misc", skyblockItems, true);
		}

		int fillGap = 3 - ((eb.getFields().size() % 3) == 0 ? 3 : (eb.getFields().size() % 3));
		for (int i = 0; i < fillGap; i++) {
			eb.addBlankField(true);
		}
		eb.setThumbnail("https://cravatar.eu/helmavatar/" + usernameUuid.playerUuid + "/64.png");
		return eb;
	}

	@Override
	protected void execute(CommandEvent event) {
		new CommandExecute(this, event) {
			@Override
			protected void execute() {
				if ((args.length == 3 || args.length == 2) && args[1].equals("parkour")) {
					if (getMentionedUsername(args.length == 2 ? -1 : 1)) {
						return;
					}

					embed(getParkourStats(username));
					return;
				} else if (args.length == 2 || args.length == 1) {
					if (getMentionedUsername(args.length == 1 ? -1 : 1)) {
						return;
					}

					embed(getHypixelStats(username));
					return;
				}

				sendErrorEmbed();
			}
		}
			.submit();
	}
}
