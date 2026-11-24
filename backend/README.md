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

## Entidades y endpoints

| Recurso | Métodos y rutas | Permisos (`X-Rol`) |
|---|---|---|
| **Catálogo** (`Centro`, `Tren`, `Linea`) | `GET /api/centros`<br>`GET /api/centros/{centroId}/trenes`<br>`GET /api/trenes/{trenId}/lineas` | Todos |
| **Muestras** (`Muestra`) | `GET /api/muestras` (filtros: `centroId`, `tren`, `linea`, `estado`, `desde`, `hasta`)<br>`GET /api/muestras/{id}`<br>`POST /api/muestras` (idempotente por `idCliente`)<br>`PUT /api/muestras/{id}`<br>`DELETE /api/muestras/{id}` | Lectura: todos<br>`POST`/`PUT`: `OPERARIO`, `SUPERVISOR`<br>`DELETE`: solo `SUPERVISOR` |
| **Fotos y metadatos** (`FotoMuestra`) | `GET /api/muestras/{id}/foto`<br>`PUT /api/muestras/{id}/foto`<br>`DELETE /api/muestras/{id}/foto` | Lectura: todos<br>`PUT`: `OPERARIO`, `SUPERVISOR`<br>`DELETE`: solo `SUPERVISOR` |
| **Revisiones y estados** (`Revision`) | `GET /api/muestras/{id}/revisiones`<br>`POST /api/muestras/{id}/revisiones` (`VALIDADO` o `CORREGIDO`; al corregir exige comentario ≥ 5 caracteres) | Lectura: todos<br>`POST`: solo `SUPERVISOR` |
| **Reportes** | `GET /api/reportes/resumen`<br>`GET /api/reportes/por-linea` | Todos |

## Pruebas automatizadas

```bash
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" mvn -f backend/pom.xml test
```
