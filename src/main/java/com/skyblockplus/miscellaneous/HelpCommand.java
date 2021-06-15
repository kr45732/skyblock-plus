package com.skyblockplus.miscellaneous;

import static com.skyblockplus.Main.waiter;
import static com.skyblockplus.utils.Utils.*;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.skyblockplus.utils.CustomPaginator;
import com.skyblockplus.utils.structs.PaginatorExtras;
import java.util.HashMap;
import java.util.Map;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.interactions.InteractionHook;

public class HelpCommand extends Command {

	public HelpCommand() {
		this.name = "help";
		this.aliases = new String[] { "commands" };
		this.cooldown = globalCooldown;
	}

	public static void getHelp(String pageStr, Member member, MessageChannel channel, InteractionHook hook) {
		String[] pageTitles = new String[] {
			"Navigation",
			"General",
			"Slayer",
			"Skills",
			"Dungeons",
			"Guild",
			"Price Commands",
			"Inventory",
			"Miscellaneous Commands",
			"Skyblock Event",
			"Settings",
			"Verify Settings",
			"Apply Settings",
			"Roles Settings",
			"Guild Role Settings",
		};

		CustomPaginator.Builder paginateBuilder = defaultPaginator(waiter, member.getUser())
			.setColumns(1)
			.setItemsPerPage(1)
			.setPaginatorExtras(new PaginatorExtras().setTitles(pageTitles));

		int startingPage = 0;
		Map<String, Integer> pageMap = new HashMap<>();
		putMultiple(pageMap, 2, "general", "link", "unlink");
		putMultiple(pageMap, 3, "slayer");
		putMultiple(pageMap, 4, "skills");
		putMultiple(pageMap, 5, "dungeons", "essence", "catacombs", "cata");
		putMultiple(pageMap, 6, "g", "guild", "guild-leaderboard", "g-lb", "guild-kicker", "g-kicker");
		putMultiple(pageMap, 7, "auction", "auctions", "ah", "bazaar", "bz", "bin", "bids", "query", "bits", "bit");
		putMultiple(pageMap, 8, "wardrobe", "talisman", "inv", "inventory", "echest", "enderchest", "sacks");
		putMultiple(pageMap, 9, "roles", "networth", "nw", "bank", "weight", "hypixel", "profiles");
		putMultiple(pageMap, 10, "event");
		putMultiple(pageMap, 11, "categories", "settings", "setup");
		putMultiple(pageMap, 12, "settings_verify");
		putMultiple(pageMap, 13, "settings_apply");
		putMultiple(pageMap, 14, "settings_roles");
		putMultiple(pageMap, 15, "settings_guild");

		try {
			if (pageMap.containsKey(pageStr)) {
				startingPage = pageMap.get(pageStr);
			} else {
				try {
					startingPage = Integer.parseInt(pageStr);
				} catch (Exception ignored) {}
			}
		} catch (Exception ignored) {}

		paginateBuilder.clearItems();

		boolean isAdmin = member.hasPermission(Permission.ADMINISTRATOR);

		if (isAdmin) {
			paginateBuilder.addItems(
				"Use the arrow emojis to navigate through the pages" +
				generatePageMap(
					"General",
					"Slayer",
					"Skills",
					"Dungeons",
					"Guild",
					"Price Commands",
					"Inventory",
					"Miscellaneous Commands",
					"Skyblock Event",
					"Settings",
					"Verify Settings",
					"Apply Settings",
					"Roles Settings",
					"Guild Roles Settings"
				)
			);
		} else {
			paginateBuilder.addItems(
				"Use the arrow emojis to navigate through the pages" +
				generatePageMap(
					"General",
					"Slayer",
					"Skills",
					"Dungeons",
					"Guild",
					"Price Commands",
					"Inventory",
					"Miscellaneous Commands",
					"Skyblock Event"
				)
			);
		}

		paginateBuilder.addItems(
			generateHelp("Show this help page", "help", "commands") +
			generateHelp("Go to the help page of a specific command", "help [command name]") +
			generateHelp("Get information about this bot", "information", "info") +
			generateHelp("Invite this bot to your server", "invite") +
			generateHelp("Link your discord and Hypixel account", "link [player]") +
			generateHelp("Get what Hypixel account you are linked to", "link") +
			generateHelp("Unlink your account", "unlink")
		);

		paginateBuilder.addItems(generateHelp("Get the slayer data of a player", "slayer [player] <profile>"));

		paginateBuilder.addItems(generateHelp("Get the skills data of a player", "skills [player] <profile>"));

		paginateBuilder.addItems(
			generateHelp("Get the dungeons data of a player", "catacombs [player] <profile>", "cata [player] " + "<profile>") +
			generateHelp("Interactive message to find the essence amount to upgrade an item", "essence upgrade [item]") +
			generateHelp("Get the amount of essence to upgrade an item for each level", "essence info [item]") +
			generateHelp(
				"A party finder helper that shows a player's dungeon stats",
				"partyfinder [player] <profile>",
				"pf [player] <profile>"
			)
		);

		paginateBuilder.addItems(
			generateHelp("Find what guild a player is in", "guild [player]") +
			generateHelp("Get information and statistics about a player's guild", "guild info [u:player]") +
			generateHelp("Get information and statistics about a guild", "guild info [g:guild name]") +
			generateHelp("Get a list of all members in a player's guild", "guild members [u:player]") +
			generateHelp("Get the experience leaderboard for a player's guild", "guild experience [u:player]", "guild exp [u:player]") +
			generateHelp("Get the application requirements set for this server", "guild-requirements [name]", "g-reqs [name]") +
			generateHelp(
				"Get the weight, skills, catacombs, or slayer leaderboard for a player's guild",
				"g-lb [weight|skills|catacombs|slayer] [u:player]"
			) +
			generateHelp(
				"Get all player's who don't meet the provided requirements. The requirement name can be skills, slayer, catacombs, or weight. The requirement value must be an integer.",
				"g-kicker [u:IGN] [name:value] ..."
			)
		);

		paginateBuilder.addItems(
			generateHelp("Get player's active (not claimed) auctions on all profiles", "auction [player]", "ah [player]") +
			generateHelp("Get an auction by it's UUID", "auction uuid [UUID]", "ah uuid [UUID]") +
			generateHelp("Get the lowest bin of an item", "bin [item]") +
			generateHelp("Get bazaar prices of an item", "bazaar [item]", "bz [item]") +
			generateHelp("Get the average auction price of an item", "average [item]", "avg [item]") +
			generateHelp("Get a player's bids", "bids [player]") +
			generateHelp("Query the auction house", "query [item]") +
			generateHelp("Get the price of an item from the bits shop", "bits [item]") +
			generateHelp("Calculate the price of an item on the auction house using the auction's UUID", "price [uuid]")
		);

		paginateBuilder.addItems(
			generateHelp("Get a player's inventory represented in emojis", "inventory [player] <profile>", "inv [player] <profile>") +
			generateHelp(
				"Get a player's inventory with lore",
				"inventory [player] <profile> [slot:number]",
				"inv [player] <profile> [slot:number]"
			) +
			generateHelp("Get a player's equipped armor with lore", "inventory armor [player] <profile>", "inv armor [player] <profile>") +
			generateHelp("Get a player's ender chest represented in emojis", "enderchest [player] <profile>", "echest [player] <profile>") +
			generateHelp("Get a player's talisman bag represented in emojis", "talisman [player] <profile>") +
			generateHelp("Get a player's talisman bag with lore", "talisman [player] <profile> [slot:number]") +
			generateHelp("Get a player's sacks' content bag represented in a list", "sacks [player] <profile>") +
			generateHelp("Get a player's wardrobe armors represented in emojis", "wardrobe [player] <profile>") +
			generateHelp("Get a player's wardrobe armors represented in a list", "wardrobe list [player] <profile>")
		);

		paginateBuilder.addItems(
			generateHelp("Claim automatic Skyblock roles. The player must be linked", "roles claim <profile>") +
			generateHelp("Get a player's bank and purse coins", "bank [player] <profile>") +
			generateHelp("Get a player's bank transaction history", "bank history [player] <profile>") +
			generateHelp("Get a player's networth", "networth [player] <profile>", "nw [player] <profile>") +
			generateHelp(
				"Get a player's networth with a detailed JSON of each item cost",
				"networth [player] <profile> verbose:true",
				"nw [player] <profile> verbose:true"
			) +
			generateHelp("Get a player's weight", "weight [player] <profile>") +
			generateHelp(
				"Calculate predicted weight using given stats (not 100% accurate)",
				"weight calculate [skill avg] [slayer] [cata level] [avg dungeon class level]"
			) +
			generateHelp("Get Hypixel information about a player", "hypixel [player]") +
			generateHelp("Get fastest Hypixel lobby parkour for a player", "hypixel parkour [player]") +
			generateHelp("Get a information about all of a player's profiles", "profiles [player]") +
			generateHelp("Get a player's missing talismans", "missing [player] <profile>")
		);

		paginateBuilder.addItems(
			(isAdmin ? generateHelp("Interactive message to create a Skyblock event", "event create") : "") +
			generateHelp("Get information about the current event", "event current") +
			generateHelp("Join the current event", "event join") +
			generateHelp("Leave the current event", "event leave") +
			generateHelp("Get the leaderboard for current event", "event leaderboard", "event lb") +
			(isAdmin ? generateHelp("Force end the event", "event end") : "")
		);

		if (isAdmin) {
			paginateBuilder.addItems(
				generateHelp("Get the current settings for the bot", "settings") +
				generateHelp("A walk-through on how to setup the bot", "setup") +
				generateHelp("Get the id's of all categories in guild", "categories") +
				generateHelp("Delete the current server settings", "settings delete --confirm") +
				generateHelp("Set a Hypixel API key for this server", "settings set hypixel_key [key]") +
				generateHelp("Delete the set Hypixel API of this server", "settings delete hypixel_key")
			);

			paginateBuilder.addItems(
				generateHelp("Get the current verify settings for the bot", "settings verify") +
				generateHelp("Enable or disable automatic verify", "settings verify [enable|disable]") +
				generateHelp("Message that users will see and react to in order to verify", "settings" + " verify message [message]") +
				generateHelp(
					"Add a role that user will receive " + "upon being verified. Cannot be @everyone or a managed role",
					"settings verify role add [@role]"
				) +
				generateHelp("Remove a verify role", "settings verify role remove [@role]") +
				generateHelp("Channel where the message to react for verifying will sent", "settings verify " + "channel [#channel]") +
				generateHelp("The nickname template on verifying. Can be set to none.", "settings verify nickname <prefix> [IGN] <postfix>")
			);

			paginateBuilder.addItems(
				generateHelp("Get the current apply names for the bot", "settings apply") +
				generateHelp("Create a new automatic apply with name 'name'", "settings apply create [name]") +
				generateHelp("Delete an automatic apply", "settings apply remove [name]") +
				generateHelp("Enable or disable automatic apply", "settings apply [name] [enable|disable]") +
				generateHelp("Message that users will see and react to in order to apply", "settings" + " apply [name] message [message]") +
				generateHelp("Role that will be pinged when a new application is submitted", "settings apply [name] staff_role [@role]") +
				generateHelp("Channel where the message to react for applying will sent", "settings apply [name] " + "channel [#channel]") +
				generateHelp(
					"Prefix that all new apply channels should start with (prefix-discordName)",
					"settings apply [name] prefix [prefix]"
				) +
				generateHelp("Category where new apply channels will be made", "settings apply [name] category " + "[category id]") +
				generateHelp(
					"Channel where new applications will be sent to be reviewed by staff",
					"settings apply [name] staff_channel " + "[#channel]"
				) +
				generateHelp(
					"Channel where the players who were accepted or waitlisted will be sent",
					"settings apply [name] waiting_channel [#channel]"
				) +
				generateHelp("Message that will be sent if applicant is accepted", "settings apply [name] accept_message [message]") +
				generateHelp(
					"Message that will be sent if applicant is waitlisted. Can be set to none",
					"settings apply [name] waitlist_message [message]"
				) +
				generateHelp(
					"Whether applicants must use an ironman profile. Default is false",
					"settings apply [name] ironman [true|false]"
				) +
				generateHelp("Message that will be sent if applicant is denied", "settings apply [name] deny_message [message]") +
				generateHelp(
					"Add a requirement that applicant must meet. At least one of the requirement types must be set",
					"settings apply [name] reqs add <slayer:amount> <skills:amount> <catacombs:amount> <weight:amount>"
				) +
				generateHelp(
					"Remove a requirement. Run `" + BOT_PREFIX + "settings apply [name]` to see the index for all current requirements",
					"settings apply [name] reqs remove [number]"
				)
			);

			paginateBuilder.addItems(
				generateHelp("Get the current roles settings for the bot", "settings roles") +
				generateHelp("Enable or disable automatic roles", "settings roles [enable|disable]") +
				generateHelp("Enable a specific automatic role (set to disable by default)", "settings roles enable [roleName]") +
				generateHelp(
					"Add a new level to a role with its corresponding discord role",
					"settings roles add [roleName] [value] [@role]"
				) +
				generateHelp("Remove a role level for a role", "settings roles remove [roleName] [value]") +
				generateHelp("Make a specific role stackable", "settings roles stackable [roleName] [true|false]") +
				generateHelp("Set a one level role's role", "settings roles set [roleName] [@role]")
			);

			paginateBuilder.addItems(
				generateHelp("Create a new guild roles with name `name`", "settings guild create [name]") +
				generateHelp("Enable or disable automatic guild role assigning", "settings guild [name] [enable|disable] role") +
				generateHelp("Set the guild name", "settings guild [name] set [guild_name]") +
				generateHelp("Set the role to give guild member's", "settings guild [name] role [@role]") +
				generateHelp("Enable or disable automatic guild rank assigning", "settings guild [name] [enable|disable] rank") +
				generateHelp("Add an automatic guild rank", "settings guild [name] add [rank_name] [@role]") +
				generateHelp("Remove an automatic guild rank", "settings guild [name] remove [rank_name]")
			);
		}

		if (channel != null) {
			paginateBuilder.build().paginate(channel, startingPage);
		} else {
			paginateBuilder.build().paginate(hook, startingPage);
		}
	}

	private static String generateHelp(String desc, String... commandName) {
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

	private static String generatePageMap(String... pageNames) {
		StringBuilder generatedStr = new StringBuilder();
		for (int i = 0; i < pageNames.length; i++) {
			generatedStr.append("\n• **Page ").append(i + 2).append(":** ").append(pageNames[i]);
		}
		return generatedStr.toString();
	}

	private static void putMultiple(Map<String, Integer> map, int value, String... keys) {
		for (String key : keys) {
			map.put(key, value);
		}
	}

	@Override
	protected void execute(CommandEvent event) {
		new Thread(
			() -> {
				logCommand(event.getGuild(), event.getAuthor(), event.getMessage().getContentRaw());
				String pageNum = event.getMessage().getContentRaw().toLowerCase().split(" ").length >= 2
					? event.getMessage().getContentRaw().toLowerCase().split(" ")[1]
					: "";
				getHelp(pageNum, event.getMember(), event.getChannel(), null);
			}
		)
			.start();
	}
}
