package cl.aldemar.musselapp.ui.componentes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cl.aldemar.musselapp.data.model.EstadoMuestra
import cl.aldemar.musselapp.data.nube.AvisoRevision
import kotlinx.coroutines.delay

/** Aviso flotante: el supervisor ya revisó una o más muestras del operario. */
@Composable
fun BannerRevision(
    avisos: List<AvisoRevision>,
    alCerrar: () -> Unit,
    alVerHistorial: () -> Unit,
) {
    // Se cierra solo si el operario no lo toca; sigue disponible en el Historial
    LaunchedEffect(avisos) {
        delay(12_000)
        alCerrar()
    }
    val unico = avisos.singleOrNull()
    val fondo = when {
        unico?.estado == EstadoMuestra.VALIDADO -> Color(0xFF166534)
        unico?.estado == EstadoMuestra.CORREGIDO -> Color(0xFFB45309)
        else -> Color(0xFF1E3A5F)
    }
    val titulo = when {
        unico == null -> "El supervisor revisó ${avisos.size} muestras"
        unico.estado == EstadoMuestra.VALIDADO -> "${unico.codigo} validada"
        else -> "${unico.codigo} necesita corrección"
    }
    val detalle = when {
        unico == null -> "Revisa el Historial para ver el dictamen de cada una."
        !unico.comentario.isNullOrBlank() -> "Supervisor: ${unico.comentario}"
        else -> "El supervisor ya la revisó. Míralo en el Historial."
    }
    Box(Modifier.fillMaxWidth().statusBarsPadding().padding(12.dp)) {
        Surface(
            color = fondo,
            shape = RoundedCornerShape(14.dp),
            shadowElevation = 8.dp,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(start = 16.dp, top = 12.dp, end = 8.dp, bottom = 4.dp)) {
                Text(titulo, color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                Text(detalle, color = Color.White, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 2.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = alCerrar) { Text("Cerrar", color = Color.White) }
                    TextButton(onClick = alVerHistorial) { Text("Ver historial", color = Color.White, fontWeight = FontWeight.Bold) }
                }
            }
        }
    }
}
