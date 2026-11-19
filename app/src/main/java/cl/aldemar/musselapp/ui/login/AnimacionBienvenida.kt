package cl.aldemar.musselapp.ui.login

import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cl.aldemar.musselapp.R
import cl.aldemar.musselapp.ui.theme.Acento
import cl.aldemar.musselapp.ui.theme.SuperficieCampo
import cl.aldemar.musselapp.ui.theme.Texto
import cl.aldemar.musselapp.ui.theme.TextoSecundario
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin

private val SuaveQuinta = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)
private val SuaveCuarta = CubicBezierEasing(0.25f, 1f, 0.5f, 1f)
private val SuaveExpo = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)
private val EntradaSalida = CubicBezierEasing(0.4f, 0f, 0.2f, 1f)

/**
 * Animación de bienvenida de 3 segundos después de iniciar sesión: el logo se arma por partes
 * (olas, anillo, valva inferior y horizonte) y la valva superior se abre desde la bisagra.
 * Luego saluda al usuario y da paso a la pantalla principal.
 *
 * Recorta y anima el mismo PNG del logo, sin librerías externas. Si el teléfono tiene las
 * animaciones desactivadas, hace una versión corta sin desplazamientos. Tocar la pantalla la salta.
 */
@Composable
fun AnimacionBienvenida(
    nombre: String,
    subtitulo: String,
    alTerminar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val logo = ImageBitmap.imageResource(id = R.drawable.logo_musselapp)
    val terminar by rememberUpdatedState(alTerminar)
    val sinMovimiento = remember(context) {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }

    val entrada = remember { Animatable(0f) }
    val olaInferior = remember { Animatable(0f) }
    val olaMedia = remember { Animatable(0f) }
    val olaSuperior = remember { Animatable(0f) }
    val anillo = remember { Animatable(0f) }
    val valvaInferior = remember { Animatable(0f) }
    val horizonte = remember { Animatable(0f) }
    val valvaSuperior = remember { Animatable(0f) }
    val respiro = remember { Animatable(0f) }
    val onda = remember { Animatable(0f) }
    val saludo = remember { Animatable(0f) }
    val salida = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        if (sinMovimiento) {
            // Versión accesible: todo aparece junto, sin desplazamientos
            coroutineScope {
                launch { entrada.animateTo(1f, tween(220, easing = SuaveCuarta)) }
                launch {
                    val spec = tween<Float>(520, easing = SuaveQuinta)
                    listOf(olaInferior, olaMedia, olaSuperior, anillo, valvaInferior, horizonte, valvaSuperior)
                        .forEach { a -> launch { a.animateTo(1f, spec) } }
                }
                launch {
                    delay(480)
                    saludo.animateTo(1f, tween(420, easing = SuaveQuinta))
                }
                launch {
                    delay(2320)
                    salida.animateTo(1f, tween(580, easing = EntradaSalida))
                }
            }
            terminar()
            return@LaunchedEffect
        }

        // Coreografía de 3.000 ms
        coroutineScope {
            // 0. Entra el fondo sobre el formulario (0-220 ms)
            launch { entrada.animateTo(1f, tween(220, easing = SuaveCuarta)) }

            // 1. Olas en cascada (140-780 ms)
            launch { delay(140); olaInferior.animateTo(1f, tween(480, easing = SuaveQuinta)) }
            launch { delay(210); olaMedia.animateTo(1f, tween(490, easing = SuaveQuinta)) }
            launch { delay(280); olaSuperior.animateTo(1f, tween(500, easing = SuaveQuinta)) }

            // 2. Se traza el anillo (220-1080 ms)
            launch { delay(220); anillo.animateTo(1f, tween(860, easing = SuaveCuarta)) }

            // 3. Valva inferior y horizonte costero (520-1320 ms)
            launch { delay(520); valvaInferior.animateTo(1f, tween(640, easing = SuaveQuinta)) }
            launch { delay(760); horizonte.animateTo(1f, tween(560, easing = SuaveQuinta)) }

            // 4. La valva superior se abre desde la bisagra (920-1820 ms)
            launch { delay(920); valvaSuperior.animateTo(1f, tween(900, easing = SuaveExpo)) }

            // 5. Pulso concéntrico y saludo (1720-2360 ms)
            launch { delay(1720); saludo.animateTo(1f, tween(500, easing = SuaveQuinta)) }
            launch { delay(1740); onda.animateTo(1f, tween(620, easing = SuaveCuarta)) }
            launch { delay(1760); respiro.animateTo(1f, tween(480, easing = EntradaSalida)) }

            // 6. Salida hacia la pantalla principal (2420-3000 ms)
            launch { delay(2420); salida.animateTo(1f, tween(580, easing = EntradaSalida)) }
        }
        terminar()
    }

    val progresoSalida = salida.value
    val escalaRespiro = 1f + sin(respiro.value * PI.toFloat()) * 0.026f
    val saludoTexto = if (nombre.isNotBlank()) "Hola, $nombre" else "Sesión iniciada"

    Box(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer { alpha = entrada.value * (1f - progresoSalida) }
            .background(SuperficieCampo)
            // Tapa el formulario: los toques no lo atraviesan, y tocar salta la animación
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { terminar() }
            .semantics { contentDescription = "$saludoTexto. Toca para continuar." },
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.graphicsLayer {
                translationY = -22.dp.toPx() * progresoSalida
                val s = (1f - 0.045f * progresoSalida) * escalaRespiro
                scaleX = s
                scaleY = s
            },
        ) {
            Canvas(modifier = Modifier.size(168.dp)) {
                val tinte = ColorFilter.tint(Texto)
                val destino = IntSize(size.width.toInt(), size.height.toInt())

                // Halo concéntrico al terminar de armarse
                if (onda.value > 0f && onda.value < 1f) {
                    val radioBase = size.minDimension * 0.37f
                    drawCircle(
                        color = Acento.copy(alpha = (1f - onda.value) * 0.22f),
                        radius = radioBase + size.minDimension * 0.14f * onda.value,
                        center = Offset(size.width * 0.496f, size.height * 0.478f),
                        style = Stroke(width = 1.5.dp.toPx()),
                    )
                }

                // Ya armado: el logo completo, para que se vea nítido
                if (valvaSuperior.value >= 0.999f && anillo.value >= 0.999f) {
                    drawImage(image = logo, dstSize = destino, colorFilter = tinte)
                    return@Canvas
                }

                // A. Anillo trazándose en sentido horario
                if (anillo.value > 0f) dibujarAnillo(logo, destino, tinte, anillo.value)

                // B. Tres olas de abajo hacia arriba
                dibujarRegion(logo, destino, tinte, rectangulo(0f, 0.765f, 1f, 1f), olaInferior.value,
                    dy = 20.dp.toPx() * (1f - olaInferior.value))
                dibujarRegion(logo, destino, tinte, rectangulo(0.40f, 0.695f, 0.85f, 0.795f), olaMedia.value,
                    dy = 16.dp.toPx() * (1f - olaMedia.value))
                dibujarRegion(logo, destino, tinte, rectangulo(0f, 0.648f, 1f, 0.765f), olaSuperior.value,
                    dy = 14.dp.toPx() * (1f - olaSuperior.value))

                // C. Horizonte y costa entrando desde la derecha
                dibujarRegion(logo, destino, tinte, rectangulo(0.64f, 0.335f, 0.90f, 0.465f), horizonte.value,
                    dx = 16.dp.toPx() * (1f - horizonte.value))

                // D. Valva inferior y chorito asentándose sobre las olas
                dibujarRegion(logo, destino, tinte, contornoValvaInferior(size.width, size.height), valvaInferior.value,
                    dy = 14.dp.toPx() * (1f - valvaInferior.value), escala = 0.93f + 0.07f * valvaInferior.value)

                // E. Valva superior abriéndose desde la bisagra derecha (-15° a 0°)
                if (valvaSuperior.value > 0f) {
                    val bisagra = Offset(size.width * 0.725f, size.height * 0.510f)
                    rotate(degrees = -15f * (1f - valvaSuperior.value), pivot = bisagra) {
                        clipPath(contornoValvaSuperior(size.width, size.height)) {
                            drawImage(
                                image = logo,
                                dstSize = destino,
                                alpha = (valvaSuperior.value * 1.4f).coerceAtMost(1f),
                                colorFilter = tinte,
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.graphicsLayer {
                    alpha = saludo.value * (1f - progresoSalida)
                    translationY = 10.dp.toPx() * (1f - saludo.value)
                },
            ) {
                Text(
                    text = saludoTexto,
                    color = Texto,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = (-0.2).sp,
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = subtitulo.uppercase(),
                    color = TextoSecundario,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 1.6.sp,
                )
            }
        }
    }
}

private fun DrawScope.dibujarAnillo(logo: ImageBitmap, destino: IntSize, tinte: ColorFilter, progreso: Float) {
    val cx = size.width * 0.496f
    val cy = size.height * 0.478f
    val radioExterior = size.minDimension * 0.42f
    val radioInterior = size.minDimension * 0.334f
    val mascaraAnillo = Path().apply {
        addOval(Rect(cx - radioExterior, cy - radioExterior, cx + radioExterior, cy + radioExterior))
        addOval(Rect(cx - radioInterior, cy - radioInterior, cx + radioInterior, cy + radioInterior))
        fillType = PathFillType.EvenOdd
    }
    val cuna = Path().apply {
        moveTo(cx, cy)
        arcTo(Rect(0f, 0f, size.width, size.height), 140f, 360f * progreso, false)
        close()
    }
    clipPath(mascaraAnillo) {
        clipPath(cuna) {
            drawImage(image = logo, dstSize = destino, alpha = (progreso * 2.2f).coerceAtMost(1f), colorFilter = tinte)
        }
    }
}

private fun DrawScope.dibujarRegion(
    logo: ImageBitmap,
    destino: IntSize,
    tinte: ColorFilter,
    recorte: Path,
    progreso: Float,
    dx: Float = 0f,
    dy: Float = 0f,
    escala: Float = 1f,
) {
    if (progreso <= 0f) return
    translate(left = dx, top = dy) {
        scale(scale = escala) {
            clipPath(recorte) {
                drawImage(image = logo, dstSize = destino, alpha = (progreso * 1.5f).coerceAtMost(1f), colorFilter = tinte)
            }
        }
    }
}

private fun DrawScope.rectangulo(izq: Float, arriba: Float, der: Float, abajo: Float): Path = Path().apply {
    addRect(Rect(size.width * izq, size.height * arriba, size.width * der, size.height * abajo))
}

private fun contornoValvaSuperior(w: Float, h: Float): Path = Path().apply {
    moveTo(w * 0.18f, h * 0.19f)
    lineTo(w * 0.76f, h * 0.19f)
    lineTo(w * 0.76f, h * 0.520f)
    lineTo(w * 0.61f, h * 0.458f)
    lineTo(w * 0.52f, h * 0.416f)
    lineTo(w * 0.38f, h * 0.428f)
    lineTo(w * 0.18f, h * 0.360f)
    close()
}

private fun contornoValvaInferior(w: Float, h: Float): Path = Path().apply {
    moveTo(w * 0.17f, h * 0.360f)
    lineTo(w * 0.38f, h * 0.428f)
    lineTo(w * 0.52f, h * 0.416f)
    lineTo(w * 0.61f, h * 0.458f)
    lineTo(w * 0.75f, h * 0.520f)
    lineTo(w * 0.75f, h * 0.655f)
    lineTo(w * 0.17f, h * 0.655f)
    close()
}
