package com.SkyblockBot.Guilds;

import com.SkyblockBot.Miscellaneous.Player;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;

import java.io.FileReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static com.SkyblockBot.Miscellaneous.BotUtils.*;

public class GuildLeaderboardCommand extends Command {
    Message ebMessage;
    JsonElement settings;

    public GuildLeaderboardCommand() {
        this.name = "guild-rank";
        this.guildOnly = true;
        this.cooldown = 60;
        this.aliases = new String[]{"g-rank"};
    }

    @Override
    protected void execute(CommandEvent event) {
        EmbedBuilder eb = defaultEmbed("Loading guild data...", null);
        eb.setDescription("**NOTE:** This can take up to a minute!");
        this.ebMessage = event.getChannel().sendMessage(eb.build()).complete();

        Message message = event.getMessage();
        String content = message.getContentRaw();
        try {
            this.settings = new JsonParser()
                    .parse(new FileReader("src/main/java/com/SkyblockBot/json/GuildSettings.json"));
        } catch (Exception e) {
            eb = defaultEmbed("Error fetching settings", null);
            ebMessage.editMessage(eb.build()).queue();
            return;
        }
        if (!event.getGuild().getMember(event.getAuthor()).getRoles().contains(event.getGuild().getRoleById(higherDepth(higherDepth(higherDepth(settings, event.getGuild().getId()), "staff_role"), "id").getAsString()))) {
            eb = defaultEmbed("You must have " + event.getGuild().getRoleById(higherDepth(higherDepth(higherDepth(settings, event.getGuild().getId()), "staff_role"), "id").getAsString()).getName() + " role to run this command", null);
            ebMessage.editMessage(eb.build()).queue();
            return;
        }

        String[] args = content.split(" ");
        if (args.length != 2) {
            eb = defaultEmbed(errorMessage(this.name), null);
            ebMessage.editMessage(eb.build()).queue();
            return;
        }

        for (String value : args) {
            System.out.print(value + " ");
        }
        System.out.println();

        if (args[1].toLowerCase().startsWith("u-")) {
            String[] rankString = getLeaderboard(args[1].split("-")[1]);
            if (rankString != null) {
                eb = defaultEmbed("Rank changes for guild " + rankString[2], null);
                eb.addField("Promote", rankString[0], false);
                eb.addField("Demote", rankString[1], false);
            } else {
                eb = defaultEmbed(errorMessage(this.name), null);
                ebMessage.editMessage(eb.build()).queue();
                return;
            }
        } else {
            eb = defaultEmbed(errorMessage(this.name), null);
            ebMessage.editMessage(eb.build()).queue();
            return;
        }

        ebMessage.editMessage(eb.build()).queue();
    }

    public String[] getLeaderboard(String username) {
        String playerUuid = usernameToUuid(username);
        if (playerUuid == null) {
            return null;
        }

        JsonElement guildJson = getJson("https://api.hypixel.net/guild?key=" + key + "&player=" + playerUuid);
        String guildId = higherDepth(higherDepth(guildJson, "guild"), "_id").getAsString();
        String guildName = higherDepth(higherDepth(guildJson, "guild"), "name").getAsString();
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
            guildLeaderboardSettings = higherDepth(higherDepth(this.settings, guildId), "guild_leaderboard");
            for (JsonElement i : higherDepth(guildLeaderboardSettings, "staff_ranks").getAsJsonArray()) {
                staffRankNames.add(i.getAsString());
            }

            for (JsonElement i : higherDepth(higherDepth(guildLeaderboardSettings, "top_role"), "names")
                    .getAsJsonArray()) {
                topRoleName.add(i.getAsString().toLowerCase());
            }
            for (JsonElement i : higherDepth(higherDepth(guildLeaderboardSettings, "middle_role"), "names")
                    .getAsJsonArray()) {
                middleRoleName.add(i.getAsString().toLowerCase());
            }
            for (JsonElement i : higherDepth(higherDepth(guildLeaderboardSettings, "default_role"), "names")
                    .getAsJsonArray()) {
                defaultRoleName.add(i.getAsString().toLowerCase());
            }
        } catch (Exception e) {
            return null;
        }

        JsonElement levelTables = getJson(
                "https://raw.githubusercontent.com/Moulberry/NotEnoughUpdates-REPO/master/constants/leveling.json");
        JsonArray guildMembers = higherDepth(higherDepth(guildJson, "guild"), "members").getAsJsonArray();

        ArrayList<Player> guildSlayer = new ArrayList<>();
        ArrayList<Player> guildSkills = new ArrayList<>();
        ArrayList<Player> guildCatacombs = new ArrayList<>();
        ArrayList<String> uniqueGuildUuid = new ArrayList<>();

        for (JsonElement guildMember : guildMembers) {
            String memberRank = higherDepth(guildMember, "rank").getAsString();
            if (!staffRankNames.contains(memberRank)) {
                Player player = new Player(higherDepth(guildMember, "uuid").getAsString(), levelTables, memberRank);
                if (player.isValidPlayer()) {
                    uniqueGuildUuid.add(player.getUuid());
                    guildSlayer.add(player);
                    guildSkills.add(player);
                    guildCatacombs.add(player);
                }
            }
        }

        guildSlayer.sort(Comparator.comparingInt(Player::getSlayer));
        Collections.reverse(guildSlayer);

        guildSkills.sort(Comparator.comparingDouble(Player::getSkillAverage));
        Collections.reverse(guildSkills);

        guildCatacombs.sort(Comparator.comparingDouble(Player::getCatacombsLevel));
        Collections.reverse(guildCatacombs);

        for (String s : uniqueGuildUuid) {
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
        int topRankSize = higherDepth(higherDepth(guildLeaderboardSettings, "top_role"), "range").getAsInt() - 1;
        int middleRankSize = higherDepth(higherDepth(guildLeaderboardSettings, "middle_role"), "range").getAsInt() - 1;
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
                            demoteString.append("\n- /g setrank ").append(playerUsername).append(" ").append(middleRankName);
                        } else {
                            promoteString.append("\n- /g setrank ").append(playerUsername).append(" ").append(middleRankName);
                        }
                    }
                } else {
                    if (!playerRank.equals(defaultRankName)) {
                        demoteString.append("\n- /g setrank ").append(playerUsername).append(" ").append(defaultRankName);
                    }
                }
            }
        }

        return new String[]{promoteString.toString(), demoteString.toString(), guildName};
    }
}