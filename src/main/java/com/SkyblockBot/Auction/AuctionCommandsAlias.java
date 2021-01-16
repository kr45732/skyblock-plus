package com.SkyblockBot.Auction;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;

public class AuctionCommandsAlias extends Command {
    private final AuctionCommands auctionCommands = new AuctionCommands();

    public AuctionCommandsAlias() {
        this.name = "ah";
        this.guildOnly = auctionCommands.isGuildOnly();
        this.cooldown = auctionCommands.getCooldown();
    }

    @Override
    protected void execute(CommandEvent event) {
        auctionCommands.execute(event);
    }
}
