package com.skyblockplus.settings;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jdautilities.commons.waiter.EventWaiter;
import com.skyblockplus.api.discordserversettings.automatedroles.RoleModel;
import com.skyblockplus.api.discordserversettings.automatedroles.RoleObject;
import com.skyblockplus.api.discordserversettings.settingsmanagers.ServerSettingsModel;
import com.skyblockplus.utils.CustomPaginator;
import com.vdurmont.emoji.EmojiParser;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.exceptions.PermissionException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.concurrent.TimeUnit;

import static com.skyblockplus.Main.database;
import static com.skyblockplus.utils.Utils.*;

public class SettingsCommand extends Command {
    private final EventWaiter waiter;
    private CommandEvent event;

    public SettingsCommand(EventWaiter waiter) {
        this.name = "settings";
        this.cooldown = globalCooldown + 1;
        this.userPermissions = new Permission[]{Permission.ADMINISTRATOR};
        this.waiter = waiter;
    }

    @Override
    protected void execute(CommandEvent event) {
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

        if (args.length == 1) {
            eb = defaultEmbed("Settings for " + event.getGuild().getName(), null);

            if (higherDepth(currentSettings, "automatedVerify") != null) {
                eb.addField("Verify Settings",
                        getCurrentVerifySettings(higherDepth(currentSettings, "automatedVerify")), false);
            } else {
                eb.addField("Verify Settings", "Error! Data not found", false);
            }

            if (higherDepth(currentSettings, "automatedApplication") != null) {
                eb.addField("Apply Settings",
                        getCurrentApplySettings(higherDepth(currentSettings, "automatedApplication")), false);
            } else {
                eb.addField("Apply Settings", "Error! Data not found", false);
            }

            if (higherDepth(currentSettings, "automatedRoles") != null) {
                eb.addField("Roles Settings",
                        "Use `" + BOT_PREFIX + "settings roles` to see the current roles settings", false);

            } else {
                eb.addField("Roles Settings", "Error! Data not found", false);
            }
        } else if (args.length >= 2 && args[1].equals("roles")) {
            if (args.length == 2) {
                eb = defaultEmbed("Settings for " + event.getGuild().getName(), null);
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
                        eb = defaultEmbed("Error", null).setDescription("No roles set!");
                    }
                } else if (args[2].equals("disable")) {
                    eb = setRolesEnable("false");
                } else {
                    eb = defaultEmbed("Error", null).setDescription("Invalid setting");
                }
            } else if (args.length == 4) {
                if (args[2].equals("enable")) {
                    eb = setRoleEnable(args[3], "true");
                } else if (args[2].equals("disable")) {
                    eb = setRoleEnable(args[3], "false");
                } else {
                    eb = defaultEmbed("Error", null).setDescription("Invalid setting");
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
                    eb = defaultEmbed("Error", null).setDescription("Invalid setting");
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
                    eb.addField("Apply Settings",
                            getCurrentApplySettings(higherDepth(currentSettings, "automatedApplication")), false);
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
                    case "deny_message":
                        eb = setApplyDenyMessageText(args[3]);
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
                        eb = defaultEmbed("Error", null)
                                .setDescription("All other verify settings must be set before " + "enabling verify!");
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
        } else if ((args.length == 4 || args.length == 5) && args[1].equals("guild")) {
            if (args.length == 4) {
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
                    default:
                        eb = defaultEmbed("Error").setDescription("Invalid setting");
                        break;
                }
            } else {
                eb = defaultEmbed("Error").setDescription("Invalid setting");
            }

        } else {
            eb = defaultEmbed("Error", null).setDescription("Invalid setting");
        }

        ebMessage.editMessage(eb.build()).queue();
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

    private EmbedBuilder setGuildRoleId(String guildName) {
        try {
            JsonElement guildJson = getJson("https://api.hypixel.net/guild?key=" + HYPIXEL_API_KEY + "&name=" + guildName.replace("_", "%20"));
            String guildId = higherDepth(higherDepth(guildJson, "guild"), "_id").getAsString();
            JsonObject currentSettings = database.getGuildRoleSettings(event.getGuild().getId()).getAsJsonObject();
            currentSettings.remove("guildId");
            currentSettings.addProperty("guildId", guildId);
            int responseCode = database.updateGuildRoleSettings(event.getGuild().getId(), currentSettings);
            if (responseCode != 200) {
                return defaultEmbed("Error", null).setDescription("API returned response code " + responseCode);
            }

            EmbedBuilder eb = defaultEmbed("Settings for " + event.getGuild().getName(), null);
            eb.setDescription("**Guild set to:** " + higherDepth(higherDepth(guildJson, "guild"), "name").getAsString());
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
    private CustomPaginator.Builder getCurrentRolesSettings(JsonElement rolesSettings) {
        CustomPaginator.Builder paginateBuilder = new CustomPaginator.Builder().setColumns(1).setItemsPerPage(1)
                .showPageNumbers(true).useNumberedItems(false).setFinalAction(m -> {
                    try {
                        m.clearReactions().queue();
                    } catch (PermissionException ex) {
                        m.delete().queue();
                    }
                }).setEventWaiter(waiter).setTimeout(30, TimeUnit.SECONDS).setColor(botColor)
                .setCommandUser(event.getAuthor());
        ArrayList<String> pageTitles = new ArrayList<>();
        pageTitles.add("Roles Settings");

        ArrayList<String> roleNames = getJsonKeys(rolesSettings);

        paginateBuilder.addItems(("**Automated Roles "
                + (higherDepth(rolesSettings, "enable").getAsString().equals("true") ? "Enabled" : "Disabled") + "**"));
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
                    ebFieldString.append("**Member role for Hypixel guilds**\nExample: `").append(BOT_PREFIX).append("settings roles add guild_member skyblock_forceful @sbf guild member`\n");
                    break;
                }
                case "sven": {
                    ebFieldString.append("**A player's sven packmaster slayer xp**\nExample: `").append(BOT_PREFIX).append("settings roles add sven 1000000 @sven 9`\n");
                    break;
                }
                case "rev": {
                    ebFieldString.append("**A player's revenant horror xp slayer**\nExample: `").append(BOT_PREFIX).append("settings roles add rev 400000 @rev 8`\n");
                    break;
                }
                case "tara": {
                    ebFieldString.append("**A player's tarantula broodfather slayer xp**\nExample: `").append(BOT_PREFIX).append("settings roles add tara 100000 @tara 7`\n");
                    break;
                }
                case "bank_coins": {
                    ebFieldString.append("**Coins in a player's bank**\nExample: `").append(BOT_PREFIX).append("settings roles add bank_coins 1000000 @millionaire`\n");
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
                case "catacombs": {
                    ebFieldString.append("**A player's ").append(roleName).append(" level**\nExample: `").append(BOT_PREFIX).append("settings roles add ").append(roleName).append(" 30 @").append(roleName).append(" 30`\n");
                    break;
                }
                case "fairy_souls": {
                    ebFieldString.append("**Amount of collected fairy souls**\nExample: `").append(BOT_PREFIX).append("settings roles add fairy_souls 50 @50 souls collected`\n");
                    break;
                }
                case "slot_collector": {
                    ebFieldString.append("**Number of minion slots excluding upgrades (__not fully working__)**\nExample: `").append(BOT_PREFIX).append("settings roles add slot_collector 24 @maxed minion slots`\n");
                    break;
                }
                case "pet_enthusiast": {
                    ebFieldString.append("**Having a level 100 epic or legendary pet that is not an enchanting or alchemy pet**\nExample: `").append(BOT_PREFIX).append("settings roles set pet_enthusiast @level 100 pet`\n");
                    break;
                }
                case "doom_slayer": {
                    ebFieldString.append("**Having at least one level nine slayer**\nExample: `").append(BOT_PREFIX).append("settings roles set doom_slayer @level nine slayer`\n");
                    break;
                }
            }

            ebFieldString.append("\nCurrent Settings:\n");

            ebFieldString.append(higherDepth(currentRoleSettings, "enable").getAsString().equals("true") ? "• Enabled"
                    : "• Disabled");
            if (roleName.equals("doom_slayer") || roleName.equals("pet_enthusiast")) {
                try {
                    ebFieldString.append("\n• default - ").append(event.getGuild().getRoleById(higherDepth(higherDepth(currentRoleSettings, "levels").getAsJsonArray().get(0), "roleId").getAsString()).getAsMention());
                } catch (Exception ignored) {
                }
                pageTitles.add(roleName + " (__one level role__)");
            } else {
                ebFieldString.append(higherDepth(currentRoleSettings, "stackable").getAsString().equals("true")
                        ? "\n• Stackable"
                        : "\n• Not stackable");

                if (roleName.equals("guild_member")) {
                    for (JsonElement roleLevel : higherDepth(currentRoleSettings, "levels").getAsJsonArray()) {
                        String guildId = higherDepth(roleLevel, "value").getAsString();
                        JsonElement guildJson = getJson("https://api.hypixel.net/guild?key=" + HYPIXEL_API_KEY + "&id=" + guildId);
                        ebFieldString.append("\n• ").append(higherDepth(higherDepth(guildJson, "guild"), "name").getAsString()).append(" - ").append(event.getGuild().getRoleById(higherDepth(roleLevel, "roleId").getAsString()).getAsMention());
                    }
                } else {
                    for (JsonElement roleLevel : higherDepth(currentRoleSettings, "levels").getAsJsonArray()) {
                        ebFieldString.append("\n• ").append(higherDepth(roleLevel, "value").getAsString()).append(" - ").append(event.getGuild().getRoleById(higherDepth(roleLevel, "roleId").getAsString()).getAsMention());
                    }
                }

                if (higherDepth(currentRoleSettings, "levels").getAsJsonArray().size() == 0) {
                    ebFieldString.append("\n• No levels set");
                }

                pageTitles.add(roleName);
            }
            paginateBuilder.addItems(ebFieldString.toString());
        }

        return paginateBuilder.setPageTitles(pageTitles.toArray(new String[0]));
    }

    private boolean allowRolesEnable() {
        JsonObject currentSettings = database.getRolesSettings(event.getGuild().getId())
                .getAsJsonObject();
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
            JsonObject newRolesJson = database.getRolesSettings(event.getGuild().getId())
                    .getAsJsonObject();
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
        JsonObject currentRoleSettings = database.getRoleSettings(event.getGuild().getId(), roleName).getAsJsonObject();
        if (currentRoleSettings == null) {
            return defaultEmbed("Error", null).setDescription("Invalid role name");
        }

        if (currentRoleSettings.get("levels").getAsJsonArray().size() != 0) {
            currentRoleSettings.remove("enable");
            currentRoleSettings.addProperty("enable", enable);
            int responseCode = database.updateRoleSettings(event.getGuild().getId(), roleName,
                    currentRoleSettings);
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
            database.updateRoleSettings(event.getGuild().getId(), roleName,
                    currentRoleSettings);
        }
        EmbedBuilder eb = defaultEmbed("Error", null);
        eb.setDescription("Specified role must have at least one configuration!");
        return eb;
    }

    private EmbedBuilder addRoleLevel(String roleType, String roleValue, String roleMention) {
        String guildName = "";
        if (roleType.equals("guild_member")) {
            try {
                JsonElement guildJson = getJson("https://api.hypixel.net/guild?key=" + HYPIXEL_API_KEY + "&name=" + roleValue.replace("_", "%20"));
                roleValue = higherDepth(higherDepth(guildJson, "guild"), "_id").getAsString();
                guildName = higherDepth(higherDepth(guildJson, "guild"), "name").getAsString();
            } catch (Exception e) {
                return defaultEmbed("Error", null).setDescription("Invalid username");
            }
        } else if (roleType.equals("pet_enthusiast") || roleType.equals("doom_slayer")) {
            return defaultEmbed("These roles do not support levels. Use `" + BOT_PREFIX + "settings roles set [roleName] [@role]` instead");
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
            newRoleSettings = database.getRoleSettings(event.getGuild().getId(), roleType)
                    .getAsJsonObject();
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
        Arrays.sort(temp, Comparator.comparing(RoleObject::getValue));
        currentLevels = gson.toJsonTree(temp).getAsJsonArray();

        newRoleSettings.remove("levels");
        newRoleSettings.add("levels", currentLevels);

        int responseCode = database.updateRoleSettings(event.getGuild().getId(), roleType,
                newRoleSettings);

        if (responseCode != 200) {
            return defaultEmbed("Error", null).setDescription("API returned response code " + responseCode);
        }

        if (roleType.equals("guild_member")) {
            roleValue = guildName;
        }

        return defaultEmbed("Settings for " + event.getGuild().getName(), null)
                .setDescription(roleType + " " + roleValue + " set to " + role.getAsMention());
    }

    private EmbedBuilder removeRoleLevel(String roleName, String value) {
        if (roleName.equals("pet_enthusiast") || roleName.equals("doom_slayer")) {
            return defaultEmbed("These roles do not support levels. Use `" + BOT_PREFIX + "settings roles set [roleName] [@role]` instead");
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
                JsonElement guildJson = getJson("https://api.hypixel.net/guild?key=" + HYPIXEL_API_KEY + "&id=" + higherDepth(level, "value").getAsString());
                currentValue = higherDepth(higherDepth(guildJson, "guild"), "name").getAsString();
            }

            if (currentValue.equalsIgnoreCase(value.replace("_", " "))) {
                currentLevels.remove(level);
                currentRoleSettings.remove("levels");
                currentRoleSettings.add("levels", currentLevels);
                int responseCode = database.updateRoleSettings(event.getGuild().getId(), roleName,
                        currentRoleSettings);
                if (responseCode != 200) {
                    return defaultEmbed("Error", null).setDescription("API returned response code " + responseCode);
                }

                currentRoleSettings = database.getRoleSettings(event.getGuild().getId(), roleName)
                        .getAsJsonObject();

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
        if (roleName.equals("pet_enthusiast") || roleName.equals("doom_slayer")) {
            return defaultEmbed("Error").setDescription("This role does not support stacking");
        }

        JsonObject currentRoleSettings = database.getRoleSettings(event.getGuild().getId(), roleName).getAsJsonObject();
        currentRoleSettings.remove("stackable");
        currentRoleSettings.addProperty("stackable", stackable);
        int responseCode = database.updateRoleSettings(event.getGuild().getId(), roleName,
                currentRoleSettings);
        if (responseCode != 200) {
            return defaultEmbed("Error", null).setDescription("API returned response code " + responseCode);
        }

        EmbedBuilder eb = defaultEmbed("Settings for " + event.getGuild().getName(), null);
        eb.setDescription(
                "**" + roleName + " role:** " + (stackable.equalsIgnoreCase("true") ? "stackable" : "not stackable"));
        return eb;
    }

    private EmbedBuilder setOneLevelRole(String roleName, String roleMention) {
        if (!roleName.equals("pet_enthusiast") && !roleName.equals("doom_slayer")) {
            return defaultEmbed("Error").setDescription("This role does is not a one level role. Use `" + BOT_PREFIX + "settings roles add [roleName] [value] [@role]` instead");
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
            newRoleSettings = database.getRoleSettings(event.getGuild().getId(), roleName)
                    .getAsJsonObject();
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

        int responseCode = database.updateRoleSettings(event.getGuild().getId(), roleName,
                newRoleSettings);

        if (responseCode != 200) {
            return defaultEmbed("Error", null).setDescription("API returned response code " + responseCode);
        }

        return defaultEmbed("Settings for " + event.getGuild().getName(), null)
                .setDescription(roleName + " set to " + role.getAsMention());
    }

    /* Verify Settings */
    private String getCurrentVerifySettings(JsonElement verifySettings) {
        String ebFieldString = "";
        ebFieldString += higherDepth(verifySettings, "enable").getAsString().equals("true") ? "• Enabled"
                : "• Disabled";
        ebFieldString += "\n• React Message Text: "
                + (higherDepth(verifySettings, "messageText").getAsString().length() != 0
                ? higherDepth(verifySettings, "messageText").getAsString()
                : "None");
        ebFieldString += "\n• React Message Channel: "
                + (higherDepth(verifySettings, "messageTextChannelId").getAsString().length() != 0 ? event.getGuild()
                .getTextChannelById(higherDepth(verifySettings, "messageTextChannelId").getAsString())
                .getAsMention() : "None");
        ebFieldString += "\n• Verified Role: "
                + (higherDepth(verifySettings, "verifiedRole").getAsString().length() != 0 ? event.getGuild()
                .getRoleById(higherDepth(verifySettings, "verifiedRole").getAsString()).getAsMention()
                : "None");
        ebFieldString += "\n• Nickname Template: "
                + ((higherDepth(verifySettings, "verifiedNickname").getAsString().length() != 0) && (!higherDepth(verifySettings, "verifiedNickname").getAsString().equalsIgnoreCase("none"))
                ? higherDepth(verifySettings, "verifiedNickname").getAsString()
                : "None");
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
            eb.setDescription("**Verify:** " + (enable.equalsIgnoreCase("true") ? "enabled" : "disabled"));
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
        if(!nickname.contains("[IGN]")){
            if(nickname.equalsIgnoreCase("none")){
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

        if(nickname.replace("[IGN]", "") .length() > 15){
            return defaultEmbed("Error").setDescription("Nickname prefix and/or postfix must be less than or equal to 15 letters");
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
        JsonObject newVerifySettings = database.getVerifySettings(event.getGuild().getId())
                .getAsJsonObject();
        newVerifySettings.remove(key);
        newVerifySettings.addProperty(key, newValue);
        return database.updateVerifySettings(event.getGuild().getId(), newVerifySettings);
    }

    /* Apply Settings */
    private String getCurrentApplySettings(JsonElement applySettings) {
        String ebFieldString = "";
        ebFieldString += higherDepth(applySettings, "enable").getAsString().equals("true") ? "• Enabled" : "• Disabled";
        ebFieldString += "\n• React Message Text: "
                + (higherDepth(applySettings, "messageText").getAsString().length() != 0
                ? higherDepth(applySettings, "messageText").getAsString()
                : "None");
        ebFieldString += "\n• React Message Channel: "
                + (higherDepth(applySettings, "messageTextChannelId").getAsString().length() != 0 ? event.getGuild()
                .getTextChannelById(higherDepth(applySettings, "messageTextChannelId").getAsString())
                .getAsMention() : "None");
        ebFieldString += "\n• Staff Message Channel: "
                + (higherDepth(applySettings, "messageStaffChannelId").getAsString().length() != 0 ? event.getGuild()
                .getTextChannelById(higherDepth(applySettings, "messageStaffChannelId").getAsString())
                .getAsMention() : "None");
        ebFieldString += "\n• Staff Ping Role: "
                + (higherDepth(applySettings, "staffPingRoleId").getAsString().length() != 0 ? event.getGuild()
                .getRoleById(higherDepth(applySettings, "staffPingRoleId").getAsString()).getAsMention()
                : "None");
        ebFieldString += "\n• Accepted Message: "
                + (higherDepth(applySettings, "acceptMessageText").getAsString().length() != 0
                ? higherDepth(applySettings, "acceptMessageText").getAsString()
                : "None");
        ebFieldString += "\n• Denied Message: "
                + (higherDepth(applySettings, "denyMessageText").getAsString().length() != 0
                ? higherDepth(applySettings, "denyMessageText").getAsString()
                : "None");
        ebFieldString += "\n• New Channel Prefix: "
                + (higherDepth(applySettings, "newChannelPrefix").getAsString().length() != 0
                ? higherDepth(applySettings, "newChannelPrefix").getAsString()
                : "None");
        ebFieldString += "\n• New Channel Category: "
                + (higherDepth(applySettings, "newChannelCategory").getAsString().length() != 0
                ? ("<#" + event.getGuild().getCategoryById(higherDepth(applySettings, "newChannelCategory").getAsString()).getId() + ">")
                : "None");
        return ebFieldString;
    }

    private boolean allowApplyEnable() {
        JsonObject currentSettings = database.getApplySettings(event.getGuild().getId()).getAsJsonObject();
        currentSettings.remove("previousMessageId");

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
            eb.setDescription("**Apply:** " + (enable.equalsIgnoreCase("true") ? "enabled" : "disabled"));
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

    private EmbedBuilder setApplyDenyMessageText(String verifyText) {
        if (verifyText.length() > 0) {
            if (EmojiParser.parseToAliases(verifyText).length() > 1500) {
                return defaultEmbed("Error", null).setDescription("Text cannot be longer than 1500 letters!");
            }

            int responseCode = updateApplySettings("denyMessageText", EmojiParser.parseToAliases(verifyText));
            if (responseCode != 200) {
                return defaultEmbed("Error", null).setDescription("API returned response code " + responseCode);
            }

            EmbedBuilder eb = defaultEmbed("Settings for " + event.getGuild().getName(), null);
            eb.setDescription("**Apply deny message set to:** " + verifyText);
            return eb;
        }
        return defaultEmbed("Invalid Input", null);
    }

    private EmbedBuilder setApplyStaffPingRoleId(String verifyRole) {
        try {
            Role verifyGuildRole = event.getGuild().getRoleById(verifyRole.replaceAll("[<@&>]", ""));
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

    private int updateApplySettings(String key, String newValue) {
        JsonObject newApplyJson = database.getApplySettings(event.getGuild().getId()).getAsJsonObject();
        newApplyJson.remove(key);
        newApplyJson.addProperty(key, newValue);
        return database.updateApplySettings(event.getGuild().getId(), newApplyJson);
    }
}