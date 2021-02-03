package com.SkyblockBot.Miscellaneous;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import net.dv8tion.jda.api.EmbedBuilder;

import java.io.File;

import static com.SkyblockBot.Utils.BotUtils.defaultEmbed;

public class InviteCommand extends Command {
    public InviteCommand() {
        this.name = "invite";
        this.guildOnly = false;
    }

    @Override
    protected void execute(CommandEvent event) {
        EmbedBuilder eb = defaultEmbed("Invite Skyblock Plus", "");
        eb.setDescription("I am currently in development and not open to the public yet!\nYou can see me in action in my server – [Skyblock Plus](https://discord.gg/DpcCAwMXwp) – or in [Skyblock Forceful](https://discord.gg/tp3jXrvSGa).");
        File file = new File("skyblock-plus-logo.png");
        eb.setImage("attachment://skyblock-plus-logo.png");
        event.getChannel().sendMessage(eb.build()).addFile(file, "skyblock-plus-logo.png").queue();
    }
}
