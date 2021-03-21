package com.skyblockplus.api.discordserversettings.automatedapplication;

import lombok.AllArgsConstructor;
import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.Lob;

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
    private String staffPingRoleId = ""; // Role to ping when new application is sent to staff channel

    private String previousMessageId = "";

    @Column(columnDefinition="TEXT")
    private String applyUsersCache = "";

    public AutomatedApplication() {
    }
}
