package cl.aldemar.musselapp.ui.login

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import cl.aldemar.musselapp.data.model.Rol
import cl.aldemar.musselapp.ui.theme.Acento
import cl.aldemar.musselapp.ui.theme.AcentoSuave
import cl.aldemar.musselapp.ui.theme.AcentoTexto
import cl.aldemar.musselapp.ui.theme.BordeTarjeta
import cl.aldemar.musselapp.ui.theme.Linea
import cl.aldemar.musselapp.ui.theme.MusselAppTheme
import cl.aldemar.musselapp.ui.theme.Principal
import cl.aldemar.musselapp.ui.theme.Superficie
import cl.aldemar.musselapp.ui.theme.SuperficieTenue
import cl.aldemar.musselapp.ui.theme.Texto
import cl.aldemar.musselapp.ui.theme.TextoSecundario
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.sin

private const val DOS_PI = (2 * PI).toFloat()

// Colores de la escena del mar (solo para esta animación)
private val CieloArriba = Color(0xFFD7ECF5)
private val CieloAbajo = Color(0xFFF3F8FB)
private val MarLejos = Color(0xFF8CC3CF)
private val MarMedio = Color(0xFF3F97A6)
private val Boya = Color(0xFFF97316)
private val Espuma = Color(0xFFFFFFFF)

/** Acelera y frena con suavidad: la lancha navega en vez de entrar de golpe. */
private val LlegadaSuave = CubicBezierEasing(0.45f, 0.05f, 0.25f, 1f)

/**
 * Transición breve al elegir el perfil, antes de pedir las credenciales.
 * Operario: una lancha de apoyo navega entre las boyas del centro de cultivo.
 * Supervisor: una ficha de revisión se va validando punto por punto.
 */
@Composable
fun AnimacionPerfil(rol: Rol, onTerminada: () -> Unit, modifier: Modifier = Modifier) {
    val forma = RoundedCornerShape(16.dp)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp)
            .clip(forma)
            .border(1.dp, BordeTarjeta, forma)
            .semantics { contentDescription = "Ingresando como ${rol.etiqueta}" },
    ) {
        when (rol) {
            Rol.OPERARIO -> EscenaMar(onTerminada)
            Rol.SUPERVISOR -> EscenaRevision(onTerminada)
        }
        Column(Modifier.align(Alignment.TopStart).padding(start = 18.dp, top = 16.dp)) {
            Text(
                rol.etiqueta,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Texto,
            )
            Text(
                if (rol == Rol.OPERARIO) "Zarpando hacia el centro de cultivo…" else "Preparando la revisión…",
                style = MaterialTheme.typography.bodySmall,
                color = TextoSecundario,
            )
        }
    }
}

/** Segundos transcurridos desde que la escena aparece, cuadro a cuadro (movimiento continuo y fluido). */
@Composable
private fun relojEscena(): Float {
    var segundos by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        val inicio = withFrameNanos { it }
        while (true) {
            withFrameNanos { segundos = (it - inicio) / 1_000_000_000f }
        }
    }
    return segundos
}

// ---------- Operario ----------

/** Altura del oleaje en x: dos ondas superpuestas para que no se vea mecánico. */
private fun alturaOla(x: Float, t: Float, ancho: Float, amplitud: Float): Float {
    val k1 = DOS_PI / (ancho * 0.55f)
    val k2 = DOS_PI / (ancho * 0.23f)
    return amplitud * (sin(k1 * x - t * 1.9f) + 0.35f * sin(k2 * x + t * 2.7f))
}

/** Pendiente del oleaje en x (para inclinar el barco siguiendo la ola). */
private fun pendienteOla(x: Float, t: Float, ancho: Float, amplitud: Float): Float {
    val k1 = DOS_PI / (ancho * 0.55f)
    val k2 = DOS_PI / (ancho * 0.23f)
    return amplitud * (k1 * cos(k1 * x - t * 1.9f) + 0.35f * k2 * cos(k2 * x + t * 2.7f))
}

@Composable
private fun EscenaMar(onTerminada: () -> Unit) {
    val alTerminar by rememberUpdatedState(onTerminada)
    val travesia = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        travesia.animateTo(1f, tween(durationMillis = 2100, easing = LlegadaSuave))
        alTerminar()
    }
    val t = relojEscena()

    Canvas(Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val linea = h * 0.70f
        val amp = 3.5.dp.toPx()

        // Cielo y sol
        drawRect(Brush.verticalGradient(listOf(CieloArriba, CieloAbajo), endY = linea))
        drawCircle(
            Brush.radialGradient(
                listOf(Color(0xFFFFF4D6), Color(0x00FFF4D6)),
                center = Offset(w * 0.80f, h * 0.30f),
                radius = h * 0.42f,
            ),
            radius = h * 0.42f,
            center = Offset(w * 0.80f, h * 0.30f),
        )
        drawCircle(Color(0xFFFFE7A8), radius = h * 0.075f, center = Offset(w * 0.80f, h * 0.30f))

        // Mar de fondo (se mueve más lento: da profundidad)
        dibujarMar(linea - 10.dp.toPx(), t * 0.55f, amp * 0.6f, MarLejos)

        // Línea de cultivo: boyas naranjas unidas por el cabo, meciéndose con el mar
        val yCabo = linea - 3.dp.toPx()
        val boyas = (0..6).map { i ->
            val x = w * (0.06f + i * 0.15f)
            Offset(x, yCabo + alturaOla(x, t * 0.8f, w, amp * 0.7f))
        }
        val cabo = Path().apply {
            moveTo(boyas.first().x, boyas.first().y)
            boyas.drop(1).forEach { lineTo(it.x, it.y) }
        }
        drawPath(cabo, Color(0xFF2B6F7C).copy(alpha = 0.55f), style = Stroke(1.2.dp.toPx()))
        boyas.forEach { b ->
            drawCircle(Boya, radius = 3.6.dp.toPx(), center = b)
            drawCircle(Color.White.copy(alpha = 0.55f), radius = 1.2.dp.toPx(), center = b + Offset(-1.dp.toPx(), -1.2.dp.toPx()))
        }

        // Mar medio
        dibujarMar(linea, t, amp, MarMedio)

        // Lancha: entra desde la izquierda, sigue la altura y la pendiente de la ola
        val largo = 92.dp.toPx()
        val xBarco = -largo + (w * 0.60f + largo) * travesia.value
        val yBarco = linea + alturaOla(xBarco, t, w, amp) + 1.5.dp.toPx()
        val inclinacion = Math.toDegrees(atan(pendienteOla(xBarco, t, w, amp).toDouble())).toFloat() * 0.45f
        val velocidad = 1f - travesia.value // estela más larga mientras avanza rápido

        dibujarEstela(xBarco, yBarco, largo, velocidad, t)
        translate(xBarco, yBarco) {
            rotate(inclinacion, pivot = Offset.Zero) { dibujarLancha(largo, t) }
        }

        // Ola delantera: tapa la quilla para que la lancha flote "dentro" del agua
        dibujarMar(linea + 7.dp.toPx(), t * 1.25f + 1.4f, amp * 0.8f, Acento.copy(alpha = 0.92f))
    }
}

/** Mar relleno desde la ola hasta el fondo, con un brillo suave en la cresta. */
private fun DrawScope.dibujarMar(base: Float, t: Float, amplitud: Float, color: Color) {
    val mar = Path().apply {
        moveTo(0f, size.height)
        var x = 0f
        while (x <= size.width + 4f) {
            lineTo(x, base + alturaOla(x, t, size.width, amplitud))
            x += 4f
        }
        lineTo(size.width, size.height)
        close()
    }
    drawPath(mar, color)
    val cresta = Path().apply {
        var x = 0f
        moveTo(0f, base + alturaOla(0f, t, size.width, amplitud))
        while (x <= size.width + 4f) {
            lineTo(x, base + alturaOla(x, t, size.width, amplitud))
            x += 4f
        }
    }
    drawPath(cresta, Color.White.copy(alpha = 0.28f), style = Stroke(1.dp.toPx(), cap = StrokeCap.Round))
}

/** Espuma que deja la lancha: burbujas que se alejan y se desvanecen. */
private fun DrawScope.dibujarEstela(x: Float, y: Float, largo: Float, velocidad: Float, t: Float) {
    if (velocidad <= 0.02f) return
    repeat(7) { i ->
        val f = i / 7f
        val dx = largo * (0.45f + f * 1.1f * velocidad)
        val radio = (2.5f + 3f * (1f - f)) * density * velocidad.coerceAtLeast(0.3f)
        val vaiven = sin(t * 6f + i) * 1.2f * density
        drawCircle(
            Espuma.copy(alpha = 0.55f * (1f - f) * velocidad.coerceAtMost(1f)),
            radius = radio,
            center = Offset(x - dx, y + 2.dp.toPx() + vaiven),
        )
    }
}

/**
 * Lancha de apoyo dibujada con trazos propios (no un ícono): casco azul marino con franja
 * turquesa, cabina blanca con ventanas y un mástil con banderín. Origen = centro de la línea de flotación.
 */
private fun DrawScope.dibujarLancha(largo: Float, t: Float) {
    val l = largo
    val casco = Path().apply {
        moveTo(-0.48f * l, -0.13f * l)                    // popa, cubierta
        lineTo(0.40f * l, -0.13f * l)
        quadraticTo(0.50f * l, -0.14f * l, 0.54f * l, -0.20f * l) // proa levantada
        quadraticTo(0.47f * l, 0.02f * l, 0.30f * l, 0.07f * l)   // proa a la quilla
        lineTo(-0.40f * l, 0.07f * l)
        quadraticTo(-0.47f * l, 0.03f * l, -0.48f * l, -0.13f * l)
        close()
    }
    // Sombra en el agua
    drawOval(
        Color(0xFF0B3B44).copy(alpha = 0.18f),
        topLeft = Offset(-0.46f * l, 0.04f * l),
        size = Size(0.92f * l, 0.08f * l),
    )
    drawPath(casco, Principal)
    // Franja turquesa y borde de cubierta
    drawLine(Acento, Offset(-0.46f * l, -0.06f * l), Offset(0.46f * l, -0.08f * l), strokeWidth = 0.025f * l, cap = StrokeCap.Round)
    drawLine(Color.White.copy(alpha = 0.85f), Offset(-0.47f * l, -0.13f * l), Offset(0.40f * l, -0.13f * l), strokeWidth = 0.012f * l)

    // Cabina
    drawRoundRect(
        Superficie,
        topLeft = Offset(-0.20f * l, -0.36f * l),
        size = Size(0.34f * l, 0.23f * l),
        cornerRadius = CornerRadius(0.04f * l),
    )
    drawRoundRect(
        Principal,
        topLeft = Offset(-0.20f * l, -0.36f * l),
        size = Size(0.34f * l, 0.23f * l),
        cornerRadius = CornerRadius(0.04f * l),
        style = Stroke(0.014f * l),
    )
    // Techo
    drawRoundRect(
        Principal,
        topLeft = Offset(-0.23f * l, -0.39f * l),
        size = Size(0.40f * l, 0.045f * l),
        cornerRadius = CornerRadius(0.02f * l),
    )
    // Ventanas con reflejo
    listOf(-0.15f, -0.05f, 0.05f).forEach { vx ->
        drawRoundRect(
            Color(0xFF134E5E),
            topLeft = Offset(vx * l, -0.31f * l),
            size = Size(0.075f * l, 0.08f * l),
            cornerRadius = CornerRadius(0.015f * l),
        )
        drawLine(
            Color.White.copy(alpha = 0.45f),
            Offset((vx + 0.015f) * l, -0.24f * l),
            Offset((vx + 0.06f) * l, -0.30f * l),
            strokeWidth = 0.01f * l,
        )
    }
    // Mástil y banderín que flamea
    drawLine(Principal, Offset(-0.08f * l, -0.39f * l), Offset(-0.08f * l, -0.62f * l), strokeWidth = 0.016f * l, cap = StrokeCap.Round)
    val flameo = sin(t * 9f) * 0.02f * l
    val banderin = Path().apply {
        moveTo(-0.08f * l, -0.62f * l)
        quadraticTo(0.0f * l, -0.61f * l + flameo, 0.07f * l, -0.585f * l + flameo)
        quadraticTo(0.0f * l, -0.56f * l - flameo, -0.08f * l, -0.55f * l)
        close()
    }
    drawPath(banderin, Acento)
}

// ---------- Supervisor ----------

@Composable
private fun EscenaRevision(onTerminada: () -> Unit) {
    val alTerminar by rememberUpdatedState(onTerminada)
    val entrada = remember { Animatable(0f) }
    val checks = remember { List(3) { Animatable(0f) } }
    val sello = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        launch { entrada.animateTo(1f, tween(450, easing = FastOutSlowInEasing)) }
        checks.forEachIndexed { i, c ->
            launch {
                kotlinx.coroutines.delay(300L + i * 260L)
                c.animateTo(1f, tween(380, easing = FastOutSlowInEasing))
            }
        }
        launch {
            kotlinx.coroutines.delay(1150)
            sello.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow))
        }
        kotlinx.coroutines.delay(1800)
        alTerminar()
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(SuperficieTenue, Superficie))),
    ) {
        Canvas(
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    alpha = entrada.value
                    translationY = (1f - entrada.value) * 24.dp.toPx()
                },
        ) {
            // Ficha de revisión
            val fichaAncho = size.width * 0.50f
            val fichaAlto = size.height * 0.62f
            val origen = Offset(size.width * 0.44f - fichaAncho / 2, size.height * 0.30f)
            drawRoundRect(
                Color(0xFF0F172A).copy(alpha = 0.07f),
                topLeft = origen + Offset(0f, 5.dp.toPx()),
                size = Size(fichaAncho, fichaAlto),
                cornerRadius = CornerRadius(14.dp.toPx()),
            )
            drawRoundRect(Superficie, topLeft = origen, size = Size(fichaAncho, fichaAlto), cornerRadius = CornerRadius(14.dp.toPx()))
            drawRoundRect(BordeTarjeta, topLeft = origen, size = Size(fichaAncho, fichaAlto), cornerRadius = CornerRadius(14.dp.toPx()), style = Stroke(1.dp.toPx()))

            val margen = 16.dp.toPx()
            val filaAlto = (fichaAlto - margen * 2) / 3
            checks.forEachIndexed { i, c ->
                val cy = origen.y + margen + filaAlto * (i + 0.5f)
                val cx = origen.x + margen + 9.dp.toPx()
                val r = 9.dp.toPx()
                // Círculo: se llena de turquesa al validarse
                drawCircle(Linea, radius = r, center = Offset(cx, cy))
                drawCircle(Acento, radius = r * c.value.coerceIn(0f, 1f), center = Offset(cx, cy))
                // Check trazado progresivamente
                if (c.value > 0f) {
                    val check = Path().apply {
                        moveTo(cx - r * 0.45f, cy + r * 0.02f)
                        lineTo(cx - r * 0.1f, cy + r * 0.38f)
                        lineTo(cx + r * 0.5f, cy - r * 0.35f)
                    }
                    val medida = PathMeasure().apply { setPath(check, false) }
                    val tramo = Path()
                    medida.getSegment(0f, medida.length * c.value, tramo, true)
                    drawPath(tramo, Color.White, style = Stroke(2.2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
                }
                // Líneas de texto de la fila
                val xTexto = cx + r + 10.dp.toPx()
                val anchoTexto = origen.x + fichaAncho - margen - xTexto
                drawRoundRect(
                    if (c.value >= 1f) Texto.copy(alpha = 0.75f) else Linea,
                    topLeft = Offset(xTexto, cy - 5.dp.toPx()),
                    size = Size(anchoTexto * 0.85f, 4.dp.toPx()),
                    cornerRadius = CornerRadius(2.dp.toPx()),
                )
                drawRoundRect(
                    Linea,
                    topLeft = Offset(xTexto, cy + 2.dp.toPx()),
                    size = Size(anchoTexto * 0.55f, 4.dp.toPx()),
                    cornerRadius = CornerRadius(2.dp.toPx()),
                )
            }
        }

        // Sello final "Revisión lista"
        Box(
            Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 22.dp, bottom = 26.dp)
                .graphicsLayer {
                    val s = sello.value
                    scaleX = 0.6f + 0.4f * s
                    scaleY = 0.6f + 0.4f * s
                    alpha = s.coerceIn(0f, 1f)
                    rotationZ = -8f
                }
                .clip(RoundedCornerShape(10.dp))
                .background(AcentoSuave)
                .border(2.dp, Acento, RoundedCornerShape(10.dp))
                .padding(horizontal = 12.dp, vertical = 6.dp),
        ) {
            Text("REVISIÓN LISTA", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = AcentoTexto)
        }
    }
}

@Preview(showBackground = true, widthDp = 380)
@Composable
private fun AnimacionOperarioPreview() {
    MusselAppTheme { AnimacionPerfil(rol = Rol.OPERARIO, onTerminada = {}) }
}

@Preview(showBackground = true, widthDp = 380)
@Composable
private fun AnimacionSupervisorPreview() {
    MusselAppTheme { AnimacionPerfil(rol = Rol.SUPERVISOR, onTerminada = {}) }
}
