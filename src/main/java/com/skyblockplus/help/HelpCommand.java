package com.skyblockplus.help;

import static com.skyblockplus.Main.waiter;
import static com.skyblockplus.utils.Utils.*;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.skyblockplus.utils.CustomPaginator;
import com.skyblockplus.utils.structs.PaginatorExtras;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.interactions.InteractionHook;

public class HelpCommand extends Command {

	private static final String[] pageTitles = new String[] {
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
	private static final String[] nonAdmin = {
		"General",
		"Slayer",
		"Skills",
		"Dungeons",
		"Guild",
		"Price Commands",
		"Inventory",
		"Miscellaneous Commands",
		"Skyblock Event",
	};
	private static final String[] admin = {
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
		"Guild Roles Settings",
	};
	public static final List<HelpData> helpDataList = new ArrayList<>();

	public HelpCommand() {
		this.name = "help";
		this.aliases = new String[] { "commands" };
		this.cooldown = globalCooldown;
		addHelpList();
	}

	public static void addHelpList() {
		helpDataList.clear();
		helpDataList.addAll(
			Arrays.asList(
				new HelpData("help", "Show the help page", "help")
					.addSecondData("Get help about a specific command.", "help [command]")
					.addExamples("help", "help Guild Experience")
					.addAliases("commands"),
				new HelpData("information", "Get information about this bot.").addAliases("info"),
				new HelpData("invite", "Invite this bot to your server."),
				new HelpData("link", "Get what account you are linked too.", "link")
					.addSecondData("Link your Hypixel account to the bot.", "link [player]")
					.addExamples("link CrypticPlasma"),
				new HelpData("unlink", "Unlink your account from the bot"),
				new HelpData("slayer", "Get the slayer data of a player.", "slayer [player] <profile>")
					.addAliases("slayers")
					.addExamples("slayer CrypticPlasma", "slayer CrypticPlasma Zucchini"),
				new HelpData("skills", "Get the skills data of a player.", "skills [player] <profile>")
					.addAliases("skill")
					.addExamples("skills CrypticPlasma", "skills CrypticPlasma Zucchini"),
				new HelpData("dungeons", "Get the dungeons data of a player.", "dungeons [player] <profile>")
					.addAliases("cata", "catacombs")
					.addExamples("catacombs CrypticPlasma", "catacombs CrypticPlasma Zucchini"),
				new HelpData("essence", "Main essence command.")
					.addSubcommands(
						new HelpData("upgrade", "Interactive message to find the essence amount to upgrade an item.", "upgrade [item]")
							.addExamples("upgrade Hyperion"),
						new HelpData("information", "Get the amount of essence to upgrade an item for each level.", "information [item]")
							.addExamples("information Hyperion")
							.addAliases("info")
					),
				new HelpData("partyfinder", "A party finder helper that shows a player's dungeon stats.", "partyfinder [player] <profile>")
					.addAliases("pf")
					.addExamples("partyfinder CrypticPlasma", "partyfinder CrypticPlasma Zucchini"),
				new HelpData("guild", "Main guild command")
					.addSubcommands(
						new HelpData("information", "Get information and statistics about a player's guild.", "information [u:player]")
							.addSecondData("Get information and statistics about a guild.", "information [g:guild name]")
							.addAliases("info")
							.addExamples("information u:CrypticPlasma", "information g:Skyblock Forceful"),
						new HelpData("members", "Get a list of all members in a player's guild.", "members [u:player]")
							.addExamples("members CrypticPlasma"),
						new HelpData(
							"experience",
							"Get the experience leaderboard for a player's guild (past 7 days).",
							"experience [u:player]"
						)
							.addAliases("exp")
							.addExamples("experience u:CrypticPlasma")
					)
					.addAliases("g"),
				new HelpData("guild-requirements", "Get the application requirements set for an application.", "guild-requirements [name]")
					.addAliases("g-reqs", "guild-reqs"),
				new HelpData(
					"guild-leaderboard",
					"Get a leaderboard for a guild. The type can be slayer, skills, catacombs, weight, sven_xp, rev_xp, tara_xp, and enderman_xp. A Hypixel API key must be set in " +
					BOT_PREFIX +
					"settings set hypixel_key [key].",
					"guild-leaderboard [type] [u:player]"
				)
					.addAliases("g-lb")
					.addExamples("guild-leaderboard weight u:CrypticPlasma"),
				new HelpData(
					"guild-kicker",
					"Get all player's who don't meet the provided requirements. The requirement type can be skills, slayer, catacombs, or weight. The requirement value must be an integer. You can have up to 3 sets of requirements. Append the `--usekey` flag to force use the set Hypixel API key.",
					"guild-kicker [u:player] [type:value] ..."
				)
					.addAliases("g-kicker")
					.addExamples("guild-kicker u:CrypticPlasma [weight:4000 skills:40] [weight:4500]"),
				new HelpData("auctions", "Get a player's not claimed auctions on all profiles.", "auctions [player]")
					.addSecondData("Get information about an auction by it's UUID", "auctions uuid [UUID]")
					.addAliases("auction", "ah")
					.addExamples("auctions CrypticPlasma"),
				new HelpData("bin", "Get the lowest bin of an item.").addAliases("lbin").addExamples("bin Necron Chestplate"),
				new HelpData("bazaar", "Get the bazaar prices of an item.", "bazaar [item]")
					.addAliases("bz")
					.addExamples("bazaar Booster Cookie"),
				new HelpData("average", "Get the average auction price of an item.", "average [item]")
					.addAliases("avg")
					.addExamples("average Necron's Handle"),
				new HelpData("bids", "Get a player's auction bids", "bids [player].").addExamples("bids CrypticPlasma"),
				new HelpData(
					"query",
					"Query the auction house for the lowest bin of an item. This command lets you make more specific queries than the lowest bin command."
				)
					.addExamples("query Necron's Chestplate ✪✪✪✪✪"),
				new HelpData("bits", "Get the bits cost of an item from the bits shop.", "bits [item]")
					.addExamples("bits God Potion")
					.addAliases("bit"),
				new HelpData("price", "Calculate the price of an item on the auction house using the auction's UUID.", "price [UUID]")
					.addExamples("price 8be8bef8c46f4dbda2eccd1ca0c30e27"),
				new HelpData("inventory", "Get a player's inventory represented in emojis.", "inventory [player] <profile>")
					.addSecondData("Get a player's inventory with lore.", "inventory [player] <profile> [slot:number]")
					.addAliases("inv")
					.addExamples(
						"inventory CrypticPlasma",
						"inventory CrypticPlasma Zucchini",
						"inventory CrypticPlasma slot:1",
						"inventory CrypticPlasma Zucchini slot:1"
					),
				new HelpData("armor", "Get a player's equipped armor with lore.", "armor [player] <profile>")
					.addExamples("armor CrypticPlasma", "armor CrypticPlasma Zucchini"),
				new HelpData("enderchest", "Get a player's ender chest represented in emojis.", "enderchest [player] <profile>")
					.addAliases("ec", "echest")
					.addExamples("enderchest CrypticPlasma", "enderchest CrypticPlasma Zucchini"),
				new HelpData("talisman", "Get a player's talisman bag represented in emojis.", "talisman [player] <profile>")
					.addSecondData("Get a player's talisman bag with lore.", "talisman [player] <profile> [slot:number]")
					.addExamples(
						"talisman CrypticPlasma",
						"talisman CrypticPlasma Zucchini",
						"talisman CrypticPlasma slot:1",
						"talisman CrypticPlasma Zucchini slot:1"
					)
					.addAliases("talismans"),
				new HelpData("sacks", "Get a player's sacks' content bag represented in a list.", "sacks [player] <profile>")
					.addExamples("sacks CrypticPlasma", "sacks CrypticPlasma Zucchini"),
				new HelpData("wardrobe", "Get a player's wardrobe armors represented in emojis.", "wardrobe [player] <profile>")
					.addSecondData("Get a player's wardrobe armors represented in a list.", "wardrobe list [player] <profile>")
					.addExamples(
						"wardrobe CrypticPlasma",
						"wardrobe CrypticPlasma Zucchini",
						"wardrobe list CrypticPlasma",
						"wardrobe list CrypticPlasma Zucchini"
					),
				new HelpData("roles", "Main roles command.")
					.addAliases("role")
					.addSubcommands(
						new HelpData("claim", "Claim your automatic Skyblock roles. You must be linked to the bot.", "claim <profile>")
							.addExamples("claim", "claim Zucchini")
					),
				new HelpData("bank", "Get a player's bank and purse coins.", "bank [player] <profile>")
					.addSecondData("Get a player's bank transaction history.", "bank history [player] <profile>")
					.addExamples(
						"bank CrypticPlasma",
						"bank CrypticPlasma Zucchini",
						"bank history CrypticPlasma",
						"bank history CrypticPlasma Zucchini"
					),
				new HelpData("networth", "Get a player's networth.", "networth [player] <profile>")
					.addSecondData(
						"Get a player's networth with a detailed JSON of each item cost.",
						"networth [player] <profile> --verbose"
					)
					.addAliases("nw")
					.addExamples(
						"networth CrypticPlasma",
						"networth CrypticPlasma Zucchini",
						"networth CrypticPlasma --verbose",
						"networth CrypticPlasma Zucchini --verbose"
					),
				new HelpData("weight", "Get a player's weight.", "weight [player] <profile>")
					.addSecondData(
						"Calculate predicted weight using given stats (not 100% accurate).",
						"weight calculate [skill avg] [slayer] [cata level] [avg dungeon class level]"
					)
					.addExamples("weight CrypticPlasma", "weight CrypticPlasma Zucchini", "weight calculate 37 600500 23 22"),
				new HelpData("hypixel", "Get Hypixel information about a player.", "hypixel [player]")
					.addSecondData("Get fastest Hypixel lobby parkour for a player", "hypixel parkour [player]")
					.addExamples("hypixel CrypticPlasma", "hypixel parkour CrypticPlasma"),
				new HelpData("missing", "Get a player's missing talismans.", "missing [player] <profile>")
					.addExamples("missing CrypticPlasma", "missing CrypticPlasma Zucchini"),
				new HelpData("profiles", "Get information about all of a player's profiles.", "missing [player] <profile>")
					.addExamples("profiles CrypticPlasma", "profiles CrypticPlasma Zucchini"),
				new HelpData("event", "Main event command.")
					.addSubcommands(
						new HelpData("create", "Interactive message to create a Skyblock event."),
						new HelpData("current", "Get information about the current event."),
						new HelpData("join", "Join the current event."),
						new HelpData("leave", "Leave the current event."),
						new HelpData("leaderboard", "Get the leaderboard for current event.").addAliases("lb"),
						new HelpData("end", "Force end the event.")
					),
				new HelpData("settings", "Main settings command.")
					.addSecondData("Get the current settings for the bot.", "settings")
					.addSubcommands(
						new HelpData("delete", "Delete certain settings or all settings from the database.")
							.addSubcommands(
								new HelpData("all", "Delete the current server settings."),
								new HelpData("hypixel_key", "Delete the set Hypixel API of this server.")
							),
						new HelpData("set", "Set certain settings.")
							.addSubcommands(
								new HelpData(
									"hypixel_key",
									"Set a Hypixel API key for this server. Once set, this cannot be viewed for the privacy of the key owner. The key is currently only used in the guild leaderboard and guild kicker command.",
									"hypixel_key [key]"
								)
							),
						new HelpData("verify", "Main command for verification settings.")
							.addSecondData("Get the current verification settings for the bot.", "verify")
							.addSubcommands(
								new HelpData("enable", "Enable automatic verify."),
								new HelpData("disable", "Disable automatic verify."),
								new HelpData("message", "Message that users will see and react to in order to verify.", "message [message]")
									.addExamples("message Run +link [IGN] replacing IGN with your IGN to verify!"),
								new HelpData("role", "Modify roles given on verification")
									.addSubcommands(
										new HelpData(
											"add",
											"Add a role that user will receive upon being verified. The role cannot be @everyone or a managed role. You can add a max of 3 roles.",
											"add [@role]"
										),
										new HelpData("remove", "Remove a verify role.", "remove [@role]")
									),
								new HelpData(
									"channel",
									"The channel where the verify message will be sent. Other messages will be auto-deleted.",
									"channel [#channel]"
								),
								new HelpData(
									"nickname",
									"The nickname template that a user will be renamed to on verifying. Can be set to none. You can use [GUILD_RANK] in the template. It will be replaced with the user's guild rank if they are in any guilds in `settings guild`.",
									"nickname <prefix> [IGN] <postfix>"
								)
									.addExamples("nickname Verified | [IGN]", "nickname [IGN] - [GUILD_RANK]")
							),
						new HelpData("apply", "Main command for application settings.")
							.addSecondData("Get the current apply names for the bot.", "apply")
							.addSubcommands(
								new HelpData("create", "Create a new automatic apply with name 'name'.", "create [name]")
									.addExamples("create myGuild"),
								new HelpData("remove", "Delete an automatic apply.", "remove [name]").addExamples("remove myGuild"),
								new HelpData("enable", " Enable automatic apply.", "[name] enable").addExamples("myGuild enable"),
								new HelpData("disable", " Enable automatic disable.", "[name] disable").addExamples("myGuild disable"),
								new HelpData(
									"message",
									"Message that users will see and react to in order to apply.",
									"[name] message [message]"
								)
									.addExamples("myGuild message Click the button below to start an application!"),
								new HelpData(
									"staff_role",
									"Role that will be pinged when a new application is submitted.",
									"[name] staff_role [@role]"
								)
									.addExamples("myGuild staff_role @Application Ping"),
								new HelpData(
									"channel",
									"Channel where the message to react for applying will sent.",
									"[name] channel [#channel]"
								)
									.addExamples("myGuild channel #apply-for-guild"),
								new HelpData(
									"prefix",
									"Prefix that all new application channels will start with (prefix-discordName).",
									"[name] prefix [prefix]"
								)
									.addExamples("myGuild prefix application"),
								new HelpData(
									"category",
									"Category where new apply channels will be made. Run `categories` to get the ID's of all categories in the server.",
									"[name] category [category id]"
								),
								new HelpData(
									"staff_channel",
									"Channel where new applications will be sent to be reviewed by staff.",
									"[name] staff_channel [#channel]"
								)
									.addExamples("myGuild staff_channel #bot-applications"),
								new HelpData(
									"waiting_channel",
									"Channel where the players who were accepted or waitlisted will be sent. Optional and can be set to none.",
									"[name] waiting_channel [#channel]"
								)
									.addExamples("myGuild waiting_channel #waiting-for-invite"),
								new HelpData(
									"accept_message",
									"Message that will be sent if applicant is accepted.",
									"[name] accept_message [message]"
								)
									.addExamples("myGuild accept_message Congrats, you have been accepted!"),
								new HelpData(
									"waitlist_message",
									"Message that will be sent if applicant is waitlisted. Optional and can be set to none.",
									"[name] waitlist_message [message]"
								)
									.addExamples(
										"myGuild waitlist_message You have been accepted but waitlisted due to the guild being full at the moment"
									),
								new HelpData(
									"ironman",
									"Whether applicants must use an ironman profile. Default is false.",
									"[name] ironman [true|false]"
								)
									.addExamples("myGuild ironman true", "myGuild ironman false"),
								new HelpData(
									"deny_message",
									"Message that will be sent if applicant is denied.",
									"[name] deny_message [message]"
								)
									.addExamples("myGuild deny_message Sorry, you were not accepted"),
								new HelpData(
									"reqs",
									"Requirements applications must meet. An application will be auto-denied if they do not meet the requirements.",
									"[name] reqs"
								)
									.addSubcommands(
										new HelpData(
											"add",
											"Add a requirement that applicant must meet. At least one of the requirement types must be set.",
											"settings apply [name] reqs add <slayer:amount> <skills:amount> <catacombs:amount> <weight:amount>",
											true
										)
											.addExamples(
												"settings apply myGuild add slayer:500000 weight:3500",
												"settings apply myGuild add skills:38 slayer:100000 catacombs:30"
											),
										new HelpData(
											"remove",
											"Remove a requirement by its index. Run `settings apply [name]` to see the index for all current requirements.",
											"settings apply [name] reqs remove [number]",
											true
										)
											.addExamples("settings apply myGuild reqs remove 1")
									)
							),
						new HelpData("roles", "Main command for automatic roles settings.")
							.addSecondData("Get the current roles settings for the bot.", "apply")
							.addSubcommands(
								new HelpData("enable", "Enable automatic roles.")
									.addSecondData("Enable a specific automatic role (disabled by default).", "enable [roleName]")
									.addExamples("enable", "enable dungeon_secrets"),
								new HelpData("disable", "Disable automatic roles.")
									.addSecondData("Disable a specific automatic role.", "disable [roleName]")
									.addExamples("disable", "disable dungeon_secrets"),
								new HelpData(
									"add",
									"Add a new level to a role with its corresponding Discord role.",
									"add [roleName] [value] [@role]"
								)
									.addExamples("add sven 400000 @sven 8", "add alchemy 50 @alchemy 50"),
								new HelpData("remove", "Remove a role level for a role.", "remove [roleName] [value]")
									.addExamples("remove sven 400000", "remove alchemy 50"),
								new HelpData(
									"stackable",
									"Make a specific role stackable. If true, only the highest role will be given.",
									"stackable [roleName] [true|false]"
								)
									.addExamples("stackable sven true", "stackable alchemy false"),
								new HelpData(
									"set [roleName] [@role]",
									"Set the Discord role for a one level role.",
									"set [roleName] [@role]"
								)
									.addExamples("set all_slayer_nine @maxed slayers", "set pet_enthusiast @pet lover")
							),
						new HelpData("guild", "Main command for automatic guild roles and ranks settings.")
							.addSecondData("Get the current automatic guild names for the bot.", "guild")
							.addSubcommands(
								new HelpData("create", "Create a new guild roles with name 'name'.", "create [name]")
									.addExamples("create myGuild"),
								new HelpData("enable", "Enable automatic guild roles or ranks.", "[name] enable [role|rank]")
									.addExamples("myGuild enable role", "myGuild enable rank"),
								new HelpData("disable", "Disable automatic guild roles or ranks.", "[name] disable [role|rank]")
									.addExamples("myGuild disable role", "myGuild disable rank"),
								new HelpData("set", "Set the guild name.", "[name] set [guild_name]")
									.addExamples("myGuild set Skyblock_Forceful"),
								new HelpData("role", "Set the role to give guild members", "[name] role [@role]")
									.addExamples("myGuild role @guild member"),
								new HelpData("add", "Add an automatic guild rank.", "[name] add [rank_name] [@role]")
									.addExamples("myGuild add moderator @moderator", "myGuild add senior_officer @officer"),
								new HelpData("remove", "Remove an automatic guild rank.", "[name] remove [rank_name]")
									.addExamples("myGuild remove moderator", "myGuild remove senior_officer")
							)
					),
				new HelpData("setup", "A short walk-through on how to setup the bot."),
				new HelpData("categories", "Get the id's of all categories in guild")
			)
		);
	}

	public static EmbedBuilder getHelp(String pageStr, Member member, MessageChannel channel, InteractionHook hook) {
		int startingPage = 0;
		if (pageStr != null) {
			String[] pageStrSplit = pageStr.split(" ", 2);

			if (pageStrSplit.length >= 1) {
				HelpData matchCmd = helpDataList.stream().filter(cmd -> cmd.matchTo(pageStrSplit[0])).findFirst().orElse(null);
				if (matchCmd != null) {
					return matchCmd.getHelp(pageStrSplit.length == 2 ? pageStrSplit[1] : null);
				}
			}

			try {
				startingPage = Integer.parseInt(pageStr);
			} catch (Exception ignored) {
				return defaultEmbed("Invalid command");
			}
		}

		CustomPaginator.Builder paginateBuilder = defaultPaginator(waiter, member.getUser())
			.setColumns(1)
			.setItemsPerPage(1)
			.setPaginatorExtras(new PaginatorExtras().setTitles(pageTitles));

		boolean isAdmin = member.hasPermission(Permission.ADMINISTRATOR);

		paginateBuilder.addItems("Use the arrow emojis to navigate through the pages" + generatePageMap(isAdmin));

		paginateBuilder.addItems(
			generateHelp("help", "Show this help page") +
			generateHelp("help [command name]", "Go to the help page of a specific command") +
			generateHelp("information", "Get information about this bot") +
			generateHelp("invite", "Invite this bot to your server") +
			generateHelp("link [player]", "Link your discord and Hypixel account") +
			generateHelp("link", "Get what Hypixel account you are linked to") +
			generateHelp("unlink", "Unlink your account")
		);

		paginateBuilder.addItems(generateHelp("slayer [player] <profile>", "Get the slayer data of a player"));

		paginateBuilder.addItems(generateHelp("skills [player] <profile>", "Get the skills data of a player"));

		paginateBuilder.addItems(
			generateHelp("catacombs [player] <profile>", "Get the dungeons data of a player") +
			generateHelp("essence upgrade [item]", "Interactive message to find the essence amount to upgrade an item") +
			generateHelp("essence info [item]", "Get the amount of essence to upgrade an item for each level") +
			generateHelp("partyfinder [player] <profile>", "A party finder helper that shows a player's dungeon stats")
		);

		paginateBuilder.addItems(
			generateHelp("guild [player]", "Find what guild a player is in") +
			generateHelp("guild info [u:player]", "Get information and statistics about a player's guild") +
			generateHelp("guild info [g:guild name]", "Get information and statistics about a guild") +
			generateHelp("guild members [u:player]", "Get a list of all members in a player's guild") +
			generateHelp("guild experience [u:player]", "Get the experience leaderboard for a player's guild") +
			generateHelp("guild-requirements [name]", "Get the application requirements set for this server") +
			generateHelp(
				"g-lb [weight|skills|catacombs|slayer] [u:player]",
				"Get the weight, skills, catacombs, or slayer leaderboard for a player's guild"
			) +
			generateHelp(
				"g-kicker [u:IGN] [type:value] ...",
				"Get all player's who don't meet the provided requirements. The requirement name can be skills, slayer, catacombs, or weight. The requirement value must be an integer."
			)
		);

		paginateBuilder.addItems(
			generateHelp("auctions [player]", "Get a player's active (not claimed) auctions on all profiles") +
			generateHelp("auction uuid [UUID]", "Get an auction by it's UUID") +
			generateHelp("bin [item]", "Get the lowest bin of an item") +
			generateHelp("bazaar [item]", "Get bazaar prices of an item") +
			generateHelp("average [item]", "Get the average auction price of an item") +
			generateHelp("bids [player]", "Get a player's bids") +
			generateHelp("query [item]", "Query the auction house") +
			generateHelp("bits [item]", "Get the price of an item from the bits shop") +
			generateHelp("price [uuid]", "Calculate the price of an item on the auction house using the auction's UUID")
		);

		paginateBuilder.addItems(
			generateHelp("inventory [player] <profile>", "Get a player's inventory represented in emojis") +
			generateHelp("inventory [player] <profile> [slot:number]", "Get a player's inventory with lore") +
			generateHelp("armor [player] <profile>", "Get a player's equipped armor with lore") +
			generateHelp("enderchest [player] <profile>", "Get a player's ender chest represented in emojis") +
			generateHelp("talisman [player] <profile>", "Get a player's talisman bag represented in emojis") +
			generateHelp("talisman [player] <profile> [slot:number]", "Get a player's talisman bag with lore") +
			generateHelp("sacks [player] <profile>", "Get a player's sacks' content bag represented in a list") +
			generateHelp("wardrobe [player] <profile>", "Get a player's wardrobe armors represented in emojis") +
			generateHelp("wardrobe list [player] <profile>", "Get a player's wardrobe armors represented in a list")
		);

		paginateBuilder.addItems(
			generateHelp("roles claim <profile>", "Claim automatic Skyblock roles. The player must be linked") +
			generateHelp("bank [player] <profile>", "Get a player's bank and purse coins") +
			generateHelp("bank history [player] <profile>", "Get a player's bank transaction history") +
			generateHelp("networth [player] <profile>", "Get a player's networth") +
			generateHelp("networth [player] <profile> --verbose", "Get a player's networth with a detailed JSON of each item cost") +
			generateHelp("weight [player] <profile>", "Get a player's weight") +
			generateHelp(
				"weight calculate [skill avg] [slayer] [cata level] [avg dungeon class level]",
				"Calculate predicted weight using given stats (not 100% accurate)"
			) +
			generateHelp("hypixel [player]", "Get Hypixel information about a player") +
			generateHelp("hypixel parkour [player]", "Get fastest Hypixel lobby parkour for a player") +
			generateHelp("profiles [player]", "Get information about all of a player's profiles") +
			generateHelp("missing [player] <profile>", "Get a player's missing talismans")
		);

		paginateBuilder.addItems(
			(isAdmin ? generateHelp("event create", "Interactive message to create a Skyblock event") : "") +
			generateHelp("event current", "Get information about the current event") +
			generateHelp("event join", "Join the current event") +
			generateHelp("event leave", "Leave the current event") +
			generateHelp("event leaderboard", "Get the leaderboard for current event") +
			(isAdmin ? generateHelp("event end", "Force end the event") : "")
		);

		if (isAdmin) {
			paginateBuilder.addItems(
				generateHelp("settings", "Get the current settings for the bot") +
				generateHelp("setup", "A walk-through on how to setup the bot") +
				generateHelp("categories", "Get the id's of all categories in guild") +
				generateHelp("settings delete --confirm", "Delete the current server settings") +
				generateHelp("settings set hypixel_key [key]", "Set a Hypixel API key for this server") +
				generateHelp("settings delete hypixel_key", "Delete the set Hypixel API of this server")
			);

			paginateBuilder.addItems(
				generateHelp("settings verify", "Get the current verify settings for the bot") +
				generateHelp("settings verify [enable|disable]", "Enable or disable automatic verify") +
				generateHelp("settings" + " verify message [message]", "Message that users will see and react to in order to verify") +
				generateHelp(
					"settings verify role add [@role]",
					"Add a role that user will receive " + "upon being verified. Cannot be @everyone or a managed role"
				) +
				generateHelp("settings verify role remove [@role]", "Remove a verify role") +
				generateHelp("settings verify " + "channel [#channel]", "Channel where the message to react for verifying will sent") +
				generateHelp("settings verify nickname <prefix> [IGN] <postfix>", "The nickname template on verifying. Can be set to none.")
			);

			paginateBuilder.addItems(
				generateHelp("settings apply", "Get the current apply names for the bot") +
				generateHelp("settings apply create [name]", "Create a new automatic apply with name 'name'") +
				generateHelp("settings apply remove [name]", "Delete an automatic apply") +
				generateHelp("settings apply [name] [enable|disable]", "Enable or disable automatic apply") +
				generateHelp("settings" + " apply [name] message [message]", "Message that users will see and react to in order to apply") +
				generateHelp("settings apply [name] staff_role [@role]", "Role that will be pinged when a new application is submitted") +
				generateHelp("settings apply [name] " + "channel [#channel]", "Channel where the message to react for applying will sent") +
				generateHelp(
					"settings apply [name] prefix [prefix]",
					"Prefix that all new apply channels should start with (prefix-discordName)"
				) +
				generateHelp("settings apply [name] category " + "[category id]", "Category where new apply channels will be made") +
				generateHelp(
					"settings apply [name] staff_channel " + "[#channel]",
					"Channel where new applications will be sent to be reviewed by staff"
				) +
				generateHelp(
					"settings apply [name] waiting_channel [#channel]",
					"Channel where the players who were accepted or waitlisted will be sent"
				) +
				generateHelp("settings apply [name] accept_message [message]", "Message that will be sent if applicant is accepted") +
				generateHelp(
					"settings apply [name] waitlist_message [message]",
					"Message that will be sent if applicant is waitlisted. Can be set to none"
				) +
				generateHelp(
					"settings apply [name] ironman [true|false]",
					"Whether applicants must use an ironman profile. Default is false"
				) +
				generateHelp("settings apply [name] deny_message [message]", "Message that will be sent if applicant is denied") +
				generateHelp(
					"settings apply [name] reqs add <slayer:amount> <skills:amount> <catacombs:amount> <weight:amount>",
					"Add a requirement that applicant must meet. At least one of the requirement types must be set"
				) +
				generateHelp(
					"settings apply [name] reqs remove [number]",
					"Remove a requirement. Run `" + BOT_PREFIX + "settings apply [name]` to see the index for all current requirements"
				)
			);

			paginateBuilder.addItems(
				generateHelp("settings roles", "Get the current roles settings for the bot") +
				generateHelp("settings roles [enable|disable]", "Enable or disable automatic roles") +
				generateHelp("settings roles enable [roleName]", "Enable a specific automatic role (set to disable by default)") +
				generateHelp(
					"settings roles add [roleName] [value] [@role]",
					"Add a new level to a role with its corresponding discord role"
				) +
				generateHelp("settings roles remove [roleName] [value]", "Remove a role level for a role") +
				generateHelp("settings roles stackable [roleName] [true|false]", "Make a specific role stackable") +
				generateHelp("settings roles set [roleName] [@role]", "Set a one level role's role")
			);

			paginateBuilder.addItems(
				generateHelp("settings guild create [name]", "Create a new guild roles with name `name`") +
				generateHelp("settings guild [name] [enable|disable] role", "Enable or disable automatic guild role assigning") +
				generateHelp("settings guild [name] set [guild_name]", "Set the guild name") +
				generateHelp("settings guild [name] role [@role]", "Set the role to give guild member's") +
				generateHelp("settings guild [name] [enable|disable] rank", "Enable or disable automatic guild rank assigning") +
				generateHelp("settings guild [name] add [rank_name] [@role]", "Add an automatic guild rank") +
				generateHelp("settings guild [name] remove [rank_name]", "Remove an automatic guild rank")
			);
		}

		if (channel != null) {
			paginateBuilder.build().paginate(channel, startingPage);
		} else {
			paginateBuilder.build().paginate(hook, startingPage);
		}

		return null;
	}

	private static String generateHelp(String commandName, String desc) {
		return "`" + BOT_PREFIX + commandName + "`: " + desc + "\n";
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
		new Thread(
			() -> {
				logCommand(event.getGuild(), event.getAuthor(), event.getMessage().getContentRaw());
				String[] args = event.getMessage().getContentRaw().toLowerCase().split(" ", 2);

				EmbedBuilder eb = getHelp(args.length >= 2 ? args[1] : null, event.getMember(), event.getChannel(), null);
				if (eb != null) {
					event.getChannel().sendMessage(eb.build()).queue();
				}
			}
		)
			.start();
	}
}
