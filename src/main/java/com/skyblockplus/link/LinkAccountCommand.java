package com.skyblockplus.link;

import static com.skyblockplus.Main.database;
import static com.skyblockplus.utils.Utils.*;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.skyblockplus.api.discordserversettings.automatedguildroles.GuildRole;
import com.skyblockplus.api.linkedaccounts.LinkedAccountModel;
import com.skyblockplus.utils.structs.DiscordInfoStruct;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;

public class LinkAccountCommand extends Command {

	public LinkAccountCommand() {
		this.name = "link";
		this.cooldown = globalCooldown;
	}

	public static EmbedBuilder linkAccount(String username, Member member, Guild guild) {
		DiscordInfoStruct playerInfo = getPlayerDiscordInfo(username);
		if (playerInfo != null) {
			if (!member.getUser().getAsTag().equals(playerInfo.discordTag)) {
				EmbedBuilder eb = defaultEmbed("Discord tag mismatch");
				eb.setDescription(
					"Account " +
					playerInfo.minecraftUsername +
					" is linked with the discord tag " +
					playerInfo.discordTag +
					"\nYour current discord tag is " +
					member.getUser().getAsTag()
				);
				return eb;
			}

			LinkedAccountModel toAdd = new LinkedAccountModel(
				"" + Instant.now().toEpochMilli(),
				member.getId(),
				playerInfo.minecraftUuid,
				playerInfo.minecraftUsername
			);

			if (database.addLinkedUser(toAdd) == 200) {
				try {
					if (
						!higherDepth(database.getVerifySettings(guild.getId()), "verifiedNickname").getAsString().equalsIgnoreCase("none")
					) {
						String nicknameTemplate = higherDepth(database.getVerifySettings(guild.getId()), "verifiedNickname").getAsString();
						nicknameTemplate = nicknameTemplate.replace("[IGN]", playerInfo.minecraftUsername);
						if (nicknameTemplate.contains("[GUILD_RANK]")) {
							try {
								List<String> settingsGuildId = database
									.getAllGuildRoles(guild.getId())
									.stream()
									.map(GuildRole::getGuildId)
									.collect(Collectors.toList());
								JsonElement playerGuild = higherDepth(
									getJson("https://api.hypixel.net/guild?key=" + HYPIXEL_API_KEY + "&player=" + playerInfo.minecraftUuid),
									"guild"
								);
								if (settingsGuildId.contains(higherDepth(playerGuild, "_id").getAsString())) {
									JsonArray guildMembers = higherDepth(playerGuild, "members").getAsJsonArray();
									for (JsonElement guildMember : guildMembers) {
										if (higherDepth(guildMember, "uuid").getAsString().equals(playerInfo.minecraftUuid)) {
											nicknameTemplate =
												nicknameTemplate.replace("[GUILD_RANK]", higherDepth(guildMember, "rank").getAsString());
											break;
										}
									}
								}
							} catch (Exception ignored) {}
						}

						member.modifyNickname(nicknameTemplate).queue();
					}
				} catch (Exception ignored) {}

				try {
					JsonArray verifyRoles = higherDepth(database.getVerifySettings(guild.getId()), "verifiedRoles").getAsJsonArray();
					for (JsonElement verifyRole : verifyRoles) {
						try {
							guild.addRoleToMember(member.getId(), guild.getRoleById(verifyRole.getAsString())).complete();
						} catch (Exception e) {
							System.out.println("== Stack Trace (linkAccount - add role inside for - " + member.getId() + ") ==");
							e.printStackTrace();
						}
					}
				} catch (Exception ignored) {}

				return defaultEmbed("Success")
					.setDescription(
						"`" + member.getUser().getAsTag() + "` was linked to `" + fixUsername(playerInfo.minecraftUsername) + "`"
					);
			} else {
				return defaultEmbed("Error")
					.setDescription(
						"Error linking `" + member.getUser().getAsTag() + " to `" + fixUsername(playerInfo.minecraftUsername) + "`"
					);
			}
		}

		return defaultEmbed("Error")
			.setDescription(
				username +
				" is not linked to a Discord account. For help on how to link view [__**this**__](https://streamable.com/sdq8tp) video"
			);
	}

	public static EmbedBuilder getLinkedAccount(User user) {
		JsonElement userInfo = database.getLinkedUserByDiscordId(user.getId());

		try {
			return defaultEmbed("Success")
				.setDescription(
					"`" + user.getAsTag() + "` is linked to `" + (higherDepth(userInfo, "minecraftUsername").getAsString()) + "`"
				);
		} catch (Exception e) {
			return defaultEmbed("Error").setDescription("`" + user.getAsTag() + "` is not linked");
		}
	}

	@Override
	protected void execute(CommandEvent event) {
		new Thread(
			() -> {
				EmbedBuilder eb = loadingEmbed();
				Message ebMessage = event.getChannel().sendMessage(eb.build()).complete();
				String content = event.getMessage().getContentRaw();
				String[] args = content.split(" ");

				logCommand(event.getGuild(), event.getAuthor(), content);

				if (args.length == 2) {
					ebMessage.editMessage(linkAccount(args[1], event.getMember(), event.getGuild()).build()).queue();
					return;
				} else if (args.length == 1) {
					ebMessage.editMessage(getLinkedAccount(event.getAuthor()).build()).queue();
					return;
				}

				ebMessage.editMessage(errorMessage(this.name).build()).queue();
			}
		)
			.start();
	}
}
