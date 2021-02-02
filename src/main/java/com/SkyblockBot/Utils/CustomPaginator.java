package com.SkyblockBot.Utils;

import com.jagrosh.jdautilities.commons.waiter.EventWaiter;
import com.jagrosh.jdautilities.menu.Menu;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.exceptions.PermissionException;
import net.dv8tion.jda.api.requests.RestAction;
import net.dv8tion.jda.internal.utils.Checks;

import java.awt.*;
import java.time.Instant;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import java.util.function.Consumer;


public class CustomPaginator extends Menu {
    public static final String BIG_LEFT = "\u23EA";
    public static final String LEFT = "\u25C0";
    public static final String RIGHT = "\u25B6";
    public static final String BIG_RIGHT = "\u23E9";
    private final BiFunction<Integer, Integer, Color> color;
    private final int columns;
    private final int itemsPerPage;
    private final boolean showPageNumbers;
    private final boolean numberItems;
    private final List<String> strings;
    private final int pages;
    private final Consumer<Message> finalAction;
    private final int bulkSkipNumber;
    private final boolean wrapPageEnds;
    private final String[] pageTitles;
    private final User user;

    CustomPaginator(EventWaiter waiter, Set<User> users, Set<Role> roles, long timeout, TimeUnit unit,
                    BiFunction<Integer, Integer, Color> color,
                    Consumer<Message> finalAction, int columns, int itemsPerPage, boolean showPageNumbers,
                    boolean numberItems, List<String> items, int bulkSkipNumber,
                    boolean wrapPageEnds, String[] pageTitles, User user) {
        super(waiter, users, roles, timeout, unit);
        this.color = color;
        this.columns = columns;
        this.itemsPerPage = itemsPerPage;
        this.showPageNumbers = showPageNumbers;
        this.numberItems = numberItems;
        this.strings = items;
        this.pages = (int) Math.ceil((double) strings.size() / itemsPerPage);
        this.finalAction = finalAction;
        this.bulkSkipNumber = bulkSkipNumber;
        this.wrapPageEnds = wrapPageEnds;
        this.pageTitles = pageTitles;
        this.user = user;
    }

    @Override
    public void display(MessageChannel channel) {
        paginate(channel, 1);
    }

    @Override
    public void display(Message message) {
        paginate(message, 1);
    }

    public void paginate(MessageChannel channel, int pageNum) {
        if (pageNum < 1)
            pageNum = 1;
        else if (pageNum > pages)
            pageNum = pages;
        Message msg = renderPage(pageNum);
        initialize(channel.sendMessage(msg), pageNum);
    }

    public void paginate(Message message, int pageNum) {
        if (pageNum < 1)
            pageNum = 1;
        else if (pageNum > pages)
            pageNum = pages;
        Message msg = renderPage(pageNum);
        initialize(message.editMessage(msg), pageNum);
    }

    private void initialize(RestAction<Message> action, int pageNum) {
        action.queue(m -> {
            if (pages > 1) {
                if (bulkSkipNumber > 1)
                    m.addReaction(BIG_LEFT).queue();
                m.addReaction(LEFT).queue();
                if (bulkSkipNumber > 1)
                    m.addReaction(RIGHT).queue();
                m.addReaction(bulkSkipNumber > 1 ? BIG_RIGHT : RIGHT)
                        .queue(v -> pagination(m, pageNum), t -> pagination(m, pageNum));
            } else {
                finalAction.accept(m);
            }
        });
    }

    private void pagination(Message message, int pageNum) {
        waiter.waitForEvent(MessageReactionAddEvent.class,
                event -> checkReaction(event, message.getIdLong()), // Check Reaction
                event -> handleMessageReactionAddAction(event, message, pageNum), // Handle Reaction
                timeout, unit, () -> finalAction.accept(message));
    }

    private boolean checkReaction(MessageReactionAddEvent event, long messageId) {
        if (event.getMessageIdLong() != messageId)
            return false;
        switch (event.getReactionEmote().getName()) {
            case LEFT:
            case RIGHT:
                return isValidUser(event.getUser(), event.isFromGuild() ? event.getGuild() : null);
            case BIG_LEFT:
            case BIG_RIGHT:
                return bulkSkipNumber > 1 && isValidUser(event.getUser(), event.isFromGuild() ? event.getGuild() : null);
            default:
                event.getReaction().removeReaction(event.getUser()).queue();
                return false;
        }
    }

    private void handleMessageReactionAddAction(MessageReactionAddEvent event, Message message, int pageNum) {
        int newPageNum = pageNum;
        if (!event.getUser().equals(user)) {
            try {
                event.getReaction().removeReaction(event.getUser()).queue();
            } catch (PermissionException ignored) {
            }
        } else {
            switch (event.getReaction().getReactionEmote().getName()) {
                case LEFT:
                    if (newPageNum == 1 && wrapPageEnds)
                        newPageNum = pages + 1;
                    if (newPageNum > 1)
                        newPageNum--;
                    break;
                case RIGHT:
                    if (newPageNum == pages && wrapPageEnds)
                        newPageNum = 0;
                    if (newPageNum < pages)
                        newPageNum++;
                    break;
                case BIG_LEFT:
                    if (newPageNum > 1 || wrapPageEnds) {
                        for (int i = 1; (newPageNum > 1 || wrapPageEnds) && i < bulkSkipNumber; i++) {
                            if (newPageNum == 1)
                                newPageNum = pages + 1;
                            newPageNum--;
                        }
                    }
                    break;
                case BIG_RIGHT:
                    if (newPageNum < pages || wrapPageEnds) {
                        for (int i = 1; (newPageNum < pages || wrapPageEnds) && i < bulkSkipNumber; i++) {
                            if (newPageNum == pages)
                                newPageNum = 0;
                            newPageNum++;
                        }
                    }
                    break;
            }
            try {
                event.getReaction().removeReaction(event.getUser()).queue();
            } catch (PermissionException ignored) {
            }

        }

        int n = newPageNum;
        message.editMessage(renderPage(newPageNum)).queue(m -> pagination(m, n));
    }

    private Message renderPage(int pageNum) {
        MessageBuilder messageBuilder = new MessageBuilder();
        EmbedBuilder embedBuilder = new EmbedBuilder();
        try {
            embedBuilder.setTitle(pageTitles[pageNum - 1]);
        } catch (Exception ignored) {
        }

        int start = (pageNum - 1) * itemsPerPage;
        int end = Math.min(strings.size(), pageNum * itemsPerPage);
        if (columns == 1) {
            StringBuilder stringBuilder = new StringBuilder();
            for (int i = start; i < end; i++)
                stringBuilder.append("\n").append(numberItems ? "`" + (i + 1) + ".` " : "").append(strings.get(i));
            embedBuilder.setDescription(stringBuilder.toString());
        } else {
            int per = (int) Math.ceil((double) (end - start) / columns);
            for (int k = 0; k < columns; k++) {
                StringBuilder stringBuilder = new StringBuilder();
                for (int i = start + k * per; i < end && i < start + (k + 1) * per; i++)
                    stringBuilder.append("\n").append(numberItems ? (i + 1) + ". " : "").append(strings.get(i));
                embedBuilder.addField("", stringBuilder.toString(), true);
            }
        }

        embedBuilder.setColor(color.apply(pageNum, pages));
        if (showPageNumbers)
            embedBuilder.setFooter("Created By CrypticPlasma • Page " + pageNum + "/" + pages, null);
        embedBuilder.setTimestamp(Instant.now());
        messageBuilder.setEmbed(embedBuilder.build());
        return messageBuilder.build();
    }

    public static class Builder extends Menu.Builder<CustomPaginator.Builder, CustomPaginator> {
        private final List<String> strings = new LinkedList<>();
        private BiFunction<Integer, Integer, Color> color = (page, pages) -> null;
        private Consumer<Message> finalAction = m -> m.delete().queue();
        private int columns = 1;
        private int itemsPerPage = 12;
        private boolean showPageNumbers = true;
        private boolean numberItems = false;
        private String[] pageTitles = null;
        private int bulkSkipNumber = 1;
        private boolean wrapPageEnds = false;
        private User user = null;

        @Override
        public CustomPaginator build() {
            Checks.check(waiter != null, "Must set an EventWaiter");
            Checks.check(!strings.isEmpty(), "Must include at least one item to paginate");
            Checks.check(user != null, "Must set message author");

            return new CustomPaginator(waiter, users, roles, timeout, unit, color, finalAction,
                    columns, itemsPerPage, showPageNumbers, numberItems, strings,
                    bulkSkipNumber, wrapPageEnds, pageTitles, user);
        }

        public Builder setColor(Color color) {
            this.color = (i0, i1) -> color;
            return this;
        }


        public Builder setPageTitles(String[] pageTitles) {
            this.pageTitles = pageTitles;
            return this;
        }

        public Builder setFinalAction(Consumer<Message> finalAction) {
            this.finalAction = finalAction;
            return this;
        }

        public Builder setCommandUser(User user) {
            this.user = user;
            return this;
        }

        public Builder setColumns(int columns) {
            if (columns < 1 || columns > 3)
                throw new IllegalArgumentException("Only 1, 2, or 3 columns are supported");
            this.columns = columns;
            return this;
        }

        public Builder setItemsPerPage(int num) {
            if (num < 1)
                throw new IllegalArgumentException("There must be at least one item per page");
            this.itemsPerPage = num;
            return this;
        }

        public Builder showPageNumbers(boolean show) {
            this.showPageNumbers = show;
            return this;
        }

        public Builder useNumberedItems(boolean number) {
            this.numberItems = number;
            return this;
        }


        public Builder clearItems() {
            strings.clear();
            return this;
        }

        public Builder addItems(String... items) {
            strings.addAll(Arrays.asList(items));
            return this;
        }

        public Builder setItems(String... items) {
            strings.clear();
            strings.addAll(Arrays.asList(items));
            return this;
        }

        public Builder setBulkSkipNumber(int bulkSkipNumber) {
            this.bulkSkipNumber = Math.max(bulkSkipNumber, 1);
            return this;
        }

        public Builder wrapPageEnds(boolean wrapPageEnds) {
            this.wrapPageEnds = wrapPageEnds;
            return this;
        }


    }
}
