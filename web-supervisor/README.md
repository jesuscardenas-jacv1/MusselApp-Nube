# Página web del supervisor (demo en tiempo real)

Página para que el supervisor revise, desde el computador, las muestras que los operarios suben en terreno.
Se conecta al proyecto Firebase `musselapp-demo` (Firestore, colección `muestras`) y se actualiza sola.
Está publicado en **https://musselapp-demo.web.app** (Firebase Hosting). También es un solo archivo (`index.html`) que funciona con doble clic: no necesita servidor ni instalación.

Para publicar una versión nueva (con la herramienta de Firebase y una cuenta con permisos en el proyecto):
```bash
firebase deploy --only hosting --project musselapp-demo
```
Se publica la carpeta `web-supervisor/` sin el portal anterior ni este README (configuración en `firebase.json`).

## Cómo abrirla
1. Abrir `web-supervisor/index.html` con Chrome (doble clic). Necesita internet (Firebase, tipografías, gráficos y mapa se cargan por CDN).
2. Ingresar con el correo y la contraseña de la cuenta de supervisor creada en Firebase Authentication (ver "Cuentas y seguridad" más abajo).

### Modo ejemplo (sin nube)
Agregando `?ejemplo=1` a la dirección (o con el enlace **Ver con datos de ejemplo** del inicio de sesión) la página
**no se conecta a Firestore**: genera en memoria unas 160 muestras ficticias de los últimos 90 días (siempre las mismas, semilla fija)
y entra sin pedir credenciales. Validar, corregir, eliminar y simular solo cambian esa memoria; al recargar se vuelve al estado inicial.
Una cinta amarilla avisa "Datos de ejemplo: nada se guarda". Sirve para probar y para mostrar los gráficos con datos.

## Pantallas
La barra lateral tiene cinco apartados. La vista activa queda en la dirección (`#control`, `#dashboard`, `#datos`, `#historial`, `#mapa`), así que al recargar no se pierde.
El botón **Contraer menú** la reduce a una franja de iconos; la preferencia se recuerda en el navegador.

1. **Control de muestras** (pantalla inicial): una bandeja de trabajo para gestionar las muestras. Cuatro cifras de resumen
   (por revisar, con más de 24 h, llegadas hoy, revisadas hoy) y una sola lista con pestañas **Por revisar / Revisadas / Todas**,
   filtro por centro, búsqueda y orden. Cada fila permite **Validar** al instante, **Corregir** (abre el detalle con el comentario
   obligatorio) o eliminar; con las casillas se validan o eliminan varias a la vez. Al dictaminar desde el detalle se abre
   la siguiente pendiente. Reemplaza a las antiguas vistas Principal y Revisión.
2. **Dashboard**: selector de periodo (7, 30, 90 días o todo) y filtro por centro. Muestras por día por estado, donut de estados,
   evolución de talla por centro, densidad frente al estándar de 380 ind/m, rendimiento, tiempo medio de revisión, cobertura de líneas (de 36),
   ranking de operarios y alertas (desvío de densidad fuera de ±20 % y pendientes con más de 24 h).
3. **Datos**, con tres pestañas:
   - *Tabla*: todas las muestras con filtros combinables, columnas ordenables y elegibles, paginación y selección múltiple (exportar o eliminar la selección).
   - *Proyección*: regresión lineal de la talla por centro (y opcionalmente tren y línea) para estimar cuándo se alcanza la talla comercial
     (55 mm por defecto, editable) y el rendimiento a 30, 60 y 90 días. Es una estimación referencial; con menos de 3 muestras muestra "datos insuficientes".
   - *Exportación*: CSV para Excel en español (UTF-8 con BOM y separador `;`), Excel `.xlsx`, JSON e informe imprimible / PDF.
     Se eligen las filas (filtradas, selección o todas), las columnas y si van los campos derivados. La foto solo sale en JSON y solo si se marca la casilla.
   - Al final están las **Herramientas de demostración** (también desde el menú del usuario).
4. **Historial**: árbol Centro → Tren → Línea con contadores y color del último estado, migas de pan, resumen del nodo, gráfico de talla y densidad,
   mapa de calor trenes × líneas (por estado o por desvío) y línea de tiempo de las muestras. Búsqueda y filtros por estado y periodo.
5. **Mapa**: los cuatro centros sobre un mapa con datos de OpenStreetMap (teselas de CARTO; coordenadas ficticias aproximadas). Al elegir un centro el mapa se acerca
   y el panel muestra sus datos, con accesos a su historial y a su dashboard.

En todas las pantallas: buscador superior (código, centro, tren, línea u operario; tecla `/`), campana con las últimas llegadas,
indicador En vivo / Sin conexión y menú del usuario. El detalle de una muestra se abre en una ventana centrada y amplia: a la izquierda la foto (ampliable), las cifras y el desvío;
a la derecha los datos, la observación y el dictamen, con los botones **Validar** / **Corregir** (el comentario es obligatorio al corregir) y **Eliminar**.
Eliminar una muestra real la borra también del teléfono que la registró, que recibe una notificación.
En una muestra pendiente el supervisor puede corregir el **conteo** (1–5.000 ind/m), el **calibre** (1–100 mm) y las **cholgas** (0–5.000 ind/m); el rendimiento y el desvío
se recalculan al escribir. Los valores corregidos se guardan solo con **Corregir**: Firestore recibe `conteoIndM`, `tallaMm` y `cholgasIndM` nuevos más
`valoresOperario` (lo que registró el operario), y el cambio se agrega al comentario, porque el teléfono solo recibe estado y comentario.
Cuando llega una muestra nueva suena un aviso y aparece una notificación.

En el modo ejemplo y en las muestras simuladas se usa la misma foto real de una muestra de choritos (`Fotos/Muestra Real.jpeg`,
reducida a 960 px e incluida dentro del HTML).

## Plan B para la presentación
En **Datos**, abajo, **Herramientas de demostración** permite simular una muestra de operario (y borrarlas) si el teléfono no logra subirla.
Ahí también está **Borrar TODAS las muestras**: en el modo normal borra de verdad en Firestore.

**Cargar historial de ejemplo** sube a Firestore 160 muestras ficticias de los últimos 90 días, con el mismo catálogo de la app
(4 centros, trenes A/B/C, líneas 1–12, estratos de 1, 3 y 5 m, operaria Carla Vera). Usan ids `hist_001`…`hist_160`, códigos
`Muestra 1001`…`Muestra 1160`, el campo `historico: true` y una miniatura de la foto (320 px) para que el portal cargue rápido.
No disparan avisos de muestra nueva ni llegan a los teléfonos. **Borrar historial de ejemplo** las elimina todas sin tocar las reales.

## Portal anterior
`web-supervisor/anterior/index.html` es el portal de antes del rediseño, sin cambios. Se abre igual (doble clic) y usa la misma
base de Firestore, por si hay que volver a él.
**Ojo:** el portal anterior no inicia sesión en Firebase (el RUT y el PIN de prueba están escritos en su código). Desde el 23-nov-2026
están publicadas las reglas de `firestore.rules`, así que ya no lee ni escribe datos. No se publica en Hosting.

## Librerías (por CDN, sin instalación)
Firebase compat 10.14.1, Chart.js 4.4.6, Leaflet 1.9.4 (teselas de CARTO con datos de OpenStreetMap y clave gratuita no comercial en `CLAVE_CARTO`; se usan porque los servidores de OpenStreetMap rechazan el portal abierto como archivo), SheetJS 0.20.3 y las tipografías Inter y Plus Jakarta Sans.
Si alguna no carga (sin internet), la pantalla correspondiente muestra un aviso en lugar de fallar.

## Datos que la app del operario debe escribir
Documento en `muestras/{id}` (id único por muestra, por ejemplo un UUID):

| Campo | Tipo | Ejemplo |
|---|---|---|
| `codigo` | string | `"Muestra 017"` |
| `centroId` | string | `"dalcahue"` (dalcahue, calbuco, quellon, melinka) |
| `tren` | string | `"B"` |
| `linea` | número | `8` |
| `profundidadM` | número | `5` |
| `tallaMm` | número | `54` |
| `conteoIndM` | número | `412` |
| `cholgasIndM` | número o null | `10` (cholgas por metro; null en muestras anteriores a la v4 de la app) |
| `tramoCm` | número o null | `100` (largo del tramo de cuerda muestreado en cm, la "longitud" del informe; null en muestras anteriores a la v6 de la base) |
| `observacion` | string | `"Buena fijación"` |
| `estado` | string | `"PENDIENTE"` |
| `registradaEn` | número (ms) | `1792900000000` |
| `operador` | string | `"Carla Vera"` |
| `operadorRut` | string | `"111111111"` |
| `tieneFoto` | booleano | `true` si la muestra tiene foto en `fotos/{id}` |
| `lat`, `lon` | número o null | `-42.3741`, `-73.6220` (posición del teléfono al guardar; null sin GPS) |
| `precisionM` | número o null | `12` (precisión del GPS en metros) |
| `distanciaCentroM` | número o null | `180` (metros al punto de referencia del centro) |
| `clima` | mapa o null | `tempAireC`, `vientoKn`, `rachaKn`, `vientoDir`, `codigo`, `descripcion`, `olaM`, `tempAguaC`, `mareaM`, `obtenidoEn` (Open-Meteo) |
| `alertas` | lista de strings | `["FUERA_DE_AREA", "MAL_TIEMPO"]` |
| `ownerUid` | string | uid de la sesión anónima del teléfono que subió la muestra (las reglas lo usan) |
| `instalacion` | string | `"a1b2c3d4"` (prefijo del id del documento; identifica al teléfono) |

Las alertas las calcula **solo la app** al registrar (`AlertasMuestra.kt`); el portal las muestra tal como llegaron. Valores posibles:
`FUERA_DE_AREA`, `DENSIDAD_BAJA` (críticas), `SIN_UBICACION`, `UBICACION_IMPRECISA`, `DESVIO_DENSIDAD`, `CHOLGAS_ALTAS`, `TALLA_FUERA_DE_RANGO` y `MAL_TIEMPO`.
Si el supervisor corrige el conteo en el portal, la lista no se recalcula: por eso el detalle la rotula "Alertas al registrar".
Se ven en la bandeja (insignia junto al código), en el detalle (con un mapa chico de la posición y el área del centro), en el panel
"Alertas" del Dashboard y como columnas en Datos y en las exportaciones. Las muestras anteriores a la v5 de la app no traen estos campos.

La foto **no** va en la muestra: va en la colección `fotos`, un documento por muestra con el mismo id y el campo
`fotoBase64` (`"data:image/jpeg;base64,..."`, máx. 800 px, menos de 500 KB). Así el portal, que escucha
toda la colección `muestras`, y los teléfonos, que escuchan sus muestras, no descargan todas las fotos; el portal pide la foto solo al abrir una muestra.
Las muestras antiguas con `fotoBase64` dentro del documento se siguen mostrando (ya se migraron las de Firestore).

Cuando el supervisor dictamina, la página actualiza: `estado` (`VALIDADO` o `CORREGIDO`), `revisadaPor`, `revisadaEn` (ms) y `comentarioSupervisor`.
La app del operario debe escuchar esos cambios para actualizar la muestra en su teléfono.

## Cuentas y seguridad

- **Supervisor (portal):** entra con **RUT y PIN**, igual que en la app. Por dentro es una cuenta de correo y contraseña de
  **Firebase Authentication**: el RUT se convierte en el correo `{rut sin puntos}@musselapp.cl` (ficticio, no recibe
  correos) y el PIN en la contraseña `pin-{PIN}`, porque Firebase pide al menos 6 caracteres. Por ejemplo, el RUT
  `22.222.222-2` corresponde a la cuenta `22222222-2@musselapp.cl`; con K, en minúscula (`12345678-k@musselapp.cl`). Para que una cuenta sea supervisor
  hay que crear, en la consola de Firebase, el documento `supervisores/{uid}` con el campo `nombre` (es el nombre que
  queda en `revisadaPor`). Una cuenta que no está en esa colección no puede entrar al portal.
- **Teléfono (app):** inicia una **sesión anónima** de Firebase sin pedirle nada al operario (el RUT y el PIN de la app
  siguen siendo locales). Cada muestra y foto que sube lleva el uid de esa sesión en `ownerUid`, y el teléfono solo
  escucha sus propias muestras. Las muestras subidas antes de la sesión se marcan como suyas la primera vez que la
  app se conecta.
- **Reglas:** están en [`firestore.rules`](../firestore.rules) y se prueban con el emulador de Firestore en GitHub Actions
  (`firestore-tests/`, `npm test`). En resumen: sin sesión no se lee nada; cada teléfono lee, crea y actualiza solo lo
  suyo, no puede eliminar ni cambiar un dictamen que ya está en la nube; el supervisor lee, dictamina y elimina todo.
- La `apiKey` del HTML no es secreta: identifica al proyecto, y lo que protege los datos son las reglas.

Orden para activar la seguridad en el proyecto (hecho una vez):
1. En **Authentication → Método de acceso**, activar **Anónimo** y **Correo electrónico/contraseña**.
2. Crear la cuenta del supervisor en **Authentication → Usuarios** (correo `{rut}@musselapp.cl`, contraseña
   `pin-` seguido del PIN del supervisor) y el documento `supervisores/{uid}` en Firestore con `nombre: "Diego Muñoz"`.
3. Instalar la versión nueva de la app en los teléfonos y abrirla una vez con internet.
4. Publicar `firestore.rules` (consola → Firestore → Reglas, o `firebase deploy --only firestore:rules`).
   Si algo falla, volver a pegar las reglas anteriores.
