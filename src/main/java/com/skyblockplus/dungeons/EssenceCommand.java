package com.skyblockplus.dungeons;

import static com.skyblockplus.Main.jda;
import static com.skyblockplus.utils.Constants.essenceItemNames;
import static com.skyblockplus.utils.Utils.*;

import com.google.gson.JsonElement;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.skyblockplus.utils.command.CommandExecute;
import java.util.Locale;
import net.dv8tion.jda.api.EmbedBuilder;

public class EssenceCommand extends Command {

	public EssenceCommand() {
		this.name = "essence";
		this.cooldown = globalCooldown;
	}

	public static EmbedBuilder getEssenceInformation(String itemName) {
		JsonElement essenceCostsJson = getEssenceCostsJson();

		String preFormattedItem = itemName.replace("'s", "").replace(" ", "_").toUpperCase();
		preFormattedItem = nameToId(preFormattedItem);

		if (higherDepth(essenceCostsJson, preFormattedItem) == null) {
			String closestMatch = getClosestMatch(preFormattedItem, essenceItemNames);
			preFormattedItem = closestMatch != null ? closestMatch : preFormattedItem;
		}

		JsonElement itemJson = higherDepth(essenceCostsJson, preFormattedItem);

		EmbedBuilder eb = defaultEmbed("Essence information for " + itemName);
		if (itemJson != null) {
			String essenceType = higherDepth(itemJson, "type").getAsString().toLowerCase(Locale.ROOT);
			for (String level : getJsonKeys(itemJson)) {
				switch (level) {
					case "type":
						eb.setDescription("**Essence Type**: " + capitalizeString(essenceType) + " essence");
						break;
					case "dungeonize":
						eb.addField("Dungeonize item", higherDepth(itemJson, level).getAsString() + " " + essenceType + " essence", false);
						break;
					case "1":
						eb.addField(level + " star", higherDepth(itemJson, level).getAsString() + " " + essenceType + " essence", false);
						break;
					default:
						eb.addField(level + " stars", higherDepth(itemJson, level).getAsString() + " " + essenceType + " essence", false);
						break;
				}
			}
			eb.setThumbnail("https://sky.shiiyu.moe/item.gif/" + preFormattedItem);
			return eb;
		}
		return defaultEmbed("Invalid item name");
	}

	@Override
	protected void execute(CommandEvent event) {
		new CommandExecute(this, event) {
			@Override
			protected void execute() {
				logCommand();
				setArgs(3);

				if (args.length == 3 && args[1].equals("upgrade")) {
					String itemName = nameToId(args[2]);

					if (higherDepth(getEssenceCostsJson(), itemName) == null) {
						String closestMatch = getClosestMatch(itemName, essenceItemNames);
						itemName = closestMatch != null ? closestMatch : itemName;
					}

					JsonElement itemJson = higherDepth(getEssenceCostsJson(), itemName);
					if (itemJson != null) {
						jda.addEventListener(new EssenceWaiter(itemName, itemJson, ebMessage, event.getAuthor()));
					} else {
						embed(invalidEmbed("Invalid item name"));
					}
					return;
				} else if (args.length == 3 && (args[1].equals("info") || args[1].equals("information"))) {
					embed(getEssenceInformation(args[2]));
					return;
				}

				sendErrorEmbed();
			}
		}
			.submit();
	}
}
