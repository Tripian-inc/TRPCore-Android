package com.tripian.trpcore.base

/**
 * Environment enum for Tripian API configuration
 */
enum class Environment {
    DEV,
    PROD;

    /**
     * Returns the API version path based on the environment
     */
    fun getApiVersion(): String {
        return when (this) {
            DEV -> "dev/"
            PROD -> "prod/"
        }
    }
}

