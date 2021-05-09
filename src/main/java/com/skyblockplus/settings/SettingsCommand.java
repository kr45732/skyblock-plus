package com.skyblockplus.settings;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.skyblockplus.api.discordserversettings.automatedapplication.ApplyRequirements;
import com.skyblockplus.api.discordserversettings.automatedguildroles.GuildRank;
import com.skyblockplus.api.discordserversettings.automatedroles.RoleModel;
import com.skyblockplus.api.discordserversettings.automatedroles.RoleObject;
import com.skyblockplus.api.discordserversettings.settingsmanagers.ServerSettingsModel;
import com.skyblockplus.utils.CustomPaginator;
import com.skyblockplus.utils.structs.PaginatorExtras;
import com.vdurmont.emoji.EmojiParser;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.exceptions.PermissionException;

import java.util.*;

import static com.skyblockplus.Main.database;
import static com.skyblockplus.Main.waiter;
import static com.skyblockplus.utils.Utils.*;

public class SettingsCommand extends Command {
    private CommandEvent event;

    public SettingsCommand() {
        this.name = "settings";
        this.cooldown = globalCooldown + 1;
        this.userPermissions = new Permission[]{Permission.ADMINISTRATOR};
    }

    @Override
    protected void execute(CommandEvent event) {
        new Thread(() -> {
            this.event = event;
            EmbedBuilder eb = loadingEmbed();
            Message ebMessage = event.getChannel().sendMessage(eb.build()).complete();
            String content = event.getMessage().getContentRaw();
            String[] args = content.split(" ");

            logCommand(event.getGuild(), event.getAuthor(), content);

            JsonElement currentSettings = database.getServerSettings(event.getGuild().getId());
            if (higherDepth(currentSettings, "serverId") == null) {
                database.addNewServerSettings(event.getGuild().getId(),
                        new ServerSettingsModel(event.getGuild().getName(), event.getGuild().getId()));
                currentSettings = database.getServerSettings(event.getGuild().getId());
            }

            if ((args.length == 3) && (args[1].equals("delete"))) {
                if (args[2].equals("--confirm")) {
                    if (database.deleteServerSettings(event.getGuild().getId()) == 200) {
                        ebMessage.editMessage(defaultEmbed("Success").setDescription("Server settings deleted").build())
                                .queue();
                    } else {
                        ebMessage
                                .editMessage(
                                        defaultEmbed("Error").setDescription("Error deleting server settings").build())
                                .queue();
                    }
                } else {
                    ebMessage.editMessage(defaultEmbed("Error").setDescription(
                            "To delete the server settings rerun this command with the `--confirm` flag (`" + BOT_PREFIX
                                    + "settings delete --confirm`)")
                            .build()).queue();
                }
                return;
            }

            if (args.length == 1) {
                eb = defaultEmbed("Settings for " + event.getGuild().getName());

                if (higherDepth(currentSettings, "automatedVerify") != null) {
                    eb.addField("Verify Settings",
                            "Use `" + BOT_PREFIX + "settings verify` to see the current verify settings", false);
                } else {
                    eb.addField("Verify Settings", "Error! Data not found", false);
                }

                if (higherDepth(currentSettings, "automatedApplication") != null) {
                    eb.addField("Apply Settings",
                            "Use `" + BOT_PREFIX + "settings apply` to see the current apply settings", false);
                } else {
                    eb.addField("Apply Settings", "Error! Data not found", false);
                }

                if (higherDepth(currentSettings, "automatedRoles") != null) {
                    eb.addField("Roles Settings",
                            "Use `" + BOT_PREFIX + "settings roles` to see the current roles settings", false);
                } else {
                    eb.addField("Roles Settings", "Error! Data not found", false);
                }

                if (higherDepth(currentSettings, "automaticGuildRoles") != null) {
                    eb.addField("Guild Role Settings",
                            "Use `" + BOT_PREFIX + "settings guild` to see the current guild role settings", false);
                } else {
                    eb.addField("Guild Role Settings", "Error! Data not found", false);
                }

            } else if (args.length >= 2 && args[1].equals("roles")) {
                if (args.length == 2) {
                    eb = defaultEmbed("Settings for " + event.getGuild().getName());
                    if (higherDepth(currentSettings, "automatedRoles") != null) {
                        ebMessage.delete().queue();
                        getCurrentRolesSettings(higherDepth(currentSettings, "automatedRoles")).build()
                                .paginate(ebMessage.getChannel(), 0);
                        return;
                    } else {
                        eb.addField("Roles Settings", "Error! Data not found", false);
                    }
                } else if (args.length == 3) {
                    if (args[2].equals("enable")) {
                        if (allowRolesEnable()) {
                            eb = setRolesEnable("true");
                        } else {
                            eb = defaultEmbed("Error").setDescription("No roles set!");
                        }
                    } else if (args[2].equals("disable")) {
                        eb = setRolesEnable("false");
                    } else {
                        eb = getCurrentRoleSettings(args[2]);
                        if (eb == null) {
                            ebMessage.delete().queue();
                            return;
                        }
                    }
                } else if (args.length == 4) {
                    if (args[2].equals("enable")) {
                        eb = setRoleEnable(args[3], "true");
                    } else if (args[2].equals("disable")) {
                        eb = setRoleEnable(args[3], "false");
                    } else {
                        eb = defaultEmbed("Error").setDescription("Invalid setting");
                    }
                } else if (args.length == 5) {
                    if (args[2].equals("stackable") && args[4].equals("true")) {
                        eb = setRoleStackable(args[3], "true");
                    } else if (args[2].equals("stackable") && args[4].equals("false")) {
                        eb = setRoleStackable(args[3], "false");
                    } else if (args[2].equals("remove")) {
                        eb = removeRoleLevel(args[3], args[4]);
                    } else if (args[2].equals("set")) {
                        eb = setOneLevelRole(args[3], args[4]);
                    } else {
                        eb = defaultEmbed("Error").setDescription("Invalid setting");
                    }
                } else if (args.length == 6 && args[2].equals("add")) {
                    eb = addRoleLevel(args[3], args[4], args[5]);
                } else {
                    eb = defaultEmbed("Error", null).setDescription("Invalid setting");
                }

                ebMessage.editMessage(eb.build()).queue();
                return;
            } else if (content.split(" ", 4).length >= 2 && content.split(" ", 4)[1].equals("apply")) {
                args = content.split(" ", 4);
                if (args.length == 2) {
                    eb = defaultEmbed("Settings for " + event.getGuild().getName(), null);
                    if (higherDepth(currentSettings, "automatedApplication") != null) {
                        eb = getCurrentApplySettings(higherDepth(currentSettings, "automatedApplication"));
                    } else {
                        eb.addField("Apply Settings", "Error! Data not found", false);
                    }
                } else if (args.length == 3) {
                    if (args[2].equals("enable")) {
                        if (allowApplyEnable()) {
                            eb = setApplyEnable("true");
                        } else {
                            eb = defaultEmbed("Error", null)
                                    .setDescription("All other apply settings must be set before " + "enabling apply!");
                        }
                    } else if (args[2].equals("disable")) {
                        eb = setApplyEnable("false");
                    } else {
                        eb = defaultEmbed("Error", null).setDescription("Invalid setting");
                    }
                } else if (args.length == 4) {
                    switch (args[2]) {
                        case "message":
                            eb = setApplyMessageText(args[3]);
                            break;
                        case "staff_role":
                            eb = setApplyStaffPingRoleId(args[3]);
                            break;
                        case "channel":
                            eb = setApplyMessageTextChannelId(args[3]);
                            break;
                        case "prefix":
                            eb = setApplyNewChannelPrefix(args[3]);
                            break;
                        case "category":
                            eb = setApplyNewChannelCategory(args[3]);
                            break;
                        case "staff_channel":
                            eb = setApplyMessageStaffChannelId(args[3]);
                            break;
                        case "accept_message":
                            eb = setApplyAcceptMessageText(args[3]);
                            break;
                        case "waitlist_message":
                            eb = setApplyWaitListMessageText(args[3]);
                            break;
                        case "deny_message":
                            eb = setApplyDenyMessageText(args[3]);
                            break;
                        case "reqs":
                        case "req":
                        case "requirements":
                            args = content.split(" ");

                            if (args.length >= 5) {
                                if (args[3].equals("add")) {
                                    eb = addApplyRequirement(content.split(" ", 5)[4]);
                                } else if (args[3].equals("remove")) {
                                    eb = removeApplyRequirement(args[4]);
                                } else {
                                    eb = defaultEmbed("Error").setDescription("Invalid setting");
                                }
                            } else {
                                eb = defaultEmbed("Error").setDescription("Invalid setting");
                            }
                            break;
                        default:
                            eb = defaultEmbed("Error", null).setDescription("Invalid setting");
                            break;
                    }
                }
            } else if (content.split(" ", 4).length >= 2 && content.split(" ", 4)[1].equals("verify")) {
                args = content.split(" ", 4);
                if (args.length == 2) {
                    eb = defaultEmbed("Settings for " + event.getGuild().getName(), null);
                    if (higherDepth(currentSettings, "automatedVerify") != null) {
                        eb.addField("Verify Settings",
                                getCurrentVerifySettings(higherDepth(currentSettings, "automatedVerify")), false);
                    } else {
                        eb.addField("Verify Settings", "Error! Data not found", false);
                    }
                } else if (args.length == 3) {
                    if (args[2].equals("enable")) {
                        if (allowVerifyEnable()) {
                            eb = setVerifyEnable("true");
                        } else {
                            eb = defaultEmbed("Error", null).setDescription(
                                    "All other verify settings must be set before " + "enabling verify!");
                        }
                    } else if (args[2].equals("disable")) {
                        eb = setVerifyEnable("false");
                    } else {
                        eb = defaultEmbed("Error", null).setDescription("Invalid setting");
                    }
                } else if (args.length == 4) {
                    switch (args[2]) {
                        case "message":
                            eb = setVerifyMessageText(args[3]);
                            break;
                        case "role":
                            eb = setVerifyVerifiedRole(args[3]);
                            break;
                        case "channel":
                            eb = setVerifyMessageTextChannelId(args[3]);
                            break;
                        case "nickname":
                            eb = setVerifyNickname(args[3]);
                            break;
                        default:
                            eb = defaultEmbed("Error", null).setDescription("Invalid setting");
                            break;
                    }
                }
            } else if ((args.length >= 2) && args[1].equals("guild")) {
                if (args.length == 2) {
                    eb = defaultEmbed("Settings for " + event.getGuild().getName(), null);
                    if (higherDepth(currentSettings, "automaticGuildRoles") != null) {
                        eb.addField("Guild Role Settings", getCurrentGuildRoleSettings(), false);
                    } else {
                        eb.addField("Guild Role Settings", "Error! Data not found", false);
                    }
                } else if (args.length == 4) {
                    switch (args[2]) {
                        case "set":
                            eb = setGuildRoleId(args[3]);
                            break;
                        case "role":
                            eb = setGuildRoleName(args[3]);
                            break;
                        case "enable":
                            if (args[3].equals("role")) {
                                eb = setGuildRoleEnable("true");
                            } else if (args[3].equals("rank")) {
                                eb = setGuildRankEnable("true");
                            } else {
                                eb = defaultEmbed("Error").setDescription("Invalid setting");
                            }
                            break;
                        case "disable":
                            if (args[3].equals("role")) {
                                eb = setGuildRoleEnable("false");
                            } else if (args[3].equals("rank")) {
                                eb = setGuildRankEnable("false");
                            } else {
                                eb = defaultEmbed("Error").setDescription("Invalid setting");
                            }
                            break;
                        case "remove":
                            eb = removeGuildRank(args[3]);
                            break;
                        default:
                            eb = defaultEmbed("Error").setDescription("Invalid setting");
                            break;
                    }
                } else if (args.length == 5) {
                    if (args[2].equals("add")) {
                        eb = addGuildRank(args[3], args[4]);
                    } else {
                        eb = defaultEmbed("Error").setDescription("Invalid setting");
                    }
                } else {
                    eb = defaultEmbed("Error").setDescription("Invalid setting");
                }

            } else {
                eb = defaultEmbed("Error", null).setDescription("Invalid setting");
            }

            ebMessage.editMessage(eb.build()).queue();
        }).start();
    }

    /* Guild Role Settings */
    private String getCurrentGuildRoleSettings() {
        JsonElement currentSettings = database.getGuildRoleSettings(event.getGuild().getId());
        String ebFieldString = "";
        ebFieldString += "**" + displaySettings(currentSettings, "enableGuildRole") + "**";
        ebFieldString += "\n**• Guild Name:** " + displaySettings(currentSettings, "guildId");
        ebFieldString += "\n**• Guild Role:** " + displaySettings(currentSettings, "roleId");
        ebFieldString += "\n\n**" + displaySettings(currentSettings, "enableGuildRanks") + "**";

        StringBuilder guildRanksString = new StringBuilder();
        try {
            for (JsonElement guildRank : higherDepth(currentSettings, "guildRanks").getAsJsonArray()) {
                guildRanksString.append("\n• ").append(higherDepth(guildRank, "minecraftRoleName").getAsString())
                        .append(" - ").append("<@&" + higherDepth(guildRank, "discordRoleId").getAsString() + ">");
            }
        } catch (Exception ignored) {
        }

        ebFieldString += guildRanksString.length() > 0 ? guildRanksString.toString() : "\n• No guild ranks set";

        return ebFieldString;
    }

    private EmbedBuilder setGuildRoleEnable(String enable) {
        JsonObject currentSettings = database.getGuildRoleSettings(event.getGuild().getId()).getAsJsonObject();
        if ((higherDepth(currentSettings, "guildId") == null) || (higherDepth(currentSettings, "roleId") == null)) {
            return defaultEmbed("Guild name and role must be set before enabling");
        }

        currentSettings.remove("enableGuildRole");
        currentSettings.addProperty("enableGuildRole", enable);
        int responseCode = database.updateGuildRoleSettings(event.getGuild().getId(), currentSettings);
        if (responseCode != 200) {
            return defaultEmbed("Error", null).setDescription("API returned response code " + responseCode);
        }

        EmbedBuilder eb = defaultEmbed("Settings for " + event.getGuild().getName());
        eb.setDescription("Guild role " + (enable.equals("true") ? "enabled" : "disabled"));
        return eb;
    }

    private EmbedBuilder setGuildRankEnable(String enable) {
        JsonObject currentSettings = database.getGuildRoleSettings(event.getGuild().getId()).getAsJsonObject();

        if ((higherDepth(currentSettings, "guildId") == null)
                || (higherDepth(currentSettings, "guildRanks").getAsJsonArray().size() == 0)) {
            return defaultEmbed("Error").setDescription("The guild name and a guild rank must be set");
        }

        currentSettings.remove("enableGuildRanks");
        currentSettings.addProperty("enableGuildRanks", enable);
        int responseCode = database.updateGuildRoleSettings(event.getGuild().getId(), currentSettings);
        if (responseCode != 200) {
            return defaultEmbed("Error", null).setDescription("API returned response code " + responseCode);
        }

        EmbedBuilder eb = defaultEmbed("Settings for " + event.getGuild().getName());
        eb.setDescription("Guild ranks " + (enable.equals("true") ? "enabled" : "disabled"));
        return eb;
    }

    private EmbedBuilder addGuildRank(String rankName, String roleMention) {
        Role guildRankRole;
        try {
            guildRankRole = event.getGuild().getRoleById(roleMention.replaceAll("[<@&>]", ""));
            if ((guildRankRole.isPublicRole() || guildRankRole.isManaged())) {
                return defaultEmbed("Error").setDescription("Role cannot be managed or @everyone");
            }
        } catch (Exception e) {
            return defaultEmbed("Invalid Role", null);
        }

        JsonObject currentSettings = database.getGuildRoleSettings(event.getGuild().getId()).getAsJsonObject();

        if (higherDepth(currentSettings, "guildId") == null) {
            return defaultEmbed("Guild name must first be set");
        }

        String guildId = higherDepth(currentSettings, "guildId").getAsString();

        JsonElement guildJson = getJson("https://api.hypixel.net/guild?key=" + HYPIXEL_API_KEY + "&id=" + guildId);

        if (higherDepth(guildJson, "guild") == null) {
            return defaultEmbed("Current guild name is invalid");
        }

        JsonArray guildRanks = higherDepth(higherDepth(guildJson, "guild"), "ranks").getAsJsonArray();

        StringBuilder guildRanksString = new StringBuilder();
        for (JsonElement guildRank : guildRanks) {
            guildRanksString.append("\n• ").append(higherDepth(guildRank, "name").getAsString().replace(" ", "_"));
            if (higherDepth(guildRank, "name").getAsString().equalsIgnoreCase(rankName.replace("_", " "))) {
                JsonArray currentGuildRanks = currentSettings.get("guildRanks").getAsJsonArray();

                for (JsonElement level : currentGuildRanks) {
                    if (higherDepth(level, "minecraftRoleName").getAsString().equalsIgnoreCase(rankName)) {
                        currentGuildRanks.remove(level);
                        break;
                    }
                }

                Gson gson = new Gson();
                currentGuildRanks.add(gson.toJsonTree(new GuildRank(rankName.toLowerCase(), guildRankRole.getId())));

                currentSettings.remove("guildRanks");
                currentSettings.add("guildRanks", currentGuildRanks);

                int responseCode = database.updateGuildRoleSettings(event.getGuild().getId(), currentSettings);
                if (responseCode != 200) {
                    return defaultEmbed("Error", null).setDescription("API returned response code " + responseCode);
                }

                EmbedBuilder eb = defaultEmbed("Settings for " + event.getGuild().getName(), null);
                eb.setDescription("**Guild rank added:** " + higherDepth(guildRank, "name").getAsString() + " - "
                        + guildRankRole.getAsMention());
                return eb;
            }
        }

        return defaultEmbed("Invalid guild rank")
                .setDescription((guildRanksString.length() > 0 ? "Valid guild ranks are: " + guildRanksString
                        : "No guild ranks found"));
    }

    private EmbedBuilder removeGuildRank(String rankName) {
        JsonObject currentSettings = database.getGuildRoleSettings(event.getGuild().getId()).getAsJsonObject();
        JsonArray currentGuildRanks = currentSettings.get("guildRanks").getAsJsonArray();

        for (JsonElement guildRank : currentGuildRanks) {
            if (higherDepth(guildRank, "minecraftRoleName").getAsString().equalsIgnoreCase(rankName)) {
                JsonArray currentGuildRanksTemp = currentSettings.get("guildRanks").getAsJsonArray();
                currentGuildRanksTemp.remove(guildRank);

                if (currentGuildRanksTemp.size() == 0) {
                    currentSettings.remove("enableGuildRanks");
                    currentSettings.addProperty("enableGuildRanks", "false");
                }

                currentSettings.remove("guildRanks");
                currentSettings.add("guildRanks", currentGuildRanksTemp);

                int responseCode = database.updateGuildRoleSettings(event.getGuild().getId(), currentSettings);
                if (responseCode != 200) {
                    return defaultEmbed("Error", null).setDescription("API returned response code " + responseCode);
                }

                EmbedBuilder eb = defaultEmbed("Settings for " + event.getGuild().getName(), null);
                eb.setDescription("**Guild rank removed:** " + rankName);
                return eb;
            }
        }

        return defaultEmbed("Error").setDescription("Invalid rank name");
    }

    private EmbedBuilder setGuildRoleId(String guildName) {
        try {
            JsonElement guildJson = getJson(
                    "https://api.hypixel.net/guild?key=" + HYPIXEL_API_KEY + "&name=" + guildName.replace("_", "%20"));
            String guildId = higherDepth(higherDepth(guildJson, "guild"), "_id").getAsString();
            JsonObject currentSettings = database.getGuildRoleSettings(event.getGuild().getId()).getAsJsonObject();
            currentSettings.remove("guildId");
            currentSettings.addProperty("guildId", guildId);
            int responseCode = database.updateGuildRoleSettings(event.getGuild().getId(), currentSettings);
            if (responseCode != 200) {
                return defaultEmbed("Error", null).setDescription("API returned response code " + responseCode);
            }

            EmbedBuilder eb = defaultEmbed("Settings for " + event.getGuild().getName(), null);
            eb.setDescription(
                    "**Guild set to:** " + higherDepth(higherDepth(guildJson, "guild"), "name").getAsString());
            return eb;
        } catch (Exception e) {
            return defaultEmbed("Error", null).setDescription("Invalid guild name");
        }
    }

    private EmbedBuilder setGuildRoleName(String roleMention) {
        try {
            Role verifyGuildRole = event.getGuild().getRoleById(roleMention.replaceAll("[<@&>]", ""));
            if (!(verifyGuildRole.isPublicRole() || verifyGuildRole.isManaged())) {
                JsonObject currentSettings = database.getGuildRoleSettings(event.getGuild().getId()).getAsJsonObject();
                currentSettings.remove("roleId");
                currentSettings.addProperty("roleId", verifyGuildRole.getId());
                int responseCode = database.updateGuildRoleSettings(event.getGuild().getId(), currentSettings);

                if (responseCode != 200) {
                    return defaultEmbed("Error", null).setDescription("API returned response code " + responseCode);
                }

                EmbedBuilder eb = defaultEmbed("Settings for " + event.getGuild().getName(), null);
                eb.setDescription("**Guild role set to:** " + verifyGuildRole.getAsMention());
                return eb;
            }
        } catch (Exception ignored) {
        }
        return defaultEmbed("Invalid Role", null);
    }

    /* Roles Settings */
    private EmbedBuilder getCurrentRoleSettings(String roleName) {
        Map<String, Integer> rolePageMap = new HashMap<>();
        rolePageMap.put("sven", 2);
        rolePageMap.put("rev", 3);
        rolePageMap.put("tara", 4);
        rolePageMap.put("bank_coins", 5);
        rolePageMap.put("alchemy", 6);
        rolePageMap.put("combat", 7);
        rolePageMap.put("fishing", 8);
        rolePageMap.put("farming", 9);
        rolePageMap.put("foraging", 10);
        rolePageMap.put("carpentry", 11);
        rolePageMap.put("mining", 12);
        rolePageMap.put("taming", 13);
        rolePageMap.put("enchanting", 14);
        rolePageMap.put("catacombs", 15);
        rolePageMap.put("guild_member", 16);
        rolePageMap.put("fairy_souls", 17);
        rolePageMap.put("slot_collector", 18);
        rolePageMap.put("pet_enthusiast", 19);
        rolePageMap.put("doom_slayer", 20);
        rolePageMap.put("all_slayer_nine", 21);
        rolePageMap.put("skill_average", 22);
        rolePageMap.put("pet_score", 23);
        rolePageMap.put("dungeon_secrets", 24);

        if (rolePageMap.containsKey(roleName)) {
            CustomPaginator.Builder currentRoleSettings = getCurrentRolesSettings(
                    database.getRolesSettings(event.getGuild().getId()));
            currentRoleSettings.build().paginate(event.getChannel(), rolePageMap.get(roleName));
            return null;
        } else {
            try {
                if (rolePageMap.containsValue(Integer.parseInt(roleName))) {
                    CustomPaginator.Builder currentRoleSettings = getCurrentRolesSettings(
                            database.getRolesSettings(event.getGuild().getId()));
                    currentRoleSettings.build().paginate(event.getChannel(), rolePageMap.get(roleName));
                    return null;
                }
            } catch (Exception ignored) {
            }
        }

        return defaultEmbed("Error").setDescription("Invalid role name");

    }

    private CustomPaginator.Builder getCurrentRolesSettings(JsonElement rolesSettings) {
        CustomPaginator.Builder paginateBuilder = defaultPaginator(waiter, event.getAuthor()).setColumns(1)
                .setItemsPerPage(1);

        ArrayList<String> pageTitles = new ArrayList<>();
        pageTitles.add("Roles Settings");

        ArrayList<String> roleNames = getJsonKeys(rolesSettings);

        StringBuilder pageNumbers = new StringBuilder();
        for (int i = 1; i < roleNames.size(); i++) {
            pageNumbers.append("\n**Page ").append(i + 1).append(":** ").append(roleNames.get(i));
        }

        paginateBuilder.addItems("**Automated Roles "
                + (higherDepth(rolesSettings, "enable").getAsString().equals("true") ? "Enabled" : "Disabled") + "**"
                + pageNumbers);
        roleNames.remove("enable");
        for (String roleName : roleNames) {
            JsonElement currentRoleSettings = higherDepth(rolesSettings, roleName);
            StringBuilder ebFieldString = new StringBuilder();

            if (higherDepth(currentRoleSettings, "enable") == null) {
                database.updateRoleSettings(event.getGuild().getId(), roleName, new Gson().toJsonTree(new RoleModel()));
                currentRoleSettings = database.getRoleSettings(event.getGuild().getId(), roleName);
            }

            if (higherDepth(currentRoleSettings, "stackable") == null) {
                database.updateRoleSettings(event.getGuild().getId(), roleName, new Gson().toJsonTree(new RoleModel()));
                currentRoleSettings = database.getRoleSettings(event.getGuild().getId(), roleName);
            }

            switch (roleName) {
                case "guild_member": {
                    ebFieldString.append("**Member role for Hypixel guilds**\nExample: `").append(BOT_PREFIX)
                            .append("settings roles add guild_member skyblock_forceful @sbf guild member`\n");
                    break;
                }
                case "sven": {
                    ebFieldString.append("**A player's sven packmaster slayer xp**\nExample: `").append(BOT_PREFIX)
                            .append("settings roles add sven 1000000 @sven 9`\n");
                    break;
                }
                case "rev": {
                    ebFieldString.append("**A player's revenant horror xp slayer**\nExample: `").append(BOT_PREFIX)
                            .append("settings roles add rev 400000 @rev 8`\n");
                    break;
                }
                case "tara": {
                    ebFieldString.append("**A player's tarantula broodfather slayer xp**\nExample: `")
                            .append(BOT_PREFIX).append("settings roles add tara 100000 @tara 7`\n");
                    break;
                }
                case "bank_coins": {
                    ebFieldString.append("**Coins in a player's bank**\nExample: `").append(BOT_PREFIX)
                            .append("settings roles add bank_coins 1000000 @millionaire`\n");
                    break;
                }
                case "alchemy":
                case "combat":
                case "fishing":
                case "farming":
                case "foraging":
                case "carpentry":
                case "mining":
                case "taming":
                case "enchanting":
                case "skill_average":
                case "pet_score":
                case "catacombs": {
                    ebFieldString.append("**A player's ").append(roleName).append(" level**\nExample: `")
                            .append(BOT_PREFIX).append("settings roles add ").append(roleName).append(" 30 @")
                            .append(roleName).append(" 30`\n");
                    break;
                }
                case "fairy_souls": {
                    ebFieldString.append("**Amount of collected fairy souls**\nExample: `").append(BOT_PREFIX)
                            .append("settings roles add fairy_souls 50 @50 souls collected`\n");
                    break;
                }
                case "slot_collector": {
                    ebFieldString
                            .append("**Number of minion slots excluding upgrades (__not fully working__)**\nExample: `")
                            .append(BOT_PREFIX).append("settings roles add slot_collector 24 @maxed minion slots`\n");
                    break;
                }
                case "pet_enthusiast": {
                    ebFieldString.append(
                            "**Having a level 100 epic or legendary pet that is not an enchanting or alchemy pet**\nExample: `")
                            .append(BOT_PREFIX).append("settings roles set pet_enthusiast @level 100 pet`\n");
                    break;
                }
                case "doom_slayer": {
                    ebFieldString.append("**Having at least one level nine slayer**\nExample: `").append(BOT_PREFIX)
                            .append("settings roles set doom_slayer @level nine slayer`\n");
                    break;
                }
                case "all_slayer_nine": {
                    ebFieldString.append("**Having all level nine slayers**\nExample: `").append(BOT_PREFIX)
                            .append("settings roles set all_slayer_nine @role`\n");
                    break;
                }
                case "dungeon_secrets": {
                    ebFieldString.append("**A player's dungeon secrets count**\nExample: `").append(BOT_PREFIX)
                            .append("settings roles add dungeon_secrets 25000 @secret sweat`\n");
                    break;
                }
            }

            ebFieldString.append("\nCurrent Settings:\n");

            ebFieldString.append(higherDepth(currentRoleSettings, "enable").getAsString().equals("true") ? "• Enabled"
                    : "• Disabled");
            if (isOneLevelRole(roleName)) {
                try {
                    ebFieldString.append("\n• default - ").append("<@&" +
                            higherDepth(higherDepth(currentRoleSettings, "levels").getAsJsonArray().get(0), "roleId")
                                    .getAsString() + ">"
                            );
                } catch (Exception ignored) {
                }
                pageTitles.add(roleName + " (__one level role__)");
            } else {
                ebFieldString.append(
                        higherDepth(currentRoleSettings, "stackable").getAsString().equals("true") ? "\n• Stackable"
                                : "\n• Not stackable");

                if (roleName.equals("guild_member")) {
                    for (JsonElement roleLevel : higherDepth(currentRoleSettings, "levels").getAsJsonArray()) {
                        String guildId = higherDepth(roleLevel, "value").getAsString();
                        JsonElement guildJson = getJson(
                                "https://api.hypixel.net/guild?key=" + HYPIXEL_API_KEY + "&id=" + guildId);
                        ebFieldString.append("\n• ")
                                .append(higherDepth(higherDepth(guildJson, "guild"), "name").getAsString())
                                .append(" - ").append("<@&" + higherDepth(roleLevel, "roleId").getAsString() + ">");
                    }
                } else {
                    for (JsonElement roleLevel : higherDepth(currentRoleSettings, "levels").getAsJsonArray()) {
                        ebFieldString.append("\n• ").append(higherDepth(roleLevel, "value").getAsString()).append(" - ")
                                .append("<@&" + higherDepth(roleLevel, "roleId").getAsString() + ">");
                    }
                }

                if (higherDepth(currentRoleSettings, "levels").getAsJsonArray().size() == 0) {
                    ebFieldString.append("\n• No levels set");
                }

                pageTitles.add(roleName);
            }
            paginateBuilder.addItems(ebFieldString.toString());
        }

        return paginateBuilder.setPaginatorExtras(new PaginatorExtras().setTitles(pageTitles));
    }

    private boolean allowRolesEnable() {
        JsonObject currentSettings = database.getRolesSettings(event.getGuild().getId()).getAsJsonObject();
        currentSettings.remove("enable");
        for (String role : getJsonKeys(currentSettings)) {
            if (higherDepth(higherDepth(currentSettings, role), "enable").getAsBoolean()) {
                return true;
            }
        }
        return false;
    }

    private EmbedBuilder setRolesEnable(String enable) {
        if (enable.equalsIgnoreCase("true") || enable.equalsIgnoreCase("false")) {
            JsonObject newRolesJson = database.getRolesSettings(event.getGuild().getId()).getAsJsonObject();
            newRolesJson.remove("enable");
            newRolesJson.addProperty("enable", enable);
            int responseCode = database.updateRolesSettings(event.getGuild().getId(), newRolesJson);
            if (responseCode != 200) {
                return defaultEmbed("Error", null).setDescription("API returned response code " + responseCode);
            }

            EmbedBuilder eb = defaultEmbed("Settings for " + event.getGuild().getName(), null);
            eb.setDescription("**Roles:** " + (enable.equalsIgnoreCase("true") ? "enabled" : "disabled"));
            return eb;
        }
        return defaultEmbed("Invalid Input", null);
    }

    private EmbedBuilder setRoleEnable(String roleName, String enable) {
        JsonObject currentRoleSettings = null;
        try {
            currentRoleSettings = database.getRoleSettings(event.getGuild().getId(), roleName).getAsJsonObject();
        } catch (Exception ignored) {
        }

        if (currentRoleSettings == null) {
            return defaultEmbed("Error", null).setDescription("Invalid role name");
        }

        if (currentRoleSettings.get("levels").getAsJsonArray().size() != 0) {
            currentRoleSettings.remove("enable");
            currentRoleSettings.addProperty("enable", enable);
            int responseCode = database.updateRoleSettings(event.getGuild().getId(), roleName, currentRoleSettings);
            if (responseCode != 200) {
                return defaultEmbed("Error", null).setDescription("API returned response code " + responseCode);
            }

            EmbedBuilder eb = defaultEmbed("Settings for " + event.getGuild().getName(), null);
            eb.setDescription(
                    "**" + roleName + " role:** " + (enable.equalsIgnoreCase("true") ? "enabled" : "disabled"));
            return eb;
        } else {
            currentRoleSettings.remove("enable");
            currentRoleSettings.addProperty("enable", "false");
            database.updateRoleSettings(event.getGuild().getId(), roleName, currentRoleSettings);
        }
        EmbedBuilder eb = defaultEmbed("Error", null);
        eb.setDescription("Specified role must have at least one configuration!");
        return eb;
    }

    private EmbedBuilder addRoleLevel(String roleName, String roleValue, String roleMention) {
        String guildName = "";
        if (roleName.equals("guild_member")) {
            try {
                JsonElement guildJson = getJson("https://api.hypixel.net/guild?key=" + HYPIXEL_API_KEY + "&name="
                        + roleValue.replace("_", "%20"));
                roleValue = higherDepth(higherDepth(guildJson, "guild"), "_id").getAsString();
                guildName = higherDepth(higherDepth(guildJson, "guild"), "name").getAsString();
            } catch (Exception e) {
                return defaultEmbed("Error", null).setDescription("Invalid username");
            }
        } else if (isOneLevelRole(roleName)) {
            return defaultEmbed("These roles do not support levels. Use `" + BOT_PREFIX
                    + "settings roles set [roleName] [@role]` instead");
        } else {
            try {
                Integer.parseInt(roleValue);
            } catch (Exception e) {
                return defaultEmbed("Error").setDescription("Role value must be an integer");
            }
        }

        Role role = event.getGuild().getRoleById(roleMention.replaceAll("[<@&>]", ""));
        if (role == null) {
            return defaultEmbed("Error", null).setDescription("Invalid role mention");
        }

        if (role.isPublicRole() || role.isManaged()) {
            return defaultEmbed("Error", null).setDescription("Role cannot be managed or @everyone!");
        }
        JsonObject newRoleSettings;
        try {
            newRoleSettings = database.getRoleSettings(event.getGuild().getId(), roleName).getAsJsonObject();
        } catch (Exception e) {
            return defaultEmbed("Error", null).setDescription("Invalid role");
        }

        JsonArray currentLevels = newRoleSettings.get("levels").getAsJsonArray();

        if (currentLevels.size() >= 5) {
            return defaultEmbed("Error").setDescription("This role has reached the max limit of levels (5/5)");
        }

        for (JsonElement level : currentLevels) {
            if (higherDepth(level, "value").getAsString().equals(roleValue)) {
                currentLevels.remove(level);
                break;
            }
        }

        Gson gson = new Gson();
        currentLevels.add(gson.toJsonTree(new RoleObject(roleValue, role.getId())));

        RoleObject[] temp = gson.fromJson(currentLevels, new TypeToken<RoleObject[]>() {
        }.getType());
        Arrays.sort(temp, Comparator.comparingInt(o -> Integer.parseInt(o.getValue())));
        currentLevels = gson.toJsonTree(temp).getAsJsonArray();

        newRoleSettings.remove("levels");
        newRoleSettings.add("levels", currentLevels);

        int responseCode = database.updateRoleSettings(event.getGuild().getId(), roleName, newRoleSettings);
        if (responseCode != 200) {
            return defaultEmbed("Error", null).setDescription("API returned response code " + responseCode);
        }

        if (roleName.equals("guild_member")) {
            roleValue = guildName;
        }

        return defaultEmbed("Settings for " + event.getGuild().getName(), null)
                .setDescription(roleName + " " + roleValue + " set to " + role.getAsMention());
    }

    private EmbedBuilder removeRoleLevel(String roleName, String value) {
        if (isOneLevelRole(roleName)) {
            return defaultEmbed("These roles do not support levels. Use `" + BOT_PREFIX
                    + "settings roles set [roleName] [@role]` instead");
        }

        JsonObject currentRoleSettings;
        try {
            currentRoleSettings = database.getRoleSettings(event.getGuild().getId(), roleName).getAsJsonObject();
        } catch (Exception e) {
            return defaultEmbed("Error").setDescription("Invalid role name");
        }

        JsonArray currentLevels = currentRoleSettings.get("levels").getAsJsonArray();
        for (JsonElement level : currentLevels) {
            String currentValue = higherDepth(level, "value").getAsString();
            if (roleName.equals("guild_member")) {
                JsonElement guildJson = getJson("https://api.hypixel.net/guild?key=" + HYPIXEL_API_KEY + "&id="
                        + higherDepth(level, "value").getAsString());
                currentValue = higherDepth(higherDepth(guildJson, "guild"), "name").getAsString();
            }

            if (currentValue.equalsIgnoreCase(value.replace("_", " "))) {
                currentLevels.remove(level);
                currentRoleSettings.remove("levels");
                currentRoleSettings.add("levels", currentLevels);
                int responseCode = database.updateRoleSettings(event.getGuild().getId(), roleName, currentRoleSettings);
                if (responseCode != 200) {
                    return defaultEmbed("Error", null).setDescription("API returned response code " + responseCode);
                }

                currentRoleSettings = database.getRoleSettings(event.getGuild().getId(), roleName).getAsJsonObject();

                if (currentRoleSettings.get("levels").getAsJsonArray().size() == 0) {
                    setRoleEnable(roleName, "false");
                }

                if (!allowRolesEnable()) {
                    setRolesEnable("false");
                }

                return defaultEmbed("Settings for " + event.getGuild().getName(), null)
                        .setDescription(roleName + " " + value + " removed");
            }
        }
        return defaultEmbed("Error", null).setDescription("Invalid role value");
    }

    private EmbedBuilder setRoleStackable(String roleName, String stackable) {
        if (isOneLevelRole(roleName)) {
            return defaultEmbed("Error").setDescription("This role does not support stacking");
        }

        JsonObject currentRoleSettings = database.getRoleSettings(event.getGuild().getId(), roleName).getAsJsonObject();
        currentRoleSettings.remove("stackable");
        currentRoleSettings.addProperty("stackable", stackable);
        int responseCode = database.updateRoleSettings(event.getGuild().getId(), roleName, currentRoleSettings);
        if (responseCode != 200) {
            return defaultEmbed("Error", null).setDescription("API returned response code " + responseCode);
        }

        EmbedBuilder eb = defaultEmbed("Settings for " + event.getGuild().getName(), null);
        eb.setDescription(
                "**" + roleName + " role:** " + (stackable.equalsIgnoreCase("true") ? "stackable" : "not stackable"));
        return eb;
    }

    private EmbedBuilder setOneLevelRole(String roleName, String roleMention) {
        if (!isOneLevelRole(roleName)) {
            return defaultEmbed("Error").setDescription("This role does is not a one level role. Use `" + BOT_PREFIX
                    + "settings roles add [roleName] [value] [@role]` instead");
        }

        Role role = event.getGuild().getRoleById(roleMention.replaceAll("[<@&>]", ""));
        if (role == null) {
            return defaultEmbed("Error", null).setDescription("Invalid role mention");
        }
        if (role.isPublicRole() || role.isManaged()) {
            return defaultEmbed("Error", null).setDescription("Role cannot be managed or @everyone!");
        }

        JsonObject newRoleSettings;
        try {
            newRoleSettings = database.getRoleSettings(event.getGuild().getId(), roleName).getAsJsonObject();
        } catch (Exception e) {
            return defaultEmbed("Error", null).setDescription("Invalid role");
        }

        JsonArray currentLevels = newRoleSettings.get("levels").getAsJsonArray();

        Gson gson = new Gson();
        currentLevels.add(gson.toJsonTree(new RoleObject("default", role.getId())));

        RoleObject[] temp = gson.fromJson(currentLevels, new TypeToken<RoleObject[]>() {
        }.getType());
        Arrays.sort(temp, Comparator.comparing(RoleObject::getValue));
        currentLevels = gson.toJsonTree(temp).getAsJsonArray();

        newRoleSettings.remove("levels");
        newRoleSettings.add("levels", currentLevels);

        int responseCode = database.updateRoleSettings(event.getGuild().getId(), roleName, newRoleSettings);

        if (responseCode != 200) {
            return defaultEmbed("Error", null).setDescription("API returned response code " + responseCode);
        }

        return defaultEmbed("Settings for " + event.getGuild().getName(), null)
                .setDescription(roleName + " set to " + role.getAsMention());
    }

    private boolean isOneLevelRole(String roleName) {
        return (roleName.equals("pet_enthusiast") || roleName.equals("doom_slayer")
                || roleName.equals("all_slayer_nine"));
    }

    /* Verify Settings */
    private String getCurrentVerifySettings(JsonElement verifySettings) {
        String ebFieldString = "";
        ebFieldString += "**" + displaySettings(verifySettings, "enable") + "**";
        ebFieldString += "\n**• React Message Text:** " + displaySettings(verifySettings, "messageText");
        ebFieldString += "\n**• React Message Channel:** " + displaySettings(verifySettings, "messageTextChannelId");
        ebFieldString += "\n**• Verified Role:** " + displaySettings(verifySettings, "verifiedRole");
        ebFieldString += "\n**• Nickname Template:** " + displaySettings(verifySettings, "verifiedNickname");
        return ebFieldString;
    }

    private boolean allowVerifyEnable() {
        JsonObject currentSettings = database.getVerifySettings(event.getGuild().getId()).getAsJsonObject();
        currentSettings.remove("previousMessageId");

        for (String key : getJsonKeys(currentSettings)) {
            if (higherDepth(currentSettings, key).getAsString().length() == 0) {
                return false;
            }
        }
        return true;
    }

    private EmbedBuilder setVerifyEnable(String enable) {
        if (enable.equalsIgnoreCase("true") || enable.equalsIgnoreCase("false")) {
            int responseCode = updateVerifySettings("enable", enable);
            if (responseCode != 200) {
                return defaultEmbed("Error", null).setDescription("API returned response code " + responseCode);
            }

            EmbedBuilder eb = defaultEmbed("Settings for " + event.getGuild().getName(), null);
            eb.setDescription("**Verify:** " + (enable.equalsIgnoreCase("true") ? "enabled" : "disabled") + "\nRun `"
                    + BOT_PREFIX + "reload` to reload the settings");
            return eb;
        }
        return defaultEmbed("Invalid Input", null);
    }

    private EmbedBuilder setVerifyMessageText(String verifyText) {
        if (verifyText.length() > 0) {
            if (EmojiParser.parseToAliases(verifyText).length() > 1500) {
                return defaultEmbed("Error", null).setDescription("Text cannot be longer than 1500 letters!");
            }

            int responseCode = updateVerifySettings("messageText", EmojiParser.parseToAliases(verifyText));
            if (responseCode != 200) {
                return defaultEmbed("Error", null).setDescription("API returned response code " + responseCode);
            }

            EmbedBuilder eb = defaultEmbed("Settings for " + event.getGuild().getName(), null);
            eb.setDescription("**Verify message set to:** " + verifyText);
            return eb;
        }
        return defaultEmbed("Invalid Input", null);
    }

    private EmbedBuilder setVerifyMessageTextChannelId(String textChannel) {
        try {
            TextChannel verifyMessageTextChannel = event.getGuild()
                    .getTextChannelById(textChannel.replaceAll("[<#>]", ""));
            int responseCode = updateVerifySettings("messageTextChannelId", verifyMessageTextChannel.getId());
            if (responseCode != 200) {
                return defaultEmbed("Error", null).setDescription("API returned response code " + responseCode);
            }

            EmbedBuilder eb = defaultEmbed("Settings for " + event.getGuild().getName(), null);
            eb.setDescription("**Verify text channel set to:** " + verifyMessageTextChannel.getAsMention());
            return eb;
        } catch (Exception ignored) {
        }
        return defaultEmbed("Invalid Text Channel", null);
    }

    private EmbedBuilder setVerifyVerifiedRole(String verifyRole) {
        try {
            Role verifyGuildRole = event.getGuild().getRoleById(verifyRole.replaceAll("[<@&>]", ""));
            if (!(verifyGuildRole.isPublicRole() || verifyGuildRole.isManaged())) {
                int responseCode = updateVerifySettings("verifiedRole", verifyGuildRole.getId());
                if (responseCode != 200) {
                    return defaultEmbed("Error", null).setDescription("API returned response code " + responseCode);
                }

                EmbedBuilder eb = defaultEmbed("Settings for " + event.getGuild().getName(), null);
                eb.setDescription("**Verify role set to:** " + verifyGuildRole.getAsMention());
                return eb;
            }
        } catch (Exception ignored) {
        }
        return defaultEmbed("Invalid Role", null);
    }

    private EmbedBuilder setVerifyNickname(String nickname) {
        if (!nickname.contains("[IGN]")) {
            if (nickname.equalsIgnoreCase("none")) {
                int responseCode = updateVerifySettings("verifiedNickname", "none");

                if (responseCode != 200) {
                    return defaultEmbed("Error", null).setDescription("API returned response code " + responseCode);
                }

                EmbedBuilder eb = defaultEmbed("Settings for " + event.getGuild().getName(), null);
                eb.setDescription("**Verify nickname disabled**");
                return eb;
            }
            return defaultEmbed("Error").setDescription("Nickname must contain [IGN] parameter");
        }

        if (nickname.replace("[IGN]", "").length() > 15) {
            return defaultEmbed("Error")
                    .setDescription("Nickname prefix and/or postfix must be less than or equal to 15 letters");
        }

        int responseCode = updateVerifySettings("verifiedNickname", nickname);

        if (responseCode != 200) {
            return defaultEmbed("Error", null).setDescription("API returned response code " + responseCode);
        }

        EmbedBuilder eb = defaultEmbed("Settings for " + event.getGuild().getName(), null);
        eb.setDescription("**Verify nickname set to:** " + nickname);
        return eb;
    }

    private int updateVerifySettings(String key, String newValue) {
        JsonObject newVerifySettings = database.getVerifySettings(event.getGuild().getId()).getAsJsonObject();
        newVerifySettings.remove(key);
        newVerifySettings.addProperty(key, newValue);
        return database.updateVerifySettings(event.getGuild().getId(), newVerifySettings);
    }

    /* Apply Settings */
    private EmbedBuilder getCurrentApplySettings(JsonElement applySettings) {
        EmbedBuilder eb = defaultEmbed("Apply Settings");
        eb.setDescription("**" + displaySettings(applySettings, "enable") + "**");
        eb.addField("React Message Channel", displaySettings(applySettings, "messageTextChannelId"), true);
        eb.addField("Staff Message Channel", displaySettings(applySettings, "messageStaffChannelId"), true);
        eb.addField("Staff Ping Role", displaySettings(applySettings, "staffPingRoleId"), true);
        eb.addField("New Channel Prefix", displaySettings(applySettings, "newChannelPrefix"), true);
        eb.addField("New Channel Category", displaySettings(applySettings, "newChannelCategory"), true);
        eb.addBlankField(true);
        eb.addField("React Message Text", displaySettings(applySettings, "messageText"), true);
        eb.addField("Accepted Message", displaySettings(applySettings, "acceptMessageText"), true);
        eb.addField("Waitlisted Message", displaySettings(applySettings, "acceptMessageText"), true);
        eb.addField("Waitlisted Message", displaySettings(applySettings, "waitlistedMessageText"), true);
        eb.addField("Denied Message", displaySettings(applySettings, "denyMessageText"), true);
        eb.addField("Requirements", displaySettings(applySettings, "applyReqs"), true);
        return eb;
    }

    private boolean allowApplyEnable() {
        JsonObject currentSettings = database.getApplySettings(event.getGuild().getId()).getAsJsonObject();
        currentSettings.remove("previousMessageId");
        currentSettings.remove("applyUsersCache");
        currentSettings.remove("waitlistedMessageText");
        currentSettings.remove("applyReqs");

        for (String key : getJsonKeys(currentSettings)) {
            if (higherDepth(currentSettings, key).getAsString().length() == 0) {
                return false;
            }
        }
        return true;
    }

    private EmbedBuilder setApplyEnable(String enable) {
        if (enable.equalsIgnoreCase("true") || enable.equalsIgnoreCase("false")) {
            int responseCode = updateApplySettings("enable", enable);
            if (responseCode != 200) {
                return defaultEmbed("Error", null).setDescription("API returned response code " + responseCode);
            }

            EmbedBuilder eb = defaultEmbed("Settings for " + event.getGuild().getName(), null);
            eb.setDescription("**Apply:** " + (enable.equalsIgnoreCase("true") ? "enabled" : "disabled") + "\nRun `"
                    + BOT_PREFIX + "reload` to reload the settings");
            return eb;
        }
        return defaultEmbed("Invalid Input", null);
    }

    private EmbedBuilder setApplyMessageTextChannelId(String textChannel) {
        try {
            TextChannel applyMessageTextChannel = event.getGuild()
                    .getTextChannelById(textChannel.replaceAll("[<#>]", ""));

            int responseCode = updateApplySettings("messageTextChannelId", applyMessageTextChannel.getId());
            if (responseCode != 200) {
                return defaultEmbed("Error", null).setDescription("API returned response code " + responseCode);
            }

            EmbedBuilder eb = defaultEmbed("Settings for " + event.getGuild().getName(), null);
            eb.setDescription("**Apply text channel set to:** " + applyMessageTextChannel.getAsMention());
            return eb;
        } catch (Exception ignored) {
        }
        return defaultEmbed("Invalid Text Channel", null);
    }

    private EmbedBuilder setApplyMessageStaffChannelId(String textChannel) {
        try {
            TextChannel staffTextChannel = event.getGuild().getTextChannelById(textChannel.replaceAll("[<#>]", ""));
            int responseCode = updateApplySettings("messageStaffChannelId", staffTextChannel.getId());
            if (responseCode != 200) {
                return defaultEmbed("Error", null).setDescription("API returned response code " + responseCode);
            }

            EmbedBuilder eb = defaultEmbed("Settings for " + event.getGuild().getName(), null);
            eb.setDescription("**Apply staff channel set to:** " + staffTextChannel.getAsMention());
            return eb;
        } catch (Exception ignored) {
        }
        return defaultEmbed("Invalid Text Channel", null);
    }

    private EmbedBuilder setApplyNewChannelPrefix(String channelPrefix) {
        if (channelPrefix.length() > 0) {
            if (EmojiParser.parseToAliases(channelPrefix).length() > 25) {
                return defaultEmbed("Error", null).setDescription("Prefix cannot be longer than 25 letters!");
            }
            int responseCode = updateApplySettings("newChannelPrefix", EmojiParser.parseToAliases(channelPrefix));
            if (responseCode != 200) {
                return defaultEmbed("Error", null).setDescription("API returned response code " + responseCode);
            }

            EmbedBuilder eb = defaultEmbed("Settings for " + event.getGuild().getName(), null);
            eb.setDescription("**Apply new channel prefix set to:** " + channelPrefix);
            return eb;
        }
        return defaultEmbed("Invalid Input", null);
    }

    private EmbedBuilder setApplyMessageText(String verifyText) {
        if (verifyText.length() > 0) {
            if (EmojiParser.parseToAliases(verifyText).length() > 1500) {
                return defaultEmbed("Error", null).setDescription("Text cannot be longer than 1500 letters!");
            }
            int responseCode = updateApplySettings("messageText", EmojiParser.parseToAliases(verifyText));
            if (responseCode != 200) {
                return defaultEmbed("Error", null).setDescription("API returned response code " + responseCode);
            }

            EmbedBuilder eb = defaultEmbed("Settings for " + event.getGuild().getName(), null);
            eb.setDescription("**Apply message set to:** " + verifyText);
            return eb;
        }
        return defaultEmbed("Invalid Input", null);
    }

    private EmbedBuilder setApplyAcceptMessageText(String verifyText) {
        if (verifyText.length() > 0) {
            if (EmojiParser.parseToAliases(verifyText).length() > 1500) {
                return defaultEmbed("Error", null).setDescription("Text cannot be longer than 1500 letters!");
            }

            int responseCode = updateApplySettings("acceptMessageText", EmojiParser.parseToAliases(verifyText));
            if (responseCode != 200) {
                return defaultEmbed("Error", null).setDescription("API returned response code " + responseCode);
            }

            EmbedBuilder eb = defaultEmbed("Settings for " + event.getGuild().getName(), null);
            eb.setDescription("**Apply accept message set to:** " + verifyText);
            return eb;
        }
        return defaultEmbed("Invalid Input", null);
    }

    private EmbedBuilder setApplyWaitListMessageText(String verifyText) {
        if (verifyText.length() > 0) {
            if (verifyText.equalsIgnoreCase("none")) {
                int responseCode = updateVerifySettings("waitlistedMessageText", "none");

                if (responseCode != 200) {
                    return defaultEmbed("Error", null).setDescription("API returned response code " + responseCode);
                }

                EmbedBuilder eb = defaultEmbed("Settings for " + event.getGuild().getName(), null);
                eb.setDescription("**Waitlist message disabled**");
                return eb;
            }

            if (EmojiParser.parseToAliases(verifyText).length() > 1500) {
                return defaultEmbed("Error", null).setDescription("Text cannot be longer than 1500 letters!");
            }

            int responseCode = updateApplySettings("waitlistedMessageText", EmojiParser.parseToAliases(verifyText));
            if (responseCode != 200) {
                return defaultEmbed("Error", null).setDescription("API returned response code " + responseCode);
            }

            EmbedBuilder eb = defaultEmbed("Settings for " + event.getGuild().getName(), null);
            eb.setDescription("**Waitlisted message set to:** " + verifyText);
            return eb;
        }
        return defaultEmbed("Invalid Input", null);
    }

    private EmbedBuilder setApplyDenyMessageText(String denyText) {
        if (denyText.length() > 0) {
            if (EmojiParser.parseToAliases(denyText).length() > 1500) {
                return defaultEmbed("Error", null).setDescription("Text cannot be longer than 1500 letters!");
            }

            int responseCode = updateApplySettings("denyMessageText", EmojiParser.parseToAliases(denyText));
            if (responseCode != 200) {
                return defaultEmbed("Error", null).setDescription("API returned response code " + responseCode);
            }

            EmbedBuilder eb = defaultEmbed("Settings for " + event.getGuild().getName(), null);
            eb.setDescription("**Apply deny message set to:** " + denyText);
            return eb;
        }
        return defaultEmbed("Invalid Input", null);
    }

    private EmbedBuilder setApplyStaffPingRoleId(String staffPingRoleMention) {
        try {
            Role verifyGuildRole = event.getGuild().getRoleById(staffPingRoleMention.replaceAll("[<@&>]", ""));
            if (!(verifyGuildRole.isPublicRole() || verifyGuildRole.isManaged())) {
                int responseCode = updateApplySettings("staffPingRoleId", verifyGuildRole.getId());
                if (responseCode != 200) {
                    return defaultEmbed("Error", null).setDescription("API returned response code " + responseCode);
                }

                EmbedBuilder eb = defaultEmbed("Settings for " + event.getGuild().getName(), null);
                eb.setDescription("**Apply staff ping role set to:** " + verifyGuildRole.getAsMention());
                return eb;
            }
        } catch (Exception ignored) {
        }
        return defaultEmbed("Invalid Role", null);
    }

    private EmbedBuilder setApplyNewChannelCategory(String messageCategory) {
        try {
            net.dv8tion.jda.api.entities.Category applyCategory = event.getGuild()
                    .getCategoryById(messageCategory.replaceAll("[<#>]", ""));
            int responseCode = updateApplySettings("newChannelCategory", applyCategory.getId());
            if (responseCode != 200) {
                return defaultEmbed("Error", null).setDescription("API returned response code " + responseCode);
            }

            EmbedBuilder eb = defaultEmbed("Settings for " + event.getGuild().getName(), null);
            eb.setDescription("**Apply new channel category set to:** <#" + applyCategory.getId() + ">");
            return eb;
        } catch (Exception ignored) {
        }
        return defaultEmbed("Invalid Guild Category", null);
    }

    private EmbedBuilder removeApplyRequirement(String reqNumber) {
        JsonArray currentReqs;
        try {
            currentReqs = database.getApplyReqs(event.getGuild().getId()).getAsJsonArray();
        } catch (Exception ignored) {
            return defaultEmbed("Error").setDescription("Unable to get current settings");
        }

        try {
            JsonElement req = currentReqs.get(Integer.parseInt(reqNumber) - 1);
            currentReqs.remove(Integer.parseInt(reqNumber) - 1);

            int responseCode = database.updateApplyReqs(event.getGuild().getId(), currentReqs);

            if (responseCode != 200) {
                return defaultEmbed("Error", null).setDescription("API returned response code " + responseCode);
            }

            EmbedBuilder eb = defaultEmbed("Settings for " + event.getGuild().getName());
            eb.setDescription(
                    "**Removed application requirement of:**\n• Slayer - " + higherDepth(req, "slayerReq").getAsInt()
                            + "\n• Skills - " + higherDepth(req, "skillsReq").getAsInt() + "\n• Catacombs - "
                            + higherDepth(req, "catacombsReq").getAsInt() + "\n• Weight - "
                            + higherDepth(req, "weightReq").getAsInt());
            return eb;
        } catch (Exception ignored) {
            return defaultEmbed("Error").setDescription("Invalid requirement number. Run `" + BOT_PREFIX
                    + "settings apply` to see the current apply requirements");
        }
    }

    private EmbedBuilder addApplyRequirement(String reqArgs) {
        JsonArray currentReqs;
        try {
            currentReqs = database.getApplyReqs(event.getGuild().getId()).getAsJsonArray();
        } catch (Exception ignored) {
            return defaultEmbed("Error").setDescription("Unable to get current settings");
        }

        if (currentReqs.size() >= 3) {
            return defaultEmbed("Error").setDescription("You can only have up to 3 requirements");
        }

        int slayerReq = 0;
        int skillsReq = 0;
        int cataReq = 0;
        int weightReq = 0;

        try {
            slayerReq = Integer.parseInt(reqArgs.split("slayer-")[1].split(" ")[0]);
        } catch (Exception ignored) {
        }

        try {
            skillsReq = Integer.parseInt(reqArgs.split("skills-")[1].split(" ")[0]);
        } catch (Exception ignored) {
        }

        try {
            cataReq = Integer.parseInt(reqArgs.split("catacombs-")[1].split(" ")[0]);
        } catch (Exception ignored) {
        }

        try {
            weightReq = Integer.parseInt(reqArgs.split("weight-")[1].split(" ")[0]);
        } catch (Exception ignored) {
        }

        ApplyRequirements toAddReq = new ApplyRequirements("" + slayerReq, "" + skillsReq, "" + cataReq,
                "" + weightReq);

        currentReqs.add(new Gson().toJsonTree(toAddReq));

        int responseCode = database.updateApplyReqs(event.getGuild().getId(), currentReqs);

        if (responseCode != 200) {
            return defaultEmbed("Error", null).setDescription("API returned response code " + responseCode);
        }

        EmbedBuilder eb = defaultEmbed("Settings for " + event.getGuild().getName());
        eb.setDescription("**Application requirement added:**\n• Slayer - " + slayerReq + "\n• Skills - " + skillsReq
                + "\n• Catacombs - " + cataReq + "\n• Weight - " + weightReq);
        return eb;
    }

    private int updateApplySettings(String key, String newValue) {
        JsonObject newApplyJson = database.getApplySettings(event.getGuild().getId()).getAsJsonObject();
        newApplyJson.remove(key);
        newApplyJson.addProperty(key, newValue);
        return database.updateApplySettings(event.getGuild().getId(), newApplyJson);
    }

    /* Misc */
    private String displaySettings(JsonElement jsonSettings, String settingName) {
        if (higherDepth(jsonSettings, settingName) != null) {
            if (settingName.equals("applyReqs")) {
                JsonArray reqs = higherDepth(jsonSettings, settingName).getAsJsonArray();

                if (reqs.size() == 0) {
                    return "None";
                }

                StringBuilder reqsString = new StringBuilder("\n");
                for (int i = 0; i < reqs.size(); i++) {
                    JsonElement req = reqs.get(i);
                    String slayerReq = higherDepth(req, "slayerReq").getAsString();
                    String skillsReq = higherDepth(req, "skillsReq").getAsString();
                    String cataReq = higherDepth(req, "catacombsReq").getAsString();
                    String weightReq = higherDepth(req, "weightReq").getAsString();

                    reqsString.append("`").append(i + 1).append(")` ").append(slayerReq).append(" slayer and ")
                            .append(skillsReq).append(" skill average and ").append(cataReq).append(" cata and ")
                            .append(weightReq).append(" weight\n");
                }

                return reqsString.toString();
            }

            String currentSettingValue = higherDepth(jsonSettings, settingName).getAsString();
            if (currentSettingValue.length() > 0) {
                switch (settingName) {
                    case "messageTextChannelId":
                    case "messageStaffChannelId":
                        try {
                            return "<#" + currentSettingValue + ">";
                        } catch (PermissionException e) {
                            if (e.getMessage().contains("Missing permission")) {
                                return "Missing permission: " + e.getMessage().split("Missing permission: ")[1];
                            }
                        }
                        break;
                    case "verifiedRole":
                    case "staffPingRoleId":
                    case "roleId":
                        try {
                            return "<@&" + currentSettingValue + ">";
                        } catch (PermissionException e) {
                            if (e.getMessage().contains("Missing permission")) {
                                return "Missing permission: " + e.getMessage().split("Missing permission: ")[1];
                            }
                        }
                        break;
                    case "newChannelCategory":
                        try {
                            return "<#" + event.getGuild().getCategoryById(currentSettingValue).getId() + ">";
                        } catch (PermissionException e) {
                            if (e.getMessage().contains("Missing permission")) {
                                return "Missing permission: " + e.getMessage().split("Missing permission: ")[1];
                            }
                        }
                        break;
                    case "enable":
                        return currentSettingValue.equals("true") ? "• Enabled" : "• Disabled";
                    case "guildId":
                        try {
                            JsonElement guildJson = getJson("https://api.hypixel.net/guild?key=" + HYPIXEL_API_KEY
                                    + "&id=" + currentSettingValue);
                            return higherDepth(higherDepth(guildJson, "guild"), "name").getAsString();
                        } catch (Exception e) {
                            return "Error finding guild associated with " + currentSettingValue + " id";
                        }
                    case "enableGuildRole":
                        return currentSettingValue.equals("true") ? "• Guild role enabled" : "• Guild role disabled";
                    case "enableGuildRanks":
                        return currentSettingValue.equals("true") ? "• Guild ranks enabled" : "• Guild ranks disabled";
                }
                return currentSettingValue;
            }
        }
        return "None";
    }
}