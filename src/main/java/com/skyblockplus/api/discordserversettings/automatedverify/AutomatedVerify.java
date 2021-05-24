package com.skyblockplus.api.discordserversettings.automatedverify;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Embeddable;
import javax.persistence.FetchType;

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

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

    @ElementCollection(fetch = FetchType.EAGER)
    @Fetch(value = FetchMode.SUBSELECT)
    private List<String> verifiedRoles = new ArrayList<>();

    private String verifiedNickname = "";

    private String previousMessageId = "";

    public AutomatedVerify() {
    }
}
