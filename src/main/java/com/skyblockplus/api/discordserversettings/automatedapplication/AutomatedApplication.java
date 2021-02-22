package com.skyblockplus.api.discordserversettings.automatedapplication;

import lombok.AllArgsConstructor;
import lombok.Data;

import javax.persistence.Embeddable;

@Data
@AllArgsConstructor
@Embeddable
public class AutomatedApplication {
    private String enable = "false"; // Enable or disable
    private String messageTextChannelId = ""; // Channel for react apply message
    private String messageStaffChannelId = ""; // Channel for staff to view incoming applications
    private String newChannelPrefix = ""; // New user application channel prefix
    private String newChannelCategory = ""; // Category where new channels will be put
    private String messageText = ""; // React message text
    private String acceptMessageText = ""; // Message sent to applicant if accepted
    private String denyMessageText = ""; // Message sent to applicant if denied
    private String staffPingRoleId = ""; // Role to ping when new application is sent to staff channel

    public AutomatedApplication() {
    }
}
