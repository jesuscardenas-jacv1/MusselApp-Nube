package cl.aldemar.musselapp.ui.muestra

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import cl.aldemar.musselapp.util.Fotos
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Color de las marcas: ámbar con borde blanco, visible sobre choritos negros y fondo claro. */
private val ColorMarca = Color(0xFFF59E0B)

/** Lo que muestra el conteo mientras lee la foto. */
private sealed interface CargaConteo {
    data object Cargando : CargaConteo
    data class Lista(val imagen: ImageBitmap) : CargaConteo
    data object Error : CargaConteo
}

/**
 * Conteo asistido a pantalla completa (B6): el operario marca cada individuo sobre la foto y la
 * app calcula los individuos por metro con el tramo muestreado. [conteo] lo guarda la Vista previa,
 * así las marcas siguen ahí si se cierra y se vuelve a abrir.
 */
@Composable
fun ConteoSobreFoto(
    ruta: String,
    tramoCm: Int?,
    conteo: ConteoMarcas,
    onCambiar: (ConteoMarcas) -> Unit,
    onUsar: (Int) -> Unit,
    onCerrar: () -> Unit,
) {
    val carga by produceState<CargaConteo>(CargaConteo.Cargando, ruta) {
        value = withContext(Dispatchers.IO) { Fotos.decodificar(ruta, ladoMaxPx = 2048) }
            ?.let { CargaConteo.Lista(it.asImageBitmap()) } ?: CargaConteo.Error
    }
    Dialog(onDismissRequest = onCerrar, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        when (val actual = carga) {
            is CargaConteo.Lista -> ConteoSobreImagen(actual.imagen, tramoCm, conteo, onCambiar, onUsar, onCerrar)
            CargaConteo.Cargando -> Box(Modifier.fillMaxSize().background(Color.Black)) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.align(Alignment.Center))
            }
            CargaConteo.Error -> Box(Modifier.fillMaxSize().background(Color.Black)) {
                Text("No se pudo abrir la fotografía", color = Color.White, modifier = Modifier.align(Alignment.Center))
            }
        }
    }
}

/** Contenido del conteo, separado del diálogo para poder probarlo con una imagen cualquiera. */
@Composable
internal fun ConteoSobreImagen(
    imagen: ImageBitmap,
    tramoCm: Int?,
    conteo: ConteoMarcas,
    onCambiar: (ConteoMarcas) -> Unit,
    onUsar: (Int) -> Unit,
    onCerrar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var escala by remember { mutableFloatStateOf(1f) }
    var desplazamiento by remember { mutableStateOf(Offset.Zero) }
    // Los gestos se registran una vez: leen siempre el conteo y la acción más recientes
    val conteoActual by rememberUpdatedState(conteo)
    val onCambiarActual by rememberUpdatedState(onCambiar)
    val densidad = LocalDensity.current
    val proporcion = imagen.width.toFloat() / imagen.height
    val indPorMetro = ConteoMarcas.indPorMetro(conteo.total, tramoCm)
    val valido = indPorMetro != null && indPorMetro in 1..CONTEO_MAXIMO_IND_M

    Column(modifier.fillMaxSize().background(Color.Black)) {
        Row(
            modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onCerrar) {
                Icon(Icons.Filled.Close, contentDescription = "Cerrar el conteo", tint = Color.White)
            }
            Text("Conteo sobre la foto", color = Color.White, style = MaterialTheme.typography.titleMedium)
        }
        Text(
            text = "Toca cada chorito para marcarlo. Toca una marca para quitarla. Pellizca para acercar.",
            color = Paleta.TextoSobreOscuro,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        )

        // Zona de la foto: dos dedos acercan y mueven; un toque marca o desmarca
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clipToBounds()
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        escala = (escala * zoom).coerceIn(1f, 6f)
                        desplazamiento = if (escala == 1f) Offset.Zero else desplazamiento + pan
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .aspectRatio(proporcion)
                    .graphicsLayer {
                        scaleX = escala
                        scaleY = escala
                        translationX = desplazamiento.x
                        translationY = desplazamiento.y
                    }
                    .testTag(ETIQUETA_LIENZO)
                    // Después de graphicsLayer: el toque llega en coordenadas de la imagen sin zoom
                    .pointerInput(Unit) {
                        detectTapGestures { posicion ->
                            // El radio de toque se mantiene igual en la pantalla aunque se acerque la foto
                            val radioPx = with(densidad) { RADIO_TOQUE.toPx() } / escala
                            onCambiarActual(
                                conteoActual.tocar(
                                    Marca(posicion.x / size.width, posicion.y / size.height),
                                    radio = radioPx / size.height,
                                    proporcion = proporcion,
                                ),
                            )
                        }
                    },
            ) {
                Image(
                    bitmap = imagen,
                    contentDescription = "Fotografía de la muestra para contar los individuos",
                    contentScale = ContentScale.FillBounds,
                    modifier = Modifier.fillMaxSize(),
                )
                Canvas(Modifier.fillMaxSize()) {
                    val radio = RADIO_MARCA.toPx() / escala
                    val borde = Stroke(width = 2.dp.toPx() / escala)
                    conteo.marcas.forEach { m ->
                        val centro = Offset(m.x * size.width, m.y * size.height)
                        drawCircle(ColorMarca, radio, centro)
                        drawCircle(Color.White, radio, centro, style = borde)
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Paleta.Superficie)
                .navigationBarsPadding()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("Marcas: ${conteo.total}", color = Paleta.Texto, style = MaterialTheme.typography.titleMedium)
            Text(
                text = when {
                    tramoCm == null -> "Ingresa el tramo (cm) en la Vista previa para convertir las marcas a ind/m"
                    indPorMetro == null -> "Tramo de $tramoCm cm: marca los individuos para calcular ind/m"
                    else -> "Tramo de $tramoCm cm → $indPorMetro ind/m"
                },
                color = Paleta.TextoSuave,
                style = MaterialTheme.typography.bodyMedium,
            )
            if (indPorMetro != null && !valido) {
                Text(
                    "El conteo debe estar entre 1 y $CONTEO_MAXIMO_IND_M ind/m",
                    color = Paleta.Error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BotonSecundario("Deshacer", Icons.AutoMirrored.Filled.Undo, Modifier.weight(1f)) {
                    if (conteo.puedeDeshacer) onCambiar(conteo.deshacer())
                }
                BotonSecundario("Limpiar", Icons.Filled.DeleteSweep, Modifier.weight(1f)) {
                    onCambiar(conteo.limpiar())
                }
            }
            BotonPrincipal(
                texto = if (indPorMetro != null) "Usar $indPorMetro ind/m" else "Usar el conteo",
                habilitado = valido,
                onClick = { indPorMetro?.let(onUsar) },
            )
        }
    }
}

/** Etiqueta del área de la foto, para las pruebas de interfaz. */
internal const val ETIQUETA_LIENZO = "lienzo-conteo"
private val RADIO_MARCA = 9.dp
private val RADIO_TOQUE = 18.dp
