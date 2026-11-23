package cl.aldemar.musselapp.ui.componentes

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WbCloudy
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cl.aldemar.musselapp.data.model.Alerta
import cl.aldemar.musselapp.data.model.EstadoMuestra
import cl.aldemar.musselapp.data.model.Muestra
import cl.aldemar.musselapp.ui.theme.Acento
import cl.aldemar.musselapp.ui.theme.AlertaFondo
import cl.aldemar.musselapp.ui.theme.AlertaTexto
import cl.aldemar.musselapp.ui.theme.BordeDeTarjeta
import cl.aldemar.musselapp.ui.theme.ElevacionTarjeta
import cl.aldemar.musselapp.ui.theme.FormaCampo
import cl.aldemar.musselapp.ui.theme.FormaTarjeta
import cl.aldemar.musselapp.ui.theme.colores
import cl.aldemar.musselapp.util.AlertasMuestra
import cl.aldemar.musselapp.util.CodigoTiempo
import cl.aldemar.musselapp.util.FichaMuestra

/** Colores de una alerta: rojo si es crítica, ámbar (el de Pendiente) si es un aviso. */
@Composable
private fun coloresAlerta(alerta: Alerta): Pair<Color, Color> =
    if (alerta.critica) AlertaFondo to AlertaTexto
    else EstadoMuestra.PENDIENTE.colores.let { it.fondo to it.texto }

/**
 * Alertas automáticas de la muestra (fuera del área, densidad baja, mal tiempo…).
 * No se dibuja si la muestra no tiene ninguna.
 */
@Composable
fun TarjetaAlertas(muestra: Muestra, modifier: Modifier = Modifier) {
    val alertas = remember(muestra) { AlertasMuestra.evaluar(muestra) }
    if (alertas.isEmpty()) return
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = FormaTarjeta,
        color = Color.White,
        border = BordeDeTarjeta,
        shadowElevation = ElevacionTarjeta,
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Warning, contentDescription = null, tint = AlertaTexto, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(6.dp))
                Text("Alertas (${alertas.size})", style = MaterialTheme.typography.titleMedium)
            }
            alertas.forEach { alerta ->
                val (fondo, texto) = coloresAlerta(alerta)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(FormaCampo)
                        .background(fondo)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        alerta.etiqueta,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = texto,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        detalleAlerta(alerta, muestra),
                        style = MaterialTheme.typography.bodySmall,
                        color = texto,
                        textAlign = TextAlign.End,
                    )
                }
            }
        }
    }
}

/** Dato que explica la alerta, por ejemplo la distancia o la racha. */
private fun detalleAlerta(alerta: Alerta, m: Muestra): String = when (alerta) {
    Alerta.FUERA_DE_AREA -> FichaMuestra.distanciaAlCentro(m).substringBefore(" (")
    Alerta.DENSIDAD_BAJA, Alerta.DESVIO_DENSIDAD -> "${m.conteoIndM} ind/m"
    Alerta.SIN_UBICACION -> "No se registró"
    Alerta.UBICACION_IMPRECISA -> "±${m.posicion?.precisionM?.toInt() ?: 0} m"
    Alerta.CHOLGAS_ALTAS -> "${m.cholgasIndM ?: 0} ind/m"
    Alerta.TALLA_FUERA_DE_RANGO -> "${m.tallaMm} mm"
    Alerta.MAL_TIEMPO -> "Racha ${m.clima?.rachaKn?.toInt() ?: 0} kn"
}

/** Dónde se tomó la muestra y con qué tiempo y mar. */
@Composable
fun TarjetaCondiciones(muestra: Muestra, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = FormaTarjeta,
        color = Color.White,
        border = BordeDeTarjeta,
        shadowElevation = ElevacionTarjeta,
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.WbCloudy, contentDescription = null, tint = Acento, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(6.dp))
                Text("Condiciones al muestrear", style = MaterialTheme.typography.titleMedium)
            }
            FilaCondicion("Ubicación", CodigoTiempo.posicion(muestra.posicion))
            FilaCondicion("Distancia al centro", FichaMuestra.distanciaAlCentro(muestra))
            FilaCondicion("Tiempo", CodigoTiempo.tiempo(muestra.clima))
            FilaCondicion("Mar", CodigoTiempo.mar(muestra.clima))
            if (muestra.clima != null) {
                Text(
                    "Fuente: ${CodigoTiempo.fuente(muestra.clima)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
        }
    }
}

@Composable
private fun FilaCondicion(etiqueta: String, valor: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text(etiqueta, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary)
        Spacer(Modifier.width(12.dp))
        Text(
            valor,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f),
        )
    }
}

/** Punto con la cantidad de alertas, para las listas. Rojo si alguna es crítica. */
@Composable
fun InsigniaAlertas(muestra: Muestra, modifier: Modifier = Modifier) {
    val alertas = remember(muestra) { AlertasMuestra.evaluar(muestra) }
    if (alertas.isEmpty()) return
    val (fondo, texto) = coloresAlerta(alertas.first())
    val descripcion = "${alertas.size} " + if (alertas.size == 1) "alerta" else "alertas"
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(fondo)
            .padding(horizontal = 6.dp, vertical = 1.dp)
            .semantics { contentDescription = descripcion },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Icon(Icons.Filled.Warning, contentDescription = null, tint = texto, modifier = Modifier.size(12.dp))
        Text("${alertas.size}", style = MaterialTheme.typography.labelSmall, color = texto, fontWeight = FontWeight.Bold)
    }
}
