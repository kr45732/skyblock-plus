package com.skyblockplus.miscellaneous;

import static com.skyblockplus.Main.*;
import static com.skyblockplus.utils.Player.skyblockStatsLink;
import static com.skyblockplus.utils.Utils.*;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.skyblockplus.utils.CustomPaginator;
import com.skyblockplus.utils.structs.PaginatorExtras;
import com.skyblockplus.utils.structs.UsernameUuidStruct;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.interactions.InteractionHook;

public class ProfilesCommand extends Command {

	public ProfilesCommand() {
		this.name = "profiles";
		this.cooldown = globalCooldown;
	}

	public static EmbedBuilder getPlayerProfiles(String username, User user, MessageChannel channel, InteractionHook hook) {
		UsernameUuidStruct usernameUuidStruct = usernameToUuid(username);
		if (usernameUuidStruct != null) {
			DateTimeFormatter dateTimeFormatter = DateTimeFormatter
				.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
				.withLocale(Locale.US)
				.withZone(ZoneId.systemDefault());

			JsonArray profileArray;
			try {
				profileArray =
					higherDepth(
						getJson(
							"https://api.hypixel.net/skyblock/profiles?key=" + HYPIXEL_API_KEY + "&uuid=" + usernameUuidStruct.playerUuid
						),
						"profiles"
					)
						.getAsJsonArray();
			} catch (Exception e) {
				return defaultEmbed("Unable to fetch player data");
			}

			List<CompletableFuture<String>> profileUsernameFutureList = new ArrayList<>();

			for (JsonElement profile : profileArray) {
				List<String> uuids = getJsonKeys(higherDepth(profile, "members"));

				for (String uuid : uuids) {
					profileUsernameFutureList.add(
						asyncHttpClient
							.prepareGet("https://api.ashcon.app/mojang/v2/user/" + uuid)
							.execute()
							.toCompletableFuture()
							.thenApply(
								uuidToUsernameResponse -> {
									String playerUsername = higherDepth(
										JsonParser.parseString(uuidToUsernameResponse.getResponseBody()),
										"username"
									)
										.getAsString();
									String lastLogin = dateTimeFormatter.format(
										Instant.ofEpochMilli(higherDepth(profile, "members." + uuid + ".last_save").getAsLong())
									);

									return ("\n• " + playerUsername + " last logged in on " + lastLogin);
								}
							)
					);
				}
			}

			CustomPaginator.Builder paginateBuilder = defaultPaginator(waiter, user).setColumns(1).setItemsPerPage(1);

			List<String> pageTitlesUrls = new ArrayList<>();
			int count = 0;
			for (JsonElement profile : profileArray) {
				pageTitlesUrls.add(skyblockStatsLink(usernameUuidStruct.playerUsername, higherDepth(profile, "cute_name").getAsString()));
				StringBuilder profileStr = new StringBuilder(
					"• **Profile Name:** " +
					higherDepth(profile, "cute_name").getAsString() +
					(higherDepth(profile, "game_mode") != null ? " ♻️" : "")
				);
				List<String> uuids = getJsonKeys(higherDepth(profile, "members"));
				profileStr.append("\n• **Member Count:** ").append(uuids.size());
				profileStr.append("\n\n**Members:** ");

				for (String uuid : uuids) {
					try {
						profileStr.append(profileUsernameFutureList.get(count).get());
					} catch (Exception ignored) {}
					count++;
				}
				paginateBuilder.addItems(profileStr.toString());
			}

			paginateBuilder.setPaginatorExtras(
				new PaginatorExtras().setEveryPageTitle(usernameUuidStruct.playerUsername).setTitleUrls(pageTitlesUrls)
			);

			if (channel != null) {
				paginateBuilder.build().paginate(channel, 0);
			} else {
				paginateBuilder.build().paginate(hook, 0);
			}
			return null;
		}
		return defaultEmbed("Unable to fetch player data");
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

				if (args.length == 2) {
					eb = getPlayerProfiles(args[1], event.getAuthor(), event.getChannel(), null);
					if (eb == null) {
						ebMessage.delete().queue();
					} else {
						ebMessage.editMessage(eb.build()).queue();
					}
					return;
				}

				ebMessage.editMessage(errorEmbed(this.name).build()).queue();
			}
		);
	}
}
