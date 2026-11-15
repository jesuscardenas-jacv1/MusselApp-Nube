package cl.aldemar.musselapp.ui.muestra

import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.semantics.contentDescription
import cl.aldemar.musselapp.ui.theme.Acento
import cl.aldemar.musselapp.ui.theme.AcentoSuave
import cl.aldemar.musselapp.ui.theme.AcentoTexto
import cl.aldemar.musselapp.ui.theme.BordeCampo
import cl.aldemar.musselapp.ui.theme.BordeDeCampo
import cl.aldemar.musselapp.ui.theme.BordeTarjeta
import cl.aldemar.musselapp.ui.theme.Error
import cl.aldemar.musselapp.ui.theme.Fondo
import cl.aldemar.musselapp.ui.theme.FormaBoton
import cl.aldemar.musselapp.ui.theme.FormaCampo
import cl.aldemar.musselapp.ui.theme.Linea
import cl.aldemar.musselapp.ui.theme.Principal
import cl.aldemar.musselapp.ui.theme.Rotulo
import cl.aldemar.musselapp.ui.theme.Superficie
import cl.aldemar.musselapp.ui.theme.SuperficieCampo
import cl.aldemar.musselapp.ui.theme.SuperficieTenue
import cl.aldemar.musselapp.ui.theme.Texto
import cl.aldemar.musselapp.ui.theme.TextoSecundario
import cl.aldemar.musselapp.ui.theme.TextoSobreOscuro
import cl.aldemar.musselapp.ui.theme.tarjetaMussel

// Componentes y datos compartidos por Nueva Muestra, Captura y Vista previa

/**
 * Colores del flujo de muestreo. Son los del tema (ui/theme/Color.kt) con nombres propios del flujo,
 * para que las tres pantallas usen exactamente los mismos.
 */
internal object Paleta {
    val Fondo = cl.aldemar.musselapp.ui.theme.Fondo
    val Pie = cl.aldemar.musselapp.ui.theme.Superficie // barra inferior con la acción principal
    val Superficie = cl.aldemar.musselapp.ui.theme.Superficie
    val Campo = SuperficieCampo // campo sin completar
    val Tenue = SuperficieTenue // recuadros de íconos
    val Linea = cl.aldemar.musselapp.ui.theme.Linea // separadores dentro de una tarjeta
    val BordeTarjeta = cl.aldemar.musselapp.ui.theme.BordeTarjeta
    val BordeCampo = cl.aldemar.musselapp.ui.theme.BordeCampo // 3,5:1 sobre blanco
    val Texto = cl.aldemar.musselapp.ui.theme.Texto
    val TextoSuave = TextoSecundario // 7,6:1 sobre blanco y 6,2:1 sobre el fondo
    val TextoSobreOscuro = cl.aldemar.musselapp.ui.theme.TextoSobreOscuro
    val Acento = cl.aldemar.musselapp.ui.theme.Acento
    val AcentoSuave = cl.aldemar.musselapp.ui.theme.AcentoSuave
    val AcentoTexto = cl.aldemar.musselapp.ui.theme.AcentoTexto
    val Primario = Principal // botón principal y opción seleccionada
    val SobrePrimario = Color.White
    val Error = cl.aldemar.musselapp.ui.theme.Error
}

internal val RojoError = Paleta.Error

internal enum class Estrato(val rango: String, val nombre: String, val profundidadM: Int) {
    SUPERFICIE("0-2m", "Superficie", 1),
    MEDIA("2-4m", "Media", 3),
    FONDO("4-6m", "Fondo", 5);

    companion object {
        fun desde(profundidadM: Int): Estrato = when {
            profundidadM <= 2 -> SUPERFICIE
            profundidadM <= 4 -> MEDIA
            else -> FONDO
        }
    }
}

// TODO: mover trenes y líneas al repositorio cuando existan datos reales
// El código (A, B, C) es el que usa Muestra.tren; el texto es solo para mostrar
internal val Trenes = listOf(
    "A" to "Tren A - Longline Sur",
    "B" to "Tren B - Longline Centro",
    "C" to "Tren C - Longline Norte",
)
internal val Lineas = (1..12).toList()

internal fun etiquetaLinea(linea: Int) = "Línea %02d (Cuerda colectora 6m)".format(linea)

internal fun nombreTren(codigo: String) = Trenes.firstOrNull { it.first == codigo }?.second ?: "Tren $codigo"

/** Tarjeta blanca con borde y sombra sobre el fondo. */
@Composable
internal fun Tarjeta(
    modifier: Modifier = Modifier,
    sinPadding: Boolean = false,
    espacio: Dp = 14.dp,
    contenido: @Composable () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .tarjetaMussel()
            .padding(if (sinPadding) 0.dp else 16.dp),
        verticalArrangement = Arrangement.spacedBy(if (sinPadding) 0.dp else espacio)
    ) {
        contenido()
    }
}

/** Título de tarjeta (nivel 3): ícono de acento sin recuadro, texto y, a la derecha, un extra opcional. */
@Composable
internal fun TituloTarjeta(
    texto: String,
    icono: ImageVector? = null,
    extra: @Composable RowScope.() -> Unit = {}
) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (icono != null) {
            Icon(imageVector = icono, contentDescription = null, tint = Paleta.Acento, modifier = Modifier.size(20.dp))
        }
        Text(
            text = texto,
            style = MaterialTheme.typography.titleMedium,
            color = Paleta.Texto,
            modifier = Modifier.weight(1f)
        )
        extra()
    }
}

/** Rótulo (nivel 5): monoespaciado, en mayúsculas y en texto secundario. */
@Composable
internal fun TextoRotulo(texto: String, modifier: Modifier = Modifier, color: Color = Paleta.TextoSuave) {
    Text(text = texto.uppercase(), style = Rotulo, color = color, maxLines = 1, modifier = modifier)
}

/**
 * Barra superior clara del flujo de muestreo: volver, título, rótulo y el paso en una píldora oscura.
 * Se funde con el fondo para que el título de la pantalla sea lo primero que se lee.
 */
@Composable
internal fun BarraFlujo(titulo: String, rotulo: String, paso: Int?, onVolver: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Paleta.Fondo)
            .statusBarsPadding()
            .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .tarjetaMussel(FormaBoton)
                .clickable(onClickLabel = "Volver", role = Role.Button, onClick = onVolver),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = Paleta.Texto)
        }
        Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
            Text(text = titulo, style = MaterialTheme.typography.titleSmall, color = Paleta.Texto)
            TextoRotulo(rotulo)
        }
        if (paso != null) {
            Text(
                text = "PASO $paso DE 3",
                style = Rotulo,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(Paleta.Primario)
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            )
        }
    }
}

/** Botón principal (uno por pantalla): fondo oscuro, 56 dp de alto. */
@Composable
internal fun BotonPrincipal(
    texto: String,
    modifier: Modifier = Modifier,
    iconoInicio: ImageVector? = null,
    iconoFin: ImageVector? = null,
    habilitado: Boolean = true,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clip(FormaBoton)
            .background(if (habilitado) Paleta.Primario else Paleta.Campo)
            .then(if (habilitado) Modifier else Modifier.border(1.dp, Paleta.BordeCampo, FormaBoton))
            .clickable(enabled = habilitado, role = Role.Button, onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val color = if (habilitado) Paleta.SobrePrimario else Paleta.TextoSuave
        if (iconoInicio != null) {
            Icon(iconoInicio, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text = texto, style = MaterialTheme.typography.titleMedium, color = color)
        if (iconoFin != null) {
            Spacer(Modifier.width(8.dp))
            Icon(iconoFin, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
        }
    }
}

/** Botón secundario: contorno fino, sin competir con el principal. */
@Composable
internal fun BotonSecundario(texto: String, icono: ImageVector?, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clip(FormaBoton)
            .background(Paleta.Superficie)
            .border(BordeDeCampo, FormaBoton)
            .clickable(role = Role.Button, onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icono != null) {
            Icon(icono, contentDescription = null, tint = Paleta.TextoSuave, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text = texto, style = MaterialTheme.typography.labelLarge, color = Paleta.Texto)
    }
}

/** Píldora de acento para acciones dentro de una tarjeta ("Editar"). */
@Composable
internal fun PildoraAccion(texto: String, icono: ImageVector, activa: Boolean = false, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .heightIn(min = 40.dp)
            .clip(RoundedCornerShape(50))
            .background(if (activa) Paleta.Primario else Paleta.AcentoSuave)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        val color = if (activa) Paleta.SobrePrimario else Paleta.AcentoTexto
        Icon(imageVector = icono, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
        Text(text = texto, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, color = color)
    }
}

/**
 * Lista desplegable sin rótulo visible: el valor elegido es lo que se lee. Vacía se ve en gris
 * con el texto "Selecciona…"; TalkBack igual anuncia de qué campo se trata ([etiqueta]).
 */
@Composable
internal fun Selector(
    etiqueta: String,
    valor: String?,
    vacio: String,
    icono: ImageVector?,
    opciones: List<String>,
    error: String? = null, // si no es null, el campo se marca en rojo (como isError)
    onElegir: (Int) -> Unit
) {
    var abierto by remember { mutableStateOf(false) }
    val elegido = valor != null

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Box {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .clip(FormaCampo)
                    .background(if (elegido) Paleta.Superficie else Paleta.Campo)
                    .border(
                        width = if (error != null) 1.5.dp else 1.dp,
                        color = if (error != null) Paleta.Error else Paleta.BordeCampo,
                        shape = FormaCampo
                    )
                    .clickable(onClickLabel = "Abrir lista de opciones") { abierto = true }
                    // TalkBack lee "Centro de cultivo, lista desplegable, <valor>" en un solo elemento
                    .semantics(mergeDescendants = true) {
                        contentDescription = etiqueta
                        role = Role.DropdownList
                        stateDescription = valor ?: vacio
                        if (error != null) this.error(error)
                    }
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (icono != null) {
                    Icon(
                        imageVector = icono,
                        contentDescription = null,
                        tint = if (elegido) Paleta.Acento else Paleta.TextoSuave,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                }
                Text(
                    text = valor ?: vacio,
                    style = if (elegido) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyLarge,
                    color = if (elegido) Paleta.Texto else Paleta.TextoSuave,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = Icons.Filled.KeyboardArrowDown,
                    contentDescription = null,
                    tint = Paleta.TextoSuave
                )
            }
            DropdownMenu(
                expanded = abierto,
                onDismissRequest = { abierto = false },
                containerColor = Paleta.Superficie
            ) {
                opciones.forEachIndexed { i, opcion ->
                    DropdownMenuItem(
                        text = { Text(opcion, color = Paleta.Texto, style = MaterialTheme.typography.bodyLarge) },
                        onClick = {
                            onElegir(i)
                            abierto = false
                        }
                    )
                }
            }
        }
        // Mensaje bajo el campo (como supportingText)
        if (error != null) {
            TextoError(error)
        }
    }
}

/** Texto rojo que explica qué corregir en un campo. */
@Composable
internal fun TextoError(texto: String) {
    Text(
        text = texto,
        color = Paleta.Error,
        style = MaterialTheme.typography.bodySmall,
        fontWeight = FontWeight.Medium
    )
}

@Composable
internal fun OpcionEstrato(
    estrato: Estrato,
    seleccionado: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .heightIn(min = 64.dp)
            .clip(FormaCampo)
            .background(if (seleccionado) Paleta.Primario else Paleta.Superficie)
            .border(1.dp, if (seleccionado) Paleta.Primario else Paleta.BordeCampo, FormaCampo)
            // Se anuncia como opción de un grupo y si está seleccionada
            .selectable(selected = seleccionado, role = Role.RadioButton, onClick = onClick)
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = estrato.rango,
            color = if (seleccionado) Paleta.SobrePrimario else Paleta.Texto,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Text(
            text = estrato.nombre,
            color = if (seleccionado) Paleta.TextoSobreOscuro else Paleta.TextoSuave,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Normal,
            textAlign = TextAlign.Center
        )
    }
}
