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

package com.skyblockplus.utils.command;

import static com.skyblockplus.utils.Utils.defaultEmbed;

import com.jagrosh.jdautilities.commons.waiter.EventWaiter;
import com.jagrosh.jdautilities.menu.Menu;
import java.awt.*;
import java.time.Instant;
import java.util.*;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;
import lombok.Getter;
import lombok.Setter;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.requests.ErrorResponse;
import net.dv8tion.jda.api.requests.RestAction;
import net.dv8tion.jda.api.requests.restaction.MessageAction;
import net.dv8tion.jda.api.requests.restaction.WebhookMessageUpdateAction;
import net.dv8tion.jda.internal.utils.Checks;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CustomPaginator extends Menu {

	private static final Logger log = LoggerFactory.getLogger(CustomPaginator.class);
	private static final Consumer<Throwable> throwableConsumer = e -> {
		if (!(e instanceof ErrorResponseException ex && ex.getErrorResponse().equals(ErrorResponse.UNKNOWN_INTERACTION))) {
			log.error(e.getMessage(), e);
		}
	};
	private static final String LEFT = "paginator_left_button";
	private static final String RIGHT = "paginator_right_button";
	private final Color color;
	private final int columns;
	private final int itemsPerPage;
	private final boolean showPageNumbers;
	private int pages;
	private final Consumer<Message> finalAction;
	private final boolean wrapPageEnds;

	@Setter
	private List<String> strings;

	@Getter
	private final PaginatorExtras extras;

	private CustomPaginator(
		EventWaiter waiter,
		Set<User> users,
		Set<Role> roles,
		long timeout,
		TimeUnit unit,
		Color color,
		Consumer<Message> finalAction,
		int columns,
		int itemsPerPage,
		boolean showPageNumbers,
		List<String> items,
		boolean wrapPageEnds,
		PaginatorExtras extras
	) {
		super(waiter, users, roles, timeout, unit);
		this.color = color;
		this.columns = columns;
		this.itemsPerPage = itemsPerPage;
		this.strings = items;
		this.extras = extras;
		this.showPageNumbers = showPageNumbers;
		this.finalAction = finalAction;
		this.wrapPageEnds = wrapPageEnds;
		calculatePages();
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
		pageNum = Math.min(Math.max(pageNum, 1), pages);

		Message msg = new MessageBuilder()
			.setEmbeds(getEmbedRender(pageNum))
			//			.setContent(
			//				"**⚠️ Skyblock Plus will stop responding to message commands <t:1662004740:R>!** Please use slash commands instead. If you do not see slash commands from this bot, then please re-invite the bot using `" +
			//				getGuildPrefix(((GuildMessageChannel) channel).getGuild().getId()) +
			//				"invite`"
			//			)
			.build();
		initialize(channel.sendMessage(msg), pageNum);
	}

	public void paginate(InteractionHook channel, int pageNum) {
		pageNum = Math.min(Math.max(pageNum, 1), pages);

		Message msg = new MessageBuilder().setEmbeds(getEmbedRender(pageNum)).build();
		initialize(channel.editOriginal(msg), pageNum);
	}

	public void paginate(Message message, int pageNum) {
		pageNum = Math.min(Math.max(pageNum, 1), pages);

		Message msg = new MessageBuilder().setEmbeds(getEmbedRender(pageNum)).build();
		initialize(message.editMessage(msg), pageNum);
	}

	private void initialize(RestAction<Message> action, int pageNum) {
		List<ActionRow> actionRows = new ArrayList<>();
		if (pages > 1) {
			actionRows.add(
				ActionRow.of(
					Button.primary(LEFT, Emoji.fromMarkdown("<:left_button_arrow:885628386435821578>")).withDisabled(pageNum == 1),
					Button.primary(RIGHT, Emoji.fromMarkdown("<:right_button_arrow:885628386578423908>")).withDisabled(pageNum == pages)
				)
			);
		}
		if (!extras.getButtons().isEmpty()) {
			actionRows.add(ActionRow.of(extras.getButtons()));
		}

		if (action instanceof MessageAction a) {
			action = a.setActionRows(actionRows);
		} else if (action instanceof WebhookMessageUpdateAction<Message> a) {
			action = a.setActionRows(actionRows);
		}

		if (pages == 0) {
			if (action instanceof MessageAction a) {
				action = a.setEmbeds(defaultEmbed("No items to paginate").build());
			} else if (action instanceof WebhookMessageUpdateAction<Message> a) {
				action = a.setEmbeds(defaultEmbed("No items to paginate").build());
			}
			action.queue();
		} else {
			action.queue(m -> pagination(m, pageNum), throwableConsumer);
		}
	}

	private void pagination(Message message, int pageNum) {
		waiter.waitForEvent(
			ButtonInteractionEvent.class,
			event -> checkButtonClick(event, message.getId()),
			event -> handleButtonClick(event, pageNum),
			timeout,
			unit,
			() -> finalAction.accept(message)
		);
	}

	private boolean checkButtonClick(ButtonInteractionEvent event, String messageId) {
		if (!event.getMessageId().equals(messageId)) {
			return false;
		}

		if (!isValidUser(event.getUser(), event.isFromGuild() ? event.getGuild() : null)) {
			return false;
		}

		return (
			event.getComponentId().equals(LEFT) ||
			event.getComponentId().equals(RIGHT) ||
			extras.getReactiveButtons().stream().anyMatch(b -> b.isReacting() && event.getComponentId().equals(b.getId()))
		);
	}

	@Override
	protected boolean isValidUser(User user, Guild guild) {
		if (user.isBot()) {
			return false;
		}
		if (users.isEmpty() && roles.isEmpty()) {
			return true;
		}
		if (users.contains(user)) {
			return true;
		}
		if (guild == null || !guild.isMember(user)) {
			return false;
		}

		return guild.getMember(user).getRoles().stream().anyMatch(roles::contains);
	}

	private void handleButtonClick(ButtonInteractionEvent event, int pageNum) {
		if (event.getButton().getId() == null) {
			return;
		}

		switch (event.getButton().getId()) {
			case LEFT -> {
				if (pageNum == 1 && wrapPageEnds) {
					pageNum = pages + 1;
				}
				if (pageNum > 1) {
					pageNum--;
				}
			}
			case RIGHT -> {
				if (pageNum == pages && wrapPageEnds) {
					pageNum = 0;
				}
				if (pageNum < pages) {
					pageNum++;
				}
			}
			default -> extras
				.getReactiveButtons()
				.stream()
				.filter(b -> b.isReacting() && event.getComponentId().equals(b.getId()))
				.map(PaginatorExtras.ReactiveButton::getAction)
				.findFirst()
				.orElse(ignored -> {})
				.accept(this);
		}
		calculatePages();
		pageNum = Math.min(pageNum, pages);

		List<ActionRow> actionRows = new ArrayList<>();
		if (pages > 1) {
			actionRows.add(
				ActionRow.of(
					Button.primary(LEFT, Emoji.fromMarkdown("<:left_button_arrow:885628386435821578>")).withDisabled(pageNum == 1),
					Button.primary(RIGHT, Emoji.fromMarkdown("<:right_button_arrow:885628386578423908>")).withDisabled(pageNum == pages)
				)
			);
		}
		if (!extras.getButtons().isEmpty()) {
			actionRows.add(ActionRow.of(extras.getButtons()));
		}

		int finalPageNum = pageNum;
		event
			.editMessageEmbeds(getEmbedRender(pageNum))
			.setActionRows(actionRows)
			.queue(hook -> pagination(event.getMessage(), finalPageNum), throwableConsumer);
	}

	private MessageEmbed getEmbedRender(int pageNum) {
		EmbedBuilder embedBuilder = new EmbedBuilder();

		if (extras.getType() == PaginatorExtras.PaginatorType.EMBED_PAGES) {
			embedBuilder = extras.getEmbedPages().get(pageNum - 1);
		} else {
			try {
				String title;
				String titleUrl;

				if (extras.getEveryPageTitle() != null) {
					title = extras.getEveryPageTitle();
				} else {
					title = extras.getTitle(pageNum - 1);
				}

				if (extras.getEveryPageTitleUrl() != null) {
					titleUrl = extras.getEveryPageTitleUrl();
				} else {
					titleUrl = extras.getTitleUrl(pageNum - 1);
				}

				embedBuilder.setTitle(title, titleUrl);
			} catch (Exception ignored) {}

			try {
				if (extras.getEveryPageThumbnail() != null) {
					embedBuilder.setThumbnail(extras.getEveryPageThumbnail());
				}
			} catch (Exception ignored) {}

			try {
				embedBuilder.setDescription(extras.getEveryPageText());
			} catch (Exception ignored) {}

			int start = (pageNum - 1) * itemsPerPage;
			int end = Math.min(strings.size(), pageNum * itemsPerPage);
			if (extras.getType() == PaginatorExtras.PaginatorType.EMBED_FIELDS) {
				end = Math.min(extras.getEmbedFields().size(), pageNum * itemsPerPage);
				for (int i = start; i < end; i++) {
					embedBuilder.addField(extras.getEmbedFields().get(i));
				}
			} else if (columns == 1) {
				StringBuilder stringBuilder = new StringBuilder();
				for (int i = start; i < end; i++) {
					stringBuilder.append("\n").append(strings.get(i));
				}
				embedBuilder.appendDescription(stringBuilder.toString());
			} else {
				int per = (int) Math.ceil((double) (end - start) / columns);
				for (int k = 0; k < columns; k++) {
					StringBuilder stringBuilder = new StringBuilder();
					for (int i = start + k * per; i < end && i < start + (k + 1) * per; i++) stringBuilder
						.append("\n")
						.append(strings.get(i));
					embedBuilder.addField(
						(k == 0 && extras.getEveryPageFirstFieldTitle() != null ? extras.getEveryPageFirstFieldTitle() : ""),
						stringBuilder.toString(),
						true
					);
				}
			}
		}

		embedBuilder
			.setColor(color)
			.setFooter("By CrypticPlasma" + (showPageNumbers ? " • Page " + pageNum + "/" + pages : "") + " • dsc.gg/sb+", null)
			.setTimestamp(Instant.now());

		return embedBuilder.build();
	}

	private void calculatePages() {
		this.pages =
			switch (extras.getType()) {
				case DEFAULT -> (int) Math.ceil((double) strings.size() / itemsPerPage);
				case EMBED_FIELDS -> (int) Math.ceil((double) extras.getEmbedFields().size() / itemsPerPage);
				case EMBED_PAGES -> extras.getEmbedPages().size();
			};
	}

	public static class Builder extends Menu.Builder<CustomPaginator.Builder, CustomPaginator> {

		private final List<String> strings = new LinkedList<>();
		private PaginatorExtras extras = new PaginatorExtras(PaginatorExtras.PaginatorType.DEFAULT);
		private Color color = null;
		private Consumer<Message> finalAction = m -> m.delete().queue(null, throwableConsumer);
		private int columns = 1;
		private int itemsPerPage = 12;
		private boolean wrapPageEnds = false;
		private boolean showPageNumbers = true;

		@Override
		public CustomPaginator build() {
			Checks.check(waiter != null, "Must set an EventWaiter");
			switch (extras.getType()) {
				case DEFAULT -> {
					if (strings.isEmpty()) {
						log.error("Paginator type is DEFAULT but no strings were provided");
					}
					if (!extras.getEmbedFields().isEmpty()) {
						log.warn("Paginator type is DEFAULT but embed fields were also provided");
					}
					if (!extras.getEmbedPages().isEmpty()) {
						log.warn("Paginator type is DEFAULT but embed pages were also provided");
					}
				}
				case EMBED_FIELDS -> {
					if (extras.getEmbedFields().isEmpty()) {
						log.error("Paginator type is EMBED_FIELDS but no embed fields were provided");
					}
					if (!strings.isEmpty()) {
						log.warn("Paginator type is EMBED_FIELDS but strings were also provided");
					}
					if (!extras.getEmbedPages().isEmpty()) {
						log.warn("Paginator type is EMBED_FIELDS but embed pages were also provided");
					}
				}
				case EMBED_PAGES -> {
					if (extras.getEmbedPages().isEmpty()) {
						log.error("Paginator type is EMBED_PAGES but no embed pages were provided");
					}
					if (!strings.isEmpty()) {
						log.warn("Paginator type is EMBED_PAGES but strings were also provided");
					}
					if (!extras.getEmbedFields().isEmpty()) {
						log.warn("Paginator type is EMBED_PAGES but embed fields were also provided");
					}
				}
				default -> throw new IllegalArgumentException("Invalid paginator type");
			}

			return new CustomPaginator(
				waiter,
				users,
				roles,
				timeout,
				unit,
				color,
				finalAction,
				columns,
				itemsPerPage,
				showPageNumbers,
				strings,
				wrapPageEnds,
				extras
			);
		}

		public Builder setColor(Color color) {
			this.color = color;
			return this;
		}

		public Builder setPaginatorExtras(PaginatorExtras paginatorExtras) {
			this.extras = paginatorExtras;
			return this;
		}

		public PaginatorExtras getPaginatorExtras() {
			return extras;
		}

		public Builder setFinalAction(Consumer<Message> finalAction) {
			this.finalAction = finalAction;
			return this;
		}

		public Builder setColumns(int columns) {
			if (columns < 1 || columns > 3) {
				throw new IllegalArgumentException("Only 1, 2, or 3 columns are supported");
			}
			this.columns = columns;
			return this;
		}

		public Builder setItemsPerPage(int num) {
			if (num < 1) {
				throw new IllegalArgumentException("There must be at least one item per page");
			}
			this.itemsPerPage = num;
			return this;
		}

		public void addItems(String... items) {
			addItems(Arrays.asList(items));
		}

		public Builder addItems(Collection<String> items) {
			strings.addAll(items);
			return this;
		}

		public Builder wrapPageEnds(boolean wrapPageEnds) {
			this.wrapPageEnds = wrapPageEnds;
			return this;
		}

		public Builder showPageNumbers(boolean showPageNumbers) {
			this.showPageNumbers = showPageNumbers;
			return this;
		}

		public PaginatorExtras getExtras() {
			return extras;
		}

		public CustomPaginator.Builder updateExtras(Function<PaginatorExtras, PaginatorExtras> extras) {
			extras.apply(this.extras);
			return this;
		}

		public int size() {
			return switch (extras.getType()) {
				case DEFAULT -> strings.size();
				case EMBED_FIELDS -> extras.getEmbedFields().size();
				case EMBED_PAGES -> extras.getEmbedPages().size();
			};
		}
	}
}
