package com.SkyblockBot.Miscellaneous;

import static com.SkyblockBot.Miscellaneous.BotUtils.botPrefix;
import static com.SkyblockBot.Miscellaneous.BotUtils.defaultEmbed;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;

import net.dv8tion.jda.api.EmbedBuilder;

public class AboutCommand extends Command {
    public AboutCommand() {
        this.name = "about";
        this.guildOnly = false;
    }

    @Override
    protected void execute(CommandEvent event) {
        System.out.println(botPrefix + "about");
        String descr = "Hello! I am **Skyblock Plus**, an an all purpose skyblock bot.\n"
                + "I was written in java by CrypticPlasma.\n" + "Type `" + botPrefix + "help` or `" + botPrefix
                + "commands` to see my commands!"
                + "\nFor more information and to invite me join my server [`here`](https://discord.gg/dFwwqsVHHD)!\n\n"
                + "Some of my features include:\n"
                + "<:green_check_custom:799774962394988574> Slayers, Skills, and Dungeons\n"
                + "<:green_check_custom:799774962394988574> Guild Information\n"
                + "<:green_check_custom:799774962394988574> Auction House and Bazaar\n"
                + "<:green_check_custom:799774962394988574> Automatic skyblock applications for a guild\n";

        EmbedBuilder eb = defaultEmbed("About Skyblock Plus!", null);
        eb.setDescription(descr);
        eb.setFooter("Last restart", null);
        eb.setTimestamp(event.getClient().getStartTime());
        event.reply(eb.build());
    }

}
