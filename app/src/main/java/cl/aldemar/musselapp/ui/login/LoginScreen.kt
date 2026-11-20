package cl.aldemar.musselapp.ui.login

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.Anchor
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import cl.aldemar.musselapp.ui.theme.Acento
import cl.aldemar.musselapp.ui.theme.AcentoSuave
import cl.aldemar.musselapp.ui.theme.AcentoTexto
import cl.aldemar.musselapp.ui.theme.BordeCampo
import cl.aldemar.musselapp.ui.theme.BordeDeTarjeta
import cl.aldemar.musselapp.ui.theme.ElevacionTarjeta
import cl.aldemar.musselapp.ui.theme.FormaBoton
import cl.aldemar.musselapp.ui.theme.FormaCampo
import cl.aldemar.musselapp.ui.theme.FormaTarjeta
import cl.aldemar.musselapp.ui.theme.FormaTarjetaChica
import cl.aldemar.musselapp.ui.theme.Rotulo
import cl.aldemar.musselapp.ui.theme.Superficie
import cl.aldemar.musselapp.ui.theme.SuperficieCampo
import cl.aldemar.musselapp.ui.theme.TextoSobreOscuro
import cl.aldemar.musselapp.R
import cl.aldemar.musselapp.data.model.CentroCultivo
import cl.aldemar.musselapp.ui.theme.MusselAppTheme
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

/** Lo que tarda la animación de bienvenida en cubrir del todo el formulario (entra en 220 ms). */
private const val OCULTAR_FORMULARIO_MS = 260L

/** Punto de entrada con estado: conecta el ViewModel con la UI. */
@Composable
fun LoginRoute(viewModel: LoginViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Con la sesión iniciada se muestra la animación de bienvenida encima del formulario
    val usuario = uiState.usuario
    // Cuando la animación ya tapa el formulario, este se quita: así no reaparece mientras la
    // animación se desvanece ni durante el paso a la pantalla principal
    var formularioOculto by remember { mutableStateOf(false) }
    LaunchedEffect(usuario != null) {
        if (usuario != null) {
            delay(OCULTAR_FORMULARIO_MS)
            formularioOculto = true
        } else {
            formularioOculto = false
        }
    }
    Box(modifier = Modifier.fillMaxSize()) {
    if (formularioOculto) {
        Box(modifier = Modifier.fillMaxSize().background(SuperficieCampo))
    } else {
    LoginScreen(
        uiState = uiState,
        onRutChange = viewModel::onRutChange,
        onPasswordChange = viewModel::onPasswordChange,
        onTogglePasswordVisible = viewModel::onTogglePasswordVisible,
        onCentroChange = viewModel::onCentroChange,
        onIngresar = viewModel::ingresar,
        onMensajeMostrado = viewModel::onMensajeMostrado,
    )
    }
        if (usuario != null && !uiState.transicionTerminada) {
            AnimacionBienvenida(
                nombre = usuario.nombre.substringBefore(' '),
                subtitulo = uiState.centro.nombre.substringBefore(" - "),
                alTerminar = viewModel::onTransicionTerminada,
            )
        }
    }
}

/**
 * UI sin estado del Login (diseño "1. Login - MusselApp" de Stitch): RUT, PIN y centro.
 * El perfil no se elige: la app lo toma del usuario que ingresa.
 */
@Composable
fun LoginScreen(
    uiState: LoginUiState,
    onRutChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onTogglePasswordVisible: () -> Unit,
    onCentroChange: (CentroCultivo) -> Unit,
    onIngresar: () -> Unit,
    onMensajeMostrado: () -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(uiState.mensajeError) {
        uiState.mensajeError?.let {
            snackbarHostState.showSnackbar(it)
            onMensajeMostrado()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                // Con el teclado abierto el contenido se achica y se puede desplazar: el campo no queda tapado
                .consumeWindowInsets(innerPadding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Encabezado()

            Column(
                modifier = Modifier.widthIn(max = 480.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
                    PasoCredenciales(
                        uiState = uiState,
                        onRutChange = onRutChange,
                        onPasswordChange = onPasswordChange,
                        onTogglePasswordVisible = onTogglePasswordVisible,
                        onCentroChange = onCentroChange,
                        onIngresar = onIngresar,
                        onOlvidoPin = {
                            scope.launch { snackbarHostState.showSnackbar("Solicite el restablecimiento del PIN a su supervisor") }
                        },
                    )
                }
            }
        }
    }
}

/** Credenciales y centro de la jornada. */
@Composable
private fun PasoCredenciales(
    uiState: LoginUiState,
    onRutChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onTogglePasswordVisible: () -> Unit,
    onCentroChange: (CentroCultivo) -> Unit,
    onIngresar: () -> Unit,
    onOlvidoPin: () -> Unit,
) {
    val focusManager = LocalFocusManager.current
    val pinFocus = remember { FocusRequester() }

    Card(
        shape = FormaTarjeta,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BordeDeTarjeta,
        elevation = CardDefaults.cardElevation(defaultElevation = ElevacionTarjeta),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            CampoFormulario(
                etiqueta = "RUT",
                valor = uiState.rut,
                onValorChange = onRutChange,
                icono = Icons.Filled.Fingerprint,
                placeholder = "Ej: 15.482.901-6",
                error = uiState.rutError,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Ascii,
                    imeAction = ImeAction.Next,
                ),
                // "Siguiente" va directo al PIN, sin pasar por "¿Olvidó PIN?"
                keyboardActions = KeyboardActions(onNext = { pinFocus.requestFocus() }),
                visualTransformation = RutVisualTransformation,
            )

            CampoFormulario(
                etiqueta = "PIN de seguridad",
                valor = uiState.password,
                onValorChange = onPasswordChange,
                icono = Icons.Filled.Lock,
                placeholder = "Ingresa tu PIN",
                error = uiState.passwordError,
                accionEtiqueta = {
                    TextButton(
                        onClick = onOlvidoPin,
                        contentPadding = ButtonDefaults.TextButtonContentPadding,
                    ) {
                        Text(
                            "¿Olvidó PIN?",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = AcentoTexto,
                        )
                    }
                },
                trailingIcon = {
                    IconButton(onClick = onTogglePasswordVisible) {
                        Icon(
                            if (uiState.passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                            contentDescription = if (uiState.passwordVisible) "Ocultar PIN" else "Mostrar PIN",
                        )
                    }
                },
                visualTransformation = if (uiState.passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.NumberPassword,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(onDone = {
                    focusManager.clearFocus()
                    onIngresar()
                }),
                modifier = Modifier.focusRequester(pinFocus),
            )

            SelectorCentro(centro = uiState.centro, onCentroChange = onCentroChange)

            BotonIngresar(cargando = uiState.cargando) {
                focusManager.clearFocus()
                onIngresar()
            }
        }
    }
}

@Composable
private fun Encabezado() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Surface(
            shape = FormaTarjeta,
            color = MaterialTheme.colorScheme.surface,
            border = BordeDeTarjeta,
            shadowElevation = ElevacionTarjeta,
        ) {
            Image(
                painter = painterResource(R.drawable.logo_musselapp),
                contentDescription = "Logo MusselApp",
                modifier = Modifier
                    .padding(12.dp)
                    .size(64.dp)
                    .clip(RoundedCornerShape(8.dp)),
            )
        }
        Text(
            "MusselApp",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}

@Composable
private fun EtiquetaConIcono(texto: String, icono: ImageVector) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icono, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text(texto.uppercase(), style = Rotulo, color = MaterialTheme.colorScheme.secondary)
    }
}

@Composable
private fun campoColores() = OutlinedTextFieldDefaults.colors(
    // Campo con contorno de 3,5:1: se distingue de la tarjeta sin depender del fondo gris
    focusedContainerColor = Superficie,
    unfocusedContainerColor = SuperficieCampo,
    errorContainerColor = SuperficieCampo,
    focusedBorderColor = Acento,
    unfocusedBorderColor = BordeCampo,
    errorBorderColor = MaterialTheme.colorScheme.error,
    cursorColor = Acento,
    focusedLeadingIconColor = MaterialTheme.colorScheme.secondary,
    unfocusedLeadingIconColor = MaterialTheme.colorScheme.secondary,
    focusedTrailingIconColor = MaterialTheme.colorScheme.secondary,
    unfocusedTrailingIconColor = MaterialTheme.colorScheme.secondary,
    unfocusedPlaceholderColor = MaterialTheme.colorScheme.secondary,
    focusedPlaceholderColor = MaterialTheme.colorScheme.secondary,
)

@Composable
private fun CampoFormulario(
    etiqueta: String,
    valor: String,
    onValorChange: (String) -> Unit,
    icono: ImageVector,
    placeholder: String,
    error: String?,
    modifier: Modifier = Modifier,
    accionEtiqueta: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 32.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(etiqueta.uppercase(), style = Rotulo, color = MaterialTheme.colorScheme.secondary)
            accionEtiqueta?.invoke()
        }
        OutlinedTextField(
            value = valor,
            onValueChange = onValorChange,
            modifier = modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp),
            textStyle = MaterialTheme.typography.titleMedium,
            placeholder = { Text(placeholder) },
            leadingIcon = { Icon(icono, contentDescription = null) },
            trailingIcon = trailingIcon,
            isError = error != null,
            supportingText = error?.let { { Text(it) } },
            visualTransformation = visualTransformation,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            singleLine = true,
            shape = FormaCampo,
            colors = campoColores(),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectorCentro(centro: CentroCultivo, onCentroChange: (CentroCultivo) -> Unit) {
    var expandido by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        EtiquetaConIcono("Centro de cultivo asignado", Icons.Filled.Anchor)
        ExposedDropdownMenuBox(expanded = expandido, onExpandedChange = { expandido = it }) {
            OutlinedTextField(
                value = centro.nombre,
                onValueChange = {},
                readOnly = true,
                singleLine = true,
                textStyle = MaterialTheme.typography.titleMedium,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandido) },
                shape = FormaCampo,
                colors = campoColores(),
                modifier = Modifier
                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                    .fillMaxWidth(),
            )
            ExposedDropdownMenu(expanded = expandido, onDismissRequest = { expandido = false }) {
                CentroCultivo.disponibles.forEach { opcion ->
                    DropdownMenuItem(
                        text = { Text(opcion.nombre, maxLines = 2, overflow = TextOverflow.Ellipsis) },
                        onClick = {
                            onCentroChange(opcion)
                            expandido = false
                        },
                        contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                    )
                }
            }
        }
    }
}

@Composable
private fun BotonIngresar(cargando: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = !cargando,
        shape = FormaBoton,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            disabledContainerColor = MaterialTheme.colorScheme.secondary,
            disabledContentColor = MaterialTheme.colorScheme.onSecondary,
        ),
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
    ) {
        if (cargando) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.onSecondary,
            )
            Spacer(Modifier.width(12.dp))
            Text("Verificando credencial…", style = MaterialTheme.typography.titleMedium)
        } else {
            Icon(Icons.AutoMirrored.Filled.Login, contentDescription = null)
            Spacer(Modifier.width(12.dp))
            Text("Ingresar al sistema", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.width(12.dp))
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
        }
    }
}

@Preview(showBackground = true, heightDp = 900)
@Composable
private fun LoginPreview() {
    MusselAppTheme {
        LoginScreen(
            uiState = LoginUiState(rut = "11.111.111-1", password = "1234"),
            onRutChange = {}, onPasswordChange = {}, onTogglePasswordVisible = {},
            onCentroChange = {}, onIngresar = {}, onMensajeMostrado = {},
        )
    }
}
