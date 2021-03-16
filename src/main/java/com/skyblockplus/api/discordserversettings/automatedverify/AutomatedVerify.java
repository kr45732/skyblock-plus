package com.skyblockplus.api.discordserversettings.automatedverify;

import lombok.AllArgsConstructor;
import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Embeddable;

@Data
@AllArgsConstructor
@Embeddable
public class AutomatedVerify {
    private String enable = "false"; // Enable or disable

    @Column(length = 2048)
    private String messageText = ""; // Message that will be reacted to verify

    private String messageTextChannelId = ""; // Channel where react message will be sent
    private String verifiedRole = ""; // Role given to people who verify successfully
    private String verifiedNickname = "";

    private String previousMessageId = "";

    public AutomatedVerify() {
    }
}
