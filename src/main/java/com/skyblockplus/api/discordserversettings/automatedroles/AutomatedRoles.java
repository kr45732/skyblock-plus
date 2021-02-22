package com.skyblockplus.api.discordserversettings.automatedroles;

import lombok.AllArgsConstructor;
import lombok.Data;

import javax.persistence.Embeddable;
import javax.persistence.Embedded;

@Data
@AllArgsConstructor
@Embeddable
public class AutomatedRoles {
    private String enable = "false";

    @Embedded
    private RoleModel sven = new RoleModel();

    @Embedded
    private RoleModel rev = new RoleModel();

    @Embedded
    private RoleModel tara = new RoleModel();

    @Embedded
    private RoleModel bank_coins = new RoleModel();

    @Embedded
    private RoleModel alchemy = new RoleModel();

    @Embedded
    private RoleModel combat = new RoleModel();

    @Embedded
    private RoleModel fishing = new RoleModel();

    @Embedded
    private RoleModel farming = new RoleModel();

    @Embedded
    private RoleModel foraging = new RoleModel();

    @Embedded
    private RoleModel carpentry = new RoleModel();

    @Embedded
    private RoleModel mining = new RoleModel();

    @Embedded
    private RoleModel taming = new RoleModel();

    @Embedded
    private RoleModel enchanting = new RoleModel();

    @Embedded
    private RoleModel catacombs = new RoleModel();

    @Embedded
    private RoleModel guild_member = new RoleModel();

    public AutomatedRoles() {
    }
}
