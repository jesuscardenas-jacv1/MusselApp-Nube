# Backend REST — MusselApp (Spring Boot + JPA + Swagger)

Microservicio REST en **Spring Boot 3.5** con persistencia **JPA + H2 en archivo local** y documentación interactiva **Springdoc OpenAPI / Swagger UI** para el caso académico **AquaSample · ALDEMAR SpA** (DSY1105 Duoc UC).

## Ejecutar localmente

```bash
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" mvn -f backend/pom.xml spring-boot:run
```

> Si ya tienes `java` 17+ configurado en tu `PATH`, basta con `cd backend && mvn spring-boot:run`.

- **Swagger UI (OpenAPI):** http://localhost:8080/swagger-ui.html
- **OpenAPI JSON:** http://localhost:8080/v3/api-docs
- **Consola H2:** http://localhost:8080/h2-console (JDBC URL: `jdbc:h2:file:./data/musselapp`, usuario `sa`, sin contraseña)

## Conectar con el teléfono Android físico

Por el mismo cable USB con el que se proyecta el teléfono:

```bash
~/Library/Android/sdk/platform-tools/adb reverse tcp:8080 tcp:8080
```

La app Android usa `http://127.0.0.1:8080/` (`BackendApi.URL_BASE`), por lo que no depende del Wi-Fi ni de cambios de IP.

El servidor escucha solo en `127.0.0.1`: no se ve desde otros equipos de la red.

## Reglas

- **Rol (`X-Rol`)**: obligatorio para crear, editar, dictaminar y borrar (`OPERARIO` o `SUPERVISOR`); consultar no lo exige. El rol lo declara quien llama: es una simplificación del proyecto académico, no un inicio de sesión.
- **Gana el primer dictamen**: una muestra validada o corregida no se edita ni se vuelve a dictaminar (`409`).
- **Corregir** exige un comentario de al menos 5 caracteres y puede cambiar conteo, talla y cholgas; se guarda lo que había registrado el operario.
- **Reintentos**: un `POST` con un `idCliente` que ya existe no duplica ni pisa la muestra; solo completa el GPS y el clima que faltaban.
- **`PUT`**: los campos opcionales que no vienen se conservan.
- **Límites**: los mismos de la app (talla 1–100 mm, conteo 1–5000, cholgas 0–5000, tramo 1–500 cm, profundidad 1–6 m).

## Cómo se sincroniza con la app

| Cambio | Qué pasa |
|---|---|
| Muestra nueva en el teléfono | Se envía a Firestore y, si el backend está encendido, también a él |
| `POST` en Swagger | Al tocar **Sincronizar**, la app la importa (Retrofit `GET`) y la sube a Firestore |
| `PUT` en Swagger (muestra pendiente) | Al sincronizar cambia en la app y en el portal |
| Dictamen en Swagger | Al sincronizar llega a la app como aviso, y a Firestore |
| Dictamen o corrección en el portal | Llega al teléfono en vivo y el teléfono lo envía al backend |
| Muestra eliminada en el portal | Se borra del teléfono y, si está encendido, del backend |

- Las muestras se reconocen por `idCliente`, no por el `id` de la tabla: cada computador tiene su propia base (`GET /api/servidor` la identifica).
- El teléfono no trae muestras de otros teléfonos ni las tres de ejemplo.
- **Limitaciones:** borrar en Swagger no borra en el teléfono ni en Firestore (se elimina desde el portal). Si el portal y Swagger dictaminan distinto antes de que el teléfono sincronice, en el teléfono y en Firestore vale el del portal. Si dos teléfonos se conectan al mismo backend, los dos importan lo creado en Swagger.

## Demostración (POST / PUT)

1. Levantar el backend, conectar el teléfono y ejecutar `adb reverse tcp:8080 tcp:8080`.
2. Swagger → `POST /api/muestras`, `X-Rol: OPERARIO` → **201**. En la app, **Sincronizar**: aparece la muestra.
3. Swagger → `PUT /api/muestras/{id}` con otro conteo → **200**. **Sincronizar**: cambia en la app y en el portal.
4. Sin `X-Rol` → **403**. `POST /api/muestras/{id}/revisiones` como `SUPERVISOR` → llega el aviso al teléfono.
5. Repetir el `PUT` → **409**, porque ya tiene dictamen.

## Entidades y endpoints

| Recurso | Métodos y rutas | Permisos (`X-Rol`) |
|---|---|---|
| **Catálogo** (`Centro`, `Tren`, `Linea`) | `GET /api/centros`<br>`GET /api/centros/{centroId}/trenes`<br>`GET /api/trenes/{trenId}/lineas` | Todos |
| **Muestras** (`Muestra`) | `GET /api/muestras` (filtros: `idCliente`, `centroId`, `tren`, `linea`, `estado`, `desde`, `hasta`)<br>`GET /api/muestras/{id}`<br>`POST /api/muestras` (no duplica por `idCliente`)<br>`PUT /api/muestras/{id}` (solo pendientes)<br>`DELETE /api/muestras/{id}` | Lectura: todos<br>`POST`/`PUT`: `OPERARIO`, `SUPERVISOR`<br>`DELETE`: solo `SUPERVISOR` |
| **Fotos y metadatos** (`FotoMuestra`) | `GET /api/muestras/{id}/foto`<br>`PUT /api/muestras/{id}/foto`<br>`DELETE /api/muestras/{id}/foto` | Lectura: todos<br>`PUT`: `OPERARIO`, `SUPERVISOR`<br>`DELETE`: solo `SUPERVISOR` |
| **Revisiones y estados** (`Revision`) | `GET /api/muestras/{id}/revisiones`<br>`POST /api/muestras/{id}/revisiones` (`VALIDADO` o `CORREGIDO`; solo pendientes; al corregir exige comentario ≥ 5 caracteres) | Lectura: todos<br>`POST`: solo `SUPERVISOR` |
| **Servidor** | `GET /api/servidor` | Todos |
| **Reportes** | `GET /api/reportes/resumen`<br>`GET /api/reportes/por-linea` | Todos |

## Pruebas automatizadas

```bash
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" mvn -f backend/pom.xml test
```
