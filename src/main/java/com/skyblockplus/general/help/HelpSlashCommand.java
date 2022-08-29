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

import static com.skyblockplus.utils.Utils.*;

import com.skyblockplus.utils.command.CustomPaginator;
import com.skyblockplus.utils.command.SlashCommand;
import com.skyblockplus.utils.command.SlashCommandEvent;
import com.skyblockplus.utils.structs.AutoCompleteEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import org.springframework.stereotype.Component;

@Component
public class HelpSlashCommand extends SlashCommand {

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

	public HelpSlashCommand() {
		this.name = "help";

		setHelpList();
		helpDataList.stream().map(this::commandToNames).forEach(helpNameList::addAll);
	}

	@Override
	protected void execute(SlashCommandEvent event) {
		event.logCommand();

		event.paginate(getHelp(event.getOptionStr("command"), event));
	}

	@Override
	public CommandData getCommandData() {
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

	public static void setHelpList() {
		helpDataList.clear();
		helpDataList.addAll(
			Arrays.asList(
				// General
				new HelpData("help", "Show the help menu with all the commands.", "help")
					.addSecondData("Show the help menu for a certain command.", "help <command>")
					.addExamples("help", "help guild experience")
					.setCategory("general"),
				new HelpData("information", "Get information about this bot.").setCategory("general"),
				new HelpData("invite", "Get the invite link and support server link for the bot.").setCategory("general"),
				new HelpData("link", "Link your Hypixel account to the bot.", "link <player>")
					.addExamples("link CrypticPlasma")
					.setCategory("general"),
				new HelpData("unlink", "Unlink your account from the bot.").setCategory("general"),
				// Slayer
				new HelpData("slayer", "Get the slayer data of a player.", "slayer [player] [profile]")
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
					.addPlayerExamples()
					.setCategory("dungeons"),
				new HelpData("essence", "Main essence command.")
					.setCategory("dungeons")
					.addSubcommands(
						new HelpData("upgrade", "Interactive message to find the essence amount to upgrade an item.", "upgrade <item>")
							.addExamples("upgrade Hyperion"),
						new HelpData("information", "Get the amount of essence to upgrade an item for each level.", "information <item>")
							.addExamples("information Hyperion"),
						new HelpData("player", "Get a player's essence amounts and their essence shop upgrades.", "[player] [profile]")
							.addPlayerExamples()
					),
				new HelpData(
					"calcruns",
					"Calculate the number of runs needed to reach a certain catacombs level. The floor can be from F1 to F7 or M1 to M6.",
					"calcruns [player] [profile] <level:level> <floor:floor>"
				)
					.setCategory("dungeons"),
				new HelpData(
					"calcdrops",
					"Calculate the drop rate and cost of all chests for a floor",
					"calcdrops <floor> [luck:1|2|3|4|5] [accessory:none|talisman|ring|artifact]"
				)
					.setCategory("dungeons"),
				// Guild
				new HelpData("guild", "Main guild command")
					.addSecondData("Find what guild a player is in.", "guild <player>")
					.addSubcommands(
						new HelpData("information", "Get information and statistics about a player's guild.", "information [player]")
							.addSecondData("Get information and statistics about a guild.", "information <g:guild_name>")
							.addExamples("information CrypticPlasma", "information g:Skyblock_Forceful"),
						new HelpData("members", "Get a list of all members in a player's guild.", "members [player]")
							.addSecondData("Get a list of all members in a guild.", "members <g:guild_name>")
							.addExamples("members CrypticPlasma", "members g:Skyblock_Forceful"),
						new HelpData(
							"experience",
							"Get the experience leaderboard for a player's guild. Days can range from 1 to 7, default number of days is 7.",
							"experience [player] [days:days]"
						)
							.addSecondData(
								"Get the experience leaderboard for a guild. Days can range from 1 to 7, default number of days is 7.",
								"experience <g:guild_name> [days:days]"
							)
							.addExamples("experience CrypticPlasma", "experience g:Skyblock Forceful days:4")
					)
					.setCategory("guild"),
				new HelpData(
					"guild-leaderboard",
					"Get a leaderboard for a player's guild. All types can be seen through autocomplete. The mode can be all, ironman, or stranded.  Set key to true to force use the set Hypixel API key for more accurate results.",
					"guild-leaderboard <type> [player] [mode:all|ironman|stranded] [key]"
				)
					.addSecondData(
						"Get a leaderboard for a guild.",
						"guild-leaderboard <type> <g:guild_name> [mode:all|ironman|stranded] [key]"
					)
					.addExamples("guild-leaderboard weight CrypticPlasma", "guild-leaderboard sven CrypticPlasma mode:ironman")
					.setCategory("guild"),
				new HelpData(
					"guild-kicker",
					"Get all player's who don't meet the provided requirements. The requirement type can be skills, slayer, catacombs, or weight. The requirement value must be an integer. You can have up to 3 sets of requirements.  Set key to true to force use the set Hypixel API key for more accurate results.",
					"guild-kicker <u:player> <[type:value ...]> ... [key]"
				)
					.addExamples("guild-kicker u:CrypticPlasma [weight:4000 skills:40] [weight:4500]")
					.setCategory("guild"),
				new HelpData(
					"guild-ranks",
					"A customizable helper that will tell you who to kick, promote or demote in your Hypixel guild. Please DM me or join the Skyblock Plus [Discord Server](" +
					DISCORD_SERVER_INVITE_LINK +
					") to set this up for your guild. Set key to true to force use the set Hypixel API key for more accurate results.",
					"guild-ranks <player> [mode:all|ironman|stranded] [key]"
				)
					.addExamples("guild-ranks CrypticPlasma")
					.setCategory("guild"),
				new HelpData(
					"guild-statistics",
					"Get a guild's SkyBlock statistics of slayer, skills, catacombs, and weight. Set key to true to force use the set Hypixel API key for more accurate results.",
					"guild-statistics [player] [g:guild_name] [key]"
				)
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
					"auctions [player] [sort:low|high] [filter:unsold|sold] [verbose]"
				)
					.addExamples("auctions CrypticPlasma", "auctions CrypticPlasma filter:sold verbose:true")
					.setCategory("price"),
				new HelpData("viewauction", "Get information about an auction by it's UUID.", "viewauction <uuid>")
					.addExamples("auctions uuid 77df55d9c0084473b113265ef48fb396")
					.setCategory("price"),
				new HelpData("bin", "Get the lowest bin of an item.", "bin <item>")
					.addExamples("bin Necron Chestplate")
					.setCategory("price"),
				new HelpData("bazaar", "Get the bazaar prices of an item.", "bazaar <item>")
					.addExamples("bazaar Booster Cookie")
					.setCategory("price"),
				new HelpData("average", "Get the average auction price of an item.", "average <item>")
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
					.setCategory("price"),
				new HelpData("coinsperbit", "Get the coins to bits ratio for items in the bits shop.", "coinsperbit <item>")
					.setCategory("price"),
				// Inventory
				new HelpData("inventory", "Get a player's inventory represented in emojis.", "inventory [player] [profile]")
					.addSecondData("Get a player's inventory with lore.", "inventory [player] [profile] <slot:number>")
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
				new HelpData("pets", "Get a player's pets.", "pets [player] [profile]").addPlayerExamples().setCategory("inventory"),
				// Misc
				new HelpData("roles", "Main roles command.")
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
					.setCategory("miscellaneous"),
				new HelpData("profiles", "Get information about all of a player's profiles.", "missing [player] [profile]")
					.addPlayerExamples()
					.setCategory("miscellaneous"),
				new HelpData("cakes", "Get a player's active and inactive cake buffs.", "cakes [player] [profile]")
					.addPlayerExamples()
					.setCategory("miscellaneous"),
				new HelpData("bestiary", "Get bestiary stats.", "bestiary [player] [profile]")
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
					"leaderboard <type> [u:player] [mode:all|ironman|stranded] [page:page] [rank:rank] [amount:amount]"
				)
					.setCategory("miscellaneous"),
				new HelpData("skyblock", "Get an overview of a player's Skyblock statistics", "skyblock [player] [profile]")
					.addPlayerExamples()
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
						new HelpData("leaderboard", "Get the leaderboard for current event."),
						new HelpData("end", "Force end the event."),
						new HelpData("cancel", "Cancel the event. No prizes or winners will be announced.")
					)
					.setCategory("event"),
				// Settings
				new HelpData("settings", "Main settings command.")
					.setCategory("settings")
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

		CustomPaginator.Builder paginateBuilder = event.getPaginator().updateExtras(extra -> extra.setTitles(pageTitles));

		paginateBuilder.addItems(
			"Use the arrow buttons to navigate through the pages" + generatePageMap() + "\n\n<> = required [] = optional"
		);

		paginateBuilder.addItems(
			create("help [command]", "Show the help menu for all commands or a specific command") +
			create("information", "Show information and statistics about the bot") +
			create("invite", "Get the invite link and Discord link for the bot") +
			create("link <player>", "Link your Hypixel account to the bot") +
			create("unlink", "Unlink your account from the bot")
		);

		paginateBuilder.addItems(
			create("slayer [player] [profile]", "Get the slayer data of a player") +
			create(
				"calcslayer <type> [player] [profile] [level] [xp]",
				"Calculate the number of bosses needed to reach a level or xp amount"
			)
		);

		paginateBuilder.addItems(
			create("skills [player] [profile]", "Get the skills data of a player") +
			create("hotm [player] [profile]", "Get a player's heart of the mountain statistics") +
			create("forge [player] [profile]", "Get a player's forge items & ending times") +
			create("crimson [player] [profile]", "Get the crimson isle stats of a player")
		);

		paginateBuilder.addItems(
			create("dungeons [player] [profile]", "Get the dungeons data of a player") +
			create("essence upgrade <item>", "Interactive message to find the essence amount to upgrade an item") +
			create("essence information <item>", "Get the amount of essence to upgrade an item for each level") +
			create("essence [player] [profile]", "Get a player's essence amounts and essence shop upgrades") +
			create(
				"calcruns <level> <floor> [player] [profile]",
				"Calculate the number of runs needed to reach a certain catacombs level"
			) +
			create("calcdrops <floor> [luck] [accessory]", "Calculate the drop rate and cost of all chests for a floor")
		);

		paginateBuilder.addItems(
			create("guild information [player] [guild]", "Get information and statistics about a guild") +
			create("guild members [player] [guild]", "Get a list of all members in a guild") +
			create("guild experience [player] [guild]", "Get the experience leaderboard for a guild") +
			create("guild-leaderboard <type> [player] [guild] [mode] [key]", "Get a leaderboard for a player's guild") +
			create(
				"guild-kicker <requirements> [player] [gamemode] [key]",
				"Get all player's who don't meet the provided requirements. The requirement name can be skills, slayer, catacombs, or weight. The requirement value must be an integer"
			) +
			create(
				"guild-ranks [player] [gamemode] [key]",
				"A customizable helper that will tell you who to kick, promote or demote in your Hypixel guild"
			) +
			create(
				"guild-statistics [player] [guild] [gamemode] [key]",
				"Get a guild's SkyBlock statistics of slayer, skills, catacombs, and weight"
			) +
			create("check-guild-api [player] [exclude]", "Get which Skyblock APIs players have enabled or disabled for a guild")
		);

		paginateBuilder.addItems(
			create("auctions [player] [filter] [sort] [verbose]", "Get a player's unclaimed auctions on all profiles") +
			create("viewauction <uuid>", "Get an auction by its UUID") +
			create("bin <item>", "Get the lowest bin of an item") +
			create("bazaar <item>", "Get bazaar prices of an item") +
			create("average <item>", "Get the average auction price of an item") +
			create("bids [player]", "Get a player's auction house bids") +
			create("price <item> [auction_type]", "Query the auction house for the lowest bin of an item") +
			create("bits <item>", "Get the bits cost of an item from the bits shop") +
			create("coinsperbit <item>", "Get the coins to bits ratio for items in the bits shop")
		);

		paginateBuilder.addItems(
			create("inventory emoji [player] [profile]", "Get a player's inventory represented in emojis") +
			create("inventory list [player] [profile] [slot]", "Get a player's inventory with lore") +
			create("armor emoji [player] [profile] [slot]", "Get a player's equipped armor & equipment represented in emojis") +
			create("armor list [player] [profile]", "Get a player's equipped armor & equipment with lore") +
			create("enderchest emoji [player] [profile]", "Get a player's ender chest represented in emojis") +
			create("enderchest list [player] [profile] [slot]", "Get a player's enderchest lore") +
			create("storage [player] <profile]", "Get a player's storage represented in emojis") +
			create("talisman emoji [player] [profile]", "Get a player's talisman bag represented in emojis") +
			create("talisman list [player] [profile] [slot]", "Get a player's talisman bag with lore") +
			create("talisman tuning [player] [profile]", "Get a player's power stone stats and tuning stats") +
			create("sacks [player] [profile] [npc]", "Get a player's sacks' content bag represented in a list") +
			create("wardrobe emoji [player] [profile]", "Get a player's wardrobe armors represented in emojis") +
			create("wardrobe list [player] [profile]", "Get a player's wardrobe armors represented in a list") +
			create("pets [player] [profile]", "Get a player's pets")
		);

		paginateBuilder.addItems(
			create("roles claim [profile]", "Claim your automatic Skyblock roles based on your statistics") +
			create("roles list", "List all roles that can be claimed through the bot") +
			create("coins total [player] [profile]", "Get a player's bank and purse coins") +
			create("coins history [player] [profile]", "Get a player's bank transaction history") +
			create("networth [player] [profile] [verbose]", "Calculate a player's networth") +
			create("weight [player] [profile]", "Get a player's slayer, skills, dungeons, and total weight") +
			create(
				"calcweight <type> <amount> [player] [profile] [system]",
				"Calculate predicted weight change for a reaching certain skill, slayer, or catacombs level/amount"
			) +
			create("hypixel [player]", "Get Hypixel information about a player") +
			create("profiles [player]", "Get information about all of a player's profiles") +
			create("missing [player] [profile]", "Get a player's missing talismans") +
			create("fetchur", "Get the item that fetchur wants today") +
			create("cakes [player] [profile]", "Get a player's inactive and active cake buffs") +
			create("bestiary [player] [profile]", "Get a player's bestiary stats") +
			create("harp [player] [profile]", "Get a player's harp statistics") +
			create("uuid [player]", "Convert username to UUID or UUID to username") +
			create("calendar", "Get the current Skyblock datetime and running or upcoming events") +
			create("scammer [player]", "Check if a player is marked as a scamer in the SBZ database") +
			create("jacob [crop]", "Get the upcoming contests and their crops") +
			create("mayor", "Get information about the current mayor or the running election") +
			create("bingo [player]", "Get the current bingo goals and a player's bingo card") +
			create("leaderboard <type> [player] [gamemode] [page] [rank] [amount]", "Get a global leaderboard") +
			create("skyblock [player] [profile]", "Get an overview of a player's Skyblock statistics") +
			create("recipe <item>", "Get the crafting recipe of an item") +
			create("check-api [player]", "Check which Skyblock APIs a player has enabled or disabled") +
			create("calcdrags [position] [ratio] [eyes]", "Calculate loot quality and loot from dragons in the end") +
			create("reforge <stone>", "Get the reforge stone stats for each rarity") +
			create("collections [player] [profile]", "Get a player's island collection counts")
		);

		paginateBuilder.addItems(
			create("party create", "Interactive message to create a party") +
			create("party current", "Get information about your current party") +
			create("party join <username>", "Join a party") +
			create("party leave", "Leave your current party") +
			create("party list", "List all active parties") +
			create("party disband", "Disband your party") +
			create("party kick <username>", "Kick a member from your party")
		);

		paginateBuilder.addItems(
			create("event create", "Interactive message to create a Skyblock event") +
			create("event current", "Get information about the current event") +
			create("event join [profile]", "Join the current event") +
			create("event add <player> [profile]", "Force add a player to the event") +
			create("event leave", "Leave the current event") +
			create("event leaderboard", "Get the leaderboard for current event") +
			create("event end", "Force end the event") +
			create("event cancel", "Cancel the event. No announcement will be made")
		);

		paginateBuilder.addItems(
			create("settings", "View the current settings for the Discord server") +
			create("settings general", "View the bot's general settings for this server") +
			create("setup", "A short walk-through on how to setup the bot") +
			create("categories", "Get the name and id of all categories in this server") +
			create("reload", "Reload the guild application and verification settings") +
			create("settings set hypixel_key <key>", "Set a Hypixel API key for this server") +
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
			create("settings set log_channel <#channel>", "Set the action log channel") +
			create("settings bot_manager add <@role>", "Add a bot manager role") +
			create("settings bot_manager remove <@role>", "Remove a bot manager role") +
			create("settings delete hypixel_key", "Delete the set Hypixel API key of this server") +
			create("settings delete all", "Delete the current server settings") +
			create("settings channel_blacklist add <#channel>", "Blacklist a channel from running bot commands") +
			create("settings channel_blacklist remove <#channel>", "Unblacklist a channel from running bot commands") +
			create("settings blacklist", "List all players that are blacklisted") +
			create("settings blacklist add <player> [reason]", "Blacklist a player from using the application system on this server") +
			create("settings blacklist remove <player>", "Remove a player from the blacklist") +
			create("settings blacklist search <player>", "Remove a player from the blacklist") +
			create("settings blacklist share <server_id>", "Share your blacklist with another server") +
			create("settings blacklist unshare <server_id>", "Stop sharing your blacklist with another server") +
			create("settings blacklist use <server_id>", "Use a shared blacklist from another server") +
			create("settings blacklist stop_using <server_id>", "Stop using a shared blacklist from another server")
		);

		paginateBuilder.addItems(
			create("settings jacob", "View the current settings for farming event notifications") +
			create("settings jacob <enable|disable>", "Enable or disable farming event notifications") +
			create("settings jacob channel <#channel>", "Set the channel where farming event notifications will be sent") +
			create("settings jacob add <crop|all> [@role]", "Added a crop to be tracked. Role will automatically be created") +
			create("settings jacob remove <crop>", "Remove a crop from the tracking list")
		);

		paginateBuilder.addItems(
			create("settings event", "View the current settings for event notifications") +
			create("settings event <enable|disable>", "Enable or disable event notifications") +
			create("settings event channel <#channel>", "Set the channel where event notifications will be sent") +
			create("settings event add <event|all> [@role]", "Added an event to be notified for") +
			create("settings event remove <event>", "Remove an event from the notification list")
		);

		paginateBuilder.addItems(
			create("settings verify", "Get the current verify settings for the bot") +
			create("settings verify <enable|disable>", "Enable or disable automatic verify") +
			create("settings verify message <message>", "The message that users will see when verifying") +
			create("settings verify role add <@role>", "Add a role that user will receive upon being verified") +
			create("settings verify role remove <@role>", "Remove a verify role") +
			create(
				"settings verify channel <#channel>",
				"Channel where the verify message will be sent and messages will be auto deleted"
			) +
			create("settings verify nickname [prefix] [IGN] [postfix]", "The nickname template on verifying. Can be set to none") +
			create("settings verify remove_role <@role>", "Role that will be removed on verifying and re-added when un-verifying") +
			create("settings verify sync <enable|disable>", "Enable or disable automatic verify role and nickname syncing") +
			create("settings verify dm_on_sync <enable|disable>", "Enable or disable DMing the user on syncing") +
			create("settings verify roles_claim <enable|disable>", "Enable or disable automatic role syncing")
		);

		paginateBuilder.addItems(
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

		paginateBuilder.addItems(
			create("settings guild <name> apply <enable|disable>", "Enable or disable automatic apply") +
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
				"settings guild <name> apply gamemode <true|false>",
				"Whether applicants must use a certain gamemode profile in their application. Defaults to 'all'"
			) +
			create("settings guild <name> apply deny_message <message>", "Message that will be sent if applicant is denied") +
			create(
				"settings guild <name> apply requirements add [slayer:amount] [skills:amount] [catacombs:amount] [weight:amount]",
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

		paginateBuilder.addItems(
			create("settings roles", "Get the current roles settings for the bot") +
			create("settings roles <enable|disable>", "Enable or disable automatic roles") +
			create(
				"settings roles use_highest <enable|disable>",
				"Enable or disable using the highest values or last played on profile. Default is false"
			) +
			create(
				"settings roles <enable|disable> <role_name|all>",
				"Enable or disable a specific automatic role or enable or disable all applicable automatic roles"
			) +
			create("settings roles add <role_name> <value> <@role>", "Add a new level to a role with its corresponding Discord role") +
			create("settings roles remove <role_name> <value>", "Remove a role level for a role") +
			create("settings roles set <role_name> <@role>", "Set a one level role's role")
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

	private static String create(String commandName, String desc) {
		return "`/" + commandName + "`: " + desc + "\n";
	}
}
