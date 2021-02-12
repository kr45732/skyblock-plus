package com.skyblockplus.reload;

import static com.skyblockplus.reload.ReloadEventWatcher.onApplyReload;
import static com.skyblockplus.reload.ReloadEventWatcher.onVerifyReload;
import static com.skyblockplus.utils.BotUtils.defaultEmbed;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;

public class ReloadCommand extends Command {
    Message ebMessage;

    public ReloadCommand() {
        this.name = "reload";
        this.guildOnly = true;
        this.cooldown = 60;
        this.userPermissions = new Permission[] { Permission.MANAGE_SERVER };
    }

    @Override
    protected void execute(CommandEvent event) {
        EmbedBuilder eb = defaultEmbed("Attempting to reload settings...", null);
        this.ebMessage = event.getChannel().sendMessage(eb.build()).complete();

        eb = defaultEmbed("Reload Settings for " + event.getGuild().getName(), null);
        eb.addField("Apply settings reload status", onApplyReload(event.getGuild().getId()), false);
        eb.addField("Verify settings reload status", onVerifyReload(event.getGuild().getId()), false);
        ebMessage.editMessage(eb.build()).queue();
    }
}
