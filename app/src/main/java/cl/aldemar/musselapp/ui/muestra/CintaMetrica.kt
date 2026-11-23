package cl.aldemar.musselapp.ui.muestra

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cl.aldemar.musselapp.ui.theme.BordeDeTarjeta
import cl.aldemar.musselapp.ui.theme.DatoMedido
import cl.aldemar.musselapp.ui.theme.FormaTarjetaChica
import cl.aldemar.musselapp.ui.theme.tarjetaMussel
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

// Colores de la app: fondo verde suave y marcas en el verde de acento (5,5:1 sobre blanco)
private val FondoCinta = Paleta.AcentoSuave
private val MarcaCinta = Paleta.Acento

/** Separación entre dos centímetros de la cinta. */
private val PASO_CM = 12.dp

/**
 * Tramo muestreado como una cinta métrica: se arrastra la cinta bajo la marca fija y se lee el
 * largo en centímetros. Tiene inercia, se ajusta al centímetro y vibra suave cada 5 cm. [valor] null
 * significa que todavía no se midió (el tramo es obligatorio para guardar).
 */
@Composable
internal fun CintaMetrica(
    valor: Int?,
    maximo: Int,
    error: String?,
    onCambiar: (Int?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val densidad = LocalDensity.current
    val vista = LocalView.current
    val pasoPx = with(densidad) { PASO_CM.toPx() }
    val alCambiar by rememberUpdatedState(onCambiar)
    val alcance = rememberCoroutineScope()

    // Desplazamiento de la cinta en px: 0 es el 0 cm bajo la marca
    val desplazamiento = remember { Animatable((valor ?: 0) * pasoPx) }
    // Hasta que el operario la mueve, la cinta no inventa un valor
    var tocada by remember { mutableStateOf(valor != null) }
    var arrastrando by remember { mutableStateOf(false) }

    LaunchedEffect(pasoPx, maximo) { desplazamiento.updateBounds(0f, maximo * pasoPx) }

    // Mientras se mueve, el valor sigue a la cinta (al soltar queda en un centímetro exacto)
    LaunchedEffect(pasoPx) {
        var ultimo = valor
        snapshotFlow { (desplazamiento.value / pasoPx).roundToInt() }.collect { cm ->
            if (!tocada) return@collect
            val nuevo = cm.coerceIn(1, maximo)
            if (nuevo != ultimo) {
                if (nuevo % 5 == 0) vista.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                ultimo = nuevo
                alCambiar(nuevo)
            }
        }
    }

    // Si el tramo cambia desde afuera (por ejemplo, escrito en el conteo sobre la foto), la cinta lo sigue
    LaunchedEffect(valor) {
        if (arrastrando || desplazamiento.isRunning) return@LaunchedEffect
        if (valor == null) {
            tocada = false
            desplazamiento.snapTo(0f)
        } else if ((desplazamiento.value / pasoPx).roundToInt() != valor) {
            tocada = true
            // De un salto: animarla pasaría por todos los valores intermedios
            desplazamiento.snapTo(valor * pasoPx)
        }
    }

    val medidor = rememberTextMeasurer()
    val estiloNumero = TextStyle(color = MarcaCinta, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    val borde = if (error != null) BorderStroke(1.5.dp, Paleta.Error) else BordeDeTarjeta

    Column(
        modifier = modifier
            .tarjetaMussel(FormaTarjetaChica, borde)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        // Compacta: rótulo y valor en una sola línea
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextoRotulo("Tramo muestreado", modifier = Modifier.weight(1f))
            Text(
                text = valor?.toString() ?: "—",
                style = DatoMedido,
                color = if (valor == null) Paleta.TextoSuave else Paleta.Texto,
            )
            Text(
                " cm",
                color = Paleta.TextoSuave,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(top = 6.dp),
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(RoundedCornerShape(10.dp))
                // TalkBack: se lee como un control deslizable y se ajusta con los gestos de volumen
                .semantics {
                    contentDescription = "Tramo muestreado, cinta métrica"
                    stateDescription = valor?.let { "$it centímetros" } ?: "Sin medir"
                    progressBarRangeInfo = ProgressBarRangeInfo((valor ?: 0).toFloat(), 0f..maximo.toFloat(), maximo)
                    setProgress { objetivo ->
                        val cm = objetivo.roundToInt().coerceIn(1, maximo)
                        tocada = true
                        alcance.launch { desplazamiento.snapTo(cm * pasoPx) }
                        alCambiar(cm)
                        true
                    }
                    if (error != null) error(error)
                }
                .pointerInput(pasoPx) {
                    val velocidad = VelocityTracker()
                    detectHorizontalDragGestures(
                        onDragStart = {
                            arrastrando = true
                            tocada = true
                            velocidad.resetTracking()
                            alcance.launch { desplazamiento.stop() }
                        },
                        onDragEnd = {
                            arrastrando = false
                            val v = -velocidad.calculateVelocity().x
                            alcance.launch {
                                desplazamiento.animateDecay(v, exponentialDecay(frictionMultiplier = 2.5f))
                                // Siempre termina en un centímetro exacto
                                desplazamiento.animateTo((desplazamiento.value / pasoPx).roundToInt() * pasoPx, spring())
                            }
                        },
                        onDragCancel = { arrastrando = false },
                    ) { cambio, delta ->
                        velocidad.addPosition(cambio.uptimeMillis, cambio.position)
                        alcance.launch { desplazamiento.snapTo(desplazamiento.value - delta) }
                    }
                },
        ) {
            Canvas(Modifier.matchParentSize()) {
                drawRect(FondoCinta)
                val centro = size.width / 2f
                val primero = ((desplazamiento.value - centro) / pasoPx).toInt().coerceAtLeast(0)
                val ultimo = ((desplazamiento.value + centro) / pasoPx).toInt() + 1
                for (cm in primero..minOf(ultimo, maximo)) {
                    val x = centro + cm * pasoPx - desplazamiento.value
                    val largo = when {
                        cm % 10 == 0 -> size.height * 0.45f
                        cm % 5 == 0 -> size.height * 0.32f
                        else -> size.height * 0.18f
                    }
                    drawLine(MarcaCinta, Offset(x, 0f), Offset(x, largo), strokeWidth = if (cm % 10 == 0) 2.dp.toPx() else 1.dp.toPx())
                    if (cm % 10 == 0) {
                        val texto = medidor.measure(cm.toString(), estiloNumero)
                        drawText(texto, topLeft = Offset(x - texto.size.width / 2f, size.height - texto.size.height - 4.dp.toPx()))
                    }
                }
                // Marca fija de lectura: línea verde más gruesa con una punta arriba
                drawLine(Paleta.AcentoTexto, Offset(centro, 0f), Offset(centro, size.height), strokeWidth = 2.5.dp.toPx())
                val punta = Path().apply {
                    moveTo(centro - 6.dp.toPx(), 0f)
                    lineTo(centro + 6.dp.toPx(), 0f)
                    lineTo(centro, 8.dp.toPx())
                    close()
                }
                drawPath(punta, Paleta.AcentoTexto)
            }
        }

        when {
            error != null -> Text(error, color = Paleta.Error, style = MaterialTheme.typography.labelMedium)
            valor == null -> Text(
                "Desliza la cinta: largo de cuerda que se contó",
                color = Paleta.AcentoTexto,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}
