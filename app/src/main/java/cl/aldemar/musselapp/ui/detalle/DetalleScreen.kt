package cl.aldemar.musselapp.ui.detalle

import cl.aldemar.musselapp.ui.componentes.TarjetaAlertas
import cl.aldemar.musselapp.ui.componentes.TarjetaCondiciones
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import android.content.ActivityNotFoundException
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import cl.aldemar.musselapp.ui.theme.Acento
import cl.aldemar.musselapp.ui.theme.BordeDeCampo
import cl.aldemar.musselapp.ui.theme.BordeDeTarjeta
import cl.aldemar.musselapp.ui.theme.DatoMedido
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
import cl.aldemar.musselapp.R
import cl.aldemar.musselapp.data.model.CentroCultivo
import cl.aldemar.musselapp.data.model.EstadoMuestra
import cl.aldemar.musselapp.data.model.Muestra
import cl.aldemar.musselapp.data.model.Rol
import cl.aldemar.musselapp.data.model.Usuario
import cl.aldemar.musselapp.ui.componentes.MenuPerfil
import cl.aldemar.musselapp.ui.componentes.ChipEstado
import cl.aldemar.musselapp.ui.componentes.FotoMuestra
import androidx.compose.ui.semantics.Role
import cl.aldemar.musselapp.ui.componentes.VisorFoto
import cl.aldemar.musselapp.ui.theme.AlertaFondo
import cl.aldemar.musselapp.ui.theme.AlertaTexto
import cl.aldemar.musselapp.ui.theme.EstadoValidadoFondo
import cl.aldemar.musselapp.ui.theme.EstadoValidadoTexto
import cl.aldemar.musselapp.ui.theme.colores
import cl.aldemar.musselapp.util.Fechas
import cl.aldemar.musselapp.util.FichaMuestra
import cl.aldemar.musselapp.util.FichaPdf
import cl.aldemar.musselapp.util.RutValidator
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DetalleRoute(
    muestraId: Long,
    usuario: Usuario,
    centro: CentroCultivo,
    onVolver: () -> Unit,
    onCerrarSesion: () -> Unit,
    onIrARevision: ((Long) -> Unit)? = null,
    viewModel: DetalleViewModel = viewModel(factory = DetalleViewModel.Factory(muestraId)),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    DetalleScreen(
        uiState = uiState,
        usuario = usuario,
        centro = centro,
        onVolver = onVolver,
        onCerrarSesion = onCerrarSesion,
        onIrARevision = onIrARevision,
        onAlternarZoom = viewModel::alternarModalZoom,
        onMostrarMensaje = viewModel::mostrarMensaje,
        onLimpiarMensaje = viewModel::limpiarMensaje,
    )
}

@Composable
fun DetalleScreen(
    uiState: DetalleUiState,
    usuario: Usuario,
    onVolver: () -> Unit,
    onIrARevision: ((Long) -> Unit)?,
    onAlternarZoom: (Boolean) -> Unit,
    onMostrarMensaje: (String) -> Unit,
    onLimpiarMensaje: () -> Unit,
    centro: CentroCultivo = CentroCultivo.disponibles.first(),
    onCerrarSesion: () -> Unit = {},
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // La ficha PDF se genera fuera del hilo principal (lee la foto y escribe un archivo)
    fun conFicha(accion: (File) -> Unit) {
        val muestra = uiState.muestra ?: return
        scope.launch {
            try {
                val pdf = withContext(Dispatchers.IO) { FichaPdf.generar(context, muestra) }
                accion(pdf)
            } catch (e: ActivityNotFoundException) {
                onMostrarMensaje("No hay una app instalada para esta acción")
            } catch (e: Exception) {
                onMostrarMensaje("No se pudo generar la ficha técnica")
            }
        }
    }

    // "Descargar": el usuario elige dónde guardar el PDF (por ejemplo, Descargas)
    val guardarFicha = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/pdf"),
    ) { destino ->
        if (destino != null) {
            conFicha { pdf ->
                scope.launch {
                    try {
                        withContext(Dispatchers.IO) { FichaPdf.guardarEn(context, pdf, destino) }
                        onMostrarMensaje("Ficha técnica guardada")
                    } catch (e: Exception) {
                        onMostrarMensaje("No se pudo guardar la ficha técnica")
                    }
                }
            }
        }
    }

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
                        Text(text = "Detalle de muestra", style = MaterialTheme.typography.titleLarge)
                        Text(text = "MUSSELAPP • OPERACIONES", style = Rotulo, color = MaterialTheme.colorScheme.secondary)
                    }
                    // Al tocar el avatar se ve el perfil y se puede cerrar sesión, como en las demás pantallas
                    MenuPerfil(usuario = usuario, centro = centro, onCerrarSesion = onCerrarSesion)
                }
            }
        },
    ) { paddingValores ->
        if (uiState.cargando) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValores),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else {
            val muestra = uiState.muestra
            if (muestra == null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValores),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("No se encontró la información de la muestra.", style = MaterialTheme.typography.bodyLarge)
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValores)
                        .verticalScroll(rememberScrollState()),
                ) {
                    // ── 1. Franja Contextual Superior ─────────────────────────
                    FranjaContextualDetalle(
                        muestra = muestra,
                        onCompartir = { conFicha { pdf -> FichaPdf.compartir(context, pdf, muestra) } },
                        onImprimir = { conFicha { pdf -> FichaPdf.imprimir(context, pdf, muestra) } },
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        // ── 2. Fotografía de la muestra ───────────────────────
                        TarjetaFotoMuestra(
                            muestra = muestra,
                            onExpandir = { onAlternarZoom(true) },
                        )

                        // ── 3. Resolución y Dictamen Técnico ──────────────────
                        TarjetaDictamenTecnico(
                            muestra = muestra,
                            esSupervisor = usuario.rol == Rol.SUPERVISOR,
                            onIrARevision = { onIrARevision?.invoke(muestra.id) },
                        )

                        // ── 4. Resumen biométrico ─────────────────────────────
                        TarjetaResumenBiometrico(muestra = muestra)

                        // ── Alertas automáticas y condiciones al muestrear ────
                        TarjetaAlertas(muestra = muestra)
                        TarjetaCondiciones(muestra = muestra)

                        // ── 5. Trazabilidad ───────────────────────────────────
                        TarjetaTrazabilidad(muestra = muestra)

                        // ── 6. Acciones Finales ───────────────────────────────
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp, bottom = 24.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Button(
                                onClick = { guardarFicha.launch(FichaMuestra.nombreArchivo(muestra)) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp),
                                shape = FormaBoton,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary,
                                ),
                            ) {
                                Icon(Icons.Filled.PictureAsPdf, contentDescription = null, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Descargar ficha técnica PDF", style = MaterialTheme.typography.titleSmall)
                            }

                            OutlinedButton(
                                onClick = onVolver,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                shape = FormaBoton,
                                border = BordeDeCampo,
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Volver", style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }
                }
            }
        }
    }

    // Foto ampliada a pantalla completa, con pellizco para acercar y giro
    val muestraZoom = uiState.muestra
    val rutaZoom = muestraZoom?.fotoRuta
    if (uiState.modalZoomAbierto && muestraZoom != null && rutaZoom != null) {
        VisorFoto(
            ruta = rutaZoom,
            onCerrar = { onAlternarZoom(false) },
            titulo = "${muestraZoom.codigo} • ${muestraZoom.conteoIndM} ind/m • ${muestraZoom.tallaMm} mm",
        )
    }
}

@Composable
private fun FranjaContextualDetalle(
    muestra: Muestra,
    onCompartir: () -> Unit,
    onImprimir: () -> Unit,
) {
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
                .padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.Science,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "${muestra.codigo} • Tren ${muestra.tren} • ${muestra.codigoLinea}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                }
                Row {
                    IconButton(onClick = onCompartir) {
                        Icon(Icons.Filled.Share, contentDescription = "Compartir", tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onImprimir) {
                        Icon(Icons.Filled.Print, contentDescription = "Imprimir", tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(18.dp))
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // Grid de 4 micro-métricas
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                ItemMicroMetrica("PROF.", "${muestra.profundidadM} m", Modifier.weight(1f))
                ItemMicroMetrica("DENSIDAD", "${muestra.conteoIndM} /m", Modifier.weight(1f))
                ItemMicroMetrica("TALLA", "${muestra.tallaMm} mm", Modifier.weight(1f))
                ItemMicroMetrica("REND.", "${muestra.rendimientoKgM} kg/m", Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun ItemMicroMetrica(etiqueta: String, valor: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color.White.copy(alpha = 0.10f))
            .padding(horizontal = 8.dp, vertical = 8.dp),
    ) {
        Column {
            Text(text = etiqueta, style = Rotulo, color = TextoSobreOscuro, maxLines = 1)
            Text(
                text = valor,
                style = MaterialTheme.typography.titleSmall,
                color = Color.White,
                maxLines = 1,
            )
        }
    }
}

/** Foto real de la muestra y la fecha de registro; sin foto se muestra "Sin fotografía". */
@Composable
private fun TarjetaFotoMuestra(
    muestra: Muestra,
    onExpandir: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = FormaTarjeta,
        color = Color.White,
        border = BordeDeTarjeta,
        shadowElevation = ElevacionTarjeta,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.CenterFocusStrong,
                    contentDescription = null,
                    tint = Acento,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text("Fotografía de la muestra", style = MaterialTheme.typography.titleMedium)
            }

            Spacer(Modifier.height(10.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(4f / 3f)
                    .clip(FormaCampo)
                    // Toda la foto es el área táctil para ampliar (antes solo una píldora de ~32 dp)
                    .then(
                        if (muestra.fotoRuta != null) {
                            Modifier.clickable(onClickLabel = "Ampliar la fotografía", role = Role.Button, onClick = onExpandir)
                        } else {
                            Modifier
                        },
                    ),
            ) {
                FotoMuestra(fotoRuta = muestra.fotoRuta, modifier = Modifier.fillMaxSize())

                // Fecha y hora en que el operario registró la muestra
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Principal)
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                ) {
                    Text(fechaHora(muestra.registradaEn), color = Color.White, style = Rotulo)
                }

                if (muestra.fotoRuta != null) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(8.dp)
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
    }
}

@Composable
private fun TarjetaDictamenTecnico(
    muestra: Muestra,
    esSupervisor: Boolean,
    onIrARevision: () -> Unit,
) {
    // Pendiente y Corregido usan el ámbar de ColoresEstado
    val colorBordeEstado = when (muestra.estado) {
        EstadoMuestra.VALIDADO -> EstadoValidadoTexto
        EstadoMuestra.CORREGIDO, EstadoMuestra.PENDIENTE -> muestra.estado.colores.acento
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = FormaTarjeta,
        color = Color.White,
        border = BordeDeTarjeta,
        shadowElevation = ElevacionTarjeta,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.RateReview, contentDescription = null, tint = Acento, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Dictamen del supervisor", style = MaterialTheme.typography.titleMedium)
                }
                ChipEstado(muestra.estado)
            }

            // Quién revisó y cuándo (datos de la tabla revisiones)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = FormaCampo,
                color = SuperficieCampo,
                border = BorderStroke(1.dp, Linea),
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            muestra.revisadaPor ?: "Pendiente de revisión",
                            style = MaterialTheme.typography.titleSmall,
                        )
                        muestra.revisadaEn?.let { revisada ->
                            Text(
                                Fechas.relativa(revisada),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.secondary,
                            )
                        }
                    }
                    Text(
                        if (muestra.revisadaPor != null) "Supervisor de Calidad" else "En espera de la revisión del supervisor de calidad",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }
            }

            muestra.comentarioSupervisor?.takeIf { it.isNotBlank() }?.let { comentario ->
                CajaTexto(etiqueta = "COMENTARIO DEL SUPERVISOR", texto = comentario, colorBorde = colorBordeEstado)
            }
            CajaTexto(
                etiqueta = "OBSERVACIÓN DEL OPERARIO",
                texto = muestra.observacion.takeIf { it.isNotBlank() } ?: "Sin observaciones de terreno.",
                colorBorde = MaterialTheme.colorScheme.outline,
            )

            // Solo una muestra pendiente se puede dictaminar: Validado y Corregido son finales
            if (esSupervisor && muestra.estado == EstadoMuestra.PENDIENTE) {
                Button(
                    onClick = onIrARevision,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = FormaCampo,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = Color.White,
                    ),
                ) {
                    Icon(Icons.Filled.RateReview, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Revisar esta muestra", style = MaterialTheme.typography.titleSmall)
                }
            }
        }
    }
}

/** Texto destacado con una franja de color a la izquierda. */
@Composable
private fun CajaTexto(etiqueta: String, texto: String, colorBorde: Color) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(FormaCampo)
            .background(SuperficieCampo)
            .border(width = 1.dp, color = Linea, shape = FormaCampo)
            .drawLeftBorder(color = colorBorde, strokeWidth = 4.dp)
            .padding(12.dp),
    ) {
        Column {
            Text(etiqueta, style = Rotulo, color = MaterialTheme.colorScheme.secondary)
            Spacer(Modifier.height(4.dp))
            Text(texto, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
private fun TarjetaResumenBiometrico(muestra: Muestra) {
    val desvio = muestra.desvioDensidadPct
    val esBajo = desvio < 0
    val desvioTexto = if (desvio >= 0) "+$desvio%" else "$desvio%"
    val colorDesvio = if (esBajo) AlertaTexto else EstadoValidadoTexto

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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Analytics, contentDescription = null, tint = Acento, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Resumen biométrico", style = MaterialTheme.typography.titleMedium)
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(if (esBajo) AlertaFondo else EstadoValidadoFondo)
                        .padding(horizontal = 9.dp, vertical = 4.dp),
                ) {
                    Text("$desvioTexto DESVÍO", color = colorDesvio, style = Rotulo, fontWeight = FontWeight.SemiBold)
                }
            }

            // Densidad medida por el operario frente a la densidad de referencia
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = FormaCampo,
                color = SuperficieCampo,
                border = BorderStroke(1.dp, Linea),
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("DENSIDAD (CONTEO MANUAL)", style = Rotulo, color = MaterialTheme.colorScheme.secondary)
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            "${muestra.conteoIndM}",
                            style = DatoMedido,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("ind / m", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary, modifier = Modifier.padding(bottom = 4.dp))
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            if (esBajo) Icons.Filled.ArrowDownward else Icons.Filled.ArrowUpward,
                            contentDescription = null,
                            tint = colorDesvio,
                            modifier = Modifier.size(14.dp),
                        )
                        Spacer(Modifier.width(2.dp))
                        Text(
                            "$desvioTexto respecto a la densidad de referencia (${Muestra.DENSIDAD_ESTANDAR_IND_M} ind/m)",
                            style = MaterialTheme.typography.labelMedium,
                            color = colorDesvio,
                        )
                    }
                    // Cholgas: otra especie de la misma familia, contada aparte en el mismo tramo
                    Text(
                        "Cholgas: " + (muestra.cholgasIndM?.let { "$it ind / m" } ?: "no registradas"),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                    Text(
                        "Tramo muestreado: " + (muestra.tramoCm?.let { "$it cm" } ?: "no registrado"),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ItemMetricaBiometrica("Talla media", "${muestra.tallaMm}", "milímetros", Modifier.weight(1f))
                ItemMetricaBiometrica("Rendimiento", "${muestra.rendimientoKgM}", "kg / m (estimado)", Modifier.weight(1f))
                ItemMetricaBiometrica("Profundidad", "${muestra.profundidadM}", "metros", Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun ItemMetricaBiometrica(titulo: String, valor: String, unidad: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = FormaCampo,
        color = SuperficieCampo,
        border = BorderStroke(1.dp, Linea),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(titulo.uppercase(), style = Rotulo, color = MaterialTheme.colorScheme.secondary, maxLines = 1)
            Spacer(Modifier.height(4.dp))
            Text(valor, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Text(unidad, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary, textAlign = TextAlign.Center)
        }
    }
}

/** Quién registró y revisó la muestra, y si ya se envió: todo sale de la base. */
@Composable
private fun TarjetaTrazabilidad(muestra: Muestra) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = FormaTarjeta,
        color = Color.White,
        border = BordeDeTarjeta,
        shadowElevation = ElevacionTarjeta,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.VerifiedUser, contentDescription = null, tint = Acento, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(6.dp))
                Text("Trazabilidad", style = MaterialTheme.typography.titleMedium)
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                ItemFilaTrazabilidad("Código", muestra.codigo)
                ItemFilaTrazabilidad("Operario", muestra.operador.ifBlank { "—" })
                ItemFilaTrazabilidad(
                    "RUT operario",
                    muestra.operadorRut.takeIf { it.isNotBlank() }?.let(RutValidator::formatear) ?: "—",
                )
                ItemFilaTrazabilidad("Registrada", fechaHora(muestra.registradaEn))
                ItemFilaTrazabilidad("Revisada", muestra.revisadaEn?.let(::fechaHora) ?: "Pendiente")
                ItemFilaTrazabilidad("Envío", if (muestra.sincronizada) "Sincronizada" else "Pendiente de envío")
            }
        }
    }
}

@Composable
private fun ItemFilaTrazabilidad(label: String, valor: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary)
        Spacer(Modifier.width(12.dp))
        Text(
            valor,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.End,
        )
    }
}

/** "24/10/2026 • 09:30" en hora local. */
private fun fechaHora(instante: Long): String =
    SimpleDateFormat("dd/MM/yyyy • HH:mm", Locale.forLanguageTag("es-CL")).format(Date(instante))

private fun Modifier.drawLeftBorder(color: Color, strokeWidth: androidx.compose.ui.unit.Dp): Modifier = this.then(
    Modifier.drawWithContent {
        drawContent()
        drawLine(
            color = color,
            start = androidx.compose.ui.geometry.Offset(0f, 0f),
            end = androidx.compose.ui.geometry.Offset(0f, size.height),
            strokeWidth = strokeWidth.toPx(),
        )
    }
)
