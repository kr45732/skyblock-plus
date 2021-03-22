package com.skyblockplus.api.discordserversettings.linkedaccounts;

import lombok.AllArgsConstructor;
import lombok.Data;

import javax.persistence.Embeddable;

@Data
@AllArgsConstructor
@Embeddable
public class LinkedAccount {
    private String discordId = "";
    private String minecraftUuid = "";

    public LinkedAccount() {
    }

}
