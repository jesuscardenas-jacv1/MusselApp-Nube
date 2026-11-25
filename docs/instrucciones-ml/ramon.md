# Instrucciones para Ramón Osorio · UI/UX & Mobile Developer

Fase final de MusselApp (conteo con ML). Antes, lee [`equipo.md`](equipo.md). Reglas para tu asistente: [`CLAUDE.md`](../../CLAUDE.md).

> **Tu misión:** que la app pueda **recibir las detecciones de un modelo**, guardarlas en Room y mostrarlas en la pantalla de conteo para que el operario las corrija. Empiezas con un **detector simulado**, sin esperar al modelo real.

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
7. Al guardar la muestra, también se guardan las detecciones y el resumen en Room.

Con `CONTEO_ML = false`, la app debe quedar **idéntica a la v1.3.2**. Extiende `ConteoSobreFotoUiTest.kt` con: cambiar especie, agregar y eliminar.

## Paso 10 · Modelo real (≈6 h) · Mar 1 – Mié 2 dic

1. Lucas te entrega `model_v1.tflite`, `labels.txt` y `modelo.json` → van en `app/src/main/assets/ml/`.
2. `DetectorLiteRt.kt` implementa `DetectorMuestra`. Lucas te confirma la librería (LiteRT o MediaPipe Tasks) y su versión.
3. En `AppContainer.kt`, `DetectorSimulado()` pasa a ser `DetectorLiteRt(context)`.
4. Meta: **≤ 1,5 s** en un teléfono de gama media.

> **Ojo con la entrada del modelo (RF-DETR Nano):** float32 normalizado estilo ImageNet, no uint8. Detalle en `CONTRATO.md` del repo `MusselApp-ML`.

## Lo que no te toca

`NubeSync.kt` y `firestore.rules` son de Lucas · `web-supervisor/` es de Jesús.

## Checklist antes de cada PR

- [ ] `./gradlew assembleDebug testDebugUnitTest` en verde.
- [ ] `./gradlew compileDebugAndroidTestKotlin` compila.
- [ ] Probado en el emulador, con y sin "Contar automáticamente".
- [ ] **No** correr las pruebas instrumentadas en tu teléfono personal: desinstalan la app y borran las muestras.
