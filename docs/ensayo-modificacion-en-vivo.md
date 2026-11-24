# Ensayo de la modificación en vivo (Backend Spring Boot + App Android)

En la defensa EA5 (IE 3.1.3, **40 %**) se pide agregar en vivo un campo o regla (por ejemplo **peso de la muestra en gramos**, `pesoG`) que atraviese **backend Spring Boot, base de datos, Retrofit/Repository, ViewModel y Compose**, y verificarlo tanto en **Swagger UI** como en el **teléfono**.

**Meta:** cada integrante lo completa solo, cronometrado, en menos de 5 minutos.

**Rama de ensayo:** el cambio de práctica **no se fusiona**. Se ensaya en una rama aparte y luego se borra:

```bash
git checkout -b ensayo/peso-<nombre>
# ... hacer los pasos ...
git checkout feature/backend-springboot && git branch -D ensayo/peso-<nombre>
```

> Referencia rápida: el campo `cholgasIndM` ya recorre todas las capas. Si te pierdes en la app o en el backend, busca `cholgasIndM` en el proyecto y repite cada aparición.

---

## Parte 1 — Backend Spring Boot (`backend/`, ~1 min)

Con `spring.jpa.hibernate.ddl-auto=update`, Hibernate agrega la columna automáticamente en H2 al reiniciar el servidor.

| # | Archivo (`backend/src/main/java/cl/aldemar/musselapp/backend/`) | Qué agregar |
|---|---|---|
| 1 | `entidad/Muestra.java` | `@Column(name = "peso_g") private Integer pesoG;` + getter y setter (`getPesoG` / `setPesoG`) |
| 2 | `dto/MuestraRequest.java` | `@Min(1) @Max(50000) Integer pesoG,` (con `@Schema(example = "450")`) |
| 3 | `dto/MuestraResponse.java` | `Integer pesoG,` en el record y `m.getPesoG(),` en `MuestraResponse.desde(m)` |
| 4 | `servicio/MuestraService.java` | `m.setPesoG(req.pesoG());` dentro de `aplicarCampos(...)` |

Reiniciar Spring Boot (`mvn -f backend/pom.xml spring-boot:run`) y comprobar en **http://localhost:8080/swagger-ui.html** que `POST /api/muestras` y `GET /api/muestras` ya muestran `pesoG`.

---

## Parte 2 — App Android (`app/`, ~3 min)

| # | Capa | Archivo (`app/src/main/java/cl/aldemar/musselapp/`) | Qué agregar |
|---|---|---|---|
| 1 | Entidad Room | `data/local/entidad/MuestraEntity.kt` | `@ColumnInfo(name = "peso_g") val pesoG: Int? = null,` |
| 2 | Versión BD | `data/local/MusselDatabase.kt` | `version = 7` → `version = 8` |
| 3 | Migración | `data/local/Migraciones.kt` | `DE_7_A_8` con `ALTER TABLE muestras ADD COLUMN peso_g INTEGER` y agregarla a `TODAS` |
| 4 | Modelo | `data/model/Muestra.kt` | `val pesoG: Int? = null,` |
| 5 | Room → modelo | `data/local/Mapeos.kt` (`aModelo`) | `pesoG = muestra.pesoG,` |
| 6 | Modelo → Room | `data/repository/RoomMuestraRepository.kt` (al insertar y en `actualizarDesdeRemoto`) | `pesoG = muestra.pesoG,` |
| 7 | Retrofit DTOs | `data/nube/BackendApi.kt` y `data/nube/BackendSync.kt` | `val pesoG: Int? = null` en `MuestraEnviarDto`, `MuestraRemotaDto` y `CambioRemoto`, y pasarlo en `subir()` / `descargarCambios()` |
| 8 | ViewModel | `ui/muestra/BorradorMuestra.kt` | `val pesoG: Int? = null`, `actualizarPeso(pesoG: Int?)` y `pesoG = actual.pesoG` en `confirmar` |
| 9 | Compose (entrada) | `ui/muestra/VistaPreviaScreen.kt` | campo numérico junto a talla/cholgas llamando a `actualizarPeso` |
| 10 | Compose (lectura) | `ui/detalle/DetalleScreen.kt` | mostrar `"Peso: ${muestra.pesoG} g"` si no es null |

### Código de la migración (paso 3)

```kotlin
/** v7 → v8: peso de la muestra en gramos (opcional). */
val DE_7_A_8 = object : Migration(7, 8) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE muestras ADD COLUMN peso_g INTEGER")
    }
}

val TODAS = arrayOf(DE_1_A_2, DE_2_A_3, DE_3_A_4, DE_4_A_5, DE_5_A_6, DE_6_A_7, DE_7_A_8)
```

### Si en vez de un campo piden una regla de validación

- **En Spring Boot (`MuestraRequest.java` o `MuestraService.java`)**: agregar `@Min`/`@Max` o lanzar `ResponseStatusException(HttpStatus.BAD_REQUEST, ...)`.
- **En Android (`ui/muestra/BorradorMuestra.kt`, en `validarBorrador`)**: agregar la condición con mensaje claro y comprobarla en `BorradorConfirmarTest.kt`.

---

## Verificación en vivo (< 1 min)

1. Con el backend corriendo y `adb reverse tcp:8080 tcp:8080` activo:
   - Instalar la app (`./gradlew installDebug`) **sin desinstalar** (demuestra la migración v7 → v8 conservando muestras previas).
   - Crear una muestra con `pesoG` en el teléfono → verificar en **Swagger (`GET /api/muestras`)** que llegó el `pesoG`.
   - Editar `pesoG` con `PUT /api/muestras/{id}` en **Swagger** → pulsar **Sincronizar** en la app → abrir el Detalle y verificar el nuevo valor.

## Errores comunes

- **"A migration from 7 to 8 was required but not found"**: faltó sumar `DE_7_A_8` a `TODAS` en `Migraciones.kt`.
- **"Room cannot verify the data integrity"**: cambiaste `MuestraEntity` sin subir `version` a `8`.
- **Llega `null` en Swagger**: faltó agregar `pesoG` en `MuestraEnviarDto` (`BackendApi.kt`) o en `aplicarCampos` (`MuestraService.java`).
