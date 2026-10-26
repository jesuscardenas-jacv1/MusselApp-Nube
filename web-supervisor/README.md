# Página web del supervisor (demo en tiempo real)

Página para que el supervisor revise, desde el computador, las muestras que los operarios suben en terreno.
Se conecta al proyecto Firebase `musselapp-demo` (Firestore, colección `muestras`) y se actualiza sola.

## Cómo abrirla
1. Abrir `web-supervisor/index.html` con Chrome (doble clic). Necesita internet.
2. Ingresar con el supervisor de prueba: RUT `22.222.222-2`, PIN `1234`.

Pantallas: **Principal** (métricas), **Historial** (búsqueda, filtros), **Revisión** (cola de pendientes) y el detalle de cada muestra con **Validar** / **Corregir** (el comentario es obligatorio al corregir).
Cuando llega una muestra nueva suena un aviso y aparece una notificación.

## Plan B para la presentación
En la pantalla Principal, abajo, **Herramientas de demostración** permite simular una muestra de operario (y borrarlas) si el teléfono no logra subirla.

## Datos que la app del operario debe escribir
Documento en `muestras/{id}` (id único por muestra, por ejemplo un UUID):

| Campo | Tipo | Ejemplo |
|---|---|---|
| `codigo` | string | `"MST-017"` |
| `centroId` | string | `"dalcahue"` (dalcahue, calbuco, quellon, melinka) |
| `tren` | string | `"B"` |
| `linea` | número | `8` |
| `profundidadM` | número | `5` |
| `tallaMm` | número | `54` |
| `conteoIndM` | número | `412` |
| `observacion` | string | `"Buena fijación"` |
| `estado` | string | `"PENDIENTE"` |
| `registradaEn` | número (ms) | `1792900000000` |
| `operador` | string | `"Carla Vera"` |
| `operadorRut` | string | `"111111111"` |
| `fotoBase64` | string (opcional) | `"data:image/jpeg;base64,..."` (foto reducida, menos de ~700 KB) |

Cuando el supervisor dictamina, la página actualiza: `estado` (`VALIDADO` o `CORREGIDO`), `revisadaPor`, `revisadaEn` (ms) y `comentarioSupervisor`.
La app del operario debe escuchar esos cambios para actualizar la muestra en su teléfono.

## Seguridad
Solo datos ficticios. Firestore está en modo de prueba (abierto por 30 días): antes de presentar hay que cerrar las reglas.
