<p align="center">
  <img src="docs/diseno/logo-192.png" alt="Logo MusselApp" width="120">
</p>

<h1 align="center">MusselApp</h1>

<p align="center">
  App móvil para el registro y trazabilidad de muestras de choritos en centros de cultivo.<br>
  Caso académico <strong>AquaSample · ALDEMAR SpA</strong> · DSY1105 Desarrollo de Aplicaciones Móviles · Duoc UC Sede Puerto Montt
</p>

---

## 1. Descripción

En el muestreo de líneas de cultivo de choritos, el operario extrae un tramo representativo de la cuerda, cuenta los individuos, toma fotografías y registra observaciones. Hoy ese flujo es manual y la información queda dispersa entre planillas, fotos y notas, lo que dificulta el seguimiento histórico de una misma línea y la revisión del supervisor.

**MusselApp** es un MVP Android que digitaliza ese proceso en terreno:

- El **operario** registra la muestra (centro, tren, línea y estrato), adjunta una fotografía, ingresa conteo y talla, y la guarda en el teléfono aunque no haya señal.
- El **supervisor** revisa las muestras pendientes y las deja **Validadas** o **Corregidas** con un comentario técnico.
- Todo queda en un **historial** consultable, con búsqueda, filtros y detalle de cada muestra.
- Cuando hay señal, cada muestra se **sube a la nube** (Firestore) y el supervisor la puede dictaminar desde el **portal web**; el dictamen vuelve al teléfono con un aviso.

> Proyecto académico con datos ficticios. No reemplaza el criterio técnico de ALDEMAR ni realiza conteo automático.

## 2. Funcionalidades implementadas

| Funcionalidad | Dónde se ve | Implementación |
|---|---|---|
| Inicio de sesión por rol (operario / supervisor) | Login | `LoginViewModel` + `RoomAuthRepository`; RUT con dígito verificador (módulo 11) y formato en vivo |
| Formulario de muestra con campos obligatorios y mensajes de error | Nueva muestra | `MuestraViewModel.guardar()` valida centro, tren, línea y estrato |
| Fotografía de evidencia con cámara o galería | Captura | CameraX con permiso `CAMERA` en tiempo de ejecución; galería con Photo Picker (`PickVisualMedia`) |
| Conteo, talla y observación con validación de rangos | Vista previa | `validarBorrador()` en `BorradorMuestra.kt`; confirmación antes de guardar |
| Persistencia local que se conserva al cerrar la app | Todas | Room (SQLite): 6 entidades, 3 DAO, migraciones versionadas |
| Historial con búsqueda, filtros por estado y período, y orden | Historial | `HistorialViewModel` (`combine` + `stateIn`) y `FiltrosHistorial` |
| Detalle de la muestra con dictamen del supervisor | Detalle | `DetalleViewModel` lee la muestra por id desde Room |
| Ficha técnica en PDF: guardar, imprimir o compartir | Detalle | `FichaPdf` con `PrintManager` y `FileProvider` |
| Revisión del supervisor: validar o corregir (al corregir, comentario de al menos 5 caracteres, igual que en el portal) | Supervisión | `RevisionViewModel` registra la revisión y cambia el estado |
| Posición GPS de la muestra y distancia al centro de cultivo | Vista previa, Detalle | `UbicadorGps` (servicios de ubicación de Google) con permiso en tiempo de ejecución; sin permiso o sin señal la muestra se guarda igual |
| Clima y estado del mar al muestrear (viento, racha, ola, temperatura del agua, marea) | Detalle, ficha PDF | API REST de [Open-Meteo](https://open-meteo.com) con Retrofit (`ClimaRepository`); guardar nunca espera a la red |
| Alertas automáticas para el supervisor (fuera del área, densidad, cholgas, talla, mal tiempo) | Supervisión, Detalle, portal web | `AlertasMuestra.evaluar()`, reglas puras con los límites en `Umbrales` |
| Métricas de la jornada y muestras recientes | Principal | `PrincipalViewModel` + `ResumenJornada` |
| Sincronización con la nube y dictamen desde el computador | Todas, portal web | `NubeSync` (Firestore) detrás de la interfaz `ServicioNube`; Room sigue siendo la fuente de verdad, se guarda sin red y se sube después. `ProteccionDictamen` evita que un reintento pise un dictamen del supervisor |
| Muestra borrada en el portal | Notificación, Historial | Al eliminar una muestra en el portal, el teléfono que la registró la borra (con sus revisiones y su foto) y avisa con una notificación |
| Seguridad local | Login | PIN guardado con hash PBKDF2, bloqueo tras 5 intentos fallidos y base excluida del respaldo |

**Recursos del dispositivo integrados al flujo:** cámara, galería (Photo Picker), ubicación (GPS), documentos (ficha PDF para imprimir o compartir) y fecha/hora del registro.

**Servicio externo:** el clima y el estado del mar vienen de Open-Meteo (gratis, sin clave). Son datos modelados: en canales interiores el oleaje es una referencia, no una medición. Los límites de las alertas son supuestos del caso académico, no normas del rubro.

## 3. Capturas

| Login | Nueva muestra | Vista previa | Historial |
|---|---|---|---|
| <img src="docs/capturas/01-login.jpg" width="180"> | <img src="docs/capturas/02-nueva-muestra-validacion.jpg" width="180"> | <img src="docs/capturas/03-vista-previa.jpg" width="180"> | <img src="docs/capturas/04-historial.jpg" width="180"> |

| Detalle | Revisión del supervisor | Principal | Ficha PDF |
|---|---|---|---|
| <img src="docs/capturas/05-detalle.jpg" width="180"> | <img src="docs/capturas/06-revision-supervisor.jpg" width="180"> | <img src="docs/capturas/07-principal.jpg" width="180"> | <img src="docs/capturas/08-ficha-pdf.jpg" width="180"> |

## 4. Instrucciones de ejecución

### Requisitos

- **Android Studio** reciente (con soporte para AGP 9.3) y su JDK incluido.
- **Android SDK** con la plataforma **API 37** (`compileSdk`/`targetSdk` 37).
- Teléfono o emulador con **Android 7.0 (API 24)** o superior. Para probar la cámara se recomienda un teléfono físico.

### Pasos

1. Clonar el repositorio:
   ```bash
   git clone https://github.com/jesuscardenas-jacv1/MusselApp-Nube.git
   ```
2. Abrir la carpeta `MusselApp-Nube` en Android Studio y esperar la sincronización de Gradle.
3. Conectar un teléfono con la **depuración USB** activada (o iniciar un emulador).
4. Elegir la configuración **app** y presionar **Run ▶**.

También se puede compilar desde la terminal:

```bash
./gradlew assembleDebug        # genera app/build/outputs/apk/debug/app-debug.apk
./gradlew installDebug         # instala en el teléfono conectado
```

### Usuarios de prueba

La base se llena con datos ficticios la primera vez que se abre la app.

| Rol | RUT | PIN | Qué puede hacer |
|---|---|---|---|
| Operario (Carla Vera) | `11.111.111-1` | `1234` | Registrar muestras, ver historial y detalle |
| Supervisor (Diego Muñoz) | `22.222.222-2` | `1234` | Revisar, validar o corregir muestras |

En el Login se elige el perfil, se ingresa RUT y PIN, y el centro de cultivo de la jornada.

### Pruebas automatizadas

```bash
./gradlew testDebugUnitTest          # 108 pruebas unitarias (validaciones, regla del comentario al corregir, catálogo de trenes y líneas, filtros, ViewModels, hash del PIN, alertas, distancia, clima, protección del dictamen)
./gradlew connectedDebugAndroidTest  # 31 pruebas instrumentadas con Room real, migraciones, catálogo de trenes y líneas y sincronización con una nube falsa (requiere teléfono o emulador)
```

Las mismas pruebas corren solas en **GitHub Actions** (`.github/workflows/android.yml`) en cada push a `main`, `feature/**`, `fix/**` o `mejoras/**` y en cada PR: compila, corre las unitarias y las instrumentadas en un emulador, y deja el APK de debug como artefacto descargable por 14 días.

### Portal web del supervisor

El supervisor revisa y dictamina las muestras desde el computador abriendo [`web-supervisor/index.html`](web-supervisor/index.html) con doble clic en Chrome (sin servidor ni instalación). Se conecta al proyecto Firebase `musselapp-demo` y se actualiza en vivo. Entra con el supervisor de prueba (`22.222.222-2` / `1234`) o con **Ver con datos de ejemplo** para probar sin nube. Detalle de pantallas y funcionamiento en [`web-supervisor/README.md`](web-supervisor/README.md).

## 5. Arquitectura

La app sigue **MVVM**: la pantalla solo dibuja el estado, el ViewModel aplica las reglas y expone un `StateFlow<UiState>`, y el Repository es el único que habla con Room.

```mermaid
flowchart LR
    UI["UI · Jetpack Compose<br/>LoginScreen, MuestraScreen,<br/>HistorialScreen, RevisionScreen…"] -->|eventos| VM["ViewModel<br/>reglas + StateFlow de UiState"]
    VM -->|estado| UI
    VM --> REPO["Repository<br/>AuthRepository · MuestraRepository"]
    REPO --> DAO["Room · DAO<br/>MuestraDao · UsuarioDao · CatalogoDao"]
    DAO --> DB[("SQLite<br/>musselapp.db")]
    REPO <-->|sube muestras / recibe dictámenes| NUBE["ServicioNube · NubeSync<br/>Firestore (musselapp-demo)"]
    WEB["Portal web del supervisor<br/>web-supervisor/index.html"] <--> NUBE
```

Room es la **fuente de verdad**: la app guarda primero en el teléfono y luego sube a Firestore. Si no hay red, la muestra queda pendiente de sincronizar y se reintenta.

```
app/src/main/java/cl/aldemar/musselapp/
├── AppContainer.kt          # crea la base y los repositorios (una sola vez)
├── data/
│   ├── local/               # Room: MusselDatabase, Migraciones, dao/, entidad/
│   ├── model/               # Muestra, Usuario, Rol, EstadoMuestra, CentroCultivo
│   ├── nube/                # ServicioNube, NubeSync (Firestore), ProteccionDictamen, avisos
│   ├── clima/ ubicacion/    # Open-Meteo (Retrofit) y GPS
│   └── repository/          # interfaces + implementaciones Room y de prueba
├── ui/
│   ├── login/ principal/ muestra/ screens/ historial/ detalle/ revision/
│   ├── navegacion/          # Rutas y MusselNavHost
│   ├── componentes/         # componentes reutilizables
│   └── theme/               # colores, tipografía y tema Material 3
└── util/                    # RutValidator, Fechas, FichaPdf, HashPin, AlmacenFotos…
```

El modelo de datos (diagrama entidad-relación y tablas) está documentado en [`docs/base-de-datos.md`](docs/base-de-datos.md).

## 6. Integrantes — Equipo Pipeño (Sección 004V)

| Nombre | GitHub | Rol |
|---|---|---|
| Jesús Cárdenas | [@jesuscardenas-jacv1](https://github.com/jesuscardenas-jacv1) | Front-end |
| Ramón Osorio | [@KanninoX](https://github.com/KanninoX) | UI/UX & Mobile Developer |
| Lucas Saldivia | [@lucsaldivia-ux](https://github.com/lucsaldivia-ux) | QA & Data Engineer |

## 7. Trabajo colaborativo

- **Ramas:** cada tarea se desarrolla en su propia rama (`feature/…` o `fix/…`); nunca directamente en `main`.
- **Pull Requests:** todo cambio entra a `main` por PR, con descripción, capturas y pasos para probar, revisado por otro integrante.
- **Commits** técnicos en español con prefijo (`feat:`, `fix:`, `test:`, `docs:`, `refactor:`).
- **Issues:** los hallazgos de la revisión técnica se registran como issues asignados.
- **Planificación:** pendientes y responsables en Trello — _enlace pendiente_.

## 8. Tecnologías

- **Kotlin** + **Jetpack Compose** con **Material 3**
- Arquitectura **MVVM** con **ViewModel** y **StateFlow**
- **Navigation Compose**
- Persistencia local con **Room 2.8** (SQLite, KSP)
- **Firebase Cloud Firestore** para sincronizar muestras y dictámenes
- **CameraX** y Activity Result API (permisos, Photo Picker)
- **Retrofit** + Gson para la API REST de clima (Open-Meteo)
- **Google Play Services Location** para la posición GPS
- **Gradle** (Kotlin DSL), Android Studio, **GitHub Actions** (CI)
- Portal web: HTML y JavaScript sin framework, Firebase (SDK compat), Chart.js, Leaflet con teselas de CARTO
- Diseño: **Google Stitch**, Mermaid para UML

## 9. Identidad visual

| Elemento | Definición |
|---|---|
| Nombre | **MusselApp** |
| Logotipo | [`docs/diseno/logo.png`](docs/diseno/logo.png) · [`logo.svg`](docs/diseno/logo.svg) — representa el chorito chileno y la conexión con el mar del sur de Chile |

### Paleta de colores

| Color | HEX | Uso |
|---|---|---|
| Principal | `#0F172A` | Encabezados, TopAppBar y botones de acción clave (CTA) |
| Secundario | `#334155` | Encabezados de tablas, botones secundarios y navegación |
| Fondo | `#F1F5F9` | Fondo general; evita deslumbramiento bajo luz solar directa |
| Texto | `#000000` | Títulos, lecturas de datos y etiquetas de alto contraste |
| Adicional | `#475569` | Bordes de tarjetas, divisores, textos secundarios y metadatos (contraste AA) |

Paleta sobria y de alto contraste, pensada para una empresa de servicios y para uso en terreno.

## 10. Flujo de usuario (Diagrama de Actividad UML)

Imagen exportada: [`docs/diseno/flujo-usuario-uml.png`](docs/diseno/flujo-usuario-uml.png) · Fuente: [`flujo-usuario-uml.mmd`](docs/diseno/flujo-usuario-uml.mmd)

```mermaid
flowchart TD
    Inicio([Inicio]) --> Login[Ingresar credenciales ficticias]
    Login --> V1{¿Credenciales válidas?}
    V1 -- No --> Login
    V1 -- Sí --> Principal[Pantalla Principal según rol]
    Principal --> D1{¿Qué desea hacer?}

    D1 -- Nueva muestra<br/>(Operario) --> Form[Seleccionar Centro, Tren,<br/>Línea y Estrato]
    Form --> Foto[Capturar o adjuntar fotografía]
    Foto --> Conteo[Ingresar conteo, talla<br/>y observaciones]
    Conteo --> Preview[Vista previa de la muestra]
    Preview --> V2{¿Datos completos<br/>y correctos?}
    V2 -- No, corregir --> Form
    V2 -- Sí --> Guardar[Guardar en el teléfono<br/>Estado: Pendiente]
    Guardar --> Historial

    D1 -- Historial --> Historial[Historial de muestras<br/>buscar y filtrar por estado o período]
    Historial --> Detalle[Detalle de la muestra]
    D1 -- Supervisión<br/>(Supervisor) --> Revision[Revisión: validar o corregir<br/>con comentario técnico]
    Revision --> D3{Dictamen}
    D3 -- Validado --> Fin([Fin])
    D3 -- Corregido<br/>comentario obligatorio --> Fin
    Detalle --> Fin
```

## 11. Diseño de interfaces

Diseños base generados con **Google Stitch** (Material Design 3), guardados en [`docs/diseno/interfaces/`](docs/diseno/interfaces/). La exportación original de Stitch (HTML y capturas de cada pantalla, y el sistema de diseño) está en [`docs/diseno/stitch/`](docs/diseno/stitch/). Las capturas de la app real están en la sección 3.

| Pantalla | Objetivo | Diseño |
|---|---|---|
| Login | Verificación de credenciales de los usuarios | [`01-login.png`](docs/diseno/interfaces/01-login.png) |
| Principal | Menú de opciones según el rol | [`02-principal.png`](docs/diseno/interfaces/02-principal.png) |
| Muestra | Iniciar un muestreo | [`03-muestra.png`](docs/diseno/interfaces/03-muestra.png) |
| Captura | Capturar un registro visual de la muestra | [`04-captura.png`](docs/diseno/interfaces/04-captura.png) |
| Vista previa | Previsualizar fotografía y datos, y confirmar el muestreo | [`05-vista-previa.png`](docs/diseno/interfaces/05-vista-previa.png) |
| Historial | Listar las muestras registradas | [`06-historial.png`](docs/diseno/interfaces/06-historial.png) |
| Detalle | Ver en detalle una muestra seleccionada | [`07-detalle.png`](docs/diseno/interfaces/07-detalle.png) |
| Revisión del Supervisor | Gestionar el estado de las muestras | [`08-revision.png`](docs/diseno/interfaces/08-revision.png) |

### Navegación

| Componente | ¿Se utiliza? | Para qué |
|---|---|---|
| NavigationBar | Sí | Operario: Muestras, Nueva Muestra e Historial · Supervisor: Muestras, Historial y Supervisión |
| NavigationDrawer | No | — |
| TopAppBar | Sí | Título de pantalla, volver, sincronizar y menú de perfil (cerrar sesión) |
| Botón principal | Sí | Tarjeta "Nueva muestra" (operario) o "Revisar muestras" (supervisor) en la Principal |

## 12. Estructura del repositorio

```
.github/workflows/           # CI: build + pruebas unitarias e instrumentadas
app/                         # código fuente de la app Android
├── schemas/                 # esquemas exportados de Room (uno por versión)
└── src/
    ├── main/                # código, recursos y AndroidManifest.xml
    ├── test/                # pruebas unitarias
    └── androidTest/         # pruebas instrumentadas (Room)
docs/
├── base-de-datos.md         # modelo de datos
├── capturas/                # capturas de la app real
├── diseno/
│   ├── logo.png · logo.svg
│   ├── flujo-usuario-uml.png · flujo-usuario-uml.mmd
│   ├── interfaces/          # pantallas diseñadas en Stitch
│   └── stitch/              # exportación original de Google Stitch
└── evidencias/
    └── clase-02/            # Evidencia_Clase_02_Diseno_EquipoPipeno
web-supervisor/              # portal web del supervisor (index.html + README)
```
