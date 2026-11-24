package cl.aldemar.musselapp.ui.componentes

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Waves
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.ui.graphics.vector.ImageVector
import cl.aldemar.musselapp.ui.theme.AcentoSuave
import cl.aldemar.musselapp.ui.theme.AcentoTexto
import cl.aldemar.musselapp.ui.theme.Linea
import cl.aldemar.musselapp.ui.theme.Rotulo
import cl.aldemar.musselapp.ui.theme.SuperficieCampo
import cl.aldemar.musselapp.util.DatoCondicion
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
                    // Crítica o aviso también se distingue por el ícono, no solo por el color
                    Icon(
                        if (alerta.critica) Icons.Filled.Error else Icons.Filled.WarningAmber,
                        contentDescription = if (alerta.critica) "Alerta crítica" else "Aviso",
                        tint = texto,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(8.dp))
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

/**
 * Dónde se tomó la muestra y con qué tiempo y mar. Cada dato va en su recuadro (número grande y
 * unidad abajo) para leerlo de un vistazo; la ubicación va al final, con la distancia antes que
 * las coordenadas.
 */
@Composable
fun TarjetaCondiciones(muestra: Muestra, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = FormaTarjeta,
        color = Color.White,
        border = BordeDeTarjeta,
        shadowElevation = ElevacionTarjeta,
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.WbCloudy, contentDescription = null, tint = Acento, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(6.dp))
                Text("Condiciones al muestrear", style = MaterialTheme.typography.titleMedium)
            }
            val clima = muestra.clima
            if (clima == null) {
                Text(
                    "Tiempo y mar no registrados",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary,
                )
            } else {
                val cielo = CodigoTiempo.descripcion(clima.codigo)
                GrupoCondicion(
                    titulo = if (clima.codigo != null) "Tiempo · $cielo" else "Tiempo",
                    icono = Icons.Filled.Thermostat,
                    datos = CodigoTiempo.datosTiempo(clima),
                )
                if (CodigoTiempo.hayMar(clima)) {
                    GrupoCondicion(titulo = "Mar", icono = Icons.Filled.Waves, datos = CodigoTiempo.datosMar(clima))
                } else {
                    Text(
                        "Mar: sin datos para este punto",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }
            }
            UbicacionMuestra(muestra)
            if (clima != null) {
                Text(
                    "Fuente: ${CodigoTiempo.fuente(clima)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
        }
    }
}

/** Título con ícono y una fila de recuadros que ocupa todo el ancho. */
@Composable
private fun GrupoCondicion(titulo: String, icono: ImageVector, datos: List<DatoCondicion>) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icono, contentDescription = null, tint = Acento, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(titulo, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            datos.forEach { RecuadroDato(it.etiqueta, it.valor, it.unidad, Modifier.weight(1f)) }
        }
    }
}

/** Distancia al centro con su pastilla (dentro o fuera del área) y, debajo, las coordenadas. */
@Composable
private fun UbicacionMuestra(muestra: Muestra) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Filled.LocationOn, contentDescription = null, tint = Acento, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                FichaMuestra.distanciaCorta(muestra) ?: "Ubicación no registrada",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (muestra.posicion != null) {
                Text(
                    CodigoTiempo.posicion(muestra.posicion),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
        }
        FichaMuestra.dentroDelArea(muestra)?.let { dentro ->
            Spacer(Modifier.width(8.dp))
            Text(
                if (dentro) "Dentro del área" else "Fuera del área",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (dentro) AcentoTexto else AlertaTexto,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(if (dentro) AcentoSuave else AlertaFondo)
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            )
        }
    }
}

/**
 * Dato en un recuadro gris claro: rótulo arriba, número grande y unidad abajo. Lo usan las
 * condiciones al muestrear y el resumen biométrico del Detalle, así todos los datos se leen igual.
 */
@Composable
fun RecuadroDato(etiqueta: String, valor: String, unidad: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = FormaCampo,
        color = SuperficieCampo,
        border = BorderStroke(1.dp, Linea),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(etiqueta.uppercase(), style = Rotulo, color = MaterialTheme.colorScheme.secondary, maxLines = 1)
            Spacer(Modifier.height(4.dp))
            Text(
                valor,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = if (valor == CodigoTiempo.SIN_DATO) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
            )
            Text(
                unidad,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary,
                textAlign = TextAlign.Center,
                maxLines = 2,
            )
        }
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
