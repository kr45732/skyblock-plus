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

package com.skyblockplus.utils.structs;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed.Field;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;

public class PaginatorExtras {

	private final List<String> titles = new ArrayList<>();
	private final List<String> titleUrls = new ArrayList<>();
	private final List<String> thumbnails = new ArrayList<>();
	private final List<Field> embedFields = new ArrayList<>();
	private final List<EmbedBuilder> embedPages = new ArrayList<>();
	private final List<Button> buttons = new ArrayList<>();
	private final PaginatorType type;
	private String everyPageText = null;
	private String everyPageTitle = null;
	private String everyPageTitleUrl = null;
	private String everyPageThumbnail = null;

	public PaginatorExtras() {
		this.type = PaginatorType.DEFAULT;
	}

	public PaginatorExtras(PaginatorType type) {
		this.type = type;
	}

	public String getTitles(int index) {
		try {
			return titles.get(index);
		} catch (Exception e) {
			return null;
		}
	}

	public String getTitleUrls(int index) {
		try {
			return titleUrls.get(index);
		} catch (Exception e) {
			return null;
		}
	}

	public PaginatorExtras setTitles(List<String> titles) {
		this.titles.clear();
		this.titles.addAll(titles);
		return this;
	}

	public PaginatorExtras setTitles(String[] titles) {
		this.titles.clear();
		this.titles.addAll(Arrays.asList(titles));
		return this;
	}

	public PaginatorExtras setTitleUrls(List<String> titleUrls) {
		this.titleUrls.clear();
		this.titleUrls.addAll(titleUrls);
		return this;
	}

	public List<String> getThumbnails() {
		return thumbnails;
	}

	public PaginatorExtras setThumbnails(List<String> thumbnails) {
		this.thumbnails.clear();
		this.thumbnails.addAll(thumbnails);
		return this;
	}

	public String getEveryPageText() {
		return everyPageText;
	}

	public PaginatorExtras setEveryPageText(String everyPageText) {
		this.everyPageText = everyPageText;
		return this;
	}

	public String getEveryPageTitle() {
		return everyPageTitle;
	}

	public PaginatorExtras setEveryPageTitle(String everyPageTitle) {
		this.everyPageTitle = everyPageTitle;
		return this;
	}

	public String getEveryPageThumbnail() {
		return everyPageThumbnail;
	}

	public PaginatorExtras setEveryPageThumbnail(String everyPageThumbnail) {
		this.everyPageThumbnail = everyPageThumbnail;
		return this;
	}

	public PaginatorExtras addEmbedField(String name, String value, boolean inline) {
		this.embedFields.add(new Field(name, value, inline));
		return this;
	}

	public PaginatorExtras addBlankField(boolean inline) {
		this.embedFields.add(new Field(EmbedBuilder.ZERO_WIDTH_SPACE, EmbedBuilder.ZERO_WIDTH_SPACE, inline));
		return this;
	}

	public List<Field> getEmbedFields() {
		return this.embedFields;
	}

	public String getEveryPageTitleUrl() {
		return everyPageTitleUrl;
	}

	public PaginatorExtras setEveryPageTitleUrl(String everyPageTitleUrl) {
		this.everyPageTitleUrl = everyPageTitleUrl;
		return this;
	}

	public PaginatorType getType() {
		return type;
	}

	public List<EmbedBuilder> getEmbedPages() {
		return embedPages;
	}

	public PaginatorExtras addEmbedPage(EmbedBuilder embedBuilder) {
		this.embedPages.add(embedBuilder);
		return this;
	}

	/**
	 * Only on one row after arrows
	 */
	public PaginatorExtras addButton(Button button) {
		this.buttons.add(button);
		return this;
	}

	public ActionRow getButtons() {
		return buttons.isEmpty() ? null : ActionRow.of(buttons);
	}

	public enum PaginatorType {
		DEFAULT,
		EMBED_FIELDS,
		EMBED_PAGES,
	}
}
