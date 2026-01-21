package com.tripian.trpcore.base

/**
 * Environment enum for Tripian API configuration
 */
enum class Environment {
    DEV,
    PREDEV,
    TEST,
    PROD;

    /**
     * Returns the API version path based on the environment
     */
    fun getApiVersion(): String {
        return when (this) {
            DEV -> "dev/"
            PREDEV -> "predev/"
            TEST -> "test/"
            PROD -> "prod/"
        }
    }
}

