package com.tripian.trpcore.ui.timeline.compose.core

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.tripian.trpcore.R

val TimelineFontFamily = FontFamily(
    Font(R.font.light, FontWeight.Light),
    Font(R.font.regular, FontWeight.Normal),
    Font(R.font.medium, FontWeight.Medium),
    Font(R.font.semibold, FontWeight.SemiBold),
    Font(R.font.bold, FontWeight.Bold)
)

/**
 * Material3 theme for the Compose Timeline UI. Maps the SDK's color and font
 * resources and always renders light, matching the MODE_NIGHT_NO the
 * View-based screens enforce regardless of the host app's theme.
 */
@Composable
fun TimelineTheme(content: @Composable () -> Unit) {
    val colorScheme = lightColorScheme(
        primary = colorResource(R.color.trp_primary),
        onPrimary = colorResource(R.color.trp_white),
        secondary = colorResource(R.color.trp_secondary),
        onSecondary = colorResource(R.color.trp_white),
        background = colorResource(R.color.trp_white),
        onBackground = colorResource(R.color.trp_text_primary),
        surface = colorResource(R.color.trp_white),
        onSurface = colorResource(R.color.trp_text_primary),
        surfaceVariant = colorResource(R.color.trp_bgCloudy),
        onSurfaceVariant = colorResource(R.color.trp_fgWeak),
        outline = colorResource(R.color.trp_bgDisabled),
        error = colorResource(R.color.trp_error_message),
        onError = colorResource(R.color.trp_white)
    )
    MaterialTheme(
        colorScheme = colorScheme,
        typography = timelineTypography(),
        content = content
    )
}

private fun timelineTypography(): Typography {
    val base = Typography()
    return Typography(
        displayLarge = base.displayLarge.copy(fontFamily = TimelineFontFamily),
        displayMedium = base.displayMedium.copy(fontFamily = TimelineFontFamily),
        displaySmall = base.displaySmall.copy(fontFamily = TimelineFontFamily),
        headlineLarge = base.headlineLarge.copy(fontFamily = TimelineFontFamily),
        headlineMedium = base.headlineMedium.copy(fontFamily = TimelineFontFamily),
        headlineSmall = base.headlineSmall.copy(fontFamily = TimelineFontFamily),
        titleLarge = base.titleLarge.copy(fontFamily = TimelineFontFamily),
        titleMedium = base.titleMedium.copy(fontFamily = TimelineFontFamily),
        titleSmall = base.titleSmall.copy(fontFamily = TimelineFontFamily),
        bodyLarge = base.bodyLarge.copy(fontFamily = TimelineFontFamily),
        bodyMedium = base.bodyMedium.copy(fontFamily = TimelineFontFamily),
        bodySmall = base.bodySmall.copy(fontFamily = TimelineFontFamily),
        labelLarge = base.labelLarge.copy(fontFamily = TimelineFontFamily),
        labelMedium = base.labelMedium.copy(fontFamily = TimelineFontFamily),
        labelSmall = base.labelSmall.copy(fontFamily = TimelineFontFamily)
    )
}
