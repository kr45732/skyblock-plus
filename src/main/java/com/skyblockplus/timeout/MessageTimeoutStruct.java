package com.skyblockplus.timeout;

import net.dv8tion.jda.api.entities.Message;

public class MessageTimeoutStruct {
    public final Message message;
    public final Object eventListener;

    public MessageTimeoutStruct(Message message, Object eventListener) {
        this.message = message;
        this.eventListener = eventListener;
    }
}
