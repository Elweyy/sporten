package fr.loic.seances.ui

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import fr.loic.seances.R

/**
 * Thèmes de l'app. Mêmes couleurs que la web app (css/style.css, [data-theme=…]) ;
 * Liquid Glass et Material You n'existent que côté web.
 */
enum class AppThemeId(val label: String, val hint: String) {
    SPORTEN("Sporten classique", "Le thème d'origine"),
    SYSTEM("Suivre le système", "Clair ou sombre comme le téléphone"),
    LIGHT("Clair", "Fond blanc, lisible en plein soleil"),
    OLED("Noir OLED", "Noir pur : sobre et économe en batterie"),
    NEON("Néon", "Synthwave des années 80 : rose, cyan et lueurs"),
    ARCADE("Arcade", "Rétro : police pixel, couleurs vives, LEVEL COMPLETE"),
    PASTEL("Pastel", "Doux et clair : corail, menthe et lilas"),
    ;

    companion object {
        fun of(name: String?) = entries.firstOrNull { it.name == name } ?: SPORTEN
    }
}

/** Couleurs d'un thème concret ([AppThemeId.SYSTEM] devient LIGHT ou SPORTEN). */
data class ThemeColors(
    val id: AppThemeId,
    val background: Color,
    val surface: Color,
    val surfaceHigh: Color,
    val text: Color,
    val textMuted: Color,
    val work: Color,
    val onWork: Color,
    val rest: Color,
    val prepare: Color,
    val danger: Color = Color(0xFFFF6B6B),
    val warn: Color = Color(0xFFFFC857),
    /** Texte d'accent (titres de section…) quand [work] est trop pâle pour être lu. */
    val accentText: Color = work,
    val light: Boolean = false,
)

object Themes {
    val SPORTEN = ThemeColors(
        AppThemeId.SPORTEN, Color(0xFF121418), Color(0xFF1C2027), Color(0xFF262B34), Color(0xFFF2F3F5),
        Color(0xFF9AA3AF), Color(0xFFFF7A45), Color.Black, Color(0xFF3CC8A0), Color(0xFF5B9DFF),
    )
    val LIGHT = ThemeColors(
        AppThemeId.LIGHT, Color(0xFFF2F3F6), Color.White, Color(0xFFE4E7EC), Color(0xFF15171C),
        Color(0xFF5D6573), Color(0xFFE4541B), Color.White, Color(0xFF0E8A66), Color(0xFF2B67D1),
        danger = Color(0xFFD42F2F), warn = Color(0xFFA86200), light = true,
    )
    val OLED = SPORTEN.copy(
        id = AppThemeId.OLED, background = Color.Black, surface = Color(0xFF0C0D10), surfaceHigh = Color(0xFF1C1E23),
        textMuted = Color(0xFF8E96A3),
    )
    val NEON = ThemeColors(
        AppThemeId.NEON, Color(0xFF0F0620), Color(0xFF1B0B36), Color(0xFF2C1456), Color(0xFFFBEFFF),
        Color(0xFFB9A2D8), Color(0xFFFF3EA5), Color(0xFF16041F), Color(0xFF2DF5FF), Color(0xFFB47CFF),
        danger = Color(0xFFFF5470), warn = Color(0xFFFFD23F),
    )
    val ARCADE = ThemeColors(
        AppThemeId.ARCADE, Color(0xFF0B0B19), Color(0xFF17172E), Color(0xFF2A2A52), Color(0xFFFFF1E8),
        Color(0xFFA3A3CC), Color(0xFFFFEC27), Color(0xFF0B0B19), Color(0xFF00E436), Color(0xFF29ADFF),
        danger = Color(0xFFFF004D), warn = Color(0xFFFFA300),
    )
    val PASTEL = ThemeColors(
        AppThemeId.PASTEL, Color(0xFFFBF6F1), Color.White, Color(0xFFF1E8F3), Color(0xFF3D3549),
        Color(0xFF877D93), Color(0xFFF4A196), Color(0xFF3D3549), Color(0xFF8ED6BC), Color(0xFFA9BCF5),
        danger = Color(0xFFD9534F), warn = Color(0xFFB26E12), accentText = Color(0xFFC9564A), light = true,
    )

    fun resolve(id: AppThemeId, systemDark: Boolean): ThemeColors = when (id) {
        AppThemeId.SPORTEN -> SPORTEN
        AppThemeId.SYSTEM -> if (systemDark) SPORTEN else LIGHT
        AppThemeId.LIGHT -> LIGHT
        AppThemeId.OLED -> OLED
        AppThemeId.NEON -> NEON
        AppThemeId.ARCADE -> ARCADE
        AppThemeId.PASTEL -> PASTEL
    }
}

/** Couleurs du thème courant. Lues pendant la composition : changer de thème redessine tout. */
object Palette {
    var current by mutableStateOf(Themes.SPORTEN)

    val background get() = current.background
    val surface get() = current.surface
    val surfaceHigh get() = current.surfaceHigh
    val text get() = current.text
    val textMuted get() = current.textMuted
    val work get() = current.work
    val onWork get() = current.onWork
    val rest get() = current.rest
    val prepare get() = current.prepare
    val danger get() = current.danger
    val warn get() = current.warn
    val accentText get() = current.accentText
    val isLight get() = current.light
    val isArcade get() = current.id == AppThemeId.ARCADE
    val isNeon get() = current.id == AppThemeId.NEON
}

// Police pixel (Press Start 2P, licence OFL). Déclarée aussi en gras pour éviter un faux gras flou.
private val PixelFont = FontFamily(Font(R.font.press_start_2p, FontWeight.Normal), Font(R.font.press_start_2p, FontWeight.Bold))

/** Forme arrondie, ou à angles droits pour le thème Arcade. */
fun themedShape(radius: Dp): Shape = if (Palette.isArcade) RectangleShape else RoundedCornerShape(radius)

/**
 * Style des titres et grands chiffres : police pixel (plus petite, elle est très large) en Arcade,
 * italique lumineux en Néon.
 */
fun titleStyle(size: Int): TextStyle = when {
    Palette.isArcade -> TextStyle(fontFamily = PixelFont, fontSize = (size * 0.62f).sp, lineHeight = (size * 0.95f).sp)
    Palette.isNeon -> TextStyle(
        fontSize = size.sp, fontWeight = FontWeight.ExtraBold, fontStyle = FontStyle.Italic,
        shadow = Shadow(Palette.work.copy(alpha = 0.9f), Offset.Zero, 18f),
    )
    else -> TextStyle(fontSize = size.sp, fontWeight = FontWeight.Bold)
}

/** Fond propre au thème (dégradés, grille synthwave, lignes de balayage) dessiné autour du contenu. */
fun Modifier.themeBackdrop(): Modifier = drawWithContent {
    val t = Palette.current
    drawRect(t.background)
    when (t.id) {
        AppThemeId.NEON -> {
            drawRect(Brush.verticalGradient(listOf(Color(0xFF0F0620), Color(0xFF170833), Color(0xFF2A0A3F))))
            drawRect(
                Brush.radialGradient(
                    listOf(Color(0x59FF3EA5), Color.Transparent),
                    center = Offset(size.width / 2, size.height), radius = size.width * 0.9f,
                ),
            )
            // Sol quadrillé qui file vers l'horizon.
            val horizon = size.height * 0.64f
            val depth = size.height - horizon
            val line = Brush.verticalGradient(listOf(Color.Transparent, Color(0x70FF3EA5)), startY = horizon, endY = size.height)
            for (i in 1..9) {
                val f = i / 9f
                val y = horizon + depth * f * f
                drawLine(line, Offset(0f, y), Offset(size.width, y), strokeWidth = 2f)
            }
            val cx = size.width / 2
            for (k in -10..10) {
                val bottomX = cx + k * size.width * 0.16f
                drawLine(line, Offset(cx + k * size.width * 0.02f, horizon), Offset(bottomX, size.height), strokeWidth = 2f)
            }
        }
        AppThemeId.PASTEL -> {
            fun blob(x: Float, y: Float, color: Color) = drawRect(
                Brush.radialGradient(listOf(color, Color.Transparent), Offset(size.width * x, size.height * y), size.width * 0.7f),
            )
            blob(0.95f, 0.03f, Color(0x8CF7C8E1))
            blob(0f, 0.55f, Color(0x99C8ECDE))
            blob(1f, 1f, Color(0x99D4DCFA))
        }
        else -> Unit
    }
    drawContent()
    if (t.id == AppThemeId.ARCADE) {
        // Lignes de balayage d'écran cathodique, très légères.
        var y = 0f
        while (y < size.height) {
            drawRect(Color(0x24000000), Offset(0f, y), androidx.compose.ui.geometry.Size(size.width, 1.5f))
            y += 4.5f
        }
    }
}

@Composable
fun AppTheme(themeId: AppThemeId = AppThemeId.SPORTEN, content: @Composable () -> Unit) {
    val colors = Themes.resolve(themeId, isSystemInDarkTheme())
    if (Palette.current != colors) Palette.current = colors

    // Icônes de la barre d'état et de navigation : sombres sur les thèmes clairs.
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = colors.light
                isAppearanceLightNavigationBars = colors.light
            }
        }
    }

    val scheme = if (colors.light) {
        lightColorScheme(
            primary = colors.work, onPrimary = colors.onWork, secondary = colors.rest,
            background = colors.background, onBackground = colors.text, surface = colors.surface, onSurface = colors.text,
            surfaceVariant = colors.surfaceHigh, onSurfaceVariant = colors.textMuted,
            secondaryContainer = colors.surfaceHigh, onSecondaryContainer = colors.text, error = colors.danger,
        )
    } else {
        darkColorScheme(
            primary = colors.work, onPrimary = colors.onWork, secondary = colors.rest,
            background = colors.background, onBackground = colors.text, surface = colors.surface, onSurface = colors.text,
            surfaceVariant = colors.surfaceHigh, onSurfaceVariant = colors.textMuted,
            secondaryContainer = colors.surfaceHigh, onSecondaryContainer = colors.text, error = colors.danger,
        )
    }
    val base = Typography()
    val typography = if (themeId == AppThemeId.ARCADE) {
        val mono = FontFamily.Monospace
        base.copy(
            bodyLarge = base.bodyLarge.copy(fontFamily = mono), bodyMedium = base.bodyMedium.copy(fontFamily = mono),
            bodySmall = base.bodySmall.copy(fontFamily = mono), labelLarge = base.labelLarge.copy(fontFamily = mono),
            labelMedium = base.labelMedium.copy(fontFamily = mono), titleMedium = base.titleMedium.copy(fontFamily = mono),
        )
    } else {
        base
    }
    MaterialTheme(colorScheme = scheme, typography = typography, content = content)
}
