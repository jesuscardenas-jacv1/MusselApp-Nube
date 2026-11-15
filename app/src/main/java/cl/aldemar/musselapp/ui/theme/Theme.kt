package cl.aldemar.musselapp.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Esquema fijo con la paleta del proyecto: sin color dinámico ni modo oscuro,
// para que la app se vea igual en todos los equipos de terreno.
private val MusselColorScheme = lightColorScheme(
    primary = Principal,
    onPrimary = Color.White,
    // secondary se usa en toda la app como color del texto de apoyo
    secondary = TextoSecundario,
    onSecondary = Color.White,
    tertiary = Acento,
    onTertiary = Color.White,
    tertiaryContainer = AcentoSuave,
    onTertiaryContainer = AcentoTexto,
    background = Fondo,
    onBackground = Texto,
    surface = Superficie,
    onSurface = Texto,
    surfaceVariant = SuperficieCampo,
    onSurfaceVariant = TextoSecundario,
    surfaceContainerLowest = Superficie,
    surfaceContainerLow = Fondo,
    surfaceContainer = SuperficieTenue,
    surfaceContainerHigh = Superficie,
    surfaceContainerHighest = SuperficieCampo,
    outline = BordeCampo,
    outlineVariant = BordeTarjeta,
    error = Error,
)

@Composable
fun MusselAppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = MusselColorScheme,
        typography = Typography,
        content = content
    )
}
