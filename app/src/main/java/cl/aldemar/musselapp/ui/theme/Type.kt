package cl.aldemar.musselapp.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/*
 * Jerarquía de texto en cinco niveles, de más a menos peso:
 *  1. Dato medido      → [DatoMedido]        30 sp negrita (conteo, talla, cholgas)
 *  2. Título pantalla  → headlineSmall       22 sp negrita
 *  3. Título tarjeta   → titleMedium         16 sp seminegrita (también el valor de un campo)
 *  4. Texto de apoyo   → bodyMedium/Small    14 / 13 sp normal, en TextoSecundario
 *  5. Rótulo           → [Rotulo]            11 sp monoespaciada en mayúsculas, en TextoSecundario
 * La negrita queda para los niveles 1 y 2: el resto se distingue por tamaño y color.
 */
val Typography = Typography(
    headlineMedium = TextStyle(fontWeight = FontWeight.Bold, fontSize = 26.sp, lineHeight = 32.sp, letterSpacing = (-0.3).sp),
    headlineSmall = TextStyle(fontWeight = FontWeight.Bold, fontSize = 22.sp, lineHeight = 28.sp, letterSpacing = (-0.3).sp),
    titleLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 26.sp, letterSpacing = (-0.2).sp),
    titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 22.sp, letterSpacing = (-0.1).sp),
    titleSmall = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp),
    bodyLarge = TextStyle(fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontWeight = FontWeight.Normal, fontSize = 13.sp, lineHeight = 18.sp),
    labelLarge = TextStyle(fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp),
    labelMedium = TextStyle(fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp),
    labelSmall = TextStyle(fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 16.sp),
)

/** Nivel 5: rótulo de campo o de métrica. El texto se escribe en mayúsculas. */
val Rotulo = TextStyle(
    fontFamily = FontFamily.Monospace,
    fontWeight = FontWeight.Medium,
    fontSize = 11.sp,
    lineHeight = 14.sp,
    letterSpacing = 0.4.sp,
)

/** Nivel 1: el dato que mide el operario. Cifras de ancho fijo para que no "salten" al escribir. */
val DatoMedido = TextStyle(
    fontWeight = FontWeight.Bold,
    fontSize = 30.sp,
    lineHeight = 34.sp,
    letterSpacing = (-0.5).sp,
    fontFeatureSettings = "tnum",
)
