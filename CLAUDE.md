# MusselApp · Reglas para asistentes de IA (fase final: conteo con ML)

Este repositorio es la app Android **MusselApp** (Kotlin, Jetpack Compose, Room, Firebase Firestore/Auth, WorkManager, CameraX)
y su **portal web del supervisor** (`web-supervisor/index.html`). Proyecto de 3 estudiantes de Duoc UC (DSY1105).

## Antes de hacer nada

1. Lee el estado actual: `docs/estado-ml.md` (qué está hecho, dónde está cada cosa y qué sigue).
2. Lee el plan: `docs/plan-fase-final.md`.
3. Lee las instrucciones de la persona con la que trabajas: `docs/instrucciones-ml/` (`ramon.md`, `jesus.md`, `lucas.md`; para todos, `equipo.md`).
4. Trae lo último: `git fetch origin` y `git merge origin/feature/ml-conteo` en tu rama.
5. Muestra un plan corto y espera el OK antes de cambiar código.

## Al terminar cada sesión de trabajo

- Agrega una entrada a la bitácora de `docs/estado-ml.md` (fecha, quién, qué se hizo, qué quedó pendiente, sorpresas)
  y actualiza su tabla de estado. Marca los pasos terminados en `docs/plan-fase-final.md` §8.
- Si cambiaste algo que usa otra persona (formato, nombres, archivos), anótalo también en sus instrucciones.
- Así el asistente del siguiente integrante sabe en qué quedó todo sin preguntar.

## Reglas obligatorias

- **No trabajar en `main`.** La rama base de la fase es `feature/ml-conteo`; cada persona trabaja en su rama (`feature/ml-app`, `feature/ml-portal`, `feature/ml-reglas`) y abre el PR hacia `feature/ml-conteo`. **Nunca abrir ni fusionar un PR hacia `main`** (al crear el PR, revisar que la base sea `feature/ml-conteo`); eso se hace solo en el paso 12, con el OK de los tres.
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
| App: detector, Room, pantalla de revisión | `app/src/main/java/.../data/ml/` (salvo los de Lucas), `data/local/`, `ui/muestra/` | Ramón |
| Portal web | `web-supervisor/index.html` | Jesús |
| Reglas, sincronización, calibre, post-proceso del modelo, respaldo | `firestore.rules`, `firestore-tests/`, `data/nube/NubeSync.kt`, `data/nube/ResultadoMlNube.kt`, `data/nube/ProteccionDictamen.kt`, `data/ml/Calibre.kt`, `data/ml/SalidaRfDetr.kt`, `scripts/` | Lucas |

Si necesitas cambiar un archivo de otra persona, avísale o pídeselo; no lo cambies por tu cuenta.

## Contrato de datos (no cambiar sin avisar)

Etiquetas del modelo: `chorito`, `cholga`, `referencia` (cuadrado negro de 40 mm). En Firestore, cada `muestras/{id}` puede traer:

```js
ml: { version, total, choritos, cholgas, mmPorPx, tallaChoritoMm, tallaCholgaMm, editado },
detecciones: [ { e: "C" | "K", p, x, y, w, h, mm, o: "M" | "O" } ]   // x,y,w,h en escala 0-1000
```

Detalle completo en `docs/plan-fase-final.md` (§5).
