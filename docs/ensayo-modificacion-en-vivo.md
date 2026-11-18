# Ensayo de la modificación en vivo (parte app)

En la defensa EA5 (IE 3.1.3, 40 %) se pide agregar en vivo un campo o una regla, por ejemplo el **peso de la muestra**, y verificarlo en la app y en el backend. Esta guía cubre la **parte app**. La entidad JPA, el DTO, el endpoint y Swagger se ensayan con el equipo del backend.

**Meta:** cada integrante lo hace solo, cronometrado, en menos de 5 minutos.

**Rama de ensayo:** el cambio **no se fusiona**. Se practica en una rama aparte, por ejemplo `ensayo/peso-<nombre>`, y después se borra la rama:

```bash
git checkout -b ensayo/peso-lucas
# ... hacer los pasos ...
git checkout main && git branch -D ensayo/peso-lucas
```

El campo de cholgas (`cholgasIndM`, migración 3 → 4) se agregó exactamente así. Si te pierdes, busca `cholgasIndM` en el proyecto y repite cada lugar donde aparece.

## Pasos (campo `pesoG`: peso de la muestra en gramos, opcional)

| # | Capa | Archivo | Qué agregar |
|---|---|---|---|
| 1 | Entidad de Room | `data/local/entidad/MuestraEntity.kt` | `@ColumnInfo(name = "peso_g") val pesoG: Int? = null,` |
| 2 | Versión de la base | `data/local/MusselDatabase.kt` | `version = 5` → `version = 6` |
| 3 | Migración | `data/local/Migraciones.kt` | `DE_5_A_6` con `ALTER TABLE muestras ADD COLUMN peso_g INTEGER`, y agregarla a `TODAS` |
| 4 | Modelo | `data/model/Muestra.kt` | `val pesoG: Int? = null,` |
| 5 | Room → modelo | `data/local/Mapeos.kt` (`aModelo`) | `pesoG = muestra.pesoG,` |
| 6 | Modelo → Room | `data/repository/RoomMuestraRepository.kt` (al insertar, junto a `cholgasIndM`) | `pesoG = muestra.pesoG,` |
| 7 | Borrador (ViewModel) | `ui/muestra/BorradorMuestra.kt` | campo `pesoG` en `BorradorMuestra`, función `actualizarPeso(pesoG: Int?)` y `pesoG = actual.pesoG` en `confirmar` |
| 8 | Pantalla (Compose) | `ui/muestra/VistaPreviaScreen.kt` | un campo numérico junto a talla y cholgas que llame a `actualizarPeso` |
| 9 | Mostrarlo | `ui/detalle/DetalleScreen.kt` y `util/FichaMuestra.kt` | una línea "Peso: … g" |
| 10 | Nube (opcional) | `data/nube/NubeSync.kt` | `"pesoG" to m.pesoG,` en el mapa `datos` |

### Código de la migración (paso 3)

```kotlin
/** v5 → v6: peso de la muestra en gramos. Las muestras anteriores quedan con null (no se pesó). */
val DE_5_A_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE muestras ADD COLUMN peso_g INTEGER")
    }
}

val TODAS = arrayOf(DE_1_A_2, DE_2_A_3, DE_3_A_4, DE_4_A_5, DE_5_A_6)
```

Al compilar, Room exporta el esquema nuevo en `app/schemas/.../6.json`, y ese archivo se versiona.

### Regla de validación (si piden una regla en vez de un campo)

En `validarBorrador` (`ui/muestra/BorradorMuestra.kt`), antes del `else -> null`:

```kotlin
borrador.pesoG != null && borrador.pesoG !in 1..PESO_MAXIMO_G -> "El peso debe estar entre 1 y $PESO_MAXIMO_G g"
```

Y una prueba en `app/src/test/.../BorradorConfirmarTest.kt` que compruebe el mensaje.

## Verificación

1. `./gradlew assembleDebug testDebugUnitTest`: compila y las pruebas siguen en verde.
2. Instalar sobre la versión anterior, **sin desinstalar**. Las muestras antiguas siguen ahí, con lo que se demuestra la migración.
3. Registrar una muestra con peso y verlo en el Detalle.
4. (Si hay tiempo) `MigracionesTest` en `app/src/androidTest`: copiar la prueba de 3 → 4 y adaptarla a 5 → 6.

## Errores comunes

- **"A migration from 5 to 6 was required but not found"**: falta agregar `DE_5_A_6` a `TODAS`.
- **"Room cannot verify the data integrity"**: se cambió la entidad sin subir `version`.
- **El campo no se guarda**: falta el paso 6, porque el modelo llega al repositorio pero no a la entidad.
- **No aparece en el portal**: falta el paso 10, o el portal no lo muestra (es otro archivo, `web-supervisor/index.html`).
