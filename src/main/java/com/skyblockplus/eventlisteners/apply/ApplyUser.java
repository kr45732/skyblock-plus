package com.skyblockplus.eventlisteners.apply;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;

import java.io.Serializable;
import java.util.EnumSet;
import java.util.concurrent.TimeUnit;

import static com.skyblockplus.Main.database;
import static com.skyblockplus.Main.jda;
import static com.skyblockplus.utils.Utils.*;

public class ApplyUser implements Serializable {
    private final String applyingUserId;
    private final String applicationChannelId;
    private final String currentSettingsString;
    private final String guildId;
    private String reactMessageId;
    private int state = 0;
    private PlayerCustom player;
    private String staffChannelId;
    private boolean shouldDeleteChannel = false;
    private String playerSlayer;
    private String playerSkills;
    private String playerCatacombs;
    private String playerWeight;

    public ApplyUser(MessageReactionAddEvent event, User applyingUser, JsonElement currentSettings) {
        logCommand(event.getGuild(), applyingUser, "apply " + applyingUser.getName());

        this.applyingUserId = applyingUser.getId();
        this.currentSettingsString = new Gson().toJson(currentSettings);
        this.guildId = event.getGuild().getId();

        String channelPrefix = higherDepth(currentSettings, "newChannelPrefix").getAsString();
        Category applyCategory = event.getGuild()
                .getCategoryById(higherDepth(currentSettings, "newChannelCategory").getAsString());
        TextChannel applicationChannel = applyCategory.createTextChannel(channelPrefix + "-" + applyingUser.getName())
                .addPermissionOverride(event.getGuild().getMember(applyingUser), EnumSet.of(Permission.VIEW_CHANNEL),
                        null)
                .addPermissionOverride(event.getGuild().getPublicRole(), null, EnumSet.of(Permission.VIEW_CHANNEL))
                .complete();
        this.applicationChannelId = applicationChannel.getId();

        applicationChannel.sendMessage("Welcome " + applyingUser.getAsMention() + "!").complete();

        EmbedBuilder welcomeEb = defaultEmbed("Application for " + applyingUser.getName());
        welcomeEb.setDescription(
                "• Please enter your in-game-name **optionally** followed by the skyblock profile you want to apply with.\n• Ex: CrypticPlasma **OR** CrypticPlasma Zucchini\n");
        welcomeEb.addField("To submit your LAST message,", "React with ✅", true);
        welcomeEb.addField("To cancel the application,", "React with ❌", true);
        Message reactMessage = applicationChannel.sendMessage(welcomeEb.build()).complete();
        this.reactMessageId = reactMessage.getId();
        reactMessage.addReaction("✅").queue();
        reactMessage.addReaction("❌").queue();
    }

    public String getGuildId() {
        return guildId;
    }

    public String getApplicationChannelId() {
        return applicationChannelId;
    }

    public String getStaffChannelId() {
        return staffChannelId;
    }

    public String getMessageReactId() {
        return reactMessageId;
    }

    public boolean onMessageReactionAdd(MessageReactionAddEvent event) {
        if (state == 4) {
            return onMessageReactionAddStaff(event);
        }

        // User
        if (!event.getMessageId().equals(reactMessageId)) {
            return false;
        }
        if (event.getUser().isBot()) {
            return false;
        }

        User applyingUser = jda.getUserById(applyingUserId);
        TextChannel applicationChannel = jda.getTextChannelById(applicationChannelId);
        Message reactMessage = applicationChannel.retrieveMessageById(reactMessageId).complete();
        JsonElement currentSettings = JsonParser.parseString(currentSettingsString);

        if (!event.getUser().equals(applyingUser)) {
            if (!(event.getGuild().getMember(event.getUser()).getRoles().contains(event.getGuild().getRoleById(higherDepth(currentSettings, "staffPingRoleId").getAsString())) || event.getGuild().getMember(event.getUser()).hasPermission(Permission.ADMINISTRATOR))) {
                reactMessage.removeReaction(event.getReaction().getReactionEmote().getAsReactionCode(), event.getUser())
                        .queue();
                return false;
            }
        }

        if (event.getReactionEmote().getName().equals("❌")) {
            state = 3;
        } else if (event.getReactionEmote().getName().equals("↩️") && state == 1) {
            state = 2;
        } else if (!event.getReactionEmote().getName().equals("✅")) {
            reactMessage.clearReactions(event.getReaction().getReactionEmote().getAsReactionCode()).queue();
            return false;
        }

        reactMessage.clearReactions().queue();

        switch (state) {
            case 0:
                if (applicationChannel.hasLatestMessage()) {
                    Message messageReply = applicationChannel
                            .retrieveMessageById(applicationChannel.getLatestMessageId()).complete();
                    if (messageReply.getAuthor().equals(applyingUser)) {
                        String[] messageContent = messageReply.getContentDisplay().split(" ");
                        if (messageContent.length == 1 || messageContent.length == 2) {
                            player = messageContent.length == 1 ? new PlayerCustom(messageContent[0])
                                    : new PlayerCustom(messageContent[0], messageContent[1]);

                            if (player.isValid()) {
                                String[] playerInfo = getPlayerDiscordInfo(player.getUsername());

                                if (playerInfo != null) {
                                    if (applyingUser.getAsTag().equals(playerInfo[0])) {
                                        playerSlayer = formatNumber(player.getSlayer());
                                        playerSkills = roundSkillAverage(player.getSkillAverage());
                                        playerCatacombs = "" + player.getCatacombsSkill().skillLevel;
                                        playerWeight = roundSkillAverage(player.getWeight());
                                        EmbedBuilder statsEmbed = player.defaultPlayerEmbed();
                                        statsEmbed.setDescription("**Total Skyblock weight:** " + playerWeight);
                                        statsEmbed.addField("Total slayer", playerSlayer, true);
                                        statsEmbed.addField("Progress skill level", playerSkills, true);
                                        statsEmbed.addField("Catacombs level", "" + playerCatacombs, true);
                                        statsEmbed.addField("Are the above stats correct?",
                                                "React with ✅ for yes, ↩️ to retry, and ❌ to cancel", false);

                                        reactMessage = applicationChannel.sendMessage(statsEmbed.build()).complete();
                                        reactMessage.addReaction("✅").queue();
                                        reactMessage.addReaction("↩️").queue();
                                        reactMessage.addReaction("❌").queue();
                                        this.reactMessageId = reactMessage.getId();
                                        state = 1;
                                        break;
                                    }
                                    EmbedBuilder discordTagMismatchEb = defaultEmbed("Discord tag mismatch");
                                    discordTagMismatchEb.setDescription("Account " + player.getUsername()
                                            + " is linked with the discord tag " + playerInfo[0]
                                            + "\nYour current discord tag is " + applyingUser.getAsTag());
                                    discordTagMismatchEb.addField("To retry,", "React with ✅", true);
                                    discordTagMismatchEb.addField("To cancel the application,", "React with ❌", true);
                                    reactMessage = applicationChannel.sendMessage(discordTagMismatchEb.build())
                                            .complete();
                                } else {
                                    EmbedBuilder discordTagMismatchEb = defaultEmbed("Discord tag error");
                                    discordTagMismatchEb.setDescription("Account " + player.getUsername()
                                            + " is not linked to any discord tag\nHere is a **__[link](https://streamable.com/sdq8tp)__** to a video explaining how to link");
                                    discordTagMismatchEb.addField("To retry,", "React with ✅", true);
                                    discordTagMismatchEb.addField("To cancel the application,", "React with ❌", true);
                                    reactMessage = applicationChannel.sendMessage(discordTagMismatchEb.build())
                                            .complete();
                                }
                                this.reactMessageId = reactMessage.getId();
                                reactMessage.addReaction("✅").queue();
                                reactMessage.addReaction("❌").queue();
                                state = 2;
                                break;
                            }
                            EmbedBuilder invalidEmbed = defaultEmbed("Invalid username or profile");
                            invalidEmbed.setDescription("**Please check your input!**");
                            invalidEmbed.addField("Argument(s) given:", messageReply.getContentDisplay(), true);
                            invalidEmbed.addField("Valid Arguments Examples:",
                                    "• CrypticPlasma\n• CrypticPlasma Zucchini", true);
                            invalidEmbed.addBlankField(true);
                            invalidEmbed.addField("To retry,", "React with ✅", true);
                            invalidEmbed.addField("To cancel the application,", "React with ❌", true);
                            invalidEmbed.addBlankField(true);
                            reactMessage = applicationChannel.sendMessage(invalidEmbed.build()).complete();
                            this.reactMessageId = reactMessage.getId();
                            reactMessage.addReaction("✅").queue();
                            reactMessage.addReaction("❌").queue();
                            state = 2;
                            break;
                        }
                        EmbedBuilder invalidEmbed = defaultEmbed("Invalid arguments");
                        invalidEmbed.setDescription("**Please check your input!**");
                        invalidEmbed.addField("Argument(s) given:", messageReply.getContentDisplay(), true);
                        invalidEmbed.addField("Valid Arguments Examples:", "• CrypticPlasma\n• CrypticPlasma Zucchini",
                                true);
                        invalidEmbed.addBlankField(true);
                        invalidEmbed.addField("To retry,", "React with ✅", true);
                        invalidEmbed.addField("To cancel the application,", "React with ❌", true);
                        invalidEmbed.addBlankField(true);
                        reactMessage = applicationChannel.sendMessage(invalidEmbed.build()).complete();
                        this.reactMessageId = reactMessage.getId();
                        reactMessage.addReaction("✅").queue();
                        reactMessage.addReaction("❌").queue();
                        state = 2;
                        break;
                    }
                }
                EmbedBuilder invalidEb = defaultEmbed("Invalid Arguments");
                invalidEb.setDescription("**Unable to get latest message**");
                invalidEb.addField("To retry,", "React with ✅", true);
                invalidEb.addField("To cancel the application,", "React with ❌", true);
                reactMessage = applicationChannel.sendMessage(invalidEb.build()).complete();
                this.reactMessageId = reactMessage.getId();
                reactMessage.addReaction("✅").queue();
                reactMessage.addReaction("❌").queue();
                state = 2;
                break;
            case 1:
                EmbedBuilder finishApplyEmbed = defaultEmbed("Thank you for applying!");
                finishApplyEmbed.setDescription(
                        "**Your stats have been submitted to staff**\nYou will be notified once staff review your stats");
                applicationChannel.sendMessage(finishApplyEmbed.build()).queue();

                state = 4;
                staffCaseConstructor();
                break;
            case 2:
                EmbedBuilder retryEmbed = defaultEmbed("Application for " + applyingUser.getName());
                retryEmbed.setDescription(
                        "• Please enter your in-game-name optionally followed by the skyblock profile you want to apply with.\n• Ex: CrypticPlasma **OR** CrypticPlasma Zucchini\n");
                retryEmbed.addField("To submit your LAST message,", "React with ✅", true);
                retryEmbed.addField("To cancel the application,", "React with ❌", true);
                reactMessage = applicationChannel.sendMessage(retryEmbed.build()).complete();
                this.reactMessageId = reactMessage.getId();
                reactMessage.addReaction("✅").queue();
                reactMessage.addReaction("❌").queue();
                state = 0;
                break;
            case 3:
                EmbedBuilder cancelEmbed = defaultEmbed("Canceling application");
                cancelEmbed.setDescription("Channel closing in 5 seconds...");
                applicationChannel.sendMessage(cancelEmbed.build()).queue();
                event.getGuild().getTextChannelById(event.getChannel().getId()).delete().reason("Application canceled")
                        .queueAfter(5, TimeUnit.SECONDS);
                return true;
        }
        return false;
    }

    private boolean onMessageReactionAddStaff(MessageReactionAddEvent event) {
        if (event.getUser().isBot()) {
            return false;
        }
        TextChannel applicationChannel = jda.getTextChannelById(applicationChannelId);
        try {
            if (shouldDeleteChannel && (event.getMessageId().equals(reactMessageId))) {
                if (event.getReactionEmote().getName().equals("✅")) {
                    event.getReaction().clearReactions().queue();
                    EmbedBuilder eb = defaultEmbed("Channel closing in 10 seconds");
                    applicationChannel.sendMessage(eb.build()).queue();
                    applicationChannel.delete().reason("Applicant read final message").queueAfter(10, TimeUnit.SECONDS);
                    return true;
                } else {
                    event.getReaction().removeReaction(event.getUser()).queue();
                }
                return false;
            }
        } catch (Exception ignored) {

        }
        if (!event.getMessageId().equals(reactMessageId)) {
            return false;
        }
        JsonElement currentSettings = JsonParser.parseString(currentSettingsString);

        TextChannel staffChannel = jda.getTextChannelById(staffChannelId);
        User applyingUser = jda.getUserById(applyingUserId);
        Message reactMessage = event.getChannel().retrieveMessageById(event.getMessageId()).complete();
        if (event.getReactionEmote().getName().equals("❌")) {
            staffChannel.sendMessage(player.getUsername() + " (" + applyingUser.getAsMention() + ") was denied by "
                    + event.getUser().getName() + " (" + event.getUser().getAsMention() + ")").queue();
            reactMessage.clearReactions().queue();
            EmbedBuilder eb = defaultEmbed("Application Not Accepted");
            eb.setDescription(higherDepth(currentSettings, "denyMessageText").getAsString()
                    + "\n**React with ✅ to close the channel**");
            applicationChannel.sendMessage(applyingUser.getAsMention()).queue();
            reactMessage.delete().queueAfter(5, TimeUnit.SECONDS);
            reactMessage = applicationChannel.sendMessage(eb.build()).complete();
            reactMessage.addReaction("✅").queue();
            this.reactMessageId = reactMessage.getId();
            shouldDeleteChannel = true;

        } else if (event.getReactionEmote().getName().equals("✅")) {
            staffChannel.sendMessage(player.getUsername() + " (" + applyingUser.getAsMention() + ") was accepted by "
                    + event.getUser().getName() + " (" + event.getUser().getAsMention() + ")").queue();
            reactMessage.clearReactions().queue();
            EmbedBuilder eb = defaultEmbed("Application Accepted");
            eb.setDescription(higherDepth(currentSettings, "acceptMessageText").getAsString()
                    + "\n**React with ✅ to close the channel**");
            applicationChannel.sendMessage(applyingUser.getAsMention()).queue();
            reactMessage.delete().queueAfter(5, TimeUnit.SECONDS);
            reactMessage = applicationChannel.sendMessage(eb.build()).complete();
            reactMessage.addReaction("✅").queue();
            JsonElement guildRoleSettings = database.getGuildRoleSettings(guildId);
            try {
                Guild guild = jda.getGuildById(guildId);

                guild.addRoleToMember(applyingUserId, guild.getRoleById(higherDepth(guildRoleSettings, "roleId").getAsString())).queue();
            } catch (Exception ignored) {
            }

            this.reactMessageId = reactMessage.getId();
            shouldDeleteChannel = true;
        }
        return false;
    }

    private void staffCaseConstructor() {
        JsonElement currentSettings = JsonParser.parseString(currentSettingsString);

        TextChannel staffChannel = jda
                .getTextChannelById(higherDepth(currentSettings, "messageStaffChannelId").getAsString());
        staffChannelId = staffChannel.getId();

        EmbedBuilder applyPlayerStats = player.defaultPlayerEmbed();
        applyPlayerStats.setDescription("**Total Skyblock weight:** " + playerWeight);
        applyPlayerStats.addField("Total slayer", playerSlayer, true);
        applyPlayerStats.addField("Progress average skill level", playerSkills, true);
        applyPlayerStats.addField("Catacombs level", "" + playerCatacombs, true);
        applyPlayerStats.addField("To accept the application,", "React with ✅", true);
        applyPlayerStats.addBlankField(true);
        applyPlayerStats.addField("To deny the application,", "React with ❌", true);
        staffChannel.sendMessage("<@&" + higherDepth(currentSettings, "staffPingRoleId").getAsString() + ">")
                .complete();
        Message reactMessage = staffChannel.sendMessage(applyPlayerStats.build()).complete();
        reactMessage.addReaction("✅").queue();
        reactMessage.addReaction("❌").queue();
        reactMessageId = reactMessage.getId();
    }
}