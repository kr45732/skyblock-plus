package com.skyblockplus.miscellaneous;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jdautilities.commons.waiter.EventWaiter;
import com.skyblockplus.utils.CustomPaginator;
import net.dv8tion.jda.api.exceptions.PermissionException;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.skyblockplus.utils.BotUtils.*;

public class HelpCommand extends Command {
    private final EventWaiter waiter;
    private final int itemsPerPage = 12;

    public HelpCommand(EventWaiter waiter) {
        this.name = "help";
        this.aliases = new String[]{"commands"};
        this.guildOnly = false;
        this.waiter = waiter;
        this.cooldown = globalCooldown;

    }

    @Override
    protected void execute(CommandEvent event) {
        System.out.println(event.getMessage().getContentRaw());

        String[] pageTitles = new String[]{"Navigation", "General", "Slayer", "Skills", "Dungeons", "Guild",
                "Auction House and Bazaar", "Miscellaneous Commands"};

        CustomPaginator.Builder paginateBuilder = new CustomPaginator.Builder().setColumns(1)
                .setItemsPerPage(itemsPerPage).showPageNumbers(true).useNumberedItems(false).setFinalAction(m -> {
                    try {
                        m.clearReactions().queue();
                    } catch (PermissionException ex) {
                        m.delete().queue();
                    }
                }).setEventWaiter(waiter).setTimeout(30, TimeUnit.SECONDS).wrapPageEnds(true).setColor(botColor)
                .setPageTitles(pageTitles).setCommandUser(event.getAuthor());

        int startingPage = 0;
        try {
            String pageStr = event.getMessage().getContentDisplay().toLowerCase().split(" ")[1];
            Map<String, Integer> pageMap = new HashMap<>();
            pageMap.put("general", 2);
            pageMap.put("slayer", 3);
            pageMap.put("skills", 4);
            pageMap.put("dungeons", 5);
            pageMap.put("essence", 5);
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
            pageMap.put("roles", 8);
            pageMap.put("bank", 8);
            pageMap.put("wardrobe", 8);
            pageMap.put("talisman", 8);
            pageMap.put("inventory", 8);
            pageMap.put("inv", 8);
            pageMap.put("sacks", 8);
            pageMap.put("weight", 8);
            pageMap.put("hypixel", 8);
            pageMap.put("uuid", 8);

            if (pageMap.get(pageStr) != null) {
                startingPage = pageMap.get(pageStr);
            }
        } catch (Exception ignored) {
        }

        paginateBuilder.clearItems();
        paginateBuilder.addItems(
                fillArray(new String[]{"Use the arrow emojis to navigate through the pages", "• **Page 2**: General",
                        "• **Page 3**: Slayer", "• **Page 4**: Skills", "• **Page 5**: Dungeons", "• **Page 6**: Guild",
                        "• **Page 7**: Auction House and Bazaar", "• **Page 8**: Miscellaneous Commands"}));

        String[] generalCommands = new String[]{generateHelp("Show this help page", "help"),
                generateHelp("Show this help page", "commands"),
                generateHelp("Get information about this bot", "about"),
                generateHelp("Invite this bot to your server", "invite"),
                generateHelp("Show patch notes for this bot", "version"),
                generateHelp("Shutdown bot; can only be used by specific people", "shutdown")};
        paginateBuilder.addItems(fillArray(generalCommands));

        String[] slayerCommands = new String[]{
                generateHelp("Get a user's slayer and optionally choose which skyblock profile to get the slayer of",
                        "slayer player [IGN] <profile>")};
        paginateBuilder.addItems(fillArray(slayerCommands));

        String[] skillsCommands = new String[]{
                generateHelp("Get skills of a player and optionally choose which skyblock profile to get the skills of",
                        "skills player [IGN] <profile>")};
        paginateBuilder.addItems(fillArray(skillsCommands));

        String[] dungeonCommands = new String[]{
                generateHelp("Get catacombs level of player", "catacombs player [IGN]", "cata player [IGN]"),
                generateHelp("Calculate essence cost to upgrade an item", "essence upgrade [item]"),
                generateHelp("Get essence information for each upgrade level for an item", "essence information [item]",
                        "essence info [item]")};
        paginateBuilder.addItems(fillArray(dungeonCommands));

        String[] guildCommands = new String[]{
                generateHelp("Get guild experience leaderboard from IGN", "guild experience [u-IGN]",
                        "guild exp [u-IGN]"),
                generateHelp("Get all the members in a player's guild", "guild members [u-IGN]"),
                generateHelp("Get what guild a player is in", "guild player [IGN]"),
                generateHelp("Get information about a player's guild", "guild info [u-IGN]"),
                generateHelp("Get information about a guild", "guild info [g-IGN]"),
                generateHelp("Get promote and demote leaderboard in-game commands for a player's guild",
                        "guild-rank [u-IGN]", "g-rank [u-IGN]")};
        paginateBuilder.addItems(fillArray(guildCommands));

        String[] ahAndBazCommands = new String[]{
                generateHelp("Get player's active (not claimed) auctions on all profiles", "auction [IGN]", "ah [IGN]"),
                generateHelp("Get lowest bin of an item", "bin [item]")};
        paginateBuilder.addItems(fillArray(ahAndBazCommands));

        String[] miscCommands = new String[]{
                generateHelp("Claim automatic Skyblock roles", "roles claim [IGN] <profile>"),
                generateHelp("Get a player's bank and purse coins", "bank player [IGN] <profile>"),
                generateHelp("Get a player's bank transaction history", "bank history [IGN] <profile>"),
                generateHelp("Get a player's wardrobe armors", "wardrobe player [IGN] <profile>"),
                generateHelp("Get a player's talisman bag", "talisman player [IGN] <profile>"),
                generateHelp("Get a player's equipped armor", "inventory player [IGN] <profile>",
                        "inv player [IGN] <profile>"),
                generateHelp("Get a player's sacks content", "sacks player [IGN] <profile>"),
                generateHelp("Get a player's weight", "weight player [IGN] <profile>"),
                generateHelp("Calculate predicted weight using given stats (not 100% accurate)",
                        "weight calculate [skill avg] [slayer] [cata level] [avg dungeon class level]"),
                generateHelp("Get Hypixel information about a player", "hypixel player [IGN]"),
                generateHelp("Get fastest Hypixel lobby parkour for a player", "hypixel parkour [IGN]"),
                generateHelp("Get a player's minecraft uuid", "uuid player [IGN]")};
        paginateBuilder.addItems(fillArray(miscCommands));

        paginateBuilder.build().paginate(event.getChannel(), startingPage);
    }

    private String[] fillArray(String[] inputs) {
        String[] filledArray = new String[itemsPerPage];
        for (int i = 0; i < filledArray.length; i++) {
            try {
                filledArray[i] = inputs[i];
            } catch (IndexOutOfBoundsException e) {
                filledArray[i] = "";
            }
        }
        return filledArray;
    }

    private String generateHelp(String desc, String... commandName) {
        StringBuilder generatedStr = new StringBuilder("• ");
        for (int i = 0; i < commandName.length; i++) {
            generatedStr.append("`").append(BOT_PREFIX).append(commandName[i]).append("`");
            if (commandName.length > 1 && i < (commandName.length - 1)) {
                generatedStr.append(" or ");
            }
        }
        generatedStr.append(": ").append(desc);
        return generatedStr.toString();
    }

}
