package com.SkyblockBot.Guild;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jdautilities.commons.waiter.EventWaiter;
import com.jagrosh.jdautilities.menu.Paginator;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.exceptions.PermissionException;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static com.SkyblockBot.Miscellaneous.BotUtils.*;

public class GuildCommands extends Command {
    private final EventWaiter waiter;

    public GuildCommands(EventWaiter waiter) {
        this.name = "guild";
        this.guildOnly = false;
        this.cooldown = 5;
        this.waiter = waiter;
    }

    @Override
    protected void execute(CommandEvent event) {
        final EmbedBuilder[] eb = { defaultEmbed("Loading guild data...", null) };

        Message message = event.getMessage();
        String content = message.getContentRaw();

        String[] args = content.split(" ");
        if (args.length != 3) { // No args or too many args are given
            eb[0].setTitle("Invalid input. Type !help for help");
            event.reply(eb[0].build(), m -> m.editMessage(eb[0].build()).queue());
            return;
        }

        for (String value : args) {
            System.out.print(value + " ");
        }
        System.out.println();

        if (args[1].equals("experience") || args[1].equals("exp")) { // Experience commands (experience or exp)
            if (args[2].toLowerCase().startsWith("u-")) { // Getting guild info from IGN
                String username = args[2].split("-")[1];
                GuildStruct guildExp = getGuildExp(username);
                if (guildExp.outputArr.length == 0) {
                    eb[0] = guildExp.eb;
                } else {
                    Paginator.Builder pbuilder = new Paginator.Builder().setColumns(2).setItemsPerPage(20)
                            .showPageNumbers(true).waitOnSinglePage(false).useNumberedItems(false).setFinalAction(m -> {
                                try {
                                    m.clearReactions().queue();
                                } catch (PermissionException ex) {
                                    m.delete().queue();
                                }
                            }).setEventWaiter(waiter).setTimeout(30, TimeUnit.SECONDS).wrapPageEnds(true)
                            .setColor(botColor);

                    pbuilder.setText(" ");
                    pbuilder.addItems(guildExp.outputArr);

                    event.reply("", m -> m.delete().queue());

                    pbuilder.build().paginate(event.getChannel(), 0);

                    return;
                }
            } else if (args[2].toLowerCase().startsWith("g-")) {
            } else {
                eb[0].setTitle("Invalid input. Type !help for help");
                event.reply(eb[0].build(), m -> m.editMessage(eb[0].build()).queue());
                return;
            }
        } else if (args[1].equals("player")) { // Experience commands (experience or exp)
            String username = args[2];
            eb[0] = getGuildPlayer(username);
        } else if (args[1].equals("info")) {
            if (args[2].toLowerCase().startsWith("u-")) {
                String username = args[2].split("-")[1];
                eb[0] = getGuildInfo(username);
            } else if (args[2].toLowerCase().startsWith("g-")) {
            } else {
                eb[0].setTitle("Invalid input. Type !help for help");
                event.reply(eb[0].build(), m -> m.editMessage(eb[0].build()).queue());
                return;
            }
        } else if (args[1].equals("members")) {
            if (args[2].toLowerCase().startsWith("u-")) {
                String username = args[2].split("-")[1];
                GuildStruct guildMembers = getGuildMembers(username);
                if (guildMembers.outputArr.length == 0) {
                    eb[0] = guildMembers.eb;
                } else {
                    Paginator.Builder pbuilder = new Paginator.Builder().setColumns(3).setItemsPerPage(27)
                            .showPageNumbers(true).waitOnSinglePage(false).useNumberedItems(false).setFinalAction(m -> {
                                try {
                                    m.clearReactions().queue();
                                } catch (PermissionException ex) {
                                    m.delete().queue();
                                }
                            }).setEventWaiter(waiter).setTimeout(30, TimeUnit.SECONDS).wrapPageEnds(true)
                            .setColor(botColor);

                    pbuilder.setText(" ");
                    pbuilder.addItems(guildMembers.outputArr);
                    pbuilder.build().paginate(event.getChannel(), 0);
                    return;
                }

            } else if (args[2].toLowerCase().startsWith("g-")) {
            } else {
                eb[0].setTitle("Invalid input. Type !help for help");
                event.reply(eb[0].build(), m -> m.editMessage(eb[0].build()).queue());
                return;
            }
        } else {
            eb[0].setTitle("Invalid input. Type !help for help");
            event.reply(eb[0].build(), m -> m.editMessage(eb[0].build()).queue());
            return;
        }

        event.reply(eb[0].build(), m -> m.editMessage(eb[0].build()).queue());

    }

    public GuildStruct getGuildExp(String username) {
        if (getJson("https://api.mojang.com/users/profiles/minecraft/" + username).isJsonNull()) {
            return new GuildStruct(defaultEmbed("Error fetching player data", null), new String[0]);
        }

        JsonElement jsonProfile = getJson("https://api.mojang.com/users/profiles/minecraft/" + username);

        String uuidPlayer = higherDepth(jsonProfile, "id").getAsString();
        String guildUrl = "https://api.hypixel.net/guild?key=" + key + "&player=" + uuidPlayer;

        JsonElement guildJson = getJson(guildUrl);
        if (guildJson == null) {
            return new GuildStruct(defaultEmbed("Error fetching guild data", null), new String[0]);
        }

        JsonElement members = higherDepth(higherDepth(guildJson, "guild"), "members");
        JsonArray membersArr = members.getAsJsonArray();
        Map<Integer, String> guildExpMap = new HashMap<>();

        for (int i = 0; i < membersArr.size(); i++) {
            String expHistory = (higherDepth(membersArr.get(i), "expHistory")).toString();
            String[] playerExpArr = expHistory.substring(1, expHistory.length() - 1).split(",");
            int totalPlayerExp = 0;

            for (String value : playerExpArr) {
                totalPlayerExp += Integer.parseInt(value.split(":")[1]);
            }

            String usernameFromUuid = uuidToUsername(higherDepth(membersArr.get(i), "uuid").getAsString());
            if (usernameFromUuid == null) {
                return new GuildStruct(defaultEmbed("Error fetching guild player data", null), new String[0]);
            }
            String currentUser = usernameFromUuid;
            guildExpMap.put(totalPlayerExp, currentUser);
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
        if (getJson("https://api.mojang.com/users/profiles/minecraft/" + username).isJsonNull()) {
            return defaultEmbed("Error fetching player data", null);
        }

        JsonElement jsonProfile = getJson("https://api.mojang.com/users/profiles/minecraft/" + username);

        String uuidPlayer = higherDepth(jsonProfile, "id").getAsString();
        String guildUrl = "https://api.hypixel.net/guild?key=" + key + "&player=" + uuidPlayer;

        JsonElement guildJson = getJson(guildUrl);
        if (guildJson == null) {
            return defaultEmbed("Error fetching guild data", null);
        }

        String guildName = higherDepth(higherDepth(guildJson, "guild"), "name").getAsString();

        EmbedBuilder eb = defaultEmbed(username + " is in " + guildName, null);
        eb.addField("Guild statistics:", getGuildInfo(guildJson), false);

        return eb;
    }

    public EmbedBuilder getGuildInfo(String username) {
        if (getJson("https://api.mojang.com/users/profiles/minecraft/" + username).isJsonNull()) {
            return defaultEmbed("Error fetching player data", null);
        }

        JsonElement jsonProfile = getJson("https://api.mojang.com/users/profiles/minecraft/" + username);

        String uuidPlayer = higherDepth(jsonProfile, "id").getAsString();
        String guildUrl = "https://api.hypixel.net/guild?key=" + key + "&player=" + uuidPlayer;

        JsonElement guildJson = getJson(guildUrl);
        if (guildJson == null) {
            return defaultEmbed("Error fetching guild data", null);
        }

        String guildName = higherDepth(higherDepth(guildJson, "guild"), "name").getAsString();

        EmbedBuilder eb = defaultEmbed(guildName + " information", null);
        eb.addField("Guild statistics:", getGuildInfo(guildJson), false);

        return eb;
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

        JsonArray preferredGames = higherDepth(higherDepth(guildJson, "guild"), "preferredGames").getAsJsonArray();
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
        if (getJson("https://api.mojang.com/users/profiles/minecraft/" + username).isJsonNull()) {
            return new GuildStruct(defaultEmbed("Error fetching player data", null), null);
        }

        JsonElement jsonProfile = getJson("https://api.mojang.com/users/profiles/minecraft/" + username);

        String uuidPlayer = higherDepth(jsonProfile, "id").getAsString();
        String guildUrl = "https://api.hypixel.net/guild?key=" + key + "&player=" + uuidPlayer;

        JsonElement guildJson = getJson(guildUrl);
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
