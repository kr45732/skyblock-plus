package com.skyblockplus.apply;

import com.google.gson.JsonElement;
import com.skyblockplus.utils.Player;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.util.concurrent.TimeUnit;

import static com.skyblockplus.reload.ReloadEventWatcher.addApplySubEventListener;
import static com.skyblockplus.timeout.ChannelDeleter.removeChannel;
import static com.skyblockplus.timeout.EventListenerDeleter.addEventListener;
import static com.skyblockplus.utils.BotUtils.defaultEmbed;
import static com.skyblockplus.utils.BotUtils.higherDepth;

public class ApplyStaff extends ListenerAdapter {
    private final User user;
    private final TextChannel applyChannel;
    private final Message reactMessage;
    private final TextChannel staffChannel;
    private final JsonElement currentSettings;
    private final Player player;
    private Message deleteChannelMessage;

    public ApplyStaff(User user, TextChannel applyChannel, EmbedBuilder ebMain, JsonElement currentSettings,
                      Player player) {
        this.user = user;
        this.applyChannel = applyChannel;
        this.currentSettings = currentSettings;
        this.player = player;
        staffChannel = applyChannel.getJDA()
                .getTextChannelById(higherDepth(currentSettings, "messageStaffChannelId").getAsString());

        ebMain.addField("To accept the application,", "React with ✅", true);
        ebMain.addBlankField(true);
        ebMain.addField("To deny the application,", "React with ❌", true);
        staffChannel.sendMessage("<@&" + higherDepth(currentSettings, "staffPingRoleId").getAsString() + ">")
                .complete();
        reactMessage = staffChannel.sendMessage(ebMain.build()).complete();
        reactMessage.addReaction("✅").queue();
        reactMessage.addReaction("❌").queue();

        addApplySubEventListener(reactMessage.getGuild().getId(), this);
        addEventListener(this.reactMessage.getGuild().getId(), this.reactMessage.getChannel().getId(), this);
    }

    @Override
    public void onMessageReactionAdd(MessageReactionAddEvent event) {
        if (event.getUser().isBot()) {
            return;
        }
        try {
            if (event.getMessageIdLong() == deleteChannelMessage.getIdLong()) {
                if (event.getReactionEmote().getName().equals("✅")) {
                    deleteChannelMessage.clearReactions().queue();
                    EmbedBuilder eb = defaultEmbed("Channel closing in 10 seconds");
                    applyChannel.sendMessage(eb.build()).queue();
                    applyChannel.delete().reason("Applicant read final message").queueAfter(10, TimeUnit.SECONDS);
                    removeChannel(applyChannel);
                    event.getJDA().removeEventListener(this);
                    return;
                } else {
                    event.getReaction().removeReaction(user).queue();
                }
                return;
            }
        } catch (Exception ignored) {

        }
        if (event.getMessageIdLong() != reactMessage.getIdLong()) {
            return;
        }

        if (event.getReactionEmote().getName().equals("❌")) {
            staffChannel.sendMessage(player.getUsername() + " (" + user.getAsMention() + ") was denied by "
                    + event.getUser().getName() + " (" + event.getUser().getAsMention() + ")").queue();
            reactMessage.clearReactions().queue();
            EmbedBuilder eb = defaultEmbed("Application Not Accepted");
            eb.setDescription(higherDepth(currentSettings, "denyMessageText").getAsString()
                    + "\n**React with ✅ to confirm that you have read this message and to close channel**");
            applyChannel.sendMessage(user.getAsMention()).queue();
            deleteChannelMessage = applyChannel.sendMessage(eb.build()).complete();
            deleteChannelMessage.addReaction("✅").queue();
            reactMessage.delete().queueAfter(5, TimeUnit.SECONDS);
            addEventListener(this.deleteChannelMessage.getGuild().getId(),
                    this.deleteChannelMessage.getChannel().getId(), this);
        } else if (event.getReactionEmote().getName().equals("✅")) {
            staffChannel.sendMessage(player.getUsername() + " (" + user.getAsMention() + ") was accepted by "
                    + event.getUser().getName() + " (" + event.getUser().getAsMention() + ")").queue();
            reactMessage.clearReactions().queue();
            EmbedBuilder eb = defaultEmbed("Application Accepted");
            eb.setDescription(higherDepth(currentSettings, "acceptMessageText").getAsString()
                    + "\n**React with ✅ to confirm that you have read this message and to close channel**");
            applyChannel.sendMessage(user.getAsMention()).queue();
            deleteChannelMessage = applyChannel.sendMessage(eb.build()).complete();
            deleteChannelMessage.addReaction("✅").queue();
            reactMessage.delete().queueAfter(5, TimeUnit.SECONDS);
            addEventListener(this.deleteChannelMessage.getGuild().getId(),
                    this.deleteChannelMessage.getChannel().getId(), this);
        }
    }

}
