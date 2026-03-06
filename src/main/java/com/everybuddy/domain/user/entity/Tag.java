package com.everybuddy.domain.user.entity;

public enum Tag {

    // HOBBY
    SPORTS(TagCategory.HOBBY),
    READING(TagCategory.HOBBY),
    TRAVEL(TagCategory.HOBBY),
    COOKING(TagCategory.HOBBY),
    GAMING(TagCategory.HOBBY),

    // PERSONALITY
    INTJ(TagCategory.PERSONALITY),
    ENFP(TagCategory.PERSONALITY),
    INTROVERT(TagCategory.PERSONALITY),
    EXTROVERT(TagCategory.PERSONALITY),
    AMBIVERT(TagCategory.PERSONALITY),

    // FOOD
    KOREAN_FOOD(TagCategory.FOOD),
    JAPANESE_FOOD(TagCategory.FOOD),
    ITALIAN_FOOD(TagCategory.FOOD),
    STREET_FOOD(TagCategory.FOOD),
    VEGAN(TagCategory.FOOD),

    // ENTERTAINMENT
    MOVIES(TagCategory.ENTERTAINMENT),
    MUSIC(TagCategory.ENTERTAINMENT),
    WEBTOON(TagCategory.ENTERTAINMENT),
    SPORTS_WATCHING(TagCategory.ENTERTAINMENT),
    CONCERTS(TagCategory.ENTERTAINMENT);

    private final TagCategory category;

    Tag(TagCategory category) {
        this.category = category;
    }

    public TagCategory getCategory() {
        return category;
    }
}
