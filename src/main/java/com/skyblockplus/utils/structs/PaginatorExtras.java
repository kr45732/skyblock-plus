package com.skyblockplus.utils.structs;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.dv8tion.jda.api.entities.MessageEmbed.Field;

public class PaginatorExtras {

	private final List<String> titles = new ArrayList<>();
	private final List<String> titleUrls = new ArrayList<>();
	private final List<String> thumbnails = new ArrayList<>();
	private final List<Field> embedFields = new ArrayList<>();
	private String everyPageText = null;
	private String everyPageTitle = null;
	private String everyPageTitleUrl = null;
	private String everyPageThumbnail = null;

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

	public PaginatorExtras addEmbedField(Field embedField) {
		this.embedFields.add(embedField);
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
}
