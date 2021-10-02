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

package com.skyblockplus.dungeons;

import static com.skyblockplus.Main.waiter;
import static com.skyblockplus.utils.Utils.defaultEmbed;
import static com.skyblockplus.utils.Utils.higherDepth;

import com.google.gson.JsonElement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;

public class EssenceWaiter {

	private final String itemName;
	private final JsonElement itemJson;
	private final Message reactMessage;
	private final User user;
	private final ArrayList<String> validReactions;
	private final Map<String, Integer> essenceEmojiMap = new HashMap<>();
	private final Map<Integer, String> emojiEssenceMap;
	private int startingLevel;
	private int state = 0;

	public EssenceWaiter(String itemName, JsonElement itemJson, Message reactMessage, User user) {
		this.itemName = itemName;
		this.itemJson = itemJson;
		this.reactMessage = reactMessage;
		this.user = user;

		essenceEmojiMap.put("⏫", -1);
		essenceEmojiMap.put("0⃣", 0);
		essenceEmojiMap.put("1⃣", 1);
		essenceEmojiMap.put("2⃣", 2);
		essenceEmojiMap.put("3⃣", 3);
		essenceEmojiMap.put("4⃣", 4);
		essenceEmojiMap.put("5⃣", 5);
		emojiEssenceMap = essenceEmojiMap.entrySet().stream().collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey));

		EmbedBuilder eb = defaultEmbed("Essence upgrade for " + itemName.toLowerCase().replace("_", " "));
		eb.setDescription("Choose the current item level");

		validReactions = new ArrayList<>();
		String initialMessageInfo = "";
		if (higherDepth(itemJson, "dungeonize") != null) {
			validReactions.add("⏫");
			initialMessageInfo += "⏫ - Not dungeonized\n";
		}
		eb.addField("Levels", initialMessageInfo + "0⃣ - 0 stars\n1⃣ - 1 star\n2⃣ - 2 stars\n3⃣ - 3 stars\n4⃣ - 4 stars", false);
		eb.setThumbnail("https://sky.shiiyu.moe/item.gif/" + itemName);
		reactMessage.editMessageEmbeds(eb.build()).queue();

		validReactions.add("0⃣");
		validReactions.add("1⃣");
		validReactions.add("2⃣");
		validReactions.add("3⃣");
		validReactions.add("4⃣");
		for (String i : validReactions) {
			reactMessage.addReaction(i).queue();
		}

		waiter.waitForEvent(
			MessageReactionAddEvent.class,
			this::condition,
			this::action,
			30,
			TimeUnit.SECONDS,
			() -> reactMessage.clearReactions().queue()
		);
	}

	private boolean condition(MessageReactionAddEvent event) {
		return event.getMessageIdLong() == reactMessage.getIdLong() && !event.getUser().isBot() && event.getUser().equals(user);
	}

	private void action(MessageReactionAddEvent event) {
		if (!validReactions.contains(event.getReactionEmote().getName())) {
			reactMessage.removeReaction(event.getReaction().getReactionEmote().getAsReactionCode(), event.getUser()).queue();
			return;
		}

		switch (state) {
			case 0:
				{
					validReactions.clear();
					startingLevel = essenceEmojiMap.get(event.getReactionEmote().getName());
					reactMessage.clearReactions().complete();
					EmbedBuilder eb = defaultEmbed("Essence upgrade for " + itemName.toLowerCase().replace("_", " "));
					eb.setDescription("Choose the ending item level");

					StringBuilder levelsString = new StringBuilder();
					for (int i = (startingLevel + 1); i <= 5; i++) {
						reactMessage.addReaction(emojiEssenceMap.get(i)).queue();
						validReactions.add(emojiEssenceMap.get(i));
						if (startingLevel == -1 && i == 0) {
							levelsString.append(emojiEssenceMap.get(i)).append(" - Dungeonized\n");
						} else if (i == 1) {
							levelsString.append(emojiEssenceMap.get(i)).append(" - ").append(i).append(" star\n");
						} else {
							levelsString.append(emojiEssenceMap.get(i)).append(" - ").append(i).append(" stars\n");
						}
					}
					eb.addField("Levels", levelsString.toString(), false);
					eb.setThumbnail("https://sky.shiiyu.moe/item.gif/" + itemName);

					reactMessage.editMessageEmbeds(eb.build()).queue();
					state = 1;
					break;
				}
			case 1:
				{
					int endingLevel = essenceEmojiMap.get(event.getReactionEmote().getName());
					reactMessage.clearReactions().complete();
					int totalEssence = 0;
					for (int i = (startingLevel + 1); i <= endingLevel; i++) {
						if (i == 0) {
							totalEssence += higherDepth(itemJson, "dungeonize", 0);
						} else {
							totalEssence += higherDepth(itemJson, "" + i, 0);
						}
					}
					EmbedBuilder eb = defaultEmbed("Essence upgrade for " + itemName.toLowerCase().replace("_", " "));
					eb.addField(
						"From " +
						(startingLevel == -1 ? "not dungeonized" : startingLevel + (startingLevel == 1 ? " star" : " stars")) +
						" to " +
						endingLevel +
						(endingLevel == 1 ? " star" : " stars"),
						totalEssence + " " + higherDepth(itemJson, "type").getAsString().toLowerCase() + " essence",
						false
					);
					eb.setThumbnail("https://sky.shiiyu.moe/item.gif/" + itemName);
					reactMessage.editMessageEmbeds(eb.build()).queue();

					break;
				}
		}
	}
}
