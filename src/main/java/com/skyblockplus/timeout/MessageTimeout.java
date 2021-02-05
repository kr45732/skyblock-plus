package com.skyblockplus.timeout;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Message;
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

public class MessageTimeout extends ListenerAdapter {
    static final List<MessageTimeoutStruct> messageList = new ArrayList<>();
    JDA jda;

    public static void addMessage(Message message, Object eventListener) {
        messageList.add(new MessageTimeoutStruct(message, eventListener));
    }

    @Override
    public void onReady(@NotNull ReadyEvent event) {
        this.jda = event.getJDA();
        final Runnable channelDeleter = this::updateMessages;
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(channelDeleter, 0, 1, TimeUnit.MINUTES);
    }

    public void updateMessages() {
        for (Iterator<MessageTimeoutStruct> iteratorCur = messageList.iterator(); iteratorCur.hasNext(); ) {
            MessageTimeoutStruct currentMessageStruct = iteratorCur.next();
            Message currentMessage = currentMessageStruct.message;
            long secondsSinceLast = Instant.now().getEpochSecond() - currentMessage.getTimeCreated().toInstant().getEpochSecond();
            if (secondsSinceLast > 30) {
                currentMessage.clearReactions().complete();
                currentMessage.addReaction("\uD83C\uDDF9").queue();
                currentMessage.addReaction("\uD83C\uDDEE").queue();
                currentMessage.addReaction("\uD83C\uDDF2").queue();
                currentMessage.addReaction("\uD83C\uDDEA").queue();
                currentMessage.addReaction("\uD83C\uDDF4").queue();
                currentMessage.addReaction("\uD83C\uDDFA").queue();
                currentMessage.addReaction(":regional_indicator_t_2:805833620924923984").queue();

                iteratorCur.remove();
                jda.removeEventListener(currentMessageStruct.eventListener);
            }
        }
    }
}
