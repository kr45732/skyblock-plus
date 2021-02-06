package com.skyblockplus.miscellaneous;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import net.dv8tion.jda.api.EmbedBuilder;

import static com.skyblockplus.utils.BotUtils.BOT_PREFIX;
import static com.skyblockplus.utils.BotUtils.defaultEmbed;

public class AboutCommand extends Command {
    public AboutCommand() {
        this.name = "about";
        this.guildOnly = false;
    }

    @Override
    protected void execute(CommandEvent event) {
        System.out.println(BOT_PREFIX + "about");
        String description = "Hello! I am **Skyblock Plus**, an an all purpose skyblock bot.\n"
                + "I was written in java by CrypticPlasma.\n" + "Type `" + BOT_PREFIX + "help` or `" + BOT_PREFIX
                + "commands` to see my commands!"
                + "\nFor more information and to invite me join my server [`here`](https://discord.gg/dFwwqsVHHD)!\n\n"
                + "Some of my features include:\n"
                + "<:green_check_custom:799774962394988574> Slayers, Skills, and Dungeons\n"
                + "<:green_check_custom:799774962394988574> Guild Information\n"
                + "<:green_check_custom:799774962394988574> Auction House and Bazaar\n"
                + "<:green_check_custom:799774962394988574> Automatic Skyblock Applications For a Guild\n"
                + "<:green_check_custom:799774962394988574> Automatic Skyblock Progress Roles\n";

        EmbedBuilder eb = defaultEmbed("About Skyblock Plus!", null);
        eb.setDescription(description);
        eb.setFooter("Last restart", null);
        eb.setTimestamp(event.getClient().getStartTime());
        event.reply(eb.build());
    }

}
