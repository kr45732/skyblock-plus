package com.skyblockplus.miscellaneous;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import net.dv8tion.jda.api.EmbedBuilder;

import static com.skyblockplus.utils.BotUtils.defaultEmbed;

public class InviteCommand extends Command {
    public InviteCommand() {
        this.name = "invite";
        this.guildOnly = false;
    }

    @Override
    protected void execute(CommandEvent event) {
        EmbedBuilder eb = defaultEmbed("Invite Skyblock Plus", "");
        eb.setDescription("I am currently in development and not open to the public yet!\nYou can see me in action in my server – [Skyblock Plus](https://discord.gg/DpcCAwMXwp) – or in [Skyblock Forceful](https://discord.gg/tp3jXrvSGa).");
        eb.setImage("https://cdn.discordapp.com/attachments/803419567958392832/806994416530358302/skyblock-plus-logo.png");
        event.getChannel().sendMessage(eb.build()).queue();
    }
}
