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

import com.jagrosh.jdautilities.commons.waiter.EventWaiter;
import com.jagrosh.jdautilities.menu.Menu;
import com.skyblockplus.utils.structs.PaginatorExtras;
import java.awt.*;
import java.time.Instant;
import java.util.*;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.components.Button;
import net.dv8tion.jda.api.interactions.components.ButtonStyle;
import net.dv8tion.jda.api.requests.RestAction;
import net.dv8tion.jda.api.requests.restaction.MessageAction;
import net.dv8tion.jda.api.requests.restaction.WebhookMessageUpdateAction;
import net.dv8tion.jda.internal.utils.Checks;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CustomPaginator extends Menu {

	private static final Logger log = LoggerFactory.getLogger(CustomPaginator.class);
	private static final Consumer<Throwable> throwableConsumer = e -> {
		if (!e.getMessage().contains("Unknown Message")) {
			log.error(e.getMessage(), e);
		}
	};
	private static final String LEFT = "paginator_left_button";
	private static final String RIGHT = "paginator_right_button";
	private final Color color;
	private final int columns;
	private final int itemsPerPage;
	private final List<String> strings;
	private final int pages;
	private final Consumer<Message> finalAction;
	private final boolean wrapPageEnds;
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
		switch (extras.getType()) {
			case DEFAULT:
				this.pages = (int) Math.ceil((double) strings.size() / itemsPerPage);
				break;
			case EMBED_FIELDS:
				this.pages = (int) Math.ceil((double) extras.getEmbedFields().size() / itemsPerPage);
				break;
			case EMBED_PAGES:
				this.pages = extras.getEmbedPages().size();
				break;
			default:
				throw new IllegalArgumentException("Invalid paginator type");
		}
		this.finalAction = finalAction;
		this.wrapPageEnds = wrapPageEnds;
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
		if (pageNum < 1) {
			pageNum = 1;
		} else if (pageNum > pages) {
			pageNum = pages;
		}
		Message msg = new MessageBuilder().setEmbeds(getEmbedRender(pageNum)).build();
		initialize(channel.sendMessage(msg), pageNum);
	}

	public void paginate(InteractionHook channel, int pageNum) {
		if (pageNum < 1) {
			pageNum = 1;
		} else if (pageNum > pages) {
			pageNum = pages;
		}
		Message msg = new MessageBuilder().setEmbeds(getEmbedRender(pageNum)).build();
		initialize(channel.editOriginal(msg), pageNum);
	}

	public void paginate(Message message, int pageNum) {
		if (pageNum < 1) {
			pageNum = 1;
		} else if (pageNum > pages) {
			pageNum = pages;
		}

		Message msg = new MessageBuilder().setEmbeds(getEmbedRender(pageNum)).build();
		initialize(message.editMessage(msg), pageNum);
	}

	private void initialize(RestAction<Message> action, int pageNum) {
		if (pages > 1) {
			if (action instanceof MessageAction) {
				action =
					((MessageAction) action).setActionRow(
							Button.primary(LEFT, Emoji.fromMarkdown("<:left_button_arrow:885628386435821578>")),
							Button.primary(RIGHT, Emoji.fromMarkdown("<:right_button_arrow:885628386578423908>"))
						);
			} else if (action instanceof WebhookMessageUpdateAction) {
				action =
					((WebhookMessageUpdateAction<Message>) action).setActionRow(
							Button.primary(LEFT, Emoji.fromMarkdown("<:left_button_arrow:885628386435821578>")),
							Button.primary(RIGHT, Emoji.fromMarkdown("<:right_button_arrow:885628386578423908>"))
						);
			} else {
				log.error("This shouldn't happen | Class: " + action.getClass() + " | Action: " + action);
			}
		}

		action.queue(
			m -> {
				if (pages > 1) {
					pagination(m, pageNum);
				} else {
					finalAction.accept(m);
				}
			},
			throwableConsumer
		);
	}

	private void pagination(Message message, int pageNum) {
		waiter.waitForEvent(
			ButtonClickEvent.class,
			event -> checkButtonClick(event, message.getIdLong()),
			event -> handleButtonClick(event, pageNum),
			timeout,
			unit,
			() -> finalAction.accept(message)
		);
	}

	private boolean checkButtonClick(ButtonClickEvent event, long messageId) {
		if (event.getMessageIdLong() != messageId) {
			return false;
		}

		if (event.getButton() == null || event.getButton().getId() == null) {
			return false;
		}

		switch (event.getButton().getId()) {
			case LEFT:
			case RIGHT:
				return isValidUser(event.getUser(), event.isFromGuild() ? event.getGuild() : null);
			default:
				event
					.editButton(event.getButton().asDisabled().withId("disabled").withLabel("Disabled").withStyle(ButtonStyle.DANGER))
					.queue();
				return false;
		}
	}

	private void handleButtonClick(ButtonClickEvent event, int pageNum) {
		if (event.getButton() == null || event.getButton().getId() == null) {
			return;
		}

		int newPageNum = pageNum;

		switch (event.getButton().getId()) {
			case LEFT:
				if (newPageNum == 1 && wrapPageEnds) {
					newPageNum = pages + 1;
				}
				if (newPageNum > 1) {
					newPageNum--;
				}
				break;
			case RIGHT:
				if (newPageNum == pages && wrapPageEnds) {
					newPageNum = 0;
				}
				if (newPageNum < pages) {
					newPageNum++;
				}
				break;
		}

		int n = newPageNum;
		event.editMessageEmbeds(getEmbedRender(newPageNum)).queue(hook -> pagination(event.getMessage(), n), throwableConsumer);
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
					title = extras.getTitles(pageNum - 1);
				}

				if (extras.getEveryPageTitleUrl() != null) {
					titleUrl = extras.getEveryPageTitleUrl();
				} else {
					titleUrl = extras.getTitleUrls(pageNum - 1);
				}

				embedBuilder.setTitle(title, titleUrl);
			} catch (Exception ignored) {}

			try {
				if (extras.getEveryPageThumbnail() != null) {
					embedBuilder.setThumbnail(extras.getEveryPageThumbnail());
				} else {
					embedBuilder.setThumbnail(extras.getThumbnails().get(pageNum - 1));
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
					embedBuilder.addField("", stringBuilder.toString(), true);
				}
			}
		}

		embedBuilder
			.setColor(color)
			.setFooter("Created By CrypticPlasma • Page " + pageNum + "/" + pages, null)
			.setTimestamp(Instant.now());

		return embedBuilder.build();
	}

	public static class Builder extends Menu.Builder<CustomPaginator.Builder, CustomPaginator> {

		private final List<String> strings = new LinkedList<>();
		private Color color = null;
		private Consumer<Message> finalAction = m -> m.delete().queue(null, throwableConsumer);
		private int columns = 1;
		private int itemsPerPage = 12;
		private boolean wrapPageEnds = false;
		private PaginatorExtras extras = new PaginatorExtras();

		@Override
		public CustomPaginator build() {
			Checks.check(waiter != null, "Must set an EventWaiter");
			switch (extras.getType()) {
				case DEFAULT:
					Checks.check(!strings.isEmpty(), "Must include at least one item to paginate");
					if(extras.getEmbedFields().size() > 0){
						log.warn("Paginator type is DEFAULT but embed fields were also provided");
					}
					if(extras.getEmbedPages().size() > 0){
						log.warn("Paginator type is DEFAULT but embed pages were also provided");
					}
					break;
				case EMBED_FIELDS:
					Checks.check(!extras.getEmbedFields().isEmpty(), "Must include at least one embed field to paginate");
					if(strings.size() > 1){
						log.warn("Paginator type is EMBED_FIELDS but strings were also provided");
					}
					if(extras.getEmbedPages().size() > 0){
						log.warn("Paginator type is EMBED_FIELDS but embed pages were also provided");
					}
					break;
				case EMBED_PAGES:
					Checks.check(!extras.getEmbedPages().isEmpty(), "Must include at least one embed page to paginate");
					if(strings.size() > 0){
						log.warn("Paginator type is EMBED_PAGES but strings were also provided");
					}
					if(extras.getEmbedFields().size() > 0){
						log.warn("Paginator type is EMBED_PAGES but embed fields were also provided");
					}
					break;
				default:
					throw new IllegalArgumentException("Invalid paginator type");
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

		public Builder setFinalAction(Consumer<Message> finalAction) {
			this.finalAction = finalAction;
			return this;
		}

		public Builder setColumns(int columns) {
			if (columns < 1 || columns > 3) throw new IllegalArgumentException("Only 1, 2, or 3 columns are supported");
			this.columns = columns;
			return this;
		}

		public Builder setItemsPerPage(int num) {
			if (num < 1) throw new IllegalArgumentException("There must be at least one item per page");
			this.itemsPerPage = num;
			return this;
		}

		public void addItems(String... items) {
			strings.addAll(Arrays.asList(items));
		}

		public Builder setItems(String... items) {
			strings.clear();
			Collections.addAll(strings, items);
			return this;
		}

		public Builder wrapPageEnds(boolean wrapPageEnds) {
			this.wrapPageEnds = wrapPageEnds;
			return this;
		}

		public int getItemsSize() {
			return strings.size();
		}
	}
}
