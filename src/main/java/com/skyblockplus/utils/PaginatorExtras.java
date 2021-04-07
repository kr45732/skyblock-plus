package com.skyblockplus.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PaginatorExtras {
    private final List<String> titles = new ArrayList<>();
    private final List<String> thumbnails = new ArrayList<>();
    private String everyPageText = null;

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
}
