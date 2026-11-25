# Instrucciones para Ramón Osorio · UI/UX & Mobile Developer

Fase final de MusselApp (conteo con ML). Antes, lee [`equipo.md`](equipo.md) y el estado actual en [`estado-ml.md`](../estado-ml.md). Reglas para tu asistente: [`CLAUDE.md`](../../CLAUDE.md).

> **Ya hecho por Lucas (PR #55, ya fusionado):** `SalidaRfDetr.kt` (lee la salida del modelo) y `Calibre.kt` (mm).
> Están en `feature/ml-conteo`: `git merge origin/feature/ml-conteo`. No los modifiques; si algo no calza, avísale a Lucas.

> **Tu misión:** que la app pueda **recibir las detecciones de un modelo**, guardarlas en Room y mostrarlas en la pantalla de conteo para que el operario las corrija. Empiezas con un **detector simulado**, sin esperar al modelo real.

> **Regla del caso ALDEMAR (no negociable):** el conteo con ML se presenta como **experimental**. Nunca decir ni
> escribir que la app "cuenta automáticamente" de forma real o validada: el operario siempre revisa y corrige, y en la
> demo se muestra el error contra conteos a mano. Datos solo ficticios o autorizados; nada interno de ALDEMAR en el
> repo, videos o presentaciones.

En la app: el título va como **"Conteo automático (experimental)"** y el operario siempre confirma antes de guardar.

## Si tienes pocos tokens: por dónde partir

1. **Pasos 4 y 5 primero** (detector simulado y Room v8): Lucas los necesita para la subida a Firestore, y Jesús no
   depende de ti. El paso 6 (pantalla) puede esperar al fin de semana.
2. Pídele a tu asistente que lea **solo** `CLAUDE.md`, `docs/estado-ml.md` y este archivo; no hace falta el plan entero.
3. Una tarea por sesión ("haz el paso 5 y su prueba de migración"), y al cerrar, que anote en la bitácora en qué quedó:
   así la próxima sesión parte sin releer todo.
4. El código de este archivo ya trae el SQL y las clases listas para copiar; úsalas tal cual.

## Antes de empezar

- [ ] `git fetch origin` y `git switch -c feature/ml-app origin/feature/ml-conteo`.
- [ ] `./gradlew assembleDebug testDebugUnitTest` en verde (deben pasar 200 pruebas) antes de tocar nada.
- [ ] Los PR van hacia `feature/ml-conteo`, nunca a `main`. Commits en español y sin coautor de IA.

## Paso 4 · Detector simulado (≈5 h) · Mié 25 – Vie 27 nov

Crea `app/src/main/java/cl/aldemar/musselapp/data/ml/`:

```kotlin
// Deteccion.kt
enum class Especie { CHORITO, CHOLGA, REFERENCIA }
/** Recuadro normalizado 0..1 respecto del ancho y alto de la foto. */
data class Deteccion(val especie: Especie, val confianza: Float,
                     val x: Float, val y: Float, val ancho: Float, val alto: Float)
data class ResultadoDeteccion(val detecciones: List<Deteccion>, val modeloVersion: String, val milisegundos: Long)

// DetectorMuestra.kt
interface DetectorMuestra { suspend fun detectar(foto: android.graphics.Bitmap): ResultadoDeteccion }

// DetectorSimulado.kt → ~40 detecciones repartidas (90 % chorito, 10 % cholga) + 1 referencia en una esquina;
//                       modeloVersion = "simulado"
```

- En `app/build.gradle.kts`: `buildFeatures { buildConfig = true }` y `buildConfigField("boolean", "CONTEO_ML", ...)`, con `true` en **debug** y `false` en **release**.
- En `AppContainer.kt`: `val detector: DetectorMuestra = DetectorSimulado()`.
- **Listo cuando** hay pruebas unitarias del simulado (cantidad, especies y recuadros dentro de 0..1).

## Paso 5 · Room 7 → 8 (≈5 h) · Mié 25 – Vie 27 nov

1. `data/local/entidad/DeteccionEntity.kt` (tabla `detecciones`) y `data/local/dao/DeteccionDao.kt`: insertar lista, leer por muestra y borrar por muestra.
2. `MuestraEntity.kt`: columnas opcionales `ml_modelo_version`, `ml_total`, `ml_choritos`, `ml_cholgas`, `ml_mm_por_px` y `ml_editado`.
3. `Migraciones.kt`: `DE_7_A_8`, agregada a `TODAS`. `MusselDatabase.kt`: `version = 8` y `DeteccionEntity::class` en `entities`.

```kotlin
val DE_7_A_8 = object : Migration(7, 8) {
    override fun migrate(db: SupportSQLiteDatabase) {
        listOf("ml_modelo_version TEXT", "ml_total INTEGER", "ml_choritos INTEGER", "ml_cholgas INTEGER",
               "ml_mm_por_px REAL", "ml_editado INTEGER NOT NULL DEFAULT 0")
            .forEach { db.execSQL("ALTER TABLE muestras ADD COLUMN $it") }
        db.execSQL("""CREATE TABLE IF NOT EXISTS detecciones (
            id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, muestra_id INTEGER NOT NULL,
            especie TEXT NOT NULL, especie_modelo TEXT, confianza REAL,
            x REAL NOT NULL, y REAL NOT NULL, ancho REAL NOT NULL, alto REAL NOT NULL,
            largo_mm REAL, origen TEXT NOT NULL, eliminada INTEGER NOT NULL DEFAULT 0,
            FOREIGN KEY(muestra_id) REFERENCES muestras(id) ON DELETE CASCADE)""")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_detecciones_muestra_id ON detecciones(muestra_id)")
    }
}
```

- `especie_modelo` guarda lo que dijo el modelo; `especie`, lo que quedó después de la corrección. `origen` es `MODELO` u `OPERARIO`. `eliminada = 1` marca las que quitó el operario (no se borran: sirven para reentrenar).
- La entidad debe calzar **exacto** con el SQL. Sube el `app/schemas/.../8.json` que se genera.
- Prueba en `app/src/androidTest/.../MigracionesTest.kt` (ya usa `MigrationTestHelper`): crea la base en la versión 7 con una muestra, migra a 8 y comprueba que la muestra sigue ahí.
- **No** cambies el significado de `conteoIndM`, `cholgasIndM` ni `tallaMm`.

## Paso 6 · Pantalla de revisión (≈8 h) · Sáb 28 – Lun 30 nov

Se reutiliza el conteo con marcas que ya existe (`ui/muestra/ConteoMarcas.kt` y `ConteoSobreFoto.kt`):

1. En `VistaPreviaScreen.kt`, si `BuildConfig.CONTEO_ML` está activo y hay foto: botón **"Contar automáticamente"**.
2. El detector corre en `withContext(Dispatchers.Default)` con un indicador de carga, y se abre `ConteoSobreFoto` con las marcas puestas.
3. Colores: chorito `#0F766E`, cholga `#C76C05`. La referencia va como recuadro azul y no se cuenta.
4. **Tocar** una marca: chorito → cholga → eliminar. **Mantener presionado**: agrega una marca de la especie elegida en los chips "Agregar: Chorito | Cholga".
5. Resumen abajo: "58 choritos · 6 cholgas". "Usar el conteo" llena `conteoIndM` y `cholgasIndM` (por metro, con el tramo).
6. Si la confianza promedio es menor a 0,55 o no se detectó la referencia: aviso **"Revisa el conteo"**.
7. **Calibre:** con el recuadro de la `REFERENCIA` (en píxeles de la foto original), `Calibre.mmPorPixel(anchoPx, altoPx)`
   da los mm por píxel (null si no es un cuadrado: entonces no hay calibre y se muestra el aviso). Para cada chorito o
   cholga, recorta su recuadro, pásalo a luminancia 0..255 (`0.299 R + 0.587 G + 0.114 B`) y llama a
   `Calibre.largoMm(luminancia, anchoRecorte, altoRecorte, mmPorPx)`; el resultado va en `largo_mm`. Hazlo en
   `Dispatchers.Default`. Si `Calibre.esBorrosa(...)` sobre la foto completa da `true`, muestra "La foto está borrosa".
8. Al guardar la muestra, también se guardan las detecciones y el resumen en Room. Lucas las sube a Firestore desde
   `NubeSync` con `ResultadoMlNube.aCampos(...)`: guarda `especie`, `confianza`, `x, y, ancho, alto` (0..1), `largo_mm`,
   `origen` y `eliminada` tal como dice el paso 5, y con eso basta.

Con `CONTEO_ML = false`, la app debe quedar **idéntica a la v1.3.2**. Extiende `ConteoSobreFotoUiTest.kt` con: cambiar especie, agregar y eliminar.

## Paso 10 · Modelo real (≈6 h) · Mar 1 – Mié 2 dic

> **Puedes adelantarlo ya:** hay un modelo de prueba con el mismo formato que tendrá el real, entrenado con fotos
> dibujadas: https://github.com/lucsaldivia-ux/MusselApp-ML/releases/tag/model-v0-sintetico
> (`model_v0_sintetico.tflite`, `labels.txt`, `modelo.json` y una foto de prueba). No cuenta choritos reales: es solo para integrar.

1. Copia los archivos a `app/src/main/assets/ml/`. Cuando Lucas entregue `model_v1`, solo se reemplazan.
2. Agrega LiteRT al proyecto (`com.google.ai.edge.litert:litert`, última versión estable con al menos dos semanas).
3. `DetectorLiteRt.kt` implementa `DetectorMuestra`:
   - **Entrada** `[1, 384, 384, 3]` **float32**: la foto en RGB redimensionada a 384×384, cada canal `/255` y luego
     `(x − [0.485, 0.456, 0.406]) / [0.229, 0.224, 0.225]`. **No** es uint8.
   - **Salidas por posición** (en INT8 no tienen nombre y las dos miden `[1, 300, 4]`): salida 0 = puntajes, salida 1 = cajas.
   - Convierte con **`SalidaRfDetr.decodificar(cajas, puntajes, columnas = 4)`** (ya está hecho y probado, en `data/ml/`,
     PR #55). La especie sale de `SalidaRfDetr.etiqueta(clase, labels)`: 0 = chorito, 1 = cholga, 2 = referencia.
   - Las cajas vienen normalizadas 0..1, igual que `Deteccion`.
   - **Endereza la foto según su EXIF antes de detectar** (`ExifInterface`, `TAG_ORIENTATION`): el modelo se entrena
     con fotos derechas, y si la foto llega girada las cajas no calzan con lo que ve el operario.
4. En `AppContainer.kt`, `DetectorSimulado()` pasa a ser `DetectorLiteRt(context)`.
5. Prueba con `foto_prueba_sintetica.jpg`: deben salir 28 detecciones. Meta de tiempo: **≤ 1,5 s** en gama media.
   Con `Interpreter.Options().setNumThreads(4)` y **XNNPACK activo** (viene activo por defecto en LiteRT: no lo
   apagues), el INT8 tarda ~0,22 s en un PC; en el teléfono se espera menos de 1 s. Mide sin contar la primera
   inferencia (calienta el intérprete). Si pasa de 1,5 s, prueba con el delegado de GPU.
   *Corrección del 25-nov:* antes decía ~5 s; esa medición se hizo sin XNNPACK y no sirve.
6. Tamaño: el `.tflite` INT8 pesa ~30 MB, así que el APK crece eso.

## Lo que no te toca

`NubeSync.kt` y `firestore.rules` son de Lucas · `web-supervisor/` es de Jesús.

## Checklist antes de cada PR

- [ ] `./gradlew assembleDebug testDebugUnitTest` en verde.
- [ ] `./gradlew compileDebugAndroidTestKotlin` compila.
- [ ] Probado en el emulador, con y sin "Contar automáticamente".
- [ ] **No** correr las pruebas instrumentadas en tu teléfono personal: desinstalan la app y borran las muestras.
