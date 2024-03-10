/*
 * Skyblock Plus - A Skyblock focused Discord bot with many commands and customizable features to improve the experience of Skyblock players and guild staff!
 * Copyright (c) 2021-2024 kr45732
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

import static com.skyblockplus.utils.utils.Utils.*;

import com.skyblockplus.utils.command.CustomPaginator;
import com.skyblockplus.utils.command.SlashCommand;
import com.skyblockplus.utils.command.SlashCommandEvent;
import com.skyblockplus.utils.structs.AutoCompleteEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import org.apache.groovy.util.Maps;
import org.springframework.stereotype.Component;

@Component
public class HelpSlashCommand extends SlashCommand {

	public static final List<HelpData> helpDataList = new ArrayList<>();
	private static final List<String> helpNameList = new ArrayList<>();
	private static final Map<String, String> pageTitleToCategory = Maps.of(
		"Navigation",
		"",
		"General",
		"?category=General",
		"Slayer",
		"?category=Slayer",
		"Skills",
		"?category=Skills",
		"Dungeons",
		"?category=Dungeons",
		"Guild",
		"?category=Guild",
		"Price",
		"?category=Price",
		"Inventory",
		"?category=Inventory",
		"Miscellaneous",
		"?category=Miscellaneous",
		"Party",
		"?category=Party",
		"Skyblock Event",
		"?category=Event",
		"Settings",
		"?category=Settings",
		"Jacob Events",
		"?category=Settings",
		"Event Notifications",
		"?category=Settings",
		"Verify Settings",
		"?category=Settings",
		"Guild | Roles & Ranks Settings",
		"?category=Settings",
		"Guild | Apply Settings",
		"?category=Settings",
		"Roles Settings",
		"?category=Settings"
	);

	public HelpSlashCommand() {
		this.name = "help";

		setHelpList();
		helpDataList.stream().map(this::commandToNames).forEach(helpNameList::addAll);
	}

	public static void setHelpList() {
		helpDataList.clear();
		helpDataList.addAll(
			List.of(
				// General
				new HelpData("help", "Show the help menu with all commands or a specific command.", "help [command]")
					.addExamples("help", "help settings guild create")
					.setCategory("general"),
				new HelpData("information", "Get information about the bot.").setCategory("general"),
				new HelpData("invite", "Get the invite link and support server link for the bot.").setCategory("general"),
				new HelpData("link", "Link your Hypixel account to the bot.", "link <player>")
					.addExamples("link CrypticPlasma")
					.setCategory("general"),
				new HelpData("unlink", "Unlink your account from the bot.").setCategory("general"),
				new HelpData("vote", "Get links to upvote the bot.").setCategory("general"),
				// Slayer
				new HelpData("slayer", "Get a player's slayer data.", "slayer [player] [profile]").setCategory("slayer"),
				new HelpData(
					"calcslayer",
					"Calculate the number of slayer bosses needed to reach a certain level or xp amount.",
					"calcslayer <type> [player] [profile] [level] [xp] [system]"
				)
					.setCategory("slayer"),
				// Skills
				new HelpData("skills", "Get a player's skill data.", "skills [player] [profile]").setCategory("skills"),
				new HelpData("hotm", "Get a player's heart of the mountain statistics.", "hotm [player] [profile]").setCategory("skills"),
				new HelpData("forge", "Get a player's forge items and ending times.", "forge [player] [profile]").setCategory("skills"),
				new HelpData("crimson", "Get a player's crimson isle statistics.", "crimson [player] [profile]").setCategory("skills"),
				// Dungeons
				new HelpData("dungeons", "Get a player's dungeon data.", "dungeons [player] [profile]").setCategory("dungeons"),
				new HelpData("essence", "Main essence command.")
					.addSubcommands(
						new HelpData(
							"upgrade",
							"Interactive message to calculate the amount of essence and items to upgrade an item.",
							"upgrade <item>"
						)
							.addExamples("upgrade Hyperion"),
						new HelpData(
							"information",
							"Get the amount of essence and items to upgrade an item for each level.",
							"information <item>"
						)
							.addExamples("information Hyperion"),
						new HelpData("player", "Get a player's essence amounts and essence shop upgrades.", "player [player] [profile]")
					)
					.setCategory("dungeons"),
				new HelpData(
					"calcruns",
					"Calculate the number of runs needed to reach a certain catacombs level.",
					"calcruns <level> <floor> [player] [profile] [system]"
				)
					.setCategory("dungeons"),
				new HelpData(
					"calcdrops",
					"Calculate the drop rate and cost of all chests for a dungeons floor or for an item.",
					"calcdrops <floor> [item] [luck] [accessory]"
				)
					.setCategory("dungeons"),
				// Guild
				new HelpData("guild", "Main guild command")
					.addSubcommands(
						new HelpData("information", "Get information and statistics about a guild.", "information [player] [guild]"),
						new HelpData("members", "List all members in a guild.", "members [player] [guild]"),
						new HelpData(
							"experience",
							"Get the experience leaderboard for a guild. Defaults to seven days.",
							"experience [player] [guild] [days]"
						),
						new HelpData("leaderboard", "Get a leaderboard for a guild.", "leaderboard <type> [player] [guild] [gamemode]"),
						new HelpData(
							"kicker",
							"Get all player's who don't meet the provided requirements." +
							" Requirements are in the format `[type:value ...] ...`. The" +
							" requirement type can be skills, slayer, catacombs, weight, or" +
							" level. The requirement value must be an integer. Requirements" +
							" inside the brackets require at least one to be met, while" +
							" outside require all to be met.",
							"kicker <requirements...> [player] [gamemode]"
						)
							.addExamples("kicker [level:300] [skills:40]", "kicker [level:200 slayer:2000000] [level:300]"),
						new HelpData(
							"top",
							"Get a guild's average and top 5 leaderboards of slayer, skills, catacombs, weight, networth, and level.",
							"top [player] [guild] [gamemode]"
						),
						new HelpData(
							"check",
							"A customizable helper that will tell you who to kick, promote or demote in" +
							" your Hypixel guild based on numerical requirements. Please join the" +
							" [Skyblock Plus Discord](" +
							DISCORD_SERVER_INVITE_LINK +
							") and mention CrypticPlasma to setup this for your guild.",
							"check <player> [gamemode]"
						)
					)
					.setCategory("guild"),
				new HelpData("guildlb", "Get a global guild leaderboard.", "guildlb <type> [guild] [mode] [comparison]")
					.setCategory("guild"),
				// Auctions
				new HelpData(
					"auctions",
					"Get and/or track a player's unclaimed auctions on all profiles. Set verbose to true to show an estimated price breakdown for each auction.",
					"auctions [player] [filter] [sort] [verbose]"
				)
					.setCategory("price"),
				new HelpData("viewauction", "Get information about an auction by it's UUID.", "viewauction <uuid>")
					.addExamples("viewauction 77df55d9c0084473b113265ef48fb396")
					.setCategory("price"),
				new HelpData("bin", "Get the lowest bin of an item.", "bin <item>")
					.addExamples("bin Wither Chestplate")
					.setCategory("price"),
				new HelpData(
					"attributes",
					"Get the lowest priced attribute combination for an item.",
					"attributes <item> <attribute_one> <attribute_two>"
				)
					.addExamples("bin Aurora Boots Mana Pool Mana Regeneration")
					.setCategory("price"),
				new HelpData("bazaar", "Get the bazaar prices of an item.", "bazaar <item>")
					.addExamples("bazaar Booster Cookie")
					.setCategory("price"),
				new HelpData("average", "Get the average auction and bin price of an item.", "average <item>")
					.addExamples("average Necron's Handle")
					.setCategory("price"),
				new HelpData("bids", "Get a player's auction house bids", "bids [player]").setCategory("price"),
				new HelpData(
					"price",
					"Query the auction house for the lowest prices of an item. Allows for more" +
					" specific queries than bin or average command. Defaults to searching" +
					" auctions and bins.",
					"price <item> [type]"
				)
					.addExamples("price Necron's Chestplate ✪✪✪✪✪")
					.setCategory("price"),
				new HelpData("bits", "Get the coins to bits ratio for items in the bits shop.").setCategory("price"),
				new HelpData("copper", "Get the coins to copper ratio for items in the SkyMart shop.").setCategory("price"),
				new HelpData("flips", "Get current auction flips (**experimental**).").setCategory("price"),
				// Inventory
				new HelpData("inventory", "Main inventory command.")
					.addSubcommands(
						new HelpData("emoji", "Get a player's inventory represented in emojis.", "emoji [player] [profile]"),
						new HelpData("list", "Get a player's inventory with lore.", "list [player] [profile] [slot]")
					)
					.setCategory("inventory"),
				new HelpData("museum", "Main museum command.")
					.addSubcommands(
						new HelpData("view", "View a player's museum items.", "view [player] [profile]"),
						new HelpData("cheapest", "Get the cheapest items to donate to a player's museum.", "cheapest [player] [profile]")
					)
					.setCategory("inventory"),
				new HelpData("armor", "Main armor command.")
					.addSubcommands(
						new HelpData("emoji", "Get a player's armor and equipment represented in emojis.", "emoji [player] [profile]"),
						new HelpData("list", "Get a player's armor and equipment with lore.", "list [player] [profile] [slot]")
					)
					.setCategory("inventory"),
				new HelpData("enderchest", "Main enderchest command.")
					.addSubcommands(
						new HelpData("emoji", "Get a player's enderchest represented in emojis.", "emoji [player] [profile]"),
						new HelpData("list", "Get a player's enderchest with lore.", "list [player] [profile] [slot]")
					)
					.setCategory("inventory"),
				new HelpData("storage", "Get a player's storage represented in emojis.", "storage [player] [profile]")
					.setCategory("inventory"),
				new HelpData("talisman", "Main talisman command.")
					.addSubcommands(
						new HelpData("emoji", "Get a player's talisman bag represented in emojis.", "emoji [player] [profile]"),
						new HelpData("list", "Get a player's talisman bag with lore.", "list [player] [profile] [slot]"),
						new HelpData("tuning", "Get a player's power stone stats and tuning stats", "list [player] [profile]")
					)
					.setCategory("inventory"),
				new HelpData(
					"sacks",
					"Get a player's sacks content bag represented in a list and sorted by" +
					" descending price. Costs can be calculated from (source) bazaar only," +
					" npc only, or npc and bazaar.",
					"sacks [player] [profile] [source]"
				)
					.setCategory("inventory"),
				new HelpData("wardrobe", "Main wardrobe command.")
					.addSubcommands(
						new HelpData("emoji", "Get a player's wardrobe represented in emojis.", "emoji [player] [profile]"),
						new HelpData("list", "Get a player's wardrobe with lore.", "list [player] [profile] [slot]")
					)
					.setCategory("inventory"),
				new HelpData("pets", "Get a player's pets and missing pets.", "pets [player] [profile]").setCategory("inventory"),
				// Misc
				new HelpData("roles", "Main roles command.")
					.addSubcommands(
						new HelpData("claim", "Claim your automatic Skyblock roles. You must be linked to the bot.", "claim [profile]"),
						new HelpData("list", "List all claimable roles for this server.")
					)
					.setCategory("miscellaneous"),
				new HelpData("coins", "Get a player's coins and bank history.", "coins [player] [profile]").setCategory("miscellaneous"),
				new HelpData(
					"networth",
					"Calculate a player's networth. Set verbose to true to get a detailed breakdown of the calculations.",
					"networth [player] [profile] [verbose]"
				)
					.setCategory("miscellaneous"),
				new HelpData(
					"weight",
					"Get a player's slayer, skills, dungeons, and total weight. Shows both Senither and Lily weight.",
					"weight [player] [profile]"
				)
					.setCategory("miscellaneous"),
				new HelpData(
					"calcweight",
					"Calculate predicted weight change for a reaching a skills, slayers, or dungeons level/amount.",
					"calcweight <type> <amount> [player] [profile] [system]"
				)
					.setCategory("miscellaneous"),
				new HelpData("hypixel", "Get Hypixel information about a player.", "hypixel [player]").setCategory("miscellaneous"),
				new HelpData("missing", "Get a player's missing talismans and cheapest ones to buy.", "missing [player] [profile]")
					.setCategory("miscellaneous"),
				new HelpData("checkapi", "Get a player's enabled and disabled Skyblock APIs", "checkapi [player] [profile]")
					.setCategory("miscellaneous"),
				new HelpData("cakes", "Get a player's active and inactive cake buffs.", "cakes [player] [profile]")
					.setCategory("miscellaneous"),
				new HelpData("bestiary", "Get a player's bestiary statistics.", "bestiary [player] [profile]").setCategory("miscellaneous"),
				new HelpData("harp", "Get a player's harp statistics.", "harp [player] [profile]").setCategory("miscellaneous"),
				new HelpData(
					"checkreqs",
					"Check if a player meets any of the setup automated guild requirements.",
					"checkreqs [player] [profile]"
				)
					.setCategory("miscellaneous"),
				new HelpData("uuid", "Convert username to UUID or UUID to username.", "uuid [username|uuid]")
					.addExamples("uuid CrypticPlasma", "uuid 044903b7a9d3416d957f929557af6c88")
					.setCategory("miscellaneous"),
				new HelpData("fetchur", "Get today's fetchur item.").setCategory("miscellaneous"),
				new HelpData("level", "Get a player's skyblock level.", "level [player] [profile]").setCategory("miscellaneous"),
				new HelpData("collections", "Get a player's collections.", "collections [player] [profile]").setCategory("miscellaneous"),
				new HelpData("jacob", "Get the upcoming contests and their crops.", "jacob [crop]").setCategory("miscellaneous"),
				new HelpData("mayor", "Get the current mayor and their perks.").setCategory("miscellaneous"),
				new HelpData("leaderboard", "Get a global leaderboard.", "leaderboard <type> [player] [gamemode] [page] [rank] [amount]")
					.setCategory("miscellaneous"),
				new HelpData("skyblock", "Get an overview of a player's Skyblock profiles", "skyblock [player] [profile]")
					.setCategory("miscellaneous"),
				new HelpData("bingo", "Get the current bingo goals and a player's live bingo card.", "bingo [player]")
					.setCategory("miscellaneous"),
				new HelpData("recipe", "Get the crafting recipe of an item.", "recipe <item>").setCategory("miscellaneous"),
				new HelpData("craft", "Calculate the cost of an item and added upgrades.", "craft <item>").setCategory("miscellaneous"),
				new HelpData("calendar", "Get the current Skyblock datetime and running or upcoming events").setCategory("miscellaneous"),
				new HelpData(
					"scammer",
					"Check if a player is marked as a scammer in the SkyblockZ database with the reason, Discords, and Minecraft alts",
					"scammer [player]"
				)
					.setCategory("miscellaneous"),
				new HelpData(
					"calcdrags",
					"Calculate your loot quality and predicted loot from dragons in the end. The" +
					" ratio is your damage divided by first place damage.",
					"calcdrags [position] [eyes] [ratio]"
				)
					.setCategory("miscellaneous"),
				new HelpData("reforge", "Get the reforge stone stats for each rarity.", "reforge <stone>").setCategory("miscellaneous"),
				// Party
				new HelpData("party", "Main party command.")
					.addSubcommands(
						new HelpData("create", "Interactive message to create a new party."),
						new HelpData("current", "Get information about your current party."),
						new HelpData("join", "Join a player's party by the party leader's IGN.", "join <username>")
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
						new HelpData("add", "Force add a player to the current event.", "add <player> [profile]"),
						new HelpData("leave", "Leave the current event."),
						new HelpData("remove", "Force remove a player from the current event.", "remove <player>"),
						new HelpData("leaderboard", "Get the leaderboard for current event."),
						new HelpData(
							"end",
							"Force end the event. If silent is true, the event will be canceled and no prizes or winners will be announced.",
							"end [silent]"
						)
					)
					.setCategory("event"),
				// Settings
				new HelpData("settings", "Main settings command.")
					.setCategory("settings")
					.addSecondData("View the current settings for this server.", "settings")
					.addSubcommands(
						new HelpData("general", "View general settings for this server."),
						new HelpData("reset", "Clear and reset the current server settings."),
						new HelpData("set", "Set certain settings.")
							.addSubcommands(
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
									"Set the role that will be given to linked users that are not in" +
									" any of the setup automatic guilds. Requires at least one" +
									" automatic guild to be setup.",
									"guest_role <@role>"
								),
								new HelpData(
									"sync_unlinked",
									"Enable or disable whether unlinked members should also be synced for verified roles, automatic roles, and guild roles during automatic updates.",
									"sync_unlinked <enable|disable>"
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
						new HelpData("log", "Manage log settings.")
							.addSubcommands(
								new HelpData("channel", "Set the channel where events will be logged to.", "channel <#channel>"),
								new HelpData(
									"add",
									"Add a log event. Valid events are user_verify, guild_sync, and bot_permission_error.",
									"add <event>"
								),
								new HelpData("remove", "Remove a log event.", "remove <event>")
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
								new HelpData(
									"ban",
									"Add a player to the blacklist and ban them from the server. Reason" +
									" will default to 'not provided' if not set.",
									"ban <player> <discord> [reason]"
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
								new HelpData("stop_using", "Stop using a shared blacklist from another server.", "stop_using <server_id>"),
								new HelpData(
									"enable",
									"Blacklisted people will not be able to use this feature. Feature can be verify or apply.",
									"enable <feature>"
								),
								new HelpData("disable", "Blacklisted people will be able to use this feature.", "disable <feature>")
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
								new HelpData("add", "Add a crop to be tracked and notified.", "add <crop|all> [role]"),
								new HelpData("remove", "Remove a crop from the tracking list.", "remove <crop>")
							),
						new HelpData("event", "Main command for event notification settings.")
							.addSecondData("Get the current event notification settings for the bot.", "event")
							.addSubcommands(
								new HelpData("enable", "Enable event notifications."),
								new HelpData("disable", "Disable event notifications."),
								new HelpData("add", "Add an event to be tracked and notified.", "add <event|all> <#channel> [@role]"),
								new HelpData("remove", "Remove an event from the tracking list.", "remove <event>")
							),
						new HelpData("verify", "Main command for verification settings.")
							.addSecondData("Get the current verification settings for the bot.", "verify")
							.addSubcommands(
								new HelpData("enable", "Enable automatic verify."),
								new HelpData("disable", "Disable automatic verify."),
								new HelpData("message", "The message that users will see when verifying.", "message <message>")
									.addExamples("message Run /link <IGN> replacing IGN with your IGN to verify!"),
								new HelpData("roles", "Modify roles given on verification")
									.addSubcommands(
										new HelpData(
											"add",
											"Add a role that user will receive upon being verified. The" +
											" role cannot be @everyone or a managed role. You can" +
											" add a max of 5 roles.",
											"add <@role>"
										),
										new HelpData("remove", "Remove a verify role.", "remove <@role>")
									),
								new HelpData(
									"channel",
									"The channel where the verify message and verify button will be sent.",
									"channel <#channel>"
								),
								new HelpData(
									"nickname",
									"""
									The nickname template that a user will be renamed to on verifying. Can be set to none. Add .{text} for text that should only be added if the previous template was met. See example [here](https://discord.com/channels/796790757947867156/802749920679165994/937099157200580618)

									You can use the following templates:
									• [GUILD.NAME]
									• [GUILD.TAG]
									• [GUILD.RANK]
									• [PLAYER.SKILLS]
									• [PLAYER.CATACOMBS]
									• [PLAYER.SLAYER]
									• [PLAYER.WEIGHT]
									• [PLAYER.CLASS]
									• [PLAYER.LEVEL]
									• [PLAYER.IRONMAN]
									• [PLAYER.EMBLEM]
									""",
									"nickname [prefix] [IGN] [postfix]"
								)
									.addExamples("nickname Verified | [IGN]", "nickname [[GUILD.RANK]] [IGN]"),
								new HelpData(
									"remove_role",
									"Set the role that will be removed on verifying and re-added when un-verifying.",
									"remove_role <@role>"
								),
								new HelpData(
									"sync",
									"Enable or disable verification automatic sync. This will sync the" +
									" verified role(s) and nickname when a user joins the server" +
									" or every hour (up to 160 members per sync).",
									"sync <enable|disable>"
								),
								new HelpData(
									"dm_on_join",
									"Enable or disable verification automatic DM on join sync. This" +
									" will toggle if a user should be messaged when their roles" +
									" are synced on joining the server.",
									"dm_on_join <enable|disable>"
								),
								new HelpData(
									"roles_claim",
									"Enable or disable automatic roles claim sync. This is the same as" +
									" running the roles claim command on" +
									" joining/linking/verifying.",
									"roles_claim <enable|disable>"
								)
							),
						new HelpData("roles", "Main command for automatic roles settings.")
							.addSecondData("Get the current roles settings for the bot.", "roles")
							.addSubcommands(
								new HelpData("enable", "Enable automatic roles."),
								new HelpData("disable", "Disable automatic roles."),
								new HelpData(
									"add",
									"Add a new level to a role with its corresponding Discord role.",
									"add <role_name> <value> <@role>"
								)
									.addExamples("add wolf 400000 @wolf 8", "add alchemy 50 @alchemy 50"),
								new HelpData("remove", "Remove a role level for a role.", "remove <role_name> <value>")
									.addExamples("remove wolf 400000", "remove alchemy 50"),
								new HelpData(
									"use_highest",
									"Enable or disable using the highest values across all profile. Default is false",
									"use_highest <enable|disable>"
								),
								new HelpData(
									"sync",
									"Enable or disable automatic roles claim sync. This is the same as" +
									" running `/roles claim` automatically every hour (up to 45" +
									" members will be updated per sync).",
									"sync <enable|disable>"
								)
							),
						new HelpData("guild", "Main command for automatic guild (application and guild role/ranks).")
							.addSecondData("List all setup automatic guilds.", "guild")
							.addSubcommands(
								new HelpData("create", "Create a new automatic guild for a guild.", "create <name>")
									.addExamples("create my_guild"),
								new HelpData("remove", "Remove an automatic guild.", "remove <name>").addExamples("remove my_guild"),
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
								new HelpData(
									"counter",
									"Enable or disable the guild member counter. This will create the" +
									" voice channel to display the current guild member count and" +
									" updates every hour.",
									"settings guild <name> counter",
									true
								),
								new HelpData("apply", "Automatic application system for this guild.", "settings guild <name> apply", true)
									.addSubcommands(
										new HelpData("enable", "Enable automatic apply.", "settings guild <name> apply enable", true),
										new HelpData("disable", "Disable automatic apply.", "settings guild <name> apply disable", true),
										new HelpData(
											"close",
											"Close automatic apply. Disables the button to create a new application.",
											"settings guild <name> apply close",
											true
										),
										new HelpData(
											"open",
											"Re-enable the button to create a new application.",
											"settings guild <name> apply open",
											true
										),
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
											"Category where new apply channels will be made. Run" +
											" `categories` to get the ID's of all categories in" +
											" the server.",
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
											"Channel where the players who were accepted or waitlisted" +
											" will be sent. Optional and can be set to none.",
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
											"Whether applicants must use a certain gamemode profile in" +
											" their application. Defaults to all. Options are:" +
											" all, regular, ironman, stranded, or" +
											" ironman_stranded.",
											"settings guild <name> apply gamemode <gamemode>",
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
											"Requirements applications must meet. An application" +
											" will be auto-denied if they do not meet the" +
											" requirements.",
											"settings guild <name> apply requirements",
											true
										)
											.addSubcommands(
												new HelpData(
													"add",
													"Add a requirement that applicant must meet. Types" +
													" are slayer, skills, catacombs, weight," +
													" lily_weight, level, networth, and" +
													" farming_weight. At least one of the" +
													" requirement types must be set.",
													"settings guild <name> apply requirements add [type:amount] ...",
													true
												),
												new HelpData(
													"remove",
													"Remove a requirement by its index. Run `settings" +
													" guild <name>` to see the index for all" +
													" current requirements.",
													"settings guild <name> apply requirements remove <index>",
													true
												)
											),
										new HelpData(
											"scammer_check",
											"Enable or disable if a player should automatically be" +
											" denied if they are marked as a scammer in the" +
											" SkyblockZ database. Defaults to false.",
											"settings guild <name> apply scammer_check <enable|disable>",
											true
										)
									)
							)
					),
				new HelpData("setup", "A guide on how to setup the bot.").setCategory("settings"),
				new HelpData("categories", "Get the ids of all categories in this server.").setCategory("settings"),
				new HelpData(
					"reload",
					"Reload the automatic guild application(s) and automatic verification settings." +
					" This is required for most changes to take effect for both of these features."
				)
					.setCategory("settings")
			)
		);
	}

	public static EmbedBuilder getHelp(String pageStr, SlashCommandEvent event) {
		int startingPage = 0;
		if (pageStr != null) {
			String[] pageStrSplit = pageStr.split("\\s+", 2);

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

		CustomPaginator.Builder paginateBuilder = event
			.getPaginator()
			.updateExtras(extra ->
				extra
					.setTitles(pageTitleToCategory.keySet().stream().toList())
					.setTitleUrls(pageTitleToCategory.values().stream().map(u -> WEBSITE_LINK + "/commands" + u).toList())
			);

		paginateBuilder.addStrings(
			"Use the arrow buttons to navigate through the pages" + generatePageMap() + "\n\n<> = required [] = optional"
		);

		paginateBuilder.addStrings(
			create("help [command]", "Show the help menu for all commands or a specific command") +
			create("information", "Show information and statistics about the bot") +
			create("invite", "Get the invite link and Discord link for the bot") +
			create("link <player>", "Link your Hypixel account to the bot") +
			create("unlink", "Unlink your account from the bot") +
			create("vote", "Upvote the bot")
		);

		paginateBuilder.addStrings(
			create("slayer [player] [profile]", "Get the slayer data of a player") +
			create(
				"calcslayer <type> [player] [profile] [level] [xp] [system]",
				"Calculate the number of bosses needed to reach a level or xp amount"
			)
		);

		paginateBuilder.addStrings(
			create("skills [player] [profile]", "Get the skills data of a player") +
			create("hotm [player] [profile]", "Get a player's heart of the mountain statistics") +
			create("forge [player] [profile]", "Get a player's forge items & ending times") +
			create("crimson [player] [profile]", "Get the crimson isle stats of a player")
		);

		paginateBuilder.addStrings(
			create("dungeons [player] [profile]", "Get the dungeons data of a player") +
			create("essence upgrade <item>", "Interactive message to find the essence amount to upgrade an item") +
			create("essence information <item>", "Get the amount of essence to upgrade an item for each level") +
			create("essence player [player] [profile]", "Get a player's essence amounts and essence shop upgrades") +
			create(
				"calcruns <level> <floor> [player] [profile] [system]",
				"Calculate the number of runs needed to reach a certain catacombs level"
			) +
			create("calcdrops <floor> [item] [luck] [accessory]", "Calculate the drop rate and cost of all chests for a floor or item")
		);

		paginateBuilder.addStrings(
			create("guild information [player] [guild]", "Get information and statistics about a guild") +
			create("guild members [player] [guild]", "Get a list of all members in a guild") +
			create("guild experience [player] [guild]", "Get the experience leaderboard for a guild") +
			create("guild leaderboard <type> [player] [guild] [mode]", "Get a leaderboard for a player's guild") +
			create("guild kicker <requirements> [player] [gamemode]", "Get all player's who don't meet the provided requirements") +
			create(
				"guild check [player] [gamemode]",
				"A customizable helper that will tell you who to kick, promote or demote in your Hypixel guild"
			) +
			create(
				"guild top [player] [guild] [gamemode]",
				"Get a guild's Skyblock statistics of slayer, skills, catacombs, weight, level, and networth"
			) +
			create("guildlb <type> [guild] [mode] [comparison]", "Get a global guild leaderboard")
		);

		paginateBuilder.addStrings(
			create("auctions [player] [filter] [sort] [verbose]", "Get a player's unclaimed auctions on all profiles") +
			create("viewauction <uuid>", "Get an auction by its UUID") +
			create("attributes <item> <attribute_one> <attribute_two>", "Get the lowest priced attribute combination for an item") +
			create("bin <item>", "Get the lowest bin of an item") +
			create("bazaar <item>", "Get bazaar prices of an item") +
			create("average <item>", "Get the average auction price of an item") +
			create("bids [player]", "Get a player's auction house bids") +
			create("price <item> [auction_type]", "Query the auction house for the lowest bin of an item") +
			create("bits", "Get the coins to bits ratio for items in the bits shop") +
			create("copper", "Get the coins to copper ratio for items in the SkyMart shop") +
			create("flips", "Get current auction flips")
		);

		paginateBuilder.addStrings(
			create("inventory emoji [player] [profile]", "Get a player's inventory represented in emojis") +
			create("inventory list [player] [profile] [slot]", "Get a player's inventory with lore") +
			create("museum view [player] [profile]", "View a player's museum items") +
			create("museum cheapest [player] [profile]", "Get the cheapest items to donate to a player's museum") +
			create("armor emoji [player] [profile] [slot]", "Get a player's equipped armor & equipment represented in emojis") +
			create("armor list [player] [profile]", "Get a player's equipped armor & equipment with lore") +
			create("enderchest emoji [player] [profile]", "Get a player's ender chest represented in emojis") +
			create("enderchest list [player] [profile] [slot]", "Get a player's enderchest lore") +
			create("storage [player] [profile]", "Get a player's storage represented in emojis") +
			create("talisman emoji [player] [profile]", "Get a player's talisman bag represented in emojis") +
			create("talisman list [player] [profile] [slot]", "Get a player's talisman bag with lore") +
			create("talisman tuning [player] [profile]", "Get a player's power stone stats and tuning stats") +
			create("sacks [player] [profile] [source]", "Get a player's sacks content bag represented in a list") +
			create("wardrobe emoji [player] [profile]", "Get a player's wardrobe armors represented in emojis") +
			create("wardrobe list [player] [profile]", "Get a player's wardrobe armors represented in a list") +
			create("pets [player] [profile]", "Get a player's pets")
		);

		paginateBuilder.addStrings(
			create("roles claim [profile]", "Claim your automatic Skyblock roles based on your statistics") +
			create("roles list", "List all roles that can be claimed through the bot") +
			create("level [player] [profile]", "Get a player's Skyblock level") +
			create("coins [player] [profile]", "Get a player's coins and bank history") +
			create("networth [player] [profile] [verbose]", "Calculate a player's networth") +
			create("weight [player] [profile]", "Get a player's slayer, skills, dungeons, and total weight") +
			create(
				"calcweight <type> <amount> [player] [profile] [system]",
				"Calculate predicted weight change for a reaching certain skill, slayer, or catacombs level/amount"
			) +
			create("hypixel [player]", "Get Hypixel information about a player") +
			create("missing [player] [profile]", "Get a player's missing talismans") +
			create("fetchur", "Get the item that fetchur wants today") +
			create("cakes [player] [profile]", "Get a player's inactive and active cake buffs") +
			create("bestiary [player] [profile]", "Get a player's bestiary stats") +
			create("harp [player] [profile]", "Get a player's harp statistics") +
			create("checkreqs [player] [profile]", "Check if a player meets any of the setup automated guild requirements") +
			create("uuid [player]", "Convert username to UUID or UUID to username") +
			create("calendar", "Get the current Skyblock datetime and running or upcoming events") +
			create("scammer [player]", "Check if a player is marked as a scamer in the SBZ database") +
			create("jacob [crop]", "Get the upcoming contests and their crops") +
			create("mayor", "Get information about the current mayor or the running election") +
			create("bingo [player]", "Get the current bingo goals and a player's bingo card") +
			create("leaderboard <type> [player] [gamemode] [page] [rank] [amount]", "Get a global leaderboard") +
			create("skyblock [player] [profile]", "Get an overview of a player's Skyblock profiles") +
			create("recipe <item>", "Get the crafting recipe of an item") +
			create("craft <item>", "Calculate the cost of an item and added upgrades") +
			create("checkapi [player]", "Check which Skyblock APIs a player has enabled or disabled") +
			create("calcdrags [position] [ratio] [eyes]", "Calculate loot quality and loot from dragons in the end") +
			create("reforge <stone>", "Get the reforge stone stats for each rarity") +
			create("collections [player] [profile]", "Get a player's island collection counts")
		);

		paginateBuilder.addStrings(
			create("party create", "Interactive message to create a party") +
			create("party current", "Get information about your current party") +
			create("party join <username>", "Join a party") +
			create("party leave", "Leave your current party") +
			create("party list", "List all active parties") +
			create("party disband", "Disband your party") +
			create("party kick <username>", "Kick a member from your party")
		);

		paginateBuilder.addStrings(
			create("event create", "Interactive message to create a Skyblock event") +
			create("event current", "Get information about the current event") +
			create("event join [profile]", "Join the current event") +
			create("event add <player> [profile]", "Force add a player to the event") +
			create("event leave", "Leave the current event") +
			create("event remove <player>", "Force remove a player from the event") +
			create("event leaderboard", "Get the leaderboard for current event") +
			create("event end [silent]", "Force end or cancel the event")
		);

		paginateBuilder.addStrings(
			create("settings", "View the current settings for the Discord server") +
			create("settings general", "View the bot's general settings for this server") +
			create("setup", "A short walk-through on how to setup the bot") +
			create("categories", "Get the name and id of all categories in this server") +
			create("reload", "Reload the guild application and verification settings") +
			create(
				"settings set fetchur_channel <#channel>",
				"Set the channel where fetchur item notifications will be sent every day at 12 am EST"
			) +
			create("settings set fetchur_ping <@role>", "Role that will be pinged when the fetchur item notifications is sent") +
			create(
				"settings set mayor_channel <#channel>",
				"Set the channel where notifications will be sent when a new mayor is elected"
			) +
			create("settings set mayor_ping <@role>", "Role that will be pinged when mayor notifications are sent") +
			create("settings set guest_role <@role>", "Set the guest role") +
			create("settings set sync_unlinked <enable|disable>", "Set whether unlinked people are synced") +
			create("settings log channel <#channel>", "Set the action log channel") +
			create("settings log add <event>", "Add an event to log") +
			create("settings log remove <event>", "Remove a log event") +
			create("settings bot_manager add <@role>", "Add a bot manager role") +
			create("settings bot_manager remove <@role>", "Remove a bot manager role") +
			create("settings reset", "Reset the server settings") +
			create("settings blacklist", "List all players that are blacklisted") +
			create("settings blacklist add <player> [reason]", "Blacklist a player from verifying or applying in this server") +
			create(
				"settings blacklist ban <player> <discord> [reason]",
				"Ban and blacklist a player from verifying or applying in this serve"
			) +
			create("settings blacklist remove <player>", "Remove a player from the blacklist") +
			create("settings blacklist search <player>", "Remove a player from the blacklist") +
			create("settings blacklist share <server_id>", "Share your blacklist with another server") +
			create("settings blacklist unshare <server_id>", "Stop sharing your blacklist with another server") +
			create("settings blacklist use <server_id>", "Use a shared blacklist from another server") +
			create("settings blacklist stop_using <server_id>", "Stop using a shared blacklist from another server") +
			create("settings blacklist enable <feature>", "Blacklisted people will not be able to use this feature") +
			create("settings blacklist disable <feature>", "Blacklisted people will be able to use this feature")
		);

		paginateBuilder.addStrings(
			create("settings jacob", "View the current settings for farming event notifications") +
			create("settings jacob <enable|disable>", "Enable or disable farming event notifications") +
			create("settings jacob channel <#channel>", "Set the channel where farming event notifications will be sent") +
			create("settings jacob add <crop|all> [@role]", "Added a crop to be tracked. Role will automatically be created") +
			create("settings jacob remove <crop>", "Remove a crop from the tracking list")
		);

		paginateBuilder.addStrings(
			create("settings event", "View the current settings for event notifications") +
			create("settings event <enable|disable>", "Enable or disable event notifications") +
			create("settings event add <event|all> <#channel> [@role]", "Added an event to be notified for") +
			create("settings event remove <event>", "Remove an event from the notification list")
		);

		paginateBuilder.addStrings(
			create("settings verify", "Get the current verify settings for the bot") +
			create("settings verify <enable|disable>", "Enable or disable automatic verify") +
			create("settings verify message <message>", "The message that users will see when verifying") +
			create("settings verify roles add <@role>", "Add a role that user will receive upon being verified") +
			create("settings verify roles remove <@role>", "Remove a verify role") +
			create("settings verify channel <#channel>", "Channel where the verify message and button will be sent") +
			create("settings verify nickname [prefix] [IGN] [postfix]", "The nickname template on verifying. Can be set to none") +
			create("settings verify remove_role <@role>", "Role that will be removed on verifying and re-added when un-verifying") +
			create("settings verify sync <enable|disable>", "Enable or disable automatic verify role and nickname syncing") +
			create("settings verify dm_on_join <enable|disable>", "Enable or disable DMing the user on join sync") +
			create("settings verify roles_claim <enable|disable>", "Enable or disable SB role sync on join")
		);

		paginateBuilder.addStrings(
			create("settings guild create <name>", "Create a new automatic guild where name is the guild's name") +
			create("settings guild remove <name>", "Remove an automatic guild") +
			create("settings guild <name>", "View the settings for a specific automatic guild") +
			create("settings guild <name> member_role <enable|disable>", "Enable or disable automatic guild role assigning") +
			create("settings guild <name> member_role <@role>", "Set the role to give guild members") +
			create("settings guild <name> ranks <enable|disable>", "Enable or disable automatic guild rank assigning") +
			create("settings guild <name> ranks add <rank_name> <@role>", "Add an automatic guild rank") +
			create("settings guild <name> ranks remove <rank_name>", "Remove an automatic guild rank") +
			create("settings guild <name> counter <enable|disable>", "Enable or disable guild members counter")
		);

		paginateBuilder.addStrings(
			create("settings guild <name> apply <enable|disable>", "Enable or disable automatic apply") +
			create("settings guild <name> apply close", "Close automatic apply") +
			create("settings guild <name> apply open", "Open automatic apply") +
			create("settings guild <name> apply message <message>", "The message that users will see when verifying") +
			create(
				"settings guild <name> apply staff_roles add <@role>",
				"Add a role that will be pinged when a new application is submitted"
			) +
			create("settings guild <name> apply staff_roles remove <@role>", "Remove a staff ping role") +
			create("settings guild <name> apply channel <#channel>", "Channel where the message to click for applying will sent") +
			create("settings guild <name> apply category <category>", "Category where new apply channels will be made") +
			create(
				"settings guild <name> apply staff_channel <#channel>",
				"Channel where new applications will be sent to be reviewed by staff"
			) +
			create(
				"settings guild <name> apply waiting_channel <#channel>",
				"Channel where the players who were accepted or waitlisted will be sent. Can be set to none"
			) +
			create("settings guild <name> apply accept_message <message>", "Message that will be sent if applicant is accepted") +
			create(
				"settings guild <name> apply waitlist_message <message>",
				"Message that will be sent if applicant is waitlisted. Can be set to none"
			) +
			create(
				"settings guild <name> apply gamemode <gamemode>",
				"Whether applicants must use a certain gamemode profile in their application. Defaults to 'all'"
			) +
			create("settings guild <name> apply deny_message <message>", "Message that will be sent if applicant is denied") +
			create(
				"settings guild <name> apply requirements add [type:amount] ...",
				"Add a requirement that applicant must meet. At least one of the requirement types must be set. Can be empty"
			) +
			create(
				"settings guild <name> apply requirements remove <index>",
				"Remove a requirement. Run `settings guild <name>` to see the index for all current requirements"
			) +
			create(
				"settings guild <name> apply scammer_check <enable|disable>",
				"Whether the applicant should be automatically be denied if marked a scammer in the SkyblockZ database"
			)
		);

		paginateBuilder.addStrings(
			create("settings roles", "Get the current roles settings for the bot") +
			create("settings roles <enable|disable>", "Enable or disable automatic roles") +
			create(
				"settings roles use_highest <enable|disable>",
				"Enable or disable using the highest values or last played on profile. Default is false"
			) +
			create("settings roles sync <enable|disable>", "Enable or disable automatic roles claim sync") +
			create("settings roles add <role_name> <value> <@role>", "Add a new level to a role with its corresponding Discord role") +
			create("settings roles remove <role_name> <value>", "Remove a role level for a role")
		);

		event.paginate(paginateBuilder, startingPage);
		return null;
	}

	private static String generatePageMap() {
		StringBuilder generatedStr = new StringBuilder();
		int i = 2;
		for (Map.Entry<String, String> entry : pageTitleToCategory.entrySet()) {
			if (!entry.getKey().equals("Navigation")) {
				generatedStr
					.append("\n• **Page ")
					.append(i)
					.append(":** ")
					.append("[")
					.append(entry.getKey())
					.append("](")
					.append(WEBSITE_LINK)
					.append("/commands")
					.append(entry.getValue())
					.append(")");
				i++;
			}
		}
		return generatedStr.toString();
	}

	private static String create(String commandName, String desc) {
		return "`/" + commandName + "`: " + desc + "\n";
	}

	@Override
	protected void execute(SlashCommandEvent event) {
		event.paginate(getHelp(event.getOptionStr("command"), event));
	}

	@Override
	public SlashCommandData getCommandData() {
		return Commands
			.slash(name, "Show the help page for this bot")
			.addOption(OptionType.STRING, "command", "Name of command or page number", false, true);
	}

	@Override
	public void onAutoComplete(AutoCompleteEvent event) {
		if (event.getFocusedOption().getName().equals("command")) {
			event.replyClosestMatch(event.getFocusedOption().getValue(), helpNameList);
		}
	}

	private List<String> commandToNames(HelpData command) {
		List<String> commands = new ArrayList<>();
		commands.add(command.getName());
		for (HelpData subcommand : command.getSubcommands()) {
			commands.addAll(commandToNames(subcommand));
		}
		return commands;
	}
}
