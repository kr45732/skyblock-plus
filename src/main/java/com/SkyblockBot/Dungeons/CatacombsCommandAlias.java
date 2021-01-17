package com.SkyblockBot.Dungeons;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;

public class CatacombsCommandAlias extends Command {
    private final CatacombsCommand auctionCommands = new CatacombsCommand();

    public CatacombsCommandAlias() {
        this.name = "cata";
        this.guildOnly = auctionCommands.isGuildOnly();
        this.cooldown = auctionCommands.getCooldown();
    }

    @Override
    protected void execute(CommandEvent event) {
        auctionCommands.execute(event);
    }
}
