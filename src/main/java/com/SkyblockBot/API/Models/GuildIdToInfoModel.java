package com.SkyblockBot.API.Models;

import net.dv8tion.jda.api.entities.Category;
import net.dv8tion.jda.api.entities.GuildChannel;
import net.dv8tion.jda.api.entities.Role;

import java.util.List;

public class GuildIdToInfoModel {
    private final String name;
    private final String id;
    private final List<Role> guildRoles;
    private final List<GuildChannel> guildChannels;
    private final List<Category> guildCategories;

    public GuildIdToInfoModel(String name, String id, List<Role> guildRoles, List<GuildChannel> guildChannels, List<Category> guildCategories) {
        this.name = name;
        this.id = id;
        this.guildRoles = guildRoles;
        this.guildChannels = guildChannels;
        this.guildCategories = guildCategories;
    }

    public String getName() {
        return name;
    }

    public String getId() {
        return id;
    }

    public List<Role> getGuildRoles() {
        return guildRoles;
    }

    public List<GuildChannel> getGuildChannels() {
        return guildChannels;
    }

    public List<Category> getGuildCategories() {
        return guildCategories;
    }
}
