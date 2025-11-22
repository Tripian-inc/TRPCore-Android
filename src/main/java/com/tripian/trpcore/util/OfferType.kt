package com.tripian.trpcore.util

/**
 * Created by semihozkoroglu on 30.05.2021.
 */
enum class OfferType(val id: Int) {
    FOOD(1),
    FOOD_DINE_IN(1),
    FOOD_PICK_UP(2),
    DRINK(2),
    DRINK_DINE_IN(3),
    DRINK_PICK_UP(4),
    GROCERY(3),
    GROCERY_PICK_UP(5)
}