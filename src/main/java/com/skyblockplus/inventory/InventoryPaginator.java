package com.skyblockplus.inventory;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionAddEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.util.List;

import static com.skyblockplus.timeout.MessageTimeout.addMessage;

public class InventoryPaginator extends ListenerAdapter {
    private final List<String[]> enderChestPages;
    private final Message pagePart1;
    private final Message pagePart2;
    private final User user;
    private final int maxPageNumber;
    private int pageNumber = 0;

    public InventoryPaginator(List<String[]> enderChestPages, MessageChannel channel, User user) {
        this.enderChestPages = enderChestPages;
        this.user = user;
        this.maxPageNumber = enderChestPages.size() - 1;

        pagePart1 = channel.sendMessage(enderChestPages.get(0)[0]).complete();
        pagePart2 = channel.sendMessage(enderChestPages.get(0)[1]).complete();
        pagePart2.addReaction("◀️").queue();
        pagePart2.addReaction("▶️").queue();
        addMessage(pagePart2, this);
    }

    @Override
    public void onGuildMessageReactionAdd(GuildMessageReactionAddEvent event) {
        if (event.getUser().isBot()) {
            return;
        }

        if (!event.getGuild().getId().equals(pagePart2.getGuild().getId())) {
            return;
        }

        if (!event.getMessageId().equals(pagePart2.getId())) {
            if (event.getMessageId().equals(pagePart1.getId())) {
                pagePart1.clearReactions().queue();
            }
            return;
        }

        if (!event.getUser().equals(user)) {
            pagePart2.removeReaction(event.getReaction().getReactionEmote().getAsReactionCode(), event.getUser())
                    .queue();
            return;
        }

        if (event.getReaction().getReactionEmote().getAsReactionCode().equals("◀️")) {
            if ((pageNumber - 1) >= 0) {
                pageNumber -= 1;
            }
        } else if (event.getReaction().getReactionEmote().getAsReactionCode().equals("▶️")) {
            if ((pageNumber + 1) <= maxPageNumber) {
                pageNumber += 1;
            }
        }

        pagePart1.editMessage(enderChestPages.get(pageNumber)[0]).complete();
        pagePart2.editMessage(enderChestPages.get(pageNumber)[1]).complete();

        pagePart2.removeReaction("◀️", user).queue();
        pagePart2.removeReaction("▶️", user).queue();
    }
}
