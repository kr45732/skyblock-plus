package com.skyblockplus.price;

import static com.skyblockplus.utils.Utils.*;

import com.google.gson.JsonElement;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;

public class BitsCommand extends Command {

	public BitsCommand() {
		this.name = "bits";
		this.cooldown = globalCooldown;
		this.aliases = new String[] { "bit" };
	}

	public static EmbedBuilder getBitPrices(String itemName) {
		JsonElement bitsJson = getBitsJson();

		if (higherDepth(bitsJson, itemName) != null) {
			return defaultEmbed("Bits Price").addField(itemName, formatNumber(higherDepth(bitsJson, itemName).getAsLong()), false);
		}

		String closestMatch = getClosestMatch(itemName, getJsonKeys(bitsJson));

		if (closestMatch != null && higherDepth(bitsJson, closestMatch) != null) {
			return defaultEmbed("Bits Price").addField(closestMatch, formatNumber(higherDepth(bitsJson, closestMatch).getAsLong()), false);
		}

		return defaultEmbed("No bit price found for " + capitalizeString(itemName));
	}

	@Override
	protected void execute(CommandEvent event) {
		executor.submit(
			() -> {
				EmbedBuilder eb = loadingEmbed();
				Message ebMessage = event.getChannel().sendMessage(eb.build()).complete();
				String content = event.getMessage().getContentRaw();
				String[] args = content.split(" ", 2);

				logCommand(event.getGuild(), event.getAuthor(), content);

				if (args.length == 2) {
					ebMessage.editMessage(getBitPrices(args[1]).build()).queue();
					return;
				}

				ebMessage.editMessage(errorEmbed(this.name).build()).queue();
			}
		);
	}
}
