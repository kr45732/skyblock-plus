package com.skyblockplus.api.models.guild;

public class GuildRoleModel {
    private final String roleName;
    private final String roleId;

    public GuildRoleModel(String roleName, String roleId) {
        this.roleName = roleName;
        this.roleId = roleId;
    }

    public String getRoleName() {
        return roleName;
    }

    public String getRoleId() {
        return roleId;
    }
}
