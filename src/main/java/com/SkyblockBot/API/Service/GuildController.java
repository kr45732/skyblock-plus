package com.SkyblockBot.API.Service;

import com.SkyblockBot.API.Models.*;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import static com.SkyblockBot.Main.jda;

@RestController
public class GuildController {

    @GetMapping("/api/mutualGuilds")
    public Object getMutualGuilds(@RequestParam(value = "userId") String userId) {
        try{
            User user = jda.getUserById(userId);
            List<GuildModel> guildList = new ArrayList<>();
            for(Guild guild:user.getMutualGuilds()){
                if(guild.getMember(user).hasPermission(Permission.MANAGE_SERVER)){
                    guildList.add(new GuildModel(guild.getName(), guild.getId()));
                }
            }
            return new Template("true", guildList);
        } catch (Exception e){
            return new ErrorTemplate("false", "invalid userId");
        }
    }

    @GetMapping("/api/guildInfo")
    public Object getGuildInfo(@RequestParam(value = "guildId") String guildId) {
        try{
            Guild guild = jda.getGuildById(guildId);

            List<GuildRoleModel> guildRoles = null;
            for (Role curRole : guild.getRoles()) {
                guildRoles.add(new GuildRoleModel(curRole));
            }

            List<GuildChannel> guildChannels = guild.getChannels();
            List<Category> guildCategories = guild.getCategories();

            return new Template("true", guildRoles);
//            return new GuildIdToInfoModel(guild.getName(), guild.getId(), guildRoles, guildChannels, guildCategories);
        } catch (Exception e){
            return new ErrorTemplate("false", "invalid guildId");
        }
    }
}
