package cl.aldemar.musselapp.ui.muestra

import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import cl.aldemar.musselapp.data.ubicacion.UbicadorGps
import cl.aldemar.musselapp.data.model.EstadoMuestra
import cl.aldemar.musselapp.ui.theme.colores
import android.widget.Toast
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.ui.semantics.Role
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Anchor
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.DirectionsBoat
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import cl.aldemar.musselapp.ui.theme.FormaCampo
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import cl.aldemar.musselapp.data.model.CentroCultivo
import cl.aldemar.musselapp.ui.theme.MusselAppTheme

/** Datos del paso 1 del formulario, que se pasan a las siguientes pantallas. */
data class ParametrosMuestra(
    val centroId: String,
    val tren: String,
    val linea: Int,
    val profundidadM: Int,
)

@Composable
fun MuestraScreen(
    // Centro con el que el operario inició sesión: queda elegido de forma automática
    centroInicial: CentroCultivo? = null,
    onVolver: () -> Unit = {},
    onContinuar: (ParametrosMuestra) -> Unit = {},
    // Reconteo: ubicación de la muestra original y su código para el rótulo
    reconteo: ParametrosMuestra? = null,
    codigoReconteo: String? = null,
    vm: MuestraViewModel = viewModel()
) {
    // Observamos el estado del ViewModel
    val state by vm.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Deja elegido el centro del login (no pisa un centro que el operario ya cambió)
    LaunchedEffect(centroInicial) {
        vm.iniciar(centroInicial)
    }
    LaunchedEffect(reconteo) {
        val r = reconteo ?: return@LaunchedEffect
        val centro = CentroCultivo.disponibles.find { it.id == r.centroId } ?: return@LaunchedEffect
        vm.prefijar(centro, r.tren, r.linea, r.profundidadM)
    }

    // Permiso de ubicación: se pide una vez al entrar. Si se niega, la muestra se guarda igual
    var ubicacionPermitida by remember { mutableStateOf(tienePermisoUbicacion(context)) }
    val pedirUbicacion = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { respuesta -> ubicacionPermitida = respuesta.values.any { it } }
    LaunchedEffect(Unit) {
        if (!ubicacionPermitida) pedirUbicacion.launch(UbicadorGps.PERMISOS)
    }

    // Cuando el ViewModel confirma que todo es válido, se muestra el mensaje y se avanza
    LaunchedEffect(state.parametrosValidos) {
        val parametros = state.parametrosValidos ?: return@LaunchedEffect
        // Sin Toast: avanzar a la Captura ya confirma que los parámetros son válidos
        vm.navegacionRealizada()
        onContinuar(parametros)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Paleta.Fondo)
    ) {
        BarraFlujo(titulo = "MusselApp", rotulo = "Nueva muestra", paso = 1, onVolver = onVolver)

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            EncabezadoPasos()

            if (codigoReconteo != null) {
                AvisoFormulario(
                    icono = Icons.Filled.Refresh,
                    texto = "Reconteo de la $codigoReconteo: se copió su ubicación. Revísala y continúa.",
                    color = Paleta.Acento
                )
            }

            if (!ubicacionPermitida) {
                AvisoFormulario(
                    icono = Icons.Filled.LocationOn,
                    texto = "Sin permiso de ubicación: la muestra se guardará sin posición y el supervisor verá un aviso.",
                    color = EstadoMuestra.PENDIENTE.colores.texto
                )
            }

            Tarjeta(espacio = 10.dp) {
                TituloTarjeta("Ubicación de muestreo", Icons.Filled.LocationOn)
                Spacer(Modifier.height(2.dp))
                Selector(
                    etiqueta = "Centro de cultivo",
                    valor = state.centro?.nombre,
                    vacio = "Selecciona un centro",
                    icono = Icons.Filled.Business,
                    opciones = CentroCultivo.disponibles.map { it.nombre },
                    error = state.errorCentro,
                    onElegir = { i -> vm.cambiarCentro(CentroCultivo.disponibles[i]) }
                )
                // Principal e Historial muestran el centro de la sesión: se avisa antes de guardar en otro
                val centroElegido = state.centro
                if (centroInicial != null && centroElegido != null && centroElegido.id != centroInicial.id) {
                    AvisoFormulario(
                        icono = Icons.Filled.Info,
                        texto = "Se guardará en ${centroElegido.nombre}. No aparecerá en el resumen ni en el " +
                            "historial de ${centroInicial.nombre}, el centro de tu sesión.",
                        color = Paleta.Error
                    )
                }
                Selector(
                    etiqueta = "Tren de cultivo (longline)",
                    valor = state.tren?.let { nombreTren(it) },
                    vacio = "Selecciona un tren",
                    icono = Icons.Filled.DirectionsBoat,
                    opciones = Trenes.map { it.second },
                    error = state.errorTren,
                    onElegir = { i -> vm.cambiarTren(Trenes[i].first) }
                )
                Selector(
                    etiqueta = "Línea de siembra",
                    valor = state.linea?.let { etiquetaLinea(it) },
                    vacio = "Selecciona una línea",
                    icono = Icons.Filled.Anchor,
                    opciones = Lineas.map { etiquetaLinea(it) },
                    error = state.errorLinea,
                    onElegir = { i -> vm.cambiarLinea(Lineas[i]) }
                )
            }

            Tarjeta {
                TituloTarjeta("Estrato de profundidad", Icons.Filled.Straighten) {
                    TextoRotulo(
                        texto = "Cuerda 6m",
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Paleta.Campo)
                            .border(1.dp, Paleta.Linea, RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                Row(
                    modifier = Modifier.selectableGroup(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Estrato.entries.forEach { e ->
                        OpcionEstrato(
                            estrato = e,
                            seleccionado = state.profundidadM == e.profundidadM,
                            modifier = Modifier.weight(1f),
                            onClick = { vm.cambiarEstrato(e.profundidadM) }
                        )
                    }
                }
                state.errorEstrato?.let { TextoError(it) }
            }
        }

        // Botón de acción principal, siempre visible abajo
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Paleta.Pie)
                .border(width = 1.dp, color = Paleta.Linea)
                .navigationBarsPadding()
                .padding(16.dp)
        ) {
            // El botón siempre se puede presionar: el ViewModel decide si hay errores
            BotonPrincipal(
                texto = "Continuar a captura",
                iconoFin = Icons.AutoMirrored.Filled.ArrowForward,
                onClick = vm::guardar
            )
            // Separación para que los dos botones no se lean como uno solo ni se toquen por error
            Spacer(modifier = Modifier.height(12.dp))
            BotonSecundario(texto = "Limpiar formulario", icono = Icons.Filled.CleaningServices, onClick = vm::limpiar)
        }
    }
}

private fun tienePermisoUbicacion(context: Context): Boolean = UbicadorGps.PERMISOS.any {
    ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
}

/** Aviso breve dentro del formulario (reconteo, centro distinto al de la sesión, ubicación). */
@Composable
internal fun AvisoFormulario(icono: ImageVector, texto: String, color: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(FormaCampo)
            .background(color.copy(alpha = 0.10f))
            .border(1.dp, color.copy(alpha = 0.35f), FormaCampo)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(imageVector = icono, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
        Text(text = texto, color = Paleta.Texto, style = MaterialTheme.typography.bodySmall)
    }
}

/** Título de la pantalla (nivel 2) y avance: paso actual en oscuro, pendientes con contorno. */
@Composable
private fun EncabezadoPasos() {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "Nueva muestra de choritos",
            color = Paleta.Texto,
            style = MaterialTheme.typography.headlineSmall
        )
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("1. Parámetros", "2. Foto", "3. Validación").forEachIndexed { i, texto ->
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(5.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .then(
                                if (i == 0) Modifier.background(Paleta.Primario)
                                else Modifier.border(1.dp, Paleta.BordeCampo, RoundedCornerShape(3.dp))
                            )
                    )
                    Text(
                        text = texto,
                        color = if (i == 0) Paleta.Texto else Paleta.TextoSuave,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (i == 0) FontWeight.SemiBold else FontWeight.Normal,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MuestraScreenPreview() {
    MusselAppTheme {
        MuestraScreen(centroInicial = CentroCultivo.disponibles.first())
    }
}
