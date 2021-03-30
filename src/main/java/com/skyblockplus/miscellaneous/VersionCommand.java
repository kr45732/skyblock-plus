package com.skyblockplus.miscellaneous;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import net.dv8tion.jda.api.EmbedBuilder;

import java.io.FileReader;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static com.skyblockplus.utils.Utils.*;

public class VersionCommand extends Command {

    public VersionCommand() {
        this.name = "version";
        this.cooldown = globalCooldown;
    }

    @Override
    protected void execute(CommandEvent event) {
        new Thread(() -> {
            logCommand(event.getGuild(), event.getAuthor(), event.getMessage().getContentRaw());

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
            } catch (Exception e) {
                event.reply(defaultEmbed("Error fetching patch notes").build());
            }
        }).start();
    }
}
