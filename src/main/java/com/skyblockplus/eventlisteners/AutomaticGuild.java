package com.skyblockplus.eventlisteners;

import static com.skyblockplus.Main.database;
import static com.skyblockplus.Main.jda;
import static com.skyblockplus.eventlisteners.skyblockevent.SkyblockEventCommand.endSkyblockEvent;
import static com.skyblockplus.utils.Utils.HYPIXEL_API_KEY;
import static com.skyblockplus.utils.Utils.defaultEmbed;
import static com.skyblockplus.utils.Utils.getJson;
import static com.skyblockplus.utils.Utils.higherDepth;
import static com.skyblockplus.utils.Utils.logCommand;

import java.io.File;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.skyblockplus.api.discordserversettings.skyblockevent.EventMember;
import com.skyblockplus.api.linkedaccounts.LinkedAccountModel;
import com.skyblockplus.eventlisteners.apply.ApplyGuild;
import com.skyblockplus.eventlisteners.skyblockevent.SkyblockEvent;
import com.skyblockplus.eventlisteners.verify.VerifyGuild;

import org.apache.commons.collections4.ListUtils;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.channel.text.TextChannelDeleteEvent;
import net.dv8tion.jda.api.events.guild.GenericGuildEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.utils.concurrent.Task;

public class AutomaticGuild {
    private final String guildId;
    private ApplyGuild applyGuild = new ApplyGuild();
    private VerifyGuild verifyGuild = new VerifyGuild();
    private SkyblockEvent skyblockEvent = new SkyblockEvent();
    private List<EventMember> eventMemberList = new ArrayList<>();
    private Instant eventMemberListLastUpdated = null;

    public AutomaticGuild(GenericGuildEvent event) {
        guildId = event.getGuild().getId();
        applyConstructor(event);
        verifyConstructor(event);
        schedulerConstructor();
    }

    public List<EventMember> getEventMemberList() {
        return eventMemberList;
    }

    public void setEventMemberList(List<EventMember> eventMemberList) {
        this.eventMemberList = eventMemberList;
    }

    public Instant getEventMemberListLastUpdated() {
        return eventMemberListLastUpdated;
    }

    public void setEventMemberListLastUpdated(Instant eventMemberListLastUpdated) {
        this.eventMemberListLastUpdated = eventMemberListLastUpdated;
    }

    public ApplyGuild getApplyGuild() {
        return applyGuild;
    }

    public void schedulerConstructor() {
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        int eventDelay = (int) (Math.random() * 60 + 1);
        // scheduler.scheduleAtFixedRate(this::updateGuildRoles, eventDelay, 180,
        // TimeUnit.MINUTES);
        scheduler.scheduleAtFixedRate(this::updateSkyblockEvent, eventDelay, 60, TimeUnit.MINUTES);
        scheduler.schedule(this::updateGuildRoles, 10, TimeUnit.SECONDS);
    }

    private void updateSkyblockEvent() {
        try {
            if (database.getSkyblockEventActive(guildId)) {
                JsonElement currentSettings = database.getRunningEventSettings(guildId);
                Instant endingTime = Instant
                        .ofEpochSecond(higherDepth(currentSettings, "timeEndingSeconds").getAsLong());
                if (Duration.between(Instant.now(), endingTime).toMinutes() <= 0) {
                    endSkyblockEvent(guildId);
                }
            }
        } catch (Exception e) {
            System.out.println("== Stack Trace (updateSkyblockEvent) ==");
            e.printStackTrace();
        }
    }

    public void updateGuildRoles() {
        try {
            Guild guild = jda.getGuildById(guildId);
            JsonElement currentSettings = database.getGuildRoleSettings(guild.getId());

            if (currentSettings == null) {
                return;
            }

            boolean enableGuildRole = false;
            boolean enableGuildRanks = false;
            try {
                enableGuildRole = higherDepth(currentSettings, "enableGuildRole").getAsBoolean();
            } catch (Exception ignored) {
            }

            try {
                enableGuildRanks = higherDepth(currentSettings, "enableGuildRanks").getAsBoolean();
            } catch (Exception ignored) {
            }

            if (!enableGuildRole && !enableGuildRanks) {
                return;
            }

            long startTime = System.currentTimeMillis();

            JsonElement guildJson = getJson("https://api.hypixel.net/guild?key=" + HYPIXEL_API_KEY + "&id="
                    + higherDepth(currentSettings, "guildId").getAsString());

            if (guildJson == null) {
                return;
            }

            JsonArray guildMembers = higherDepth(higherDepth(guildJson, "guild"), "members").getAsJsonArray();
            Map<String, String> uuidToRankMap = new HashMap<>();

            for (JsonElement guildMember : guildMembers) {
                uuidToRankMap.put(higherDepth(guildMember, "uuid").getAsString(),
                        higherDepth(guildMember, "rank").getAsString().replace(" ", "_"));
            }

            List<LinkedAccountModel> linkedUsers = database.getLinkedUsers();

            Role guildMemberRole = enableGuildRole
                    ? guild.getRoleById(higherDepth(currentSettings, "roleId").getAsString())
                    : null;

            int memberCount = 0;
            List<List<LinkedAccountModel>> linkedUsersLists = ListUtils.partition(linkedUsers, 100);

            AtomicInteger requestCount = new AtomicInteger();
            List<Member> inGuildUsers = new ArrayList<>();
            CountDownLatch latch = new CountDownLatch(1);
            Map<String, String> discordIdToUuid = new HashMap<>();

            for (List<LinkedAccountModel> linkedUsersList : linkedUsersLists) {
                List<String> linkedUsersStrs = new ArrayList<>();
                for (LinkedAccountModel linkedUser : linkedUsersList) {
                    linkedUsersStrs.add(linkedUser.getDiscordId());
                    discordIdToUuid.put(linkedUser.getDiscordId(), linkedUser.getMinecraftUuid());
                }

                guild.retrieveMembersByIds(linkedUsersStrs.toArray(new String[0])).onSuccess(members -> {
                    inGuildUsers.addAll(members);
                    requestCount.incrementAndGet();
                    if (requestCount.get() == linkedUsersLists.size()) {
                        latch.countDown();
                    }
                }).onError(error -> {
                    requestCount.incrementAndGet();
                    if (requestCount.get() == linkedUsersLists.size()) {
                        latch.countDown();
                    }
                });
            }

            try {
                latch.await(15, TimeUnit.SECONDS);
            } catch (Exception e) {
                System.out.println("== Stack Trace (updateGuildRoles latch) ==");
                e.printStackTrace();
            }

            for (Member linkedUser : inGuildUsers) {
                if (enableGuildRole) {
                    if (uuidToRankMap.containsKey(discordIdToUuid.get(linkedUser.getId()))) {
                        guild.addRoleToMember(linkedUser, guildMemberRole).queue();
                    } else {
                        guild.removeRoleFromMember(linkedUser, guildMemberRole).queue();
                    }
                }

                if (enableGuildRanks) {
                    JsonArray guildRanksArr = higherDepth(currentSettings, "guildRanks").getAsJsonArray();
                    if (!uuidToRankMap.containsKey(discordIdToUuid.get(linkedUser.getId()))) {
                        for (JsonElement guildRank : guildRanksArr) {
                            guild.removeRoleFromMember(linkedUser,
                                    guild.getRoleById(higherDepth(guildRank, "discordRoleId").getAsString())).queue();
                        }
                    } else {
                        String currentRank = uuidToRankMap.get(discordIdToUuid.get(linkedUser.getId()));
                        for (JsonElement guildRank : guildRanksArr) {
                            Role currentRankRole = guild
                                    .getRoleById(higherDepth(guildRank, "discordRoleId").getAsString());
                            if (higherDepth(guildRank, "minecraftRoleName").getAsString()
                                    .equalsIgnoreCase(currentRank)) {
                                guild.addRoleToMember(linkedUser, currentRankRole).queue();
                            } else {
                                guild.removeRoleFromMember(linkedUser, currentRankRole).queue();
                            }
                        }
                    }
                }

                memberCount++;
            }
            logCommand(guild, "Guild Role | Users (" + memberCount + ") | Time ("
                    + ((System.currentTimeMillis() - startTime) / 1000) + "s)");
        } catch (Exception e) {
            System.out.println("== Stack Trace (updateGuildRoles) ==");
            e.printStackTrace();
        }
    }

    public boolean allowApplyReload() {
        return applyGuild.applyUserListSize() == 0;
    }

    public void verifyConstructor(GenericGuildEvent event) {
        JsonElement currentSettings = database.getVerifySettings(event.getGuild().getId());
        if (currentSettings == null) {
            return;
        }

        try {
            if (higherDepth(currentSettings, "enable") == null || (higherDepth(currentSettings, "enable") != null
                    && !higherDepth(currentSettings, "enable").getAsBoolean())) {
                return;
            }

            if (higherDepth(currentSettings, "enable").getAsBoolean()) {
                TextChannel reactChannel = event.getGuild()
                        .getTextChannelById(higherDepth(currentSettings, "messageTextChannelId").getAsString());
                try {
                    Message reactMessage = reactChannel
                            .retrieveMessageById(higherDepth(currentSettings, "previousMessageId").getAsString())
                            .complete();
                    if (reactMessage != null) {
                        reactMessage.editMessage(higherDepth(currentSettings, "messageText").getAsString()).queue();

                        verifyGuild = new VerifyGuild(reactChannel, reactMessage);
                        return;
                    }
                } catch (Exception ignored) {
                }

                Message reactMessage = reactChannel
                        .sendMessage(higherDepth(currentSettings, "messageText").getAsString())
                        .addFile(new File(
                                "src/main/java/com/skyblockplus/eventlisteners/verify/Link_Discord_To_Hypixel.mp4"))
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

    public void applyConstructor(GenericGuildEvent event) {
        JsonElement currentSettings = database.getApplySettings(event.getGuild().getId());
        if (currentSettings == null) {
            return;
        }

        try {
            if (higherDepth(currentSettings, "enable") == null || (higherDepth(currentSettings, "enable") != null
                    && !higherDepth(currentSettings, "enable").getAsBoolean())) {
                return;
            }

            if (higherDepth(currentSettings, "enable").getAsBoolean()) {
                TextChannel reactChannel = event.getGuild()
                        .getTextChannelById(higherDepth(currentSettings, "messageTextChannelId").getAsString());

                EmbedBuilder eb = defaultEmbed("Apply For Guild");
                eb.setDescription(higherDepth(currentSettings, "messageText").getAsString());

                try {
                    Message reactMessage = reactChannel
                            .retrieveMessageById(higherDepth(currentSettings, "previousMessageId").getAsString())
                            .complete();
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
            System.out.println("== Stack Trace (Apply constructor error - " + event.getGuild().getId() + ") ==");
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
                    Message reactMessage = reactChannel
                            .retrieveMessageById(higherDepth(currentSettings, "previousMessageId").getAsString())
                            .complete();
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
                applyGuild = new ApplyGuild();
                return "Not enabled";
            }
        } catch (Exception e) {
            System.out.println("== Stack Trace (Reload apply constructor error - " + guildId + ") ==");
            e.printStackTrace();
            if (e.getMessage().contains("Missing permission")) {
                return "Error Reloading\nMissing permission: " + e.getMessage().split("Missing permission: ")[1];
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
                    Message reactMessage = reactChannel
                            .retrieveMessageById(higherDepth(currentSettings, "previousMessageId").getAsString())
                            .complete();
                    if (reactMessage != null) {
                        reactMessage.editMessage(higherDepth(currentSettings, "messageText").getAsString()).queue();

                        verifyGuild = new VerifyGuild(reactChannel, reactMessage);
                        return "Reloaded";
                    }
                } catch (Exception ignored) {
                }

                Message reactMessage = reactChannel
                        .sendMessage(higherDepth(currentSettings, "messageText").getAsString())
                        .addFile(new File(
                                "src/main/java/com/skyblockplus/eventlisteners/verify/Link_Discord_To_Hypixel.mp4"))
                        .complete();

                JsonObject newSettings = currentSettings.getAsJsonObject();
                newSettings.remove("previousMessageId");
                newSettings.addProperty("previousMessageId", reactMessage.getId());
                database.updateVerifySettings(guild.getId(), newSettings);

                verifyGuild = new VerifyGuild(reactChannel, reactMessage);
                return "Reloaded";
            } else {
                verifyGuild = new VerifyGuild();
                return "Not enabled";
            }
        } catch (Exception e) {
            System.out.println("== Stack Trace (Reload verify constructor error - " + guildId + ") ==");
            e.printStackTrace();
            if (e.getMessage().contains("Missing permission")) {
                return "Error Reloading\nMissing permission: " + e.getMessage().split("Missing permission: ")[1];
            }
        }
        return "Error Reloading";
    }

    public void onMessageReactionAdd(MessageReactionAddEvent event) {
        applyGuild.onMessageReactionAdd(event);
    }

    public void onGuildMessageReceived(GuildMessageReceivedEvent event) {
        if (verifyGuild.onGuildMessageReceived(event)) {
            return;
        }

        String s = skyblockEvent.onGuildMessageReceived(event);
        if (s.equals("delete")) {
            skyblockEvent.getScheduler().shutdown();
            skyblockEvent = new SkyblockEvent();
        }
    }

    public void onTextChannelDelete(TextChannelDeleteEvent event) {
        applyGuild.onTextChannelDelete(event);
    }

    public void createSkyblockEvent(CommandEvent event) {
        skyblockEvent = new SkyblockEvent(event);
    }

}
