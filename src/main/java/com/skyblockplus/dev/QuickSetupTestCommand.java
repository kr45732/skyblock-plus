package com.skyblockplus.dev;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.skyblockplus.api.discordserversettings.automatedroles.RoleModel;
import com.skyblockplus.api.discordserversettings.settingsmanagers.ServerSettingsModel;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;

import static com.skyblockplus.Main.database;
import static com.skyblockplus.utils.Utils.defaultEmbed;
import static com.skyblockplus.utils.Utils.higherDepth;

public class QuickSetupTestCommand extends Command {
    private CommandEvent event;

    public QuickSetupTestCommand() {
        this.name = "q-setup";
        this.ownerCommand = true;
    }

    @Override
    protected void execute(CommandEvent event) {
        this.event = event;
        EmbedBuilder eb = defaultEmbed("Loading...", null);
        Message ebMessage = event.getChannel().sendMessage(eb.build()).complete();
        String content = event.getMessage().getContentRaw();
        String[] args = content.split(" ", 4);

        System.out.println(content);

        if (args.length >= 4) {
            if (args[1].equals("roles")) {
                ebMessage.editMessage(setRoleSettings(args[2], args[3]).build()).queue();
                return;
            }
        }
        ebMessage.editMessage(defaultEmbed("Invalid input").build()).queue();
    }

    private EmbedBuilder setRoleSettings(String roleName, String json) {
        try {
            JsonElement jsonElement = new Gson().toJsonTree(new Gson().fromJson(json, RoleModel.class));
            if (higherDepth(database.getServerSettings(event.getGuild().getId()), "serverId") == null) {
                database.addNewServerSettings(event.getGuild().getId(),
                        new ServerSettingsModel(event.getGuild().getName(), event.getGuild().getId()));
            }

            int responseCode = database.updateRoleSettings(event.getGuild().getId(), roleName, jsonElement);
            return defaultEmbed("API returned response code: " + responseCode);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return defaultEmbed("Error updating settings");
    }
}
