package com.skyblockplus.miscellaneous;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jdautilities.commons.waiter.EventWaiter;
import com.skyblockplus.utils.CustomPaginator;
import com.skyblockplus.utils.PaginatorExtras;
import com.skyblockplus.utils.UsernameUuidStruct;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

import static com.skyblockplus.Main.asyncHttpClient;
import static com.skyblockplus.utils.Utils.*;

public class ProfilesCommand extends Command {
    private final EventWaiter waiter;
    private CommandEvent event;

    public ProfilesCommand(EventWaiter waiter) {
        this.name = "profiles";
        this.cooldown = globalCooldown;
        this.waiter = waiter;
    }

    @Override
    protected void execute(CommandEvent event) {
        new Thread(() -> {
            EmbedBuilder eb = loadingEmbed();
            Message ebMessage = event.getChannel().sendMessage(eb.build()).complete();
            String content = event.getMessage().getContentRaw();
            String[] args = content.split(" ");
            this.event = event;

            logCommand(event.getGuild(), event.getAuthor(), content);

            if (args.length == 2){
                eb = getPlayerProfiles(args[1]);
                if(eb == null){
                    ebMessage.delete().queue();
                }else{
                    ebMessage.editMessage(eb.build()).queue();
                }
                return;
            }

            ebMessage.editMessage(errorMessage(this.name).build()).queue();
        }).start();
    }

    private EmbedBuilder getPlayerProfiles(String username) {
        UsernameUuidStruct usernameUuidStruct = usernameToUuid(username);
        if (usernameUuidStruct != null) {

            DateTimeFormatter dateTimeFormatter = DateTimeFormatter
                    .ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT).withLocale(Locale.US)
                    .withZone(ZoneId.systemDefault());

            JsonArray profileArray = higherDepth(
                    getJson("https://api.hypixel.net/skyblock/profiles?key=" + HYPIXEL_API_KEY + "&uuid=" + usernameUuidStruct.playerUuid), "profiles").getAsJsonArray();

            List<CompletableFuture<String>> profileUsernameFutureList = new ArrayList<>();

            for(JsonElement profile:profileArray) {
                List<String> uuids = getJsonKeys(higherDepth(profile, "members"));

                for (String uuid: uuids) {
                    profileUsernameFutureList.add(asyncHttpClient
                            .prepareGet("https://api.ashcon.app/mojang/v2/user/" + uuid)
                            .execute()
                            .toCompletableFuture()
                            .thenApply(
                                    uuidToUsernameResponse -> {
                                        String playerUsername = higherDepth(JsonParser.parseString(uuidToUsernameResponse.getResponseBody()), "username").getAsString();
                                        String lastLogin = dateTimeFormatter.format(Instant.ofEpochMilli(higherDepth(higherDepth(higherDepth(profile, "members"), uuid), "last_save").getAsLong()));

                                        return "\n• " + playerUsername + " last logged in on " + lastLogin;
                                    }
                            )
                    );
                }
            }

            CustomPaginator.Builder paginateBuilder = defaultPaginator(waiter, event.getAuthor()).setColumns(1).setItemsPerPage(1);

            List<String> pageTitles = new ArrayList<>();
            int count = 0;
            for(JsonElement profile:profileArray) {
                pageTitles.add("Profiles for " + usernameUuidStruct.playerUsername);
                StringBuilder profileStr = new StringBuilder("• **Profile Name:** " + higherDepth(profile, "cute_name").getAsString() + (higherDepth(profile, "game_mode") != null ? " ♻️" : ""));
                List<String> uuids = getJsonKeys(higherDepth(profile, "members"));
                profileStr.append("\n• **Member Count:** ").append(uuids.size());
                profileStr.append("\n\n**Members:** ");

                for (String ignored:uuids) {
                    try {
                        profileStr.append(profileUsernameFutureList.get(count).get());
                    } catch (Exception ignored1) {
                    }
                    count ++;
                }
                paginateBuilder.addItems(profileStr.toString());
            }

            paginateBuilder.setPaginatorExtras(new PaginatorExtras().setTitles(pageTitles));
            return null;
        }
        return defaultEmbed("Unable to fetch player data");
    }
}
