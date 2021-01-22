package com.SkyblockBot.Verify;

import static com.SkyblockBot.Miscellaneous.BotUtils.defaultEmbed;
import static com.SkyblockBot.Miscellaneous.BotUtils.getJson;
import static com.SkyblockBot.Miscellaneous.BotUtils.higherDepth;
import static com.SkyblockBot.Miscellaneous.BotUtils.key;
import static com.SkyblockBot.Miscellaneous.ChannelDeleter.addChannel;
import static com.SkyblockBot.Miscellaneous.ChannelDeleter.removeChannel;

import java.util.EnumSet;
import java.util.concurrent.TimeUnit;

import com.google.gson.JsonElement;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Category;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class VerifyUser extends ListenerAdapter {
    Message reactMessage;
    User verifyingUser;
    TextChannel verifyChannel;
    int state = 0;
    JsonElement currentSettings;
    String[] playerInfo;

    public VerifyUser(MessageReactionAddEvent event, User verifyingUser, JsonElement currentSettings) {
        this.verifyingUser = verifyingUser;
        this.currentSettings = currentSettings;
        System.out.println("Verify: " + verifyingUser.getName());

        String channelPrefix = higherDepth(currentSettings, "new_channel_prefix").getAsString();
        Category verifyCategory = event.getGuild()
                .getCategoryById(higherDepth(higherDepth(currentSettings, "new_channel_category"), "id").getAsString());
        verifyCategory.createTextChannel(channelPrefix + "-" + verifyingUser.getName())
                .addPermissionOverride(event.getGuild().getMember(verifyingUser), EnumSet.of(Permission.VIEW_CHANNEL),
                        null)
                .addPermissionOverride(event.getGuild().getPublicRole(), null, EnumSet.of(Permission.VIEW_CHANNEL))
                .complete();

        verifyChannel = event.getGuild().getTextChannelsByName(channelPrefix + "-" + verifyingUser.getName(), true)
                .get(0);

        verifyChannel.sendMessage("Welcome " + verifyingUser.getAsMention() + "!").queue();

        addChannel(verifyChannel);
        EmbedBuilder welcomeEb = defaultEmbed("Verification for " + verifyingUser.getName(), null);
        welcomeEb.setDescription("• Please enter your in-game-name.\n• Ex: CrypticPlasma\n");
        welcomeEb.addField("To submit your LAST message,", "React with ✅", true);
        welcomeEb.addField("To cancel the verification,", "React with ❌", true);
        verifyChannel.sendMessage(welcomeEb.build()).queue(message -> {
            message.addReaction("✅").queue();
            message.addReaction("❌").queue();
            this.reactMessage = message;
        });

    }

    @Override
    public void onMessageReactionAdd(MessageReactionAddEvent event) {
        if (event.getMessageIdLong() != reactMessage.getIdLong()) {
            return;
        }
        if (event.getUser().isBot()) {
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
                verifyChannel.retrieveMessageById(verifyChannel.getLatestMessageId()).queue(messageReply -> {
                    if (messageReply.getAuthor().equals(verifyingUser)) {
                        try {
                            String username = messageReply.getContentDisplay();
                            playerInfo = getPlayerInfo(username).split(" ");

                            if (playerInfo.length != 0) {
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
                                } else {
                                    EmbedBuilder eb = defaultEmbed("Discord tag mismatch", null);
                                    eb.setDescription("Account " + playerInfo[1] + " is linked with the discord tag "
                                            + playerInfo[0] + "\nYour current discord tag is "
                                            + verifyingUser.getAsTag());
                                    eb.addField("To retry,", "React with ✅", true);
                                    eb.addField("To cancel the verification,", "React with ❌", true);
                                    verifyChannel.sendMessage(eb.build()).queue(message -> {
                                        message.addReaction("✅").queue();
                                        message.addReaction("❌").queue();
                                        this.reactMessage = message;

                                    });
                                    state = 2;
                                }
                            } else {
                                EmbedBuilder eb = invalidInput(messageReply.getContentDisplay());
                                verifyChannel.sendMessage(eb.build()).queue(message -> {
                                    message.addReaction("✅").queue();
                                    message.addReaction("❌").queue();
                                    this.reactMessage = message;
                                });
                                state = 2;
                            }

                        } catch (Exception ex) {
                            EmbedBuilder eb = invalidInput(messageReply.getContentDisplay());
                            verifyChannel.sendMessage(eb.build()).queue(message -> {
                                message.addReaction("✅").queue();
                                message.addReaction("❌").queue();
                                this.reactMessage = message;
                            });
                            state = 2;
                        }

                    } else {
                        EmbedBuilder eb = invalidInput(messageReply.getContentDisplay());
                        verifyChannel.sendMessage(eb.build()).queue(message -> {
                            message.addReaction("✅").queue();
                            message.addReaction("❌").queue();
                            this.reactMessage = message;
                        });
                        state = 2;
                    }
                });
                break;
            case 2:
                EmbedBuilder eb2 = defaultEmbed("Verification for " + verifyingUser.getName(), null);
                eb2.setDescription("• Please enter your in-game-name.\n• Ex: CrypticPlasma\n");
                eb2.addField("To submit your LAST message,", "React with ✅", true);
                eb2.addField("To cancel the verification,", "React with ❌", true);
                verifyChannel.sendMessage(eb2.build()).queue(message -> {
                    message.addReaction("✅").queue();
                    message.addReaction("❌").queue();
                    this.reactMessage = message;
                });
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

    public String getPlayerInfo(String username) {
        JsonElement playerJson = getJson("https://api.hypixel.net/player?key=" + key + "&name=" + username);

        if (playerJson == null) {
            return " ";
        }

        if (higherDepth(playerJson, "player").isJsonNull()) {
            return " ";
        }
        try {
            String discordID = higherDepth(
                    higherDepth(higherDepth(higherDepth(playerJson, "player"), "socialMedia"), "links"), "DISCORD")
                            .getAsString();
            return discordID + " " + higherDepth(higherDepth(playerJson, "player"), "displayname").getAsString();
        } catch (Exception e) {
            return " ";
        }
    }

    public EmbedBuilder invalidInput(String invalidUserInput) {
        EmbedBuilder eb = defaultEmbed("Invalid Arguments / Username", null);
        eb.setDescription("**Please check your input!**");
        eb.addField("Argument(s) given:", invalidUserInput, true);
        eb.addField("Valid Argument Example:", "CrypticPlasma", true);
        eb.addBlankField(true);
        eb.addField("To retry,", "React with ✅", true);
        eb.addField("To cancel the verification,", "React with ❌", true);
        eb.addBlankField(true);
        return eb;
    }
}
