package com.skyblockplus.guildroles;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static com.skyblockplus.Main.database;
import static com.skyblockplus.utils.Utils.*;

public class GuildRolesGuild extends ListenerAdapter {
    private final Guild guild;

    public GuildRolesGuild(Guild guild){
        this.guild = guild;
        final Runnable channelDeleter = this::updateGuildRolesRunner;
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(channelDeleter, 0, 6, TimeUnit.HOURS);
    }

    public void updateGuildRolesRunner(){
        JsonElement currentSettings = database.getGuildRoleSettings(guild.getId());

        if(currentSettings == null){
            return;
        }

        if(higherDepth(currentSettings, "enableGuildRole").getAsBoolean()){
            updateGuildRoles(currentSettings);
        }
    }

    public void updateGuildRoles(JsonElement currentSettings){
        long startTime = System.currentTimeMillis();

        Role role = guild.getRoleById(higherDepth(currentSettings, "roleId").getAsString());
        JsonElement guildJson = getJson("https://api.hypixel.net/guild?key=" + HYPIXEL_API_KEY + "&id=" + higherDepth(currentSettings, "guildId").getAsString());

        if(role == null || guildJson.isJsonNull()){
            return;
        }

        JsonArray guildMembers = higherDepth(higherDepth(guildJson, "guild"), "members").getAsJsonArray();
        List<String> guildMembersUuids = new ArrayList<>();

        for(JsonElement guildMember:guildMembers ){
            guildMembersUuids.add(higherDepth(guildMember, "uuid").getAsString());
        }

        JsonArray linkedUsers = database.getLinkedUsers(guild.getId()).getAsJsonArray();

        for(JsonElement linkedUser: linkedUsers){
            if(guildMembersUuids.contains(higherDepth(linkedUser, "minecraftUuid").getAsString())){
                guild.addRoleToMember(higherDepth(linkedUser, "discordId").getAsString(), role).queue();
            }else{
                guild.removeRoleFromMember(higherDepth(linkedUser, "discordId").getAsString(), role).queue();
            }
        }

        System.out.println("GuildRolesGuild: " + (System.currentTimeMillis() - startTime)/1000 + "s (" + guild.getName() + ")");
    }
}
