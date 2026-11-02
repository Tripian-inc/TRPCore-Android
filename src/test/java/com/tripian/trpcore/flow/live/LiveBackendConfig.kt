package com.tripian.trpcore.flow.live

import java.io.File
import java.util.Properties

/**
 * Settings for the live-backend flow suite, read from `live-test.properties` at the TRPCore module root.
 * `TRIPIAN_LIVE_STAGE` and `TRIPIAN_LIVE_API_KEY` environment variables override the file.
 * [toString] never includes the API key.
 */
class LiveBackendConfig private constructor(
    val stage: String,
    val apiKey: String,
    val cityId: Int
) {
    val baseUrl: String get() = "$HOST/$stage/"

    override fun toString(): String = "LiveBackendConfig(stage=$stage, cityId=$cityId)"

    companion object {
        const val FILE_NAME = "live-test.properties"
        const val DEFAULT_CITY_ID = 109
        private const val HOST = "https://gyssxjfp9d.execute-api.eu-west-1.amazonaws.com"
        private const val ENV_STAGE = "TRIPIAN_LIVE_STAGE"
        private const val ENV_API_KEY = "TRIPIAN_LIVE_API_KEY"
        private val STAGES = setOf("predev", "prod")

        /**
         * The configuration, or null when no stage or API key is configured so the live suite is skipped.
         * Throws when `apiKeyFile`/`apiKeyProperty` are configured but do not resolve to a key.
         */
        fun loadOrNull(): LiveBackendConfig? {
            val file = locatePropertiesFile()
            val properties = Properties().apply { file?.inputStream()?.use(::load) }

            val stage = (System.getenv(ENV_STAGE) ?: properties.getProperty("stage"))?.unquoted() ?: return null
            require(stage in STAGES) { "Unknown live stage '$stage', expected one of $STAGES" }

            val apiKey = System.getenv(ENV_API_KEY)?.unquoted()
                ?: properties.getProperty("apiKey")?.unquoted()
                ?: readKeyFromFile(file, properties)
                ?: return null

            val cityId = properties.getProperty("cityId")?.trim()?.toIntOrNull() ?: DEFAULT_CITY_ID
            return LiveBackendConfig(stage, apiKey, cityId)
        }

        private fun locatePropertiesFile(): File? {
            val start = File(System.getProperty("user.dir") ?: ".").absoluteFile
            File(start, FILE_NAME).takeIf { it.isFile }?.let { return it }
            return generateSequence(start) { it.parentFile }
                .firstOrNull { File(it, "build.gradle").isFile && File(it, "src/test").isDirectory }
                ?.let { File(it, FILE_NAME) }
                ?.takeIf { it.isFile }
        }

        /**
         * The key named by `apiKeyProperty` in the properties file at `apiKeyFile`, or null when neither is set.
         * Fails, naming the path and property but never the value, when they are set but cannot be resolved.
         */
        private fun readKeyFromFile(propertiesFile: File?, properties: Properties): String? {
            val relativePath = properties.getProperty("apiKeyFile")?.trim()?.takeIf { it.isNotEmpty() }
            val keyName = properties.getProperty("apiKeyProperty")?.trim()?.takeIf { it.isNotEmpty() }
            if (relativePath == null && keyName == null) return null
            check(relativePath != null && keyName != null) {
                "$FILE_NAME must set both apiKeyFile and apiKeyProperty (apiKeyFile=$relativePath, apiKeyProperty=$keyName)"
            }
            val keyFile = File(propertiesFile?.parentFile, relativePath)
            check(keyFile.isFile) { "apiKeyFile ${keyFile.path} does not exist" }
            val secrets = Properties().apply { keyFile.inputStream().use(::load) }
            return secrets.getProperty(keyName)?.unquoted()
                ?: error("Property $keyName is missing or empty in ${keyFile.path}")
        }

        private fun String.unquoted(): String? =
            trim().removeSurrounding("\"").removeSurrounding("'").trim().takeIf { it.isNotEmpty() }
    }
}
