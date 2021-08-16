package com.skyblockplus.price;

import static com.skyblockplus.utils.Constants.bitsItemNames;
import static com.skyblockplus.utils.Utils.*;

import com.google.gson.JsonElement;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.skyblockplus.utils.command.CommandExecute;
import net.dv8tion.jda.api.EmbedBuilder;

public class BitsCommand extends Command {

	public BitsCommand() {
		this.name = "bits";
		this.cooldown = globalCooldown;
		this.aliases = new String[] { "bit" };
	}

	public static EmbedBuilder getBitPrices(String itemName) {
		JsonElement bitsJson = getBitsJson();

		String closestMatch = getClosestMatch(itemName, bitsItemNames);
		if (closestMatch != null) {
			return defaultEmbed("Bits Price").addField(closestMatch, formatNumber(higherDepth(bitsJson, closestMatch, 0L)), false);
		}

		return defaultEmbed("No bit price found for " + capitalizeString(itemName));
	}

	@Override
	protected void execute(CommandEvent event) {
		new CommandExecute(this, event) {
			@Override
			protected void execute() {
				logCommand();
				setArgs(2);

				if (args.length == 2) {
					embed(getBitPrices(args[1]));
					return;
				}

				sendErrorEmbed();
			}
		}
			.submit();
	}
}
