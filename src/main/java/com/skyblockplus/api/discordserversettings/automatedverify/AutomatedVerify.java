package com.skyblockplus.api.discordserversettings.automatedverify;

import javax.persistence.Column;
import javax.persistence.Embeddable;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@Embeddable
public class AutomatedVerify {
    private String enable = "false";

    @Column(length = 2048)
    private String messageText = "";

    private String messageTextChannelId = "";
    private String verifiedRole = "";
    private String verifiedNickname = "";

    private String previousMessageId = "";

    public AutomatedVerify() {
    }
}
