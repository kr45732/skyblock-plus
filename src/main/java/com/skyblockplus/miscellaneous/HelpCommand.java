package com.skyblockplus.miscellaneous;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jdautilities.commons.waiter.EventWaiter;
import com.skyblockplus.utils.CustomPaginator;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.exceptions.PermissionException;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.skyblockplus.utils.BotUtils.*;

public class HelpCommand extends Command {
    private final EventWaiter waiter;

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
                "Auction House and Bazaar", "Miscellaneous Commands", "Verify Settings", "Apply Settings",
                "Roles Settings"};

        CustomPaginator.Builder paginateBuilder = new CustomPaginator.Builder().setColumns(1).setItemsPerPage(1)
                .showPageNumbers(true).useNumberedItems(false).setFinalAction(m -> {
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
            pageMap.put("settings", 2);
            pageMap.put("categories", 2);
            pageMap.put("setup", 2);
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
            pageMap.put("settings_verify", 9);
            pageMap.put("settings_apply", 10);
            pageMap.put("settings_roles", 11);

            if (pageMap.get(pageStr) != null) {
                startingPage = pageMap.get(pageStr);
            }
        } catch (Exception ignored) {
        }

        paginateBuilder.clearItems();

        if (event.getGuild().getMember(event.getAuthor()).hasPermission(Permission.ADMINISTRATOR)) {
            paginateBuilder.addItems("Use the arrow emojis to navigate through the pages\n• **Page 2**: General\n• **Page"
                    + " 3**: Slayer\n• **Page 4**: Skills\n• **Page 5**: Dungeons\n• **Page 6**: Guild\n• **Page 7**: "
                    + "Auction House and Bazaar\n• **Page 8**: Miscellaneous Commands\n• **Page 9**: Verify Settings\n• "
                    + "**Page 10**: Apply Settings\n• **Page 11**: Roles Settings");
        }else{
            paginateBuilder.addItems("Use the arrow emojis to navigate through the pages\n• **Page 2**: General\n• **Page"
                    + " 3**: Slayer\n• **Page 4**: Skills\n• **Page 5**: Dungeons\n• **Page 6**: Guild\n• **Page 7**: "
                    + "Auction House and Bazaar\n• **Page 8**: Miscellaneous Commands");
        }

        paginateBuilder
                .addItems(generateHelp("Show this help page", "help") + generateHelp("Show this help page", "commands")
                        + generateHelp("Get information about this bot", "about")
                        + generateHelp("Invite this bot to your server", "invite")
                        + generateHelp("Show patch notes for this bot", "version")
                        + generateHelp("Get the current settings for the bot", "settings")
                        + generateHelp("A walk-through on how to setup the bot", "setup")
                        + generateHelp("Get the id's of all categories in guild", "categories"));
        paginateBuilder.addItems(
                generateHelp("Get a user's slayer and optionally choose which skyblock profile to get the slayer of",
                        "slayer [IGN] <profile>"));

        paginateBuilder.addItems(
                generateHelp("Get skills of a player and optionally choose which skyblock profile to get the skills of",
                        "skills [IGN] <profile>"));

        paginateBuilder.addItems(generateHelp("Get catacombs level of player", "catacombs [IGN] <profile>",
                "cata [IGN] " + "<profile>")
                + generateHelp("Calculate essence cost to upgrade an item", "essence upgrade [item]")
                + generateHelp("Get essence information for each upgrade level for an item",
                "essence information [item]", "essence info [item]")
        + generateHelp("A party finder helper that shows dungeon stats of a person", "partyfinder [IGN] profile", "pf [IGN] profile")
        );

        paginateBuilder.addItems(generateHelp("Get guild experience leaderboard from IGN", "guild experience [u-IGN]",
                "guild exp [u-IGN]") + generateHelp("Get all the members in a player's guild", "guild members [u-IGN]")
                + generateHelp("Get what guild a player is in", "guild [IGN]")
                + generateHelp("Get information about a player's guild", "guild info [u-IGN]")
                + generateHelp("Get information about a guild", "guild info [g-IGN]")
//                + generateHelp("Get promote and demote leaderboard in-game commands for a player's guild",
//                "guild-rank [u-IGN]", "g-rank [u-IGN]")
        );

        paginateBuilder.addItems(
                generateHelp("Get player's active (not claimed) auctions on all profiles", "auction [IGN]", "ah [IGN]")
                        + generateHelp("Get lowest bin of an item", "bin [item]"));

        paginateBuilder.addItems(generateHelp("Claim automatic Skyblock roles", "roles claim [IGN] <profile>")
                + generateHelp("Get a player's bank and purse coins", "bank [IGN] <profile>")
                + generateHelp("Get a player's bank transaction history", "bank history [IGN] <profile>")
                + generateHelp("Get a player's wardrobe armors", "wardrobe [IGN] <profile>")
                + generateHelp("Get a player's talisman bag", "talisman [IGN] <profile>")
                + generateHelp("Get a player's inventory", "inventory [IGN] <profile>",
                "inv [IGN] <profile>")
                + generateHelp("Get an item's lore from a player's inventory. Item name should be the same as the emoji name", "inventory [item] [IGN] <profile>",
                "inv [item] [IGN] <profile>")
                + generateHelp("Get a player's equipped armor", "inventory armor [IGN] <profile>",
                "inv [IGN] <profile>")
                + generateHelp("Get a player's sacks content", "sacks [IGN] <profile>")
                + generateHelp("Get a player's weight", "weight [IGN] <profile>")
                + generateHelp("Calculate predicted weight using given stats (not 100% accurate)",
                "weight calculate [skill avg] [slayer] [cata level] [avg dungeon class level]")
                + generateHelp("Get Hypixel information about a player", "hypixel [IGN]")
                + generateHelp("Get fastest Hypixel lobby parkour for a player", "hypixel parkour [IGN]")
                + generateHelp("Get a player's minecraft uuid", "uuid [IGN]")
        );

        if (event.getGuild().getMember(event.getAuthor()).hasPermission(Permission.ADMINISTRATOR)) {
            paginateBuilder.addItems(generateHelp("Get the current verify settings for the bot", "settings verify")
                    + generateHelp("Enable or disable automatic verify", "settings verify [enable|disable]")
                    + generateHelp("Message that users will see and react to in order to verify",
                    "settings" + " verify message [message]")
                    + generateHelp(
                    "Role that user will receive " + "upon being verified. Cannot be @everyone or a managed role",
                    "settings verify role " + "[@role]")
                    + generateHelp("Channel where the message to react for verifying will sent",
                    "settings verify " + "channel [#channel]")
                    + generateHelp("Prefix that all new verify channels should start with (prefix-discordName)",
                    "settings verify " + "prefix [prefix]")
                    + generateHelp("Category where new verify channels will be made",
                    "settings verify category " + "[category id]"));

            paginateBuilder.addItems(generateHelp("Get the current apply settings for the bot", "settings apply")
                    + generateHelp("Enable or disable automatic apply", "settings apply [enable|disable]")
                    + generateHelp("Message that users will see and react to in order to apply",
                    "settings" + " apply message [message]")
                    + generateHelp("Role that will be pinged when a new application is submitted",
                    "settings apply staff_role [@role]")
                    + generateHelp("Channel where the message to react for applying will sent",
                    "settings apply " + "channel [#channel]")
                    + generateHelp("Prefix that all new apply channels should start with (prefix-discordName)",
                    "settings apply prefix [prefix]")
                    + generateHelp("Category where new apply channels will be made",
                    "settings apply category " + "[category id]")
                    + generateHelp("Channel where new applications will be sent to be reviewed by staff",
                    "settings apply staff_channel " + "[#channel]")
                    + generateHelp("Message that will be sent if applicant is accepted",
                    "settings apply accept_message [message]")
                    + generateHelp("Message that will be sent if applicant is denied",
                    "settings apply deny_message [message]"));

            paginateBuilder.addItems(generateHelp("Get the current roles settings for the bot", "settings roles")
                    + generateHelp("Enable or disable automatic roles", "settings roles [enable|disable]")
                    + generateHelp("Enable a specific automatic role (set to disable by default)",
                    "settings roles enable [roleName]")
                    + generateHelp("Add a new level to a role with its corresponding discord role",
                    "settings roles add [roleName] [value] [@role]")
                    + generateHelp("Remove a role level for a role", "settings roles remove [roleName] [value]")
                    + generateHelp("Make a specific role stackable", "settings roles stackable [roleName] [true|false]"));
        }

        paginateBuilder.build().paginate(event.getChannel(), startingPage);
    }

    private String generateHelp(String desc, String... commandName) {
        StringBuilder generatedStr = new StringBuilder("• ");
        for (int i = 0; i < commandName.length; i++) {
            generatedStr.append("`").append(BOT_PREFIX).append(commandName[i]).append("`");
            if (commandName.length > 1 && i < (commandName.length - 1)) {
                generatedStr.append(" or ");
            }
        }
        generatedStr.append(": ").append(desc).append("\n");
        return generatedStr.toString();
    }

}
