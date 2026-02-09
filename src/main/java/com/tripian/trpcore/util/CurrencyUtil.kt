package com.tripian.trpcore.util

import com.tripian.trpcore.base.TRPCore

/**
 * Utility class for currency formatting and symbol mapping.
 */
object CurrencyUtil {

    private val symbolMap = mapOf(
        "EUR" to "€",
        "USD" to "$",
        "GBP" to "£",
        "TRY" to "₺",
        "JPY" to "¥",
        "AUD" to "A$",
        "CAD" to "C$",
        "CHF" to "CHF"
    )

    /**
     * Returns the currency symbol for the given currency code.
     *
     * @param currencyCode The ISO 4217 currency code (e.g., "EUR", "USD")
     * @return The currency symbol, or the currency code itself if not found
     */
    fun getSymbol(currencyCode: String): String {
        return symbolMap[currencyCode.uppercase()] ?: currencyCode
    }

    /**
     * Formats a price with the appropriate currency symbol.
     *
     * @param amount The price amount
     * @param currencyCode The currency code (uses global appCurrency if null)
     * @return Formatted price string (e.g., "€19.99")
     */
    fun formatPrice(amount: Double, currencyCode: String? = null): String {
        val code = currencyCode ?: TRPCore.core.appConfig.appCurrency
        val symbol = getSymbol(code)
        return "$symbol${String.format("%.2f", amount)}"
    }

    /**
     * Formats a price with the appropriate currency symbol (Int version).
     *
     * @param amount The price amount
     * @param currencyCode The currency code (uses global appCurrency if null)
     * @return Formatted price string (e.g., "€19")
     */
    fun formatPrice(amount: Int, currencyCode: String? = null): String {
        val code = currencyCode ?: TRPCore.core.appConfig.appCurrency
        val symbol = getSymbol(code)
        return "$symbol$amount"
    }

    /**
     * Returns a list of all supported currency codes.
     */
    fun getSupportedCurrencies(): List<String> {
        return symbolMap.keys.toList()
    }
}
