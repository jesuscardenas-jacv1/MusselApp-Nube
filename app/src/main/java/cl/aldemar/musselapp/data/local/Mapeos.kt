package cl.aldemar.musselapp.data.local

import cl.aldemar.musselapp.data.local.entidad.MuestraCompleta
import cl.aldemar.musselapp.data.local.entidad.MuestraEntity
import cl.aldemar.musselapp.data.local.entidad.UsuarioEntity
import cl.aldemar.musselapp.data.model.Clima
import cl.aldemar.musselapp.data.model.Muestra
import cl.aldemar.musselapp.data.model.Posicion
import cl.aldemar.musselapp.data.model.Usuario
import cl.aldemar.musselapp.util.RutValidator

// Conversión de las filas de Room a los modelos que usan las pantallas.

fun MuestraCompleta.aModelo() = Muestra(
    id = muestra.id,
    centroId = centroId,
    linea = lineaNumero,
    tren = trenCodigo,
    profundidadM = muestra.profundidadM,
    tallaMm = muestra.tallaMm,
    conteoIndM = muestra.conteoIndM,
    observacion = muestra.observacion,
    estado = muestra.estado,
    registradaEn = muestra.registradaEn,
    sincronizada = muestra.sincronizada,
    operador = operadorNombre,
    operadorRut = muestra.operadorRut,
    revisadaPor = supervisorNombre,
    revisadaEn = revisadaEn,
    comentarioSupervisor = comentarioSupervisor,
    fotoRuta = muestra.fotoRuta,
    cholgasIndM = muestra.cholgasIndM,
    tramoCm = muestra.tramoCm,
    posicion = if (muestra.lat != null && muestra.lon != null) {
        Posicion(muestra.lat, muestra.lon, muestra.precisionM)
    } else null,
    clima = muestra.climaObtenidoEn?.let { obtenidoEn ->
        Clima(
            tempAireC = muestra.climaTempAireC,
            vientoKn = muestra.climaVientoKn,
            rachaKn = muestra.climaRachaKn,
            vientoDir = muestra.climaVientoDir,
            codigo = muestra.climaCodigo,
            olaM = muestra.climaOlaM,
            tempAguaC = muestra.climaTempAguaC,
            mareaM = muestra.climaMareaM,
            obtenidoEn = obtenidoEn,
        )
    },
)

fun UsuarioEntity.aModelo() = Usuario(
    rut = RutValidator.formatear(rut),
    nombre = nombre,
    rol = rol,
)

fun MuestraEntity.conPosicion(p: Posicion?) =
    if (p == null) this else copy(lat = p.lat, lon = p.lon, precisionM = p.precisionM)

fun MuestraEntity.conClima(c: Clima?) =
    if (c == null) this else copy(
        climaTempAireC = c.tempAireC,
        climaVientoKn = c.vientoKn,
        climaRachaKn = c.rachaKn,
        climaVientoDir = c.vientoDir,
        climaCodigo = c.codigo,
        climaOlaM = c.olaM,
        climaTempAguaC = c.tempAguaC,
        climaMareaM = c.mareaM,
        climaObtenidoEn = c.obtenidoEn,
    )
