package com.skyblockplus.guilds;

import com.skyblockplus.utils.CustomPaginator;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jdautilities.commons.waiter.EventWaiter;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.exceptions.PermissionException;

import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

import static com.skyblockplus.utils.BotUtils.*;

public class GuildCommands extends Command {
    private final EventWaiter waiter;
    Message ebMessage;

    public GuildCommands(EventWaiter waiter) {
        this.name = "guild";
        this.guildOnly = false;
        this.cooldown = globalCooldown;
        this.waiter = waiter;
    }

    @Override
    protected void execute(CommandEvent event) {
        EmbedBuilder eb = defaultEmbed("Loading guild data...", null);
        this.ebMessage = event.getChannel().sendMessage(eb.build()).complete();

        Message message = event.getMessage();
        String content = message.getContentRaw();

        String[] args = content.split(" ");

        if (content.contains("g-")) {
            if (args.length < 3) {
                eb.setTitle(errorMessage(this.name));
                ebMessage.editMessage(eb.build()).queue();
                return;
            }
        } else if (args.length != 3) {
            eb.setTitle(errorMessage(this.name));
            ebMessage.editMessage(eb.build()).queue();
            return;
        }

        if (content.contains("u-") || content.contains("g-")) {
            if (args[2].endsWith("-")) {
                eb.setTitle(errorMessage(this.name));
                ebMessage.editMessage(eb.build()).queue();
                return;
            }
        }


        for (String value : args) {
            System.out.print(value + " ");
        }
        System.out.println();

        switch (args[1]) {
            case "experience":
            case "exp":  // Experience commands (experience or exp)
                if (args[2].toLowerCase().startsWith("u-")) { // Getting guild info from IGN
                    String username = args[2].split("-")[1];
                    GuildStruct guildExp = getGuildExp(username);
                    if (guildExp.outputArr.length == 0) {
                        eb = guildExp.eb;
                    } else {
                        CustomPaginator.Builder paginateBuilder = new CustomPaginator.Builder().setColumns(2).setItemsPerPage(20)
                                .showPageNumbers(true).useNumberedItems(false).setFinalAction(m -> {
                                    try {
                                        m.clearReactions().queue();
                                    } catch (PermissionException ex) {
                                        m.delete().queue();
                                    }
                                }).setEventWaiter(waiter).setTimeout(30, TimeUnit.SECONDS).wrapPageEnds(true)
                                .setColor(botColor).setCommandUser(event.getAuthor());

                        paginateBuilder.addItems(guildExp.outputArr);

                        ebMessage.delete().queue();
                        paginateBuilder.build().paginate(event.getChannel(), 0);

                        return;
                    }
                } else {
                    eb.setTitle(errorMessage(this.name));
                    ebMessage.editMessage(eb.build()).queue();
                    return;
                }
                break;
            case "player":  // Experience commands (experience or exp)
                String username = args[2];
                eb = getGuildPlayer(username);
                break;
            case "info":
                if (args[2].toLowerCase().startsWith("u-")) {
                    String usernameInfo = args[2].split("-")[1];
                    eb = getGuildInfo(usernameInfo);
                } else if (args[2].toLowerCase().startsWith("g-")) {
                    String guildName = content.split("-")[1];
                    eb = guildInfoFromGuildName(guildName);
                } else {
                    eb.setTitle(errorMessage(this.name));
                    ebMessage.editMessage(eb.build()).queue();
                    return;
                }
                break;
            case "members":
                if (args[2].toLowerCase().startsWith("u-")) {
                    String usernameMembers = args[2].split("-")[1];
                    GuildStruct guildMembers = getGuildMembers(usernameMembers);
                    if (guildMembers.outputArr.length == 0) {
                        eb = guildMembers.eb;
                    } else {
                        CustomPaginator.Builder paginateBuilder = new CustomPaginator.Builder().setColumns(3).setItemsPerPage(27)
                                .showPageNumbers(true).useNumberedItems(false).setFinalAction(m -> {
                                    try {
                                        m.clearReactions().queue();
                                    } catch (PermissionException ex) {
                                        m.delete().queue();
                                    }
                                }).setEventWaiter(waiter).setTimeout(30, TimeUnit.SECONDS).wrapPageEnds(true)
                                .setColor(botColor).setCommandUser(event.getAuthor());

                        paginateBuilder.addItems(guildMembers.outputArr);

                        ebMessage.delete().queue();
                        paginateBuilder.build().paginate(event.getChannel(), 0);
                        return;
                    }

                } else {
                    eb.setTitle(errorMessage(this.name));
                    ebMessage.editMessage(eb.build()).queue();
                    return;
                }
                break;
            default:
                eb.setTitle(errorMessage(this.name));
                ebMessage.editMessage(eb.build()).queue();
                return;
        }

        ebMessage.editMessage(eb.build()).queue();

    }

    public GuildStruct getGuildExp(String username) {
        UsernameUuidStruct uuidUsername = usernameToUuidUsername(username);
        if (uuidUsername == null) {
            return new GuildStruct(defaultEmbed("Error fetching player data", null), null);
        }

        JsonElement guildJson = getJson("https://api.hypixel.net/guild?key=" + key + "&player=" + uuidUsername.playerUuid);
        if (guildJson == null) {
            return new GuildStruct(defaultEmbed("Error fetching guild data", null), null);
        }

        JsonElement members = higherDepth(higherDepth(guildJson, "guild"), "members");
        JsonArray membersArr = members.getAsJsonArray();
        Map<Integer, String> guildExpMap = new HashMap<>();

        for (int i = 0; i < membersArr.size(); i++) {
            String expHistory = higherDepth(membersArr.get(i), "expHistory").toString();
            String[] playerExpArr = expHistory.substring(1, expHistory.length() - 1).split(",");
            int totalPlayerExp = 0;

            for (String value : playerExpArr) {
                totalPlayerExp += Integer.parseInt(value.split(":")[1]);
            }

            String currentUsername = uuidToUsername(higherDepth(membersArr.get(i), "uuid").getAsString());
            if (currentUsername == null) {
                return new GuildStruct(defaultEmbed("Error fetching guild player data", null), null);
            }
            guildExpMap.put(totalPlayerExp, currentUsername);
        }

        Map<Integer, String> guildExpTreeMap = new TreeMap<>(guildExpMap).descendingMap();

        int counter = 0;
        String[] outputStrArr = new String[guildExpMap.size()];
        for (Map.Entry<Integer, String> entry : guildExpTreeMap.entrySet()) {
            Integer exp = entry.getKey();
            String user = entry.getValue();
            outputStrArr[counter] = "**" + (counter + 1) + ")** " + fixUsername(user) + ": " + formatNumber(exp)
                    + " EXP\n";
            counter++;
        }

        return new GuildStruct(null, outputStrArr);
    }

    public EmbedBuilder getGuildPlayer(String username) {
        UsernameUuidStruct uuidUsername = usernameToUuidUsername(username);
        if (uuidUsername == null) {
            return defaultEmbed("Error fetching player data", null);
        }

        JsonElement guildJson = getJson("https://api.hypixel.net/guild?key=" + key + "&player=" + uuidUsername.playerUuid);
        if (guildJson == null) {
            return defaultEmbed("Error fetching guild data", null);
        }

        try {
            String guildName = higherDepth(higherDepth(guildJson, "guild"), "name").getAsString();
            EmbedBuilder eb = defaultEmbed(uuidUsername.playerUsername + " is in " + guildName, null);
            eb.addField("Guild statistics:", getGuildInfo(guildJson), false);
            return eb;
        } catch (Exception e) {
            return defaultEmbed(uuidUsername.playerUsername + " is not in a guild", null);
        }

    }

    public EmbedBuilder getGuildInfo(String username) {
        UsernameUuidStruct uuidUsername = usernameToUuidUsername(username);
        if (uuidUsername == null) {
            return defaultEmbed("Error fetching player data", null);
        }

        JsonElement guildJson = getJson("https://api.hypixel.net/guild?key=" + key + "&player=" + uuidUsername.playerUuid);
        if (guildJson == null) {
            return defaultEmbed("Error fetching guild data", null);
        }

        String guildName = higherDepth(higherDepth(guildJson, "guild"), "name").getAsString();

        EmbedBuilder eb = defaultEmbed(guildName + " information", null);
        eb.addField("Guild statistics:", getGuildInfo(guildJson), false);

        return eb;
    }

    public EmbedBuilder guildInfoFromGuildName(String guildName) {
        try {
            String guildId = higherDepth(getJson("https://api.hypixel.net/findGuild?key=" + key + "&byName=" + guildName.replace(" ", "%20")), "guild").getAsString();
            JsonElement guildJson = getJson("https://api.hypixel.net/guild?key=" + key + "&id=" + guildId);
            if (guildJson == null) {
                return defaultEmbed("Error fetching guild data", null);
            }
            guildName = higherDepth(higherDepth(guildJson, "guild"), "name").getAsString();

            EmbedBuilder eb = defaultEmbed(guildName + " information", null);
            eb.addField("Guild statistics:", getGuildInfo(guildJson), false);
            return eb;
        } catch (Exception e) {
            return defaultEmbed("Error fetching guild data", null);
        }
    }

    public String getGuildInfo(JsonElement guildJson) {
        String guildInfo = "";
        String guildName = higherDepth(higherDepth(guildJson, "guild"), "name").getAsString();

        JsonElement created = higherDepth(higherDepth(guildJson, "guild"), "created");
        String[] date = Date.from(Instant.ofEpochMilli(created.getAsLong())).toString().split(" ");
        guildInfo += ("• " + guildName + " was created on " + date[1] + " " + date[2] + ", " + date[5]) + "\n";

        JsonArray guildMembers = higherDepth(higherDepth(guildJson, "guild"), "members").getAsJsonArray();
        for (int i = 0; i < guildMembers.size(); i++) {
            JsonElement currentMember = guildMembers.get(i).getAsJsonObject();
            if (higherDepth(currentMember, "rank").getAsString().equals("Guild Master")) {
                guildInfo += ("• " + guildName + "'s guild master is "
                        + uuidToUsername(higherDepth(currentMember, "uuid").getAsString())) + "\n";
                break;
            }
        }

        int numGuildMembers = higherDepth(higherDepth(guildJson, "guild"), "members").getAsJsonArray().size();
        guildInfo += ("• " + guildName + " has " + numGuildMembers + " members") + "\n";
        JsonArray preferredGames;
        try {
            preferredGames = higherDepth(higherDepth(guildJson, "guild"), "preferredGames").getAsJsonArray();
        } catch (Exception e) {
            preferredGames = new JsonArray();
        }
        if (preferredGames.size() > 1) {
            String prefString = preferredGames.toString();
            prefString = prefString.substring(1, prefString.length() - 1).toLowerCase().replace("\"", "").replace(",",
                    ", ");
            String firstHalf = prefString.substring(0, prefString.lastIndexOf(","));
            String lastHalf = prefString.substring(prefString.lastIndexOf(",") + 1);
            if (preferredGames.size() > 2) {
                guildInfo += ("• " + guildName + "'s preferred games are " + firstHalf + ", and" + lastHalf) + "\n";
            } else {
                guildInfo += ("• " + guildName + "'s preferred games are " + firstHalf + " and" + lastHalf) + "\n";
            }
        } else if (preferredGames.size() == 1) {
            guildInfo += ("• " + guildName + "'s preferred game is "
                    + preferredGames.get(0).getAsString().toLowerCase()) + "\n";
        }

        int guildExp = higherDepth(higherDepth(guildJson, "guild"), "exp").getAsInt();
        guildInfo += ("• " + guildName + " has a total of " + formatNumber(guildExp) + " exp") + "\n";

        return guildInfo;
    }

    public GuildStruct getGuildMembers(String username) {
        UsernameUuidStruct uuidUsername = usernameToUuidUsername(username);
        if (uuidUsername == null) {
            return new GuildStruct(defaultEmbed("Error fetching player data", null), null);
        }

        JsonElement guildJson = getJson("https://api.hypixel.net/guild?key=" + key + "&player=" + uuidUsername.playerUuid);
        if (guildJson == null) {
            return new GuildStruct(defaultEmbed("Error fetching guild data", null), null);
        }

        JsonArray guildMembers = higherDepth(higherDepth(guildJson, "guild"), "members").getAsJsonArray();
        String[] members = new String[guildMembers.size()];
        for (int i = 0; i < guildMembers.size(); i++) {
            String currentMember = uuidToUsername(
                    higherDepth(guildMembers.get(i).getAsJsonObject(), "uuid").getAsString());
            members[i] = "• " + fixUsername(currentMember) + "  \n";
        }

        return new GuildStruct(null, members);
    }

}
