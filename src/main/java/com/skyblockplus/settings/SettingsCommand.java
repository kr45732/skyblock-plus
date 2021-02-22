package com.skyblockplus.settings;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jdautilities.commons.waiter.EventWaiter;
import com.skyblockplus.api.discordserversettings.automatedroles.RoleObject;
import com.skyblockplus.api.discordserversettings.settingsmanagers.ServerSettingsModel;
import com.skyblockplus.utils.CustomPaginator;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.exceptions.PermissionException;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import static com.skyblockplus.utils.BotUtils.*;

public class SettingsCommand extends Command {
    private final String baseUrl = API_BASE_URL + "api/discord/serverSettings/";
    private CommandEvent event;
    private EventWaiter waiter;

    public SettingsCommand(EventWaiter waiter) {
        this.name = "settings";
        this.cooldown = globalCooldown;
        this.userPermissions = new Permission[] { Permission.MANAGE_SERVER };
        this.waiter = waiter;
    }

    @Override
    protected void execute(CommandEvent event) {
        this.event = event;
        EmbedBuilder eb = defaultEmbed("Loading...", null);
        Message ebMessage = event.getChannel().sendMessage(eb.build()).complete();
        String content = event.getMessage().getContentRaw();
        String[] args = content.split(" ");

        System.out.println(content);

        JsonElement currentSettings = getJson(baseUrl + "get/byId?serverId=" + event.getGuild().getId());
        if (higherDepth(currentSettings, "serverId") == null) {
            postJson(baseUrl + "add/byId?serverId=" + event.getGuild().getId(),
                    new ServerSettingsModel(event.getGuild().getName(), event.getGuild().getId()));
            currentSettings = getJson(baseUrl + "get/byId?serverId=" + event.getGuild().getId());
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
                if (args[2].equals("stackable") && args[3].equals("true")) {
                    eb = setRoleStackable(args[4], "true");
                } else if (args[2].equals("stackable") && args[3].equals("false")) {
                    eb = setRoleStackable(args[4], "false");
                } else if (args[2].equals("remove")) {
                    eb = removeRoleLevel(args[3], args[4]);
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
                    case "prefix":
                        eb = setVerifyNewChannelPrefix(args[3]);
                        break;
                    case "category":
                        eb = setVerifyNewChannelCategory(args[3]);
                        break;
                    default:
                        eb = defaultEmbed("Error", null).setDescription("Invalid setting");
                        break;
                }
            }
        } else {
            eb = defaultEmbed("Error", null).setDescription("Invalid setting");
        }

        ebMessage.editMessage(eb.build()).queue();
    }

    private EmbedBuilder removeRoleLevel(String roleName, String value) {
        JsonObject currentRoleSettings = getJson(
                baseUrl + "get/role?serverId=" + event.getGuild().getId() + "&roleName=" + roleName).getAsJsonObject();
        JsonArray currentLevels = currentRoleSettings.get("levels").getAsJsonArray();
        for (JsonElement level : currentLevels) {
            if (higherDepth(level, "value").getAsString().equals(value)) {
                currentLevels.remove(level);
                currentRoleSettings.remove("levels");
                currentRoleSettings.add("levels", currentLevels);
                int responseCode = postJson(
                        baseUrl + "update/role?serverId=" + event.getGuild().getId() + "&roleName=" + roleName,
                        currentRoleSettings);
                if (responseCode != 200) {
                    return defaultEmbed("Error", null).setDescription("API returned response code " + responseCode);
                }

                currentRoleSettings = getJson(
                        baseUrl + "get/role?serverId=" + event.getGuild().getId() + "&roleName=" + roleName)
                                .getAsJsonObject();

                if (currentRoleSettings.get("levels").getAsJsonArray().size() == 0) {
                    setRoleEnable(roleName, "false");
                }

                if (!allowRolesEnable())
                    setRolesEnable("false");

                return defaultEmbed("Settings for " + event.getGuild().getName(), null)
                        .setDescription(roleName + " " + value + " removed");
            }
        }
        return defaultEmbed("Error", null).setDescription("Invalid role value");
    }

    private EmbedBuilder setRoleEnable(String roleName, String enable) {
        JsonObject currentRoleSettings = getJson(
                baseUrl + "get/role?serverId=" + event.getGuild().getId() + "&roleName=" + roleName).getAsJsonObject();
        if (currentRoleSettings.get("levels").getAsJsonArray().size() != 0) {
            currentRoleSettings.remove("enable");
            currentRoleSettings.addProperty("enable", enable);
            int responseCode = postJson(
                    baseUrl + "update/role?serverId=" + event.getGuild().getId() + "&roleName=" + roleName,
                    currentRoleSettings);
            if (responseCode != 200) {
                return defaultEmbed("Error", null).setDescription("API returned response code " + responseCode);
            }

            EmbedBuilder eb = defaultEmbed("Settings for " + event.getGuild().getName(), null);
            eb.setDescription(
                    "**" + roleName + " role:** " + (enable.equalsIgnoreCase("true") ? "enabled" : "disabled"));
            return eb;
        }
        EmbedBuilder eb = defaultEmbed("Error", null);
        eb.setDescription("Specified role must have at least one configuration!");
        return eb;
    }

    private EmbedBuilder setRoleStackable(String roleName, String stackable) {
        JsonObject currentRoleSettings = getJson(
                baseUrl + "get/role?serverId=" + event.getGuild().getId() + "&roleName=" + roleName).getAsJsonObject();
        currentRoleSettings.remove("stackable");
        currentRoleSettings.addProperty("stackable", stackable);
        int responseCode = postJson(
                baseUrl + "update/role?serverId=" + event.getGuild().getId() + "&roleName=" + roleName,
                currentRoleSettings);
        if (responseCode != 200) {
            return defaultEmbed("Error", null).setDescription("API returned response code " + responseCode);
        }

        EmbedBuilder eb = defaultEmbed("Settings for " + event.getGuild().getName(), null);
        eb.setDescription(
                "**" + roleName + " role:** " + (stackable.equalsIgnoreCase("true") ? "stackable" : "not stackable"));
        return eb;
    }

    private EmbedBuilder addRoleLevel(String roleType, String roleValue, String roleMention) {
        Role role = event.getGuild().getRoleById(roleMention.replaceAll("[<@&>]", ""));
        if (role == null) {
            return defaultEmbed("Error", null).setDescription("Invalid role mention");
        }

        if (role.isPublicRole() || role.isManaged()) {
            return defaultEmbed("Error", null).setDescription("Role cannot be managed or @everyone!");
        }
        JsonObject newRoleSettings = null;
        try {
            newRoleSettings = getJson(
                    baseUrl + "get/role?serverId=" + event.getGuild().getId() + "&roleName=" + roleType)
                            .getAsJsonObject();
        } catch (Exception e) {
            return defaultEmbed("Error", null).setDescription("Invalid role");
        }
        JsonArray currentLevels = newRoleSettings.get("levels").getAsJsonArray();
        for (JsonElement level : currentLevels) {
            if (higherDepth(level, "value").getAsString().equals(roleValue)) {
                currentLevels.remove(level);
                break;
            }
        }

        currentLevels.add(new Gson().toJsonTree(new RoleObject(roleValue, role.getId())));
        newRoleSettings.remove("levels");
        newRoleSettings.add("levels", currentLevels);

        int responseCode = postJson(
                baseUrl + "update/role?serverId=" + event.getGuild().getId() + "&roleName=" + roleType,
                newRoleSettings);
        if (responseCode != 200) {
            return defaultEmbed("Error", null).setDescription("API returned response code " + responseCode);
        }

        return defaultEmbed("Settings for " + event.getGuild().getName(), null)
                .setDescription(roleType + " " + roleValue + " set to " + role.getAsMention());
    }

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
        ebFieldString += "\n• New Channel Prefix: "
                + (higherDepth(verifySettings, "newChannelPrefix").getAsString().length() != 0
                        ? higherDepth(verifySettings, "newChannelPrefix").getAsString()
                        : "None");
        ebFieldString += "\n• New Channel Category: "
                + (higherDepth(verifySettings, "newChannelCategory").getAsString().length() != 0
                        ? higherDepth(verifySettings, "newChannelCategory").getAsString()
                        : "None");
        return ebFieldString;
    }

    private CustomPaginator.Builder getCurrentRolesSettings(JsonElement rolesSettings) {
        CustomPaginator.Builder paginateBuilder = new CustomPaginator.Builder().setColumns(1).setItemsPerPage(1)
                .showPageNumbers(true).useNumberedItems(false).setFinalAction(m -> {
                    try {
                        m.clearReactions().queue();
                    } catch (PermissionException ex) {
                        m.delete().queue();
                    }
                }).setEventWaiter(waiter).setTimeout(30, TimeUnit.SECONDS).wrapPageEnds(true).setColor(botColor)
                .setCommandUser(event.getAuthor());
        ArrayList<String> pageTitles = new ArrayList<>();
        pageTitles.add("Roles Settings");

        ArrayList<String> roleNames = getJsonKeys(rolesSettings);

        paginateBuilder.addItems(("**Automated Roles "
                + (higherDepth(rolesSettings, "enable").getAsString().equals("true") ? "Enabled" : "Disabled") + "**"));
        roleNames.remove("enable");
        for (String roleName : roleNames) {
            JsonElement currentRoleSettings = higherDepth(rolesSettings, roleName);
            String ebFieldString = "";
            ebFieldString += higherDepth(currentRoleSettings, "enable").getAsString().equals("true") ? "• Enabled"
                    : "• Disabled";
            ebFieldString += higherDepth(currentRoleSettings, "stackable").getAsString().equals("stackable")
                    ? "\n• Stackable"
                    : "\n• Not stackable";

            for (JsonElement roleLevel : higherDepth(currentRoleSettings, "levels").getAsJsonArray()) {
                ebFieldString += "\n• " + higherDepth(roleLevel, "value").getAsString() + " - "
                        + event.getGuild().getRoleById(higherDepth(roleLevel, "roleId").getAsString()).getAsMention();
            }
            paginateBuilder.addItems(ebFieldString);
            pageTitles.add(roleName);
        }

        return paginateBuilder.setPageTitles(pageTitles.toArray(new String[0]));
    }

    private boolean allowRolesEnable() {
        JsonObject currentSettings = getJson(baseUrl + "get/roles?serverId=" + event.getGuild().getId())
                .getAsJsonObject();
        currentSettings.remove("enable");
        for (String role : getJsonKeys(currentSettings)) {
            if (higherDepth(higherDepth(currentSettings, role), "enable").getAsBoolean()) {
                return true;
            }
        }
        return false;
    }

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
                        ? higherDepth(applySettings, "newChannelCategory").getAsString()
                        : "None");
        return ebFieldString;
    }

    private EmbedBuilder setRolesEnable(String enable) {
        if (enable.equalsIgnoreCase("true") || enable.equalsIgnoreCase("false")) {
            JsonObject newRolesJson = getJson(baseUrl + "get/roles?serverId=" + event.getGuild().getId())
                    .getAsJsonObject();
            newRolesJson.remove("enable");
            newRolesJson.addProperty("enable", enable);
            int responseCode = postJson(baseUrl + "update/roles?serverId=" + event.getGuild().getId(), newRolesJson);
            if (responseCode != 200) {
                return defaultEmbed("Error", null).setDescription("API returned response code " + responseCode);
            }

            EmbedBuilder eb = defaultEmbed("Settings for " + event.getGuild().getName(), null);
            eb.setDescription("**Roles:** " + (enable.equalsIgnoreCase("true") ? "enabled" : "disabled"));
            return eb;
        }
        return defaultEmbed("Invalid Input", null);
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
            int responseCode = updateApplySettings("newChannelPrefix", channelPrefix);
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
            int responseCode = updateApplySettings("messageText", verifyText);
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
            int responseCode = updateApplySettings("acceptMessageText", verifyText);
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
            int responseCode = updateApplySettings("denyMessageText", verifyText);
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
            int responseCode = updateVerifySettings("messageText", verifyText);
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

    private EmbedBuilder setVerifyNewChannelPrefix(String channelPrefix) {
        if (channelPrefix.length() > 0) {
            int responseCode = updateVerifySettings("newChannelPrefix", channelPrefix);
            if (responseCode != 200) {
                return defaultEmbed("Error", null).setDescription("API returned response code " + responseCode);
            }

            EmbedBuilder eb = defaultEmbed("Settings for " + event.getGuild().getName(), null);
            eb.setDescription("**Verify new channel prefix set to:** " + channelPrefix);
            return eb;
        }
        return defaultEmbed("Invalid Input", null);
    }

    private EmbedBuilder setVerifyNewChannelCategory(String messageCategory) {
        try {
            net.dv8tion.jda.api.entities.Category verifyCategory = event.getGuild()
                    .getCategoryById(messageCategory.replaceAll("[<#>]", ""));
            int responseCode = updateVerifySettings("newChannelCategory", verifyCategory.getId());
            if (responseCode != 200) {
                return defaultEmbed("Error", null).setDescription("API returned response code " + responseCode);
            }

            EmbedBuilder eb = defaultEmbed("Settings for " + event.getGuild().getName(), null);
            eb.setDescription("**Verify category set to:** <#" + verifyCategory.getId() + ">");
            return eb;
        } catch (Exception ignored) {
        }
        return defaultEmbed("Invalid Guild Category", null);
    }

    private int updateVerifySettings(String key, String newValue) {
        JsonObject newVerifySettings = getJson(baseUrl + "get/verify?serverId=" + event.getGuild().getId())
                .getAsJsonObject();
        newVerifySettings.remove(key);
        newVerifySettings.addProperty(key, newValue);
        return postJson(baseUrl + "update/verify?serverId=" + event.getGuild().getId(), newVerifySettings);
    }

    private int updateApplySettings(String key, String newValue) {
        JsonObject newApplyJson = getJson(baseUrl + "get/apply?serverId=" + event.getGuild().getId()).getAsJsonObject();
        newApplyJson.remove(key);
        newApplyJson.addProperty(key, newValue);
        return postJson(baseUrl + "update/apply?serverId=" + event.getGuild().getId(), newApplyJson);
    }

    private boolean allowVerifyEnable() {
        JsonElement currentSettings = getJson(baseUrl + "get/verify?serverId=" + event.getGuild().getId());
        for (String key : getJsonKeys(currentSettings)) {
            if (higherDepth(currentSettings, key).getAsString().length() == 0) {
                return false;
            }
        }
        return true;
    }

    private boolean allowApplyEnable() {
        JsonElement currentSettings = getJson(baseUrl + "get/apply?serverId=" + event.getGuild().getId());
        for (String key : getJsonKeys(currentSettings)) {
            if (higherDepth(currentSettings, key).getAsString().length() == 0) {
                return false;
            }
        }
        return true;
    }
}
