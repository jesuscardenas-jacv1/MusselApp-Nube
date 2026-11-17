package cl.aldemar.musselapp.ui.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cl.aldemar.musselapp.R
import cl.aldemar.musselapp.ui.theme.Acento
import cl.aldemar.musselapp.ui.theme.AcentoSobreOscuro
import cl.aldemar.musselapp.ui.theme.MusselAppTheme
import cl.aldemar.musselapp.ui.theme.Principal
import cl.aldemar.musselapp.ui.theme.SuperficieCampo
import cl.aldemar.musselapp.ui.theme.Texto
import cl.aldemar.musselapp.ui.theme.TextoSecundario
import cl.aldemar.musselapp.ui.theme.TextoSobreOscuro
import kotlin.math.abs
import kotlin.math.pow
import kotlin.random.Random

/** Duración total de la animación de inicio. */
private const val DURACION_MS = 5600

/** Un chorito de la animación: cae, forma el montón y se dispersa (o se convierte en el logo). */
private data class Chorito(
    val protagonista: Boolean,
    val inicioX: Float,
    val inicioY: Float,
    val destinoX: Float,
    val destinoY: Float,
    val sueltaEn: Float,
    val llegaEn: Float,
    val giro: Float,
    val escala: Float,
    val dispersionX: Float,
    val dispersionY: Float,
)

/** Posición, giro, tamaño, opacidad y apertura de un chorito en un instante. */
private data class Cuadro(
    val x: Float,
    val y: Float,
    val rotacion: Float,
    val escala: Float,
    val alfa: Float,
    val apertura: Float,
)

/**
 * Animación de inicio de MusselApp:
 * 1. El nombre aparece al centro.
 * 2. Sube y se desgrana en choritos.
 * 3. Los choritos caen y forman un montón.
 * 4. Queda uno solo, abre su concha y da paso al logo.
 *
 * Al terminar llama a [alTerminar]. Tocar la pantalla la salta.
 */
@Composable
fun SplashScreen(
    alTerminar: () -> Unit,
    modoOscuro: Boolean = false,
) {
    val progreso = remember { Animatable(0f) }
    val terminar by rememberUpdatedState(alTerminar)
    var terminada by remember { mutableStateOf(false) }
    val finalizar = {
        if (!terminada) {
            terminada = true
            terminar()
        }
    }

    LaunchedEffect(Unit) {
        progreso.animateTo(1f, animationSpec = tween(durationMillis = DURACION_MS, easing = LinearEasing))
        finalizar()
    }

    val choritos = remember { crearChoritos(cantidad = 32) }
    val p = progreso.value

    val fondo = if (modoOscuro) Principal else SuperficieCampo
    val colorTitulo = if (modoOscuro) Color(0xFFF8FAFC) else Texto
    val colorAcento = if (modoOscuro) AcentoSobreOscuro else Acento

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(fondo)
            // Tocar en cualquier parte salta la animación
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { finalizar() }
            .semantics { contentDescription = "MusselApp. Toca para continuar." },
        contentAlignment = Alignment.Center,
    ) {
        // Fases 1 y 2: el nombre aparece al centro y sube antes de desgranarse
        if (p <= 0.40f) {
            val entrada = (p / 0.14f).coerceIn(0f, 1f)
            val subida = FastOutSlowInEasing.transform(((p - 0.22f) / 0.14f).coerceIn(0f, 1f))
            val desgrane = 1f - ((p - 0.33f) / 0.07f).coerceIn(0f, 1f)

            Row(
                modifier = Modifier
                    .offset(y = interpolar(0f, -210f, subida).dp)
                    .graphicsLayer {
                        alpha = entrada * desgrane
                        scaleX = 0.90f + 0.10f * entrada
                        scaleY = 0.90f + 0.10f * entrada
                    },
            ) {
                Text(text = "Mussel", color = colorTitulo, fontSize = 36.sp, fontWeight = FontWeight.ExtraBold)
                Text(text = "App", color = colorAcento, fontSize = 36.sp, fontWeight = FontWeight.ExtraBold)
            }
        }

        // Fases 2 a 4: desgrane en choritos, montón, queda uno solo y abre su concha
        Canvas(modifier = Modifier.fillMaxSize()) {
            val ancho = size.width
            val alto = size.height
            // El protagonista se dibuja al final para quedar encima del montón
            choritos.sortedBy { it.protagonista }.forEach { c ->
                if (p < c.sueltaEn) return@forEach
                if (c.protagonista && p >= 0.93f) return@forEach
                val cuadro = calcularCuadro(c, p, ancho, alto)
                if (cuadro.alfa <= 0.01f) return@forEach
                withTransform({
                    translate(left = cuadro.x, top = cuadro.y)
                    rotate(degrees = cuadro.rotacion, pivot = Offset.Zero)
                    scale(scaleX = cuadro.escala, scaleY = cuadro.escala, pivot = Offset.Zero)
                }) {
                    dibujarChorito(
                        colorConcha = if (modoOscuro) TextoSobreOscuro else Texto,
                        colorAcento = colorAcento,
                        apertura = cuadro.apertura,
                        alfa = cuadro.alfa,
                    )
                }
            }
        }

        // Fase 4: logo de MusselApp
        val alfaLogo = ((p - 0.87f) / 0.08f).coerceIn(0f, 1f)
        if (alfaLogo > 0f) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.graphicsLayer {
                    alpha = alfaLogo
                    val s = 0.92f + 0.08f * alfaLogo
                    scaleX = s
                    scaleY = s
                },
            ) {
                Image(
                    painter = painterResource(id = R.drawable.logo_musselapp),
                    contentDescription = null, // el nombre ya se lee en el texto de abajo
                    modifier = Modifier.size(196.dp),
                    colorFilter = if (modoOscuro) ColorFilter.tint(Color.White) else null,
                )
                Spacer(modifier = Modifier.height(14.dp))
                Row {
                    Text(text = "Mussel", color = colorTitulo, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
                    Text(text = "App", color = colorAcento, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
                }
                Text(
                    text = "Registro y muestreo de choritos",
                    color = if (modoOscuro) TextoSobreOscuro else TextoSecundario,
                    fontSize = 13.sp,
                )
            }
        }
    }
}

/** Choritos con semilla fija: la animación se ve igual cada vez. */
private fun crearChoritos(cantidad: Int): List<Chorito> {
    val azar = Random(412)
    val indiceProtagonista = cantidad / 2
    return List(cantidad) { i ->
        val protagonista = i == indiceProtagonista
        val inicioX = 0.25f + (i.toFloat() / cantidad) * 0.50f + (azar.nextFloat() - 0.5f) * 0.04f
        val dispersion = (azar.nextFloat() + azar.nextFloat()) * 0.5f - 0.5f
        val destinoX = if (protagonista) 0.5f else 0.5f + dispersion * 0.42f
        // Más alto al centro y más bajo a los lados: forma de montón
        val distancia = abs(destinoX - 0.5f) / 0.22f
        val destinoY = if (protagonista) 0.69f else (0.68f + distancia.pow(1.5f) * 0.08f) + azar.nextFloat() * 0.08f
        Chorito(
            protagonista = protagonista,
            inicioX = inicioX,
            inicioY = 0.18f,
            destinoX = destinoX,
            destinoY = destinoY,
            sueltaEn = 0.31f + (i % 9) * 0.01f,
            llegaEn = if (protagonista) 0.61f else 0.56f + azar.nextFloat() * 0.10f,
            giro = (if (azar.nextBoolean()) 1f else -1f) * (180f + azar.nextFloat() * 240f),
            escala = if (protagonista) 1.2f else 0.8f + azar.nextFloat() * 0.3f,
            dispersionX = (if (destinoX < 0.5f) -1f else 1f) * (0.14f + azar.nextFloat() * 0.2f),
            dispersionY = 0.05f + azar.nextFloat() * 0.10f,
        )
    }
}

private fun calcularCuadro(c: Chorito, p: Float, ancho: Float, alto: Float): Cuadro {
    // Caída con gravedad hasta el montón
    if (p < c.llegaEn) {
        val t = ((p - c.sueltaEn) / (c.llegaEn - c.sueltaEn)).coerceIn(0f, 1f)
        val x = interpolar(c.inicioX, c.destinoX, 1f - (1f - t).pow(1.8f)) * ancho
        val y = interpolar(c.inicioY, c.destinoY, t * t) * alto
        return Cuadro(x, y, (1f - t) * c.giro, c.escala, 1f, 0f)
    }
    // Los demás se dispersan y desaparecen
    if (!c.protagonista) {
        val t = ((p - 0.68f) / 0.13f).coerceIn(0f, 1f)
        val x = (c.destinoX + c.dispersionX * t) * ancho
        val y = (c.destinoY + c.dispersionY * t) * alto
        return Cuadro(x, y, t * 45f, c.escala * (1f - t * 0.35f), 1f - t, 0f)
    }
    // El protagonista espera solo en el centro
    if (p < 0.77f) {
        return Cuadro(c.destinoX * ancho, c.destinoY * alto, 0f, c.escala * 1.1f, 1f, 0f)
    }
    // Sube, crece, abre la concha y se desvanece en el logo
    val subida = ((p - 0.77f) / 0.11f).coerceIn(0f, 1f)
    val apertura = ((p - 0.81f) / 0.09f).coerceIn(0f, 1f)
    val desvanece = ((p - 0.88f) / 0.05f).coerceIn(0f, 1f)
    return Cuadro(
        x = interpolar(c.destinoX * ancho, ancho * 0.5f, subida),
        y = interpolar(c.destinoY * alto, alto * 0.45f, subida),
        rotacion = 0f,
        escala = interpolar(c.escala * 1.1f, 3.0f, subida),
        alfa = 1f - desvanece,
        apertura = apertura,
    )
}

/** Chorito vectorial: concha alargada que se abre y deja ver la carne. */
private fun DrawScope.dibujarChorito(colorConcha: Color, colorAcento: Color, apertura: Float, alfa: Float) {
    val concha = Path().apply {
        moveTo(-18f, 1f)
        cubicTo(-15f, -8f, -1f, -10f, 14f, -2.5f)
        cubicTo(18f, -0.5f, 18f, 2.5f, 13f, 4.2f)
        cubicTo(-1f, 9f, -15f, 7f, -18f, 1f)
        close()
    }
    withTransform({ rotate(degrees = -apertura * 32f, pivot = Offset(14f, 2f)) }) {
        drawPath(path = concha, color = colorConcha.copy(alpha = alfa))
    }
    if (apertura > 0.05f) {
        drawOval(
            color = colorAcento.copy(alpha = alfa * apertura),
            topLeft = Offset(-10f, -3f),
            size = Size(18f, 8f),
        )
    }
}

private fun interpolar(a: Float, b: Float, t: Float): Float = a + (b - a) * t

@Preview(showBackground = true, widthDp = 360, heightDp = 760)
@Composable
private fun SplashScreenPreview() {
    MusselAppTheme { SplashScreen(alTerminar = {}) }
}
