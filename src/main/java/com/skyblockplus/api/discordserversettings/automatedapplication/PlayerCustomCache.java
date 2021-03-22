package com.skyblockplus.api.discordserversettings.automatedapplication;

import lombok.AllArgsConstructor;
import lombok.Data;

import javax.persistence.Embeddable;

@Data
@AllArgsConstructor
@Embeddable
public class PlayerCustomCache {
    private String validPlayer = "false";
    private String profileStr;
    private String levelTablesStr;
    private String outProfileStr;
    private String playerUuid;
    private String playerUsername;
    private String profileName;

    public PlayerCustomCache(){}
}
