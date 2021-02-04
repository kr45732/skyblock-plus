package com.SkyblockBot.API.Models;

import net.dv8tion.jda.api.entities.Role;

public class GuildRoleModel {
    private final String roleName;
    private final String roleId;
    public GuildRoleModel(Role curRole) {
        this.roleName = curRole.getName();
        this.roleId = curRole.getId();
    }

    public String getRoleName() {
        return roleName;
    }

    public String getRoleId() {
        return roleId;
    }
}
