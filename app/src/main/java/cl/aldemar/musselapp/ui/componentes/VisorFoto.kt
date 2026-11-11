package cl.aldemar.musselapp.ui.componentes

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.RotateLeft
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import cl.aldemar.musselapp.util.Fotos
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val FondoBoton = Color(0xFF0F172A)

/** Lo que muestra el visor mientras lee (o gira) la foto. */
private sealed interface CargaVisor {
    data object Cargando : CargaVisor
    data class Lista(val bitmap: Bitmap) : CargaVisor
    data object Error : CargaVisor
}

/**
 * Foto de una muestra a pantalla completa: se acerca con dos dedos (doble toque restablece)
 * y se puede girar. La lectura y el giro corren fuera del hilo principal.
 * La usan Vista previa, Detalle y Revisión para inspeccionar la evidencia igual en todas.
 */
@Composable
fun VisorFoto(ruta: String, onCerrar: () -> Unit, titulo: String? = null) {
    var grados by remember { mutableIntStateOf(0) }
    var escala by remember { mutableFloatStateOf(1f) }
    var desplazamiento by remember { mutableStateOf(Offset.Zero) }

    val original by produceState<CargaVisor>(CargaVisor.Cargando, ruta) {
        value = withContext(Dispatchers.IO) { Fotos.decodificar(ruta, ladoMaxPx = 2048) }
            ?.let { CargaVisor.Lista(it) } ?: CargaVisor.Error
    }
    // La foto se gira de verdad (no solo se estira), para que siempre se vea completa
    val carga by produceState<CargaVisor>(CargaVisor.Cargando, original, grados) {
        value = when (val o = original) {
            is CargaVisor.Lista -> CargaVisor.Lista(
                withContext(Dispatchers.Default) { Fotos.girar(o.bitmap, grados.toFloat()) },
            )
            else -> o
        }
    }

    fun girar(delta: Int) {
        grados = (grados + delta + 360) % 360
        escala = 1f
        desplazamiento = Offset.Zero
    }

    Dialog(
        onDismissRequest = onCerrar,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
        ) {
            when (val actual = carga) {
                is CargaVisor.Lista -> Image(
                    bitmap = actual.bitmap.asImageBitmap(),
                    contentDescription = "Fotografía de la muestra ampliada. Pellizca para acercar",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTapGestures(onDoubleTap = {
                                escala = if (escala > 1f) 1f else 2.5f
                                desplazamiento = Offset.Zero
                            })
                        }
                        .pointerInput(Unit) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                escala = (escala * zoom).coerceIn(1f, 5f)
                                desplazamiento = if (escala == 1f) Offset.Zero else desplazamiento + pan
                            }
                        }
                        .graphicsLayer {
                            scaleX = escala
                            scaleY = escala
                            translationX = desplazamiento.x
                            translationY = desplazamiento.y
                        },
                )
                CargaVisor.Cargando -> CircularProgressIndicator(
                    color = Color.White,
                    modifier = Modifier.align(Alignment.Center),
                )
                CargaVisor.Error -> Text(
                    text = "No se pudo abrir la fotografía",
                    color = Color.White,
                    modifier = Modifier.align(Alignment.Center),
                )
            }

            BotonVisor(
                icono = Icons.Filled.Close,
                descripcion = "Cerrar",
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .statusBarsPadding()
                    .padding(12.dp),
                onClick = onCerrar,
            )

            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                BotonVisor(Icons.AutoMirrored.Filled.RotateLeft, "Girar a la izquierda") { girar(-90) }
                BotonVisor(Icons.AutoMirrored.Filled.RotateRight, "Girar a la derecha") { girar(90) }
            }

            if (titulo != null) {
                Text(
                    text = titulo,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(16.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(FondoBoton.copy(alpha = 0.75f))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                )
            }
        }
    }
}

/** Botón redondo de 48 dp para usar sobre la foto ampliada. */
@Composable
private fun BotonVisor(
    icono: ImageVector,
    descripcion: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(FondoBoton.copy(alpha = 0.85f))
            .clickable(onClickLabel = descripcion, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(imageVector = icono, contentDescription = descripcion, tint = Color.White)
    }
}
