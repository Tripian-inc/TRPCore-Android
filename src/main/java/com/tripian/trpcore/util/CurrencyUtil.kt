package com.tripian.trpcore.util

import com.tripian.trpcore.base.TRPCore
import java.util.Currency
import java.util.Locale

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
        "CHF" to "CHF",
        "MXN" to "MX$"
    )

    /**
     * Resolves currency code from various input formats.
     * Supports:
     * - ISO 4217 currency codes (e.g., "USD", "EUR", "MXN")
     * - Locale format (e.g., "es-MX", "en-US", "de-DE")
     *
     * @param input The currency code or locale string
     * @return The ISO 4217 currency code (e.g., "MXN" for "es-MX")
     */
    fun resolveCurrencyCode(input: String): String {
        // If it's already a valid 3-letter currency code, return as-is
        if (input.length == 3 && input.all { it.isLetter() }) {
            return input.uppercase()
        }

        // Try to parse as locale format (e.g., "es-MX", "en_US")
        val parts = input.replace("_", "-").split("-")
        if (parts.size == 2) {
            val language = parts[0].lowercase()
            val country = parts[1].uppercase()

            return try {
                val locale = Locale(language, country)
                val currency = Currency.getInstance(locale)
                currency.currencyCode
            } catch (e: Exception) {
                // Fallback: return input as-is if parsing fails
                input.uppercase()
            }
        }

        // Return input as-is if no pattern matches
        return input.uppercase()
    }

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
