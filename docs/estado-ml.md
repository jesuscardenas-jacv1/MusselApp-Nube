# Estado de la fase final · Bitácora

> **Para los asistentes de IA:** lee esto **primero**. Dice qué está hecho, dónde está cada cosa y qué sigue.
> Al terminar una sesión de trabajo, **agrega una entrada al final** (fecha, quién, qué hiciste, qué quedó pendiente)
> y actualiza las tablas si cambió algo. El plan completo está en [`plan-fase-final.md`](plan-fase-final.md).

**Última actualización:** 2026-11-25 (Lucas)

## 1. Dónde está cada cosa

| Qué | Dónde |
|---|---|
| Plan, calendario y contrato | `docs/plan-fase-final.md` |
| Instrucciones por persona | `docs/instrucciones-ml/` (`equipo.md`, `ramon.md`, `jesus.md`, `lucas.md`) |
| Reglas para la IA | `CLAUDE.md` (raíz) |
| Código ya hecho de Lucas (calibre, post-proceso del modelo, formato para Firestore, reglas) | Rama `feature/ml-reglas`, **PR #55** hacia `feature/ml-conteo` |
| Entrenamiento (notebook, split, dataset sintético, contrato del `.tflite`) | https://github.com/lucsaldivia-ux/MusselApp-ML |
| Modelo de prueba para integrar la app | Release `model-v0-sintetico` de MusselApp-ML |
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
| 7 | `Calibre.kt` | Lucas | **Hecho** (PR #55) |
| 8 | Reglas `ml`/`detecciones` + formato para Firestore | Lucas | **Hecho** (PR #55). Falta la subida en `NubeSync` (espera el paso 5) |
| 9 | Entrenar `model_v1` y go/no-go | Lucas | Pipeline probado; espera las fotos etiquetadas |
| 10 | `DetectorLiteRt` | Ramón | Se puede adelantar con `model-v0-sintetico` |
| 11 | Portal: tarjeta, histograma y recuadros | Jesús | Sin empezar |
| 12 | Prueba final, `main` y `v2.0.0` | Los 3 | Jue 3 dic |

## 3. Piezas ya hechas que los demás usan (PR #55)

Mientras el PR #55 no esté fusionado, estos archivos **no están** en `feature/ml-conteo`. Si los necesitas antes,
trae la rama: `git fetch origin && git merge origin/feature/ml-reglas`.

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

- ¿Quién revisa y fusiona el PR #55? (otro integrante, según el plan)
- Teléfono de prueba para medir el tiempo de inferencia (meta ≤ 1,5 s).
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
- Pendiente: generar la clave y correr el respaldo; descargar el dataset público; revisión del PR #55; subida en `NubeSync`.
