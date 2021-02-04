package com.SkyblockBot.Verify;

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

import static com.SkyblockBot.TimeoutHandler.ChannelDeleter.addChannel;
import static com.SkyblockBot.TimeoutHandler.ChannelDeleter.removeChannel;
import static com.SkyblockBot.Utils.BotUtils.*;

public class VerifyUser extends ListenerAdapter {
    final User verifyingUser;
    final TextChannel verifyChannel;
    final JsonElement currentSettings;
    Message reactMessage;
    int state = 0;
    String[] playerInfo;

    public VerifyUser(MessageReactionAddEvent event, User verifyingUser, JsonElement currentSettings) {
        System.out.println("Verify: " + verifyingUser.getName());
        this.verifyingUser = verifyingUser;
        this.currentSettings = currentSettings;

        String channelPrefix = higherDepth(currentSettings, "new_channel_prefix").getAsString();
        Category verifyCategory = event.getGuild()
                .getCategoryById(higherDepth(higherDepth(currentSettings, "new_channel_category"), "id").getAsString());
        this.verifyChannel = verifyCategory.createTextChannel(channelPrefix + "-" + verifyingUser.getName())
                .addPermissionOverride(event.getGuild().getMember(verifyingUser), EnumSet.of(Permission.VIEW_CHANNEL),
                        null)
                .addPermissionOverride(event.getGuild().getPublicRole(), null, EnumSet.of(Permission.VIEW_CHANNEL))
                .complete();

        verifyChannel.sendMessage("Welcome " + verifyingUser.getAsMention() + "!").queue();

        addChannel(verifyChannel);
        EmbedBuilder welcomeEb = defaultEmbed("Verification for " + verifyingUser.getName(), null);
        welcomeEb.setDescription("• Please enter your in-game-name.\n• Ex: CrypticPlasma\n");
        welcomeEb.addField("To submit your LAST message,", "React with ✅", true);
        welcomeEb.addField("To cancel the verification,", "React with ❌", true);
        reactMessage = verifyChannel.sendMessage(welcomeEb.build()).complete();
        reactMessage.addReaction("✅").queue();
        reactMessage.addReaction("❌").queue();

    }

    @Override
    public void onMessageReactionAdd(MessageReactionAddEvent event) {
        if (event.getMessageIdLong() != reactMessage.getIdLong()) {
            return;
        }
        if (event.getUser().isBot()) {
            return;
        }

        if (!event.getUser().equals(verifyingUser)) {
            reactMessage.removeReaction(event.getReaction().getReactionEmote().getAsReactionCode(), event.getUser()).queue();
            return;
        }

        if (event.getReactionEmote().getName().equals("❌")) {
            state = 4;
        } else if (!event.getReactionEmote().getName().equals("✅")) {
            reactMessage.clearReactions(event.getReaction().getReactionEmote().getAsReactionCode()).queue();
            return;
        }
        reactMessage.clearReactions().queue();
        switch (state) {
            case 0:
                if (verifyChannel.hasLatestMessage()) {
                    Message messageReply = verifyChannel.retrieveMessageById(verifyChannel.getLatestMessageId()).complete();
                    if (messageReply.getAuthor().equals(verifyingUser)) {
                        String username = messageReply.getContentDisplay();
                        playerInfo = getPlayerDiscordInfo(username);

                        if (playerInfo != null) {
                            if (verifyingUser.getAsTag().equals(playerInfo[0])) {
                                EmbedBuilder eb = defaultEmbed("Verification successful!", null);
                                eb.setDescription("**You have successfully been verified as " + playerInfo[1]
                                        + "**\nChannel closing in 30 seconds...");
                                verifyChannel.sendMessage(eb.build()).queue();
                                verifyChannel.delete().reason("Verification successful").queueAfter(30,
                                        TimeUnit.SECONDS);
                                event.getGuild().addRoleToMember(event.getGuild().getMember(verifyingUser),
                                        event.getGuild().getRoleById(
                                                higherDepth(higherDepth(currentSettings, "verified_role"), "id")
                                                        .getAsString()))
                                        .queue();
                                removeChannel(verifyChannel);
                                event.getJDA().removeEventListener(this);
                                break;
                            }
                            EmbedBuilder eb = defaultEmbed("Discord tag mismatch", null);
                            eb.setDescription("Account " + playerInfo[1] + " is linked with the discord tag "
                                    + playerInfo[0] + "\nYour current discord tag is "
                                    + verifyingUser.getAsTag());
                            eb.addField("To retry,", "React with ✅", true);
                            eb.addField("To cancel the verification,", "React with ❌", true);
                            reactMessage = verifyChannel.sendMessage(eb.build()).complete();
                            reactMessage.addReaction("✅").queue();
                            reactMessage.addReaction("❌").queue();
                            state = 2;
                            break;
                        }
                        EmbedBuilder eb = defaultEmbed("Invalid Arguments / Username", null);
                        eb.setDescription("**Please check your input!**");
                        eb.addField("Argument(s) given:", messageReply.getContentDisplay(), true);
                        eb.addField("Valid Argument Example:", "CrypticPlasma", true);
                        eb.addBlankField(true);
                        eb.addField("To retry,", "React with ✅", true);
                        eb.addField("To cancel the verification,", "React with ❌", true);
                        eb.addBlankField(true);
                    }
                    EmbedBuilder invalidEmbed = defaultEmbed("Invalid arguments", null);
                    invalidEmbed.setDescription("**Please check your input!**");
                    invalidEmbed.addField("Argument(s) given:", messageReply.getContentDisplay(), true);
                    invalidEmbed.addField("Valid Arguments Example:", "• CrypticPlasma", true);
                    invalidEmbed.addBlankField(true);
                    invalidEmbed.addField("To retry,", "React with ✅", true);
                    invalidEmbed.addField("To cancel the application,", "React with ❌", true);
                    invalidEmbed.addBlankField(true);
                    reactMessage = verifyChannel.sendMessage(invalidEmbed.build()).complete();
                    reactMessage.addReaction("✅").queue();
                    reactMessage.addReaction("❌").queue();
                    state = 2;
                    break;
                }
                EmbedBuilder invalidEb = defaultEmbed("Invalid Arguments", null);
                invalidEb.setDescription("**Unable to get latest message**");
                invalidEb.addField("To retry,", "React with ✅", true);
                invalidEb.addField("To cancel the verification,", "React with ❌", true);
                reactMessage = verifyChannel.sendMessage(invalidEb.build()).complete();
                reactMessage.addReaction("✅").queue();
                reactMessage.addReaction("❌").queue();
                state = 2;
                break;
            case 2:
                EmbedBuilder eb2 = defaultEmbed("Verification for " + verifyingUser.getName(), null);
                eb2.setDescription("• Please enter your in-game-name.\n• Ex: CrypticPlasma\n");
                eb2.addField("To submit your LAST message,", "React with ✅", true);
                eb2.addField("To cancel the verification,", "React with ❌", true);
                reactMessage = verifyChannel.sendMessage(eb2.build()).complete();
                reactMessage.addReaction("✅").queue();
                reactMessage.addReaction("❌").queue();
                state = 0;
                break;
            case 4:
                EmbedBuilder eb4 = defaultEmbed("Canceling verification", null);
                eb4.setDescription("Channel closing in 5 seconds...");
                verifyChannel.sendMessage(eb4.build()).queue();
                event.getJDA().removeEventListener(this);
                removeChannel(verifyChannel);
                event.getGuild().getTextChannelById(event.getChannel().getId()).delete().reason("Verification canceled")
                        .queueAfter(5, TimeUnit.SECONDS);
                break;
        }
    }

}
