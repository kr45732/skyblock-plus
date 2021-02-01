package com.SkyblockBot.Miscellaneous;

import net.dv8tion.jda.api.entities.Message;

public class MessageTimeoutStruct {
    public Message message;
    public Object eventListener;

    public MessageTimeoutStruct(Message message, Object eventListener) {
        this.message = message;
        this.eventListener = eventListener;
    }
}
