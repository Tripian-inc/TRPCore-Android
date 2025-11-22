package com.tripian.trpcore.util

/**
 * Created by semihozkoroglu on 30.08.2020.
 */
enum class Category(var type: String, var id: Int) {
    ATTRACTIONS("Attraction", 1),
    RESTAURANT("Restaurant", 3),
    NIGHTLIFE("Nightlife", 4),
    COOL_FIND("Cool Find", 8),
    CAFE("Cafe", 24),
    RELIGIOUS_PLACE("Religious Place", 25),
    THEATER("Theater", 26),
    CINEMA("Cinema", 27),
    STADIUM("Stadium", 28),
    CIVIC_CENTER("Civic Center", 29),
    MUSEUM("Museum", 30),
    BAR("Bar", 31),
    ART_GALLERY("Art Gallery", 32),
    BAKERY("Bakery", 33),
    SHOPPING_CENTER("Shopping Center", 34),
    BREWERY("Brewery", 35),
    DESSERT("Dessert", 36);
}