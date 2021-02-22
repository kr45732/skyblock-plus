package com.skyblockplus.miscellaneous;

import static com.skyblockplus.utils.BotUtils.defaultEmbed;
import static com.skyblockplus.utils.BotUtils.globalCooldown;
import static com.skyblockplus.utils.BotUtils.higherDepth;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import com.google.gson.JsonElement;
import com.google.gson.JsonIOException;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;

import net.dv8tion.jda.api.EmbedBuilder;

public class VersionCommand extends Command {

    public VersionCommand() {
        this.name = "version";
        this.guildOnly = false;
        this.cooldown = globalCooldown;
    }

    @Override
    protected void execute(CommandEvent event) {
        System.out.println(event.getMessage().getContentRaw());
        try {
            JsonElement patchNotes = JsonParser
                    .parseReader(new FileReader("src/main/java/com/skyblockplus/json/PatchNotes.json"));
            List<Integer> patchVersions = patchNotes.getAsJsonObject().entrySet().stream()
                    .map(i -> Integer.parseInt(i.getKey().replace(".", "")))
                    .collect(Collectors.toCollection(ArrayList::new));
            int latestVersion = Collections.max(patchVersions);
            JsonElement currentDesc = higherDepth(patchNotes, "" + latestVersion);
            EmbedBuilder eb = defaultEmbed("Version " + higherDepth(currentDesc, "version").getAsString());
            eb.setDescription(higherDepth(currentDesc, "description").getAsString());
            eb.setFooter("Released at");
            eb.setTimestamp(Instant.parse(higherDepth(currentDesc, "date").getAsString()));
            event.reply(eb.build());
        } catch (JsonIOException | JsonSyntaxException | FileNotFoundException e) {
            e.printStackTrace();
        }

    }
}
