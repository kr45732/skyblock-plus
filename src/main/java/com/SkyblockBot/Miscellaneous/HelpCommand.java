package com.SkyblockBot.Miscellaneous;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jdautilities.commons.waiter.EventWaiter;
import com.jagrosh.jdautilities.menu.Paginator;
import net.dv8tion.jda.api.exceptions.PermissionException;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.SkyblockBot.Miscellaneous.BotUtils.botColor;
import static com.SkyblockBot.Miscellaneous.BotUtils.botPrefix;

public class HelpCommand extends Command {
    private final Paginator.Builder paginateBuilder;
    private final int itemsPerPage = 9;

    public HelpCommand(EventWaiter waiter) {
        this.name = "help";
        this.aliases = new String[]{"commands"};
        this.guildOnly = false;

        paginateBuilder = new Paginator.Builder().setColumns(1).setItemsPerPage(itemsPerPage).showPageNumbers(true)
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
        System.out.println(botPrefix + "help");
        // "!slayer leaderboard [u-IGN]\n"
        // "!slayer lb [u-IGN]\n"
        // "!slayer leaderboard [g-guild name]\n"
        // "!slayer lb [g-guild name]"

        // "!skill player [IGN] <profile>\n"
        // "!skill leaderboard [u-IGN]\n"
        // "!skill lb [u-IGN]\n"
        // "!skill leaderboard [g-guild name]\n"
        // "!skill lb [g-guild name]\n"

        // "!guild info [g-guild name]\n"
        // "!guild members [g-guild name]\n"
        // "!guild experience [g-guild name]\n "
        // "!guild exp [g-guild name]\n"

        // "!bazaar [item]\n"
        // "!bz [item]\n";

        // !player [ign]

        int startingPage = 0;
        try {
            String pageStr = event.getMessage().getContentDisplay().toLowerCase().split(" ")[1];
            Map<String, Integer> pageMap = new HashMap<>();
            pageMap.put("general", 2);
            pageMap.put("roles", 2);
            pageMap.put("slayer", 3);
            pageMap.put("skills", 4);
            pageMap.put("dungeons", 5);
            pageMap.put("catacombs", 5);
            pageMap.put("cata", 5);
            pageMap.put("guild", 6);
            pageMap.put("guild-rank", 6);
            pageMap.put("g-rank", 6);
            pageMap.put("auction", 7);
            pageMap.put("ah", 7);
            pageMap.put("bazaar", 7);
            pageMap.put("bz", 7);
            pageMap.put("bin", 7);
            if (pageMap.get(pageStr) != null) {
                startingPage = pageMap.get(pageStr);
            }
        } catch (Exception ignored) {
        }

        paginateBuilder.clearItems();
        paginateBuilder.setText("Help Page");
        paginateBuilder.addItems(fillArray(
                new String[]{"**__Navigation__**", " ", "Use the arrow emojis to navigate through the pages",
                        "• **Page 2**: General", "• **Page 3**: Slayer", "• **Page 4**: Skills",
                        "• **Page 5**: Dungeons", "• **Page 6**: Guild", "• **Page 7**: Auction House and Bazaar"}));

        String[] generalCommands = new String[]{"**__General__**", " ", generateHelp("Show this help page", "help"),
                generateHelp("Show this help page", "commands"),
                generateHelp("Get information about this bot", "about"),
                generateHelp("Show patch notes for this bot", "version"),
                generateHelp("Shutdown bot; can only be used by specific people", "shutdown"),
                generateHelp("Claim automatic Skyblock roles", "roles claim [IGN] <profile>"),};
        paginateBuilder.addItems(fillArray(generalCommands));

        String[] slayerCommands = new String[]{"**__Slayer__**", " ",
                generateHelp("Get a user's slayer and optionally choose which skyblock profile to get the slayer of",
                        "slayer player [IGN] <profile>")};
        paginateBuilder.addItems(fillArray(slayerCommands));

        String[] skillsCommands = new String[]{"**__Skills__**", " ",
                generateHelp("Get skills of a player", "skill player [IGN]")};
        paginateBuilder.addItems(fillArray(skillsCommands));

        String[] dungeonCommands = new String[]{"**__Dungeons__**", " ",
                generateHelp("Get catacombs level of player", "catacombs player [IGN]", "cata player [IGN]")};
        paginateBuilder.addItems(fillArray(dungeonCommands));

        String[] guildCommands = new String[]{"**__Guild__**", " ",
                generateHelp("Get guild experience leaderboard from IGN", "guild experience [u-IGN]",
                        "guild exp [u-IGN]"),
                generateHelp("Get all the members in a player's guild", "guild members [u-IGN]"),
                generateHelp("Get what guild a player is in", "guild player [IGN]"),
                generateHelp("Get information about a player's guild", "guild info [u-IGN]"),
                generateHelp("Get promote and demote leaderboard in-game commands for a player's guild", "guild-rank [u-IGN]", "g-rank [u-IGN]")};
        paginateBuilder.addItems(fillArray(guildCommands));

        String[] ahAndBazCommands = new String[]{"**__Auction House and Bazaar__**", " ",
                generateHelp("Get player's active (not claimed) auctions on all profiles", "auction [IGN]", "ah [IGN]"),
                generateHelp("Get lowest bin of an item", "bin [item]")};
        paginateBuilder.addItems(fillArray(ahAndBazCommands));

        paginateBuilder.build().paginate(event.getChannel(), startingPage);
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

    public String generateHelp(String desc, String... commandName) {
        StringBuilder generatedStr = new StringBuilder("• ");
        for (int i = 0; i < commandName.length; i++) {
            generatedStr.append("`").append(botPrefix).append(commandName[i]).append("`");
            if (commandName.length > 1 && i < (commandName.length - 1)) {
                generatedStr.append(" or ");
            }
        }
        generatedStr.append(": ").append(desc);
        return generatedStr.toString();
    }

}
