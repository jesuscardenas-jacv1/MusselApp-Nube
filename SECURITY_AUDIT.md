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

*Hecho en la rama (⚠️, sin publicar):* `firestore.rules` valida ahora los campos, tipos y rangos de lo que escribe el teléfono, y el tamaño de `fotos.fotoBase64`. Ver la Fase 3.

### Hallazgo adicional (fuera de la lista, se informa)
| Hallazgo | Riesgo | Evidencia |
|---|---|---|
| La contraseña del supervisor en Firebase es `"pin-" + PIN` (4 o más dígitos), y el correo se deduce del RUT. Quien conozca el RUT solo debe adivinar el PIN (10.000 combinaciones con 4 dígitos). Firebase frena los intentos seguidos, pero no evita un ataque lento desde muchas IP | Medio | `index.html:3182-3183, 3227` |

*Opciones, sin aplicar:* (a) exigir un PIN de 6 o más dígitos para las cuentas de supervisor; (b) activar App Check en el portal; (c) activar la protección contra enumeración de correos en Firebase Auth.
⚠ La (a) cambia cómo entra el supervisor: hay que coordinarla con el equipo.

---

### A) Controles base, continuación

**A6 · Validación del lado del servidor**
*Qué es:* que el servidor revise los datos aunque el cliente ya los haya revisado, porque el cliente se puede saltar.
- ✔️ **Backend:** `@Valid` en los DTO.
- ❌ → ⚠️ **Firestore:** las reglas eran el único "servidor" y no validaban datos. Se corrigió en la rama (ver la Fase 3).

**A7 · Mínimo privilegio en las reglas**
*Qué es:* cada uno puede hacer solo lo que necesita.
- ✔️ **Ya cumple:**
  - `supervisores` solo se lee a sí mismo (`firestore.rules:91-93`).
  - El teléfono lee y lista solo lo suyo (`:101-103`) y no borra nada (`:119`).
  - Gana el primer dictamen (`:113-114`).
- ❌ → ⚠️ **Qué campos escribe el teléfono:** podía escribir cualquier campo. Ahora solo puede escribir los campos de la app (`camposDelTelefono`, `:46-51`).
- 🕒 **Rol del teléfono:** el supervisor de la app usa la misma sesión anónima que el operario, así que Firestore no puede distinguirlos (ver B).

**A8 · Cifrado**
*Qué es:* que los datos viajen y se guarden protegidos.
- ✔️ **En tránsito:**
  - Firestore y Auth usan HTTPS siempre.
  - La release no permite HTTP.
  - Debug permite HTTP solo hacia la propia máquina (`seguridad_red.xml`).
- ✔️ **En el teléfono:**
  - El PIN se guarda con hash PBKDF2 y sal (`HashPin.kt:18-19`).
  - `allowBackup="false"` (`AndroidManifest.xml:19`).
  - Las SharedPreferences (`nube`, `bloqueo_login`, `backend_sync`, `pantalla_principal`) usan `MODE_PRIVATE` y no guardan PIN ni tokens.
- ➖ **Cifrar Room completa:** los datos son de muestreo, no personales sensibles, y Room está protegida por el sandbox de Android. No se cambia (restricción: no tocar Room).

**A9 · Sesión**
*Qué es:* cómo se entra, se sale y se bloquea.
- ✔️ **App:**
  - Bloqueo tras 5 intentos (`BloqueoLogin.kt:47-48`).
  - Cerrar sesión limpia el estado (`LoginViewModel.kt:47`).
  - La sesión anónima de Firebase se conserva a propósito: es la identidad del teléfono (`ownerUid`).
- ✔️ **Portal:**
  - `signOut` al salir (`index.html:3218`).
  - Si la cuenta no está en `supervisores`, se cierra la sesión (`index.html:3267`).
  - `auth/too-many-requests` se maneja (`index.html:3201`).
- ❌ → ⚠️ **Política de contraseñas y protección contra enumeración de correos:** son ajustes de la consola de Firebase. No están en el código, así que no se pueden verificar desde aquí. Ver los pasos manuales.

**A10 · CORS**
*Qué es:* qué páginas web pueden llamar a una API.
- ➖ El portal no llama a ninguna API propia: usa el SDK de Firebase.
- ➖ El backend escucha solo en `127.0.0.1` y no tiene `@CrossOrigin` (`application.properties:4`).
- ✔️ El valor por defecto de Spring (sin CORS) es el más cerrado.

### B) Roles
| Pregunta | Respuesta | Evidencia |
|---|---|---|
| ¿Quién puede leer muestras ajenas? | Solo el supervisor del portal | `firestore.rules:97, 101-103`; prueba "el teléfono lee sus muestras y no las de otro" |
| ¿Quién puede dictaminar? | El supervisor del portal (todo) y el teléfono, sobre muestras suyas sin dictamen previo | `:111-114`; prueba "gana el primer dictamen" |
| ¿Un usuario puede darse el rol de supervisor? | No: nadie escribe en `supervisores` | `:91-93`; prueba "nadie edita la lista" |
| ¿El rol del teléfono se revisa en el servidor? | 🕒 No: operario y supervisor de la app usan la misma sesión anónima. Se revisa solo en la app. Arreglarlo exige que el supervisor de la app entre con su cuenta de Firebase, lo que es un cambio de diseño | `NubeSync.kt:304` |

### C) OWASP Top 10:2025
| Ítem | Estado | Nota |
|---|---|---|
| A01 Control de acceso roto | ✔️ · 🕒 | Reglas por dueño probadas (22 pruebas). El rol del teléfono queda pendiente (B) |
| A02 Mala configuración | ❌ → ✅ / ⚠️ | Faltaban cabeceras de Hosting (✅ nosniff y Referrer-Policy). La CSP y X-Frame-Options quedan ⚠️. La consola H2 del backend sigue activa, pero solo en local (➖) |
| A03 Cadena de suministro | ✔️ · 🕒 | Gradle con versiones fijas. `npm audit` de `firestore-tests` sin fallas en producción; las de desarrollo (`@grpc/grpc-js` dentro de firebase-tools) solo se arreglan con `--force` (versión mayor) → 🕒 |
| A04 Fallas criptográficas | ✔️ | PBKDF2 con sal, HTTPS, sin secretos en el repo |
| A05 Inyección | ✔️ | Room sin SQL armado con datos del usuario; el portal escapa todo con `esc()` (`index.html:1113`) |
| A06 Diseño inseguro | ⚠️ | La contraseña del supervisor es `pin-` + PIN (ver el hallazgo adicional) |
| A07 Fallas de autenticación | ✔️ · ⚠️ | Bloqueo en la app y en Auth. Faltan App Check y la protección contra enumeración (consola) |
| A08 Integridad de datos | ❌ → ⚠️ | Validación de datos en reglas (Fase 3); APK firmado con llave fuera del repo |
| A09 Registro y alertas | ✔️ | Logs de release sin RUT, PIN ni tokens; R8 quita `Log.v/d/i` (`rules.keep`) |
| A10 Manejo de errores | ✔️ | Sin conexión o sin sesión, la muestra queda pendiente sin caerse (`NubeSync.kt:139-140`) |

### D) Endurecimiento Android (MASVS)
| Control | Estado | Evidencia |
|---|---|---|
| Componentes exportados | ✔️ Solo el launcher; el FileProvider no se exporta | `AndroidManifest.xml:29, 46` |
| Copias de seguridad | ✔️ `allowBackup="false"` | `AndroidManifest.xml:19` |
| Tráfico sin cifrar | ✔️ Release sin HTTP; debug solo a la propia máquina | `app/src/debug/res/xml/seguridad_red.xml` |
| Ofuscación (R8) | ✔️ Activa en release | `app/build.gradle.kts:61` |
| Capturas de pantalla | ✔️ `FLAG_SECURE` en release | `MainActivity.kt:31` |
| Permisos | ✔️ Todos se usan (cámara, ubicación, notificaciones, red) | Manifest |
| Certificate pinning / root | ➖ No se implementa sin tu permiso (restricción) | — |
| Cabeceras del portal | ❌ → ✅ nosniff y Referrer-Policy · ⚠️ CSP y X-Frame-Options | `firebase.json` |

---

## Clasificación de lo que falla
| Ítem | Clase | Por qué |
|---|---|---|
| `.firebase/` sin ignorar | ✅ SEGURA | Solo `.gitignore` |
| Cabeceras nosniff y Referrer-Policy | ✅ SEGURA | No cambian nada visible; se activan al publicar Hosting |
| Reglas: validación de datos | ⚠️ CON RIESGO | Si olvidan un campo, la subida falla. Están escritas y probadas, **sin publicar** |
| CSP y X-Frame-Options | ⚠️ CON RIESGO | El portal tiene scripts en línea y usa CDN; una CSP mal hecha lo deja en blanco. X-Frame-Options podría afectar al iframe interno de Firebase Auth en `/__/auth/` |
| App Check | ⚠️ CON RIESGO | Si se exige sin registrar bien la app, deja afuera a los teléfonos. Primero en modo monitor |
| Contraseña `pin-` + PIN del supervisor | ⚠️ CON RIESGO | Cambia cómo entra el supervisor; hay que coordinarlo con el equipo |
| Restricción de las API keys, enumeración de correos, política de contraseñas | ⚠️ manual | Se hacen en la consola, no en el código |
| Rol del teléfono en Firestore | 🕒 DESPUÉS | Cambio de diseño |
| Dependencias de desarrollo de `firestore-tests` | 🕒 DESPUÉS | El arreglo es una versión mayor |

---

## Fase 3 · Cambios en la rama `security/hardening`
Cada cambio va en su propio commit. **No se fusionó con main ni se publicó nada.**

| Commit | Qué hace | Clase | Cómo deshacerlo |
|---|---|---|---|
| `d2441f1` | Ignora `.firebase/` (caché de la CLI de Hosting) | ✅ | `git revert d2441f1` |
| `cc3dc62` | Cabeceras `X-Content-Type-Options: nosniff` y `Referrer-Policy: strict-origin-when-cross-origin` en Hosting | ✅ | `git revert cc3dc62` |
| `c8a1d3e` | Reglas v2: campos permitidos, tipos y rangos de lo que escribe el teléfono; `codigo` y `registradaEn` no cambian; fotos solo con `fotoBase64` (≤ 1 MB) y `ownerUid`. Incluye 7 pruebas nuevas | ⚠️ | `git revert c8a1d3e` |

**Qué se validó en las reglas v2.** Solo afecta al teléfono; el supervisor no queda limitado.
- Campos permitidos: los que sube `NubeSync.subir` (`NubeSync.kt:144-186`), más `fotoBase64` para poder borrarlo.
- Al actualizar se revisan solo los campos que cambian. Así, una muestra antigua con campos que ya no se usan se sigue pudiendo actualizar.
- Límites holgados, por ejemplo conteo entre 0 y 100.000, talla entre 0 y 1.000 mm y textos de hasta 1.000 caracteres. Todos aceptan `null`, porque la app sube `null` cuando no hay dato.
- `estado` debe ser PENDIENTE, VALIDADO o CORREGIDO. Se permite crear una muestra ya validada, porque la app valida sin conexión.

**Pruebas** (`firestore-tests/reglas.test.js`, emulador con `@firebase/rules-unit-testing`):
- Las 15 pruebas existentes no se tocaron y siguen pasando.
- 7 pruebas nuevas:
  - subidas reales de la app (completa, con nulos, con foto y validada sin conexión);
  - una muestra antigua con campos extra;
  - ataques: campos extra, valores absurdos o de otro tipo, cambiar la fecha o el código, y fotos con campos extra o gigantes;
  - el supervisor sin límites.
- Resultado: **22/22 con las reglas v2**. Con las reglas actuales, 4 de las pruebas de ataque **fallan**, lo que demuestra el hueco.

**Publicar las reglas (decides tú):**
```
npx.cmd --yes firebase-tools@15.35.0 deploy --only firestore:rules --project musselapp-demo
```
**Volver a las reglas anteriores:**
```
git show main:firestore.rules > firestore.rules
npx.cmd --yes firebase-tools@15.35.0 deploy --only firestore:rules --project musselapp-demo
git checkout firestore.rules
```
No hay `storage.rules`: el proyecto no usa Firebase Storage (las fotos van en Firestore).

**Cabeceras de Hosting:** se activan en el próximo `deploy --only hosting`. El emulador local de Hosting no aplica cabeceras personalizadas, así que hay que comprobarlas después de publicar:
```
curl -sI https://musselapp-demo.web.app/
```
Deben aparecer `x-content-type-options: nosniff` y `referrer-policy`.

---

## Fase 4 · Pasada de atacante
| Intento | Resultado |
|---|---|
| Abrir el APK release y buscar secretos | Solo aparecen la API key de Firebase (pública por diseño, en `resources.arsc`) y la URL `http://127.0.0.1:8080/` del backend de debug (local, no se usa en release). No hay contraseñas, llaves privadas ni el prefijo `pin-`. El código está ofuscado por R8 |
| Con esa configuración, entrar de forma anónima y leer muestras de otros | Bloqueado: solo lista con filtro por su `ownerUid` (pruebas de lista y lectura) |
| Darse el rol de supervisor | Bloqueado: nadie escribe en `supervisores` |
| Escribir basura (campos extra, conteos negativos, textos enormes) | **Hoy funciona en producción**; queda bloqueado al publicar las reglas v2 |
| Crear muchas muestras falsas a su nombre, que el supervisor vería en el portal | ⚠️ Sigue siendo posible con datos bien formados. Lo frena **App Check**, porque solo la app real obtendría token |
| Adivinar el PIN del supervisor en el portal | Frenado por `too-many-requests`, pero posible lento (ver el hallazgo adicional) |

---

## Fase 5 · No-regresión (comparado con la línea base)
| Prueba | Línea base | Después |
|---|---|---|
| `assembleDebug` | ✔ | ✔ |
| `assembleRelease` (firmado) | ✔ | ✔ |
| Pruebas unitarias | 200/200 | 200/200 |
| Pruebas instrumentadas (compilan) | ✔ | ✔ |
| Pruebas de reglas | 15/15 | 22/22 (las 15 originales sin tocar) |

**No se tocó código de la app ni del portal:** los commits cambian solo `.gitignore`, `firebase.json`, `firestore.rules` y `firestore-tests/`. Por eso el checklist de "Funciones que deben seguir funcionando" no cambia hasta publicar las reglas.

**Checklist manual después de publicar las reglas v2.** Hazlo con un teléfono real:
1. Registra una muestra **con foto y GPS** con internet: debe llegar al portal con la foto.
2. Registra otra **sin internet** y vuelve a conectarte: debe subir sola.
3. Valida una muestra en el teléfono como supervisor sin internet y conéctate: debe llegar como VALIDADO.
4. Corrige una muestra en el portal: el teléfono debe recibir el dictamen.
5. Elimina una muestra y activa el modo demo en el portal.
6. Si algo falla, usa el comando de "Volver a las reglas anteriores".

---

## Pasos manuales en la consola de Firebase (no se pueden hacer desde el código)
1. **Publicar las reglas v2.** Usa el comando de la Fase 3, después de revisar el diff.
2. **App Check en modo monitor:**
   1. Ve a Consola → App Check → Apps.
   2. Registra la app Android con **Play Integrity**. Agrega la huella SHA-256 de la llave de release, que está en `../musselapp-release.jks` (alias `musselapp`).
   3. Registra el portal con **reCAPTCHA Enterprise**.
   4. Deja Firestore en "No aplicado" y mira las métricas una semana antes de exigirlo.
   Ojo: esto también requiere agregar el SDK de App Check a la app y al portal, que es un cambio de código ⚠️ y va aparte.
3. **Protección contra enumeración de correos:** Authentication → Configuración → Acciones del usuario → activar "Protección contra la enumeración de correos".
4. **Política de contraseñas:** Authentication → Configuración → Política de contraseñas. Hay que coordinarla con el cambio de PIN del supervisor, porque una política más estricta deja afuera a las cuentas actuales.
5. **Restringir la API key:** Google Cloud → APIs y servicios → Credenciales.
   - La key de Android: restringirla a apps Android con el paquete `cl.aldemar.musselapp` y la huella SHA-1.
   - La key web: restringirla a los referentes `musselapp-demo.web.app/*` y `musselapp-demo.firebaseapp.com/*`.

---

## Resumen de estado
| Ítem | Qué es | Aplica | Antes | Acción | Evidencia | Riesgo |
|---|---|---|---|---|---|---|
| A1 Rate limiting | Frenar intentos repetidos | Sí | ✔️ / ❌ App Check | Pendiente ⚠️ | `BloqueoLogin.kt:47-48` | Medio |
| A2 Secretos / .env | Claves fuera del repo | Sí | ✔️ / ❌ keys sin restringir | Manual (consola) | `.gitignore` | Bajo |
| A3 Secretos en la plataforma | Llaves fuera del código | Sí | ✔️ | Ninguna | `keystore.properties` ignorado | — |
| A4 IP limiting | Filtrar por origen | Parcial | ➖ / ✔️ | Ninguna | `application.properties:4` | — |
| A5 Validación de entrada | Tipos y rangos | Sí | ✔️ / ❌ reglas | Reglas v2 ⚠️ | `firestore.rules:44-84` | Medio |
| A6 Validación en el servidor | No confiar en el cliente | Sí | ❌ | Reglas v2 ⚠️ | `firestore.rules:105-117` | Medio |
| A7 Mínimo privilegio | Solo lo necesario | Sí | ✔️ / ❌ campos | Reglas v2 ⚠️ | `firestore.rules:46-51` | Medio |
| A8 Cifrado | En tránsito y guardado | Sí | ✔️ | Ninguna | `HashPin.kt:18-19` | — |
| A9 Sesión | Entrar y salir | Sí | ✔️ / ❌ consola | Manual (consola) | `index.html:3218, 3267` | Bajo |
| A10 CORS | Quién llama a la API | No | ➖ | Ninguna | `application.properties:4` | — |
| B Roles | Quién puede qué | Sí | ✔️ / 🕒 teléfono | 🕒 | `NubeSync.kt:304` | Medio |
| C OWASP | Top 10:2025 | Sí | Ver tabla C | Ver tabla C | — | — |
| D Android | MASVS | Sí | ✔️ / ❌ cabeceras | ✅ cabeceras | `firebase.json` | Bajo |

### Lo que ya cumplía y no se tocó
- PIN con PBKDF2 y bloqueo.
- `allowBackup=false`, componentes no exportados, `FLAG_SECURE`, R8 y logs limpios.
- Red de debug limitada a la propia máquina.
- Backend en `127.0.0.1` con `@Valid`.
- Escape en el portal (`esc()`).
- Room sin SQL armado con datos del usuario.
- Reglas de acceso por dueño, primer dictamen y lista de supervisores.
- `.gitignore` de llaves, sin secretos en el historial y workflows sin secretos.

### Para la presentación
1. Auditamos MusselApp con OWASP Top 10:2025 y MASVS: la mayoría de los controles ya se cumplían, y lo dejamos documentado con archivo y línea.
2. El hueco principal era que Firestore revisaba **quién** escribía pero no **qué** escribía; escribimos reglas nuevas que validan campos, tipos y rangos, con 22 pruebas automáticas en el emulador.
3. Demostramos el hueco: 4 pruebas de ataque fallan con las reglas actuales y pasan con las nuevas, sin romper ninguna de las 15 pruebas originales.
4. Cada cambio está en un commit aparte en la rama `security/hardening`, se puede deshacer con `git revert`, y la app sigue pasando sus 200 pruebas.
5. Lo que queda (App Check y la contraseña del supervisor) está priorizado, con pasos concretos para la consola de Firebase.
