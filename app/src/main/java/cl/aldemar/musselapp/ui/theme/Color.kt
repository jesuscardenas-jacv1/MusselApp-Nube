package cl.aldemar.musselapp.ui.theme

import androidx.compose.ui.graphics.Color

// Paleta MusselApp (rediseño "variante C": tarjetas blancas con borde y sombra sobre un fondo gris azulado).
// Entre paréntesis, el contraste con el fondo donde se usa (mínimo WCAG: 4,5:1 texto, 3:1 bordes).

// Superficies
val Fondo = Color(0xFFE3E8EF)           // fondo de pantalla (la tarjeta blanca destaca 1,23:1 más el borde)
val Superficie = Color(0xFFFFFFFF)      // tarjetas, barras inferiores y menús
val SuperficieCampo = Color(0xFFF4F6F9) // campo sin completar
val SuperficieTenue = Color(0xFFEEF2F7) // recuadros de íconos y códigos dentro de una tarjeta
val Linea = Color(0xFFE2E8F0)           // separadores decorativos dentro de una tarjeta
val BordeTarjeta = Color(0xFFB6C2D2)    // contorno de las tarjetas
val BordeCampo = Color(0xFF7C8BA1)      // contorno de campos y opciones (3,5:1 sobre blanco)

// Texto: solo dos tonos
val Texto = Color(0xFF0F172A)           // títulos y datos (17,9:1 sobre blanco)
val TextoSecundario = Color(0xFF475569) // apoyo, unidades y rótulos (7,6:1 blanco · 6,2:1 fondo)
val TextoSobreOscuro = Color(0xFFCBD5E1) // texto secundario sobre Principal (12:1)

// Acción y acento
val Principal = Color(0xFF0F172A)       // botón principal, opción elegida y paso actual
val Acento = Color(0xFF0F766E)          // íconos, foco y lo ya completado (5,5:1 sobre blanco)
val AcentoSuave = Color(0xFFDDF3EF) // fondo de "Editar", del menú activo y de avisos
val AcentoTexto = Color(0xFF0B5E57)     // texto sobre AcentoSuave (6,6:1)
val AcentoSobreOscuro = Color(0xFF5EEAD4) // íconos de acento sobre Principal (11:1)
val Error = Color(0xFFB91C1C)           // errores y acciones de borrado (6,5:1 sobre blanco)

// Nombres anteriores, usados por las pantallas con sesión iniciada
val Secundario = TextoSecundario
val Adicional = BordeCampo

// Estados de una muestra (chips del historial y la pantalla principal).
// Validado es verde, igual que en el portal y que la franja del Historial (6,8:1 texto/fondo).
val EstadoValidadoFondo = Color(0xFFD1FAE5)
val EstadoValidadoTexto = Color(0xFF065F46)

// Pendiente en ámbar
val EstadoEnEsperaFondo = Color(0xFFFEF3C7)
val EstadoEnEsperaTexto = Color(0xFF92400E)

// Corregido en azul, con su propio color: los mismos del portal (texto/fondo 7,2:1)
val EstadoCorregidoFondo = Color(0xFFDBEAFE)
val EstadoCorregidoTexto = Color(0xFF1E40AF)

// Franja lateral de las tarjetas del Historial según el estado
val AcentoValidado = Color(0xFF16A34A)
val AcentoCorregido = Color(0xFF2563EB) // 5,2:1 sobre blanco
val AcentoPendiente = Color(0xFFC76C05) // 3,1:1 sobre el fondo de pantalla (punto de conexión) y 3,8:1 sobre blanco

// Alertas que no son un estado (por ejemplo, densidad bajo el mínimo)
val AlertaFondo = Color(0xFFFEE2E2)
val AlertaTexto = Color(0xFFB91C1C)
