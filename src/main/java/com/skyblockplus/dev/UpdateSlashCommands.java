package com.skyblockplus.dev;

import static com.skyblockplus.Main.jda;
import static com.skyblockplus.utils.Utils.defaultEmbed;
import static com.skyblockplus.utils.Utils.logCommand;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
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
		new Thread(
			() -> {
				String content = event.getMessage().getContentRaw();

				logCommand(event.getGuild(), event.getAuthor(), content);

				CommandListUpdateAction slashCommands = jda.getGuildById("796790757947867156").updateCommands();
				if (content.split(" ").length == 2 && content.split(" ")[1].equals("clear")) {} else {
					slashCommands.addCommands(
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
						new CommandData("guild", "A party finder helper that shows a player's dungeon stats")
							.addSubcommands(
								new SubcommandData("player", "Find what guild a player is in")
									.addOption(OptionType.STRING, "player", "Player username", true),
								new SubcommandData("information", "Get information and statistics about a player's guild")
									.addOption(OptionType.STRING, "player", "Player username", true),
								new SubcommandData("members", "Get a list of all members in a player's guild")
									.addOption(OptionType.STRING, "player", "Player username", true),
								new SubcommandData("experience", "Get the experience leaderboard for a player's guild")
									.addOption(OptionType.STRING, "player", "Player username", true)
							),
						new CommandData("help", "Show the help page for this bot")
							.addOption(OptionType.STRING, "page", "Page number or name of a command"),
						new CommandData("auctions", "Get player's active (not claimed) auctions on all profiles")
							.addOption(OptionType.STRING, "player", "Player username", true),
						new CommandData("bin", "Get the lowest bin of an item").addOption(OptionType.STRING, "item", "Item name", true),
						new CommandData("bazaar", "Get bazaar prices of an item").addOption(OptionType.STRING, "item", "Item name", true),
						new CommandData("average", "Get the average auction price of an item")
							.addOption(OptionType.STRING, "item", "Item name", true),
						new CommandData("bids", "Get a player's bids").addOption(OptionType.STRING, "player", "Player username", true),
						new CommandData("query", "Query the auction house for the lowest bin of an item")
							.addOption(OptionType.STRING, "item", "Item name", true),
						new CommandData("bits", "Get the price of an item from the bits shop")
							.addOption(OptionType.STRING, "item", "Item name", true)
					);
				}

				int size = slashCommands.complete().size();
				event.getChannel().sendMessage(defaultEmbed("Success - total of " + size + " commands").build()).queue();
			}
		)
			.start();
	}
}
