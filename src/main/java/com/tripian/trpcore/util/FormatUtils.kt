package com.tripian.trpcore.util

import java.util.Locale

/**
 * FormatUtils
 * Common formatting utility functions
 */
object FormatUtils {

    // Turkish locale for formatting: comma as decimal separator
    private val turkishLocale = Locale("tr", "TR")

    /**
     * Format price with currency symbol
     * @param price The price value
     * @param currency The currency code (EUR, USD, GBP, TRY, etc.)
     * @return Formatted price string with currency symbol
     *
     * Examples:
     * - EUR: "25,00 €" (symbol after, comma decimal separator)
     * - USD: "$25,00" (symbol before, comma decimal separator)
     * - GBP: "£25,00" (symbol before, comma decimal separator)
     * - TRY: "₺25,00" (symbol before, comma decimal separator)
     */
    fun formatPriceWithCurrency(price: Double, currency: String): String {
        val formattedPrice = if (price == price.toLong().toDouble()) {
            price.toLong().toString()
        } else {
            String.format(turkishLocale, "%.2f", price)
        }

        val symbol = when (currency.uppercase()) {
            "EUR" -> "€"
            "USD" -> "$"
            "GBP" -> "£"
            "TRY" -> "₺"
            else -> currency
        }

        // EUR: symbol after, others: symbol before
        return if (currency.uppercase() == "EUR") {
            "$formattedPrice $symbol"
        } else {
            "$symbol$formattedPrice"
        }
    }
}
