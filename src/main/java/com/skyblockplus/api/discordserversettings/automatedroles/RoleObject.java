package com.skyblockplus.api.discordserversettings.automatedroles;

import lombok.AllArgsConstructor;
import lombok.Data;

import javax.persistence.Embeddable;

@Data
@AllArgsConstructor
@Embeddable
public class RoleObject {

    private String value = "";
    private String roleId = "";

    public RoleObject() {
    }
}
