package com.skyblockplus.api.models;

public class GuildCategoryModel {
    private final String categoryName;
    private final String categoryId;

    public GuildCategoryModel(String categoryName, String categoryId) {
        this.categoryName = categoryName;
        this.categoryId = categoryId;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public String getCategoryId() {
        return categoryId;
    }
}
