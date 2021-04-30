package com.skyblockplus.api.discordserversettings.automatedapplication;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Embeddable;
import javax.persistence.FetchType;
import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
@Embeddable
public class AutomatedApplication {
    private String enable = "false"; // Enable or disable
    private String messageTextChannelId = ""; // Channel for react apply message
    private String messageStaffChannelId = ""; // Channel for staff to view incoming applications
    private String newChannelPrefix = ""; // New user application channel prefix
    private String newChannelCategory = ""; // Category where new channels will be put

    @Column(length = 2048)
    private String messageText = ""; // React message text

    @Column(length = 2048)
    private String acceptMessageText = ""; // Message sent to applicant if accepted

    @Column(length = 2048)
    private String denyMessageText = ""; // Message sent to applicant if denied

    @Column(length = 2048)
    private String waitlistedMessageText = ""; // Message sent to applicant if waitlisted

    private String staffPingRoleId = ""; // Role to ping when new application is sent to staff channel

    private String previousMessageId = "";

    @Column(columnDefinition = "TEXT")
    private String applyUsersCache = "";

    @ElementCollection(fetch = FetchType.EAGER)
    @Fetch(value = FetchMode.SUBSELECT)
    private List<ApplyRequirements> applyReqs = new ArrayList<>();

    public AutomatedApplication() {
    }
}
