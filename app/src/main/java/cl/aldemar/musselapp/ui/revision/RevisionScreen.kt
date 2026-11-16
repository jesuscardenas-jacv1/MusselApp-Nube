package cl.aldemar.musselapp.ui.revision

import cl.aldemar.musselapp.ui.componentes.InsigniaAlertas
import cl.aldemar.musselapp.ui.componentes.TarjetaAlertas
import cl.aldemar.musselapp.ui.componentes.TarjetaCondiciones
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.BorderStroke
import cl.aldemar.musselapp.ui.theme.Acento
import cl.aldemar.musselapp.ui.theme.AcentoSobreOscuro
import cl.aldemar.musselapp.ui.theme.AcentoTexto
import cl.aldemar.musselapp.ui.theme.BordeDeTarjeta
import cl.aldemar.musselapp.ui.theme.ElevacionTarjeta
import cl.aldemar.musselapp.ui.theme.FormaBoton
import cl.aldemar.musselapp.ui.theme.FormaCampo
import cl.aldemar.musselapp.ui.theme.FormaTarjeta
import cl.aldemar.musselapp.ui.theme.Linea
import cl.aldemar.musselapp.ui.theme.Principal
import cl.aldemar.musselapp.ui.theme.Rotulo
import cl.aldemar.musselapp.ui.theme.SuperficieCampo
import cl.aldemar.musselapp.ui.theme.TextoSobreOscuro
import cl.aldemar.musselapp.ui.theme.tarjetaMussel
import cl.aldemar.musselapp.ui.componentes.VisorFoto
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import cl.aldemar.musselapp.R
import cl.aldemar.musselapp.data.model.CentroCultivo
import cl.aldemar.musselapp.data.model.EstadoMuestra
import cl.aldemar.musselapp.data.model.Muestra
import cl.aldemar.musselapp.data.model.Usuario
import cl.aldemar.musselapp.ui.componentes.MenuPerfil
import cl.aldemar.musselapp.ui.componentes.ChipEstado
import cl.aldemar.musselapp.ui.componentes.FotoMuestra
import cl.aldemar.musselapp.ui.componentes.NavegacionInferior
import cl.aldemar.musselapp.ui.principal.Destino
import cl.aldemar.musselapp.ui.theme.EstadoValidadoFondo
import cl.aldemar.musselapp.ui.theme.EstadoValidadoTexto
import cl.aldemar.musselapp.ui.theme.colores

@Composable
fun RevisionRoute(
    usuario: Usuario,
    centro: CentroCultivo,
    onVolver: () -> Unit,
    onNavegar: (Destino) -> Unit,
    onCerrarSesion: () -> Unit,
    /** Muestra abierta desde el Detalle; null abre la cola desde la primera pendiente. */
    muestraIdInicial: Long? = null,
    viewModel: RevisionViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(muestraIdInicial) { muestraIdInicial?.let(viewModel::enfocarMuestra) }
    // La cola corresponde al centro de la sesión, como el resumen de la Principal
    LaunchedEffect(centro.id) { viewModel.mostrarCentro(centro.id) }

    RevisionScreen(
        uiState = uiState,
        usuario = usuario,
        centro = centro,
        onVolver = onVolver,
        onNavegar = onNavegar,
        onCerrarSesion = onCerrarSesion,
        onSeleccionarMuestra = viewModel::seleccionarMuestra,
        onElegirEstado = viewModel::elegirEstado,
        onActualizarComentario = viewModel::actualizarComentario,
        onAgregarMotivo = viewModel::agregarMotivoRapido,
        onAlternarZoom = viewModel::alternarModalZoom,
        onValidar = viewModel::validarDictamen,
        onGuardar = {
            // Si se llegó desde el Detalle de una muestra, se vuelve a él para ver el estado nuevo
            viewModel.guardarDictamen(supervisor = usuario) { if (muestraIdInicial != null) onVolver() }
        },
        onLimpiarMensaje = viewModel::limpiarMensaje,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RevisionScreen(
    uiState: RevisionUiState,
    usuario: Usuario,
    centro: CentroCultivo,
    onVolver: () -> Unit,
    onNavegar: (Destino) -> Unit,
    onSeleccionarMuestra: (Long) -> Unit,
    onElegirEstado: (EstadoMuestra) -> Unit,
    onActualizarComentario: (String) -> Unit,
    onAgregarMotivo: (String) -> Unit,
    onAlternarZoom: (Boolean) -> Unit,
    onGuardar: () -> Unit,
    onLimpiarMensaje: () -> Unit,
    onCerrarSesion: () -> Unit = {},
    /** Revisa el dictamen antes de pedir confirmación; false si falta algo. */
    onValidar: () -> Boolean = { true },
) {
    val snackbarHostState = remember { SnackbarHostState() }
    // El dictamen es final: se confirma antes de guardarlo
    var confirmando by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(uiState.mensajeToast) {
        val mensaje = uiState.mensajeToast
        if (mensaje != null) {
            snackbarHostState.showSnackbar(mensaje)
            onLimpiarMensaje()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Surface(
                color = MaterialTheme.colorScheme.background,
                contentColor = MaterialTheme.colorScheme.onBackground,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .tarjetaMussel(FormaBoton)
                            .clickable(onClickLabel = "Volver", role = Role.Button, onClick = onVolver),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = MaterialTheme.colorScheme.onBackground,
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "Supervisión", style = MaterialTheme.typography.titleLarge)
                        // Antes decía "ONLINE EN TERRENO" fijo, aunque no hubiera conexión
                        Text(
                            text = usuario.nombre.uppercase(),
                            style = Rotulo,
                            maxLines = 1,
                            color = MaterialTheme.colorScheme.secondary,
                        )
                    }
                    // Al tocar el avatar se ve el perfil y se puede cerrar sesión, como en las demás pantallas
                    MenuPerfil(usuario = usuario, centro = centro, onCerrarSesion = onCerrarSesion)
                }
            }
        },
        bottomBar = {
            NavegacionInferior(
                rol = usuario.rol,
                seleccionado = Destino.SUPERVISION,
                onNavegar = onNavegar,
            )
        },
    ) { innerPadding ->
        if (uiState.cargando) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    // Con el teclado abierto se puede desplazar hasta el comentario técnico sin que quede tapado
                    .consumeWindowInsets(innerPadding)
                    .imePadding()
                    .verticalScroll(rememberScrollState()),
            ) {
                // ── 1. Contextual Header de Supervisión ──────────────────────
                Surface(
                    color = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = FormaTarjeta,
                    shadowElevation = ElevacionTarjeta,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Filled.VerifiedUser,
                                    contentDescription = null,
                                    tint = AcentoSobreOscuro,
                                    modifier = Modifier.size(20.dp),
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "Supervisión de calidad",
                                    style = MaterialTheme.typography.titleMedium,
                                )
                            }

                            // Badge de pendientes
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(Color.White.copy(alpha = 0.15f))
                                    .padding(horizontal = 10.dp, vertical = 4.dp),
                            ) {
                                Text(
                                    "${uiState.muestrasPendientes.size} PENDIENTES",
                                    style = Rotulo,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White,
                                )
                            }
                        }

                        Spacer(Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Filled.LocationOn,
                                contentDescription = null,
                                tint = TextoSobreOscuro,
                                modifier = Modifier.size(14.dp),
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                centro.nombre,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextoSobreOscuro,
                            )
                        }
                    }
                }

                // Si no hay muestras pendientes
                if (uiState.muestrasPendientes.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Icon(
                                Icons.Filled.AssignmentTurnedIn,
                                contentDescription = null,
                                modifier = Modifier.size(56.dp),
                                tint = Acento,
                            )
                            Text(
                                "¡Todas las muestras han sido revisadas!",
                                style = MaterialTheme.typography.titleMedium,
                                textAlign = TextAlign.Center,
                            )
                            Text(
                                "No hay muestras pendientes de validación en este momento.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.secondary,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                } else {
                    val muestra = uiState.muestraActual

                    // ── Selector de muestras pendientes (Chips Horizontales) ──
                    Column(modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)) {
                        Text(
                            "SELECCIONAR MUESTRA EN COLA",
                            style = Rotulo,
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = 16.dp)
                                .selectableGroup(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            uiState.muestrasPendientes.forEach { m ->
                                val seleccionada = m.id == muestra?.id
                                Box(
                                    modifier = Modifier
                                        .clip(FormaCampo)
                                        .background(if (seleccionada) MaterialTheme.colorScheme.primary else Color.White)
                                        .border(
                                            width = 1.dp,
                                            color = if (seleccionada) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                            shape = FormaCampo,
                                        )
                                        // TalkBack anuncia la muestra como opción seleccionada o no
                                        .selectable(
                                            selected = seleccionada,
                                            role = Role.RadioButton,
                                            onClick = { onSeleccionarMuestra(m.id) },
                                        )
                                        .heightIn(min = 48.dp)
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        ) {
                                            Text(
                                                m.codigo,
                                                style = MaterialTheme.typography.titleSmall,
                                                color = if (seleccionada) Color.White else MaterialTheme.colorScheme.onSurface,
                                            )
                                            // Las muestras con alertas se distinguen en la cola sin abrirlas
                                            InsigniaAlertas(m)
                                        }
                                        Text(
                                            "${m.codigoLinea} • Tren ${m.tren}",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = if (seleccionada) TextoSobreOscuro else MaterialTheme.colorScheme.secondary,
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (muestra != null) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            // ── 2. Tarjeta Hero de Muestra Evaluada ───────────────
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = FormaTarjeta,
                                color = Color.White,
                                border = BordeDeTarjeta,
                                shadowElevation = ElevacionTarjeta,
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.Top,
                                    ) {
                                        Column {
                                            Text(
                                                "EVALUANDO MUESTRA",
                                                style = Rotulo,
                                                color = MaterialTheme.colorScheme.secondary,
                                            )
                                            Text(
                                                muestra.codigo,
                                                style = MaterialTheme.typography.headlineSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface,
                                            )
                                            Text(
                                                "${muestra.codigoLinea} • Tren de cultivo ${muestra.tren}",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.secondary,
                                            )
                                        }

                                        Column(horizontalAlignment = Alignment.End) {
                                            ChipEstado(muestra.estado)
                                            Spacer(Modifier.height(4.dp))
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = SuperficieCampo,
                                                border = BorderStroke(1.dp, Linea),
                                            ) {
                                                Text(
                                                    "PROF. ${muestra.profundidadM}M",
                                                    style = Rotulo,
                                                    color = MaterialTheme.colorScheme.secondary,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                )
                                            }
                                        }
                                    }

                                    Spacer(Modifier.height(10.dp))

                                    // Tira de métricas marinas
                                    Surface(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = FormaCampo,
                                        color = SuperficieCampo,
                                        border = BorderStroke(1.dp, Linea),
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(12.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                        ) {
                                            Column {
                                                Text("DENSIDAD", style = Rotulo, color = MaterialTheme.colorScheme.secondary)
                                                Row(verticalAlignment = Alignment.Bottom) {
                                                    Text("${muestra.conteoIndM}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                                    Text(" ind/m", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary, modifier = Modifier.padding(bottom = 3.dp))
                                                }
                                            }
                                            Column {
                                                Text("CALIBRE PROM.", style = Rotulo, color = MaterialTheme.colorScheme.secondary)
                                                Row(verticalAlignment = Alignment.Bottom) {
                                                    Text("${muestra.tallaMm}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                                    Text(" mm", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary, modifier = Modifier.padding(bottom = 3.dp))
                                                }
                                            }
                                            Column {
                                                Text("RENDIMIENTO", style = Rotulo, color = MaterialTheme.colorScheme.secondary)
                                                Row(verticalAlignment = Alignment.Bottom) {
                                                    Text("${muestra.rendimientoKgM}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                                    Text(" kg/m", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary, modifier = Modifier.padding(bottom = 3.dp))
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // ── Alertas automáticas y condiciones al muestrear ──
                            TarjetaAlertas(muestra = muestra)
                            TarjetaCondiciones(muestra = muestra)

                            // ── 3. Fotografía de la muestra ─────────────────────
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = FormaTarjeta,
                                color = Color.White,
                                border = BordeDeTarjeta,
                                shadowElevation = ElevacionTarjeta,
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(4f / 3f)
                                        .clip(FormaTarjeta)
                                        // Toda la foto es el área táctil para ampliar
                                        .then(
                                            if (muestra.fotoRuta != null) {
                                                Modifier.clickable(
                                                    onClickLabel = "Ampliar la fotografía",
                                                    role = Role.Button,
                                                ) { onAlternarZoom(true) }
                                            } else {
                                                Modifier
                                            },
                                        ),
                                ) {
                                    FotoMuestra(
                                        fotoRuta = muestra.fotoRuta,
                                        modifier = Modifier.fillMaxSize(),
                                    )

                                    if (muestra.fotoRuta != null) {
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .padding(10.dp)
                                                .clip(RoundedCornerShape(50))
                                                .background(Principal)
                                                .padding(horizontal = 12.dp, vertical = 7.dp),
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Filled.ZoomIn, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                                Spacer(Modifier.width(4.dp))
                                                Text("Ampliar", color = Color.White, style = MaterialTheme.typography.labelMedium)
                                            }
                                        }
                                    }
                                }
                            }

                            // ── 4. Formulario de Dictamen Técnico ───────────────
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = FormaTarjeta,
                                color = Color.White,
                                border = BordeDeTarjeta,
                                shadowElevation = ElevacionTarjeta,
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    Text(
                                        "Dictamen técnico del supervisor",
                                        style = MaterialTheme.typography.titleMedium,
                                    )

                                    // Botones de Decisión (Validar / Corregir)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    ) {
                                        // Botón VALIDAR
                                        val esValidado = uiState.estadoElegido == EstadoMuestra.VALIDADO
                                        Button(
                                            onClick = { onElegirEstado(EstadoMuestra.VALIDADO) },
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(56.dp)
                                                .semantics { selected = esValidado },
                                            shape = FormaCampo,
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (esValidado) EstadoValidadoTexto else EstadoValidadoFondo,
                                                contentColor = if (esValidado) Color.White else EstadoValidadoTexto,
                                            ),
                                            border = androidx.compose.foundation.BorderStroke(
                                                width = 1.dp,
                                                color = EstadoValidadoTexto,
                                            ),
                                        ) {
                                            Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                                            Spacer(Modifier.width(6.dp))
                                            Text("Validar", style = MaterialTheme.typography.titleSmall)
                                        }

                                        // Botón CORREGIR: mismos colores que el chip Corregido
                                        val esCorregido = uiState.estadoElegido == EstadoMuestra.CORREGIDO
                                        val coloresCorregido = EstadoMuestra.CORREGIDO.colores
                                        Button(
                                            onClick = { onElegirEstado(EstadoMuestra.CORREGIDO) },
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(56.dp)
                                                .semantics { selected = esCorregido },
                                            shape = FormaCampo,
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (esCorregido) coloresCorregido.texto else coloresCorregido.fondo,
                                                contentColor = if (esCorregido) Color.White else coloresCorregido.texto,
                                            ),
                                            border = androidx.compose.foundation.BorderStroke(
                                                width = 1.dp,
                                                color = coloresCorregido.texto,
                                            ),
                                        ) {
                                            Icon(Icons.Filled.EditNote, contentDescription = null, modifier = Modifier.size(18.dp))
                                            Spacer(Modifier.width(6.dp))
                                            Text("Corregir", style = MaterialTheme.typography.titleSmall)
                                        }
                                    }

                                    // El aviso aparece junto a los botones que faltan, no en el comentario
                                    uiState.errorDictamen?.let { error ->
                                        Text(
                                            error,
                                            color = MaterialTheme.colorScheme.error,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Medium,
                                            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                                        )
                                    }

                                    // Chips de motivos rápidos
                                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text(
                                            "JUSTIFICACIONES TÉCNICAS FRECUENTES",
                                            style = Rotulo,
                                            color = MaterialTheme.colorScheme.secondary,
                                        )
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .horizontalScroll(rememberScrollState()),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        ) {
                                            MotivoChip("Densidad bajo umbral (< 350 ind/m)") { onAgregarMotivo("Densidad bajo el rango mínimo estándar (< 350 ind/m)") }
                                            MotivoChip("Epifauna / Fouling detectado") { onAgregarMotivo("Presencia notable de epifauna y fouling en el tramo") }
                                            MotivoChip("Talla irregular") { onAgregarMotivo("Dispersión de calibre anómala para el ciclo de engorda") }
                                            MotivoChip("Fijación débil") { onAgregarMotivo("Biso débil en cuerda, verificar anclaje") }
                                        }
                                    }

                                    // Campo de Comentario Técnico
                                    OutlinedTextField(
                                        value = uiState.comentarioTecnico,
                                        onValueChange = onActualizarComentario,
                                        label = { Text("Comentario técnico") },
                                        placeholder = { Text("Ingresa el fundamento del dictamen o qué se corrigió...") },
                                        modifier = Modifier.fillMaxWidth(),
                                        minLines = 3,
                                        shape = FormaCampo,
                                        isError = uiState.errorComentario != null,
                                        supportingText = {
                                            if (uiState.errorComentario != null) {
                                                Text(uiState.errorComentario, color = MaterialTheme.colorScheme.error)
                                            } else {
                                                Text("Obligatorio al Corregir; opcional al Validar.")
                                            }
                                        },
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = Acento,
                                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                                            focusedLabelColor = AcentoTexto,
                                            cursorColor = Acento,
                                        ),
                                    )

                                    // Botón Guardar Dictamen
                                    Button(
                                        onClick = { if (onValidar()) confirmando = true },
                                        enabled = !uiState.guardando,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(56.dp),
                                        shape = FormaBoton,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.primary,
                                            contentColor = MaterialTheme.colorScheme.onPrimary,
                                        ),
                                    ) {
                                        if (uiState.guardando) {
                                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                                        } else {
                                            Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(20.dp))
                                            Spacer(Modifier.width(8.dp))
                                            Text("Confirmar y guardar dictamen", style = MaterialTheme.typography.titleMedium)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Confirmación del dictamen: Validado y Corregido no se pueden cambiar después
    val muestraDictamen = uiState.muestraActual
    val estadoDictamen = uiState.estadoElegido
    if (confirmando && muestraDictamen != null && estadoDictamen != null) {
        AlertDialog(
            onDismissRequest = { confirmando = false },
            icon = { Icon(Icons.Filled.VerifiedUser, contentDescription = null) },
            title = { Text("¿Registrar dictamen ${estadoDictamen.etiqueta}?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("${muestraDictamen.codigo} • ${muestraDictamen.codigoLinea} • Tren ${muestraDictamen.tren}")
                    uiState.comentarioTecnico.trim().takeIf { it.isNotEmpty() }?.let {
                        Text("Comentario: $it", style = MaterialTheme.typography.bodySmall, maxLines = 4)
                    }
                    Text(
                        "El dictamen es final y no se podrá modificar.",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    confirmando = false
                    onGuardar()
                }) { Text("Registrar", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold) }
            },
            dismissButton = {
                TextButton(onClick = { confirmando = false }) { Text("Seguir revisando") }
            },
        )
    }

    // Foto ampliada a pantalla completa, con pellizco para acercar y giro
    val muestraZoom = uiState.muestraActual
    val rutaZoom = muestraZoom?.fotoRuta
    if (uiState.modalZoomAbierto && muestraZoom != null && rutaZoom != null) {
        VisorFoto(
            ruta = rutaZoom,
            onCerrar = { onAlternarZoom(false) },
            titulo = "Inspección ${muestraZoom.codigo}",
        )
    }
}

@Composable
private fun MotivoChip(texto: String, onClick: () -> Unit) {
    Surface(
        // Área táctil mínima de 48 dp aunque el chip se vea más bajo
        modifier = Modifier
            .minimumInteractiveComponentSize()
            .clip(RoundedCornerShape(50))
            .clickable(onClickLabel = "Agregar motivo al comentario", role = Role.Button, onClick = onClick),
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Text(
            text = texto,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
        )
    }
}
