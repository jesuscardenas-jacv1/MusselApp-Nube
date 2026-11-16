package cl.aldemar.musselapp.ui.historial

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.draw.rotate
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FilterAltOff
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.foundation.border
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import cl.aldemar.musselapp.ui.theme.Acento
import cl.aldemar.musselapp.ui.theme.AcentoSuave
import cl.aldemar.musselapp.ui.theme.AcentoTexto
import cl.aldemar.musselapp.ui.theme.BordeCampo
import cl.aldemar.musselapp.ui.theme.BordeDeCampo
import cl.aldemar.musselapp.ui.theme.BordeDeTarjeta
import cl.aldemar.musselapp.ui.theme.BordeTarjeta
import cl.aldemar.musselapp.ui.theme.ElevacionTarjeta
import cl.aldemar.musselapp.ui.theme.FormaCampo
import cl.aldemar.musselapp.ui.theme.FormaTarjetaChica
import cl.aldemar.musselapp.ui.theme.Linea
import cl.aldemar.musselapp.ui.theme.Rotulo
import cl.aldemar.musselapp.ui.theme.SuperficieCampo
import cl.aldemar.musselapp.ui.theme.tarjetaMussel
import cl.aldemar.musselapp.data.model.CentroCultivo
import cl.aldemar.musselapp.data.model.EstadoMuestra
import cl.aldemar.musselapp.data.model.Muestra
import cl.aldemar.musselapp.data.model.Rol
import cl.aldemar.musselapp.data.model.Usuario
import cl.aldemar.musselapp.data.repository.FakeMuestraRepository
import cl.aldemar.musselapp.ui.componentes.BarraSuperiorMussel
import cl.aldemar.musselapp.ui.componentes.ChipEstado
import cl.aldemar.musselapp.ui.componentes.NavegacionInferior
import cl.aldemar.musselapp.ui.principal.Destino
import cl.aldemar.musselapp.ui.theme.AcentoValidado
import cl.aldemar.musselapp.ui.theme.AlertaFondo
import cl.aldemar.musselapp.ui.theme.AlertaTexto
import cl.aldemar.musselapp.ui.theme.EstadoValidadoFondo
import cl.aldemar.musselapp.ui.theme.EstadoValidadoTexto
import cl.aldemar.musselapp.ui.theme.colores
import cl.aldemar.musselapp.ui.theme.MusselAppTheme
import cl.aldemar.musselapp.util.Fechas
import java.util.Locale

/** Punto de entrada con estado: conecta el ViewModel con la UI. */
@Composable
fun HistorialRoute(
    usuario: Usuario,
    centro: CentroCultivo,
    onNavegar: (Destino) -> Unit,
    onVerMuestra: (Long) -> Unit,
    onRecontar: (Long) -> Unit,
    onCerrarSesion: () -> Unit,
    viewModel: HistorialViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    // El Historial muestra el centro con el que se inició sesión
    LaunchedEffect(centro.id) { viewModel.mostrarCentro(centro.id) }
    HistorialScreen(
        usuario = usuario,
        centro = centro,
        uiState = uiState,
        onBusquedaChange = viewModel::onBusquedaChange,
        onFiltroChange = viewModel::onFiltroChange,
        onPeriodoChange = viewModel::onPeriodoChange,
        onAlternarOrden = viewModel::onAlternarOrden,
        onRestablecerFiltros = viewModel::onRestablecerFiltros,
        onSincronizar = viewModel::sincronizar,
        onMensajeMostrado = viewModel::onMensajeMostrado,
        onNavegar = onNavegar,
        onVerMuestra = onVerMuestra,
        onRecontar = onRecontar,
        onCerrarSesion = onCerrarSesion,
    )
}

/** UI sin estado del Historial (diseño "6. Historial" de Stitch). */
@Composable
fun HistorialScreen(
    usuario: Usuario,
    centro: CentroCultivo,
    uiState: HistorialUiState,
    onBusquedaChange: (String) -> Unit,
    onFiltroChange: (FiltroEstado) -> Unit,
    onPeriodoChange: (Periodo) -> Unit,
    onAlternarOrden: () -> Unit,
    onRestablecerFiltros: () -> Unit,
    onSincronizar: () -> Unit,
    onMensajeMostrado: () -> Unit,
    onNavegar: (Destino) -> Unit,
    onVerMuestra: (Long) -> Unit,
    onRecontar: (Long) -> Unit,
    onCerrarSesion: () -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    var expandidaId by rememberSaveable { mutableStateOf<Long?>(null) }
    LaunchedEffect(uiState.mensaje) {
        uiState.mensaje?.let {
            snackbarHostState.showSnackbar(it)
            onMensajeMostrado()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            BarraSuperiorMussel(
                seccion = "Historial",
                usuario = usuario,
                centro = centro,
                pendientesSincronizar = uiState.pendientesSincronizar,
                sincronizando = uiState.sincronizando,
                onSincronizar = onSincronizar,
                onCerrarSesion = onCerrarSesion,
                onIrAInicio = { onNavegar(Destino.MUESTRAS) },
            )
        },
        bottomBar = { NavegacionInferior(rol = usuario.rol, seleccionado = Destino.HISTORIAL, onNavegar = onNavegar) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                // Al buscar, los resultados se pueden recorrer por encima del teclado
                .consumeWindowInsets(innerPadding)
                .imePadding(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { BarraBusqueda(texto = uiState.criterios.busqueda, onTextoChange = onBusquedaChange) }
            item {
                SelectorPeriodoYOrden(
                    periodo = uiState.criterios.periodo,
                    orden = uiState.criterios.orden,
                    onPeriodoChange = onPeriodoChange,
                    onAlternarOrden = onAlternarOrden,
                )
            }
            item {
                ChipsEstado(
                    seleccionado = uiState.criterios.filtro,
                    conteos = uiState.conteos,
                    onFiltroChange = onFiltroChange,
                )
            }
            item { AvisoToque() }
            uiState.tallaPromedioMm?.let { promedio ->
                item { ResumenTalla(promedioMm = promedio, cantidad = uiState.muestras.size) }
            }

            if (uiState.muestras.isEmpty()) {
                item {
                    EstadoVacio(
                        criterios = uiState.criterios,
                        puedeRegistrar = usuario.rol == Rol.OPERARIO,
                        onRestablecer = onRestablecerFiltros,
                        onVerTodo = { onPeriodoChange(Periodo.TODO) },
                        onRegistrar = { onNavegar(Destino.NUEVA_MUESTRA) },
                    )
                }
            } else {
                items(uiState.muestras, key = { it.id }) { muestra ->
                    TarjetaMuestra(
                        muestra = muestra,
                        expandida = muestra.id == expandidaId,
                        puedeRecontar = usuario.rol == Rol.OPERARIO,
                        // Solo una abierta a la vez; tocar la abierta la cierra
                        onAlternar = { expandidaId = if (expandidaId == muestra.id) null else muestra.id },
                        onVerDetalle = { onVerMuestra(muestra.id) },
                        onRecontar = { onRecontar(muestra.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun BarraBusqueda(texto: String, onTextoChange: (String) -> Unit) {
    val focusManager = LocalFocusManager.current
    OutlinedTextField(
        value = texto,
        onValueChange = onTextoChange,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp),
        placeholder = { Text("Buscar por línea, tren o fecha…") },
        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
        trailingIcon = {
            if (texto.isNotEmpty()) {
                IconButton(onClick = {
                    onTextoChange("")
                    focusManager.clearFocus()
                }) {
                    Icon(Icons.Filled.Cancel, contentDescription = "Limpiar búsqueda")
                }
            }
        },
        singleLine = true,
        shape = FormaCampo,
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            focusedBorderColor = Acento,
            unfocusedBorderColor = BordeCampo,
            cursorColor = Acento,
            focusedLeadingIconColor = MaterialTheme.colorScheme.secondary,
            unfocusedLeadingIconColor = MaterialTheme.colorScheme.secondary,
            focusedPlaceholderColor = MaterialTheme.colorScheme.secondary,
            unfocusedPlaceholderColor = MaterialTheme.colorScheme.secondary,
        ),
    )
}

@Composable
private fun SelectorPeriodoYOrden(
    periodo: Periodo,
    orden: Orden,
    onPeriodoChange: (Periodo) -> Unit,
    onAlternarOrden: () -> Unit,
) {
    var menuAbierto by remember { mutableStateOf(false) }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.weight(1f)) {
            BotonClaro(
                icono = Icons.Filled.CalendarToday,
                texto = if (periodo == Periodo.HOY) "Hoy, ${Fechas.corta(System.currentTimeMillis())}" else periodo.etiqueta,
                iconoFinal = Icons.Filled.ExpandMore,
                onClick = { menuAbierto = true },
                modifier = Modifier.fillMaxWidth(),
            )
            DropdownMenu(expanded = menuAbierto, onDismissRequest = { menuAbierto = false }) {
                Periodo.entries.forEach { opcion ->
                    DropdownMenuItem(
                        text = { Text(opcion.etiqueta, fontWeight = if (opcion == periodo) FontWeight.SemiBold else null) },
                        onClick = {
                            onPeriodoChange(opcion)
                            menuAbierto = false
                        },
                    )
                }
            }
        }
        BotonClaro(icono = Icons.AutoMirrored.Filled.Sort, texto = orden.etiqueta, onClick = onAlternarOrden)
    }
}

@Composable
private fun BotonClaro(
    icono: ImageVector,
    texto: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconoFinal: ImageVector? = null,
) {
    Surface(
        onClick = onClick,
        shape = FormaCampo,
        color = MaterialTheme.colorScheme.surface,
        border = BordeDeCampo,
        modifier = modifier.heightIn(min = 48.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icono, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                texto,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = if (iconoFinal != null) Modifier.weight(1f) else Modifier,
            )
            if (iconoFinal != null) {
                Icon(iconoFinal, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
            }
        }
    }
}

@Composable
private fun ChipsEstado(
    seleccionado: FiltroEstado,
    conteos: Map<FiltroEstado, Int>,
    onFiltroChange: (FiltroEstado) -> Unit,
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(FiltroEstado.entries) { filtro ->
            val activo = filtro == seleccionado
            val cantidad = conteos[filtro] ?: 0
            Surface(
                onClick = { onFiltroChange(filtro) },
                shape = RoundedCornerShape(50),
                color = if (activo) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                contentColor = if (activo) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                border = if (activo) null else BordeDeCampo,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    acentoDe(filtro)?.let { color ->
                        Box(Modifier.size(9.dp).clip(CircleShape).background(color))
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(filtro.etiqueta, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.width(6.dp))
                    if (activo) {
                        Text(
                            "$cantidad",
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(Color.White.copy(alpha = 0.2f))
                                .padding(horizontal = 6.dp, vertical = 1.dp),
                        )
                    } else {
                        Text("($cantidad)", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
                    }
                }
            }
        }
    }
}

private fun acentoDe(filtro: FiltroEstado): Color? = when (filtro) {
    FiltroEstado.TODOS -> null
    FiltroEstado.VALIDADOS -> EstadoMuestra.VALIDADO.colores.acento
    FiltroEstado.PENDIENTES -> EstadoMuestra.PENDIENTE.colores.acento
    FiltroEstado.CORREGIDOS -> EstadoMuestra.CORREGIDO.colores.acento
}

private fun acentoDe(estado: EstadoMuestra): Color = estado.colores.acento

@Composable
private fun AvisoToque() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(FormaCampo)
            .background(AcentoSuave)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.TouchApp, contentDescription = null, tint = AcentoTexto, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Text(
            "Toca una muestra para ver sus datos; desde ahí puedes abrir el detalle completo",
            style = MaterialTheme.typography.bodySmall,
            color = AcentoTexto,
        )
    }
}

@Composable
private fun ResumenTalla(promedioMm: Double, cantidad: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .tarjetaMussel(FormaTarjetaChica)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.WaterDrop, contentDescription = null, tint = Acento, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(
            "Promedio talla: ${"%.1f".format(Locale("es", "CL"), promedioMm)} mm",
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.weight(1f),
        )
        Text(
            "$cantidad ${if (cantidad == 1) "muestra" else "muestras"}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.secondary,
        )
    }
}

/**
 * Tarjeta de dos líneas (muestra, ubicación y estado; fecha de registro).
 * Al tocarla se expande con las métricas, el dictamen y las acciones.
 */
@Composable
private fun TarjetaMuestra(
    muestra: Muestra,
    expandida: Boolean,
    puedeRecontar: Boolean,
    onAlternar: () -> Unit,
    onVerDetalle: () -> Unit,
    onRecontar: () -> Unit,
) {
    val corregida = muestra.estado == EstadoMuestra.CORREGIDO
    val giroFlecha by animateFloatAsState(if (expandida) 180f else 0f, label = "flecha")
    Surface(
        onClick = onAlternar,
        shape = FormaTarjetaChica,
        color = MaterialTheme.colorScheme.surface,
        border = BordeDeTarjeta,
        shadowElevation = ElevacionTarjeta,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            // Franja lateral con el color del estado
            Box(
                Modifier
                    .width(5.dp)
                    .fillMaxHeight()
                    .background(acentoDe(muestra.estado)),
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .animateContentSize()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Muestra %03d · L%02d Tren %s".format(muestra.id, muestra.linea, muestra.tren),
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(6.dp))
                    ChipEstado(muestra.estado)
                    Icon(
                        Icons.Filled.ExpandMore,
                        contentDescription = if (expandida) "Contraer" else "Expandir",
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.rotate(giroFlecha),
                    )
                }
                Text(
                    // Las métricas se ven al expandir la tarjeta
                    Fechas.relativa(muestra.registradaEn),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                if (expandida) {
                    Column(
                        modifier = Modifier.padding(top = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        MetricasMuestra(muestra)
                        when {
                            muestra.estado == EstadoMuestra.VALIDADO && muestra.revisadaPor != null ->
                                Aviso(Icons.Filled.Verified, "Aprobado por Sup. ${muestra.revisadaPor}", EstadoValidadoFondo, EstadoValidadoTexto)
                            // Mismo ámbar que su chip y su franja; muestra qué corrigió el supervisor
                            corregida ->
                                Aviso(
                                    Icons.Filled.EditNote,
                                    muestra.comentarioSupervisor ?: "Corregida por Sup. ${muestra.revisadaPor ?: "—"}",
                                    muestra.estado.colores.fondo,
                                    muestra.estado.colores.texto,
                                )
                        }
                        EstadoEnvio(muestra)
                        AccionesTarjeta(
                            codigo = muestra.codigo,
                            puedeRecontar = corregida && puedeRecontar,
                            onRecontar = onRecontar,
                            onVerDetalle = onVerDetalle,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricasMuestra(muestra: Muestra) {
    val densidadBaja = muestra.conteoIndM < Muestra.DENSIDAD_MINIMA_IND_M
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(FormaCampo)
            .background(if (densidadBaja) AlertaFondo else SuperficieCampo)
            .border(1.dp, if (densidadBaja) AlertaFondo else Linea, FormaCampo)
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Metrica(
            "Densidad", "${muestra.conteoIndM} ind/m", Modifier.weight(1.1f),
            color = if (densidadBaja) AlertaTexto else null,
        )
        Metrica("Talla media", "${muestra.tallaMm} mm", Modifier.weight(1f))
        Metrica("Profundidad", "${muestra.profundidadM} m", Modifier.weight(1f))
    }
}

@Composable
private fun Metrica(titulo: String, valor: String, modifier: Modifier, color: Color? = null) {
    Column(modifier = modifier) {
        Text(
            titulo.uppercase(),
            style = Rotulo,
            color = color ?: MaterialTheme.colorScheme.secondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            valor,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = color ?: MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
        )
    }
}

@Composable
private fun Aviso(icono: ImageVector, texto: String, fondo: Color, colorTexto: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(FormaCampo)
            .background(fondo)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icono, contentDescription = null, tint = colorTexto, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(texto, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium, color = colorTexto)
    }
}

@Composable
private fun AccionesTarjeta(codigo: String, puedeRecontar: Boolean, onRecontar: () -> Unit, onVerDetalle: () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (puedeRecontar) {
            OutlinedButton(
                onClick = onRecontar,
                shape = FormaCampo,
                border = BordeDeCampo,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                modifier = Modifier.weight(1f),
            ) {
                Icon(Icons.Filled.Replay, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Recontar", style = MaterialTheme.typography.labelLarge, maxLines = 1)
            }
        }
        Button(
            onClick = onVerDetalle,
            shape = FormaCampo,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
            modifier = Modifier.weight(1f),
        ) {
            Text("Ver detalle", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, maxLines = 1)
            Spacer(Modifier.width(6.dp))
            // Decorativo: el texto del botón ya lo anuncia (evita doble lectura en TalkBack)
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
        }
    }
}

/** Quién registró la muestra o si ya se envió. */
@Composable
private fun EstadoEnvio(muestra: Muestra) {
    val (icono, texto) = when {
        muestra.estado == EstadoMuestra.PENDIENTE && muestra.operador.isNotBlank() -> Icons.Filled.Person to "Op. ${muestra.operador}"
        muestra.sincronizada -> Icons.Filled.CloudDone to "Sincronizado"
        else -> Icons.Filled.CloudUpload to "Pendiente de envío"
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icono, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text(
            texto,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.secondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * Estado vacío que explica POR QUÉ no hay resultados y ofrece la salida adecuada:
 * quitar filtros, ampliar el periodo o registrar la primera muestra.
 */
@Composable
private fun EstadoVacio(
    criterios: CriteriosHistorial,
    puedeRegistrar: Boolean,
    onRestablecer: () -> Unit,
    onVerTodo: () -> Unit,
    onRegistrar: () -> Unit,
) {
    val hayFiltros = criterios.busqueda.isNotBlank() || criterios.filtro != FiltroEstado.TODOS
    val (icono, titulo, detalle) = when {
        hayFiltros -> Triple(
            Icons.Filled.FilterAltOff,
            "Sin muestras coincidentes",
            "No hay registros que coincidan con la búsqueda o el estado seleccionado.",
        )
        criterios.periodo != Periodo.TODO -> Triple(
            Icons.Filled.EventBusy,
            if (criterios.periodo == Periodo.HOY) "Sin muestras hoy" else "Sin muestras en los últimos 7 días",
            "Aún no se registran muestras en este periodo. Puedes revisar el historial completo.",
        )
        else -> Triple(
            Icons.Filled.Inventory2,
            "Aún no hay muestras registradas",
            "Cuando se registre la primera muestra de este centro aparecerá aquí.",
        )
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, BordeTarjeta, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icono, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(32.dp))
        }
        Text(titulo, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        Text(
            detalle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.secondary,
            textAlign = TextAlign.Center,
        )
        when {
            hayFiltros -> OutlinedButton(onClick = onRestablecer, shape = RoundedCornerShape(10.dp)) {
                Text("Quitar búsqueda y filtros", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            }
            criterios.periodo != Periodo.TODO -> Button(onClick = onVerTodo, shape = RoundedCornerShape(10.dp)) {
                Text("Ver todo el historial", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            }
            puedeRegistrar -> Button(onClick = onRegistrar, shape = RoundedCornerShape(10.dp)) {
                Text("Registrar muestra", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Preview(showBackground = true, heightDp = 1800)
@Composable
private fun HistorialPreview() {
    val repositorio = remember { FakeMuestraRepository() }
    val todas by repositorio.muestras.collectAsState()
    val resultado = FiltrosHistorial.aplicar(todas, CriteriosHistorial(periodo = Periodo.TODO))
    MusselAppTheme {
        HistorialScreen(
            usuario = Usuario("11.111.111-1", "Carla Vera", Rol.OPERARIO),
            centro = CentroCultivo.disponibles.first(),
            uiState = HistorialUiState(
                criterios = CriteriosHistorial(periodo = Periodo.TODO),
                muestras = resultado.muestras,
                conteos = resultado.conteos,
                tallaPromedioMm = resultado.tallaPromedioMm,
            ),
            onBusquedaChange = {}, onFiltroChange = {}, onPeriodoChange = {}, onAlternarOrden = {},
            onRestablecerFiltros = {}, onSincronizar = {}, onMensajeMostrado = {}, onNavegar = {},
            onVerMuestra = {}, onRecontar = {}, onCerrarSesion = {},
        )
    }
}
