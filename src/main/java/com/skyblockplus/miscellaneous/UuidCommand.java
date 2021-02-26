package com.skyblockplus.miscellaneous;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.skyblockplus.guilds.UsernameUuidStruct;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;

import static com.skyblockplus.utils.BotUtils.*;

public class UuidCommand extends Command {

    public UuidCommand() {
        this.name = "uuid";
        this.cooldown = globalCooldown;
    }

    @Override
    protected void execute(CommandEvent event) {
        EmbedBuilder eb = defaultEmbed("Loading...");
        Message ebMessage = event.getChannel().sendMessage(eb.build()).complete();
        String content = event.getMessage().getContentRaw();
        String[] args = content.split(" ");

        System.out.println(content);

        if (args.length == 2) {
            ebMessage.editMessage(getUuidPlayer(args[1]).build()).queue();
            return;
        }

        ebMessage.editMessage(errorMessage(this.name).build()).queue();
    }

    private EmbedBuilder getUuidPlayer(String username) {
        UsernameUuidStruct usernameUuid = usernameToUuidUsername(username);
        if (usernameUuid != null) {

            return defaultEmbed("Uuid for " + usernameUuid.playerUsername)
                    .setDescription("**Uuid:** " + usernameUuid.playerUuid);
        }
        return defaultEmbed("Invalid username");
    }
}