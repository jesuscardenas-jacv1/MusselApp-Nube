package cl.aldemar.musselapp.ui.principal

import android.content.Context
import androidx.core.content.edit
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.ChecklistRtl
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Dataset
import androidx.compose.material.icons.automirrored.filled.FactCheck
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.HistoryEdu
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import cl.aldemar.musselapp.data.model.CentroCultivo
import cl.aldemar.musselapp.data.model.Muestra
import cl.aldemar.musselapp.data.model.Rol
import cl.aldemar.musselapp.data.model.Usuario
import cl.aldemar.musselapp.data.repository.FakeMuestraRepository
import cl.aldemar.musselapp.ui.componentes.BarraSuperiorMussel
import cl.aldemar.musselapp.ui.componentes.ChipEstado
import cl.aldemar.musselapp.ui.componentes.NavegacionInferior
import androidx.compose.foundation.border
import cl.aldemar.musselapp.ui.theme.Acento
import cl.aldemar.musselapp.ui.theme.AcentoSuave
import cl.aldemar.musselapp.ui.theme.AcentoTexto
import cl.aldemar.musselapp.ui.theme.BordeDeTarjeta
import cl.aldemar.musselapp.ui.theme.DatoMedido
import cl.aldemar.musselapp.ui.theme.ElevacionTarjeta
import cl.aldemar.musselapp.ui.theme.FormaCampo
import cl.aldemar.musselapp.ui.theme.FormaTarjeta
import cl.aldemar.musselapp.ui.theme.FormaTarjetaChica
import cl.aldemar.musselapp.ui.theme.Linea
import cl.aldemar.musselapp.ui.theme.Rotulo
import cl.aldemar.musselapp.ui.theme.TextoSobreOscuro
import cl.aldemar.musselapp.ui.theme.SuperficieCampo
import cl.aldemar.musselapp.ui.theme.MusselAppTheme
import cl.aldemar.musselapp.util.Fechas

/** Secciones de la app accesibles desde la Pantalla Principal. */
enum class Destino(val etiqueta: String, val icono: ImageVector) {
    MUESTRAS("Muestras", Icons.Filled.Dataset),
    NUEVA_MUESTRA("Nueva Muestra", Icons.Filled.AddCircleOutline),
    HISTORIAL("Historial", Icons.Filled.History),
    SUPERVISION("Supervisión", Icons.Filled.ManageAccounts),
    ;

    companion object {
        /** Menú de opciones según el rol: el operario registra, el supervisor revisa. */
        fun disponiblesPara(rol: Rol): List<Destino> = when (rol) {
            Rol.OPERARIO -> listOf(MUESTRAS, NUEVA_MUESTRA, HISTORIAL)
            Rol.SUPERVISOR -> listOf(MUESTRAS, HISTORIAL, SUPERVISION)
        }
    }
}

/** Punto de entrada con estado: conecta el ViewModel con la UI. */
@Composable
fun PrincipalRoute(
    usuario: Usuario,
    centro: CentroCultivo,
    onCerrarSesion: () -> Unit,
    onNavegar: (Destino) -> Unit,
    onVerMuestra: (Long) -> Unit,
    viewModel: PrincipalViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    // La Principal resume el centro con el que se inició sesión
    LaunchedEffect(centro.id) { viewModel.mostrarCentro(centro.id) }
    // Cada usuario elige si ver los contadores del historial; se recuerda al volver a entrar
    val contexto = LocalContext.current
    val preferencias = remember { PreferenciasPrincipal(contexto) }
    var historialDesplegado by rememberSaveable(usuario.rut) { mutableStateOf(preferencias.historialDesplegado(usuario.rut)) }
    PrincipalScreen(
        usuario = usuario,
        centro = centro,
        uiState = uiState,
        onSincronizar = viewModel::sincronizar,
        onCerrarSesion = onCerrarSesion,
        onNavegar = onNavegar,
        onVerMuestra = onVerMuestra,
        onMensajeMostrado = viewModel::onMensajeMostrado,
        historialDesplegado = historialDesplegado,
        onAlternarHistorial = {
            historialDesplegado = !historialDesplegado
            preferencias.guardarHistorialDesplegado(usuario.rut, historialDesplegado)
        },
    )
}

/** Preferencias de la pantalla Principal por usuario (por ahora, si el cuadro del historial va desplegado). */
class PreferenciasPrincipal(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences("pantalla_principal", Context.MODE_PRIVATE)

    /** Desplegado salvo que el usuario lo haya plegado. */
    fun historialDesplegado(rut: String): Boolean = prefs.getBoolean("historial_desplegado_$rut", true)

    fun guardarHistorialDesplegado(rut: String, desplegado: Boolean) {
        prefs.edit { putBoolean("historial_desplegado_$rut", desplegado) }
    }
}

/** UI sin estado de la Pantalla Principal (diseño "2. Principal / Dashboard" de Stitch). */
@Composable
fun PrincipalScreen(
    usuario: Usuario,
    centro: CentroCultivo,
    uiState: PrincipalUiState,
    onSincronizar: () -> Unit,
    onCerrarSesion: () -> Unit,
    onNavegar: (Destino) -> Unit,
    onVerMuestra: (Long) -> Unit,
    onMensajeMostrado: () -> Unit,
    historialDesplegado: Boolean = true,
    onAlternarHistorial: () -> Unit = {},
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val destinos = Destino.disponiblesPara(usuario.rol)

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
                seccion = "Muestras",
                usuario = usuario,
                centro = centro,
                pendientesSincronizar = uiState.resumen.pendientesSincronizar,
                sincronizando = uiState.sincronizando,
                onSincronizar = onSincronizar,
                onCerrarSesion = onCerrarSesion,
                onIrAInicio = {}, // ya estamos en la Principal
            )
        },
        bottomBar = {
            NavegacionInferior(rol = usuario.rol, seleccionado = Destino.MUESTRAS, onNavegar = onNavegar)
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            AccionPrincipal(
                rol = usuario.rol,
                pendientesRevision = uiState.resumen.pendientesRevision,
                onClick = {
                    onNavegar(if (usuario.rol == Rol.OPERARIO) Destino.NUEVA_MUESTRA else Destino.SUPERVISION)
                },
            )
            MetricasJornada(resumen = uiState.resumen)
            TarjetaHistorial(
                resumen = uiState.resumen,
                desplegado = historialDesplegado,
                onAlternar = onAlternarHistorial,
                onVerTodo = { onNavegar(Destino.HISTORIAL) },
            )
            MuestrasRecientes(
                muestras = uiState.recientes,
                ultimaRegistradaEn = uiState.resumen.ultimaRegistradaEn,
                onMuestraClick = { onVerMuestra(it.id) },
            )
        }
    }
}

/** Acción principal según el rol: registrar (operario) o revisar (supervisor). */
@Composable
private fun AccionPrincipal(rol: Rol, pendientesRevision: Int, onClick: () -> Unit) {
    val operario = rol == Rol.OPERARIO
    Surface(
        onClick = onClick,
        shape = FormaTarjeta,
        color = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        shadowElevation = ElevacionTarjeta,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    if (operario) Icons.Filled.AddCircle else Icons.AutoMirrored.Filled.FactCheck,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(36.dp),
                )
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    if (operario) "LÍNEA ACTIVA" else "$pendientesRevision PENDIENTES",
                    style = Rotulo,
                    color = Color.White,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Color.White.copy(alpha = 0.16f))
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    if (operario) "Nueva muestra" else "Revisar muestras",
                    style = MaterialTheme.typography.headlineSmall,
                    maxLines = 2,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    if (operario) "Registro de muestra en cuerda y tren activo" else "Validar o corregir las muestras en cola",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextoSobreOscuro,
                )
            }
            Spacer(Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.ChevronRight, contentDescription = null)
            }
        }
    }
}

@Composable
private fun EncabezadoSeccion(titulo: String, detalle: String?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(titulo, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        if (detalle != null) {
            Text(detalle.uppercase(), style = Rotulo, color = MaterialTheme.colorScheme.secondary)
        }
    }
}

@Composable
private fun MetricasJornada(resumen: ResumenJornada) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        EncabezadoSeccion("Métricas de la jornada", "HOY")
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TarjetaMetrica("Talla Med.", Icons.Filled.Straighten, "${resumen.tallaPromedioMm}", "mm", Modifier.weight(1f)) {
                TextoMetrica(resumen.calibre)
            }
            TarjetaMetrica("Conteo", Icons.Filled.GridView, "${resumen.conteoPromedioIndM}", "ind/m", Modifier.weight(1f)) {
                TextoMetrica(resumen.densidad)
            }
            TarjetaMetrica("Líneas", Icons.Filled.ChecklistRtl, "${resumen.lineasMuestreadas}", "/${resumen.lineasTotales}", Modifier.weight(1f)) {
                LinearProgressIndicator(
                    progress = { resumen.avanceLineas },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(50)),
                    color = Acento,
                    trackColor = Linea,
                    drawStopIndicator = {},
                )
            }
        }
    }
}

@Composable
private fun TextoMetrica(texto: String) {
    Text(texto, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
}

@Composable
private fun TarjetaMetrica(
    titulo: String,
    icono: ImageVector,
    valor: String,
    unidad: String,
    modifier: Modifier = Modifier,
    pie: @Composable () -> Unit,
) {
    Surface(
        shape = FormaTarjetaChica,
        color = MaterialTheme.colorScheme.surface,
        border = BordeDeTarjeta,
        shadowElevation = ElevacionTarjeta,
        modifier = modifier.fillMaxHeight(),
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    titulo.uppercase(),
                    style = Rotulo,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Icon(icono, contentDescription = null, tint = Acento, modifier = Modifier.size(16.dp))
            }
            Row(verticalAlignment = Alignment.Bottom) {
                Text(valor, style = DatoMedido.copy(fontSize = 24.sp, lineHeight = 28.sp))
                Spacer(Modifier.width(3.dp))
                Text(unidad, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary, modifier = Modifier.padding(bottom = 3.dp))
            }
            Spacer(Modifier.weight(1f))
            pie()
        }
    }
}

@Composable
internal fun TarjetaHistorial(
    resumen: ResumenJornada,
    desplegado: Boolean,
    onAlternar: () -> Unit,
    onVerTodo: () -> Unit,
) {
    Surface(
        shape = FormaTarjeta,
        color = MaterialTheme.colorScheme.surface,
        border = BordeDeTarjeta,
        shadowElevation = ElevacionTarjeta,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconoCuadrado(Icons.Filled.HistoryEdu, fondo = AcentoSuave)
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Historial de muestras", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "${resumen.totalMuestras} registros capturados hoy",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }
                TextButton(onClick = onVerTodo) {
                    Text("Ver todo", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, color = AcentoTexto)
                }
            }
            // Contadores en gris neutro para no saturar el resumen; se pliegan con la flecha de abajo
            AnimatedVisibility(visible = desplegado) {
                Row(
                    modifier = Modifier.padding(top = 12.dp).testTag(ETIQUETA_CONTADORES),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ContadorEstado(resumen.validadas, "VALIDADAS", SuperficieCampo, MaterialTheme.colorScheme.primary, Modifier.weight(1f))
                    ContadorEstado(resumen.pendientes, "PENDIENTES", SuperficieCampo, MaterialTheme.colorScheme.primary, Modifier.weight(1f))
                    ContadorEstado(resumen.corregidas, "CORREGIDAS", SuperficieCampo, MaterialTheme.colorScheme.primary, Modifier.weight(1f))
                }
            }
            // Flecha para mostrar u ocultar los contadores: toda la franja inferior responde al toque
            val giro by animateFloatAsState(if (desplegado) 180f else 0f, label = "flecha-historial")
            val accion = if (desplegado) "Ocultar los contadores del historial" else "Mostrar los contadores del historial"
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp)
                    .clickable(onClick = onAlternar)
                    .semantics {
                        role = Role.Button
                        contentDescription = accion
                        stateDescription = if (desplegado) "Desplegado" else "Plegado"
                    }
                    .testTag(ETIQUETA_FLECHA_HISTORIAL),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.KeyboardArrowDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(22.dp).rotate(giro),
                )
            }
        }
    }
}

@Composable
private fun ContadorEstado(cantidad: Int, etiqueta: String, fondo: Color, texto: Color, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(FormaCampo)
            .background(fondo)
            .border(1.dp, Linea, FormaCampo)
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("$cantidad", style = DatoMedido.copy(fontSize = 22.sp, lineHeight = 26.sp), color = texto)
        // Color completo para mantener el contraste mínimo (4,5:1)
        Text(etiqueta, style = Rotulo, color = MaterialTheme.colorScheme.secondary)
    }
}

@Composable
private fun IconoCuadrado(icono: ImageVector, fondo: Color) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(fondo),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icono, contentDescription = null, tint = AcentoTexto, modifier = Modifier.size(22.dp))
    }
}

@Composable
private fun MuestrasRecientes(muestras: List<Muestra>, ultimaRegistradaEn: Long?, onMuestraClick: (Muestra) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        EncabezadoSeccion("Muestras recientes", ultimaRegistradaEn?.let { "Última ${haceCuanto(it)}" })
        if (muestras.isEmpty()) {
            Text(
                "Aún no hay muestras registradas",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
        }
        muestras.forEach { muestra -> FilaMuestra(muestra, onClick = { onMuestraClick(muestra) }) }
    }
}

@Composable
private fun FilaMuestra(muestra: Muestra, onClick: () -> Unit) {
    Surface(
        shape = FormaTarjetaChica,
        color = MaterialTheme.colorScheme.surface,
        border = BordeDeTarjeta,
        shadowElevation = ElevacionTarjeta,
        modifier = Modifier
            .fillMaxWidth()
            .clip(FormaTarjetaChica)
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier
                .heightIn(min = 64.dp)
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainer),
                contentAlignment = Alignment.Center,
            ) {
                Text(muestra.codigoLinea, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Línea %02d • Tren %s".format(muestra.linea, muestra.tren),
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    // La observación es opcional: si viene vacía no se agrega el último separador
                    listOfNotNull(
                        "Prof. ${muestra.profundidadM} m",
                        "${muestra.tallaMm} mm",
                        "${muestra.conteoIndM} ind/m",
                        muestra.observacion.takeIf { it.isNotBlank() },
                    ).joinToString(" • "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                ChipEstado(muestra.estado)
                Text(
                    Fechas.relativa(muestra.registradaEn),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
        }
    }
}

private fun haceCuanto(instante: Long, ahora: Long = System.currentTimeMillis()): String {
    val minutos = ((ahora - instante) / 60_000).coerceAtLeast(0)
    return when {
        minutos < 1 -> "hace instantes"
        minutos < 60 -> "hace $minutos min"
        minutos < 24 * 60 -> "hace ${minutos / 60} h"
        minutos < 2 * 24 * 60 -> "hace 1 día"
        else -> "hace ${minutos / (24 * 60)} días"
    }
}

@Preview(showBackground = true, heightDp = 1600)
@Composable
private fun PrincipalOperarioPreview() {
    val repositorio = remember { FakeMuestraRepository() }
    val muestras by repositorio.muestras.collectAsState()
    MusselAppTheme {
        PrincipalScreen(
            usuario = Usuario("11.111.111-1", "Carla Vera", Rol.OPERARIO),
            centro = CentroCultivo.disponibles.first(),
            uiState = PrincipalUiState(
                resumen = ResumenJornada.desde(muestras),
                recientes = muestras.take(3),
            ),
            onSincronizar = {},
            onCerrarSesion = {},
            onNavegar = {},
            onVerMuestra = {},
            onMensajeMostrado = {},
        )
    }
}

/** Etiquetas del cuadro del historial, para las pruebas de interfaz. */
internal const val ETIQUETA_CONTADORES = "historial-contadores"
internal const val ETIQUETA_FLECHA_HISTORIAL = "historial-flecha"
