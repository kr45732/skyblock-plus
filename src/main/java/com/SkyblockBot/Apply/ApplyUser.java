package com.SkyblockBot.Apply;

import com.SkyblockBot.Miscellaneous.Player;
import com.google.gson.JsonElement;
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

import static com.SkyblockBot.Miscellaneous.BotUtils.*;
import static com.SkyblockBot.Miscellaneous.ChannelDeleter.addChannel;
import static com.SkyblockBot.Miscellaneous.ChannelDeleter.removeChannel;

public class ApplyUser extends ListenerAdapter {
    Message reactMessage;
    User applyingUser;
    TextChannel applicationChannel;
    int state = 0;
    EmbedBuilder applyPlayerStats;
    JsonElement currentSettings;
    Player player;

    public ApplyUser(MessageReactionAddEvent event, User applyingUser, JsonElement currentSettings) {
        System.out.println("Apply: " + applyingUser.getName());
        this.applyingUser = applyingUser;
        this.currentSettings = currentSettings;

        String channelPrefix = higherDepth(currentSettings, "new_channel_prefix").getAsString();
        Category applyCategory = event.getGuild()
                .getCategoryById(higherDepth(higherDepth(currentSettings, "new_channel_category"), "id").getAsString());
        this.applicationChannel = applyCategory.createTextChannel(channelPrefix + "-" + applyingUser.getName())
                .addPermissionOverride(event.getGuild().getMember(applyingUser), EnumSet.of(Permission.VIEW_CHANNEL),
                        null)
                .addPermissionOverride(event.getGuild().getPublicRole(), null, EnumSet.of(Permission.VIEW_CHANNEL))
                .complete();

        addChannel(this.applicationChannel);
        this.applicationChannel.sendMessage("Welcome " + applyingUser.getAsMention() + "!").complete();

        EmbedBuilder welcomeEb = defaultEmbed("Application for " + applyingUser.getName(), null);
        welcomeEb.setDescription(
                "• Please enter your in-game-name **optionally** followed by the skyblock profile you want to apply with.\n• Ex: CrypticPlasma **OR** CrypticPlasma Zucchini\n");
        welcomeEb.addField("To submit your LAST message,", "React with ✅", true);
        welcomeEb.addField("To cancel the application,", "React with ❌", true);
        this.reactMessage = applicationChannel.sendMessage(welcomeEb.build()).complete();
        this.reactMessage.addReaction("✅").queue();
        this.reactMessage.addReaction("❌").queue();
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
            reactMessage.removeReaction(event.getReaction().getReactionEmote().getAsReactionCode(), event.getUser()).queue();
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
                // Checks username and profile and returns stats/error
                if (applicationChannel.hasLatestMessage()) {
                    Message messageReply = applicationChannel
                            .retrieveMessageById(applicationChannel.getLatestMessageId()).complete();

                    String[] messageContent = messageReply.getContentDisplay().split(" ");
                    if (messageContent.length == 1 || messageContent.length == 2) {
                        player = messageContent.length == 1 ? new Player(messageContent[0])
                                : new Player(messageContent[0], messageContent[1]);

                        if (player.isValidPlayer()) {
                            String playerSlayer = formatNumber(player.getSlayer());
                            String playerSkills = roundSkillAverage(player.getSkillAverage());
                            String playerCatacombs = "" + player.getCatacombsSkill().skillLevel;
                            EmbedBuilder statsEmbed = defaultEmbed("Stats for " + player.getUsername(),
                                    skyblockStatsLink(player.getUsername(), player.getProfileName()));
                            statsEmbed.addField("Total slayer", playerSlayer, true);
                            statsEmbed.addField("Progress skill level", playerSkills, true);
                            statsEmbed.addField("Catacombs level", "" + playerCatacombs, true);

                            this.applyPlayerStats = defaultEmbed("Stats for " + player.getUsername(),
                                    skyblockStatsLink(player.getUsername(), player.getProfileName()));
                            this.applyPlayerStats.addField("Total slayer", playerSlayer, true);
                            this.applyPlayerStats.addField("Progress average skill level", playerSkills, true);
                            this.applyPlayerStats.addField("Catacombs level", "" + playerCatacombs, true);

                            statsEmbed.addField("Are the above stats correct?", "React with ✅ for yes, ↩️ to retry, and ❌ to cancel",
                                    false);
                            reactMessage = applicationChannel.sendMessage(statsEmbed.build()).complete();
                            reactMessage.addReaction("✅").queue();
                            reactMessage.addReaction("↩️").queue();
                            reactMessage.addReaction("❌").queue();
                            state = 1;
                            break;
                        }
                    }
                    EmbedBuilder invalidEmbed = invalidInput(messageReply.getContentDisplay());
                    reactMessage = applicationChannel.sendMessage(invalidEmbed.build()).complete();
                    reactMessage.addReaction("✅").queue();
                    reactMessage.addReaction("❌").queue();
                    state = 2;
                    break;
                }
                EmbedBuilder invalidEmbed = invalidInput(" ");
                reactMessage = applicationChannel.sendMessage(invalidEmbed.build()).complete();
                reactMessage.addReaction("✅").queue();
                reactMessage.addReaction("❌").queue();
                state = 2;
                break;
            case 1:
                // Valid username and confirmed apply, add staff listener
                EmbedBuilder finishApplyEmbed = defaultEmbed("Thank you for applying!", null);
                finishApplyEmbed.setDescription(
                        "**Your stats have been submitted to staff**\nYou will be notified once staff review your stats");
                applicationChannel.sendMessage(finishApplyEmbed.build()).queue();
                event.getJDA().removeEventListener(this);

                event.getJDA().addEventListener(
                        new ApplyStaff(applyingUser, applicationChannel, applyPlayerStats, currentSettings, player));
                break;
            case 2:
                // Retrying because of invalid username/profile
                EmbedBuilder retryEmbed = defaultEmbed("Application for " + applyingUser.getName(), null);
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
                // Cancel
                EmbedBuilder cancelEmbed = defaultEmbed("Canceling application", null);
                cancelEmbed.setDescription("Channel closing in 5 seconds...");
                applicationChannel.sendMessage(cancelEmbed.build()).queue();
                event.getJDA().removeEventListener(this);
                event.getGuild().getTextChannelById(event.getChannel().getId()).delete().reason("Application canceled")
                        .queueAfter(5, TimeUnit.SECONDS);
                removeChannel(applicationChannel);
                break;
        }
    }

    public EmbedBuilder invalidInput(String invalidUserInput) {
        EmbedBuilder eb = defaultEmbed("Invalid Arguments", null);
        eb.setDescription("**Please check your input!**");
        eb.addField("Argument(s) given:", invalidUserInput, true);
        eb.addField("Valid Arguments Example:", "• CrypticPlasma\n• CrypticPlasma Zucchini", true);
        eb.addBlankField(true);
        eb.addField("To retry,", "React with ✅", true);
        eb.addField("To cancel the application,", "React with ❌", true);
        eb.addBlankField(true);
        return eb;
    }
}