package com.skyblockplus.api.service;

import com.skyblockplus.api.models.*;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

import static com.skyblockplus.Main.jda;

@RestController
public class GuildController {

    @GetMapping("/api/guild/mutualGuilds")
    public Object getMutualGuilds(@RequestParam(value = "userId") String userId) {
        try {
            User user = jda.getUserById(userId);
            List<GuildModel> guildList = new ArrayList<>();
            for (Guild guild : user.getMutualGuilds()) {
                if (guild.getMember(user).hasPermission(Permission.MANAGE_SERVER)) {
                    guildList.add(new GuildModel(guild.getName(), guild.getId()));
                }
            }
            return new Template("true", guildList);
        } catch (Exception e) {
            return new ErrorTemplate("false", "Invalid field [userId]");
        }
    }

    @GetMapping("/api/guild/channels/text")
    public Object getGuildChannels(@RequestParam(value = "guildId") String guildId) {
        try {
            Guild guild = jda.getGuildById(guildId);
            List<GuildChannelModel> guildChannels = new ArrayList<>();
            guild.getChannels().forEach(curChannel -> {
                if (curChannel.getType().toString().equals("TEXT")) {
                    guildChannels.add(new GuildChannelModel(curChannel.getName(), curChannel.getId()));
                }
            });
            return new Template("true", guildChannels);
        } catch (Exception e) {
            return new ErrorTemplate("false", "Invalid field [guildId]");
        }
    }

    @GetMapping("/api/guild/categories")
    public Object getGuildCategories(@RequestParam(value = "guildId") String guildId) {
        try {
            Guild guild = jda.getGuildById(guildId);
            List<GuildCategoryModel> guildCategories = new ArrayList<>();
            guild.getCategories().forEach(curCategory -> {
                guildCategories.add(new GuildCategoryModel(curCategory.getName(), curCategory.getId()));
            });
            return new Template("true", guildCategories);
        } catch (Exception e) {
            return new ErrorTemplate("false", "Invalid field [guildId]");
        }
    }

    @GetMapping("/api/guild/roles")
    public Object getGuildRoles(@RequestParam(value = "guildId") String guildId) {
        try {
            Guild guild = jda.getGuildById(guildId);
            List<GuildRoleModel> guildRoles = new ArrayList<>();
            guild.getRoles().forEach(curRole -> {

                if (!(curRole.isPublicRole() || curRole.isManaged())) {
                    guildRoles.add(new GuildRoleModel(curRole.getName(), curRole.getId()));
                }
            });
            return new Template("true", guildRoles);
        } catch (Exception e) {
            return new ErrorTemplate("false", "Invalid field [guildId]");
        }
    }

    @GetMapping("/api/guild/name")
    public Object getGuildName(@RequestParam(value = "guildId") String guildId) {
        try {
            Guild guild = jda.getGuildById(guildId);
            return new Template("true", new GuildModel(guild.getName(), guild.getId()));
        } catch (Exception e) {
            return new ErrorTemplate("false", "Invalid field [guildId]");
        }
    }
}
