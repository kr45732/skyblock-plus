package com.skyblockplus.dev;

import static com.skyblockplus.Main.*;
import static com.skyblockplus.utils.Utils.*;
import static com.skyblockplus.utils.Utils.defaultEmbed;
import static com.skyblockplus.utils.Utils.logCommand;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;

public class PlaceholderCommand extends Command {

	public PlaceholderCommand() {
		this.name = "d-placeholder";
		this.ownerCommand = true;
		this.aliases = new String[] { "ph" };
	}

	@Override
	protected void execute(CommandEvent event) {
		new Thread(
			() -> {
				EmbedBuilder eb = loadingEmbed();
				Message ebMessage = event.getChannel().sendMessage(eb.build()).complete();
				String content = event.getMessage().getContentRaw();
				String[] args = content.split(" ");

				logCommand(event.getGuild(), event.getAuthor(), content);

				switch (args[1]) {
					case "get":
						{
							Guild guild = jda.getGuildById(args[2]);
							List<TextChannel> channels = guild.getTextChannels();
							System.out.println(guild.getName() + " - " + guild.getId() + " - " + guild.getMemberCount());
							for (TextChannel channel : channels) {
								System.out.println(channel.getName() + " - " + channel.getId());
							}
							break;
						}
					case "send_silent":
						{
							Guild guild = jda.getGuildById(args[2]);
							guild.leave().complete();
							System.out.println("Left " + guild.getName() + " - " + guild.getId());
							break;
						}
					case "list":
						{
							List<Guild> guilds = new LinkedList<>(jda.getGuilds());

							guilds.sort(Comparator.comparingInt(Guild::getMemberCount));

							for (Guild guild : guilds) {
								if (guild.getName().startsWith("Skyblock Plus - Emoji Server")) {
									continue;
								}

								System.out.println(
									guild.getName() +
									" (" +
									guild.getMemberCount() +
									") | Id: " +
									guild.getId() +
									" | Owner: " +
									guild.getOwner().getEffectiveName() +
									" (" +
									guild.getOwnerId() +
									")"
								);
							}
							break;
						}
				}

				ebMessage.editMessage(defaultEmbed("Done").build()).queue();
			}
		)
			.start();
	}
}
