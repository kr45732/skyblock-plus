package com.SkyblockBot.Miscellaneous;

import static com.SkyblockBot.Miscellaneous.BotUtils.defaultEmbed;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.ApplicationInfo;

public class AboutCommand extends Command {
    private final Permission[] perms;
    private String oauthLink;

    public AboutCommand(Permission... perms) {
        this.name = "about";
        this.help = "shows info about the bot";
        this.guildOnly = false;
        this.perms = perms;
        this.botPermissions = new Permission[] { Permission.MESSAGE_EMBED_LINKS };
    }

    @Override
    protected void execute(CommandEvent event) {
        if (oauthLink == null) {
            try {
                ApplicationInfo info = event.getJDA().retrieveApplicationInfo().complete();
                oauthLink = info.isBotPublic() ? info.getInviteUrl(0L, perms) : "";
            } catch (Exception e) {
                oauthLink = "";
            }
        }
        boolean join = !(event.getClient().getServerInvite() == null || event.getClient().getServerInvite().isEmpty());
        boolean inv = !oauthLink.isEmpty();
        String invline = "\n"
                + (join ? "Join my server [`here`](" + event.getClient().getServerInvite() + ")"
                        : (inv ? "Please " : ""))
                + (inv ? (join ? ", or " : "") + "[`invite`](" + oauthLink + ") me to your server" : "") + "!";
        String descr = "Hello! I am **Skyblock Multipurpose**, an an all purpose skyblock bot.\n"
                + "I was written in java by CrypticPlasma.\n" + "Type `!help` or `!commands` to see my commands!"
                + invline + "\n\n" + "Some of my features include:\n"
                + "<:green_check_custom:799774962394988574> Slayers, Skills, and Dungeons\n"
                + "<:green_check_custom:799774962394988574> Guild Information\n"
                + "<:green_check_custom:799774962394988574> Auction House and Bazaar\n";

        EmbedBuilder eb = defaultEmbed("All about Skyblock Multipurpose!", null);
        eb.setDescription(descr);
        eb.setFooter("Last restart", null);
        eb.setTimestamp(event.getClient().getStartTime());
        event.reply(eb.build());
    }

}
