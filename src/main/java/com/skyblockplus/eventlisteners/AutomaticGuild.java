package com.skyblockplus.eventlisteners;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.skyblockplus.eventlisteners.apply.ApplyGuild;
import com.skyblockplus.eventlisteners.verify.VerifyGuild;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.channel.text.TextChannelDeleteEvent;
import net.dv8tion.jda.api.events.guild.GuildReadyEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static com.skyblockplus.Main.database;
import static com.skyblockplus.Main.jda;
import static com.skyblockplus.utils.Utils.*;

public class AutomaticGuild {
    private final String guildId;
    private ApplyGuild applyGuild = new ApplyGuild(false);
    private VerifyGuild verifyGuild = new VerifyGuild(false);

    public AutomaticGuild(GuildReadyEvent event) {
        applyConstructor(event);
        verifyConstructor(event);
        guildRoleConstructor();
        guildId = event.getGuild().getId();
    }

    public ApplyGuild getApplyGuild() {
        return applyGuild;
    }

    public void guildRoleConstructor() {
        Runnable channelDeleter = this::updateGuildRoles;
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        int randomDelay = (int) (Math.random() * 360);
        scheduler.scheduleAtFixedRate(channelDeleter, randomDelay, 180, TimeUnit.MINUTES);
    }

    public void updateGuildRoles() {
        Guild guild = jda.getGuildById(guildId);
        JsonElement currentSettings = database.getGuildRoleSettings(guild.getId());

        if (currentSettings == null) {
            return;
        }

        if (higherDepth(currentSettings, "enableGuildRole").getAsBoolean()) {
            long startTime = System.currentTimeMillis();

            Role role = guild.getRoleById(higherDepth(currentSettings, "roleId").getAsString());
            JsonElement guildJson = getJson("https://api.hypixel.net/guild?key=" + HYPIXEL_API_KEY + "&id=" + higherDepth(currentSettings, "guildId").getAsString());

            if (role == null || guildJson.isJsonNull()) {
                return;
            }

            JsonArray guildMembers = higherDepth(higherDepth(guildJson, "guild"), "members").getAsJsonArray();
            List<String> guildMembersUuids = new ArrayList<>();

            for (JsonElement guildMember : guildMembers) {
                guildMembersUuids.add(higherDepth(guildMember, "uuid").getAsString());
            }

            JsonArray linkedUsers = database.getLinkedUsers().getAsJsonArray();
            for (JsonElement linkedUser : linkedUsers) {
                if (guild.getMemberById(higherDepth(linkedUser, "discordId").getAsString()) == null) {
                    continue;
                }

                if (guildMembersUuids.contains(higherDepth(linkedUser, "minecraftUuid").getAsString())) {
                    guild.addRoleToMember(higherDepth(linkedUser, "discordId").getAsString(), role).queue();
                } else {
                    guild.removeRoleFromMember(higherDepth(linkedUser, "discordId").getAsString(), role).queue();
                }
            }

            logCommand(guild, "Guild Role | Users (" + linkedUsers.size() + ") | Time (" + ((System.currentTimeMillis() - startTime) / 1000) + "s)");
        }
    }

    public boolean allowApplyReload() {
        return applyGuild.applyUserListSize() == 0;
    }

    public void verifyConstructor(GuildReadyEvent event) {
        JsonElement currentSettings = database.getVerifySettings(event.getGuild().getId());
        if (currentSettings == null) {
            return;
        }

        try {
            if(higherDepth(currentSettings, "enable") == null || (higherDepth(currentSettings, "enable") != null && !higherDepth(currentSettings, "enable").getAsBoolean())){
                return;
            }

            if (higherDepth(currentSettings, "enable").getAsBoolean()) {
                TextChannel reactChannel = event.getGuild()
                        .getTextChannelById(higherDepth(currentSettings, "messageTextChannelId").getAsString());
                try {
                    Message reactMessage = reactChannel.retrieveMessageById(higherDepth(currentSettings, "previousMessageId").getAsString()).complete();
                    if (reactMessage != null) {
                        reactMessage.editMessage(higherDepth(currentSettings, "messageText").getAsString()).queue();

                        verifyGuild = new VerifyGuild(reactChannel, reactMessage);
                        return;
                    }
                } catch (Exception ignored) {
                }

                Message reactMessage = reactChannel.sendMessage(higherDepth(currentSettings, "messageText").getAsString())
                        .addFile(new File("src/main/java/com/skyblockplus/eventlisteners/verify/Link_Discord_To_Hypixel.mp4"))
                        .complete();

                JsonObject newSettings = currentSettings.getAsJsonObject();
                newSettings.remove("previousMessageId");
                newSettings.addProperty("previousMessageId", reactMessage.getId());
                database.updateVerifySettings(event.getGuild().getId(), newSettings);

                verifyGuild = new VerifyGuild(reactChannel, reactMessage);
            }
        } catch (Exception e) {
            System.out.println("== Stack Trace (Verify constructor error - " + event.getGuild().getId() + ") ==");
            e.printStackTrace();
        }
    }

    public void applyConstructor(GuildReadyEvent event) {
        JsonElement currentSettings = database.getApplySettings(event.getGuild().getId());
        if (currentSettings == null) {
            return;
        }

        try {
            if(higherDepth(currentSettings, "enable") == null || (higherDepth(currentSettings, "enable") != null && !higherDepth(currentSettings, "enable").getAsBoolean())){
                return;
            }

            if (higherDepth(currentSettings, "enable").getAsBoolean()) {
                TextChannel reactChannel = event.getGuild()
                        .getTextChannelById(higherDepth(currentSettings, "messageTextChannelId").getAsString());

                EmbedBuilder eb = defaultEmbed("Apply For Guild");
                eb.setDescription(higherDepth(currentSettings, "messageText").getAsString());

                try {
                    Message reactMessage = reactChannel.retrieveMessageById(higherDepth(currentSettings, "previousMessageId").getAsString()).complete();
                    reactMessage.editMessage(eb.build()).queue();

                    applyGuild = new ApplyGuild(reactMessage, currentSettings);
                    return;
                } catch (Exception ignored) {
                }

                Message reactMessage = reactChannel.sendMessage(eb.build()).complete();
                reactMessage.addReaction("✅").queue();

                JsonObject newSettings = currentSettings.getAsJsonObject();
                newSettings.remove("previousMessageId");
                newSettings.addProperty("previousMessageId", reactMessage.getId());
                database.updateApplySettings(event.getGuild().getId(), newSettings);

                applyGuild = new ApplyGuild(reactMessage, currentSettings);
            }
        } catch (Exception e) {
            System.out.println("== Stack Trace (Apply constructor error - "+event.getGuild().getId()+") ==");
            e.printStackTrace();
        }
    }

    public String reloadApplyConstructor(String guildId) {
        Guild guild = jda.getGuildById(guildId);
        if (guild == null) {
            return "Invalid guild";
        }


        JsonElement currentSettings = database.getApplySettings(guild.getId());
        if (currentSettings == null) {
            return "No settings found";
        }

        try {
            if (higherDepth(currentSettings, "enable").getAsBoolean()) {
                TextChannel reactChannel = guild
                        .getTextChannelById(higherDepth(currentSettings, "messageTextChannelId").getAsString());

                EmbedBuilder eb = defaultEmbed("Apply For Guild");
                eb.setDescription(higherDepth(currentSettings, "messageText").getAsString());

                try {
                    Message reactMessage = reactChannel.retrieveMessageById(higherDepth(currentSettings, "previousMessageId").getAsString()).complete();
                    reactMessage.editMessage(eb.build()).queue();

                    applyGuild = new ApplyGuild(reactMessage, currentSettings);
                    return "Reloaded";
                } catch (Exception ignored) {
                }

                Message reactMessage = reactChannel.sendMessage(eb.build()).complete();
                reactMessage.addReaction("✅").queue();

                JsonObject newSettings = currentSettings.getAsJsonObject();
                newSettings.remove("previousMessageId");
                newSettings.addProperty("previousMessageId", reactMessage.getId());
                database.updateApplySettings(guild.getId(), newSettings);

                applyGuild = new ApplyGuild(reactMessage, currentSettings);
                return "Reloaded";
            } else {
                applyGuild = new ApplyGuild(false);
                return "Not enabled";
            }
        } catch (Exception e) {
            System.out.println("== Stack Trace (Reload apply constructor error - "+guildId+") ==");
            e.printStackTrace();
            if(e.getMessage().contains("Missing permission")){
                return "Error Reloading\nMissing permission(s): " + e.getMessage().split("Missing permission: ")[1];
            }
        }
        return "Error Reloading";
    }

    public String reloadVerifyConstructor(String guildId) {
        Guild guild = jda.getGuildById(guildId);
        if (guild == null) {
            return "Invalid guild";
        }


        JsonElement currentSettings = database.getVerifySettings(guild.getId());
        if (currentSettings == null) {
            return "No settings found";
        }

        try {
            if (higherDepth(currentSettings, "enable").getAsBoolean()) {
                TextChannel reactChannel = guild
                        .getTextChannelById(higherDepth(currentSettings, "messageTextChannelId").getAsString());
                try {
                    Message reactMessage = reactChannel.retrieveMessageById(higherDepth(currentSettings, "previousMessageId").getAsString()).complete();
                    if (reactMessage != null) {
                        reactMessage.editMessage(higherDepth(currentSettings, "messageText").getAsString()).queue();

                        verifyGuild = new VerifyGuild(reactChannel, reactMessage);
                        return "Reloaded";
                    }
                } catch (Exception ignored) {
                }

                Message reactMessage = reactChannel.sendMessage(higherDepth(currentSettings, "messageText").getAsString())
                        .addFile(new File("src/main/java/com/skyblockplus/eventlisteners/verify/Link_Discord_To_Hypixel.mp4"))
                        .complete();

                JsonObject newSettings = currentSettings.getAsJsonObject();
                newSettings.remove("previousMessageId");
                newSettings.addProperty("previousMessageId", reactMessage.getId());
                database.updateVerifySettings(guild.getId(), newSettings);

                verifyGuild = new VerifyGuild(reactChannel, reactMessage);
                return "Reloaded";
            } else {
                verifyGuild = new VerifyGuild(false);
                return "Not enabled";
            }
        } catch (Exception e) {
            System.out.println("== Stack Trace (Reload verify constructor error - "+guildId+") ==");
            e.printStackTrace();
            if(e.getMessage().contains("Missing permission")){
                return "Error Reloading\nMissing permission(s): " + e.getMessage().split("Missing permission: ")[1];
            }
        }
        return "Error Reloading";
    }

    public void onMessageReactionAdd(MessageReactionAddEvent event) {
        applyGuild.onMessageReactionAdd(event);
    }

    public void onGuildMessageReceived(GuildMessageReceivedEvent event) {
        verifyGuild.onGuildMessageReceived(event);
    }

    public void onTextChannelDelete(TextChannelDeleteEvent event) {
        applyGuild.onTextChannelDelete(event);
    }
}
