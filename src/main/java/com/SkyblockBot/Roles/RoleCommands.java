package com.SkyblockBot.Roles;

import static com.SkyblockBot.Miscellaneous.BotUtils.*;
import static com.SkyblockBot.Skills.SkillsCommands.skillInfoFromExp;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import com.SkyblockBot.Miscellaneous.LatestProfileStruct;
import com.SkyblockBot.Skills.SkillsStruct;
import com.google.gson.JsonElement;
import com.google.gson.JsonIOException;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;

public class RoleCommands extends Command {
    Message ebMessage;
    String profileId = "";

    public RoleCommands() {
        this.name = "roles";
        this.cooldown = 30;
    }

    @Override
    protected void execute(CommandEvent event) {
        EmbedBuilder eb = defaultEmbed("Loading roles data...", null);
        eb.setDescription("**NOTE: This may take some time**");

        String content = event.getMessage().getContentRaw();
        MessageChannel channel = event.getChannel();
        Guild guild = event.getGuild();
        User user = event.getAuthor();
        this.ebMessage = channel.sendMessage(eb.build()).complete();

        String[] args = content.split(" ");
        System.out.println(args.length);
        if (args.length < 3 || args.length > 4) {
            eb.setTitle(errorMessage(this.name));
            eb.setDescription("");
            ebMessage.editMessage(eb.build()).queue();
            return;
        }

        for (String value : args) {
            System.out.print(value + " ");
        }
        System.out.println();

        if (getPlayerInfo(args[2]) == null) {
            eb = defaultEmbed("Discord tag mismatch", null);
            eb.setDescription("Unable to get Discord tag linked with Hypixel account");
            ebMessage.editMessage(eb.build()).queue();
            return;
        }
        DiscordStruct playerInfo = getPlayerInfo(args[2]);

        if (!user.getAsTag().equals(playerInfo.discordTag)) {
            eb = defaultEmbed("Discord tag mismatch", null);
            eb.setDescription("Account " + playerInfo.username + " is linked with the discord tag `"
                    + playerInfo.discordTag + "`\nYour current discord tag is `" + user.getAsTag() + "`");
            ebMessage.editMessage(eb.build()).queue();
            return;
        }

        String username = playerInfo.username;
        String profile = args.length == 4 ? args[3] : null;

        if (!checkValid(username, profile)) {
            eb = defaultEmbed("Error fetching data", null);
            eb.setDescription("**Please check given username and profile**");
            ebMessage.editMessage(eb.build()).queue();
            return;
        }

        if (args[1].equals("claim")) {
            try {
                eb = defaultEmbed("Automatic roles for " + username, skyblockStatsLink(username, profile));
                String addedRoles = "";
                String removedRoles = "";
                String errorRoles = "";
                JsonElement settings = new JsonParser()
                        .parse(new FileReader("src/main/java/com/SkyblockBot/json/GuildSettings.json"));
                if (higherDepth(settings, guild.getId()) != null) {
                    JsonElement rolesJson = higherDepth(higherDepth(settings, guild.getId()), "automated_roles");
                    if (!higherDepth(rolesJson, "enable").getAsBoolean()) {
                        eb = defaultEmbed("Automatic roles not enabled for this server", null);
                        ebMessage.editMessage(eb.build()).queue();
                        return;
                    }
                    List<String> rolesID = rolesJson.getAsJsonObject().entrySet().stream().map(i -> i.getKey())
                            .collect(Collectors.toCollection(ArrayList::new));

                    for (String currentRoleName : rolesID) {
                        JsonElement currentRole = higherDepth(rolesJson, currentRoleName);
                        if (currentRoleName.equals("guild_member") || currentRoleName.equals("related_guild_member")) {
                            JsonElement guildJson = getJson("https://api.hypixel.net/findGuild?key=" + key + "&byUuid="
                                    + usernameToUuid(username));
                            if (guildJson != null) {
                                Role curRole = guild.getRoleById(higherDepth(currentRole, "id").getAsString());
                                if (!higherDepth(guildJson, "guild").isJsonNull()) {
                                    if (higherDepth(currentRole, "guild_id").getAsString()
                                            .equals(higherDepth(guildJson, "guild").getAsString())) {
                                        if (!guild.getMember(user).getRoles().contains(curRole)) {
                                            guild.addRoleToMember(guild.getMember(user), curRole).queue();

                                            addedRoles += roleChangeString(
                                                    higherDepth(currentRole, "name").getAsString());
                                        }
                                    } else {
                                        if (guild.getMember(user).getRoles().contains(curRole)) {
                                            removedRoles += roleChangeString(
                                                    higherDepth(currentRole, "name").getAsString());
                                            guild.removeRoleFromMember(guild.getMember(user), curRole).queue();
                                        }
                                    }
                                }
                            }
                        } else if (currentRoleName.equals("sven") || currentRoleName.equals("rev")
                                || currentRoleName.equals("tara")) {
                            String slayerPlayer = getPlayerSlayer(username);
                            if (slayerPlayer == null && !errorRoles.contains("Slayer")) {
                                errorRoles += roleChangeString("Slayer API disabled");
                            }

                            if (slayerPlayer.length() > 0) {
                                String svenExp;
                                if (currentRoleName.equals("sven")) {
                                    svenExp = slayerPlayer.split(" ")[0];
                                } else if (currentRoleName.equals("rev")) {
                                    svenExp = slayerPlayer.split(" ")[1];
                                } else {
                                    svenExp = slayerPlayer.split(" ")[2];
                                }

                                List<Integer> svenLevels = currentRole.getAsJsonObject().entrySet().stream()
                                        .map(i -> Integer.parseInt(i.getKey()))
                                        .collect(Collectors.toCollection(ArrayList::new));
                                Collections.sort(svenLevels);
                                Collections.reverse(svenLevels);
                                for (int i = 0; i < svenLevels.size(); i++) {
                                    Long levelExp = higherDepth(higherDepth(currentRole, "" + svenLevels.get(i)), "xp")
                                            .getAsLong();
                                    if (Integer.parseInt(svenExp) >= levelExp) {
                                        for (int j = i; j < svenLevels.size(); j++) {
                                            Role curRole = guild.getRoleById(
                                                    higherDepth(higherDepth(currentRole, "" + svenLevels.get(j)), "id")
                                                            .getAsString());
                                            if (guild.getMember(user).getRoles().contains(curRole)) {
                                                continue;
                                            }
                                            guild.addRoleToMember(guild.getMember(user), curRole).queue();

                                            addedRoles += roleChangeString(
                                                    higherDepth(higherDepth(currentRole, "" + svenLevels.get(j)),
                                                            "name").getAsString());

                                        }
                                        break;
                                    } else {
                                        Role curRole = guild.getRoleById(
                                                higherDepth(higherDepth(currentRole, "" + svenLevels.get(i)), "id")
                                                        .getAsString());
                                        if (guild.getMember(user).getRoles().contains(curRole)) {
                                            removedRoles += roleChangeString(
                                                    higherDepth(higherDepth(currentRole, "" + svenLevels.get(i)),
                                                            "name").getAsString());
                                            guild.removeRoleFromMember(guild.getMember(user), curRole).queue();
                                        }
                                    }
                                }
                            }
                        } else if (currentRoleName.equals("bank_coins")) {
                            double playerBankCoins = getBankCoins(username);
                            if (playerBankCoins == -1 || playerBankCoins == -2) {
                                if (playerBankCoins == -2 && !errorRoles.contains("Banking")) {
                                    errorRoles += roleChangeString("Banking API disabled");
                                }
                            }

                            List<Integer> bankLevels = currentRole.getAsJsonObject().entrySet().stream()
                                    .map(i -> Integer.parseInt(i.getKey()))
                                    .collect(Collectors.toCollection(ArrayList::new));
                            Collections.sort(bankLevels);
                            Collections.reverse(bankLevels);
                            for (int i = 0; i < bankLevels.size(); i++) {
                                Long levelCoins = higherDepth(higherDepth(currentRole, "" + bankLevels.get(i)), "coins")
                                        .getAsLong();
                                if (playerBankCoins >= levelCoins) {
                                    for (int j = i; j < bankLevels.size(); j++) {
                                        Role curRole = guild.getRoleById(
                                                higherDepth(higherDepth(currentRole, "" + bankLevels.get(j)), "id")
                                                        .getAsString());
                                        if (guild.getMember(user).getRoles().contains(curRole)) {
                                            continue;
                                        }
                                        guild.addRoleToMember(guild.getMember(user), curRole).queue();

                                        addedRoles += roleChangeString(
                                                higherDepth(higherDepth(currentRole, "" + bankLevels.get(j)), "name")
                                                        .getAsString());

                                    }
                                    break;
                                }
                            }
                        } else if (currentRoleName.equals("alchemy") || currentRoleName.equals("combat")
                                || currentRoleName.equals("fishing") || currentRoleName.equals("farming")
                                || currentRoleName.equals("foraging") || currentRoleName.equals("carpentry")
                                || currentRoleName.equals("mining") || currentRoleName.equals("taming")
                                || currentRoleName.equals("enchanting") || currentRoleName.equals("catacombs")) {
                            String currSkill = currentRoleName.equals("catacombs") ? getPlayerCatacombs(username)
                                    : getPlayerSkills(username, currentRoleName);

                            if (currSkill == null && !errorRoles.contains("Skills")) {
                                errorRoles += roleChangeString("Skills API is disabled");
                            }
                            if (currSkill != null) {
                                List<Integer> skillLevels = currentRole.getAsJsonObject().entrySet().stream()
                                        .map(i -> Integer.parseInt(i.getKey()))
                                        .collect(Collectors.toCollection(ArrayList::new));
                                Collections.sort(skillLevels);
                                Collections.reverse(skillLevels);
                                for (int i = 0; i < skillLevels.size(); i++) {
                                    int levelCoins = higherDepth(higherDepth(currentRole, "" + skillLevels.get(i)),
                                            "level").getAsInt();
                                    if (Integer.parseInt(currSkill) >= levelCoins) {
                                        for (int j = i; j < skillLevels.size(); j++) {
                                            Role curRole = guild.getRoleById(
                                                    higherDepth(higherDepth(currentRole, "" + skillLevels.get(j)), "id")
                                                            .getAsString());
                                            if (guild.getMember(user).getRoles().contains(curRole)) {
                                            } else {
                                                guild.addRoleToMember(guild.getMember(user), curRole).queue();

                                                addedRoles += roleChangeString(
                                                        higherDepth(higherDepth(currentRole, "" + skillLevels.get(j)),
                                                                "name").getAsString());
                                            }

                                        }
                                        break;
                                    } else {
                                        Role curRole = guild.getRoleById(
                                                higherDepth(higherDepth(currentRole, "" + skillLevels.get(i)), "id")
                                                        .getAsString());
                                        if (guild.getMember(user).getRoles().contains(curRole)) {
                                            removedRoles += roleChangeString(
                                                    higherDepth(higherDepth(currentRole, "" + skillLevels.get(i)),
                                                            "name").getAsString());
                                            guild.removeRoleFromMember(guild.getMember(user), curRole).queue();
                                        }
                                    }
                                }
                            }
                        } else if (currentRoleName.equals("fairy")) {
                            String fairySouls = getPlayerFairySouls(username);
                            if (getPlayerFairySouls(username) != null) {
                                Role curRole = guild.getRoleById(higherDepth(currentRole, "id").getAsString());
                                if (Integer.parseInt(fairySouls) >= higherDepth(currentRole, "souls").getAsInt()) {
                                    if (!guild.getMember(user).getRoles().contains(curRole)) {
                                        guild.addRoleToMember(guild.getMember(user), curRole).queue();

                                        addedRoles += roleChangeString(higherDepth(currentRole, "name").getAsString());
                                    }
                                } else {
                                    if (guild.getMember(user).getRoles().contains(curRole)) {
                                        removedRoles += roleChangeString(
                                                higherDepth(currentRole, "name").getAsString());
                                        guild.removeRoleFromMember(guild.getMember(user), curRole).queue();
                                    }
                                }
                            }
                        } else if (currentRoleName.equals("doom_slayer")) {
                            String[] curSlayer = getPlayerSlayer(username).split(" ");
                            Role curRole = guild.getRoleById(higherDepth(currentRole, "id").getAsString());
                            boolean shouldHaveDoomSlayer = false;
                            for (String curType : curSlayer) {
                                if (Integer.parseInt(curType) >= 1000000) {
                                    if (!guild.getMember(user).getRoles().contains(curRole)) {
                                        guild.addRoleToMember(guild.getMember(user), curRole).queue();
                                        addedRoles += roleChangeString(higherDepth(currentRole, "name").getAsString());
                                        shouldHaveDoomSlayer = true;
                                        break;
                                    }
                                }
                            }
                            if (guild.getMember(user).getRoles().contains(curRole) && !shouldHaveDoomSlayer) {
                                removedRoles += roleChangeString(higherDepth(currentRole, "name").getAsString());
                                guild.removeRoleFromMember(guild.getMember(user), curRole).queue();
                            }
                        }
                    }
                    eb.setDescription("**Added Roles:**\n" + (addedRoles.length() > 0 ? addedRoles : "• None\n")
                            + "\n**Removed Roles:**\n" + (removedRoles.length() > 0 ? removedRoles : "• None"));
                    if (errorRoles.length() > 0) {
                        eb.addField("Disabled APIs:", errorRoles, false);
                    }
                    eb.setTimestamp(Instant.now());
                } else {
                    eb = defaultEmbed("Error fetching server's settings", null);
                }
            } catch (JsonIOException | JsonSyntaxException | FileNotFoundException e) {
                eb = defaultEmbed("Error fetching data", null);
            }
        } else {
            eb = defaultEmbed(errorMessage(this.name), null);
            eb.setDescription("");
            ebMessage.editMessage(eb.build()).queue();
            return;

        }
        ebMessage.editMessage(eb.build()).queue();
    }

    public String roleChangeString(String name) {
        return "• " + name + "\n";
    }

    public String getPlayerFairySouls(String username) {
        String playerUrl = "https://api.hypixel.net/skyblock/profile?key=" + key + "&profile=" + profileId;
        JsonElement skyblockJson = getJson(playerUrl);

        if (skyblockJson == null) {
            return null;
        }

        String uuidPlayer = usernameToUuid(username);

        String fairySouls = higherDepth(
                higherDepth(higherDepth(higherDepth(skyblockJson, "profile"), "members"), uuidPlayer),
                "fairy_souls_collected").getAsString();

        return fairySouls;
    }

    public String getPlayerCatacombs(String username) {

        String playerUrl = "https://api.hypixel.net/skyblock/profile?key=" + key + "&profile=" + profileId;
        JsonElement skyblockJson = getJson(playerUrl);

        if (skyblockJson == null) {
            return null;
        }

        String uuidPlayer = usernameToUuid(username);

        double skillExp = higherDepth(higherDepth(higherDepth(
                higherDepth(higherDepth(higherDepth(higherDepth(skyblockJson, "profile"), "members"), uuidPlayer),
                        "dungeons"),
                "dungeon_types"), "catacombs"), "experience").getAsLong();
        SkillsStruct skillInfo = skillInfoFromExp(skillExp, "catacombs");

        return "" + skillInfo.skillLevel;
    }

    public String getPlayerSkills(String username, String skill) {
        String playerUrl = "https://api.hypixel.net/skyblock/profile?key=" + key + "&profile=" + profileId;
        JsonElement skyblockJson = getJson(playerUrl);

        if (skyblockJson == null) {
            return null;
        }
        String uuidPlayer = usernameToUuid(username);

        try {
            double skillExp = higherDepth(
                    higherDepth(higherDepth(higherDepth(skyblockJson, "profile"), "members"), uuidPlayer),
                    "experience_skill_" + skill).getAsLong();
            SkillsStruct skillInfo = skillInfoFromExp(skillExp, skill);
            return "" + skillInfo.skillLevel;
        } catch (Exception e) {
            return null;
        }
    }

    public boolean checkValid(String username, String profile) {
        if (profile == null) {
            LatestProfileStruct curProfileID = getLatestProfile(username);
            if (curProfileID == null) {
                return false;
            }
            this.profileId = curProfileID.profileID;
        } else {
            String curProfileID = profileIdFromName(username, profile);
            if (curProfileID == null) {
                return false;
            }
            this.profileId = curProfileID;
        }
        return true;
    }

    public String getPlayerSlayer(String username) {
        String playerUrl = "https://api.hypixel.net/skyblock/profile?key=" + key + "&profile=" + profileId;
        JsonElement skyblockJson = getJson(playerUrl);

        if (skyblockJson == null) {
            return null;
        }
        String uuidPlayer = usernameToUuid(username);
        JsonElement profileSlayer = higherDepth(
                higherDepth(higherDepth(higherDepth(skyblockJson, "profile"), "members"), uuidPlayer), "slayer_bosses");

        int slayerWolf = higherDepth(higherDepth(profileSlayer, "wolf"), "xp") != null
                ? higherDepth(higherDepth(profileSlayer, "wolf"), "xp").getAsInt()
                : -1;
        int slayerZombie = higherDepth(higherDepth(profileSlayer, "zombie"), "xp") != null
                ? higherDepth(higherDepth(profileSlayer, "zombie"), "xp").getAsInt()
                : -1;
        int slayerSpider = higherDepth(higherDepth(profileSlayer, "spider"), "xp") != null
                ? higherDepth(higherDepth(profileSlayer, "spider"), "xp").getAsInt()
                : -1;

        return (slayerWolf + " " + slayerZombie + " " + slayerSpider);
    }

    public double getBankCoins(String username) {
        String playerUrl = "https://api.hypixel.net/skyblock/profile?key=" + key + "&profile=" + profileId;
        JsonElement skyblockJson = getJson(playerUrl);

        if (skyblockJson == null) {
            return -1;
        }

        try {
            double profileCoins = higherDepth(higherDepth(higherDepth(skyblockJson, "profile"), "banking"), "balance")
                    .getAsDouble();
            return profileCoins;
        } catch (Exception e) {
            return -2;
        }

    }

    public DiscordStruct getPlayerInfo(String username) {
        JsonElement playerJson = getJson("https://api.hypixel.net/player?key=" + key + "&name=" + username);

        if (playerJson == null) {
            return null;
        }

        if (higherDepth(playerJson, "player").isJsonNull()) {
            return null;
        }
        try {
            String discordID = higherDepth(
                    higherDepth(higherDepth(higherDepth(playerJson, "player"), "socialMedia"), "links"), "DISCORD")
                            .getAsString();
            return new DiscordStruct(discordID,
                    higherDepth(higherDepth(playerJson, "player"), "displayname").getAsString());
        } catch (Exception e) {
            return null;
        }
    }
}
