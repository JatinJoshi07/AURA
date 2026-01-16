package com.aura.ui.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val AuraLightColorScheme = lightColorScheme(
    primary = Primary,
    onPrimary = OnPrimary,
    primaryContainer = PrimaryLight.copy(alpha = 0.2f),
    onPrimaryContainer = PrimaryDark,
    secondary = Secondary,
    onSecondary = OnSecondary,
    secondaryContainer = Secondary.copy(alpha = 0.1f),
    onSecondaryContainer = SecondaryDark,
    tertiary = Tertiary,
    onTertiary = OnTertiary,
    background = Background,
    onBackground = OnBackground,
    surface = Surface,
    onSurface = OnSurface,
    error = Error
)

private val AuraDarkColorScheme = darkColorScheme(
    primary = PrimaryLight,
    onPrimary = OnBackground,
    primaryContainer = Primary.copy(alpha = 0.3f),
    onPrimaryContainer = PrimaryLight,
    secondary = Secondary,
    onSecondary = OnBackground,
    secondaryContainer = Secondary.copy(alpha = 0.2f),
    onSecondaryContainer = Secondary,
    tertiary = Tertiary,
    onTertiary = OnBackground,
    background = OnBackground,
    onBackground = Background,
    surface = androidx.compose.ui.graphics.Color(0xFF1E293B),
    onSurface = Background,
    error = Error
)

@Composable
fun AURATheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> AuraDarkColorScheme
        else -> AuraLightColorScheme
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val activity = view.context.findActivity()
            activity?.window?.let { window ->
                window.statusBarColor = colorScheme.primary.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

private fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
