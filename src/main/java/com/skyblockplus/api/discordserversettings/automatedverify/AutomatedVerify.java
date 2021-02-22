package com.skyblockplus.api.discordserversettings.automatedverify;

import lombok.AllArgsConstructor;
import lombok.Data;

import javax.persistence.Embeddable;

@Data
@AllArgsConstructor
@Embeddable
public class AutomatedVerify {
    private String enable = "false"; // Enable or disable
    private String messageText = ""; // Message that will be reacted to verify
    private String messageTextChannelId = ""; // Channel where react message will be sent
    private String verifiedRole = ""; // Role given to people who verify successfully
    private String newChannelPrefix = ""; // Channel prefix of a new verify
    private String newChannelCategory = ""; // Where new verify channels will be made

    public AutomatedVerify() {
    }
}
