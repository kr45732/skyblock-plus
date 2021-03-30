package com.skyblockplus.miscellaneous;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.sun.management.OperatingSystemMXBean;
import net.dv8tion.jda.api.EmbedBuilder;

import java.lang.management.ManagementFactory;

import static com.skyblockplus.Main.jda;
import static com.skyblockplus.utils.Utils.*;

public class AboutCommand extends Command {
    public AboutCommand() {
        this.name = "information";
        this.cooldown = globalCooldown;
        this.aliases = new String[]{"info"};
    }

    @Override
    protected void execute(CommandEvent event) {
        new Thread(() -> {
            logCommand(event.getGuild(), event.getAuthor(), BOT_PREFIX + "about");

            EmbedBuilder eb = defaultEmbed("Skyblock Plus");
            eb.setDescription("Skyblock Plus is a Skyblock focused discord bot that has many commands to help Skyblock players and guild staff! It allows for quick retrieval of Skyblock stats plus customizable features for a better Skyblock experience.");
            eb.addField("Stats", "**Servers:** " + jda.getGuilds().size() + "\n**Members:** " + jda.getUsers().size(), true);
            eb.addField("Usage", "**Memory:** " + roundSkillAverage(100.0 *(Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory())/(Runtime.getRuntime().totalMemory())) + "%\n**CPU:** " + roundSkillAverage(100.0 * ManagementFactory.getPlatformMXBean(OperatingSystemMXBean.class).getProcessCpuLoad()) + "%", true);
            eb.addField("Links", "[**Invite Link**](https://discord.com/api/oauth2/authorize?client_id=796791167366594592&permissions=403040368&scope=bot)\n[**Discord Link**](https://discord.gg/DpcCAwMXwp)\n[**Forum Post**](https://hypixel.net/threads/discord-bot-skyblock-plus-90-servers.3980092/)", true);
            eb.setFooter("Last restart", null);
            eb.setTimestamp(event.getClient().getStartTime());
            event.reply(eb.build());
        }).start();
    }

}
