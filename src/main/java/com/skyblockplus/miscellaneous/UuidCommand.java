package com.skyblockplus.miscellaneous;

import static com.skyblockplus.utils.BotUtils.defaultEmbed;
import static com.skyblockplus.utils.BotUtils.errorMessage;
import static com.skyblockplus.utils.BotUtils.globalCooldown;
import static com.skyblockplus.utils.BotUtils.usernameToUuidUsername;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.skyblockplus.guilds.UsernameUuidStruct;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;

public class UuidCommand extends Command {
    Message ebMessage;

    public UuidCommand() {
        this.name = "uuid";
        this.guildOnly = false;
        this.cooldown = globalCooldown;
    }

    @Override
    protected void execute(CommandEvent event) {
        EmbedBuilder eb = defaultEmbed("Loading player data...", null);
        this.ebMessage = event.getChannel().sendMessage(eb.build()).complete();

        Message message = event.getMessage();
        String content = message.getContentRaw();

        String[] args = content.split(" ");
        if (args.length != 3) {
            eb = defaultEmbed(errorMessage(this.name), null);
            ebMessage.editMessage(eb.build()).queue();
            return;
        }

        for (String value : args) {
            System.out.print(value + " ");
        }
        System.out.println();

        if (args[1].equals("player")) {
            eb = getUuidPlayer(args[2]);
        } else {
            eb = defaultEmbed(errorMessage(this.name), null);
            ebMessage.editMessage(eb.build()).queue();
            return;
        }
        ebMessage.editMessage(eb.build()).queue();
    }

    private EmbedBuilder getUuidPlayer(String username) {
        UsernameUuidStruct usernameUuid = usernameToUuidUsername(username);
        if (usernameUuid != null) {

            return defaultEmbed("Uuid for " + usernameUuid.playerUsername, null)
                    .setDescription("**Uuid:** " + usernameUuid.playerUuid);
        }
        return defaultEmbed("Invalid username", null);
    }
}