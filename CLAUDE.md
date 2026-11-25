# MusselApp · Reglas para asistentes de IA (fase final: conteo con ML)

Este repositorio es la app Android **MusselApp** (Kotlin, Jetpack Compose, Room, Firebase Firestore/Auth, WorkManager, CameraX)
y su **portal web del supervisor** (`web-supervisor/index.html`). Proyecto de 3 estudiantes de Duoc UC (DSY1105).

## Antes de hacer nada

1. Lee el plan: `docs/plan-fase-final.md`.
2. Lee las instrucciones de la persona con la que trabajas: `docs/instrucciones-ml/` (`ramon.md`, `jesus.md`, `lucas.md`; para todos, `equipo.md`).
3. Muestra un plan corto y espera el OK antes de cambiar código.

## Reglas obligatorias

- **No trabajar en `main`.** La rama base de la fase es `feature/ml-conteo`; cada persona trabaja en su rama (`feature/ml-app`, `feature/ml-portal`, `feature/ml-reglas`) y abre el PR hacia `feature/ml-conteo`.
- **Commits en español y sin coautor de IA**: nada de líneas `Co-Authored-By` ni firmas de asistentes en commits o PR.
- **La app no se puede romper:** `./gradlew assembleDebug testDebugUnitTest` debe seguir en verde (200 pruebas al empezar la fase). Cambios mínimos; nada de refactorizaciones ni renombres.
- **Room es obligatorio** (requisito del ramo): no quitarlo ni reemplazarlo. La migración 7 → 8 solo agrega columnas y tablas, y se prueba con `MigrationTestHelper`.
- **No cambiar el flujo de estados**: Pendiente → Validado / Corregido. No cambiar el significado de `conteoIndM`, `cholgasIndM` ni `tallaMm`.
- **Con `BuildConfig.CONTEO_ML = false`, la app debe comportarse igual que la versión `v1.3.2`.**
- **Firebase gratis (plan Spark):** nada que exija el plan Blaze. No usar Cloud Storage, Cloud Functions ni APIs de visión de pago. La inferencia corre en el teléfono.
- **No usar la base real de Firestore para probar:** usar el emulador (`firebase emulators:start`). No publicar reglas ni Hosting sin el OK del equipo.
- **Nunca subir claves**: `*firebase-adminsdk*.json`, `keystore.properties`, `*.jks`. No mostrar sus valores.
- **No subir fotos, datasets ni modelos pesados** a este repo. El dataset vive en Google Drive y el entrenamiento en `lucsaldivia-ux/MusselApp-ML`.
- **Licencias aptas para uso comercial** (Apache 2.0 / MIT). No usar Ultralytics YOLO (AGPL).
- **No correr pruebas instrumentadas en el teléfono personal** de nadie: desinstalan la app y borran sus muestras.

## Quién toca qué (para no pisarse)

| Área | Archivos | Responsable |
|---|---|---|
| App: detector, Room, pantalla de revisión | `app/src/main/java/.../data/ml/`, `data/local/`, `ui/muestra/` | Ramón |
| Portal web | `web-supervisor/index.html` | Jesús |
| Reglas, sincronización, calibre, respaldo | `firestore.rules`, `firestore-tests/`, `data/nube/NubeSync.kt`, `data/ml/Calibre.kt`, `scripts/` | Lucas |

## Contrato de datos (no cambiar sin avisar)

Etiquetas del modelo: `chorito`, `cholga`, `referencia` (cuadrado negro de 40 mm). En Firestore, cada `muestras/{id}` puede traer:

```js
ml: { version, total, choritos, cholgas, mmPorPx, tallaChoritoMm, tallaCholgaMm, editado },
detecciones: [ { e: "C" | "K", p, x, y, w, h, mm, o: "M" | "O" } ]   // x,y,w,h en escala 0-1000
```

Detalle completo en `docs/plan-fase-final.md` (§5).
