package com.skyblockplus.apply;

import com.google.gson.JsonElement;
import com.skyblockplus.utils.Player;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Category;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.util.EnumSet;
import java.util.concurrent.TimeUnit;

import static com.skyblockplus.reload.ReloadEventWatcher.addApplySubEventListener;
import static com.skyblockplus.timeout.ChannelDeleter.addChannel;
import static com.skyblockplus.timeout.ChannelDeleter.removeChannel;
import static com.skyblockplus.timeout.EventListenerDeleter.addEventListener;
import static com.skyblockplus.utils.Utils.*;

public class ApplyUser extends ListenerAdapter {
    private final User applyingUser;
    private final TextChannel applicationChannel;
    private final JsonElement currentSettings;
    private Message reactMessage;
    private int state = 0;
    private EmbedBuilder applyPlayerStats;
    private Player player;

    public ApplyUser(MessageReactionAddEvent event, User applyingUser, JsonElement currentSettings) {
        logCommand(event.getGuild(), applyingUser, "apply " + applyingUser.getName());
        this.applyingUser = applyingUser;
        this.currentSettings = currentSettings;

        String channelPrefix = higherDepth(currentSettings, "newChannelPrefix").getAsString();
        Category applyCategory = event.getGuild()
                .getCategoryById(higherDepth(currentSettings, "newChannelCategory").getAsString());
        this.applicationChannel = applyCategory.createTextChannel(channelPrefix + "-" + applyingUser.getName())
                .addPermissionOverride(event.getGuild().getMember(applyingUser), EnumSet.of(Permission.VIEW_CHANNEL),
                        null)
                .addPermissionOverride(event.getGuild().getPublicRole(), null, EnumSet.of(Permission.VIEW_CHANNEL))
                .complete();

        addChannel(this.applicationChannel);
        this.applicationChannel.sendMessage("Welcome " + applyingUser.getAsMention() + "!").complete();

        EmbedBuilder welcomeEb = defaultEmbed("Application for " + applyingUser.getName());
        welcomeEb.setDescription(
                "• Please enter your in-game-name **optionally** followed by the skyblock profile you want to apply with.\n• Ex: CrypticPlasma **OR** CrypticPlasma Zucchini\n");
        welcomeEb.addField("To submit your LAST message,", "React with ✅", true);
        welcomeEb.addField("To cancel the application,", "React with ❌", true);
        this.reactMessage = applicationChannel.sendMessage(welcomeEb.build()).complete();
        this.reactMessage.addReaction("✅").queue();
        this.reactMessage.addReaction("❌").queue();

        addApplySubEventListener(this.reactMessage.getGuild().getId(), this);
        addEventListener(this.reactMessage.getGuild().getId(), this.reactMessage.getChannel().getId(), this);

    }

    @Override
    public void onMessageReactionAdd(MessageReactionAddEvent event) {
        if (event.getMessageIdLong() != reactMessage.getIdLong()) {
            return;
        }
        if (event.getUser().isBot()) {
            return;
        }

        if (!event.getUser().equals(applyingUser)) {
            reactMessage.removeReaction(event.getReaction().getReactionEmote().getAsReactionCode(), event.getUser())
                    .queue();
            return;
        }

        if (event.getReactionEmote().getName().equals("❌")) {
            state = 3;
        } else if (event.getReactionEmote().getName().equals("↩️") && state == 1) {
            state = 2;
        } else if (!event.getReactionEmote().getName().equals("✅")) {
            reactMessage.clearReactions(event.getReaction().getReactionEmote().getAsReactionCode()).queue();
            return;
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
                            player = messageContent.length == 1 ? new Player(messageContent[0])
                                    : new Player(messageContent[0], messageContent[1]);

                            if (player.isValid()) {
                                String[] playerInfo = getPlayerDiscordInfo(player.getUsername());

                                if (playerInfo != null) {
                                    if (applyingUser.getAsTag().equals(playerInfo[0])) {
                                        String playerSlayer = formatNumber(player.getSlayer());
                                        String playerSkills = roundSkillAverage(player.getSkillAverage());
                                        String playerCatacombs = "" + player.getCatacombsSkill().skillLevel;
                                        String playerWeight = roundSkillAverage(player.getWeight());
                                        EmbedBuilder statsEmbed = defaultEmbed("Stats for " + player.getUsername(),
                                                skyblockStatsLink(player.getUsername(), player.getProfileName()));
                                        statsEmbed.setDescription("**Total Skyblock weight:** " + playerWeight);
                                        statsEmbed.addField("Total slayer", playerSlayer, true);
                                        statsEmbed.addField("Progress skill level", playerSkills, true);
                                        statsEmbed.addField("Catacombs level", "" + playerCatacombs, true);
                                        statsEmbed.addField("Are the above stats correct?",
                                                "React with ✅ for yes, ↩️ to retry, and ❌ to cancel", false);

                                        applyPlayerStats = defaultEmbed("Stats for " + player.getUsername(),
                                                skyblockStatsLink(player.getUsername(), player.getProfileName()));
                                        applyPlayerStats.setDescription("**Total Skyblock weight:** " + playerWeight);
                                        applyPlayerStats.addField("Total slayer", playerSlayer, true);
                                        applyPlayerStats.addField("Progress average skill level", playerSkills, true);
                                        applyPlayerStats.addField("Catacombs level", "" + playerCatacombs, true);

                                        reactMessage = applicationChannel.sendMessage(statsEmbed.build()).complete();
                                        reactMessage.addReaction("✅").queue();
                                        reactMessage.addReaction("↩️").queue();
                                        reactMessage.addReaction("❌").queue();
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
                                    reactMessage.addReaction("✅").queue();
                                    reactMessage.addReaction("❌").queue();
                                    state = 2;
                                    break;
                                }
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
                reactMessage.addReaction("✅").queue();
                reactMessage.addReaction("❌").queue();
                state = 2;
                break;
            case 1:
                EmbedBuilder finishApplyEmbed = defaultEmbed("Thank you for applying!");
                finishApplyEmbed.setDescription(
                        "**Your stats have been submitted to staff**\nYou will be notified once staff review your stats");
                applicationChannel.sendMessage(finishApplyEmbed.build()).queue();
                event.getJDA().removeEventListener(this);

                event.getJDA().addEventListener(
                        new ApplyStaff(applyingUser, applicationChannel, applyPlayerStats, currentSettings, player));
                break;
            case 2:
                EmbedBuilder retryEmbed = defaultEmbed("Application for " + applyingUser.getName());
                retryEmbed.setDescription(
                        "• Please enter your in-game-name optionally followed by the skyblock profile you want to apply with.\n• Ex: CrypticPlasma **OR** CrypticPlasma Zucchini\n");
                retryEmbed.addField("To submit your LAST message,", "React with ✅", true);
                retryEmbed.addField("To cancel the application,", "React with ❌", true);
                reactMessage = applicationChannel.sendMessage(retryEmbed.build()).complete();
                reactMessage.addReaction("✅").queue();
                reactMessage.addReaction("❌").queue();
                state = 0;
                break;
            case 3:
                EmbedBuilder cancelEmbed = defaultEmbed("Canceling application");
                cancelEmbed.setDescription("Channel closing in 5 seconds...");
                applicationChannel.sendMessage(cancelEmbed.build()).queue();
                event.getJDA().removeEventListener(this);
                event.getGuild().getTextChannelById(event.getChannel().getId()).delete().reason("Application canceled")
                        .queueAfter(5, TimeUnit.SECONDS);
                removeChannel(applicationChannel);
                break;
        }
    }
}