/*
 * Skyblock Plus - A Skyblock focused Discord bot with many commands and customizable features to improve the experience of Skyblock players and guild staff!
 * Copyright (c) 2021 kr45732
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.skyblockplus.help;

import static com.skyblockplus.features.listeners.AutomaticGuild.getGuildPrefix;
import static com.skyblockplus.utils.Utils.*;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.skyblockplus.utils.command.CommandExecute;
import com.skyblockplus.utils.command.CustomPaginator;
import com.skyblockplus.utils.command.PaginatorEvent;
import com.skyblockplus.utils.structs.PaginatorExtras;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import org.apache.commons.lang3.ArrayUtils;

public class HelpCommand extends Command {

	public static final List<HelpData> helpDataList = new ArrayList<>();
	private static final String[] nonAdmin = {
		"General",
		"Slayer",
		"Skills",
		"Dungeons",
		"Guild",
		"Price Commands",
		"Inventory",
		"Miscellaneous Commands",
		"Party",
		"Skyblock Event",
	};
	private static final String[] admin = ArrayUtils.addAll(
		nonAdmin,
		"Settings",
		"Verify Settings",
		"Guild | Roles & Ranks Settings",
		"Guild | Apply Settings",
		"Roles Settings"
	);

	private static final String[] pageTitles = ArrayUtils.addFirst(admin, "Navigation");

	public HelpCommand() {
		this.name = "help";
		this.aliases = new String[] { "commands" };
		this.cooldown = globalCooldown;
		this.botPermissions = defaultPerms();
		setHelpList();
	}

	public static void setHelpList() {
		helpDataList.clear();
		helpDataList.addAll(
			Arrays.asList(
				// General
				new HelpData("help", "Show the help menu with all the commands.", "help")
					.addSecondData("Show the help menu for a certain command.", "help <command>")
					.addExamples("help", "help Guild Experience")
					.addAliases("commands"),
				new HelpData("information", "Get information about this bot.").addAliases("info", "about"),
				new HelpData("invite", "Get the invite link and Discord link for the bot."),
				new HelpData("link", "Get what Hypixel account you are linked to.", "link")
					.addSecondData("Link your Hypixel account to the bot.", "link <player>")
					.addExamples("link CrypticPlasma"),
				new HelpData("unlink", "Unlink your account from the bot.").addAliases("unverify"),
				new HelpData("vote", "Links for where you can vote for the bot."),
				// Slayer
				new HelpData("slayer", "Get the slayer data of a player.", "slayer [player] [profile]")
					.addAliases("slayers")
					.addExamples("slayer CrypticPlasma", "slayer CrypticPlasma Zucchini"),
				// Skills
				new HelpData("skills", "Get the skills data of a player.", "skills [player] [profile]")
					.addAliases("skill")
					.addExamples("skills CrypticPlasma", "skills CrypticPlasma Zucchini"),
				new HelpData("hotm", "Get a player's heart of the mountain statistics.", "hotm [player] [profile]")
					.addExamples("hotm CrypticPlasma", "hotm CrypticPlasma Zucchini"),
				// Dungeons
				new HelpData("dungeons", "Get the dungeons data of a player.", "dungeons [player] [profile]")
					.addAliases("cata", "catacombs")
					.addExamples("dungeons CrypticPlasma", "dungeons CrypticPlasma Zucchini"),
				new HelpData("essence", "Main essence command.")
					.addSubcommands(
						new HelpData("upgrade", "Interactive message to find the essence amount to upgrade an item.", "upgrade <item>")
							.addExamples("upgrade Hyperion"),
						new HelpData("information", "Get the amount of essence to upgrade an item for each level.", "information <item>")
							.addExamples("information Hyperion")
							.addAliases("info"),
						new HelpData("player", "Get a player's essence amounts and their essence shop upgrades.", "[player] [profile]")
							.addExamples("player CrypticPlasma", "player CrypticPlasma Zucchini")
					),
				new HelpData("partyfinder", "A party finder helper that shows a player's dungeon stats.", "partyfinder [player] [profile]")
					.addAliases("pf")
					.addExamples("partyfinder CrypticPlasma", "partyfinder CrypticPlasma Zucchini"),
				// Guild
				new HelpData("guild", "Main guild command")
					.addSecondData("Find what guild a player is in.", "guild <player>")
					.addSubcommands(
						new HelpData("information", "Get information and statistics about a player's guild.", "information <u:player>")
							.addSecondData("Get information and statistics about a guild.", "information <g:guild_name>")
							.addAliases("info")
							.addExamples("information u:CrypticPlasma", "information g:Skyblock_Forceful"),
						new HelpData("members", "Get a list of all members in a player's guild.", "members <u:player>")
							.addSecondData("Get a list of all members in a guild.", "members <g:guild_name>")
							.addExamples("members u:CrypticPlasma", "members g:Skyblock_Forceful"),
						new HelpData(
							"experience",
							"Get the experience leaderboard for a player's guild. Days can range from 1 to 7, default number of days is 7.",
							"experience <u:player> [days:days]"
						)
							.addAliases("exp")
							.addSecondData(
								"Get the experience leaderboard for a guild. Days can range from 1 to 7, default number of days is 7.",
								"experience <g:guild_name> [days:days]"
							)
							.addExamples("experience u:CrypticPlasma", "experience g:Skyblock_Forceful days:4")
					)
					.addAliases("g"),
				new HelpData(
					"guild-leaderboard",
					"Get a leaderboard for a guild. The type can be slayer, skills, catacombs, weight, sven_xp, rev_xp, tara_xp, and enderman_xp. The mode can be normal or ironman. A Hypixel API key must be set in settings set hypixel_key <key>.",
					"guild-leaderboard <type> <u:player> [mode:normal|ironman]"
				)
					.addAliases("g-lb")
					.addExamples("guild-leaderboard weight u:CrypticPlasma"),
				new HelpData(
					"guild-kicker",
					"Get all player's who don't meet the provided requirements. The requirement type can be skills, slayer, catacombs, or weight. The requirement value must be an integer. You can have up to 3 sets of requirements. Append the `--usekey` flag to force use the set Hypixel API key.",
					"guild-kicker <u:player> <[type:value ...]> ..."
				)
					.addAliases("g-kicker")
					.addExamples("guild-kicker u:CrypticPlasma [weight:4000 skills:40] [weight:4500]"),
				new HelpData(
					"guild-ranks",
					"A customizable helper that will tell you who to promote or demote in your Hypixel guild. Please DM me or join the Skyblock Plus Discord server to set this up for your guild.",
					"guild-ranks <u:player> [mode:normal|ironman]"
				)
					.addAliases("g-ranks")
					.addAliases("g-rank")
					.addExamples("guild-ranks u:CrypticPlasma"),
				// Auctions
				new HelpData("auctions", "Get a player's unclaimed auctions on all profiles.", "auctions [player]")
					.addSecondData("Get information about an auction by it's UUID", "auctions uuid <UUID>")
					.addAliases("auction", "ah")
					.addExamples("auctions CrypticPlasma", "auctions uuid 77df55d9c0084473b113265ef48fb396"),
				new HelpData("bin", "Get the lowest bin of an item.", "bin <item>").addAliases("lbin").addExamples("bin Necron Chestplate"),
				new HelpData("bazaar", "Get the bazaar prices of an item.", "bazaar <item>")
					.addAliases("bz")
					.addExamples("bazaar Booster Cookie"),
				new HelpData("average", "Get the average auction price of an item.", "average <item>")
					.addAliases("avg")
					.addExamples("average Necron's Handle"),
				new HelpData("bids", "Get a player's auction house bids", "bids [player].").addExamples("bids CrypticPlasma"),
				new HelpData(
					"query",
					"Query the auction house for the lowest bin of an item. This command lets you make more specific queries than the lowest bin command.",
					"query <item>"
				)
					.addExamples("query Necron's Chestplate ✪✪✪✪✪"),
				new HelpData("bits", "Get the bits cost of an item from the bits shop.", "bits <item>")
					.addExamples("bits God Potion")
					.addAliases("bit"),
				new HelpData(
					"calculate",
					"Calculate the price of an item on the auction house using the auction's UUID.",
					"calculate <UUID>"
				)
					.addExamples("calculate 8be8bef8c46f4dbda2eccd1ca0c30e27"),
				new HelpData("track", "Main track command")
					.addSubcommands(
						new HelpData(
							"auctions",
							"Track a player's auctions. You will get a DM whenever any of a player's auctions sell.",
							"track [player]"
						)
							.addExamples("auctions CrypticPlasma"),
						new HelpData("stop", "Stop tracking a player's auctions")
					),
				// Inventory
				new HelpData("inventory", "Get a player's inventory represented in emojis.", "inventory [player] [profile]")
					.addSecondData("Get a player's inventory with lore.", "inventory [player] [profile] <slot:number>")
					.addAliases("inv")
					.addExamples(
						"inventory CrypticPlasma",
						"inventory CrypticPlasma Zucchini",
						"inventory CrypticPlasma slot:1",
						"inventory CrypticPlasma Zucchini slot:1"
					),
				new HelpData("armor", "Get a player's equipped armor with lore.", "armor [player] [profile]")
					.addExamples("armor CrypticPlasma", "armor CrypticPlasma Zucchini"),
				new HelpData("enderchest", "Get a player's ender chest represented in emojis.", "enderchest [player] [profile]")
					.addAliases("ec", "echest")
					.addExamples("enderchest CrypticPlasma", "enderchest CrypticPlasma Zucchini"),
				new HelpData("talisman", "Get a player's talisman bag represented in emojis.", "talisman [player] [profile]")
					.addSecondData("Get a player's talisman bag with lore.", "talisman [player] [profile] <slot:number>")
					.addExamples(
						"talisman CrypticPlasma",
						"talisman CrypticPlasma Zucchini",
						"talisman CrypticPlasma slot:1",
						"talisman CrypticPlasma Zucchini slot:1"
					)
					.addAliases("talismans"),
				new HelpData(
					"sacks",
					"Get a player's sacks' content bag represented in a list. Sorted by descending price.",
					"sacks [player] [profile]"
				)
					.addExamples("sacks CrypticPlasma", "sacks CrypticPlasma Zucchini"),
				new HelpData("wardrobe", "Get a player's wardrobe armors represented in emojis.", "wardrobe [player] [profile]")
					.addSecondData("Get a player's wardrobe armors represented in a list.", "wardrobe list [player] [profile]")
					.addExamples(
						"wardrobe CrypticPlasma",
						"wardrobe CrypticPlasma Zucchini",
						"wardrobe list CrypticPlasma",
						"wardrobe list CrypticPlasma Zucchini"
					),
				new HelpData("pets", "Get a player's pets.", "pets [player] [profile]")
					.addExamples("pets CrypticPlasma", "pets CrypticPlasma Zucchini"),
				// Misc
				new HelpData("roles", "Main roles command.")
					.addAliases("role")
					.addSubcommands(
						new HelpData("claim", "Claim your automatic Skyblock roles. You must be linked to the bot.", "claim <profile>")
							.addExamples("claim", "claim Zucchini")
					),
				new HelpData("bank", "Get a player's bank and purse coins.", "bank [player] [profile]")
					.addSecondData("Get a player's bank transaction history.", "bank history [player] [profile]")
					.addExamples(
						"bank CrypticPlasma",
						"bank CrypticPlasma Zucchini",
						"bank history CrypticPlasma",
						"bank history CrypticPlasma Zucchini"
					),
				new HelpData("networth", "Calculate a player's networth.", "networth [player] [profile]")
					.addSecondData(
						"Calculate a player's networth with a detailed JSON of each item cost.",
						"networth [player] [profile] --verbose"
					)
					.addAliases("nw", "n")
					.addExamples(
						"networth CrypticPlasma",
						"networth CrypticPlasma Zucchini",
						"networth CrypticPlasma --verbose",
						"networth CrypticPlasma Zucchini --verbose"
					),
				new HelpData(
					"active-coins",
					"Get a player's active coins. This is the sum of their bank, purse, and sold auction coins.",
					"active-coins [player] [profile]"
				)
					.addExamples("active-coins CrypticPlasma", "active-coins CrypticPlasma Zucchini")
					.addAliases("ac"),
				new HelpData(
					"weight",
					"Get a player's slayer, skills, dungeons, and total weight. Shows both senither and lily weight.",
					"weight [player] [profile]"
				)
					.addSecondData(
						"Calculate predicted senither weight using given stats (not 100% accurate).",
						"weight calculate [skill avg] [slayer] [cata level] [avg dungeon class level]"
					)
					.addExamples("weight CrypticPlasma", "weight CrypticPlasma Zucchini", "weight calculate 37 600500 23 22"),
				new HelpData("hypixel", "Get Hypixel information about a player.", "hypixel [player]")
					.addSecondData("Get fastest Hypixel lobby parkour for a player.", "hypixel parkour [player]")
					.addExamples("hypixel CrypticPlasma", "hypixel parkour CrypticPlasma"),
				new HelpData("missing", "Get a player's missing talismans.", "missing [player] [profile]")
					.addExamples("missing CrypticPlasma", "missing CrypticPlasma Zucchini"),
				new HelpData("profiles", "Get information about all of a player's profiles.", "missing [player] [profile]")
					.addExamples("profiles CrypticPlasma", "profiles CrypticPlasma Zucchini"),
				new HelpData("cakes", "Get a player's active and inactive cake buffs.", "cakes [player] [profile]")
					.addExamples("cakes CrypticPlasma", "cakes CrypticPlasma Zucchini"),
				new HelpData("harp", "Get a player's harp statistics.", "harp [player] [profile]")
					.addExamples("harp CrypticPlasma", "harp CrypticPlasma Zucchini"),
				new HelpData("uuid", "Convert username to UUID or UUID to username.", "uuid [username|uuid]")
					.addExamples("uuid CrypticPlasma", "uuid 044903b7a9d3416d957f929557af6c88"),
				new HelpData("fetchur", "Get the item that fetchur wants today"),
				// Party
				new HelpData("party", "Main party command.")
					.addSubcommands(
						new HelpData("create", "Interactive message to create a new party."),
						new HelpData("current", "Get information about your current party."),
						new HelpData("join <username>", "Join a player's party where username is the party leader's IGN.")
							.addExamples("join CrypticPlasma"),
						new HelpData("leave", "Leave your current party."),
						new HelpData("list", "List all active parties"),
						new HelpData("disband", "Disband your current party."),
						new HelpData("kick <username>", "Kick a member from your party").addExamples("kick CrypticPlasma")
					),
				// Skyblock event
				new HelpData("event", "Main event command.")
					.addSubcommands(
						new HelpData("create", "Interactive message to create a Skyblock event."),
						new HelpData("current", "Get information about the current event."),
						new HelpData("join", "Join the current event."),
						new HelpData("leave", "Leave the current event."),
						new HelpData("leaderboard", "Get the leaderboard for current event.").addAliases("lb"),
						new HelpData("end", "Force end the event."),
						new HelpData("cancel", "Cancel the event. No prizes or winners will be announced.")
					),
				// Settings
				new HelpData("settings", "Main settings command.")
					.addAliases("config", "configuration")
					.addSecondData("View the current settings for the Discord server.", "settings")
					.addSubcommands(
						new HelpData("delete", "Delete certain settings or all settings from the database.")
							.addSubcommands(
								new HelpData("all", "Delete the current server settings."),
								new HelpData("hypixel_key", "Delete the set Hypixel API of this server."),
								new HelpData("prefix", "Reset the prefix of the bot")
							),
						new HelpData("set", "Set certain settings.")
							.addSubcommands(
								new HelpData(
									"hypixel_key",
									"Set a Hypixel API key for this server. Once set, this cannot be viewed for the privacy of the key owner. The key is currently only used in the guild leaderboard and guild kicker command.",
									"hypixel_key <key>"
								),
								new HelpData(
									"prefix",
									"Set the prefix of the bot. Must be a least one character and no more than five.",
									"prefix [prefix]"
								),
								new HelpData(
									"pf_category",
									"Set the category where new channels will be created for the bot's party feature. Can be set to 'none' to disable creating new channels.",
									"pf_category <category>"
								)
							),
						new HelpData("verify", "Main command for verification settings.")
							.addSecondData("Get the current verification settings for the bot.", "verify")
							.addSubcommands(
								new HelpData("enable", "Enable automatic verify."),
								new HelpData("disable", "Disable automatic verify."),
								new HelpData("message", "The message that users will see when verifying.", "message <message>")
									.addExamples("message Run +link <IGN> replacing IGN with your IGN to verify!"),
								new HelpData("role", "Modify roles given on verification")
									.addSubcommands(
										new HelpData(
											"add",
											"Add a role that user will receive upon being verified. The role cannot be @everyone or a managed role. You can add a max of 3 roles.",
											"add <@role>"
										),
										new HelpData("remove", "Remove a verify role.", "remove <@role>")
									),
								new HelpData(
									"channel",
									"The channel where the verify message will be sent. Other messages will be auto-deleted.",
									"channel <#channel>"
								),
								new HelpData(
									"nickname",
									"The nickname template that a user will be renamed to on verifying. Can be set to none. You can use [GUILD_RANK] in the template. It will be replaced with the user's guild rank if they are in any guilds in `settings guild`.",
									"nickname [prefix] [IGN] [postfix]"
								)
									.addExamples("nickname Verified | [IGN]", "nickname [[GUILD_RANK]] [IGN]")
							),
						new HelpData("roles", "Main command for automatic roles settings.")
							.addSecondData("Get the current roles settings for the bot.", "roles")
							.addSubcommands(
								new HelpData("enable", "Enable automatic roles.")
									.addSecondData("Enable a specific automatic role (disabled by default).", "enable <roleName>")
									.addExamples("enable", "enable dungeon_secrets"),
								new HelpData("disable", "Disable automatic roles.")
									.addSecondData("Disable a specific automatic role.", "disable <roleName>")
									.addExamples("disable", "disable dungeon_secrets"),
								new HelpData(
									"add",
									"Add a new level to a role with its corresponding Discord role.",
									"add <roleName> <value> <@role>"
								)
									.addExamples("add sven 400000 @sven 8", "add alchemy 50 @alchemy 50"),
								new HelpData("remove", "Remove a role level for a role.", "remove <roleName> <value>")
									.addExamples("remove sven 400000", "remove alchemy 50"),
								new HelpData(
									"set <roleName> <@role>",
									"Set the Discord role for a one level role.",
									"set <roleName> <@role>"
								)
									.addExamples("set ironman @ironman player", "set pet_enthusiast @pet lover")
							),
						new HelpData("guild", "Main command for automatic guild (application and guild roles/ranks).")
							.addSecondData("List all setup automatic guilds.", "guild")
							.addSubcommands(
								new HelpData("create", "Create a new automatic guild for a guild.", "create <name>")
									.addExamples("create myGuild"),
								new HelpData("remove", "Remove an automatic guild.", "remove <name>").addExamples("remove myGuild"),
								new HelpData("member_role", "Automatic guild member role.", "settings guild <name> member_role", true)
									.addSubcommands(
										new HelpData("enable", "Enable automatic guild role."),
										new HelpData("disable", "Disable automatic guild role."),
										new HelpData("<@role>", "Set the role to give guild members.")
									),
								new HelpData("ranks", "Automatic guild ranks.", "settings guild <name> ranks", true)
									.addSubcommands(
										new HelpData("enable", "Enable automatic guild ranks."),
										new HelpData("disable", "Disable automatic guild ranks."),
										new HelpData("add", "Add a guild rank.", "add <rank> <@role>"),
										new HelpData("remove", "Remove a guild rank.", "remove <rank>")
									),
								new HelpData("counter", "Automatic guild member counter.", "settings guild <name> counter", true)
									.addSubcommands(
										new HelpData(
											"enable",
											"Enable and create the voice channel for the automatic guild member counter."
										),
										new HelpData("disable", "Disable automatic guild member counter.")
									),
								new HelpData("apply", "Automatic application system for this guild.")
									.addSubcommands(
										new HelpData("enable", " Enable automatic apply.", "settings guild <name> apply enable", true),
										new HelpData("disable", " Enable automatic disable.", "settings guild <name> apply disable", true),
										new HelpData(
											"message",
											"The message that users will see when verifying.",
											"settings guild <name> apply message <message>",
											true
										),
										new HelpData(
											"staff_role",
											"Modify roles that will be pinged for an application",
											"settings guild <name> apply staff_role",
											true
										)
											.addSubcommands(
												new HelpData(
													"add",
													"Add a role which will be pinged when a new application is submitted.",
													"settings guild <name> apply staff_role add <@role>",
													true
												),
												new HelpData(
													"remove",
													"Remove a staff ping role.",
													"settings guild <name> apply staff_role remove <@role>",
													true
												)
											),
										new HelpData(
											"channel",
											"Channel where the message to react for applying will sent.",
											"settings guild <name> apply channel <#channel>",
											true
										),
										new HelpData(
											"category",
											"Category where new apply channels will be made. Run `categories` to get the ID's of all categories in the server.",
											"settings guild <name> apply category <category>",
											true
										),
										new HelpData(
											"staff_channel",
											"Channel where new applications will be sent to be reviewed by staff.",
											"settings guild <name> apply staff_channel <#channel>",
											true
										),
										new HelpData(
											"waiting_channel",
											"Channel where the players who were accepted or waitlisted will be sent. Optional and can be set to none.",
											"settings guild <name> apply waiting_channel <#channel>",
											true
										),
										new HelpData(
											"accept_message",
											"Message that will be sent if applicant is accepted.",
											"settings guild <name> apply accept_message <message>",
											true
										),
										new HelpData(
											"waitlist_message",
											"Message that will be sent if applicant is waitlisted. Optional and can be set to none.",
											"settings guild <name> apply waitlist_message <message>",
											true
										),
										new HelpData(
											"ironman",
											"Whether applicants must use an ironman profile. Default is false.",
											"settings guild <name> apply ironman <true|false>",
											true
										),
										new HelpData(
											"deny_message",
											"Message that will be sent if applicant is denied.",
											"settings guild <name> apply deny_message <message>",
											true
										),
										new HelpData(
											"requirements",
											"Requirements applications must meet. An application will be auto-denied if they do not meet the requirements.",
											"settings guild <name> apply requirements",
											true
										)
											.addAliases("reqs", "req")
											.addSubcommands(
												new HelpData(
													"add",
													"Add a requirement that applicant must meet. At least one of the requirement types must be set.",
													"settings guild <name> apply requirements add [slayer:amount] [skills:amount] [catacombs:amount] [weight:amount]",
													true
												),
												new HelpData(
													"remove",
													"Remove a requirement by its index. Run `settings guild <name>` to see the index for all current requirements.",
													"settings guild <name> apply requirements remove <index>",
													true
												)
											)
									)
							)
					),
				new HelpData("setup", "A short walk-through on how to setup the bot."),
				new HelpData("categories", "Get the id's of all categories in the Discord server.")
			)
		);
	}

	public static EmbedBuilder getHelp(String pageStr, PaginatorEvent event) {
		int startingPage = 0;
		if (pageStr != null) {
			String[] pageStrSplit = pageStr.split(" ", 2);

			if (pageStrSplit.length >= 1) {
				HelpData matchCmd = helpDataList.stream().filter(cmd -> cmd.matchTo(pageStrSplit[0])).findFirst().orElse(null);
				if (matchCmd != null) {
					return matchCmd.getHelp(pageStrSplit.length == 2 ? pageStrSplit[1] : null, getGuildPrefix(event.getGuild().getId()));
				}
			}

			try {
				startingPage = Integer.parseInt(pageStr);
			} catch (Exception ignored) {
				return defaultEmbed("Invalid command");
			}
		}

		CustomPaginator.Builder paginateBuilder = defaultPaginator(event.getUser())
			.setColumns(1)
			.setItemsPerPage(1)
			.setPaginatorExtras(new PaginatorExtras().setTitles(pageTitles));

		boolean isAdmin = event.getMember().hasPermission(Permission.ADMINISTRATOR);

		paginateBuilder.addItems(
			"Use the arrow buttons to navigate through the pages" + generatePageMap(isAdmin) + "\n\n<> = required [] = optional"
		);

		HelpGenerator help = new HelpGenerator(getGuildPrefix(event.getGuild().getId()));
		paginateBuilder.addItems(
			help.create("help", "Show the help menu with all the commands") +
			help.create("help <command>", "Show the help menu for a certain command") +
			help.create("information", "Show information and statistics about the bot") +
			help.create("invite", "Get the invite link and Discord link for the bot") +
			help.create("link <player>", "Link your Hypixel account to the bot") +
			help.create("link", "Get what Hypixel account you are linked to") +
			help.create("unlink", "Unlink your account from the bot") +
			help.create("vote", "Vote for the bot")
		);

		paginateBuilder.addItems(help.create("slayer [player] [profile]", "Get the slayer data of a player"));

		paginateBuilder.addItems(
			help.create("skills [player] [profile]", "Get the skills data of a player") +
			help.create("hotm [player] [profile]", "Get a player's heart of the mountain statistics")
		);

		paginateBuilder.addItems(
			help.create("dungeons [player] [profile]", "Get the dungeons data of a player") +
			help.create("essence upgrade <item>", "Interactive message to find the essence amount to upgrade an item") +
			help.create("essence information <item>", "Get the amount of essence to upgrade an item for each level") +
			help.create("essence player [player] [profile]", "Get a player's essence amounts and essence shop upgrades") +
			help.create("partyfinder [player] [profile]", "A party finder helper that shows a player's dungeon statistics")
		);

		paginateBuilder.addItems(
			help.create("guild <player>", "Find what guild a player is in") +
			help.create("guild information <u:player>", "Get information and statistics about a player's guild") +
			help.create("guild information <g:guild_name>", "Get information and statistics about a guild") +
			help.create("guild members <u:player>", "Get a list of all members in a player's guild") +
			help.create("guild members <g:guild_name>", "Get a list of all members in a guild") +
			help.create("guild experience <u:player>", "Get the experience leaderboard for a player's guild") +
			help.create("guild experience <g:guild_name>", "Get the experience leaderboard for a guild") +
			help.create("g-lb <type> <u:player> [mode:normal|ironman]", "Get a leaderboard for a player's guild") +
			help.create(
				"g-kicker <u:player> <type:value> ...",
				"Get all player's who don't meet the provided requirements. The requirement name can be skills, slayer, catacombs, or weight. The requirement value must be an integer."
			) +
			help.create(
				"g-ranks <u:player> [mode:normal|ironman]",
				"A customizable helper that will tell you who to promote or demote in your Hypixel guild"
			)
		);

		paginateBuilder.addItems(
			help.create(
				"auctions [player] [sort:low|high] [filter:unsold|sold] [--verbose]",
				"Get a player's unclaimed auctions on all profiles"
			) +
			help.create("auctions uuid <UUID>", "Get an auction by its UUID") +
			help.create("bin <item>", "Get the lowest bin of an item") +
			help.create("bazaar <item]", "Get bazaar prices of an item") +
			help.create("average <item>", "Get the average auction price of an item") +
			help.create("bids [player]", "Get a player's auction house bids") +
			help.create("query <item>", "Query the auction house for the lowest bin of an item") +
			help.create("bits <item>", "Get the bits cost of an item from the bits shop") +
			help.create("calculate <uuid>", "Calculate the price of an item on the auction house using the auction's UUID") +
			help.create("track auctions <player>", "Get a DM when any of a player's auctions sell") +
			help.create("track stop", "Stop tracking a player's auctions")
		);

		paginateBuilder.addItems(
			help.create("inventory [player] [profile]", "Get a player's inventory represented in emojis") +
			help.create("inventory [player] [profile] <slot:number>", "Get a player's inventory with lore") +
			help.create("armor [player] [profile]", "Get a player's equipped armor with lore") +
			help.create("enderchest [player] <profile]", "Get a player's ender chest represented in emojis") +
			help.create("talisman [player] [profile]", "Get a player's talisman bag represented in emojis") +
			help.create("talisman [player] [profile] <slot:number>", "Get a player's talisman bag with lore") +
			help.create("sacks [player] [profile]", "Get a player's sacks' content bag represented in a list") +
			help.create("wardrobe [player] [profile]", "Get a player's wardrobe armors represented in emojis") +
			help.create("wardrobe list [player] [profile]", "Get a player's wardrobe armors represented in a list") +
			help.create("pets [player] [profile]", "Get a player's pets")
		);

		paginateBuilder.addItems(
			help.create("roles claim [profile]", "Claim your automatic Skyblock roles based on your statistics") +
			help.create("roles list", "List all roles that can be claimed through the bot") +
			help.create("bank [player] [profile]", "Get a player's bank and purse coins") +
			help.create("bank history [player] [profile]", "Get a player's bank transaction history") +
			help.create("active-coins [player] [profile]", "Get a player's active coins (bank, purse, and sold auctions)") +
			help.create("networth [player] [profile]", "Calculate a player's networth") +
			help.create("networth [player] [profile] --verbose", "Calculate a player's networth with a detailed JSON of each item cost") +
			help.create("weight [player] [profile]", "Get a player's slayer, skills, dungeons, and total weight") +
			help.create(
				"weight calculate <skill avg> <slayer> <cata lvl> <avg dungeon class lvl>",
				"Estimate a weight using given statistics"
			) +
			help.create("hypixel [player]", "Get Hypixel information about a player") +
			help.create("hypixel parkour [player]", "Get fastest Hypixel lobby parkour for a player") +
			help.create("profiles [player]", "Get information about all of a player's profiles") +
			help.create("missing [player] [profile]", "Get a player's missing talismans") +
			help.create("fetchur", "Get the item that fetchur wants today") +
			help.create("cakes [player] [profile]", "Get a player's inactive and active cake buffs") +
			help.create("harp [player] [profile]", "Get a player's harp statistics") +
			help.create("uuid [username|uuid]", "Convert username to UUID or UUID to username")
		);

		paginateBuilder.addItems(
			help.create("party create", "Interactive message to create a party") +
			help.create("party current", "Get information about your current party") +
			help.create("party join <username>", "Join a party") +
			help.create("party leave", "Leave your current party") +
			help.create("party list", "List all active parties") +
			help.create("party disband", "Disband your party") +
			help.create("party kick <username>", "Kick a member from your party")
		);

		paginateBuilder.addItems(
			help.createAdmin("event create", "Interactive message to create a Skyblock event", isAdmin) +
			help.create("event current", "Get information about the current event") +
			help.create("event join [profile]", "Join the current event") +
			help.create("event leave", "Leave the current event") +
			help.create("event leaderboard", "Get the leaderboard for current event") +
			help.createAdmin("event end", "Force end the event", isAdmin) +
			help.createAdmin("event cancel", "Cancel the event. No announcement will be made", isAdmin)
		);

		if (isAdmin) {
			paginateBuilder.addItems(
				help.create("settings", "View the current settings for the Discord server") +
				help.create("settings general", "View the bot's general settings for this server") +
				help.create("setup", "A short walk-through on how to setup the bot") +
				help.create("categories", "Get the name and id of all categories in this server") +
				help.create("settings set hypixel_key <key>", "Set a Hypixel API key for this server") +
				help.create("settings set prefix <prefix>", "Set the prefix of the bot") +
				help.create(
					"settings set pf_category <category>",
					"Set the category where new channels will be created for the bot's party feature"
				) +
				help.create("settings set guest_role <@role>", "Set the guest role") +
				help.create("settings delete hypixel_key", "Delete the set Hypixel API key of this server") +
				help.create("settings delete prefix", "Reset the prefix of the bot") +
				help.create("settings delete all", "Delete the current server settings")
			);

			paginateBuilder.addItems(
				help.create("settings verify", "Get the current verify settings for the bot") +
				help.create("settings verify <enable|disable>", "Enable or disable automatic verify") +
				help.create("settings verify message <message>", "The message that users will see when verifying") +
				help.create("settings verify role add <@role>", "Add a role that user will receive upon being verified") +
				help.create("settings verify role remove <@role>", "Remove a verify role") +
				help.create(
					"settings verify channel <#channel>",
					"Channel where the verify message will be sent and messages will be auto deleted"
				) +
				help.create(
					"settings verify nickname [prefix] [IGN] [postfix]",
					"The nickname template on verifying. Can be set to none."
				) +
				help.create("settings verify <enable|disable> sync", "Enable or disable automatic verify role and nickname syncing")
			);

			paginateBuilder.addItems(
				help.create("settings guild create <name>", "Create a new automatic guild where name is the guild's name") +
				help.create("settings guild remove <name>", "Remove an automatic guild") +
				help.create("settings guild <name>", "View the settings for a specific automatic guild") +
				help.create("settings guild <name> member_role <enable|disable>", "Enable or disable automatic guild role assigning") +
				help.create("settings guild <name> member_role <@role>", "Set the role to give guild members") +
				help.create("settings guild <name> ranks <enable|disable>", "Enable or disable automatic guild rank assigning") +
				help.create("settings guild <name> ranks add <rank_name> <@role>", "Add an automatic guild rank") +
				help.create("settings guild <name> ranks remove <rank_name>", "Remove an automatic guild rank") +
				help.create("settings guild <name> counter <enable|disable>", "Enable or disable guild members counter")
			);

			paginateBuilder.addItems(
				help.create("settings guild <name> apply <enable|disable>", "Enable or disable automatic apply") +
				help.create("settings guild <name> apply message <message>", "The message that users will see when verifying") +
				help.create(
					"settings guild <name> apply staff_role add <@role>",
					"Add a role that will be pinged when a new application is submitted"
				) +
				help.create("settings guild <name> apply staff_role remove <@role>", "Remove a staff ping role") +
				help.create("settings guild <name> apply channel <#channel>", "Channel where the message to click for applying will sent") +
				help.create("settings guild <name> category <category>", "Category where new apply channels will be made") +
				help.create(
					"settings guild <name> apply staff_channel <#channel>",
					"Channel where new applications will be sent to be reviewed by staff"
				) +
				help.create(
					"settings guild <name> apply waiting_channel <#channel>",
					"Channel where the players who were accepted or waitlisted will be sent. Can be set to none"
				) +
				help.create("settings guild <name> apply accept_message <message>", "Message that will be sent if applicant is accepted") +
				help.create(
					"settings guild <name> apply waitlist_message <message>",
					"Message that will be sent if applicant is waitlisted. Can be set to none"
				) +
				help.create(
					"settings guild <name> apply ironman <true|false>",
					"Whether applicants must use an ironman profile. Defaults to false"
				) +
				help.create("settings guild <name> apply deny_message <message>", "Message that will be sent if applicant is denied") +
				help.create(
					"settings guild <name> requirements add [slayer:amount] [skills:amount] [catacombs:amount] [weight:amount]",
					"Add a requirement that applicant must meet. At least one of the requirement types must be set. Can be empty"
				) +
				help.create(
					"settings guild <name> apply reqs remove <index>",
					"Remove a requirement. Run `settings guild <name>` to see the index for all current requirements"
				)
			);

			paginateBuilder.addItems(
				help.create("settings roles", "Get the current roles settings for the bot") +
				help.create("settings roles <enable|disable>", "Enable or disable automatic roles") +
				help.create(
					"settings roles <enable|disable> <roleName|all>",
					"Enable or disable a specific automatic role or enable or disable each automatic role"
				) +
				help.create(
					"settings roles add <roleName> <value> <@role>",
					"Add a new level to a role with its corresponding Discord role"
				) +
				help.create("settings roles remove <roleName> <value>", "Remove a role level for a role") +
				help.create("settings roles set <roleName> <@role>", "Set a one level role's role")
			);
		}

		event.paginate(paginateBuilder, startingPage);
		return null;
	}

	private static String generatePageMap(boolean isAdmin) {
		StringBuilder generatedStr = new StringBuilder();
		for (int i = 0; i < (isAdmin ? admin : nonAdmin).length; i++) {
			generatedStr.append("\n• **Page ").append(i + 2).append(":** ").append((isAdmin ? admin : nonAdmin)[i]);
		}
		return generatedStr.toString();
	}

	@Override
	protected void execute(CommandEvent event) {
		new CommandExecute(this, event) {
			@Override
			protected void execute() {
				logCommand();
				setArgs(2);

				paginate(getHelp(args.length >= 2 ? args[1].toLowerCase() : null, new PaginatorEvent(event)));
			}
		}
			.queue();
	}

	static class HelpGenerator {

		private final String prefix;

		public HelpGenerator(String prefix) {
			this.prefix = prefix;
		}

		public String create(String commandName, String desc) {
			return "`" + prefix + commandName + "`: " + desc + "\n";
		}

		public String createAdmin(String commandName, String desc, boolean isAdmin) {
			return isAdmin ? "`" + prefix + commandName + "`: " + desc + "\n" : "";
		}
	}
}
