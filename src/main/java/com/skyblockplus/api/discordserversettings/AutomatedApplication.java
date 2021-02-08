package com.skyblockplus.api.discordserversettings;

import java.io.Serializable;

public class AutomatedApplication implements Serializable {
    private boolean enable;
    private String messageTextChannelId;
    private String messageStaffChannelId;
    private String newChannelPrefix;
    private String messageText;
    private String applyMessageText;
    private String denyMessageText;
    private String staffPingRoleId;

    public AutomatedApplication() {
    }

    public AutomatedApplication(boolean enable, String messageTextChannelId, String messageStaffChannelId, String newChannelPrefix, String messageText, String applyMessageText, String denyMessageText, String staffPingRoleId) {
        this.enable = enable;
        this.messageTextChannelId = messageTextChannelId;
        this.messageStaffChannelId = messageStaffChannelId;
        this.newChannelPrefix = newChannelPrefix;
        this.messageText = messageText;
        this.applyMessageText = applyMessageText;
        this.denyMessageText = denyMessageText;
        this.staffPingRoleId = staffPingRoleId;
    }

    public boolean isEnable() {
        return enable;
    }

    public void setEnable(boolean enable) {
        this.enable = enable;
    }

    public String getMessageTextChannelId() {
        return messageTextChannelId;
    }

    public void setMessageTextChannelId(String messageTextChannelId) {
        this.messageTextChannelId = messageTextChannelId;
    }

    public String getMessageStaffChannelId() {
        return messageStaffChannelId;
    }

    public void setMessageStaffChannelId(String messageStaffChannelId) {
        this.messageStaffChannelId = messageStaffChannelId;
    }

    public String getNewChannelPrefix() {
        return newChannelPrefix;
    }

    public void setNewChannelPrefix(String newChannelPrefix) {
        this.newChannelPrefix = newChannelPrefix;
    }

    public String getMessageText() {
        return messageText;
    }

    public void setMessageText(String messageText) {
        this.messageText = messageText;
    }

    public String getApplyMessageText() {
        return applyMessageText;
    }

    public void setApplyMessageText(String applyMessageText) {
        this.applyMessageText = applyMessageText;
    }

    public String getDenyMessageText() {
        return denyMessageText;
    }

    public void setDenyMessageText(String denyMessageText) {
        this.denyMessageText = denyMessageText;
    }

    public String getStaffPingRoleId() {
        return staffPingRoleId;
    }

    public void setStaffPingRoleId(String staffPingRoleId) {
        this.staffPingRoleId = staffPingRoleId;
    }
}
