package com.skyblockplus.miscellaneous;

import static com.skyblockplus.Main.waiter;
import static com.skyblockplus.utils.Utils.BOT_PREFIX;
import static com.skyblockplus.utils.Utils.defaultPaginator;
import static com.skyblockplus.utils.Utils.globalCooldown;
import static com.skyblockplus.utils.Utils.logCommand;

import java.util.HashMap;
import java.util.Map;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.skyblockplus.utils.CustomPaginator;
import com.skyblockplus.utils.structs.PaginatorExtras;

import net.dv8tion.jda.api.Permission;

public class HelpCommand extends Command {
    public HelpCommand() {
        this.name = "help";
        this.aliases = new String[] { "commands" };
        this.cooldown = globalCooldown;
    }

    @Override
    protected void execute(CommandEvent event) {
        new Thread(() -> {
            logCommand(event.getGuild(), event.getAuthor(), event.getMessage().getContentRaw());

            String[] pageTitles = new String[] { "Navigation", "General", "Slayer", "Skills", "Dungeons", "Guild",
                    "Auction House and Bazaar", "Inventory", "Miscellaneous Commands", "Skyblock Event", "Settings",
                    "Verify Settings", "Apply Settings", "Roles Settings", "Guild Role Settings" };

            CustomPaginator.Builder paginateBuilder = defaultPaginator(waiter, event.getAuthor()).setColumns(1)
                    .setItemsPerPage(1).setPaginatorExtras(new PaginatorExtras().setTitles(pageTitles));

            int startingPage = 0;
            Map<String, Integer> pageMap = new HashMap<>();
            pageMap = putMultiple(pageMap, 2, "general", "link", "unlink");
            pageMap = putMultiple(pageMap, 3, "slayer");
            pageMap = putMultiple(pageMap, 4, "skills");
            pageMap = putMultiple(pageMap, 5, "dungeons", "essence", "catacombs", "cata");
            pageMap = putMultiple(pageMap, 6, "guild");
            pageMap = putMultiple(pageMap, 7, "auction", "auctions", "ah", "bazaar", "bz", "bin", "bids", "query");
            pageMap = putMultiple(pageMap, 8, "wardrobe", "talisman", "inv", "inventory", "echest", "enderchest",
                    "sacks");
            pageMap = putMultiple(pageMap, 9, "roles", "networth", "nw", "bank", "weight", "hypixel", "profiles");
            pageMap = putMultiple(pageMap, 10, "event");
            pageMap = putMultiple(pageMap, 11, "categories", "settings", "setup");
            pageMap = putMultiple(pageMap, 12, "settings_verify");
            pageMap = putMultiple(pageMap, 13, "settings_apply");
            pageMap = putMultiple(pageMap, 14, "settings_roles");
            pageMap = putMultiple(pageMap, 15, "settings_guild");

            try {
                String pageStr = event.getMessage().getContentDisplay().toLowerCase().split(" ")[1];

                if (pageMap.containsKey(pageStr)) {
                    startingPage = pageMap.get(pageStr);
                } else {
                    try {
                        startingPage = Integer.parseInt(pageStr);
                    } catch (Exception ignored) {
                    }
                }
            } catch (Exception ignored) {
            }

            paginateBuilder.clearItems();

            boolean isAdmin = event.getGuild().getMember(event.getAuthor()).hasPermission(Permission.ADMINISTRATOR);

            if (isAdmin) {
                paginateBuilder
                        .addItems("Use the arrow emojis to navigate through the pages" + generatePageMap("General",
                                "Slayer", "Skills", "Dungeons", "Guild", "Auction House and Bazaar", "Inventory",
                                "Miscellaneous Commands", "Skyblock Event", "Settings", "Verify Settings",
                                "Apply Settings", "Roles Settings", "Guild Roles Settings"));
            } else {
                paginateBuilder.addItems("Use the arrow emojis to navigate through the pages"
                        + generatePageMap("General", "Slayer", "Skills", "Dungeons", "Guild",
                                "Auction House and Bazaar", "Inventory", "Miscellaneous Commands", "Skyblock Event"));
            }

            paginateBuilder.addItems(generateHelp("Show this help page", "help", "commands")
                    + generateHelp("Go to the help page of a specific command", "help [command name]")
                    + generateHelp("Get information about this bot", "information", "info")
                    + generateHelp("Invite this bot to your server", "invite")
                    + generateHelp("Show patch notes for this bot", "version")
                    + generateHelp("Link your discord and Hypixel account", "link [player]")
                    + generateHelp("Get what Hypixel account you are linked to", "link")
                    + generateHelp("Unlink your account", "unlink"));

            paginateBuilder.addItems(generateHelp("Get the slayer data of a player", "slayer [player] <profile>"));

            paginateBuilder.addItems(generateHelp("Get the skills data of a player", "skills [player] <profile>"));

            paginateBuilder.addItems(generateHelp("Get the dungeons data of a player", "catacombs [player] <profile>",
                    "cata [player] " + "<profile>")
                    + generateHelp("Interactive message to find the essence amount to upgrade an item",
                            "essence upgrade [item]")
                    + generateHelp("Get the amount of essence to upgrade an item for each level",
                            "essence information [item]", "essence info [item]")
                    + generateHelp("A party finder helper that shows a player's dungeon stats",
                            "partyfinder [player] <profile>", "pf [player] <profile>"));

            paginateBuilder.addItems(generateHelp("Find what guild a player is in", "guild [player]")
                    + generateHelp("Get information and statistics about a player's guild", "guild info [u-player]")
                    + generateHelp("Get information and statistics about a guild", "guild info [g-player]")
                    + generateHelp("Get a list of all members in a player's guild", "guild members [u-player]")
                    + generateHelp("Get the experience leaderboard for a player's guild", "guild experience [u-player]",
                            "guild exp [u-player]"));

            paginateBuilder.addItems(generateHelp("Get player's active (not claimed) auctions on all profiles",
                    "auction [player]", "ah [player]") + generateHelp("Get the lowest bin of an item", "bin [item]")
                    + generateHelp("Get bazaar prices of an item", "bazaar [item]", "bz [item]")
                    + generateHelp("Get the average auction price of an item", "average [item]", "avg [item]")
                    + generateHelp("Get a player's bids", "bids [player]")
                    + generateHelp("Query the auction house", "query [item]"));

            paginateBuilder.addItems(generateHelp("Get a player's inventory represented in emojis",
                    "inventory [player] <profile>", "inv [player] <profile>")
                    + generateHelp("Get a player's inventory with lore", "inventory [player] <profile> [slot-number]",
                            "inv [player] <profile> [slot-number]")
                    + generateHelp("Get a player's equipped armor with lore", "inventory armor [player] <profile>",
                            "inv armor [player] <profile>")
                    + generateHelp("Get a player's ender chest represented in emojis", "enderchest [player] <profile>",
                            "echest [player] <profile>")
                    + generateHelp("Get a player's talisman bag represented in emojis", "talisman [player] <profile>")
                    + generateHelp("Get a player's talisman bag with lore", "talisman [player] <profile> [slot-number]")
                    + generateHelp("Get a player's sacks' content bag represented in a list",
                            "sacks [player] <profile>")
                    + generateHelp("Get a player's wardrobe armors represented in emojis",
                            "wardrobe [player] <profile>")
                    + generateHelp("Get a player's wardrobe armors represented in a list",
                            "wardrobe list [player] <profile>"));

            paginateBuilder.addItems(
                    generateHelp("Claim automatic Skyblock roles. The player must be linked", "roles claim <profile>")
                            + generateHelp("Get a player's bank and purse coins", "bank [player] <profile>")
                            + generateHelp("Get a player's bank transaction history", "bank history [player] <profile>")
                            + generateHelp("Get a player's networth (in development)", "networth [player] <profile>",
                                    "nw [player] <profile>")
                            + generateHelp("Get a player's weight", "weight [player] <profile>")
                            + generateHelp("Calculate predicted weight using given stats (not 100% accurate)",
                                    "weight calculate [skill avg] [slayer] [cata level] [avg dungeon class level]")
                            + generateHelp("Get Hypixel information about a player", "hypixel [player]")
                            + generateHelp("Get fastest Hypixel lobby parkour for a player", "hypixel parkour [player]")
                            + generateHelp("Get a information about all of a player's profiles", "profiles [player]"));

            paginateBuilder.addItems(
                    (isAdmin ? generateHelp("Interactive message to create a Skyblock event", "event create") : "")
                            + generateHelp("Get information about the current event", "event current")
                            + generateHelp("Join the current event", "event join")
                            + generateHelp("Leave the current event", "event leave")
                            + generateHelp("Get the leaderboard for current event", "event leaderboard", "event lb")
                            + (isAdmin ? generateHelp("Force end the event", "event end") : ""));

            if (isAdmin) {
                paginateBuilder.addItems(generateHelp("Get the current settings for the bot", "settings")
                        + generateHelp("A walk-through on how to setup the bot", "setup")
                        + generateHelp("Get the id's of all categories in guild", "categories"));

                paginateBuilder.addItems(generateHelp("Get the current verify settings for the bot", "settings verify")
                        + generateHelp("Enable or disable automatic verify", "settings verify [enable|disable]")
                        + generateHelp("Message that users will see and react to in order to verify",
                                "settings" + " verify message [message]")
                        + generateHelp(
                                "Role that user will receive "
                                        + "upon being verified. Cannot be @everyone or a managed role",
                                "settings verify role " + "[@role]")
                        + generateHelp("Channel where the message to react for verifying will sent",
                                "settings verify " + "channel [#channel]")
                        + generateHelp("The nickname template on verifying. Can be set to none",
                                "settings verify nickname <prefix> [IGN] <postfix>"));

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
                        + generateHelp("Message that will be sent if applicant is waitlisted. Can be set to none",
                                "settings apply waitlist_message [message]")
                        + generateHelp("Message that will be sent if applicant is denied",
                                "settings apply deny_message [message]")
                        + generateHelp(
                                "Add a requirement that applicant must meet. At least one of the requirement types must be set",
                                "settings apply reqs add <slayer-amount> <skills-amount> <catacombs-amount> <weight-amount>")
                        + generateHelp(
                                "Remove a requirement. Run `" + BOT_PREFIX
                                        + "settings apply` to see the index for all current requirements",
                                "settings apply reqs remove [number]"));

                paginateBuilder.addItems(generateHelp("Get the current roles settings for the bot", "settings roles")
                        + generateHelp("Enable or disable automatic roles", "settings roles [enable|disable]")
                        + generateHelp("Enable a specific automatic role (set to disable by default)",
                                "settings roles enable [roleName]")
                        + generateHelp("Add a new level to a role with its corresponding discord role",
                                "settings roles add [roleName] [value] [@role]")
                        + generateHelp("Remove a role level for a role", "settings roles remove [roleName] [value]")
                        + generateHelp("Make a specific role stackable",
                                "settings roles stackable [roleName] [true|false]")
                        + generateHelp("Set a one level role's role", "settings roles set [roleName] [@role]"));

                paginateBuilder.addItems(generateHelp("Enable or disable automatic guild role assigning",
                        "settings guild [enable|disable] role")
                        + generateHelp("Set the guild name", "settings guild set [guild_name]")
                        + generateHelp("Set the role to give guild member's", "settings guild role [@role]")
                        + generateHelp("Enable or disable automatic guild rank assigning",
                                "settings guild [enable|disable] rank")
                        + generateHelp("Add an automatic guild rank", "settings guild add [rank_name] [@role]")
                        + generateHelp("Remove an automatic guild rank", "settings guild remove [rank_name]"));
            }

            paginateBuilder.build().paginate(event.getChannel(), startingPage);
        }).start();
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

    private String generatePageMap(String... pageNames) {
        StringBuilder generatedStr = new StringBuilder();
        for (int i = 0; i < pageNames.length; i++) {
            generatedStr.append("\n• **Page ").append(i + 2).append(":** ").append(pageNames[i]);
        }
        return generatedStr.toString();
    }

    private Map<String, Integer> putMultiple(Map<String, Integer> map, int value, String... keys) {
        for (String key : keys) {
            map.put(key, value);
        }

        return map;
    }

}
