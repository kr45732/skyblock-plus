package com.skyblockplus.miscellaneous;

import static com.skyblockplus.Main.waiter;
import static com.skyblockplus.utils.Hypixel.*;
import static com.skyblockplus.utils.Utils.*;

import com.google.gson.JsonElement;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.skyblockplus.utils.command.CommandExecute;
import com.skyblockplus.utils.command.CustomPaginator;
import com.skyblockplus.utils.structs.HypixelResponse;
import com.skyblockplus.utils.structs.PaginatorExtras;
import com.skyblockplus.utils.structs.UsernameUuidStruct;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.interactions.InteractionHook;

public class ProfilesCommand extends Command {

	public ProfilesCommand() {
		this.name = "profiles";
		this.cooldown = globalCooldown;
	}

	public static EmbedBuilder getPlayerProfiles(String username, User user, MessageChannel channel, InteractionHook hook) {
		UsernameUuidStruct usernameUuid = usernameToUuid(username);
		if (usernameUuid.isNotValid()) {
			return invalidEmbed(usernameUuid.failCause);
		}

		HypixelResponse profilesJson = skyblockProfilesFromUuid(usernameUuid.playerUuid);
		if (profilesJson.isNotValid()) {
			return invalidEmbed(profilesJson.failCause);
		}

		List<CompletableFuture<String>> profileUsernameFutureList = new ArrayList<>();

		for (JsonElement profile : profilesJson.response.getAsJsonArray()) {
			List<String> uuids = getJsonKeys(higherDepth(profile, "members"));

			for (String uuid : uuids) {
				profileUsernameFutureList.add(
					asyncUuidToUsername(uuid)
						.thenApply(
							playerUsername -> {
								String lastLogin =
									"<t:" +
									Instant
										.ofEpochMilli(higherDepth(profile, "members." + uuid + ".last_save").getAsLong())
										.getEpochSecond() +
									">";

								return "\n• " + playerUsername + " last logged in on " + lastLogin;
							}
						)
				);
			}
		}

		CustomPaginator.Builder paginateBuilder = defaultPaginator(waiter, user).setColumns(1).setItemsPerPage(1);

		List<String> pageTitlesUrls = new ArrayList<>();
		int count = 0;
		for (JsonElement profile : profilesJson.response.getAsJsonArray()) {
			pageTitlesUrls.add(skyblockStatsLink(usernameUuid.playerUsername, higherDepth(profile, "cute_name").getAsString()));
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
			new PaginatorExtras().setEveryPageTitle(usernameUuid.playerUsername).setTitleUrls(pageTitlesUrls)
		);

		if (channel != null) {
			paginateBuilder.build().paginate(channel, 0);
		} else {
			paginateBuilder.build().paginate(hook, 0);
		}
		return null;
	}

	@Override
	protected void execute(CommandEvent event) {
		new CommandExecute(this, event) {
			@Override
			protected void execute() {
				logCommand();

				if (args.length == 2 || args.length == 1) {
					if (getMentionedUsername(args.length == 1 ? -1 : 1)) {
						return;
					}

					paginate(getPlayerProfiles(username, event.getAuthor(), event.getChannel(), null));
					return;
				}

				sendErrorEmbed();
			}
		}
			.submit();
	}
}
