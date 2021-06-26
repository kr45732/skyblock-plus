package com.skyblockplus.dungeons;

import static com.skyblockplus.Main.jda;
import static com.skyblockplus.utils.Utils.*;

import com.google.gson.JsonElement;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import java.util.Locale;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;

public class EssenceCommand extends Command {

	public EssenceCommand() {
		this.name = "essence";
		this.cooldown = globalCooldown;
	}

	public static EmbedBuilder getEssenceInformation(String itemName, JsonElement essenceCostsJson) {
		String preFormattedItem = itemName.replace("'s", "").replace(" ", "_").toUpperCase();
		preFormattedItem = convertToInternalName(preFormattedItem);

		if (higherDepth(essenceCostsJson, preFormattedItem) == null) {
			String closestMatch = getClosestMatch(preFormattedItem, getJsonKeys(essenceCostsJson));
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
			eb.setThumbnail("https://sky.lea.moe/item.gif/" + preFormattedItem);
			return eb;
		}
		return defaultEmbed("Invalid item name");
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

				JsonElement essenceCostsJson = getEssenceCostsJson();

				if (args.length >= 3 && args[1].equals("upgrade")) {
					String itemName = content.split(" ", 3)[2].replace("'s", "").replace(" ", "_").toUpperCase();
					itemName = convertToInternalName(itemName);

					if (higherDepth(essenceCostsJson, itemName) == null) {
						String closestMatch = getClosestMatch(itemName, getJsonKeys(essenceCostsJson));
						itemName = closestMatch != null ? closestMatch : itemName;
					}

					JsonElement itemJson = higherDepth(essenceCostsJson, itemName);
					if (itemJson != null) {
						jda.addEventListener(new EssenceWaiter(itemName, itemJson, ebMessage, event.getAuthor()));
					} else {
						eb = defaultEmbed("Invalid item name");
						ebMessage.editMessage(eb.build()).queue();
					}
					return;
				} else if (args.length >= 3 && (args[1].equals("info") || args[1].equals("information"))) {
					String itemName = content.split(" ", 3)[2];

					eb = getEssenceInformation(itemName, essenceCostsJson);
					ebMessage.editMessage(eb.build()).queue();
					return;
				}

				ebMessage.editMessage(errorEmbed(this.name).build()).queue();
			}
		)
			.start();
	}
}
