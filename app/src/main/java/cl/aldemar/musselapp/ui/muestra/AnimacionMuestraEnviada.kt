package cl.aldemar.musselapp.ui.muestra

import android.os.Build
import android.provider.Settings
import android.view.HapticFeedbackConstants
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cl.aldemar.musselapp.ui.theme.Acento
import cl.aldemar.musselapp.ui.theme.AcentoSuave
import cl.aldemar.musselapp.ui.theme.SuperficieCampo
import cl.aldemar.musselapp.ui.theme.Texto
import cl.aldemar.musselapp.ui.theme.TextoSecundario
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private val EaseOutQuart = CubicBezierEasing(0.25f, 1f, 0.5f, 1f)
private val EaseOutQuint = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)
private val EaseOutExpo = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)
private val EaseInOutSmooth = CubicBezierEasing(0.4f, 0f, 0.2f, 1f)

/**
 * Confirmación de 1,5 s que se muestra al guardar una muestra: sello verde-mar con un check
 * que se traza, anillo, destello, seis muescas, vibración de confirmación y el código de la
 * muestra. Un toque la salta. [alTerminar] se llama una sola vez.
 *
 * @param codigo el que da `Muestra.codigo`, por ejemplo "Muestra 012".
 */
@Composable
fun AnimacionMuestraEnviada(
    codigo: String,
    alTerminar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val view = LocalView.current
    // Si el usuario apagó las animaciones del sistema, solo aparece y se va
    val movimientoReducido = remember(context) {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }

    // Se llama una vez aunque el toque y el final coincidan
    val aviso by rememberUpdatedState(alTerminar)
    val terminado = remember { booleanArrayOf(false) }
    val terminar = remember {
        {
            if (!terminado[0]) {
                terminado[0] = true
                aviso()
            }
        }
    }

    val inicial = if (movimientoReducido) 1f else 0f
    val entrada = remember { Animatable(0f) }
    val anillo = remember { Animatable(inicial) }
    val halo = remember { Animatable(inicial) }
    val sello = remember { Animatable(inicial) }
    val check = remember { Animatable(inicial) }
    val asiento = remember { Animatable(0f) }
    val destello = remember { Animatable(0f) }
    val muescas = remember { Animatable(0f) }
    val onda = remember { Animatable(0f) }
    val textos = remember { Animatable(0f) }
    val separador = remember { Animatable(inicial) }
    val salida = remember { Animatable(0f) }

    val trazoCompleto = remember { Path() }
    val trazoParcial = remember { Path() }
    val recorteSello = remember { Path() }
    val medida = remember { PathMeasure() }

    LaunchedEffect(Unit) {
        if (movimientoReducido) {
            coroutineScope {
                launch { entrada.animateTo(1f, tween(240, easing = EaseOutQuint)) }
                launch { delay(180); textos.animateTo(1f, tween(300, easing = EaseOutQuint)) }
                launch { delay(1080); salida.animateTo(1f, tween(420, easing = EaseInOutSmooth)) }
            }
            terminar()
            return@LaunchedEffect
        }

        coroutineScope {
            // Fondo (0-180 ms)
            launch { entrada.animateTo(1f, tween(180, easing = EaseOutQuart)) }
            // Anillo, sello y halo (70-460 ms)
            launch { delay(70); anillo.animateTo(1f, tween(390, easing = EaseOutQuint)) }
            launch { delay(90); sello.animateTo(1f, tween(340, easing = EaseOutExpo)) }
            launch { delay(110); halo.animateTo(1f, tween(350, easing = EaseOutQuint)) }
            // Check blanco (320-700 ms)
            launch { delay(320); check.animateTo(1f, tween(380, easing = EaseOutExpo)) }
            // Destello, muescas, onda y vibración (540-1020 ms)
            launch { delay(540); destello.animateTo(1f, tween(380, easing = EaseInOutSmooth)) }
            launch { delay(560); muescas.animateTo(1f, tween(380, easing = EaseOutQuart)) }
            launch { delay(560); onda.animateTo(1f, tween(460, easing = EaseOutQuart)) }
            launch {
                delay(560)
                view.performHapticFeedback(
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) HapticFeedbackConstants.CONFIRM
                    else HapticFeedbackConstants.LONG_PRESS,
                )
                asiento.animateTo(1f, tween(280, easing = EaseInOutSmooth))
            }
            // Textos y separador (520-900 ms)
            launch { delay(520); textos.animateTo(1f, tween(320, easing = EaseOutQuint)) }
            launch { delay(580); separador.animateTo(1f, tween(320, easing = EaseOutExpo)) }
            // Salida (1080-1500 ms)
            launch { delay(1080); salida.animateTo(1f, tween(420, easing = EaseInOutSmooth)) }
        }
        terminar()
    }

    val alfaSalida = 1f - salida.value
    val escalaSalida = 1f - 0.028f * salida.value
    val escalaAsiento = 1f + sin(asiento.value * PI.toFloat()) * 0.036f

    Box(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer { alpha = entrada.value * alfaSalida }
            .background(SuperficieCampo)
            // Además de saltarla, el toque no llega a la pantalla de abajo
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { terminar() },
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.graphicsLayer {
                scaleX = escalaSalida
                scaleY = escalaSalida
                alpha = alfaSalida
            },
        ) {
            Canvas(modifier = Modifier.size(144.dp)) {
                val cx = size.width / 2f
                val cy = size.height / 2f
                val radioSello = size.minDimension * 0.285f
                val radioHalo = size.minDimension * 0.365f
                val radioAnillo = size.minDimension * 0.415f

                // Onda que se expande
                if (onda.value in 0.01f..0.99f) {
                    drawCircle(
                        color = Acento.copy(alpha = (1f - onda.value) * 0.24f),
                        radius = radioAnillo + size.minDimension * 0.13f * onda.value,
                        center = Offset(cx, cy),
                        style = Stroke(width = 1.2.dp.toPx()),
                    )
                }

                // Seis muescas radiales
                if (muescas.value in 0.01f..0.99f) {
                    val envolvente = sin(muescas.value * PI.toFloat())
                    val radioInterior = radioAnillo + 4.dp.toPx() + 5.dp.toPx() * muescas.value
                    val largo = 5.5.dp.toPx() * (1f - 0.35f * muescas.value)
                    for (i in 0 until 6) {
                        val angulo = -PI / 2.0 + (i * PI * 2.0) / 6.0 + PI / 6.0
                        val c = cos(angulo).toFloat()
                        val s = sin(angulo).toFloat()
                        drawLine(
                            color = Acento.copy(alpha = envolvente * 0.52f),
                            start = Offset(cx + c * radioInterior, cy + s * radioInterior),
                            end = Offset(cx + c * (radioInterior + largo), cy + s * (radioInterior + largo)),
                            strokeWidth = 1.5.dp.toPx(),
                            cap = StrokeCap.Round,
                        )
                    }
                }

                scale(scale = escalaAsiento, pivot = Offset(cx, cy)) {
                    if (halo.value > 0f) {
                        drawCircle(
                            color = AcentoSuave.copy(alpha = 0.88f * halo.value),
                            radius = radioHalo * (0.76f + 0.24f * halo.value),
                            center = Offset(cx, cy),
                        )
                    }

                    // Anillo fino que se traza de 0° a 360°
                    if (anillo.value > 0f) {
                        drawArc(
                            color = Acento.copy(alpha = 0.36f),
                            startAngle = -90f,
                            sweepAngle = 360f * anillo.value,
                            useCenter = false,
                            topLeft = Offset(cx - radioAnillo, cy - radioAnillo),
                            size = Size(radioAnillo * 2f, radioAnillo * 2f),
                            style = Stroke(width = 1.4.dp.toPx(), cap = StrokeCap.Round),
                        )
                    }

                    if (sello.value > 0f) {
                        val r = radioSello * (0.62f + 0.38f * sello.value)
                        drawCircle(color = Acento, radius = r, center = Offset(cx, cy))
                        drawCircle(
                            color = Color.White.copy(alpha = 0.28f),
                            radius = (r - 1.dp.toPx()).coerceAtLeast(1f),
                            center = Offset(cx, cy),
                            style = Stroke(width = 1.1.dp.toPx()),
                        )

                        // Destello diagonal dentro del sello
                        if (destello.value in 0.01f..0.99f) {
                            recorteSello.reset()
                            recorteSello.addOval(Rect(cx - r, cy - r, cx + r, cy + r))
                            clipPath(recorteSello) {
                                val x = cx - r * 2.1f + r * 4.2f * destello.value
                                drawRect(
                                    brush = Brush.linearGradient(
                                        colors = listOf(Color.Transparent, Color.White.copy(alpha = 0.22f), Color.Transparent),
                                        start = Offset(x - r * 0.55f, cy - r),
                                        end = Offset(x + r * 0.55f, cy + r),
                                    ),
                                    topLeft = Offset(cx - r, cy - r),
                                    size = Size(r * 2f, r * 2f),
                                )
                            }
                        }

                        // Check blanco que se va trazando
                        if (check.value > 0f) {
                            trazoCompleto.reset()
                            trazoCompleto.moveTo(cx - r * 0.36f, cy + r * 0.03f)
                            trazoCompleto.lineTo(cx - r * 0.08f, cy + r * 0.31f)
                            trazoCompleto.lineTo(cx + r * 0.40f, cy - r * 0.25f)
                            medida.setPath(trazoCompleto, forceClosed = false)
                            trazoParcial.reset()
                            medida.getSegment(0f, medida.length * check.value, trazoParcial, startWithMoveTo = true)
                            drawPath(
                                path = trazoParcial,
                                color = Color.White,
                                style = Stroke(width = 4.2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.graphicsLayer {
                    alpha = textos.value
                    translationY = 6.dp.toPx() * (1f - textos.value)
                },
            ) {
                Text(
                    text = "Muestra enviada",
                    color = Texto,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = (-0.2).sp,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .width(28.dp)
                        .height(1.5.dp)
                        .graphicsLayer { scaleX = separador.value }
                        .background(Acento.copy(alpha = 0.75f), CircleShape),
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "${codigo.uppercase()} · REGISTRADA",
                    color = TextoSecundario,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 1.5.sp,
                )
            }
        }
    }
}
