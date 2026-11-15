package cl.aldemar.musselapp.ui.muestra

import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.Role
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.produceState
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import cl.aldemar.musselapp.util.Fotos
import cl.aldemar.musselapp.ui.componentes.VisorFoto
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Anchor
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsBoat
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RotateLeft
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cl.aldemar.musselapp.data.model.CentroCultivo
import cl.aldemar.musselapp.data.model.EstadoMuestra
import cl.aldemar.musselapp.ui.theme.MusselAppTheme
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.focus.onFocusChanged
import cl.aldemar.musselapp.ui.theme.BordeDeTarjeta
import cl.aldemar.musselapp.ui.theme.DatoMedido
import cl.aldemar.musselapp.ui.theme.FormaCampo
import cl.aldemar.musselapp.ui.theme.FormaTarjetaChica
import cl.aldemar.musselapp.ui.theme.Rotulo
import cl.aldemar.musselapp.ui.theme.tarjetaMussel
import cl.aldemar.musselapp.ui.theme.colores
import cl.aldemar.musselapp.util.AlmacenFotos
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Estado "Pendiente": mismos colores que su chip en el resto de la app (ColoresEstado)
private val PendienteFondo = EstadoMuestra.PENDIENTE.colores.fondo
private val PendienteTexto = EstadoMuestra.PENDIENTE.colores.texto

private const val MAX_OBSERVACION = 300

/**
 * Última pantalla del operario: revisa la foto y los datos, los corrige si hace falta
 * y confirma el muestreo.
 */
@Composable
fun VistaPreviaScreen(
    borrador: BorradorMuestra,
    onVolver: () -> Unit = {},
    onRepetirFoto: () -> Unit = {},
    onFotoCambiada: (String?) -> Unit = {},
    onParametrosCambiados: (ParametrosMuestra) -> Unit = {},
    onObservacionCambiada: (String) -> Unit = {},
    onConteoCambiado: (Int?) -> Unit = {},
    onTallaCambiada: (Int?) -> Unit = {},
    onCholgasCambiadas: (Int?) -> Unit = {},
    guardando: Boolean = false,
    onConfirmar: () -> Unit = {},
) {
    val context = LocalContext.current
    val focoActual = LocalFocusManager.current
    val teclado = LocalSoftwareKeyboardController.current

    // Cierra el teclado y quita el foco del cuadro de notas; lo escrito ya queda guardado
    val cerrarTeclado = {
        focoActual.clearFocus()
        teclado?.hide()
    }

    var confirmando by remember { mutableStateOf(false) }
    var eliminando by remember { mutableStateOf(false) }
    var verFoto by remember { mutableStateOf(false) }
    var editando by remember { mutableStateOf(false) }

    val parametros = borrador.parametros
    val rutaFoto = borrador.fotoRuta
    // Se puede guardar con parámetros, foto, conteo y talla válidos (reglas en validarBorrador)
    val faltante = validarBorrador(borrador)
    val listo = faltante == null && !guardando

    // Elegir otra foto desde la galería del teléfono
    val selectorGaleria = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val archivo = AlmacenFotos.copiarDesdeUri(context, uri)
            if (archivo != null) {
                AlmacenFotos.borrar(context, rutaFoto)
                onFotoCambiada(archivo.absolutePath)
            } else {
                Toast.makeText(context, "No se pudo abrir la imagen", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Paleta.Fondo)
            .imePadding()
            .pointerInput(Unit) { detectTapGestures(onTap = { cerrarTeclado() }) }
    ) {
        BarraFlujo(titulo = "Vista previa", rotulo = "MusselApp • Operario", paso = 3, onVolver = onVolver)

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Estado de la muestra
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextoRotulo("Nueva muestra", modifier = Modifier.weight(1f))
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(PendienteFondo)
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(Modifier.size(6.dp).clip(CircleShape).background(PendienteTexto))
                    Text(
                        text = "Pendiente envío",
                        color = PendienteTexto,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Fotografía: se toca para verla completa
            TarjetaFoto(
                rutaFoto = rutaFoto,
                onAmpliar = { verFoto = true },
                onRepetir = onRepetirFoto,
                onGaleria = {
                    selectorGaleria.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                onEliminar = { eliminando = true }
            )

            // Métricas medidas por el operario: obligatorias para guardar la muestra.
            // Las tres en una línea y del mismo ancho (conteo de choritos, talla y cholgas)
            Row(
                modifier = Modifier.height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TarjetaMetrica(
                    titulo = "Conteo",
                    valor = borrador.conteoIndM,
                    unidad = "uds / m",
                    error = errorMedicion(borrador.conteoIndM, CONTEO_MAXIMO_IND_M),
                    onCambiar = onConteoCambiado,
                    modifier = Modifier.weight(1f).fillMaxHeight()
                )
                TarjetaMetrica(
                    titulo = "Talla media",
                    valor = borrador.tallaMm,
                    unidad = "mm",
                    error = errorMedicion(borrador.tallaMm, TALLA_MAXIMA_MM),
                    onCambiar = onTallaCambiada,
                    modifier = Modifier.weight(1f).fillMaxHeight()
                )
                // Cholgas: otra especie de la misma familia; se cuentan en el mismo tramo y se anotan aparte
                TarjetaMetrica(
                    titulo = "Cholgas",
                    valor = borrador.cholgasIndM,
                    unidad = "uds / m",
                    error = errorMedicion(borrador.cholgasIndM, CONTEO_MAXIMO_IND_M, minimo = 0),
                    onCambiar = onCholgasCambiadas,
                    modifier = Modifier.weight(1f).fillMaxHeight()
                )
            }
            Text(
                text = "Cholgas: se anotan aparte de los choritos · 0 si no hay",
                color = Paleta.TextoSuave,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(horizontal = 2.dp)
            )

            // Ubicación: lectura o edición en la misma pantalla
            Tarjeta(espacio = 10.dp) {
                TituloTarjeta("Ubicación y muestreo", Icons.Filled.Anchor) {
                    if (parametros != null) {
                        PildoraAccion(
                            texto = if (editando) "Listo" else "Editar",
                            icono = if (editando) Icons.Filled.Check else Icons.Filled.Edit,
                            activa = editando,
                            onClick = { editando = !editando }
                        )
                    }
                }

                if (parametros == null) {
                    Text(
                        "Completa el formulario de Nueva muestra.",
                        color = Paleta.TextoSuave,
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else if (editando) {
                    Selector(
                        etiqueta = "Centro de cultivo",
                        valor = CentroCultivo.disponibles.firstOrNull { it.id == parametros.centroId }?.nombre,
                        vacio = "Selecciona un centro",
                        icono = Icons.Filled.Business,
                        opciones = CentroCultivo.disponibles.map { it.nombre },
                        onElegir = { i ->
                            onParametrosCambiados(parametros.copy(centroId = CentroCultivo.disponibles[i].id))
                        }
                    )
                    Selector(
                        etiqueta = "Tren de cultivo (longline)",
                        valor = nombreTren(parametros.tren),
                        vacio = "Selecciona un tren",
                        icono = Icons.Filled.DirectionsBoat,
                        opciones = Trenes.map { it.second },
                        onElegir = { i -> onParametrosCambiados(parametros.copy(tren = Trenes[i].first)) }
                    )
                    Selector(
                        etiqueta = "Línea de siembra",
                        valor = etiquetaLinea(parametros.linea),
                        vacio = "Selecciona una línea",
                        icono = Icons.Filled.Anchor,
                        opciones = Lineas.map { etiquetaLinea(it) },
                        onElegir = { i -> onParametrosCambiados(parametros.copy(linea = Lineas[i])) }
                    )
                    TextoRotulo("Estrato de profundidad", modifier = Modifier.padding(top = 4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Estrato.entries.forEach { e ->
                            OpcionEstrato(
                                estrato = e,
                                seleccionado = Estrato.desde(parametros.profundidadM) == e,
                                modifier = Modifier.weight(1f),
                                onClick = { onParametrosCambiados(parametros.copy(profundidadM = e.profundidadM)) }
                            )
                        }
                    }
                } else {
                    val centro = CentroCultivo.disponibles.firstOrNull { it.id == parametros.centroId }
                    val estrato = Estrato.desde(parametros.profundidadM)
                    Column {
                        Fila("Centro", centro?.nombre ?: "—")
                        Fila("Tren", nombreTren(parametros.tren))
                        Fila("Línea", "Línea %02d".format(parametros.linea))
                        Fila("Profundidad", "Estrato ${estrato.rango} (${estrato.nombre})")
                    }
                }
            }

            // Notas del operador (editables)
            Tarjeta(espacio = 10.dp) {
                TituloTarjeta("Notas del operador en terreno")
                OutlinedTextField(
                    value = borrador.observacion,
                    onValueChange = { onObservacionCambiada(it.take(MAX_OBSERVACION)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 96.dp),
                    placeholder = {
                        Text("Escribe una observación de la muestra…", color = Paleta.TextoSuave)
                    },
                    textStyle = MaterialTheme.typography.bodyLarge,
                    minLines = 3,
                    maxLines = 6,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    shape = FormaCampo,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Paleta.Texto,
                        unfocusedTextColor = Paleta.Texto,
                        focusedBorderColor = Paleta.Acento,
                        unfocusedBorderColor = Paleta.BordeCampo,
                        cursorColor = Paleta.Acento,
                        focusedContainerColor = Paleta.Superficie,
                        unfocusedContainerColor = Paleta.Superficie,
                    ),
                    supportingText = {
                        Text(
                            text = "${borrador.observacion.length}/$MAX_OBSERVACION",
                            color = Paleta.TextoSuave,
                            style = Rotulo,
                            textAlign = TextAlign.End,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                )
            }
        }

        // Acción principal, siempre visible abajo
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Paleta.Pie)
                .border(width = 1.dp, color = Paleta.Linea)
                .navigationBarsPadding()
                .padding(16.dp)
        ) {
            BotonPrincipal(
                texto = if (guardando) "Guardando…" else "Guardar muestra",
                iconoInicio = Icons.Filled.Check,
                habilitado = listo,
                onClick = {
                    cerrarTeclado()
                    confirmando = true
                }
            )
            // Explica por qué el botón está deshabilitado
            if (faltante != null) {
                Text(
                    text = faltante,
                    color = Paleta.TextoSuave,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                )
            }
        }
    }

    if (verFoto && rutaFoto != null) {
        VisorFoto(ruta = rutaFoto, onCerrar = { verFoto = false })
    }

    if (eliminando) {
        AlertDialog(
            onDismissRequest = { eliminando = false },
            containerColor = Paleta.Superficie,
            titleContentColor = Paleta.Texto,
            textContentColor = Paleta.TextoSuave,
            title = { Text("¿Eliminar la foto?", style = MaterialTheme.typography.titleLarge) },
            text = { Text("Se quitará la foto de esta muestra. Podrás tomar otra o elegir una de la galería.") },
            confirmButton = {
                TextButton(onClick = {
                    eliminando = false
                    AlmacenFotos.borrar(context, rutaFoto)
                    onFotoCambiada(null)
                }) { Text("Eliminar", color = Paleta.Error, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold) }
            },
            dismissButton = {
                TextButton(onClick = { eliminando = false }) {
                    Text("Cancelar", color = Paleta.TextoSuave, style = MaterialTheme.typography.labelLarge)
                }
            }
        )
    }

    if (confirmando) {
        AlertDialog(
            onDismissRequest = { confirmando = false },
            containerColor = Paleta.Superficie,
            titleContentColor = Paleta.Texto,
            textContentColor = Paleta.TextoSuave,
            title = { Text("¿Confirmar muestreo?", style = MaterialTheme.typography.titleLarge) },
            text = { Text("Revisaste la fotografía y los datos. Se guardará la muestra en este dispositivo.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmando = false
                    onConfirmar()
                }) { Text("Confirmar", color = Paleta.AcentoTexto, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold) }
            },
            dismissButton = {
                TextButton(onClick = { confirmando = false }) {
                    Text("Cancelar", color = Paleta.TextoSuave, style = MaterialTheme.typography.labelLarge)
                }
            }
        )
    }
}

// Foto + acciones en orden: Repetir (cámara), Galería, Eliminar
@Composable
private fun TarjetaFoto(
    rutaFoto: String?,
    onAmpliar: () -> Unit,
    onRepetir: () -> Unit,
    onGaleria: () -> Unit,
    onEliminar: () -> Unit
) {
    // Se lee fuera del hilo principal: decodificar varios MB aquí trababa la apertura de la pantalla
    val bitmapCargado by produceState<Bitmap?>(null, rutaFoto) {
        value = rutaFoto?.let { withContext(Dispatchers.IO) { Fotos.decodificar(it, ladoMaxPx = 1024) } }
    }
    val hora = remember(rutaFoto) {
        rutaFoto?.let {
            SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(File(it).lastModified()))
        }
    }

    val bitmap = bitmapCargado
    Tarjeta(sinPadding = true) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .background(Paleta.Primario)
                .clickable(enabled = bitmap != null, onClickLabel = "Ampliar la fotografía", onClick = onAmpliar),
            contentAlignment = Alignment.Center
        ) {
            if (bitmap != null) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "Fotografía de la muestra. Toca para ampliar",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                EtiquetaSobreFoto(
                    modifier = Modifier.align(Alignment.BottomStart),
                    icono = Icons.Filled.ZoomIn,
                    texto = "Toca para ampliar"
                )
                if (hora != null) {
                    EtiquetaSobreFoto(modifier = Modifier.align(Alignment.BottomEnd), icono = null, texto = hora)
                }
            } else if (rutaFoto != null) {
                CircularProgressIndicator(color = Color.White)
            } else {
                Text("Sin fotografía", color = Paleta.TextoSobreOscuro, style = MaterialTheme.typography.bodyMedium)
            }
        }

        // Acciones de la foto en una fila, separadas por líneas finas
        Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
            AccionFoto(
                texto = if (rutaFoto != null) "Repetir" else "Tomar foto",
                icono = if (rutaFoto != null) Icons.Filled.Refresh else Icons.Filled.PhotoCamera,
                modifier = Modifier.weight(1f),
                onClick = onRepetir
            )
            SeparadorVertical()
            AccionFoto(
                texto = "Galería",
                icono = Icons.Filled.PhotoLibrary,
                modifier = Modifier.weight(1f),
                onClick = onGaleria
            )
            SeparadorVertical()
            AccionFoto(
                texto = "Eliminar",
                icono = Icons.Filled.Delete,
                modifier = Modifier.weight(1f),
                peligro = true,
                habilitado = rutaFoto != null,
                onClick = onEliminar
            )
        }
    }
}

@Composable
private fun EtiquetaSobreFoto(modifier: Modifier, icono: ImageVector?, texto: String) {
    Row(
        modifier = modifier
            .padding(10.dp)
            .clip(RoundedCornerShape(50))
            .background(Paleta.Primario)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        if (icono != null) {
            Icon(imageVector = icono, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
        }
        Text(texto, color = Color.White, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun SeparadorVertical() {
    Box(
        Modifier
            .fillMaxHeight()
            .padding(vertical = 10.dp)
            .width(1.dp)
            .background(Paleta.Linea)
    )
}

@Composable
private fun AccionFoto(
    texto: String,
    icono: ImageVector,
    modifier: Modifier = Modifier,
    peligro: Boolean = false,
    habilitado: Boolean = true,
    onClick: () -> Unit
) {
    // Deshabilitado sigue legible (texto secundario), pero sin el rojo de borrar
    val colorTexto = when {
        !habilitado -> Paleta.TextoSuave
        peligro -> Paleta.Error
        else -> Paleta.Texto
    }
    val colorIcono = if (peligro && habilitado) Paleta.Error else Paleta.TextoSuave
    Row(
        modifier = modifier
            .heightIn(min = 52.dp)
            .clickable(enabled = habilitado, role = Role.Button, onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icono, contentDescription = null, tint = colorIcono, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text(text = texto, color = colorTexto, style = MaterialTheme.typography.labelLarge, maxLines = 1)
    }
}

// Tarjeta de una medición: se toca el número para escribirlo (solo dígitos)
@Composable
private fun TarjetaMetrica(
    titulo: String,
    valor: Int?,
    unidad: String,
    error: String? = null,
    onCambiar: (Int?) -> Unit,
    modifier: Modifier = Modifier
) {
    val foco = remember { FocusRequester() }
    val teclado = LocalSoftwareKeyboardController.current
    var enfocada by remember { mutableStateOf(false) }
    val borde = when {
        error != null -> BorderStroke(1.5.dp, Paleta.Error)
        enfocada -> BorderStroke(2.dp, Paleta.Acento)
        else -> BordeDeTarjeta
    }
    Column(
        modifier = modifier
            .tarjetaMussel(FormaTarjetaChica, borde)
            // Tocar cualquier parte de la tarjeta enfoca el campo y abre el teclado numérico
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                foco.requestFocus()
                teclado?.show()
            }
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        TextoRotulo(titulo)
        Spacer(Modifier.height(4.dp))
        BasicTextField(
            value = valor?.toString() ?: "",
            onValueChange = { texto -> onCambiar(texto.filter(Char::isDigit).take(4).toIntOrNull()) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
            textStyle = DatoMedido.copy(color = Paleta.Texto),
            cursorBrush = SolidColor(Paleta.Acento),
            // Sin etiqueta visible pegada al campo: TalkBack anuncia qué medición es y su unidad
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(foco)
                .onFocusChanged { enfocada = it.isFocused }
                .semantics {
                    contentDescription = "$titulo en $unidad"
                    if (error != null) error(error)
                },
            decorationBox = { campo ->
                Box {
                    if (valor == null) {
                        Text(text = "—", style = DatoMedido, color = Paleta.TextoSuave)
                    }
                    campo()
                }
            }
        )
        Text(text = unidad, color = Paleta.TextoSuave, style = MaterialTheme.typography.labelMedium, maxLines = 1)
        if (error != null) {
            Text(text = error, color = Paleta.Error, style = MaterialTheme.typography.labelMedium)
        } else if (valor == null) {
            // Obligatorio para guardar: se avisa que hay que tocar y escribir
            Text(
                text = "Toca para ingresar",
                color = Paleta.AcentoTexto,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

/** Mensaje de error de una medición ya escrita (null si está vacía o dentro del rango). */
private fun errorMedicion(valor: Int?, maximo: Int, minimo: Int = 1): String? =
    if (valor != null && valor !in minimo..maximo) "Debe estar entre $minimo y $maximo" else null

/** Fila de lectura: rótulo a la izquierda y valor a la derecha, separadas por una línea fina. */
@Composable
private fun Fila(etiqueta: String, valor: String) {
    Column {
        Box(Modifier.fillMaxWidth().height(1.dp).background(Paleta.Linea))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 44.dp)
                .padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = etiqueta,
                color = Paleta.TextoSuave,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(0.4f)
            )
            Text(
                text = valor,
                color = Paleta.Texto,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.End,
                modifier = Modifier.weight(0.6f)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun VistaPreviaScreenPreview() {
    MusselAppTheme {
        VistaPreviaScreen(
            borrador = BorradorMuestra(
                parametros = ParametrosMuestra("dalcahue", "B", 12, 3),
                observacion = "Buena fijación, ligera presencia de macroalgas."
            )
        )
    }
}
