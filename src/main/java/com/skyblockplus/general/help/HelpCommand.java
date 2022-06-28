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

package com.skyblockplus.general.help;

import static com.skyblockplus.features.listeners.AutomaticGuild.getGuildPrefix;
import static com.skyblockplus.utils.Utils.*;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.skyblockplus.utils.command.CommandExecute;
import com.skyblockplus.utils.command.CustomPaginator;
import com.skyblockplus.utils.command.PaginatorEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.dv8tion.jda.api.EmbedBuilder;
import org.springframework.stereotype.Component;

@Component
public class HelpCommand extends Command {

	public static final List<HelpData> helpDataList = new ArrayList<>();
	public static final List<String> helpNameList = new ArrayList<>();
	private static final String[] pageTitles = {
		"Navigation",
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
		"Settings",
		"Jacob Events",
		"Event Notifications",
		"Verify Settings",
		"Guild | Roles & Ranks Settings",
		"Guild | Apply Settings",
		"Roles Settings",
	};

	public HelpCommand() {
		this.name = "help";
		this.aliases = new String[] { "commands" };
		this.cooldown = globalCooldown;
		this.botPermissions = defaultPerms();

		setHelpList();
		helpDataList.stream().map(this::commandToNames).forEach(helpNameList::addAll);
	}

	private List<String> commandToNames(HelpData command) {
		List<String> commands = new ArrayList<>();
		commands.add(command.getName());
		for (HelpData subcommand : command.getSubcommands()) {
			commands.addAll(commandToNames(subcommand));
		}
		return commands;
	}

	public static void setHelpList() {
		helpDataList.clear();
		helpDataList.addAll(
			Arrays.asList(
				// General
				new HelpData("help", "Show the help menu with all the commands.", "help")
					.addSecondData("Show the help menu for a certain command.", "help <command>")
					.addExamples("help", "help guild experience")
					.addAliases("commands")
					.setCategory("general"),
				new HelpData("information", "Get information about this bot.").addAliases("info", "about").setCategory("general"),
				new HelpData("invite", "Get the invite link and support server link for the bot.").setCategory("general"),
				new HelpData("link", "Link your Hypixel account to the bot.", "link <player>")
					.addAliases("verify")
					.addExamples("link CrypticPlasma")
					.setCategory("general"),
				new HelpData("unlink", "Unlink your account from the bot.").addAliases("unverify").setCategory("general"),
				// Slayer
				new HelpData("slayer", "Get the slayer data of a player.", "slayer [player] [profile]")
					.addAliases("slayers")
					.addPlayerExamples()
					.setCategory("slayer"),
				new HelpData(
					"calcslayer",
					"Calculate the number of slayer bosses needed to reach a certain level or xp amount. The type can be sven, rev, tara, or enderman.",
					"calcslayer <type> [player] [profile] [level:level] [xp:xp]"
				)
					.addExamples("calcslayer rev CrypticPlasma level:8", "calcslayer enderman CrypticPlasma xp:100000")
					.setCategory("slayer"),
				// Skills
				new HelpData("skills", "Get the skills data of a player.", "skills [player] [profile]")
					.addAliases("skill")
					.addPlayerExamples()
					.setCategory("skills"),
				new HelpData("hotm", "Get a player's heart of the mountain statistics.", "hotm [player] [profile]")
					.addPlayerExamples()
					.setCategory("skills"),
				new HelpData("forge", "Get a player's forge items and ending times", "forge [player] [profile]")
						.addPlayerExamples()
					.setCategory("skills"),
					new HelpData("crimson", "Get a player's crimson isle stats", "crimson [player] [profile]")
							.addPlayerExamples()
							.setCategory("skills"),
				// Dungeons
				new HelpData("dungeons", "Get the dungeons data of a player.", "dungeons [player] [profile]")
					.addAliases("cata", "catacombs")
					.addPlayerExamples()
					.setCategory("dungeons"),
				new HelpData("essence", "Main essence command.")
					.setCategory("dungeons")
					.addSubcommands(
						new HelpData("upgrade", "Interactive message to find the essence amount to upgrade an item.", "upgrade <item>")
							.addExamples("upgrade Hyperion"),
						new HelpData("information", "Get the amount of essence to upgrade an item for each level.", "information <item>")
							.addExamples("information Hyperion")
							.addAliases("info"),
						new HelpData("player", "Get a player's essence amounts and their essence shop upgrades.", "[player] [profile]")
							.addPlayerExamples()
					),
				new HelpData(
					"calcruns",
					"Calculate the number of runs needed to reach a certain catacombs level. The floor can be from F1 to F7 or M1 to M6.",
					"calcruns [player] [profile] <level:level> <floor:floor>"
				)
					.addAliases("runs")
					.setCategory("dungeons"),
				new HelpData(
					"calcdrops",
					"Calculate the drop rate and cost of all chests for a floor",
					"calcdrops <floor> [luck:1|2|3|4|5] [accessory:none|talisman|ring|artifact]"
				)
					.addAliases("drops")
					.setCategory("dungeons"),
				// Guild
				new HelpData("guild", "Main guild command")
					.addSecondData("Find what guild a player is in.", "guild <player>")
					.addSubcommands(
						new HelpData("information", "Get information and statistics about a player's guild.", "information [player]")
							.addSecondData("Get information and statistics about a guild.", "information <g:guild_name>")
							.addAliases("info")
							.addExamples("information CrypticPlasma", "information g:Skyblock_Forceful"),
						new HelpData("members", "Get a list of all members in a player's guild.", "members [player]")
							.addSecondData("Get a list of all members in a guild.", "members <g:guild_name>")
							.addExamples("members CrypticPlasma", "members g:Skyblock_Forceful"),
						new HelpData(
							"experience",
							"Get the experience leaderboard for a player's guild. Days can range from 1 to 7, default number of days is 7.",
							"experience [player] [days:days]"
						)
							.addAliases("exp")
							.addSecondData(
								"Get the experience leaderboard for a guild. Days can range from 1 to 7, default number of days is 7.",
								"experience <g:guild_name> [days:days]"
							)
							.addExamples("experience CrypticPlasma", "experience g:Skyblock Forceful days:4")
					)
					.addAliases("g")
					.setCategory("guild"),
				new HelpData(
					"guild-leaderboard",
					"Get a leaderboard for a player's guild. All types can be seen through autocomplete. The mode can be all, ironman, or stranded. A Hypixel API key must be set in settings set hypixel_key <key>.",
					"guild-leaderboard <type> [player] [mode:all|ironman|stranded]"
				)
					.addSecondData("Get a leaderboard for a guild.", "guild-leaderboard <type> <g:guild_name> [mode:all|ironman|stranded]")
					.addAliases("g-lb")
					.addExamples("guild-leaderboard weight CrypticPlasma", "guild-leaderboard sven CrypticPlasma mode:ironman")
					.setCategory("guild"),
				new HelpData(
					"guild-kicker",
					"Get all player's who don't meet the provided requirements. The requirement type can be skills, slayer, catacombs, or weight. The requirement value must be an integer. You can have up to 3 sets of requirements. Append the `--usekey` flag to force use the set Hypixel API key.",
					"guild-kicker <u:player> <[type:value ...]> ..."
				)
					.addAliases("g-kicker")
					.addExamples("guild-kicker u:CrypticPlasma [weight:4000 skills:40] [weight:4500]")
					.setCategory("guild"),
				new HelpData(
					"guild-ranks",
					"A customizable helper that will tell you who to promote or demote in your Hypixel guild. Please DM me or join the Skyblock Plus [Discord Server](" +
					DISCORD_SERVER_INVITE_LINK +
					") to set this up for your guild.",
					"guild-ranks <u:player> [mode:all|ironman|stranded]"
				)
					.addAliases("g-ranks", "g-rank")
					.addExamples("guild-ranks u:CrypticPlasma")
					.setCategory("guild"),
				new HelpData(
					"guild-statistics",
					"Get a guild's SkyBlock statistics of slayer, skills, catacombs, and weight. Requires a Hypixel API key to be set.",
					"guild-statistics [player] [g:guild_name]"
				)
					.addAliases("guild-stats", "g-stats")
					.addExamples("guild-stats u:CrypticPlasma")
					.setCategory("guild"),
				new HelpData(
					"check-guild-api",
					"Get which Skyblock APIs players have enabled or disabled for a guild. Requires a Hypixel API key to be set.",
					"check-guild-api [player]"
				)
					.addExamples("check-guild-api CrypticPlasma")
					.setCategory("guild"),
				// Auctions
				new HelpData(
					"auctions",
					"Get (and/or track) a player's unclaimed auctions on all profiles. Can be sorted ascending or descending. Can be filtered by sold or unsold. Add verbose flag to show estimated price of each auction.",
					"auctions [player] [sort:low|high] [filter:unsold|sold] [--verbose]"
				)
					.addAliases("auction", "ah")
					.addExamples("auctions CrypticPlasma", "auctions CrypticPlasma filter:sold --verbose")
					.setCategory("price"),
				new HelpData("viewauction", "Get information about an auction by it's UUID.", "viewauction <uuid>")
					.addAliases("viewah")
					.addExamples("auctions uuid 77df55d9c0084473b113265ef48fb396")
					.setCategory("price"),
				new HelpData("bin", "Get the lowest bin of an item.", "bin <item>")
					.addAliases("lbin")
					.addExamples("bin Necron Chestplate")
					.setCategory("price"),
				new HelpData("bazaar", "Get the bazaar prices of an item.", "bazaar <item>")
					.addAliases("bz")
					.addExamples("bazaar Booster Cookie")
					.setCategory("price"),
				new HelpData("average", "Get the average auction price of an item.", "average <item>")
					.addAliases("avg")
					.addExamples("average Necron's Handle")
					.setCategory("price"),
				new HelpData("bids", "Get a player's auction house bids", "bids [player].")
					.addExamples("bids CrypticPlasma")
					.setCategory("price"),
				new HelpData(
					"price",
					"Query the auction house for the lowest price of an item. Allows for more specific queries than bin or average command. Can be filtered by bin only, auction only, or both.",
					"price <item> [type:bin|auction|both]"
				)
					.addExamples("price Necron's Chestplate ✪✪✪✪✪", "price Withered Hyperion ✪✪✪ type:both")
					.setCategory("price"),
				new HelpData("bits", "Get the bits cost of an item from the bits shop.", "bits <item>")
					.addExamples("bits God Potion")
					.addAliases("bit")
					.setCategory("price"),
				new HelpData("coinsperbit", "Get the coins to bits ratio for items in the bits shop.", "coinsperbit <item>")
					.addAliases("cpb")
					.setCategory("price"),
				// Inventory
				new HelpData("inventory", "Get a player's inventory represented in emojis.", "inventory [player] [profile]")
					.addSecondData("Get a player's inventory with lore.", "inventory [player] [profile] <slot:number>")
					.addAliases("inv")
					.addExamples(
						"inventory CrypticPlasma",
						"inventory CrypticPlasma Zucchini",
						"inventory CrypticPlasma slot:1",
						"inventory CrypticPlasma Zucchini slot:1"
					)
					.setCategory("inventory"),
				new HelpData("armor", "Get a player's equipped armor with lore.", "armor [player] [profile]")
					.addPlayerExamples()
					.setCategory("inventory"),
				new HelpData("enderchest", "Get a player's ender chest represented in emojis.", "enderchest [player] [profile]")
					.addAliases("ec", "echest")
					.addPlayerExamples()
					.setCategory("inventory"),
				new HelpData("storage", "Get a player's storage represented in emojis.", "storage [player] [profile]")
					.addPlayerExamples()
					.setCategory("inventory"),
				new HelpData("talisman", "Get a player's talisman bag represented in emojis.", "talisman [player] [profile]")
					.addSecondData("Get a player's talisman bag with lore.", "talisman [player] [profile] <slot:number>")
					.addExamples(
						"talisman CrypticPlasma",
						"talisman CrypticPlasma Zucchini",
						"talisman CrypticPlasma slot:1",
						"talisman CrypticPlasma Zucchini slot:1"
					)
					.addAliases("talismans")
					.setCategory("inventory"),
				new HelpData(
					"sacks",
					"Get a player's sacks' content bag represented in a list. Sorted by descending price.",
					"sacks [player] [profile]"
				)
					.addPlayerExamples()
					.setCategory("inventory"),
				new HelpData("wardrobe", "Get a player's wardrobe armors represented in emojis.", "wardrobe [player] [profile]")
					.addSecondData("Get a player's wardrobe armors represented in a list.", "wardrobe list [player] [profile]")
					.addExamples(
						"wardrobe CrypticPlasma",
						"wardrobe CrypticPlasma Zucchini",
						"wardrobe list CrypticPlasma",
						"wardrobe list CrypticPlasma Zucchini"
					),
				new HelpData("pets", "Get a player's pets.", "pets [player] [profile]")
					.addPlayerExamples()
					.setCategory("inventory"),
				// Misc
				new HelpData("roles", "Main roles command.")
					.addAliases("role")
					.addSubcommands(
						new HelpData("claim", "Claim your automatic Skyblock roles. You must be linked to the bot.", "claim <profile>")
							.addExamples("claim", "claim Zucchini"),
						new HelpData("list", "List all enabled claimable roles for this server.")
					)
					.setCategory("miscellaneous"),
				new HelpData("coins", "Get a player's bank and purse coins.", "bank [player] [profile]")
					.addSecondData("Get a player's bank transaction history.", "bank history [player] [profile]")
					.addExamples(
						"coins CrypticPlasma",
						"coins CrypticPlasma Zucchini",
						"coins history CrypticPlasma",
						"coins history CrypticPlasma Zucchini"
					)
					.setCategory("miscellaneous"),
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
					)
					.setCategory("miscellaneous"),
				new HelpData(
					"weight",
					"Get a player's slayer, skills, dungeons, and total weight. Shows both senither and lily weight.",
					"weight [player] [profile]"
				)
					.addAliases("we")
					.addPlayerExamples()
					.setCategory("miscellaneous"),
				new HelpData(
					"calcweight",
					"Calculate predicted weight change for a reaching certain skill, slayer, or catacombs level/amount.",
					"weight calculate [player] [profile] <type:type> <amount:amount>"
				)
					.addExamples("weight calculate CrypticPlasma type:catacombs amount:43")
					.setCategory("miscellaneous"),
				new HelpData("hypixel", "Get Hypixel information about a player.", "hypixel [player]")
					.addExamples("hypixel CrypticPlasma")
					.setCategory("miscellaneous"),
				new HelpData("missing", "Get a player's missing talismans.", "missing [player] [profile]")
					.addPlayerExamples()
					.setCategory("miscellaneous"),
				new HelpData("check-api", "Get a player's enabled and/or disabled Skyblock APIs", "check-api [player] [profile]")
						.addPlayerExamples()
					.addAliases("api")
					.setCategory("miscellaneous"),
				new HelpData("profiles", "Get information about all of a player's profiles.", "missing [player] [profile]")
					.addPlayerExamples()
					.setCategory("miscellaneous"),
				new HelpData("cakes", "Get a player's active and inactive cake buffs.", "cakes [player] [profile]")
					.addPlayerExamples()
					.setCategory("miscellaneous"),
				new HelpData("harp", "Get a player's harp statistics.", "harp [player] [profile]")
					.addPlayerExamples()
					.setCategory("miscellaneous"),
				new HelpData("uuid", "Convert username to UUID or UUID to username.", "uuid [username|uuid]")
					.addExamples("uuid CrypticPlasma", "uuid 044903b7a9d3416d957f929557af6c88")
					.setCategory("miscellaneous"),
				new HelpData("fetchur", "Get the item that fetchur wants today.").setCategory("miscellaneous"),
				new HelpData("jacob", "Get the upcoming contests and their crops.").setCategory("miscellaneous"),
				new HelpData("mayor", "Get the current mayor and their perks.").setCategory("miscellaneous"),
				new HelpData(
					"leaderboard",
					"Get a global leaderboard. All types can be seen through autocomplete. The mode can be all, ironman, or stranded.",
					"leaderboard <type> [player] [mode:all|ironman|stranded] [page:page] [rank:rank] [amount:amount]"
				)
					.addAliases("lb")
					.setCategory("miscellaneous"),
				new HelpData("skyblock", "Get an overview of a player's Skyblock statistics", "skyblock [player] [profile]")
						.addPlayerExamples()
					.addAliases("sb")
					.setCategory("miscellaneous"),
				new HelpData("bingo", "Get the current bingo goals and a player's live bingo card.", "bingo [player]")
					.setCategory("miscellaneous"),
				new HelpData("recipe", "Get the crafting recipe of an item.", "recipe <item>").setCategory("miscellaneous"),
				new HelpData("calendar", "Get the current Skyblock datetime and running or upcoming events").setCategory("miscellaneous"),
				new HelpData(
					"scammer",
					"Check if a player is marked as a scammer in the SkyblockZ database with the reason, discord(s), and Minecraft alt(s)",
					"scammer [player]"
				)
					.addExamples("scammer CrypticPlasma")
					.setCategory("miscellaneous"),
				new HelpData(
					"calcdrags",
					"Calculate your loot quality and loot from dragons in the end. The ratio is your damage divided by first place damage.",
					"calcdrags [position:position] [eyes:eyes] [ratio:ratio]"
				)
					.addAliases("drags")
					.setCategory("miscellaneous"),
				new HelpData("reforge", "Get the reforge stone stats for each rarity.", "reforge <stone>").setCategory("miscellaneous"),
				// Party
				new HelpData("party", "Main party command.")
					.addSubcommands(
						new HelpData("create", "Interactive message to create a new party."),
						new HelpData("current", "Get information about your current party."),
						new HelpData("join", "Join a player's party where username is the party leader's IGN.", "join <username>")
							.addExamples("join CrypticPlasma"),
						new HelpData("leave", "Leave your current party."),
						new HelpData("list", "List all active parties"),
						new HelpData("disband", "Disband your current party."),
						new HelpData("kick", "Kick a member from your party", "kick <username>").addExamples("kick CrypticPlasma")
					)
					.setCategory("party"),
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
					)
					.setCategory("event"),
				// Settings
				new HelpData("settings", "Main settings command.")
					.setCategory("settings")
					.addAliases("config", "configuration")
					.addSecondData("View the current settings for the Discord server.", "settings")
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
									"Set a Hypixel API key for this server. Once set, this cannot be viewed for the privacy of the key owner.",
									"hypixel_key <key>"
								),
								new HelpData(
									"prefix",
									"Set the prefix of the bot. Must be a least one character and no more than five.",
									"prefix <prefix>"
								),
								new HelpData(
									"fetchur_channel",
									"Set the channel where fetchur notifications will be posted at 12 am EST every day.",
									"fetchur_channel <#channel>"
								),
								new HelpData(
									"fetchur_ping",
									"Set the role that will be pinged with the daily fetchur notifications.",
									"fetchur_ping <@role>"
								),
								new HelpData(
									"mayor_channel",
									"Set the channel where notifications will be sent when a new mayor is elected or perks rotate.",
									"mayor_channel <#channel>"
								),
								new HelpData(
									"mayor_ping",
									"Set the role that will be pinged with mayor notifications.",
									"mayor_ping <@role>"
								),
								new HelpData(
									"guest_role",
									"Set the role that will be given to linked users that are not in any of the setup automatic guilds. Requires at least one automatic guild to be setup.",
									"guest_role <@role>"
								)
							),
						new HelpData("bot_manager", "Manage bot manager roles.")
							.addSubcommands(
								new HelpData(
									"add",
									"Add a bot manager role. This allows members with this role to use features that are admin only.",
									"add <@role>"
								),
								new HelpData("remove", "Remove a bot manager role.", "remove <@role>")
							),
						new HelpData("channel_blacklist", "Blacklisted command channels.")
							.addSubcommands(
								new HelpData("add", "Blacklist a channel from be able to run bot commands in it.", "add <#channel>"),
								new HelpData("remove", "Unblacklist a channel", "remove <#channel>")
							),
						new HelpData("blacklist", "View or manage the application blacklist for this server.")
							.addSecondData(
								"Get a list of all the blacklisted players on this server with the reasons and NameMC links.",
								"blacklist"
							)
							.addSubcommands(
								new HelpData(
									"add",
									"Add a player to the blacklist. Reason will default to 'not provided' if not set.",
									"add <player> [reason]"
								),
								new HelpData("remove", "Remove a player from the blacklist.", "remove <player>"),
								new HelpData(
									"search",
									"Search for a player in the blacklist. Will show top five closest results.",
									"search <player>"
								),
								new HelpData("share", "Share your blacklist with another server.", "share <server_id>"),
								new HelpData("unshare", "Stop sharing your blacklist with another server.", "unshare <server_id>"),
								new HelpData("use", "Use a shared blacklist from another server.", "use <server_id>"),
								new HelpData("stop_using", "Stop using a shared blacklist from another server.", "stop_using <server_id>")
							),
						new HelpData("jacob", "Main command for jacob event settings.")
							.addSecondData("Get the current farming event settings for the bot.", "jacob")
							.addSubcommands(
								new HelpData("enable", "Enable farming event notifications."),
								new HelpData("disable", "Disable farming event notifications."),
								new HelpData(
									"channel",
									"Set the channel where farming event notifications will be posted.",
									"channel <#channel>"
								),
								new HelpData(
									"add",
									"Add a crop to be tracked and notified. The role will automatically be created.",
									"add <crop|all>"
								),
								new HelpData("remove", "Remove a crop from the tracking list.", "remove <crop>")
							),
						new HelpData("event", "Main command for event notification settings.")
							.addSecondData("Get the current event notification settings for the bot.", "event")
							.addSubcommands(
								new HelpData("enable", "Enable event notifications."),
								new HelpData("disable", "Disable event notifications."),
								new HelpData("channel", "Set the channel where event notifications will be posted.", "channel <#channel>"),
								new HelpData("add", "Add an event to be tracked and notified.", "add <event|all> [@role]"),
								new HelpData("remove", "Remove an event from the tracking list.", "remove <event>")
							),
						new HelpData("verify", "Main command for verification settings.")
							.addSecondData("Get the current verification settings for the bot.", "verify")
							.addSubcommands(
								new HelpData("enable", "Enable automatic verify."),
								new HelpData("disable", "Disable automatic verify."),
								new HelpData("message", "The message that users will see when verifying.", "message <message>")
									.addExamples("message Run /link <IGN> replacing IGN with your IGN to verify!"),
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
									"The nickname template that a user will be renamed to on verifying. Can be set to none. Add .{text} for text that should only be added if the previous template was met. See example [here](https://discord.com/channels/796790757947867156/802749920679165994/937099157200580618)\n\nYou can use the following templates:\n• [GUILD.NAME]\n• [GUILD.TAG]\n• [GUILD.RANK]\n• [PLAYER.SKILLS]\n• [PLAYER.CATACOMBS]\n• [PLAYER.SLAYER]\n• [PLAYER.WEIGHT]\n• [PLAYER.CLASS]",
									"nickname [prefix] [IGN] [postfix]"
								)
									.addExamples("nickname Verified | [IGN]", "nickname [[GUILD.RANK]] [IGN]"),
								new HelpData(
									"remove_role",
									"Set the role that will be removed on verifying and re-added when un-verifying.",
									"remove_role <@role>"
								),
								new HelpData("sync", "Enable or disable verify automatic sync")
									.addSubcommands(
										new HelpData(
											"enable",
											"Enable verification automatic sync. This will sync the verified role(s) and nickname when a user joins the server or every 3 hours."
										),
										new HelpData("disable", "Disable verification automatic sync")
									),
									new HelpData("dm_on_sync", "Enable or disable verify DM on join automatic sync")
											.addSubcommands(
													new HelpData(
															"enable",
															"Enable verification automatic DM on join sync. This will toggle if a user should be DMd when their roles are synced on joining the server."
													),
													new HelpData("disable", "Disable verification automatic DM on join sync")
											),
								new HelpData("roles_claim", "Enable or disable automatic roles sync")
									.addSubcommands(
										new HelpData(
											"enable",
											"Enable automatic roles sync. This is the same as running the roles claim command on joining/linking/verifying"
										),
										new HelpData("disable", "Disable automatic roles sync")
									)
							),
						new HelpData("roles", "Main command for automatic roles settings.")
							.addSecondData("Get the current roles settings for the bot.", "roles")
							.addSubcommands(
								new HelpData("enable", "Enable automatic roles.")
									.addSecondData("Enable a specific automatic role (disabled by default).", "enable <role_name>")
									.addExamples("enable", "enable dungeon_secrets"),
								new HelpData("disable", "Disable automatic roles.")
									.addSecondData("Disable a specific automatic role.", "disable <role_name>")
									.addExamples("disable", "disable dungeon_secrets"),
								new HelpData(
									"add",
									"Add a new level to a role with its corresponding Discord role.",
									"add <role_name> <value> <@role>"
								)
									.addExamples("add sven 400000 @sven 8", "add alchemy 50 @alchemy 50"),
								new HelpData("remove", "Remove a role level for a role.", "remove <role_name> <value>")
									.addExamples("remove sven 400000", "remove alchemy 50"),
								new HelpData("set", "Set the Discord role for a one level role.", "set <role_name> <@role>")
									.addExamples("set ironman @ironman player", "set pet_enthusiast @pet lover"),
								new HelpData(
									"use_highest",
									"Enable or disable using the highest values across all profile. Default is false",
									"use_highest <enable|disable>"
								)
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
										new HelpData("role", "Set the role to give guild members.", "<@role>")
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
										new HelpData("enable", "Enable automatic apply.", "settings guild <name> apply enable", true),
										new HelpData("disable", "Enable automatic disable.", "settings guild <name> apply disable", true),
										new HelpData(
											"message",
											"The message that users will see when verifying.",
											"settings guild <name> apply message <message>",
											true
										),
										new HelpData(
											"staff_roles",
											"Modify roles that will be pinged for an application",
											"settings guild <name> apply staff_roles",
											true
										)
											.addSubcommands(
												new HelpData(
													"add",
													"Add a role which will be pinged when a new application is submitted.",
													"settings guild <name> apply staff_roles add <@role>",
													true
												),
												new HelpData(
													"remove",
													"Remove a staff ping role.",
													"settings guild <name> apply staff_roles remove <@role>",
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
											"check_api",
											"Whether an applicant must have all APIs enabled in order to apply.",
											"settings guild <name> apply check_api <enable|disable>",
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
											"gamemode",
											"Whether applicants must use a certain gamemode profile in their application. Defaults to all. Options are: all, regular, ironman, stranded, or ironman_stranded.",
											"settings guild <name> apply gamemode <true|false>",
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
											),
										new HelpData(
											"scammer_check",
											"Enable or disable if a player should automatically be denied if they are marked as a scammer in the SkyblockZ database. Defaults to false.",
											"settings guild <name> apply scammer_check <enable|disable>",
											true
										),
										new HelpData(
											"log_channel",
											"Set the channel where application will be logged to once complete. Can be set to none to disable this feature.",
											"settings guild <name> apply log_channel <#channel>",
											true
										)
									)
							)
					),
				new HelpData("setup", "A short walk-through on how to setup the bot.").setCategory("settings"),
				new HelpData("categories", "Get the id's of all categories in the Discord server.").setCategory("settings"),
				new HelpData(
					"reload",
					"Reload the automatic guild application(s) and automatic verification settings. This for changes to take effect for both of these features."
				)
					.setCategory("settings")
			)
		);
	}

	public static EmbedBuilder getHelp(String pageStr, PaginatorEvent event) {
		int startingPage = 0;
		if (pageStr != null) {
			String[] pageStrSplit = pageStr.split("\\s+", 2);

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

		CustomPaginator.Builder paginateBuilder = event.getPaginator().updateExtras(extra -> extra.setTitles(pageTitles));

		paginateBuilder.addItems(
			"Use the arrow buttons to navigate through the pages" + generatePageMap() + "\n\n<> = required [] = optional"
		);

		HelpGenerator help = new HelpGenerator(getGuildPrefix(event.getGuild().getId()));
		paginateBuilder.addItems(
			help.create("help", "Show the help menu with all the commands") +
			help.create("help <command>", "Show the help menu for a certain command") +
			help.create("information", "Show information and statistics about the bot") +
			help.create("invite", "Get the invite link and Discord link for the bot") +
			help.create("link <player>", "Link your Hypixel account to the bot") +
			help.create("unlink", "Unlink your account from the bot")
		);

		paginateBuilder.addItems(
			help.create("slayer [player] [profile]", "Get the slayer data of a player") +
			help.create(
				"calcslayer <type> [player] [profile] [level:level] [xp:xp]",
				"Calculate the number of bosses needed to reach a level or xp amount"
			)
		);

		paginateBuilder.addItems(
			help.create("skills [player] [profile]", "Get the skills data of a player") +
			help.create("hotm [player] [profile]", "Get a player's heart of the mountain statistics") +
			help.create("forge [player] [profile]", "Get a player's forge items & ending times") +
			help.create("crimson [player] [profile]", "Get the crimson isle stats of a player")
		);

		paginateBuilder.addItems(
			help.create("dungeons [player] [profile]", "Get the dungeons data of a player") +
			help.create("essence upgrade <item>", "Interactive message to find the essence amount to upgrade an item") +
			help.create("essence information <item>", "Get the amount of essence to upgrade an item for each level") +
			help.create("essence [player] [profile]", "Get a player's essence amounts and essence shop upgrades") +
			help.create(
				"calcruns [player] [profile] <level:level> <floor:floor>",
				"Calculate the number of runs needed to reach a certain catacombs level"
			) +
			help.create(
				"calcdrops <floor> [luck:boss_luck] [accessory:accessory]",
				"Calculate the drop rate and cost of all chests for a floor"
			)
		);

		paginateBuilder.addItems(
			help.create("guild <player>", "Get information and statistics about a player's guild") +
			help.create("guild <g:guild_name>", "Get information and statistics about a guild") +
			help.create("guild members <player>", "Get a list of all members in a player's guild") +
			help.create("guild members <g:guild_name>", "Get a list of all members in a guild") +
			help.create("guild experience <player>", "Get the experience leaderboard for a player's guild") +
			help.create("guild experience <g:guild_name>", "Get the experience leaderboard for a guild") +
			help.create("g-lb <type> [player] [g:guild_name] [mode:normal|ironman|stranded]", "Get a leaderboard for a player's guild") +
			help.create(
				"g-kicker <u:player> <type:value> ...",
				"Get all player's who don't meet the provided requirements. The requirement name can be skills, slayer, catacombs, or weight. The requirement value must be an integer"
			) +
			help.create(
				"g-ranks <u:player> [mode:normal|ironman|stranded]",
				"A customizable helper that will tell you who to promote or demote in your Hypixel guild"
			) +
			help.create("g-stats [player] [g:guild_name]", "Get a guild's SkyBlock statistics of slayer, skills, catacombs, and weight") +
			help.create("check-guild-api <player>", "Get which Skyblock APIs players have enabled or disabled for a guild")
		);

		paginateBuilder.addItems(
			help.create(
				"auctions [player] [sort:low|high] [filter:unsold|sold] [--verbose]",
				"Get a player's unclaimed auctions on all profiles"
			) +
			help.create("viewauction <UUID>", "Get an auction by its UUID") +
			help.create("bin <item>", "Get the lowest bin of an item") +
			help.create("bazaar <item]", "Get bazaar prices of an item") +
			help.create("average <item>", "Get the average auction price of an item") +
			help.create("bids [player]", "Get a player's auction house bids") +
			help.create("price <item> [type:bin|auction|both]", "Query the auction house for the lowest bin of an item") +
			help.create("bits <item>", "Get the bits cost of an item from the bits shop") +
			help.create("coinsperbit <item>", "Get the coins to bits ratio for items in the bits shop")
		);

		paginateBuilder.addItems(
			help.create("inventory [player] [profile]", "Get a player's inventory represented in emojis") +
			help.create("inventory [player] [profile] <slot:number>", "Get a player's inventory with lore") +
			help.create("armor list [player] [profile]", "Get a player's equipped armor & equipment with lore") +
			help.create("armor [player] [profile]", "Get a player's equipped armor & equipment represented in emojis") +
			help.create("enderchest [player] <profile]", "Get a player's ender chest represented in emojis") +
			help.create("storage [player] <profile]", "Get a player's storage represented in emojis") +
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
			help.create("coins [player] [profile]", "Get a player's bank and purse coins") +
			help.create("coins history [player] [profile]", "Get a player's bank transaction history") +
			help.create("networth [player] [profile]", "Calculate a player's networth") +
			help.create("networth [player] [profile] --verbose", "Calculate a player's networth with a detailed JSON of each item cost") +
			help.create("weight [player] [profile]", "Get a player's slayer, skills, dungeons, and total weight") +
			help.create(
				"calcweight [player] [profile] <type:type> <amount:amount>",
				"Calculate predicted weight change for a reaching certain skill, slayer, or catacombs level/amount"
			) +
			help.create("hypixel [player]", "Get Hypixel information about a player") +
			help.create("profiles [player]", "Get information about all of a player's profiles") +
			help.create("missing [player] [profile]", "Get a player's missing talismans") +
			help.create("fetchur", "Get the item that fetchur wants today") +
			help.create("cakes [player] [profile]", "Get a player's inactive and active cake buffs") +
			help.create("harp [player] [profile]", "Get a player's harp statistics") +
			help.create("uuid [username|uuid]", "Convert username to UUID or UUID to username") +
			help.create("calendar", "Get the current Skyblock datetime and running or upcoming events") +
			help.create("scammer [player]", "Check if a player is marked as a scamer in the SBZ database") +
			help.create("jacob [crop]", "Get the upcoming contests and their crops") +
			help.create("mayor", "Get information about the current mayor or the running election") +
			help.create("bingo [player]", "Get the current bingo goals and a player's bingo card") +
			help.create("leaderboard <type> [player] [page:page] [rank:rank] [amount:amount]", "Get a global leaderboard") +
			help.create("skyblock [player] [profile]", "Get an overview of a player's Skyblock statistics") +
			help.create("recipe <item>", "Get the crafting recipe of an item") +
			help.create("check-api [player]", "Check which Skyblock APIs a player has enabled or disabled") +
			help.create(
				"calcdrags [eyes:eyes] [position:position] [ratio:ratio]",
				"Calculate loot quality and loot from dragons in the end"
			) +
			help.create("reforgestone <stone>", "Get the reforge stone stats for each rarity") +
			help.create("collections [player] [profile]", "Get a player's collection counts")
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
			help.create("event create", "Interactive message to create a Skyblock event") +
			help.create("event current", "Get information about the current event") +
			help.create("event join [profile]", "Join the current event") +
			help.create("event leave", "Leave the current event") +
			help.create("event leaderboard", "Get the leaderboard for current event") +
			help.create("event end", "Force end the event") +
			help.create("event cancel", "Cancel the event. No announcement will be made")
		);

		paginateBuilder.addItems(
			help.create("settings", "View the current settings for the Discord server") +
			help.create("settings general", "View the bot's general settings for this server") +
			help.create("setup", "A short walk-through on how to setup the bot") +
			help.create("categories", "Get the name and id of all categories in this server") +
			help.create("reload", "Reload the guild application and verification settings") +
			help.create("settings set hypixel_key <key>", "Set a Hypixel API key for this server") +
			help.create("settings set prefix <prefix>", "Set the prefix of the bot") +
			help.create(
				"settings set fetchur_channel <#channel>",
				"Set the channel where fetchur item notifications will be sent every day at 12 am EST"
			) +
			help.create("settings set fetchur_ping <@role>", "Role that will be pinged when the fetchur item notifications is sent") +
			help.create(
				"settings set mayor_channel <#channel>",
				"Set the channel where notifications will be sent when a new mayor is elected"
			) +
			help.create("settings set mayor_ping <@role>", "Role that will be pinged when mayor notifications are sent") +
			help.create("settings set guest_role <@role>", "Set the guest role") +
			help.create("settings set log_channel <#channel>", "Set the action log channel") +
			help.create("settings bot_manager add <@role>", "Add a bot manager role") +
			help.create("settings bot_manager remove <@role>", "Remove a bot manager role") +
			help.create("settings delete hypixel_key", "Delete the set Hypixel API key of this server") +
			help.create("settings delete all", "Delete the current server settings") +
			help.create("settings channel_blacklist add <#channel>", "Blacklist a channel from running bot commands") +
			help.create("settings channel_blacklist remove <#channel>", "Unblacklist a channel from running bot commands") +
			help.create("fix-application <#channel> <state>", "Fix an application") +
			help.create("settings blacklist", "List all players that are blacklisted") +
			help.create("settings blacklist add <player> [reason]", "Blacklist a player from using the application system on this server") +
			help.create("settings blacklist remove <player>", "Remove a player from the blacklist") +
			help.create("settings blacklist search <player>", "Remove a player from the blacklist") +
			help.create("settings blacklist share <server_id>", "Share your blacklist with another server") +
			help.create("settings blacklist unshare <server_id>", "Stop sharing your blacklist with another server") +
			help.create("settings blacklist use <server_id>", "Use a shared blacklist from another server") +
			help.create("settings blacklist stop_using <server_id>", "Stop using a shared blacklist from another server")
		);

		paginateBuilder.addItems(
			help.create("settings jacob", "View the current settings for farming event notifications") +
			help.create("settings jacob <enable|disable>", "Enable or disable farming event notifications") +
			help.create("settings jacob channel <#channel>", "Set the channel where farming event notifications will be sent") +
			help.create("settings jacob add <crop|all> [@role]", "Added a crop to be tracked. Role will automatically be created") +
			help.create("settings jacob remove <crop>", "Remove a crop from the tracking list")
		);

		paginateBuilder.addItems(
			help.create("settings event", "View the current settings for event notifications") +
			help.create("settings event <enable|disable>", "Enable or disable event notifications") +
			help.create("settings event channel <#channel>", "Set the channel where event notifications will be sent") +
			help.create("settings event add <event|all> [@role]", "Added an event to be notified for") +
			help.create("settings event remove <event>", "Remove an event from the notification list")
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
			help.create("settings verify nickname [prefix] [IGN] [postfix]", "The nickname template on verifying. Can be set to none") +
			help.create("settings verify remove_role <@role>", "Role that will be removed on verifying and re-added when un-verifying") +
			help.create("settings verify sync <enable|disable>", "Enable or disable automatic verify role and nickname syncing") +
			help.create("settings verify dm_on_sync <enable|disable>", "Enable or disable DMing the user on syncing") +
			help.create("settings verify roles_claim <enable|disable>", "Enable or disable automatic role syncing")
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
				"settings guild <name> apply staff_roles add <@role>",
				"Add a role that will be pinged when a new application is submitted"
			) +
			help.create("settings guild <name> apply staff_roles remove <@role>", "Remove a staff ping role") +
			help.create("settings guild <name> apply channel <#channel>", "Channel where the message to click for applying will sent") +
			help.create("settings guild <name> apply category <category>", "Category where new apply channels will be made") +
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
				"settings guild <name> apply gamemode <true|false>",
				"Whether applicants must use a certain gamemode profile in their application. Defaults to 'all'"
			) +
			help.create("settings guild <name> apply deny_message <message>", "Message that will be sent if applicant is denied") +
			help.create(
				"settings guild <name> apply requirements add [slayer:amount] [skills:amount] [catacombs:amount] [weight:amount]",
				"Add a requirement that applicant must meet. At least one of the requirement types must be set. Can be empty"
			) +
			help.create(
				"settings guild <name> apply requirements remove <index>",
				"Remove a requirement. Run `settings guild <name>` to see the index for all current requirements"
			) +
			help.create(
				"settings guild <name> apply scammer_check <enable|disable>",
				"Whether the applicant should be automatically be denied if marked a scammer in the SkyblockZ database"
			) +
			help.create(
				"settings guild <name> apply log_channel <#channel>",
				"Channel where application logs will sent to. Can be set to none"
			)
		);

		paginateBuilder.addItems(
			help.create("settings roles", "Get the current roles settings for the bot") +
			help.create("settings roles <enable|disable>", "Enable or disable automatic roles") +
			help.create(
				"settings roles use_highest <enable|disable>",
				"Enable or disable using the highest values or last played on profile. Default is false"
			) +
			help.create(
				"settings roles <enable|disable> <role_name|all>",
				"Enable or disable a specific automatic role or enable or disable all applicable automatic roles"
			) +
			help.create("settings roles add <role_name> <value> <@role>", "Add a new level to a role with its corresponding Discord role") +
			help.create("settings roles remove <role_name> <value>", "Remove a role level for a role") +
			help.create("settings roles set <role_name> <@role>", "Set a one level role's role")
		);

		event.paginate(paginateBuilder, startingPage);
		return null;
	}

	private static String generatePageMap() {
		StringBuilder generatedStr = new StringBuilder();
		for (int i = 1; i < pageTitles.length; i++) {
			generatedStr.append("\n• **Page ").append(i + 1).append(":** ").append(pageTitles[i]);
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

				paginate(getHelp(args.length >= 2 ? args[1].toLowerCase() : null, getPaginatorEvent()));
			}
		}
			.queue();
	}

	record HelpGenerator(String prefix) {
		public String create(String commandName, String desc) {
			return "`" + prefix + commandName + "`: " + desc + "\n";
		}
	}
}
