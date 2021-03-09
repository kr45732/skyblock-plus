package com.skyblockplus.miscellaneous;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import net.dv8tion.jda.api.EmbedBuilder;

import static com.skyblockplus.utils.Utils.*;

public class InviteCommand extends Command {
    public InviteCommand() {
        this.name = "invite";
        this.cooldown = globalCooldown;
    }

    @Override
    protected void execute(CommandEvent event) {
        logCommand(event.getGuild(), event.getAuthor(), event.getMessage().getContentRaw());

        EmbedBuilder eb = defaultEmbed("Invite Skyblock Plus");
        eb.addField("Invite me to your server", "[Click here](https://discord.com/api/oauth2/authorize?client_id=796791167366594592&permissions=403040336&scope=bot)", false);
        eb.addField("Join my server", "[Click here](https://discord.gg/DpcCAwMXwp)", false);
        eb.setThumbnail(
                "https://cdn.discordapp.com/attachments/803419567958392832/806994416530358302/skyblock-plus-logo.png");
        event.getChannel().sendMessage(eb.build()).queue();
    }
}
