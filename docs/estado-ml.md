# Estado de la fase final · Bitácora

> **Para los asistentes de IA:** lee esto **primero**. Dice qué está hecho, dónde está cada cosa y qué sigue.
> Al terminar una sesión de trabajo, **agrega una entrada al final** (fecha, quién, qué hiciste, qué quedó pendiente)
> y actualiza las tablas si cambió algo. El plan completo está en [`plan-fase-final.md`](plan-fase-final.md).

**Última actualización:** 2026-11-25, después de mediodía (Lucas)

## 1. Dónde está cada cosa

| Qué | Dónde |
|---|---|
| Plan, calendario y contrato | `docs/plan-fase-final.md` |
| Instrucciones por persona | `docs/instrucciones-ml/` (`equipo.md`, `ramon.md`, `jesus.md`, `lucas.md`) |
| Reglas para la IA | `CLAUDE.md` (raíz) |
| Código ya hecho de Lucas (calibre, post-proceso del modelo, formato para Firestore, reglas) | Ya en `feature/ml-conteo` (PR #55, fusionado) |
| Entrenamiento (notebook, split, dataset sintético, contrato del `.tflite`, `validar_calibre.py`, `evaluar_tflite.py`) | https://github.com/lucsaldivia-ux/MusselApp-ML |
| Modelo de prueba para integrar la app | Release `model-v0-sintetico` de MusselApp-ML |
| Datos de prueba para el portal (emulador) | `scripts/datos-prueba-emulador/` (`npm run cargar`; supervisor RUT 22.222.222-2, PIN 1234, solo en el emulador) |
| Fotos, etiquetas y checkpoints | Drive `MusselApp-ML` (`dataset_v1/crudas`, `dataset_v1/etiquetas`, `checkpoints`, planilla `planilla_fotos`) |

## 2. Estado por paso

| Paso | Qué | Quién | Estado |
|---|---|---|---|
| 0 | Respaldo de Firestore | Lucas | Script listo; falta que Lucas genere la clave y lo ejecute |
| 1–2 | Fotos y etiquetado | Los 3 | **Sin empezar.** Fotos listas el vie 27, etiquetas el lun 30 |
| 3 | Split sin fuga + notebook | Lucas | Listo y probado con datos sintéticos. Falta correrlo con fotos reales |
| 3a | Pre-entrenar con datos públicos | Lucas | Dataset elegido ("mussel" de Stephen, CC BY 4.0). Falta descargarlo (requiere cuenta de Roboflow) |
| 4 | Detector simulado + `CONTEO_ML` | Ramón | Sin empezar |
| 5 | Room 7 → 8 | Ramón | Sin empezar |
| 6 | Pantalla de revisión | Ramón | Sin empezar |
| 7 | `Calibre.kt` + medición de su error | Lucas | **Hecho** (PR #55). `validar_calibre.py` listo; espera las fotos `_cal` |
| 8 | Reglas `ml`/`detecciones` + formato para Firestore | Lucas | **Hecho** (PR #55). Falta la subida en `NubeSync` (espera el paso 5) |
| 9 | Entrenar `model_v1` y go/no-go | Lucas | Pipeline probado. `evaluar_tflite.py` mide el `.tflite` de la app contra las metas. Espera las fotos etiquetadas |
| 10 | `DetectorLiteRt` | Ramón | Se puede adelantar con `model-v0-sintetico` |
| 11 | Portal: tarjeta, histograma y recuadros | Jesús | Avanzado en `feature/ml-portal` (tarjeta, histograma y "Ver detecciones" funcionando con el emulador) |
| 12 | Prueba final, `main` y `v2.0.0` | Los 3 | Jue 3 dic |

## 3. Piezas ya hechas que los demás usan (PR #55)

El PR #55 se fusionó el 25-nov: estos archivos ya están en `feature/ml-conteo`. Para tenerlos en tu rama:
`git fetch origin && git merge origin/feature/ml-conteo`.

> **`main` ya tiene `feature/ml-conteo` (PR #56, fusionado por error el 25-nov).** No se publicó nada y la app se
> comporta igual (el código nuevo aún no se usa). Se decidió **no revertirlo**: revertir una fusión complica la
> fusión final. Sigue valiendo: **ningún PR hacia `main`** hasta el paso 12.

| Archivo | Para qué sirve | Quién lo usa |
|---|---|---|
| `data/ml/SalidaRfDetr.kt` | `decodificar(cajas, puntajes, columnas = 4)` convierte la salida del `.tflite` en `CajaDetectada(clase, confianza, x, y, ancho, alto)` normalizadas 0..1, ya con umbral (0,40) y sin duplicados. `etiqueta(clase, labels)` da `chorito`, `cholga` o `referencia` | Ramón, paso 10 |
| `data/ml/Calibre.kt` | `mmPorPixel(anchoRefPx, altoRefPx)` con el recuadro de la referencia (null si no es cuadrado); `largoMm(luminancia, ancho, alto, mmPorPixel)` mide una concha dentro de su recuadro (luminancia 0..255 del recorte); `esBorrosa(...)` avisa si la foto está desenfocada; `ResumenCalibre.de(largos)` da promedio, mínimo, máximo y rangos de 5 mm | Ramón, paso 6 (calcular `mm` de cada detección) |
| `data/nube/ResultadoMlNube.kt` | `aCampos(version, detecciones, mmPorPx, editado)` arma `ml` y `detecciones` con el formato del contrato | Lucas, en `NubeSync` |
| `data/nube/ProteccionDictamen.kt` | Si la muestra ya tiene dictamen, el teléfono no reescribe `ml` ni `detecciones` | Automático |
| `firestore.rules` | Acepta y valida `ml` (conteos 0..2000) y `detecciones` (máx. 600). **No publicadas** | Todos (emulador) |

## 4. Datos verificados del modelo (no adivinar)

- Modelo: **RF-DETR Nano** (Apache 2.0), `rfdetr==1.12.0`. Se cambió desde MediaPipe Model Maker porque no instala en Colab.
- Entrada `[1, 384, 384, 3]` **float32**: RGB `/255` y luego `(x − [0.485, 0.456, 0.406]) / [0.229, 0.224, 0.225]`.
- Salida 0 = puntajes `[1, 300, 4]` (logits; columnas **0 = chorito, 1 = cholga, 2 = referencia, 3 sin uso**).
  Salida 1 = cajas `[1, 300, 4]` como `cx, cy, w, h` normalizadas 0..1. En INT8 no tienen nombre: **tomarlas por posición**.
- El `.tflite` INT8 pesa ~30 MB.
- Con `foto_prueba_sintetica.jpg`, el modelo de prueba encuentra 28 de 28 individuos con la especie correcta.

## 5. Decisiones abiertas

- Teléfono de prueba para medir el tiempo de inferencia (meta ≤ 1,5 s). En PC con XNNPACK el INT8 tarda 0,22 s, así
  que la meta se ve alcanzable.
- Ver también `plan-fase-final.md` §10.

## 6. Bitácora

Formato: `### AAAA-MM-DD · Nombre` y debajo qué se hizo, qué quedó pendiente y cualquier sorpresa.

### 2026-11-25 · Lucas
- Rama `feature/ml-conteo` con el plan, `CLAUDE.md` e instrucciones por persona. Repo MusselApp-ML y Drive compartidos.
- Script de respaldo de Firestore (`scripts/respaldo-firestore/`).
- PR #55: `Calibre.kt`, `SalidaRfDetr.kt`, `ResultadoMlNube.kt`, `ProteccionDictamen` y reglas para `ml`/`detecciones`.
  224 pruebas unitarias y 27 de reglas en verde; CI en verde.
- Notebook de RF-DETR probado de punta a punta (entrenar, reanudar, evaluar, exportar a TFLite) con un dataset sintético.
  Publicado `model-v0-sintetico` para que Ramón integre la app sin esperar el modelo real.
- **Sorpresa:** al principio supuse que la columna 0 del modelo no se usaba; con un modelo exportado se vio que es
  chorito. Ya corregido en el código, el notebook y las instrucciones.
- Pendiente: generar la clave y correr el respaldo; descargar el dataset público; subida en `NubeSync`.
- Decisión: el PR #55 lo revisan y fusionan Jesús o Ramón cuando terminen su parte. Lo que sigue de Lucas
  (subida en `NubeSync`, `model_v1`) irá en PR nuevos.

### 2026-11-25 (tarde) · Lucas
- `scripts/datos-prueba-emulador/`: carga 6 muestras con `ml` y `detecciones`, fotos dibujadas con los recuadros
  calzando y un supervisor de prueba. Probado: el portal (con `useEmulator`) entra y muestra las 6, y los recuadros
  calzan con las conchas.
- **Sorpresa:** `firebase.json` no tenía configurado el emulador de Auth, así que no arrancaba y el portal no podía
  iniciar sesión. Agregados los puertos de Auth (9099) y Hosting (5000). Además, el emulador debe iniciarse con
  `--project musselapp-demo` (el `projectId` del portal); `jesus.md` decía otro nombre.
- `ramon.md`: orden sugerido si tiene pocos tokens (pasos 4 y 5 primero).

### 2026-11-25 (noche) · Lucas
- **Cambio de protocolo (avisar a todos):** el set de calibre lleva una foto extra `_cal` con solo los 5 individuos
  medidos, en fila de izquierda a derecha en el orden de la planilla. Ver `instrucciones-ml/equipo.md` §3.
- MusselApp-ML: `src/validar_calibre.py` mide el error del calibre (meta ≤ 3 mm) con el mismo cálculo de la app.
  Autoprueba con fotos dibujadas: error máximo 0,3 mm.
- **Sorpresa:** si una concha vecina asoma en el recuadro, el largo salía inflado (+3,1 mm). `Calibre.kt` ahora mide
  solo la mancha oscura más grande (commit en `feature/ml-reglas`, PR #55; 225 pruebas en verde).
- **Sorpresa 2:** RF-DETR lee las fotos sin aplicar la rotación EXIF, pero Label Studio etiqueta la foto ya girada:
  con fotos verticales del celular las etiquetas quedarían corridas. El notebook ahora endereza cada foto al
  prepararla y avisa si un tamaño no calza. Lo mismo vale para la app (nota en `ramon.md`, paso 10).

### 2026-11-25 (después de mediodía) · Lucas
- Jesús encontró un error en `scripts/datos-prueba-emulador/cargar.mjs`: `detecciones` había quedado dentro de un
  comentario y no se subía (por eso su portal decía "Sin calibre"). Corregido. Además, las conchas dibujadas medían
  ~27 mm y quedaban fuera del histograma (30–95 mm): ahora miden 45–75 mm, cada foto tiene su escala (como en la
  realidad) y la referencia va arriba a la derecha (a la izquierda la tapaba el botón "Ver detecciones").
  Probado con el portal de `feature/ml-portal` en el emulador: tarjeta, histograma, aviso de corrección y recuadros OK.
- PR #55 fusionado por Jesús en `feature/ml-conteo`. PR #56 (`feature/ml-conteo` → `main`) fusionado por error;
  no se revierte (ver §3). `CLAUDE.md` ahora lo dice explícito.
- MusselApp-ML: `src/evaluar_tflite.py` mide el `.tflite` (el archivo que corre en el teléfono) contra las metas del
  go/no-go, con el mismo pre y post-proceso que la app; compara INT8 con FP16/FP32 y exporta predicciones COCO para
  `validar_calibre.py`. Con `model-v0-sintetico`: INT8 error de conteo 1,4 %, recall de chorito 98,3 %, confusión 0 %.
- **Sorpresa:** en Windows, el intérprete de `ai-edge-litert` corre sin XNNPACK: 2,5–5 s por foto en vez de 0,22 s, y
  con el FP16 daba resultados erróneos. El "~5 s en PC" que decía `ramon.md` venía de ahí; corregido. En Android
  XNNPACK viene activo por defecto (no apagarlo).
