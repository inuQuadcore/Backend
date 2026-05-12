package com.everybuddy.domain.user.entity;

public enum Tag {

    // HOBBY
    CAFE_TOUR(TagCategory.HOBBY),
    TRAVEL(TagCategory.HOBBY),
    PHOTOGRAPHY(TagCategory.HOBBY),
    WORKOUT(TagCategory.HOBBY),
    RUNNING(TagCategory.HOBBY),
    GYM(TagCategory.HOBBY),
    YOGA(TagCategory.HOBBY),
    HIKING(TagCategory.HOBBY),
    CAMPING(TagCategory.HOBBY),
    READING(TagCategory.HOBBY),
    WRITING(TagCategory.HOBBY),
    ART(TagCategory.HOBBY),
    INSTRUMENT(TagCategory.HOBBY),
    SINGING(TagCategory.HOBBY),
    GAMING(TagCategory.HOBBY),
    BOARD_GAME(TagCategory.HOBBY),
    DIY(TagCategory.HOBBY),
    COOKING(TagCategory.HOBBY),
    BAKING(TagCategory.HOBBY),
    PLANT(TagCategory.HOBBY),
    PET(TagCategory.HOBBY),
    INVESTMENT(TagCategory.HOBBY),
    SELF_DEV(TagCategory.HOBBY),
    STUDY_GROUP(TagCategory.HOBBY),

    // MBTI
    ISTJ(TagCategory.MBTI),
    ISFJ(TagCategory.MBTI),
    INFJ(TagCategory.MBTI),
    INTJ(TagCategory.MBTI),
    ISTP(TagCategory.MBTI),
    ISFP(TagCategory.MBTI),
    INFP(TagCategory.MBTI),
    INTP(TagCategory.MBTI),
    ESTP(TagCategory.MBTI),
    ESFP(TagCategory.MBTI),
    ENFP(TagCategory.MBTI),
    ENTP(TagCategory.MBTI),
    ESTJ(TagCategory.MBTI),
    ESFJ(TagCategory.MBTI),
    ENFJ(TagCategory.MBTI),
    ENTJ(TagCategory.MBTI),
    EXTROVERT(TagCategory.MBTI),
    INTROVERT(TagCategory.MBTI),
    EMOTIONAL(TagCategory.MBTI),
    RATIONAL(TagCategory.MBTI),
    PLANNER(TagCategory.MBTI),
    SPONTANEOUS(TagCategory.MBTI),
    SOLO(TagCategory.MBTI),
    TOGETHER(TagCategory.MBTI),
    QUIET(TagCategory.MBTI),
    ACTIVE(TagCategory.MBTI),

    // FOOD
    DESSERT(TagCategory.FOOD),
    BREAD(TagCategory.FOOD),
    COFFEE(TagCategory.FOOD),
    TEA(TagCategory.FOOD),
    KOREAN_FOOD(TagCategory.FOOD),
    CHINESE_FOOD(TagCategory.FOOD),
    JAPANESE_FOOD(TagCategory.FOOD),
    WESTERN_FOOD(TagCategory.FOOD),
    KOREAN_SNACK(TagCategory.FOOD),
    CHICKEN(TagCategory.FOOD),
    PIZZA(TagCategory.FOOD),
    BURGER(TagCategory.FOOD),
    MEAT(TagCategory.FOOD),
    SEAFOOD(TagCategory.FOOD),
    SPICY(TagCategory.FOOD),
    VEGAN(TagCategory.FOOD),
    DIET(TagCategory.FOOD),
    SOLO_MEAL(TagCategory.FOOD),
    FOOD_TOUR(TagCategory.FOOD),
    DRINK(TagCategory.FOOD),
    WINE(TagCategory.FOOD),
    BEER(TagCategory.FOOD),

    // ENTERTAINMENT
    MOVIE(TagCategory.ENTERTAINMENT),
    DRAMA(TagCategory.ENTERTAINMENT),
    VARIETY(TagCategory.ENTERTAINMENT),
    DOCUMENTARY(TagCategory.ENTERTAINMENT),
    YOUTUBE(TagCategory.ENTERTAINMENT),
    NETFLIX(TagCategory.ENTERTAINMENT),
    OTT(TagCategory.ENTERTAINMENT),
    MUSIC(TagCategory.ENTERTAINMENT),
    CONCERT(TagCategory.ENTERTAINMENT),
    MUSICAL(TagCategory.ENTERTAINMENT),
    PLAY(TagCategory.ENTERTAINMENT),
    EXHIBITION(TagCategory.ENTERTAINMENT),
    WEBTOON(TagCategory.ENTERTAINMENT),
    WEB_NOVEL(TagCategory.ENTERTAINMENT),
    BOOK(TagCategory.ENTERTAINMENT),
    GAME_STREAM(TagCategory.ENTERTAINMENT),
    ESPORTS(TagCategory.ENTERTAINMENT),
    IDOL(TagCategory.ENTERTAINMENT),
    HIPHOP(TagCategory.ENTERTAINMENT),
    BALLAD(TagCategory.ENTERTAINMENT);

    private final TagCategory category;

    Tag(TagCategory category) {
        this.category = category;
    }

    public TagCategory getCategory() {
        return category;
    }
}
