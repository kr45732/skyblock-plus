package com.skyblockplus.api.discordserversettings.skyblockevent;

import lombok.AllArgsConstructor;
import lombok.Data;

import javax.persistence.Embeddable;

@Data
@AllArgsConstructor
@Embeddable
public class EventMember {
    private String username = "";
    private String uuid = "";
    private String startingAmount = "";
    private String profileName = "";

    public EventMember() {
    }
}
