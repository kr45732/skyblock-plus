package com.skyblockplus.timeout;

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

import static com.skyblockplus.Main.jda;

public class MessageTimeout extends ListenerAdapter {
    private static final List<MessageTimeoutStruct> messageList = new ArrayList<>();

    public static void addMessage(Message message, Object eventListener) {
        messageList.add(new MessageTimeoutStruct(message, eventListener));
    }

    public static void removeMessage(Object eventListener) {
        for (Iterator<MessageTimeoutStruct> iteratorCur = messageList.iterator(); iteratorCur.hasNext(); ) {
            MessageTimeoutStruct currentMessage = iteratorCur.next();
            if (currentMessage.eventListener.equals(eventListener)) {
                iteratorCur.remove();
                break;
            }
        }
    }

    @Override
    public void onReady(@NotNull ReadyEvent event) {
        final Runnable channelDeleter = this::updateMessages;
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(channelDeleter, 0, 1, TimeUnit.MINUTES);
    }

    public void updateMessages() {
        for (Iterator<MessageTimeoutStruct> iteratorCur = messageList.iterator(); iteratorCur.hasNext(); ) {
            MessageTimeoutStruct currentMessageStruct = iteratorCur.next();
            Message currentMessage = currentMessageStruct.message;
            long secondsSinceLast = Instant.now().getEpochSecond()
                    - currentMessage.getTimeCreated().toInstant().getEpochSecond();
            if (secondsSinceLast > 30) {
                currentMessage.clearReactions().queue();

                iteratorCur.remove();
                jda.removeEventListener(currentMessageStruct.eventListener);
            }
        }
    }
}
