package com.skyblockplus.guilds;

import net.dv8tion.jda.api.EmbedBuilder;

public class GuildStruct {
    public final EmbedBuilder eb;
    public final String[] outputArr;

    public GuildStruct(EmbedBuilder eb, String[] outputArr) {
        this.eb = eb;
        this.outputArr = outputArr;
    }

}