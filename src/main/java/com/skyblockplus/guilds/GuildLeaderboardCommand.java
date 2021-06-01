package com.skyblockplus.guilds;

import static com.skyblockplus.Main.waiter;
import static com.skyblockplus.utils.Utils.HYPIXEL_API_KEY;
import static com.skyblockplus.utils.Utils.defaultEmbed;
import static com.skyblockplus.utils.Utils.defaultPaginator;
import static com.skyblockplus.utils.Utils.errorMessage;
import static com.skyblockplus.utils.Utils.fixUsername;
import static com.skyblockplus.utils.Utils.getJson;
import static com.skyblockplus.utils.Utils.globalCooldown;
import static com.skyblockplus.utils.Utils.higherDepth;
import static com.skyblockplus.utils.Utils.loadingEmbed;
import static com.skyblockplus.utils.Utils.logCommand;
import static com.skyblockplus.utils.Utils.usernameToUuid;

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
import com.skyblockplus.utils.CustomPaginator;
import com.skyblockplus.utils.structs.PaginatorExtras;
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
                eb = getLeaderboard(args[1].split(":")[1], event);

                if (eb == null) {
                    ebMessage.delete().queue();
                } else {
                    ebMessage.editMessage(eb.build()).queue();
                }
                return;
            }

            ebMessage.editMessage(errorMessage(this.name).build()).queue();
        }).start();
    }

    private EmbedBuilder getLeaderboard(String username, CommandEvent event) {
        UsernameUuidStruct usernameUuidStruct = usernameToUuid(username);
        if (usernameUuidStruct == null) {
            return null;
        }

        JsonElement guildJson = getJson(
                "https://api.hypixel.net/guild?key=" + HYPIXEL_API_KEY + "&player=" + usernameUuidStruct.playerUuid);
        String guildId = higherDepth(guildJson, "guild._id").getAsString();
        String guildName = higherDepth(guildJson, "guild.name").getAsString();
        if (!guildName.equals("Skyblock Forceful") && !guildName.equals("Skyblock Gods")) {
            return defaultEmbed("Only for SBF or SBG right now");
        }

        List<String> staffRankNames = new ArrayList<>();
        JsonElement lbSettings;
        List<String> rankTypes = new ArrayList<>();

        try {
            lbSettings = higherDepth(
                    JsonParser.parseReader(new FileReader("src/main/java/com/skyblockplus/json/GuildSettings.json")),
                    guildId + ".guild_leaderboard");

            for (JsonElement i : higherDepth(lbSettings, "staff_ranks").getAsJsonArray()) {
                staffRankNames.add(i.getAsString().toLowerCase());
            }

            for (JsonElement i : higherDepth(lbSettings, "types").getAsJsonArray()) {
                rankTypes.add(i.getAsString().toLowerCase());
            }

        } catch (Exception e) {
            e.printStackTrace();
            return defaultEmbed("Error getting data");
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
                        higherDepth(lbM, "weight").getAsDouble(), curRank));
                uniqueGuildUuid.add(higherDepth(lbM, "username").getAsString());
            }
        }

        gMembers.sort(Comparator.comparingDouble(o1 -> -o1.getSlayer()));
        ArrayList<GuildLeaderboardStruct> guildSlayer = new ArrayList<>(gMembers);

        gMembers.sort(Comparator.comparingDouble(o1 -> -o1.getSkills()));
        ArrayList<GuildLeaderboardStruct> guildSkills = new ArrayList<>(gMembers);

        gMembers.sort(Comparator.comparingDouble(o1 -> -o1.getCatacombs()));
        ArrayList<GuildLeaderboardStruct> guildCatacombs = new ArrayList<>(gMembers);

        gMembers.sort(Comparator.comparingDouble(o1 -> -o1.getWeight()));
        ArrayList<GuildLeaderboardStruct> guildWeight = new ArrayList<>(gMembers);

        for (String s : uniqueGuildUuid) {
            int slayerRank = -1;
            int skillsRank = -1;
            int catacombsRank = -1;
            int weightRank = -1;

            if (rankTypes.contains("slayer")) {
                for (int j = 0; j < guildSlayer.size(); j++) {
                    try {
                        if (s.equals(guildSlayer.get(j).getName())) {
                            slayerRank = j;
                            break;
                        }
                    } catch (NullPointerException ignored) {
                    }
                }
            }

            if (rankTypes.contains("skills")) {
                for (int j = 0; j < guildSkills.size(); j++) {
                    try {
                        if (s.equals(guildSkills.get(j).getName())) {
                            skillsRank = j;
                            break;
                        }
                    } catch (NullPointerException ignored) {
                    }
                }
            }

            if (rankTypes.contains("catacombs")) {
                for (int j = 0; j < guildCatacombs.size(); j++) {
                    try {
                        if (s.equals(guildCatacombs.get(j).getName())) {
                            catacombsRank = j;
                            break;
                        }
                    } catch (NullPointerException ignored) {
                    }
                }
            }

            if (rankTypes.contains("weight")) {
                for (int j = 0; j < guildWeight.size(); j++) {
                    try {
                        if (s.equals(guildWeight.get(j).getName())) {
                            weightRank = j;
                            break;
                        }
                    } catch (NullPointerException ignored) {
                    }
                }
            }

            if (guildName.equals("Skyblock Forceful")) {
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
        }

        ArrayList<ArrayList<GuildLeaderboardStruct>> guildLeaderboards = new ArrayList<>();

        if (rankTypes.contains("slayer")) {
            guildLeaderboards.add(guildSlayer);
        }
        if (rankTypes.contains("skills")) {
            guildLeaderboards.add(guildSkills);
        }
        if (rankTypes.contains("catacombs")) {
            guildLeaderboards.add(guildCatacombs);
        }
        if (rankTypes.contains("weight")) {
            guildLeaderboards.add(guildWeight);
        }
        JsonArray ranksArr = higherDepth(lbSettings, "ranks").getAsJsonArray();

        CustomPaginator.Builder paginateBuilder = defaultPaginator(waiter, event.getAuthor()).setColumns(1)
                .setItemsPerPage(20);
        int totalChange = 0;
        for (ArrayList<GuildLeaderboardStruct> currentLeaderboard : guildLeaderboards) {
            for (int i = 0; i < currentLeaderboard.size(); i++) {
                GuildLeaderboardStruct currentPlayer = currentLeaderboard.get(i);
                if (currentPlayer == null) {
                    continue;
                }

                String playerRank = currentPlayer.getGuildRank().toLowerCase();
                String playerUsername = currentPlayer.getName();

                for (JsonElement rank : ranksArr) {
                    if (i <= higherDepth(rank, "range").getAsInt() - 1) {
                        JsonArray rankNames = higherDepth(rank, "names").getAsJsonArray();
                        List<String> rankNamesList = new ArrayList<>();
                        for (JsonElement rankName : rankNames) {
                            rankNamesList.add(rankName.getAsString());
                        }

                        if (!rankNamesList.contains(playerRank.toLowerCase())) {
                            paginateBuilder.addItems(
                                    ("- /g setrank " + fixUsername(playerUsername) + " " + rankNamesList.get(0)));
                            totalChange++;
                        }
                        break;
                    }
                }

            }
        }

        paginateBuilder
                .setPaginatorExtras(new PaginatorExtras().setEveryPageTitle("Rank changes for " + guildName)
                        .setEveryPageText("**Total rank changes:** " + totalChange))
                .build().paginate(event.getChannel(), 0);

        return null;
    }
}