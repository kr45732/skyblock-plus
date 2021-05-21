package com.skyblockplus.guilds;

import static com.skyblockplus.utils.Utils.*;

import java.io.FileReader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.skyblockplus.utils.structs.UsernameUuidStruct;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;

public class GuildLeaderboardCommand extends Command {

    public GuildLeaderboardCommand() {
        this.name = "guild-rank";
        this.cooldown = globalCooldown;
        this.aliases = new String[] { "g-rank" };
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

            if (args[1].toLowerCase().startsWith("u:")) {
                String[] rankString = getLeaderboard(args[1].split(":")[1]);
                if (rankString != null) {
                    eb = defaultEmbed("Rank changes for guild " + rankString[2]);
                    eb.addField("Promote", rankString[0], false);
                    eb.addField("Demote", rankString[1], false);
                    ebMessage.editMessage(eb.build()).queue();
                    return;
                }
            }

            ebMessage.editMessage(errorMessage(this.name).build()).queue();
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
            return new String[] { "Currently only supported for the Skyblock Forceful guild",
                    "Currently only supported for the Skyblock Forceful guild", "" };
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
                staffRankNames.add(i.getAsString().toLowerCase());
            }

            for (JsonElement i : higherDepth(guildLeaderboardSettings, "top_role.names").getAsJsonArray()) {
                topRoleName.add(i.getAsString().toLowerCase());
            }
            for (JsonElement i : higherDepth(guildLeaderboardSettings, "middle_role.names").getAsJsonArray()) {
                middleRoleName.add(i.getAsString().toLowerCase());
            }
            for (JsonElement i : higherDepth(guildLeaderboardSettings, "default_role.names").getAsJsonArray()) {
                defaultRoleName.add(i.getAsString().toLowerCase());
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        JsonArray guildMembers = higherDepth(guildJson, "guild.members").getAsJsonArray();
        Map<String, String> ranksMap = new HashMap<>();
        for (JsonElement guildM : guildMembers) {
            ranksMap.put(higherDepth(guildM, "uuid").getAsString(),
                    higherDepth(guildM, "rank").getAsString().toLowerCase());
        }

        List<String> uniqueGuildUuid = new ArrayList<>();
        List<GuildLeaderboardStruct> gMembers = new ArrayList<>();
        JsonArray guildLbJson = higherDepth(
                getJson("https://hypixel-app-api.senither.com/leaderboard/players/" + guildId), "data")
                        .getAsJsonArray();
        for (JsonElement lbM : guildLbJson) {
            String lbUuid = higherDepth(lbM, "uuid").getAsString().replace("-", "");
            String curRank = ranksMap.get(lbUuid);

            if (curRank != null && !staffRankNames.contains(curRank)) {
                gMembers.add(new GuildLeaderboardStruct(higherDepth(lbM, "username").getAsString(),
                        higherDepth(lbM, "average_skill_progress").getAsDouble(),
                        higherDepth(lbM, "total_slayer").getAsDouble(), higherDepth(lbM, "catacomb").getAsDouble(),
                        curRank));
                uniqueGuildUuid.add(higherDepth(lbM, "username").getAsString());
            }
        }

        gMembers.sort(Comparator.comparingDouble(o1 -> -o1.getSlayer()));
        ArrayList<GuildLeaderboardStruct> guildSlayer = new ArrayList<>(gMembers);

        gMembers.sort(Comparator.comparingDouble(o1 -> -o1.getSkills()));
        ArrayList<GuildLeaderboardStruct> guildSkills = new ArrayList<>(gMembers);

        gMembers.sort(Comparator.comparingDouble(o1 -> -o1.getCatacombs()));
        ArrayList<GuildLeaderboardStruct> guildCatacombs = new ArrayList<>(gMembers);

        for (String s : uniqueGuildUuid) {
            int slayerRank = -1;
            int skillsRank = -1;
            int catacombsRank = -1;

            for (int j = 0; j < guildSlayer.size(); j++) {
                try {
                    if (s.equals(guildSlayer.get(j).getName())) {
                        slayerRank = j;
                        break;
                    }
                } catch (NullPointerException ignored) {
                }
            }

            for (int j = 0; j < guildSkills.size(); j++) {
                try {
                    if (s.equals(guildSkills.get(j).getName())) {
                        skillsRank = j;
                        break;
                    }
                } catch (NullPointerException ignored) {
                }
            }

            for (int j = 0; j < guildCatacombs.size(); j++) {
                try {
                    if (s.equals(guildCatacombs.get(j).getName())) {
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

        ArrayList<ArrayList<GuildLeaderboardStruct>> guildLeaderboards = new ArrayList<>();
        guildLeaderboards.add(guildSlayer);
        guildLeaderboards.add(guildSkills);
        guildLeaderboards.add(guildCatacombs);
        int topRankSize = higherDepth(guildLeaderboardSettings, "top_role.range").getAsInt() - 1;
        int middleRankSize = higherDepth(guildLeaderboardSettings, "middle_role.range").getAsInt() - 1;
        String topRankName = topRoleName.get(0).toLowerCase();
        String middleRankName = middleRoleName.get(0).toLowerCase();
        String defaultRankName = defaultRoleName.get(0).toLowerCase();

        for (ArrayList<GuildLeaderboardStruct> currentLeaderboard : guildLeaderboards) {
            for (int i = 0; i < currentLeaderboard.size(); i++) {
                GuildLeaderboardStruct currentPlayer = currentLeaderboard.get(i);
                if (currentPlayer == null) {
                    continue;
                }

                String playerRank = currentPlayer.getGuildRank().toLowerCase();
                String playerUsername = currentPlayer.getName();
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

        return new String[] { promoteString.toString(), demoteString.toString(), guildName };
    }
}