package cl.aldemar.musselapp.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// Formas y bordes comunes: así todas las tarjetas de la app se separan del fondo igual.

val FormaTarjeta = RoundedCornerShape(20.dp)
val FormaTarjetaChica = RoundedCornerShape(16.dp)
val FormaCampo = RoundedCornerShape(12.dp)
val FormaBoton = RoundedCornerShape(14.dp)

/** Contorno de tarjeta, para usar en `Surface(border = …)`. */
val BordeDeTarjeta = BorderStroke(1.dp, BordeTarjeta)

/** Contorno de campo y de opción sin elegir. */
val BordeDeCampo = BorderStroke(1.dp, BordeCampo)

/** Elevación de las tarjetas cuando se dibujan con `Surface(shadowElevation = …)`. */
val ElevacionTarjeta: Dp = 3.dp

private val ColorSombra = Color(0xFF0F172A)

/**
 * Tarjeta blanca con borde y sombra sobre el fondo. Se aplica antes de `clickable` y de `padding`.
 * [borde] permite marcar un error o el foco con otro color.
 */
fun Modifier.tarjetaMussel(
    forma: Shape = FormaTarjeta,
    borde: BorderStroke = BordeDeTarjeta,
    fondo: Color = Superficie,
): Modifier = this
    .shadow(ElevacionTarjeta, forma, clip = true, ambientColor = ColorSombra, spotColor = ColorSombra)
    .background(fondo, forma)
    .border(borde, forma)
