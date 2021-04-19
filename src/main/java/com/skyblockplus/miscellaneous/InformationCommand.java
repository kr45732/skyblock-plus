package com.skyblockplus.miscellaneous;

import static com.skyblockplus.Main.jda;
import static com.skyblockplus.utils.Utils.BOT_PREFIX;
import static com.skyblockplus.utils.Utils.defaultEmbed;
import static com.skyblockplus.utils.Utils.globalCooldown;
import static com.skyblockplus.utils.Utils.logCommand;
import static com.skyblockplus.utils.Utils.roundAndFormat;

import java.time.temporal.ChronoUnit;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;

// import com.sun.management.OperatingSystemMXBean;
import net.dv8tion.jda.api.EmbedBuilder;

public class InformationCommand extends Command {
    public InformationCommand() {
        this.name = "information";
        this.cooldown = globalCooldown;
        this.aliases = new String[] { "info" };
    }

    @Override
    protected void execute(CommandEvent event) {
        new Thread(() -> {
            logCommand(event.getGuild(), event.getAuthor(), BOT_PREFIX + "information");

            EmbedBuilder eb = defaultEmbed("Skyblock Plus");

            final long[] ping = { -1 };
            event.reply("Loading...", m -> {
                ping[0] = event.getMessage().getTimeCreated().until(m.getTimeCreated(), ChronoUnit.MILLIS);
                m.delete().queue();
                eb.setDescription(
                        "Skyblock Plus is a Skyblock focused discord bot that has many commands to help Skyblock players and guild staff! It allows for quick retrieval of Skyblock stats plus customizable features for a better Skyblock experience.");
                eb.addField("Stats",
                        "**Servers:** " + jda.getGuilds().size() + "\n**Members:** " + jda.getUsers().size()
                                + "\n**Ping:** " + ping[0] + "ms\n**Websocket:** " + event.getJDA().getGatewayPing()
                                + "ms",
                        true);
                eb.addField("Usage",
                        "**Memory:** " + roundAndFormat(
                                100.0 * (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory())
                                        / (Runtime.getRuntime().totalMemory()))
                                + "%",
                        false);

                eb.addField("Links",
                        "[**Invite Link**](https://discord.com/api/oauth2/authorize?client_id=796791167366594592&permissions=403040368&scope=bot)\n[**Discord Link**](https://discord.gg/DpcCAwMXwp)\n[**Forum Post**](https://hypixel.net/threads/discord-bot-skyblock-plus-90-servers.3980092/)",
                        true);
                eb.setFooter("Last restart", null);
                eb.setTimestamp(event.getClient().getStartTime());
                event.reply(eb.build());
            });
        }).start();
    }

}
