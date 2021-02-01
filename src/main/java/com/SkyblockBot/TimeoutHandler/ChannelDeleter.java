package com.SkyblockBot.TimeoutHandler;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static com.SkyblockBot.Utils.BotUtils.defaultEmbed;

public class ChannelDeleter extends ListenerAdapter {
    static List<TextChannel> channelsList = new ArrayList<>();

    public static void addChannel(TextChannel channel) {
        channelsList.add(channel);
    }

    public static void removeChannel(TextChannel channel) {
        channelsList.remove(channel);
    }

    @Override
    public void onReady(@NotNull ReadyEvent event) {
        final Runnable channelDeleter = this::updateChannels;
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(channelDeleter, 0, 30, TimeUnit.MINUTES);
    }

    public void updateChannels() {
        for (Iterator<TextChannel> iteratorCur = channelsList.iterator(); iteratorCur.hasNext(); ) {
            TextChannel currentChannel = iteratorCur.next();

            Instant lastMessageSentTime = currentChannel.retrieveMessageById(currentChannel.getLatestMessageId())
                    .complete().getTimeCreated().toInstant();
            long secondsSinceLast = Instant.now().getEpochSecond() - lastMessageSentTime.getEpochSecond();
            long secondsDiff = Instant.now().getEpochSecond()
                    - currentChannel.getTimeCreated().toInstant().getEpochSecond();
            EmbedBuilder eb = defaultEmbed("Channel Closing", null);
            if (secondsSinceLast / 3600 % 24 > 1) {
                eb.addField("Reason", "Inactive for an hour", false);
                currentChannel.sendMessage(eb.build()).queue();
                currentChannel.delete().reason("Exceeded inactivity time").queueAfter(15, TimeUnit.SECONDS);
                iteratorCur.remove();
            } else if ((secondsDiff / 3600 % 24) > 48) {
                eb.addField("Reason", "Open for 48 hours", false);
                currentChannel.sendMessage(eb.build()).queue();
                currentChannel.delete().reason("Exceeded max time").queueAfter(15, TimeUnit.SECONDS);
                iteratorCur.remove();
            }
        }
    }
}
