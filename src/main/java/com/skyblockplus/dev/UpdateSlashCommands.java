package com.skyblockplus.dev;

import static com.skyblockplus.Main.jda;
import static com.skyblockplus.utils.Utils.defaultEmbed;
import static com.skyblockplus.utils.Utils.errorEmbed;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.skyblockplus.utils.command.CommandExecute;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.requests.restaction.CommandListUpdateAction;

public class UpdateSlashCommands extends Command {

	public UpdateSlashCommands() {
		this.name = "d-slash";
		this.ownerCommand = true;
	}

	@Override
	protected void execute(CommandEvent event) {
		new CommandExecute(this, event, false) {
			@Override
			protected void execute() {
				logCommand();

				if (args.length == 1) {
					CommandListUpdateAction slashCommands = jda.getGuildById(event.getGuild().getId()).updateCommands();
					slashCommands.addCommands(generateSlashCommands()).queue();
					event
						.getChannel()
						.sendMessageEmbeds(
							defaultEmbed("Success - added " + slashCommands.complete().size() + " slash commands for this guild").build()
						)
						.queue();
					return;
				} else if (args.length == 2) {
					if (args[1].equals("clear")) {
						CommandListUpdateAction slashCommands = jda.getGuildById(event.getGuild().getId()).updateCommands();
						slashCommands.queue();
						event.getChannel().sendMessageEmbeds(defaultEmbed("Success - cleared commands for this guild").build()).queue();
						return;
					} else if (args[1].equals("global")) {
						CommandListUpdateAction slashCommands = jda.updateCommands();
						slashCommands.addCommands(generateSlashCommands()).queue();
						event
							.getChannel()
							.sendMessageEmbeds(
								defaultEmbed("Success - added " + slashCommands.complete().size() + " slash commands globally").build()
							)
							.queue();
						return;
					}
				}

				event.getChannel().sendMessageEmbeds(errorEmbed(name).build()).queue();
			}
		}
			.submit();
	}

	private CommandData[] generateSlashCommands() {
		return new CommandData[] {
			new CommandData("information", "Get information about this bot"),
			new CommandData("invite", "Invite this bot to your server"),
			new CommandData("link", "Get what Hypixel account you are linked to")
				.addOption(OptionType.STRING, "player", "Link your Hypixel account to this bot"),
			new CommandData("unlink", "Unlink your account from this bot"),
			new CommandData("slayer", "Get the slayer data of a player")
				.addOption(OptionType.STRING, "player", "Player username", true)
				.addOption(OptionType.STRING, "profile", "Profile name"),
			new CommandData("skills", "Get the skills data of a player")
				.addOption(OptionType.STRING, "player", "Player username", true)
				.addOption(OptionType.STRING, "profile", "Profile name"),
			new CommandData("dungeons", "Get the dungeons data of a player")
				.addOption(OptionType.STRING, "player", "Player username", true)
				.addOption(OptionType.STRING, "profile", "Profile name"),
			new CommandData("essence", "Get essence upgrade information for an item")
				.addSubcommands(
					new SubcommandData("upgrade", "Interactive message to find the essence amount to upgrade an item")
						.addOption(OptionType.STRING, "item", "Item name", true),
					new SubcommandData("information", "Get the amount of essence to upgrade an item for each level")
						.addOption(OptionType.STRING, "item", "Item name", true)
				),
			new CommandData("partyfinder", "A party finder helper that shows a player's dungeon stats")
				.addOption(OptionType.STRING, "player", "Player username", true)
				.addOption(OptionType.STRING, "profile", "Profile name"),
			new CommandData("guild", "Main guild command")
				.addSubcommands(
					new SubcommandData("player", "Find what guild a player is in")
						.addOption(OptionType.STRING, "player", "Player username", true),
					new SubcommandData("information", "Get information and statistics about a player's guild")
						.addOption(OptionType.STRING, "player", "Player username", true),
					new SubcommandData("members", "Get a list of all members in a player's guild")
						.addOption(OptionType.STRING, "player", "Player username", true),
					new SubcommandData("experience", "Get the experience leaderboard for a player's guild")
						.addOption(OptionType.STRING, "player", "Player username", true)
						.addOption(OptionType.INTEGER, "days", "Number of days", false)
				),
			new CommandData("help", "Show the help page for this bot")
				.addOption(OptionType.STRING, "page", "Page number or name of a command"),
			new CommandData("auctions", "Main auctions command")
				.addSubcommands(
					new SubcommandData("player", "Get player's active (not claimed) auctions on all profiles")
						.addOption(OptionType.STRING, "player", "Player username", true),
					new SubcommandData("uuid", "Get an auction by it's UUID").addOption(OptionType.STRING, "uuid", "Auction UUID", true)
				),
			new CommandData("bin", "Get the lowest bin of an item").addOption(OptionType.STRING, "item", "Item name", true),
			new CommandData("bazaar", "Get bazaar prices of an item").addOption(OptionType.STRING, "item", "Item name", true),
			new CommandData("average", "Get the average auction price of an item").addOption(OptionType.STRING, "item", "Item name", true),
			new CommandData("bids", "Get a player's bids").addOption(OptionType.STRING, "player", "Player username", true),
			new CommandData("query", "Query the auction house for the lowest bin of an item")
				.addOption(OptionType.STRING, "item", "Item name", true),
			new CommandData("bits", "Get the price of an item from the bits shop").addOption(OptionType.STRING, "item", "Item name", true),
			new CommandData("roles", "Main roles command")
				.addSubcommands(
					new SubcommandData("claim", "Claim automatic Skyblock roles. The player must be linked to the bot")
						.addOption(OptionType.STRING, "profile", "Profile name")
				),
			new CommandData("bank", "Main bank command")
				.addSubcommands(
					new SubcommandData("total", "Get a player's bank and purse coins")
						.addOption(OptionType.STRING, "player", "Player username", true)
						.addOption(OptionType.STRING, "profile", "Profile name")
				)
				.addSubcommands(
					new SubcommandData("history", "Get a player's bank transaction history")
						.addOption(OptionType.STRING, "player", "Player username", true)
						.addOption(OptionType.STRING, "profile", "Profile name")
				),
			new CommandData("weight", "Main weight command")
				.addSubcommands(
					new SubcommandData("player", "Get a player's weight")
						.addOption(OptionType.STRING, "player", "Player username", true)
						.addOption(OptionType.STRING, "profile", "Profile name")
				)
				.addSubcommands(
					new SubcommandData("calculate", "Calculate predicted weight using given stats (not 100% accurate)")
						.addOption(OptionType.STRING, "skill_average", "Player's skill average", true)
						.addOption(OptionType.STRING, "slayer", "Player's slayer XP", true)
						.addOption(OptionType.STRING, "dungeons", "Player's catacombs level", true)
						.addOption(OptionType.STRING, "average_class", "Player's average dungeon class level", true)
				),
			new CommandData("hypixel", "Main hypixel command")
				.addSubcommands(
					new SubcommandData("player", "Get Hypixel information about a player")
						.addOption(OptionType.STRING, "player", "Player username", true)
				)
				.addSubcommands(
					new SubcommandData("parkour", "Get fastest Hypixel lobby parkour for a player")
						.addOption(OptionType.STRING, "player", "Player username", true)
				),
			new CommandData("profiles", "Get a information about all of a player's profiles")
				.addOption(OptionType.STRING, "player", "Player username", true),
			new CommandData("missing", "Get a player's missing talismans")
				.addOption(OptionType.STRING, "player", "Player username", true)
				.addOption(OptionType.STRING, "profile", "Profile name"),
			new CommandData("price", "Calculate the price of an auction").addOption(OptionType.STRING, "uuid", "Auction UUID", true),
		};
	}
}
