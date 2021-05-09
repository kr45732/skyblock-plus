package com.skyblockplus.guilds;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.skyblockplus.utils.Player;
import com.skyblockplus.utils.structs.UsernameUuidStruct;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;

import java.io.FileReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static com.skyblockplus.Main.asyncHttpClient;
import static com.skyblockplus.utils.Utils.*;

public class GuildLeaderboardCommand extends Command {

    public GuildLeaderboardCommand() {
        this.name = "guild-rank";
        // this.cooldown = (BOT_PREFIX.equals("+") ? 240 : 0);
        this.aliases = new String[]{"g-rank"};
        this.ownerCommand = true;
    }

    @Override
    protected void execute(CommandEvent event) {
        new Thread(() -> {
            EmbedBuilder eb = loadingEmbed();
            Message ebMessage = event.getChannel().sendMessage(eb.build()).complete();

            String content = event.getMessage().getContentRaw();

            String[] args = content.split(" ");
            if (args.length != 2) {
                eb = errorMessage(this.name);
                ebMessage.editMessage(eb.build()).queue();
                return;
            }

            logCommand(event.getGuild(), event.getAuthor(), content);

            if (args[1].toLowerCase().startsWith("u-")) {
                String[] rankString = getLeaderboard(args[1].split("-")[1]);
                if (rankString != null) {
                    eb = defaultEmbed("Rank changes for guild " + rankString[2]);
                    if (rankString.length == 4 && rankString[3].equals("false")) {
                        eb.setDescription("**There was an error fetching some stats!**");
                    }
                    eb.addField("Promote", rankString[0], false);
                    eb.addField("Demote", rankString[1], false);
                } else {
                    eb = errorMessage(this.name);
                    ebMessage.editMessage(eb.build()).queue();
                    return;
                }
            } else {
                eb = errorMessage(this.name);
                ebMessage.editMessage(eb.build()).queue();
                return;
            }

            ebMessage.editMessage(eb.build()).queue();
        }).start();
    }

    private String[] getLeaderboard(String username) {
        UsernameUuidStruct usernameUuidStruct = usernameToUuid(username);
        if (usernameUuidStruct == null) {
            return null;
        }

        JsonElement guildJson = getJson(
                "https://api.hypixel.net/guild?key=" + HYPIXEL_API_KEY + "&player=" + usernameUuidStruct.playerUuid);
        String guildId = higherDepth(guildJson, "guild._id").getAsString();
        String guildName = higherDepth(guildJson, "guild.name").getAsString();
        if (!guildName.equals("Skyblock Forceful")) {
            return new String[]{"Currently only supported for the Skyblock Forceful guild",
                    "Currently only supported for the Skyblock Forceful guild", ""};
        }

        List<String> staffRankNames = new ArrayList<>();
        List<String> topRoleName = new ArrayList<>();
        List<String> middleRoleName = new ArrayList<>();
        List<String> defaultRoleName = new ArrayList<>();
        JsonElement guildLeaderboardSettings;

        try {
            JsonElement settings = higherDepth(
                    JsonParser.parseReader(new FileReader("src/main/java/com/skyblockplus/json/GuildSettings.json")),
                    guildId);

            guildLeaderboardSettings = higherDepth(settings, "guild_leaderboard");
            for (JsonElement i : higherDepth(guildLeaderboardSettings, "staff_ranks").getAsJsonArray()) {
                staffRankNames.add(i.getAsString());
            }

            for (JsonElement i : higherDepth(guildLeaderboardSettings, "top_role.names")
                    .getAsJsonArray()) {
                topRoleName.add(i.getAsString().toLowerCase());
            }
            for (JsonElement i : higherDepth(guildLeaderboardSettings, "middle_role.names")
                    .getAsJsonArray()) {
                middleRoleName.add(i.getAsString().toLowerCase());
            }
            for (JsonElement i : higherDepth(guildLeaderboardSettings, "default_role.names")
                    .getAsJsonArray()) {
                defaultRoleName.add(i.getAsString().toLowerCase());
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

        JsonArray guildMembers = higherDepth(guildJson, "guild.members").getAsJsonArray();

        ArrayList<Player> guildSlayer = new ArrayList<>();
        ArrayList<Player> guildSkills = new ArrayList<>();
        ArrayList<Player> guildCatacombs = new ArrayList<>();
        ArrayList<String> uniqueGuildUuid = new ArrayList<>();
        List<CompletableFuture<?>> futures = new ArrayList<>();

        for (JsonElement guildMember : guildMembers) {
            String memberRank = higherDepth(guildMember, "rank").getAsString();
            if (!staffRankNames.contains(memberRank)) {

                String guildMemberUuid = higherDepth(guildMember, "uuid").getAsString();

                try {
                    if (remainingLimit < 5) {
                        TimeUnit.SECONDS.sleep(timeTillReset);
                        System.out.println("Sleeping for " + timeTillReset + " seconds");
                    }
                } catch (Exception ignored) {
                }

                asyncHttpClient.prepareGet("https://api.ashcon.app/mojang/v2/user/" + guildMemberUuid).execute()
                        .toCompletableFuture().thenApply(uuidToUsernameResponse -> {
                    try {
                        return higherDepth(JsonParser.parseString(uuidToUsernameResponse.getResponseBody()),
                                "username").getAsString();
                    } catch (Exception ignored) {
                    }
                    return null;
                }).thenApply(guildMemberUsernameResponse -> {
                    futures.add(asyncHttpClient
                            .prepareGet("https://api.hypixel.net/skyblock/profiles?key=" + HYPIXEL_API_KEY
                                    + "&uuid=" + guildMemberUuid)
                            .execute().toCompletableFuture().thenApply(guildMemberOuterProfileJsonResponse -> {
                                try {
                                    try {
                                        remainingLimit = Integer.parseInt(guildMemberOuterProfileJsonResponse
                                                .getHeader("RateLimit-Remaining"));
                                        timeTillReset = Integer.parseInt(guildMemberOuterProfileJsonResponse
                                                .getHeader("RateLimit-Reset"));
                                    } catch (Exception ignored) {
                                    }

                                    JsonElement guildMemberOuterProfileJson = JsonParser
                                            .parseString(guildMemberOuterProfileJsonResponse.getResponseBody());
                                    Player guildMemberPlayer = new Player(guildMemberUuid,
                                            guildMemberUsernameResponse, guildMemberOuterProfileJson,
                                            memberRank);
                                    if (guildMemberPlayer.isValid()) {
                                        uniqueGuildUuid.add(guildMemberPlayer.getUuid());
                                        guildSlayer.add(guildMemberPlayer);
                                        guildSkills.add(guildMemberPlayer);
                                        guildCatacombs.add(guildMemberPlayer);
                                        return true;
                                    }
                                } catch (Exception ignored) {
                                }
                                uniqueGuildUuid.add("null");
                                return false;
                            }));
                    return null;
                });
            }
        }

        boolean success = true;
        try {
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).get();
            for(CompletableFuture<?> future:futures){
                System.out.println(future + " - " + future.isDone());
            }
            System.out.println(futures.size());
        } catch (Exception e) {
            success = false;
        }

        guildSlayer.sort(Comparator.comparingInt(Player::getSlayer));
        Collections.reverse(guildSlayer);

        guildSkills.sort(Comparator.comparingDouble(Player::getSkillAverage));
        Collections.reverse(guildSkills);

        guildCatacombs.sort(Comparator.comparingDouble(Player::getCatacombsLevel));
        Collections.reverse(guildCatacombs);

        for (String s : uniqueGuildUuid) {
            if (s.equals("null")) {
                continue;
            }

            int slayerRank = -1;
            int skillsRank = -1;
            int catacombsRank = -1;

            for (int j = 0; j < guildSlayer.size(); j++) {
                try {
                    if (s.equals(guildSlayer.get(j).getUuid())) {
                        slayerRank = j;
                        break;
                    }
                } catch (NullPointerException ignored) {
                }
            }

            for (int j = 0; j < guildSkills.size(); j++) {
                try {
                    if (s.equals(guildSkills.get(j).getUuid())) {
                        skillsRank = j;
                        break;
                    }
                } catch (NullPointerException ignored) {
                }
            }

            for (int j = 0; j < guildCatacombs.size(); j++) {
                try {
                    if (s.equals(guildCatacombs.get(j).getUuid())) {
                        catacombsRank = j;
                        break;
                    }
                } catch (NullPointerException ignored) {
                }
            }

            if (slayerRank < skillsRank) {
                guildSkills.set(skillsRank, null);
                if (slayerRank < catacombsRank) {
                    guildCatacombs.set(catacombsRank, null);
                } else {
                    guildSlayer.set(slayerRank, null);
                }
            } else {
                guildSlayer.set(slayerRank, null);
                if (skillsRank < catacombsRank) {
                    guildCatacombs.set(catacombsRank, null);
                } else {
                    guildSkills.set(skillsRank, null);
                }
            }
        }

        StringBuilder promoteString = new StringBuilder();
        StringBuilder demoteString = new StringBuilder();

        ArrayList<ArrayList<Player>> guildLeaderboards = new ArrayList<>();
        guildLeaderboards.add(guildSlayer);
        guildLeaderboards.add(guildSkills);
        guildLeaderboards.add(guildCatacombs);
        int topRankSize = higherDepth(guildLeaderboardSettings, "top_role.range").getAsInt() - 1;
        int middleRankSize = higherDepth(guildLeaderboardSettings, "middle_role.range").getAsInt() - 1;
        String topRankName = topRoleName.get(0).toLowerCase();
        String middleRankName = middleRoleName.get(0).toLowerCase();
        String defaultRankName = defaultRoleName.get(0).toLowerCase();

        for (ArrayList<Player> currentLeaderboard : guildLeaderboards) {
            for (int i = 0; i < currentLeaderboard.size(); i++) {
                Player currentPlayer = currentLeaderboard.get(i);
                if (currentPlayer == null) {
                    continue;
                }

                String playerRank = currentPlayer.getGuildRank().toLowerCase();
                String playerUsername = currentPlayer.getUsername();
                if (topRoleName.contains(playerRank)) {
                    if (topRoleName.size() > 1) {
                        if (!topRoleName.get(0).equals(playerRank)) {
                            playerRank = topRoleName.get(0);
                        }
                    }
                } else if (middleRoleName.contains(playerRank)) {
                    if (middleRoleName.size() > 1) {
                        if (!middleRoleName.get(0).equals(playerRank)) {
                            playerRank = middleRoleName.get(0);
                        }
                    }
                } else if (defaultRoleName.contains(playerRank)) {
                    if (defaultRoleName.size() > 1) {
                        if (!defaultRoleName.get(0).equals(playerRank)) {
                            playerRank = defaultRoleName.get(0);
                        }
                    }
                }

                if (i <= topRankSize) {
                    if (!playerRank.equals(topRankName)) {
                        promoteString.append("\n- /g setrank ").append(playerUsername).append(" ").append(topRankName);
                    }
                } else if (i <= middleRankSize) {
                    if (!playerRank.equals(middleRankName)) {
                        if (playerRank.equals(topRankName)) {
                            demoteString.append("\n- /g setrank ").append(playerUsername).append(" ")
                                    .append(middleRankName);
                        } else {
                            promoteString.append("\n- /g setrank ").append(playerUsername).append(" ")
                                    .append(middleRankName);
                        }
                    }
                } else {
                    if (!playerRank.equals(defaultRankName)) {
                        demoteString.append("\n- /g setrank ").append(playerUsername).append(" ")
                                .append(defaultRankName);
                    }
                }
            }
        }

        return new String[]{promoteString.toString(), demoteString.toString(), guildName, "" + success};
    }
}