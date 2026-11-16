package cl.aldemar.musselapp.data.model

/**
 * Aviso automático sobre una muestra, para el supervisor. Las reglas están en
 * [cl.aldemar.musselapp.util.AlertasMuestra]. El nombre del enum es el que se sube a la nube.
 */
enum class Alerta(val etiqueta: String, val critica: Boolean) {
    FUERA_DE_AREA("Fuera del área", critica = true),
    DENSIDAD_BAJA("Densidad baja", critica = true),
    SIN_UBICACION("Sin ubicación", critica = false),
    UBICACION_IMPRECISA("Ubicación imprecisa", critica = false),
    DESVIO_DENSIDAD("Desvío de densidad", critica = false),
    CHOLGAS_ALTAS("Cholgas altas", critica = false),
    TALLA_FUERA_DE_RANGO("Talla fuera de rango", critica = false),
    MAL_TIEMPO("Mal tiempo al muestrear", critica = false),
}
