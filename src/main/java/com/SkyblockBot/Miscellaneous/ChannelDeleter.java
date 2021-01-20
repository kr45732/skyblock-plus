package com.SkyblockBot.Miscellaneous;

import static com.SkyblockBot.Miscellaneous.BotUtils.defaultEmbed;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class ChannelDeleter extends ListenerAdapter {
    static List<TextChannel> applicationChannelsList = new ArrayList<TextChannel>();

    @Override
    public void onReady(ReadyEvent event) {
        final Runnable channelDeleter = new Runnable() {
            public void run() {
                updateChannels();
            }
        };
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(channelDeleter, 0, 30, TimeUnit.MINUTES);
    }

    public static void addChannel(TextChannel channel) {
        applicationChannelsList.add(channel);
    }

    public static void removeChannel(TextChannel channel) {
        applicationChannelsList.remove(channel);
    }

    public void updateChannels() {
        for (TextChannel currentChannel : applicationChannelsList) {
            Instant lastMessageSentTime = currentChannel.retrieveMessageById(currentChannel.getLatestMessageId())
                    .complete().getTimeCreated().toInstant();
            long secondsSinceLast = Instant.now().getEpochSecond() - lastMessageSentTime.getEpochSecond();
            long secondsDiff = Instant.now().getEpochSecond()
                    - currentChannel.getTimeCreated().toInstant().getEpochSecond();
            EmbedBuilder eb = defaultEmbed("Channel Closing", null);
            if (secondsSinceLast / 3600 % 24 > 1) {
                eb.addField("Reason", "Inactive for an hour", false);
                currentChannel.sendMessage(eb.build()).queue();
                currentChannel.delete().reason("Exceeded inactivity time for application").queueAfter(15,
                        TimeUnit.SECONDS);
                applicationChannelsList.remove(currentChannel);
            } else if (secondsDiff / 3600 % 24 > 48) {
                eb.addField("Reason", "Open for 48 hours", false);
                currentChannel.sendMessage(eb.build()).queue();
                currentChannel.delete().reason("Exceeded max time for application").queueAfter(15, TimeUnit.SECONDS);
                applicationChannelsList.remove(currentChannel);
            }
        }
    }
}
