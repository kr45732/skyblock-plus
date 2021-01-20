package com.SkyblockBot.Guilds;

import net.dv8tion.jda.api.EmbedBuilder;

public class GuildStruct {
    public EmbedBuilder eb;
    public String[] outputArr;

    public GuildStruct(EmbedBuilder eb, String[] outputArr) {
        this.eb = eb;
        this.outputArr = outputArr;
    }

}