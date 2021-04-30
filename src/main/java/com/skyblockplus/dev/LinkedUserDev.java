package com.skyblockplus.dev;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;

import java.io.File;
import java.io.FileWriter;
import java.io.Writer;

import static com.skyblockplus.Main.database;
import static com.skyblockplus.utils.Utils.*;

public class LinkedUserDev extends Command {

    public LinkedUserDev() {
        this.name = "d-linked";
        this.ownerCommand = true;
    }

    @Override
    protected void execute(CommandEvent event) {
        new Thread(() -> {
            EmbedBuilder eb = loadingEmbed();
            Message ebMessage = event.getChannel().sendMessage(eb.build()).complete();
            String content = event.getMessage().getContentRaw();
            String[] args = content.split(" ");

            logCommand(event.getGuild(), event.getAuthor(), content);

            if (args.length == 4) {
                if (args[1].equals("delete")) {
                    switch (args[2]) {
                        case "discordId":
                            database.deleteLinkedUserByDiscordId(args[3]);
                            ebMessage.editMessage(defaultEmbed("Done").build()).queue();
                            return;
                        case "username":
                            database.deleteLinkedUserByMinecraftUsername(args[3]);
                            ebMessage.editMessage(defaultEmbed("Done").build()).queue();
                            return;
                        case "uuid":
                            database.deleteLinkedUserByMinecraftUuid(args[3]);
                            ebMessage.editMessage(defaultEmbed("Done").build()).queue();
                            return;
                    }
                }
            } else if (args.length == 2) {
                if (args[1].equals("all")) {
                    if (getAllLinkedUsers(event)) {
                        ebMessage.delete().queue();
                        return;
                    }
                }
            }

            ebMessage.editMessage(errorMessage(this.name).build()).queue();
        }).start();
    }

    private boolean getAllLinkedUsers(CommandEvent event) {
        JsonElement allSettings = database.getLinkedUsers();
        if (allSettings == null) {
            return false;
        }

        try {
            String pathName = "src/main/java/com/skyblockplus/json/All_Linked_Users.json";
            File file = new File(pathName);
            if (!file.createNewFile()) {
                file.delete();
                file.createNewFile();
            }

            Writer writer = new FileWriter(pathName);
            new GsonBuilder().setPrettyPrinting().create().toJson(allSettings, writer);
            writer.close();

            event.getChannel().sendMessage("All linked users").addFile(file).complete();

            if (file.delete()) {
                return true;
            }

        } catch (Exception ignored) {
        }
        return false;
    }

}
