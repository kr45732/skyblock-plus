package com.SkyblockBot.Dungeons;

import com.google.gson.JsonElement;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import static com.SkyblockBot.TimeoutHandler.MessageTimeout.addMessage;
import static com.SkyblockBot.Utils.BotUtils.defaultEmbed;
import static com.SkyblockBot.Utils.BotUtils.higherDepth;

public class EssenceWaiter extends ListenerAdapter {
    final String itemName;
    final JsonElement itemJson;
    final Message reactMessage;
    final User user;
    int startingLevel;
    int endingLevel;
    int state = 0;
    final ArrayList<String> validReactions;
    final Map<String, Integer> essenceEmojiMap = new HashMap<>();
    final Map<Integer, String> emojiEssenceMap;

    public EssenceWaiter(String itemName, JsonElement itemJson, Message reactMessage, User user) {
        this.itemName = itemName;
        this.itemJson = itemJson;
        this.reactMessage = reactMessage;
        this.user = user;
        addMessage(this.reactMessage, this);

        essenceEmojiMap.put("⏫", -1);
        essenceEmojiMap.put("0⃣", 0);
        essenceEmojiMap.put("1⃣", 1);
        essenceEmojiMap.put("2⃣", 2);
        essenceEmojiMap.put("3⃣", 3);
        essenceEmojiMap.put("4⃣", 4);
        essenceEmojiMap.put("5⃣", 5);
        emojiEssenceMap = essenceEmojiMap.entrySet().stream().collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey));


        EmbedBuilder eb = defaultEmbed("Essence upgrade for " + itemName.toLowerCase().replace("_", " "), null);
        eb.setDescription("Choose the current item level");

        validReactions = new ArrayList<>();
        String initialMessageInfo = "";
        if (higherDepth(itemJson, "dungeonize") != null) {
            validReactions.add("⏫");
            initialMessageInfo += "⏫ - Not dungeonized\n";
        }
        eb.addField("Levels", initialMessageInfo + "0⃣ - 0 stars\n1⃣ - 1 star\n2⃣ - 2 stars\n3⃣ - 3 stars\n4⃣ - 4 stars", false);
        reactMessage.editMessage(eb.build()).queue();

        validReactions.add("0⃣");
        validReactions.add("1⃣");
        validReactions.add("2⃣");
        validReactions.add("3⃣");
        validReactions.add("4⃣");
        for (String i : validReactions) {
            reactMessage.addReaction(i).queue();
        }

    }

    @Override
    public void onMessageReactionAdd(@NotNull MessageReactionAddEvent event) {
        if (event.getMessageIdLong() != reactMessage.getIdLong()) {
            return;
        }
        if (event.getUser().isBot()) {
            return;
        }

        if (!event.getUser().equals(user)) {
            reactMessage.removeReaction(event.getReaction().getReactionEmote().getAsReactionCode(), event.getUser()).queue();
            return;
        }

        if (!validReactions.contains(event.getReactionEmote().getName())) {
            reactMessage.removeReaction(event.getReaction().getReactionEmote().getAsReactionCode(), event.getUser()).queue();
            return;
        }

        switch (state) {
            case 0: {
                validReactions.clear();
                startingLevel = essenceEmojiMap.get(event.getReactionEmote().getName());
                reactMessage.clearReactions().complete();
                EmbedBuilder eb = defaultEmbed("Essence upgrade for " + itemName.toLowerCase().replace("_", " "), null);
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

                reactMessage.editMessage(eb.build()).queue();
                state = 1;
                break;
            }
            case 1: {
                endingLevel = essenceEmojiMap.get(event.getReactionEmote().getName());
                reactMessage.clearReactions().complete();
                int totalEssence = 0;
                for (int i = (startingLevel + 1); i <= endingLevel; i++) {
                    if (i == 0) {
                        totalEssence += higherDepth(itemJson, "dungeonize").getAsInt();
                    } else {
                        totalEssence += higherDepth(itemJson, "" + i).getAsInt();
                    }
                }
                EmbedBuilder eb = defaultEmbed("Essence upgrade for " + itemName.toLowerCase().replace("_", " "), null);
                eb.addField("From " + (startingLevel == -1 ? "not dungeonized" : startingLevel + (startingLevel == 1 ? " star" : " stars")) + " to " + endingLevel + (endingLevel == 1 ? " star" : " stars"), totalEssence + " " + higherDepth(itemJson, "type").getAsString().toLowerCase() + " essence", false);
                reactMessage.editMessage(eb.build()).queue();

                event.getJDA().removeEventListener(this);
                break;
            }
        }
    }


}
