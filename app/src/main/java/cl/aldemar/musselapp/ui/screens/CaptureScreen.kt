package cl.aldemar.musselapp.ui.screens

import android.Manifest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import cl.aldemar.musselapp.util.Fotos
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.IconButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.produceState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.core.app.ActivityCompat
import android.provider.Settings
import android.content.Intent
import android.content.ContentValues
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.view.OrientationEventListener
import android.view.Surface
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview as CameraPreview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import cl.aldemar.musselapp.ui.muestra.Paleta
import cl.aldemar.musselapp.ui.componentes.VisorFoto
import cl.aldemar.musselapp.ui.theme.MusselAppTheme
import cl.aldemar.musselapp.util.AlmacenFotos
import java.io.File

// Colores del sistema de diseño (DESIGN.md)
private val Navy = Color(0xFF0F172A)
private val Fondo = Color(0xFFF1F5F9)
private val GrisTexto = Color(0xFF94A3B8)

/**
 * Paso 2 del registro: fotografía de la muestra.
 *
 * @param fotoExistente foto ya confirmada del borrador (al volver desde la Vista previa):
 * se muestra de inmediato con la opción de continuar, en vez de obligar a sacar otra.
 */
@Composable
fun CaptureScreen(
    fotoExistente: String? = null,
    onVolver: () -> Unit = {},
    onFotoLista: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val alcance = rememberCoroutineScope()

    var tienePermiso by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                    PackageManager.PERMISSION_GRANTED
        )
    }
    // El sistema ya no muestra el diálogo: solo se puede conceder desde los Ajustes
    var permisoBloqueado by remember { mutableStateOf(false) }

    val pedirPermiso = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { concedido ->
        tienePermiso = concedido
        val actividad = context.buscarActividad()
        permisoBloqueado = !concedido && actividad != null &&
            !ActivityCompat.shouldShowRequestPermissionRationale(actividad, Manifest.permission.CAMERA)
    }

    // Al volver de los Ajustes del sistema se revisa de nuevo el permiso
    LifecycleResumeEffect(Unit) {
        val concedido = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED
        tienePermiso = concedido
        if (concedido) permisoBloqueado = false
        onPauseOrDispose { }
    }

    LaunchedEffect(Unit) {
        // Si ya hay una foto confirmada no se interrumpe con el diálogo de permiso
        if (!tienePermiso && fotoExistente == null) pedirPermiso.launch(Manifest.permission.CAMERA)
    }

    val imageCapture = remember {
        ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .build()
    }

    var fotoArchivo by remember { mutableStateOf(fotoExistente?.let(::File)) }
    var capturando by remember { mutableStateOf(false) }
    // Copiar, decodificar y guardar fotos de varios MB se hace fuera del hilo principal
    var procesando by remember { mutableStateOf(false) }
    val fotoCargada by produceState<Bitmap?>(null, fotoArchivo) {
        value = fotoArchivo?.let { withContext(Dispatchers.IO) { Fotos.decodificar(it.absolutePath, ladoMaxPx = 1280) } }
    }
    val foto = fotoCargada
    var estadoFoto by remember {
        mutableStateOf(if (fotoExistente != null) EstadoFoto.GUARDADA else EstadoFoto.SIN_FOTO)
    }
    // Volver desde la Vista previa: la foto ya confirmada se puede reutilizar tal cual
    val reutilizando = fotoExistente != null && fotoArchivo?.absolutePath == fotoExistente
    var origenCamara by remember { mutableStateOf(false) }
    var camaraAbierta by remember { mutableStateOf(false) }
    var verFoto by remember { mutableStateOf(false) }
    var animando by remember { mutableStateOf(false) } // el botón se va como bote hacia la Vista previa
    var rutaSiguiente by remember { mutableStateOf<String?>(null) }
    val progresoBote = remember { Animatable(0f) }

    // Foto nueva (cámara o galería): queda PENDIENTE hasta que el operario la confirme
    fun alRecibirFoto(archivo: File, desdeCamara: Boolean) {
        if (estadoFoto == EstadoFoto.REVISAR) fotoArchivo?.delete()
        fotoArchivo = archivo
        origenCamara = desdeCamara
        capturando = false
        camaraAbierta = false
        estadoFoto = EstadoFoto.REVISAR
    }

    // Descarta la foto pendiente (si no se confirmó, no se guarda) y vuelve a la cámara
    fun volverACamara() {
        if (estadoFoto == EstadoFoto.REVISAR) fotoArchivo?.delete()
        fotoArchivo = null
        estadoFoto = EstadoFoto.SIN_FOTO
    }

    // Una foto tomada pero no confirmada no queda olvidada en la carpeta temporal al salir
    DisposableEffect(Unit) {
        onDispose { if (estadoFoto == EstadoFoto.REVISAR) fotoArchivo?.delete() }
    }

    // El operario confirma que la foto salió bien: recién ahí se guarda
    fun confirmarFoto() {
        val temporal = fotoArchivo ?: return
        if (procesando) return
        procesando = true
        alcance.launch {
            // La foto confirmada pasa a la carpeta privada de la app (filesDir/fotos)
            val destino = try {
                withContext(Dispatchers.IO) { AlmacenFotos.guardar(context, temporal) }
            } catch (e: Exception) {
                procesando = false
                Toast.makeText(context, "No se pudo guardar la foto", Toast.LENGTH_SHORT).show()
                return@launch
            }
            fotoArchivo = destino

            // Las fotos de la cámara también van a la galería del teléfono
            val enGaleria = origenCamara && withContext(Dispatchers.IO) { guardarEnGaleria(context, destino) }
            estadoFoto = if (!origenCamara || enGaleria) EstadoFoto.GUARDADA else EstadoFoto.SOLO_APP
            val mensaje = when {
                !origenCamara -> "Foto guardada en la app"
                enGaleria -> "Foto guardada en la galería y en la app"
                else -> "Foto guardada solo en la app"
            }
            Toast.makeText(context, mensaje, Toast.LENGTH_SHORT).show()
            procesando = false

            // Con la foto confirmada, el botón se expande suavemente y pasa a la Vista previa
            rutaSiguiente = destino.absolutePath
            animando = true
        }
    }

    // Sigue con la foto que ya estaba confirmada, sin copiarla de nuevo
    fun continuarConFoto() {
        val ruta = fotoArchivo?.absolutePath ?: return
        rutaSiguiente = ruta
        animando = true
    }

    // Navega cuando termina la animación del botón
    LaunchedEffect(animando) {
        if (!animando) return@LaunchedEffect
        progresoBote.animateTo(1f, tween(durationMillis = 650, easing = FastOutSlowInEasing))
        rutaSiguiente?.let(onFotoLista)
    }

    // Abre la cámara a pantalla completa (pide el permiso si aún no lo tiene)
    fun abrirCamara() {
        when {
            tienePermiso -> camaraAbierta = true
            permisoBloqueado -> abrirAjustesApp(context)
            else -> pedirPermiso.launch(Manifest.permission.CAMERA)
        }
    }

    val selectorGaleria = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            procesando = true
            alcance.launch {
                val archivo = withContext(Dispatchers.IO) { copiarDesdeGaleria(context, uri) }
                procesando = false
                if (archivo != null) {
                    alRecibirFoto(archivo, desdeCamara = false)
                } else {
                    Toast.makeText(context, "No se pudo abrir la imagen", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun tomarNuevaFoto() {
        if (capturando) return
        capturando = true
        tomarFoto(
            context = context,
            imageCapture = imageCapture,
            onGuardada = { archivo -> alRecibirFoto(archivo, desdeCamara = true) },
            onError = { capturando = false }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Paleta.Fondo)
    ) {
        BarraSuperior(onVolver = onVolver)

        // Zona de cámara
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clipToBounds()
                .background(Color.Black)
        ) {
            when {
                foto != null -> {
                    Image(
                        bitmap = foto.asImageBitmap(),
                        contentDescription = "Foto de la muestra. Toca para ampliar",
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable { verFoto = true },
                        contentScale = ContentScale.Fit
                    )
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(10.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Navy)
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ZoomIn,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Text("Toca para ampliar", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
                fotoArchivo != null || procesando -> CircularProgressIndicator(
                    color = Color.White,
                    modifier = Modifier.align(Alignment.Center)
                )
                else -> ZonaSinFoto(
                    tienePermiso = tienePermiso,
                    permisoBloqueado = permisoBloqueado,
                    onAbrirAjustes = { abrirAjustesApp(context) }
                )
            }
        }

        // Panel inferior
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TarjetaAlineacion()

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (estadoFoto == EstadoFoto.REVISAR || animando || reutilizando) {
                    BotonLateral(Icons.Filled.Refresh, "Repetir", false) {
                        if (animando || procesando) return@BotonLateral
                        volverACamara()
                        abrirCamara()
                    }
                } else {
                    BotonLateral(Icons.Filled.PhotoLibrary, "Galería", false) {
                        selectorGaleria.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }
                }
                Obturador(
                    icono = when {
                        reutilizando -> Icons.AutoMirrored.Filled.ArrowForward
                        estadoFoto == EstadoFoto.REVISAR || animando -> Icons.Filled.Check
                        else -> Icons.Filled.PhotoCamera
                    },
                    descripcion = when {
                        reutilizando -> "Continuar con esta foto"
                        estadoFoto == EstadoFoto.REVISAR -> "Foto buena, guardar y continuar"
                        estadoFoto == EstadoFoto.SIN_FOTO -> "Tomar foto"
                        else -> "Tomar otra foto"
                    },
                    habilitado = !procesando,
                    respirar = estadoFoto == EstadoFoto.SIN_FOTO,
                    navegar = progresoBote.value,
                    onClick = {
                        if (animando || procesando) return@Obturador
                        when {
                            reutilizando -> continuarConFoto()
                            estadoFoto == EstadoFoto.SIN_FOTO -> abrirCamara()
                            estadoFoto == EstadoFoto.REVISAR -> confirmarFoto()
                            else -> {
                                volverACamara()
                                abrirCamara()
                            }
                        }
                    }
                )
                // Espacio del tamaño de un botón lateral, para que el obturador quede centrado
                Spacer(modifier = Modifier.size(56.dp))
            }
        }
    }

    val rutaVisor = fotoArchivo?.absolutePath
    if (verFoto && rutaVisor != null) {
        VisorFoto(ruta = rutaVisor, onCerrar = { verFoto = false })
    }

    if (camaraAbierta) {
        CamaraPantallaCompleta(
            imageCapture = imageCapture,
            capturando = capturando,
            onTomar = { tomarNuevaFoto() },
            onGaleria = {
                camaraAbierta = false
                selectorGaleria.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            },
            onCerrar = { camaraAbierta = false }
        )
    }
    }
}

// Zona que se ve antes de tomar la foto: la cámara se abre a pantalla completa con el obturador
@Composable
private fun ZonaSinFoto(
    tienePermiso: Boolean,
    permisoBloqueado: Boolean = false,
    onAbrirAjustes: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Navy)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Filled.PhotoCamera,
            contentDescription = null,
            tint = GrisTexto,
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Foto de la muestra",
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = when {
                tienePermiso ->
                    "Toca el botón de la cámara para abrirla a pantalla completa. Puedes tomar la foto en vertical u horizontal."
                permisoBloqueado ->
                    "El permiso de cámara está desactivado. Actívalo en los Ajustes o elige una foto desde la Galería."
                else -> "Se necesita permiso de cámara. Toca el botón para concederlo o usa la Galería."
            },
            color = GrisTexto,
            fontSize = 13.sp,
            textAlign = TextAlign.Center
        )
        if (permisoBloqueado) {
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedButton(
                onClick = onAbrirAjustes,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
            ) {
                Icon(Icons.Filled.Settings, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Abrir ajustes", fontWeight = FontWeight.Bold)
            }
        }
    }
}

// Cámara a pantalla completa con los botones integrados; gira con el teléfono (vertical u horizontal)
@Composable
private fun CamaraPantallaCompleta(
    imageCapture: ImageCapture,
    capturando: Boolean,
    onTomar: () -> Unit,
    onGaleria: () -> Unit,
    onCerrar: () -> Unit
) {
    val context = LocalContext.current
    val view = LocalView.current
    val horizontal = LocalConfiguration.current.orientation ==
            android.content.res.Configuration.ORIENTATION_LANDSCAPE

    BackHandler(onBack = onCerrar)

    // Mientras la cámara está abierta: permite girar la pantalla y oculta las barras del sistema
    DisposableEffect(Unit) {
        val actividad = context.buscarActividad()
        val orientacionPrevia = actividad?.requestedOrientation
        actividad?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR

        val controlador = actividad?.let { WindowCompat.getInsetsController(it.window, view) }
        controlador?.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controlador?.hide(WindowInsetsCompat.Type.systemBars())

        onDispose {
            controlador?.show(WindowInsetsCompat.Type.systemBars())
            if (actividad != null && orientacionPrevia != null) {
                actividad.requestedOrientation = orientacionPrevia
            }
        }
    }

    // La foto sale con la orientación en que el operario sostiene el teléfono
    DisposableEffect(Unit) {
        val escucha = object : OrientationEventListener(context) {
            override fun onOrientationChanged(grados: Int) {
                if (grados == OrientationEventListener.ORIENTATION_UNKNOWN) return
                imageCapture.targetRotation = when (grados) {
                    in 45..134 -> Surface.ROTATION_270
                    in 135..224 -> Surface.ROTATION_180
                    in 225..314 -> Surface.ROTATION_90
                    else -> Surface.ROTATION_0
                }
            }
        }
        escucha.enable()
        onDispose { escucha.disable() }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        VistaPreviaCamara(
            imageCapture = imageCapture,
            modifier = Modifier.fillMaxSize()
        )
        EsquinasVisor(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth(if (horizontal) 0.5f else 0.7f)
                .fillMaxHeight(if (horizontal) 0.6f else 0.4f)
        )

        // Botones integrados sobre la imagen
        if (horizontal) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .safeDrawingPadding()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                BotonCamara(Icons.Filled.Close, "Cerrar cámara", onCerrar)
                Column(
                    modifier = Modifier.fillMaxHeight(),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.size(52.dp))
                    Obturador(
                        icono = Icons.Filled.PhotoCamera,
                        descripcion = "Tomar foto",
                        habilitado = !capturando,
                        respirar = false,
                        onClick = onTomar
                    )
                    BotonCamara(Icons.Filled.PhotoLibrary, "Galería", onGaleria)
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .safeDrawingPadding()
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    BotonCamara(Icons.Filled.Close, "Cerrar cámara", onCerrar)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BotonCamara(Icons.Filled.PhotoLibrary, "Galería", onGaleria)
                    Obturador(
                        icono = Icons.Filled.PhotoCamera,
                        descripcion = "Tomar foto",
                        habilitado = !capturando,
                        respirar = false,
                        onClick = onTomar
                    )
                    Spacer(modifier = Modifier.size(52.dp))
                }
            }
        }
    }
}

// Botón redondo semitransparente para usar sobre la cámara
@Composable
private fun BotonCamara(icono: ImageVector, descripcion: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(52.dp)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.55f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icono,
            contentDescription = descripcion,
            tint = Color.White
        )
    }
}

private tailrec fun Context.buscarActividad(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.buscarActividad()
    else -> null
}

@Composable
private fun BarraSuperior(onVolver: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Navy)
            .statusBarsPadding()
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Igual que en Muestra y Vista previa: se puede volver al paso anterior
        IconButton(onClick = onVolver) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = Color.White)
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "MusselApp • Nueva muestra",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Paso 2 · Fotografía de la muestra",
                color = GrisTexto,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// Esquinas blancas del visor.
// TODO: chips flotantes (flash, grilla, etc.) si se requieren
@Composable
private fun EsquinasVisor(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val largo = 28.dp.toPx()
        val grosor = 3.dp.toPx()
        val color = Color.White
        val ancho = size.width
        val alto = size.height

        // arriba izquierda
        drawLine(color, Offset(0f, 0f), Offset(largo, 0f), grosor)
        drawLine(color, Offset(0f, 0f), Offset(0f, largo), grosor)
        // arriba derecha
        drawLine(color, Offset(ancho, 0f), Offset(ancho - largo, 0f), grosor)
        drawLine(color, Offset(ancho, 0f), Offset(ancho, largo), grosor)
        // abajo izquierda
        drawLine(color, Offset(0f, alto), Offset(largo, alto), grosor)
        drawLine(color, Offset(0f, alto), Offset(0f, alto - largo), grosor)
        // abajo derecha
        drawLine(color, Offset(ancho, alto), Offset(ancho - largo, alto), grosor)
        drawLine(color, Offset(ancho, alto), Offset(ancho, alto - largo), grosor)
    }
}

@Composable
private fun TarjetaAlineacion() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Paleta.Superficie)
            .border(1.5.dp, Paleta.Borde, RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Paleta.AcentoSuave),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Straighten,
                contentDescription = null,
                tint = Paleta.Acento
            )
        }
        Column {
            Text(
                text = "Alineación de Muestra",
                color = Paleta.Texto,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Mantenga la regla calibrada paralela a la línea de choritos para una foto clara.",
                color = Paleta.TextoSuave,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun BotonLateral(
    icono: ImageVector,
    texto: String,
    seleccionado: Boolean,
    onClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(if (seleccionado) Paleta.Borde else Paleta.Campo)
                .border(1.5.dp, Paleta.BordeCampo, RoundedCornerShape(12.dp))
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icono,
                contentDescription = texto,
                tint = if (seleccionado) Color.White else Paleta.Etiqueta
            )
        }
        Text(
            text = texto,
            color = Paleta.Etiqueta,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

private enum class EstadoFoto { SIN_FOTO, REVISAR, GUARDADA, SOLO_APP }

// Botón principal: "respira" esperando la foto, se encoge al presionarlo y, al confirmar,
// libera un aro suave que se expande y se disuelve (como un respiro) antes de pasar de pantalla
@Composable
private fun Obturador(
    icono: ImageVector,
    descripcion: String,
    habilitado: Boolean,
    respirar: Boolean,
    navegar: Float = 0f, // 0 = quieto; hacia 1 el botón se expande y se desvanece
    onClick: () -> Unit
) {
    val interaccion = remember { MutableInteractionSource() }
    val presionado by interaccion.collectIsPressedAsState()

    val escalaPresion by animateFloatAsState(
        targetValue = if (presionado) 0.88f else 1f,
        animationSpec = tween(durationMillis = 120),
        label = "presion"
    )
    val respiracion by rememberInfiniteTransition(label = "respiracion").animateFloat(
        initialValue = 1f,
        targetValue = 1.07f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1300, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "escala"
    )
    val escalaBase = escalaPresion * (if (respirar && habilitado && navegar == 0f) respiracion else 1f)
    // Al confirmar: una leve contracción y luego se expande disolviéndose
    val escala = if (navegar <= 0f) escalaBase else {
        if (navegar < 0.25f) 1f - 0.08f * (navegar / 0.25f)
        else 0.92f + 0.4f * ((navegar - 0.25f) / 0.75f)
    }
    val opacidad = if (navegar < 0.4f) 1f else (1f - (navegar - 0.4f) / 0.6f).coerceIn(0f, 1f)
    val progresoAro = ((navegar - 0.15f) / 0.85f).coerceIn(0f, 1f)

    Box(modifier = Modifier.size(76.dp), contentAlignment = Alignment.Center) {
        if (navegar > 0f) {
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .graphicsLayer {
                        val e = 1f + 1.1f * progresoAro
                        scaleX = e
                        scaleY = e
                        alpha = 0.45f * (1f - progresoAro)
                    }
                    .border(2.dp, Navy, CircleShape)
            )
        }
        Box(
            modifier = Modifier
                .graphicsLayer {
                    scaleX = escala
                    scaleY = escala
                    alpha = opacidad
                }
                .size(76.dp)
                .clip(CircleShape)
                .background(Navy)
                .padding(4.dp)
                .clip(CircleShape)
                .background(Color.White)
                .padding(5.dp)
                .clip(CircleShape)
                .background(if (habilitado) Navy else Paleta.TextoSuave)
                .clickable(
                    interactionSource = interaccion,
                    indication = null,
                    enabled = habilitado,
                    onClick = onClick
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icono,
                contentDescription = descripcion,
                tint = Color.White,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

@Composable
private fun VistaPreviaCamara(
    imageCapture: ImageCapture,
    modifier: Modifier = Modifier
) {
    val lifecycleOwner = LocalLifecycleOwner.current

    // En el panel Preview de Android Studio no hay cámara real
    if (LocalInspectionMode.current) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text("Vista previa de cámara", color = Color.White)
        }
        return
    }

    val contexto = LocalContext.current
    DisposableEffect(Unit) {
        onDispose {
            runCatching { ProcessCameraProvider.getInstance(contexto).get().unbindAll() }
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            val previewView = PreviewView(ctx).apply {
                scaleType = PreviewView.ScaleType.FIT_CENTER
            }
            val futuro = ProcessCameraProvider.getInstance(ctx)

            futuro.addListener({
                val proveedor = futuro.get()
                val vistaPrevia = CameraPreview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

                proveedor.unbindAll()
                proveedor.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    vistaPrevia,
                    imageCapture
                )
            }, ContextCompat.getMainExecutor(ctx))

            previewView
        }
    )
}

// Archivo temporal: la foto solo pasa a la carpeta de la app si el operario la confirma
private fun nuevoArchivoTemporal(context: Context): File {
    val carpeta = File(context.cacheDir, "pendientes")
    carpeta.mkdirs()
    return File(carpeta, "pendiente_${System.currentTimeMillis()}.jpg")
}

private fun tomarFoto(
    context: Context,
    imageCapture: ImageCapture,
    onGuardada: (File) -> Unit,
    onError: () -> Unit
) {
    val archivo = nuevoArchivoTemporal(context)
    val opciones = ImageCapture.OutputFileOptions.Builder(archivo).build()

    imageCapture.takePicture(
        opciones,
        ContextCompat.getMainExecutor(context),
        object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                onGuardada(archivo)
            }

            override fun onError(exception: ImageCaptureException) {
                Toast.makeText(context, "No se pudo tomar la foto", Toast.LENGTH_SHORT).show()
                onError()
            }
        }
    )
}

// Guarda una copia en la galería del teléfono (Pictures/MusselApp).
// En Android 10+ no necesita permisos; en versiones anteriores se omite.
private fun guardarEnGaleria(context: Context, archivo: File): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return false
    return try {
        val valores = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, archivo.name)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/MusselApp")
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, valores)
            ?: return false

        resolver.openOutputStream(uri)?.use { salida ->
            archivo.inputStream().use { entrada -> entrada.copyTo(salida) }
        } ?: return false

        valores.clear()
        valores.put(MediaStore.Images.Media.IS_PENDING, 0)
        resolver.update(uri, valores, null, null)
        true
    } catch (e: Exception) {
        false
    }
}

// Copia la imagen elegida en la galería a la carpeta de la app
private fun copiarDesdeGaleria(context: Context, uri: Uri): File? {
    return try {
        val archivo = nuevoArchivoTemporal(context)
        context.contentResolver.openInputStream(uri)?.use { entrada ->
            archivo.outputStream().use { salida -> entrada.copyTo(salida) }
        } ?: return null
        archivo
    } catch (e: Exception) {
        null
    }
}

// Abre la ficha de la app en los Ajustes del sistema para conceder el permiso de cámara
private fun abrirAjustesApp(context: Context) {
    val intent = Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.fromParts("package", context.packageName, null)
    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    try {
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "No se pudieron abrir los Ajustes", Toast.LENGTH_SHORT).show()
    }
}

@Preview(showBackground = true)
@Composable
fun CaptureScreenPreview() {
    MusselAppTheme {
        CaptureScreen()
    }
}
