package com.SkyblockBot;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jdautilities.commons.waiter.EventWaiter;
import com.jagrosh.jdautilities.menu.Paginator;
import net.dv8tion.jda.api.exceptions.PermissionException;

import java.util.concurrent.TimeUnit;

import static com.SkyblockBot.BotUtils.*;

public class Help extends Command {
    private final Paginator.Builder pbuilder;
    private final int itemsPerPage = 9;

    public Help(EventWaiter waiter) {
        this.name = "help";
        this.aliases = new String[] { "commands" };
        this.help = "display help menu";
        this.guildOnly = false;

        pbuilder = new Paginator.Builder().setColumns(1).setItemsPerPage(itemsPerPage).showPageNumbers(true)
                .waitOnSinglePage(false).useNumberedItems(false).setFinalAction(m -> {
                    try {
                        m.clearReactions().queue();
                    } catch (PermissionException ex) {
                        m.delete().queue();
                    }
                }).setEventWaiter(waiter).setTimeout(30, TimeUnit.SECONDS).wrapPageEnds(true).setColor(botColor);
    }

    @Override
    protected void execute(CommandEvent event) {
        String inProgressCommands = "!slayer leaderboard [u-IGN]\n" + "!slayer lb [u-IGN]\n"
                + "!slayer leaderboard [g-guild name]\n" + "!slayer lb [g-guild name]" +

                "!skill player [IGN] <profile>\n" + "!skill leaderboard [u-IGN]\n" + "!skill lb [u-IGN]\n"
                + "!skill leaderboard [g-guild name]\n" + "!skill lb [g-guild name]\n" +

                "!guild info [g-guild name]\n" + "!guild members [g-guild name]\n" + "!guild exp [u-IGN]\n"
                + "!guild experience [g-guild name]\n " + "!guild exp [g-guild name]\n" +

                "!bin [item] <lowest/highest>\n" +

                "!bazaar [item]\n" + "!bz [item]\n";

        pbuilder.clearItems();
        pbuilder.setText("Help Page");
        pbuilder.addItems(fillArray(
                new String[] { "**__Navigation__**", " ", "Use the arrow emojis to navigate through the pages",
                        "• **Page 2**: General", "• **Page 3**: Slayer", "• **Page 4**: Skills",
                        "• **Page 5**: Dungeons", "• **Page 6**: Guild", "• **Page 7**: Auction House and Bazaar" }));

        String[] generalCommands = new String[] { "**__General__**", " ", "• `!help`: Show this help page",
                "• `!commands`: Show this help page", "• `!about`: Get information about this bot" };
        pbuilder.addItems(fillArray(generalCommands));

        String[] slayerCommands = new String[] { "**__Slayer__**", " ",
                "• `!slayer player [IGN] <profile>`: Get a user's slayer and optionally choose which skyblock profile to get the slayer of" };
        pbuilder.addItems(fillArray(slayerCommands));

        String[] skillsCommands = new String[] { "**__Skills__**", " ", "• WIP" };
        pbuilder.addItems(fillArray(skillsCommands));

        String[] dungeonCommands = new String[] { "**__Dungeons__**", " ", "• WIP" };
        pbuilder.addItems(fillArray(dungeonCommands));

        String[] guildCommands = new String[] { "**__Guild__**", " ",
                "• `!guild experience [u-IGN]` or `!guild exp [u-IGN]`: Get guild experience leaderboard from IGN",
                "• `!guild members [u-IGN]`: Get all the members in a player's guild",
                "• `!guild player [IGN]`: Get what guild a player is in",
                "• `!guild info [u-IGN]`: Get information about a player's guild" };
        pbuilder.addItems(fillArray(guildCommands));

        String[] ahAndBazCommands = new String[] { "**__Auction House and Bazaar__**", " ",
                "• `!auction [IGN]` or `!ah [IGN]`: Get player's active (not claimed) auctions on all profiles" };
        pbuilder.addItems(fillArray(ahAndBazCommands));

        pbuilder.build().paginate(event.getChannel(), 0);
    }

    public String[] fillArray(String[] inputs) {
        String[] filedArray = new String[itemsPerPage];
        for (int i = 0; i < filedArray.length; i++) {
            try {
                filedArray[i] = inputs[i];
            } catch (IndexOutOfBoundsException e) {
                filedArray[i] = "";
            }
        }
        return filedArray;
    }

}
