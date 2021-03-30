package com.skyblockplus.dev;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.skyblockplus.api.discordserversettings.settingsmanagers.ServerSettingsModel;
import net.dv8tion.jda.api.entities.Guild;

import java.io.File;
import java.io.FileWriter;
import java.io.Writer;
import java.util.List;

import static com.skyblockplus.Main.database;
import static com.skyblockplus.Main.jda;
import static com.skyblockplus.utils.Utils.errorMessage;
import static com.skyblockplus.utils.Utils.logCommand;

public class GetSettingsFile extends Command {
    CommandEvent event;

    public GetSettingsFile() {
        this.name = "d-getsettings";
        this.ownerCommand = true;
    }

    @Override
    protected void execute(CommandEvent event) {
        new Thread(() -> {
            String content = event.getMessage().getContentRaw();
            String[] args = content.split(" ");
            this.event = event;

            logCommand(event.getGuild(), event.getAuthor(), content);

            if (args.length == 2) {
                if (args[1].equals("current")) {
                    if (getCurrentServerSettings(event.getGuild().getId())) {
                        return;
                    }
                } else if (args[1].equals("all")) {
                    if (getAllServerSettings()) {
                        return;
                    }
                } else {
                    if (getCurrentServerSettings(args[1])) {
                        return;
                    }
                }
            }

            event.getChannel().sendMessage(errorMessage(this.name).build()).queue();
        }).start();
    }

    private boolean getAllServerSettings() {
        List<ServerSettingsModel> allSettings = database.getAllServerSettings();
        if (allSettings == null) {
            return false;
        }

        try {
            String pathName = "src/main/java/com/skyblockplus/json/All_Settings.json";
            File file = new File(pathName);
            if (!file.createNewFile()) {
                file.delete();
                file.createNewFile();
            }

            Writer writer = new FileWriter(pathName);
            new GsonBuilder()
                    .setPrettyPrinting()
                    .create().toJson(allSettings, writer);
            writer.close();

            event.getChannel().sendMessage("All settings").addFile(file).complete();

            if (file.delete()) {
                return true;
            }

        } catch (Exception ignored) {
        }
        return false;
    }

    private boolean getCurrentServerSettings(String guildId) {
        Guild guild = jda.getGuildById(guildId);
        JsonElement currentSettings = database.getServerSettings(guildId);
        if (currentSettings == null || guild == null) {
            return false;
        }

        try {
            String pathName = "src/main/java/com/skyblockplus/json/" + guild.getName() + "_Settings.json";
            File file = new File(pathName);
            if (!file.createNewFile()) {
                file.delete();
                file.createNewFile();
            }

            Writer writer = new FileWriter(pathName);
            new GsonBuilder()
                    .setPrettyPrinting()
                    .create().toJson(currentSettings, writer);
            writer.close();

            event.getChannel().sendMessage("Settings for " + guild.getName()).addFile(file).complete();

            if (file.delete()) {
                return true;
            }

        } catch (Exception ignored) {
        }

        return false;
    }
}
