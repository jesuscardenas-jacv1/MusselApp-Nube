# Auditoría de seguridad · MusselApp

**Fecha:** 24-11-2026. **Commit auditado:** `ed1840f` (`main`, versión 1.3.2 más el cambio de color de Corregido).
**Alcance:** app Android, reglas de Firestore, portal web del supervisor y backend Spring Boot.
**Estado:** solo análisis. **No se modificó código de la app**; este archivo es el único cambio.

> El prompt de la auditoría llegó **cortado** en "A) 5. Input sanity". Este documento cubre la Fase 1, la Fase 1.5 y los ítems A1–A5. Las listas B, C y D se agregarán cuando llegue el resto del prompt.

---

## Fase 1 · Reconocimiento

### Tecnologías
| Qué | Valor | Dónde |
|---|---|---|
| Lenguaje y UI | Kotlin 2.2.10 con Jetpack Compose | `gradle/libs.versions.toml` |
| Gradle / AGP | AGP 9.3.3 | `gradle/libs.versions.toml:2` |
| SDK | minSdk 24, targetSdk 37 | `app/build.gradle.kts:29-30` |
| Firebase | BoM 34.0.0: **Firestore** y **Auth** (sin Storage, sin App Check, sin Functions) | `app/build.gradle.kts:95-98` |
| Ofuscación | R8 activado en release, con reglas propias | `app/build.gradle.kts:61`, `app/src/main/keepRules/rules.keep` |
| Base local | Room (SQLite) v7 con migraciones | `data/local/` |
| Backend | Spring Boot con Swagger y base H2. La app lo usa **solo en la versión debug** | `backend/`, `AppContainer.kt:52` |
| Portal web | Un solo `index.html` (JS y CSS sin framework) en Firebase Hosting. Usa el SDK de Firebase directo, sin API propia ni Cloud Functions | `web-supervisor/index.html`, `firebase.json` |

### Cómo funciona el ingreso
- **App (operario y supervisor):** RUT y PIN **validados en el teléfono** contra Room. El PIN se guarda como hash PBKDF2 con sal y 10.000 iteraciones (`util/HashPin.kt:18-19`). Tras 5 fallos seguidos el ingreso se bloquea y la espera crece (`BloqueoLogin.kt:47-48`).
- **App → nube:** una **sesión anónima** de Firebase (`NubeSync.kt:304`). Cada documento que sube lleva el uid en `ownerUid`.
- **Portal (supervisor):** el RUT se convierte en correo (`{rut}@musselapp.cl`) y el PIN en contraseña (`"pin-" + PIN`). Entra con correo y contraseña de Firebase y además debe existir `supervisores/{uid}` (`index.html:3182-3183, 3209, 3234`).

### Archivos de seguridad
| Archivo | ¿Existe? | Comentario |
|---|---|---|
| `firestore.rules` | Sí | Con pruebas en `firestore-tests/` que corren en el CI (`reglas-firestore.yml`, último resultado ✔). |
| `storage.rules` | No | No se usa Firebase Storage: las fotos van en la colección `fotos` de Firestore. |
| `firebase.json` | Sí | Reglas, emulador y Hosting. |
| `AndroidManifest.xml` | Sí | `allowBackup="false"`, una sola actividad exportada (el lanzador) y el FileProvider no exportado. |
| `proguard-rules.pro` | No | Se usa `keepRules/rules.keep`, que además elimina `Log.v/d/i` del release (`rules.keep:32`). |

### Inventario de accesos a Firestore (lo que debe seguir funcionando)
| # | Quién | Colección | Operación | Dónde |
|---|---|---|---|---|
| 1 | Teléfono (anónimo) | — | Iniciar sesión anónima | `NubeSync.kt:304` |
| 2 | Teléfono | `muestras/{inst_id}` | Transacción: `get` y luego `set` con merge (subir y actualizar) | `NubeSync.kt:194-208` |
| 3 | Teléfono | `fotos/{inst_id}` | `set` dentro de la misma transacción | `NubeSync.kt:210` |
| 4 | Teléfono | `muestras` | Escucha filtrada `where ownerUid == uid` (dictámenes del supervisor) | `NubeSync.kt:263` |
| 5 | Teléfono | `muestras`, `fotos` | Reclamar documentos antiguos sin dueño (`get` y luego `set` con merge en lote) | `NubeSync.kt:342-355` |
| 6 | Portal | `supervisores/{uid}` | `get` (¿es supervisor?) | `index.html:3209` |
| 7 | Portal | `muestras` | Escucha de toda la colección | `index.html:1480` |
| 8 | Portal | `fotos/{id}` | `get` al abrir una muestra | `index.html:1503` |
| 9 | Portal | `muestras/{id}` | Transacción de dictamen (validar o corregir) | `index.html:1505-1507` |
| 10 | Portal | `muestras`, `fotos` | Eliminar muestras | `index.html:1524-1525` |
| 11 | Portal | `muestras`, `fotos` | Crear muestras de prueba ("Plan B") en lote | `index.html:1534-1548` |
| 12 | Portal | `muestras` | Borrar el historial o las muestras de prueba (`where historico/demo`) | `index.html:1554-1555` |
| 13 | App (solo debug) | Backend `127.0.0.1:8080` | REST (Retrofit) | `BackendSync.kt:216`, `BackendApi.kt:38` |

---

## Fase 1.5 · Línea base (antes de cualquier cambio)

| Prueba | Resultado |
|---|---|
| `./gradlew assembleDebug` | ✔ compila |
| `./gradlew assembleRelease` | ✔ compila (firmado con la llave local) |
| Pruebas unitarias de la app (`testDebugUnitTest`) | ✔ 200 de 200 |
| Pruebas instrumentadas | ✔ compilan; corren en el CI (`android.yml`) |
| Lint | ✔ sin errores (41 advertencias, casi todas por versiones nuevas de librerías) |
| Pruebas de las reglas de Firestore | ✔ último CI (`reglas-firestore.yml`, `3052f39`). No se corren aquí: necesitan el emulador de Firebase |
| Pruebas del backend | ✔ último CI (`backend.yml`, `2598d45`). No se corren aquí: Maven no está instalado |

**No había nada fallando antes de empezar.**

### Funciones que deben seguir funcionando (checklist de no-regresión)
- [ ] **App:** ingreso con RUT y PIN (operario y supervisor), bloqueo tras 5 fallos y salir de la sesión.
- [ ] **App:** registrar una muestra (formulario, cámara o galería, vista previa, conteo con marcas y tramo) y guardarla sin internet.
- [ ] **App:** la muestra se sube sola a Firestore al volver internet (al guardar, al reconectar y con WorkManager).
- [ ] **App (supervisor):** validar o corregir en el teléfono; el dictamen se sube y no pisa uno ya hecho en el portal.
- [ ] **App:** recibir el dictamen del portal (aviso y notificación) y aplicar los valores corregidos.
- [ ] **App:** ver el Historial, el Detalle y la ficha PDF, y compartir.
- [ ] **App (debug):** sincronizar con el backend local por `adb reverse`.
- [ ] **Portal:** ingreso con RUT y PIN, ver la lista en vivo y las fotos, validar o corregir (con edición de valores) y eliminar.
- [ ] **Portal:** Dashboard, Datos, exportación, Historial, Mapa, modo ejemplo y "Plan B".

---

## Fase 2 · Auditoría

### A) Controles base

**A1 · Rate limiting (límite de intentos)**
*Qué es:* impedir que alguien haga miles de intentos o peticiones seguidas, por ejemplo para adivinar un PIN o llenar la base de basura.

| Parte | Estado | Evidencia |
|---|---|---|
| PIN en la app (Room) | ✔️ **Ya cumple** | 5 fallos y luego un bloqueo que crece (`BloqueoLogin.kt:47-48`); hash PBKDF2 (`HashPin.kt:18-19`) |
| Login del portal (Firebase Auth) | ✔️ Firebase lo limita | Firebase Auth corta tras muchos fallos (`auth/too-many-requests`, manejado en `index.html:3201`) |
| Peticiones a Firestore desde fuera de la app | ❌ **No existe** | No hay **App Check**. Cualquiera con la configuración pública puede crear una sesión anónima y escribir muestras a su nombre, que el portal mostraría |

*Explicación:* App Check hace que Firebase solo acepte peticiones de **tu** app (con Play Integrity) y de **tu** portal (con reCAPTCHA).
⚠ **Activarlo puede romper la app** si se fuerza antes de registrar todo: el APK de debug, el del CI y el que se instala a mano no pasan Play Integrity sin configuración extra. **Propuesta segura:** agregar App Check solo en *modo monitoreo* (sin forzar) y decidir con datos. Queda **pendiente de tu aprobación**.

**A2 · Archivo .env (secretos en el código)**
*Qué es:* contraseñas o llaves escritas en el código o subidas a git por error.

| Qué | Estado | Evidencia |
|---|---|---|
| `local.properties`, `keystore.properties`, `*.jks`, `*.keystore` | ✔️ **Ya cumple** | En `.gitignore`. El historial de git no tiene ninguno; la llave `.jks` está fuera del repo |
| `app/google-services.json` (en git) | ✔️ No es secreto | Trae la *API key* de Firebase, que identifica el proyecto pero **no da permisos**: la seguridad real está en `firestore.rules`. Google lo documenta así |
| Configuración web en `index.html:1047-1049` | ✔️ No es secreto | Mismo caso: es pública por diseño |
| Restringir las API keys en Google Cloud | ❌ **No verificado** | Conviene limitar la key Android al paquete y la huella SHA-1, y la web al dominio `musselapp-demo.web.app`. Se hace en la consola; no se puede ver desde el repo |
| Backend: contraseña de H2 vacía (`application.properties:11`) | ✔️ Aceptable | La base es local y el servidor solo escucha en `127.0.0.1` (`application.properties:4`) |

**A3 · Secretos en la plataforma**
*Qué es:* cuentas de servicio o llaves de administrador guardadas en el repo o en el CI.

| Qué | Estado | Evidencia |
|---|---|---|
| Cuentas de servicio o llaves admin | ✔️ **Ya cumple** | No hay ninguna en el repo ni en el historial. El portal no usa el Admin SDK |
| GitHub Secrets | ✔️ **Ya cumple** | Los workflows no usan secretos; `release.yml` solo pide el permiso `contents: write` (`release.yml:16`) |

**A4 · IP limiting (bloquear por dirección IP)**
*Qué es:* aceptar o bloquear según desde dónde llega la petición.
- ➖ **No aplica a Firebase:** no tiene servidor propio donde filtrar IPs. Su equivalente es App Check (A1).
- ✔️ **Backend:** escucha solo en `127.0.0.1` (`application.properties:4`). Nadie de la red puede conectarse; el teléfono llega por USB con `adb reverse`.
- ✔️ **Tráfico sin cifrar:** la app release no permite HTTP. En debug solo se permite hacia `127.0.0.1`, `localhost` y `10.0.2.2` (`app/src/debug/res/xml/seguridad_red.xml`).

**A5 · Input sanity (validar lo que entra)**
*Qué es:* revisar que los datos tengan el tipo y el tamaño correctos antes de guardarlos.

| Capa | Estado | Evidencia |
|---|---|---|
| Formularios de la app | ✔️ **Ya cumple** | Rangos de conteo, talla, cholgas y tramo (`BorradorMuestra.kt:56-62`), observación de hasta 300 caracteres (`VistaPreviaScreen.kt:125`), RUT con módulo 11 (`RutValidator.kt:10`) |
| Formularios del portal | ✔️ Cumple en el navegador | Calibre 1-100 mm, conteo y comentario mínimo al corregir (`index.html`, validación al corregir) |
| Backend | ✔️ **Ya cumple** | `@Valid` con límites en los DTO (`MuestraRequest`, `RevisionRequest`, `FotoRequest`) |
| Reglas de Firestore | ❌ **Falta** | Controlan **quién** escribe, pero no **qué**: no revisan tipos, rangos ni tamaño. Una sesión anónima fuera de la app podría escribir un conteo negativo, un texto gigante o campos extra |

*Propuesta (pendiente de aprobación):* agregar a `firestore.rules` una validación del esquema de `muestras` (tipos y rangos de los campos que hoy escribe la app) y un tamaño máximo para `fotos.fotoBase64`, con pruebas nuevas en `firestore-tests/` antes de publicar.
⚠ Si la regla olvida un campo que la app escribe, **la subida falla**. Por eso solo se aplicaría con las pruebas en verde y revisando el inventario de arriba.

### Hallazgo adicional (fuera de la lista, se informa)
| Hallazgo | Riesgo | Evidencia |
|---|---|---|
| La contraseña del supervisor en Firebase es `"pin-" + PIN` (4 o más dígitos), y el correo se deduce del RUT. Quien conozca el RUT solo debe adivinar el PIN (10.000 combinaciones con 4 dígitos). Firebase frena los intentos seguidos, pero no evita un ataque lento desde muchas IP | Medio | `index.html:3182-3183, 3227` |

*Opciones, sin aplicar:* (a) exigir un PIN de 6 o más dígitos para las cuentas de supervisor; (b) activar App Check en el portal; (c) activar la protección contra enumeración de correos en Firebase Auth.
⚠ La (a) cambia cómo entra el supervisor: hay que coordinarla con el equipo.

---

## Resumen de estado
| Ítem | Estado |
|---|---|
| A1 Rate limiting | ✔️ PIN y Auth · ❌ falta App Check |
| A2 .env / secretos | ✔️ · ❌ sin verificar las restricciones de las API keys |
| A3 Secretos en la plataforma | ✔️ |
| A4 IP limiting | ➖ Firebase · ✔️ backend y red |
| A5 Validación de entrada | ✔️ app, portal y backend · ❌ reglas de Firestore |
| Contraseña del supervisor en el portal | ⚠ riesgo medio |

**Cambios aplicados al código: ninguno.** Las propuestas esperan tu aprobación y el resto del prompt (listas B, C y D).
