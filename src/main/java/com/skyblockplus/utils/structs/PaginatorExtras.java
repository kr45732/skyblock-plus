package com.skyblockplus.utils.structs;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.dv8tion.jda.api.entities.MessageEmbed.Field;

public class PaginatorExtras {

  private final List<String> titles = new ArrayList<>();
  private final List<String> thumbnails = new ArrayList<>();
  private final List<Field> embedFields = new ArrayList<>();
  private String everyPageText = null;
  private String everyPageTitle = null;
  private String everyPageThumnail = null;

  public List<String> getTitles() {
    return titles;
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

  public List<String> getThumbnails() {
    return thumbnails;
  }

  public PaginatorExtras setThumbnails(List<String> thumbnails) {
    this.thumbnails.clear();
    this.thumbnails.addAll(thumbnails);
    return this;
  }

  public PaginatorExtras setThumbnails(String[] thumbnails) {
    this.thumbnails.clear();
    this.thumbnails.addAll(Arrays.asList(thumbnails));
    return this;
  }

  public String getEveryPageText() {
    return everyPageText;
  }

  public PaginatorExtras setEveryPageText(String everyPageText) {
    this.everyPageText = everyPageText;
    return this;
  }

  public String getEverPageTitle() {
    return everyPageTitle;
  }

  public PaginatorExtras setEveryPageTitle(String everyPageTitle) {
    this.everyPageTitle = everyPageTitle;
    return this;
  }

  public String getEveryPageThumbnail() {
    return everyPageThumnail;
  }

  public PaginatorExtras setEveryPageThumbnail(String everyPageThumnail) {
    this.everyPageThumnail = everyPageThumnail;
    return this;
  }

  public PaginatorExtras addEmbedField(Field embedField) {
    this.embedFields.add(embedField);
    return this;
  }

  public List<Field> getEmbedFields() {
    return this.embedFields;
  }
}
