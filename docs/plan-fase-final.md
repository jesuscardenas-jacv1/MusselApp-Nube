# Plan de la fase final · Conteo automático con Machine Learning

**Proyecto:** MusselApp (DSY1105, sección 004V, Duoc UC Puerto Montt)
**Equipo:** Jesús Cárdenas (Front-end) · Ramón Osorio (UI/UX & Mobile Developer) · Lucas Saldivia (QA & Data Engineer)
**Fecha de aprobación:** 2026-11-25 (aprobado por Lucas Saldivia; pendiente de confirmación del equipo)
**Rama de trabajo:** `feature/ml-conteo` · **Punto seguro de retorno:** release `v1.3.2`
**Entrega:** viernes 4 de diciembre de 2026 (podría extenderse)
**Entrenamiento:** [lucsaldivia-ux/MusselApp-ML](https://github.com/lucsaldivia-ux/MusselApp-ML) · dataset en Google Drive (`MusselApp-ML/dataset_v1/`)

---

## 1. Resumen ejecutivo

- **Veredicto: alcance escalonado.**
  1. Conteo y especie (chorito/cholga) en el teléfono, con revisión del operario. Este es el **mínimo presentable**.
  2. Calibre en mm con una referencia impresa de 40 mm.
  3. Datos agregados en el portal.
- **Estrategia: híbrida.**
  - El entrenamiento va en el repositorio aparte `MusselApp-ML` (Python, Colab/Kaggle). El dataset vive en Google Drive.
  - La integración va en la rama `feature/ml-conteo` de esta app.
  - Ambas partes se conectan con un contrato fijo (§5). Mientras no esté el modelo real, la app usa un detector simulado.
- **Mayor riesgo:** conseguir y etiquetar fotos reales. Hacen falta ~150 fotos y ≥ 500 individuos por especie.
- **Ventaja:** la app ya tiene el conteo con marcas sobre la foto (`ConteoMarcas`, `ConteoSobreFoto`) y los campos `conteoIndM`, `cholgasIndM`, `tallaMm` y `tramoCm`. El modelo solo pre-llena las marcas.
- **Costo cero:**
  - Firebase sigue en Spark.
  - No se usa Cloud Storage, que exige Blaze desde el 3-feb-2026.
  - La inferencia corre en el teléfono.
  - Todas las licencias son Apache 2.0 o MIT.

## 2. Hallazgos del repositorio (al 2026-11-25, `main` en `5720fa7`)

| Componente | Estado | Archivos | Cambio previsto | Riesgo |
|---|---|---|---|---|
| Room | Versión 7, `exportSchema = true` | `data/local/MusselDatabase.kt`, `Migraciones.kt` | `DE_7_A_8`: tabla `detecciones` y columnas `ml_*` en `muestras` | Medio |
| Conteo manual | Marcas sobre la foto → ind/m | `ui/muestra/ConteoMarcas.kt`, `ConteoSobreFoto.kt` | Las marcas llevan especie y se pre-llenan con el modelo | Bajo |
| Cámara | CameraX 1.6.2 + Photo Picker | `app/build.gradle.kts`, `CaptureScreen.kt` | Guía de la referencia y aviso de foto borrosa | Bajo |
| Sincronización | `NubeSync` (transacción, primer dictamen gana), WorkManager | `data/nube/NubeSync.kt` | Subir `ml` y `detecciones` | Medio |
| Fotos | Base64 en `fotos/{id}`, ≤ 500 KB | `NubeSync.kt` | Bajar a ~200 KB (cuota de 1 GiB) | Bajo |
| Reglas | v2 publicadas (#50), lista cerrada de campos | `firestore.rules`, `firestore-tests/` | Agregar `ml` y `detecciones` | **Alto** si se olvida |
| Roles | Teléfono anónimo; supervisor con `supervisores/{uid}` | `firestore.rules` | Se mantiene; Custom Claims opcionales | Medio |
| Portal | HTML + JS en Hosting (Spark) | `web-supervisor/index.html` | Tarjeta de conteo, histograma y recuadros | Bajo |
| CI | GitHub Actions; repo público | `.github/workflows/` | Sin cambios | Bajo |

## 3. Matriz de riesgos

| Riesgo | Prob. | Impacto | Mitigación |
|---|---|---|---|
| Pocas fotos reales | Alta | Alto | Recolección desde la semana 1; go/no-go; plan B |
| Confusión chorito/cholga | Media | Alto | Fotos nítidas y cercanas; ≥ 500 instancias por clase; revisión manual obligatoria |
| Individuos amontonados | Alta | Medio | Protocolo de captura; teselado 2×2 + NMS |
| Calibre impreciso | Media | Medio | Referencia como clase del modelo; foto cenital; validación con pie de metro |
| Migración de Room con pérdida de datos | Baja | Alto | Solo `ADD COLUMN` + `CREATE TABLE`; prueba con `MigrationTestHelper` |
| Campos nuevos ausentes en las reglas | Media | Alto | Pruebas de reglas con la subida real nueva |
| Pasar la cuota gratuita de Firestore | Media | Medio | Fotos de ~200 KB; detecciones compactas; purga de fotos antiguas validadas |
| Licencia copyleft | Media | Alto | No usar Ultralytics YOLO (AGPL) |
| Inferencia lenta o APK pesado | Media | Medio | int8, entrada de 384 px, modelo en `assets` |
| Falla en la demo | Media | Alto | Feature flag; fotos probadas; conteo manual intacto |

## 4. Herramientas y servicios (todos gratuitos)

| Herramienta | Uso | Límites | Licencia |
|---|---|---|---|
| Firestore (Spark) | Muestras, resumen y detecciones | 1 GiB, 50.000 lecturas, 20.000 escrituras y 20.000 borrados al día, 10 GiB de salida al mes | Servicio |
| Firebase Auth / Hosting (Spark) | Sesiones / portal | Gratis en Spark | Servicio |
| Cloud Storage | **No se usa** | **[TIENE COSTO]**: exige Blaze desde el 3-feb-2026 | — |
| LiteRT / MediaPipe Tasks | Inferencia en Android | — | Apache 2.0 |
| MediaPipe Model Maker | Entrenamiento (opción A) | Backbones MobileNet | Apache 2.0 |
| RF-DETR Nano | Detector alternativo (opción B) | Tamaños Nano a Large | Apache 2.0 |
| Google Colab / Kaggle | Entrenamiento | Colab sin garantía; Kaggle ~30 h de GPU a la semana (no es oficial) | Servicio |
| Label Studio Community | Etiquetado local | Sin límites | Apache 2.0 |
| Google Drive | Dataset y checkpoints | 15 GB por cuenta | Servicio |
| Hugging Face Hub | Publicar modelos | Repos públicos gratis | — |
| GitHub Actions | CI | Ilimitado en repos públicos | — |

## 5. Contrato de integración del modelo

Modelo elegido: **RF-DETR Nano** (Apache 2.0, `rfdetr==1.12.0`). Contrato vigente y detallado: [`CONTRATO.md`](https://github.com/lucsaldivia-ux/MusselApp-ML/blob/main/CONTRATO.md) del repo de entrenamiento.

```json
{
  "version": "model_v1",
  "arquitectura": "RF-DETR Nano (Apache 2.0)",
  "archivo": "model_v1.tflite",
  "entrada": { "formato": "NHWC float32", "normalizacion": "ImageNet (media 0.485,0.456,0.406; desv. 0.229,0.224,0.225)" },
  "etiquetas": ["chorito", "cholga", "referencia"],
  "umbral_confianza": 0.40,
  "referencia_mm": 40.0
}
```

- Las salidas del `.tflite` no tienen nombre: cajas = tensor de rango 3 con última dimensión 4 (cx, cy, w, h normalizados); puntajes = el otro tensor de rango 3 (con sigmoide). El orden se confirma con el `.rfdetr.json` del modelo real (paso 9).
- La exportación a TFLite de RF-DETR es experimental; la alternativa es ONNX Runtime para Android (MIT).
- Archivos en la app: `app/src/main/assets/ml/model_vN.tflite`, `labels.txt` y `modelo.json`.
- Para actualizar el modelo: reemplazar los 3 archivos y subir `versionCode`. Cada muestra guarda `modelo_version`.

**Formato en Firestore** (en `muestras/{id}`):
```js
ml: { version: "model_v1", total: 64, choritos: 58, cholgas: 6, mmPorPx: 0.21,
      tallaChoritoMm: 54.2, tallaCholgaMm: 71.0, editado: true },
detecciones: [ { e: "C", p: 0.93, x: 120, y: 340, w: 60, h: 28, mm: 55, o: "M" } ]
// e: "C" chorito, "K" cholga · p: confianza · x,y,w,h en escala 0-1000 · mm: largo · o: "M" modelo, "O" operario
```

## 6. Estrategia de ramas

| Rama | Sale de | Responsable | Destino del PR |
|---|---|---|---|
| `feature/ml-conteo` | `main` | Lucas | `main` (al final, con revisión) |
| `feature/ml-app` | `feature/ml-conteo` | Ramón | `feature/ml-conteo` |
| `feature/ml-portal` | `feature/ml-conteo` | Jesús | `feature/ml-conteo` |
| `feature/ml-reglas` | `feature/ml-conteo` | Lucas | `feature/ml-conteo` |

- **Sincronizar** dos veces por semana: `git merge origin/feature/ml-conteo`.
- **Merge final a `main`:** PR revisado por otro integrante, con este checklist:
  - CI en verde;
  - migración 7→8 probada;
  - prueba de punta a punta en un teléfono;
  - feature flag apagable.
- **Volver atrás:**
  - App: `git revert -m 1 <merge>`, o una rama de rescate con `git switch -c rescate v1.3.2`.
  - Reglas: `git show ed1840f:firestore.rules`.
  - Datos: restaurar el respaldo JSON.
  - Un APK v1.3.2 no se instala sobre la base v8 sin desinstalar.
- **Release final:** `v2.0.0`.

## 7. Calendario (entrega el 4 de diciembre)

| Fechas | App (Ramón) | Portal (Jesús) | Datos y modelo (Lucas) | Los 3 |
|---|---|---|---|---|
| Mié 25 – Vie 27 nov | Pasos 4 y 5 | Pasos 1 a 3 del portal | Paso 0 · pre-entrenamiento con datos públicos · notebook | **Fotos (viernes 27)** |
| Sáb 28 – Lun 30 nov | Paso 6 | Tarjeta y gráfico | Pasos 7 y 8 | **Etiquetado (lunes 30)** |
| Mar 1 – Mié 2 dic | Paso 10 | Recuadros sobre la foto | Paso 9 · **go/no-go (miércoles 2)** | — |
| Jue 3 dic | — | — | — | Paso 12: prueba final, PR a `main`, `v2.0.0` |
| Vie 4 dic | — | — | — | Presentación |

**Datos de internet:** solo para pre-entrenar la detección de "individuo", con licencia CC BY 4.0 o CC0, registrados en `DATOS_EXTERNOS.md` del repo de entrenamiento. No reemplazan las fotos propias, que son las únicas que enseñan a distinguir chorito de cholga.

## 8. Plan paso a paso

| Estado | # | Paso | Responsable | h | Listo cuando |
|---|---|---|---|---|---|
| [ ] | 0 | Respaldo de Firestore a JSON (script listo en `scripts/respaldo-firestore/`; falta ejecutarlo) + `.gitignore` de llaves (hecho) | Lucas | 2 | JSON guardado fuera de git |
| [ ] | 1 | Recolectar 150 fotos con la referencia de 40 mm (protocolo del documento de equipo) | Los 3 | 10 | Fotos en Drive + planilla completa |
| [ ] | 2 | Etiquetado en Label Studio (COCO) + revisión cruzada | Los 3 (Jesús +20 fotos) | 12 | ≥ 500 instancias por especie |
| [ ] | 3 | Split sin fuga (por bandeja) + notebook de entrenamiento con checkpoints (listos en `MusselApp-ML`; falta correrlo con fotos reales) | Lucas | 8 | El notebook corre de punta a punta |
| [ ] | 4 | `DetectorMuestra` + `DetectorSimulado` + flag `CONTEO_ML` | Ramón | 5 | Pruebas unitarias del simulado |
| [ ] | 5 | Room 7→8 + `DeteccionEntity` / `DeteccionDao` + prueba de migración | Ramón | 5 | La migración conserva las muestras |
| [ ] | 6 | Revisión con especie sobre `ConteoSobreFoto` | Ramón | 8 | Cambiar especie, agregar y eliminar funcionan |
| [x] | 7 | `Calibre.kt` (mm por píxel y largo por PCA) + validaciones de foto (rama `feature/ml-reglas`, 10 pruebas) | Lucas | 6 | Pruebas unitarias en verde |
| [ ] | 8 | Reglas para `ml` / `detecciones` + pruebas (listas en `feature/ml-reglas`, 27 pruebas) + subida en `NubeSync` (espera Room v8 de Ramón) | Lucas | 5 | Pruebas de reglas en verde (emulador) |
| [ ] | 9 | Entrenar, evaluar, exportar int8 → **go/no-go** | Lucas | 8 | Métricas del §8 |
| [ ] | 10 | `DetectorLiteRt` con `model_v1` + medición de tiempos | Ramón | 6 | ≤ 1,5 s en gama media |
| [ ] | 11 | Portal: tarjeta de conteo, histograma y recuadros | Jesús | 8 | Se ve en el emulador |
| [ ] | 12 | Prueba de punta a punta + PR a `main` + publicar reglas + `v2.0.0` | Los 3 | 4 | Checklist de merge completo |

## 9. Go/no-go, pruebas, demo y plan B

**Métricas mínimas** (conjunto de prueba, fotos nunca vistas):

| Métrica | Meta |
|---|---|
| mAP@0,5 por especie | ≥ 0,75 |
| Recall de chorito | ≥ 0,85 |
| Confusión chorito/cholga | ≤ 10 % |
| Error medio absoluto del conteo | ≤ 10 % del total |
| Error del calibre contra pie de metro | ≤ 3 mm |
| Inferencia en gama media | ≤ 1,5 s |

**Pruebas:**
- unitarias: calibre, agregados y mapeo de la salida del modelo;
- integración: Room ↔ Firestore offline y online, y migración 7→8;
- reglas: en el emulador;
- rendimiento: en un teléfono real;
- manual de punta a punta: captura → conteo → corrección → sincronización → validación en el portal.

**Checklist de demo:**
1. foto preparada;
2. conteo automático;
3. corregir una especie;
4. guardar sin internet y reconectar;
5. el portal muestra recuadros e histograma;
6. validar.

Si algo falla en vivo: apagar el flag y mostrar el conteo manual.

**Plan B:** conteo asistido. El operario marca y elige la especie, y el calibre se calcula con la referencia. Se reutilizan la pantalla de revisión, Room v8, las reglas y el portal, y se presentan las métricas reales del notebook.

## 10. Pendientes por definir

- [x] Fecha de entrega: viernes 4 de diciembre (podría extenderse).
- [ ] Pauta de la fase final (¿un solo repositorio?).
- [x] Choritos y cholgas reales: los reúne el equipo.
- [ ] ¿Distinguir también el choro zapato?
- [ ] Teléfonos de prueba.
- [ ] Condiciones reales de captura.
- [ ] Bajar las fotos a ~200 KB y purgar las antiguas validadas.
- [ ] ¿Supervisor de la app con cuenta propia y Custom Claims en esta fase?
- [ ] Horas por semana de cada integrante.

## 11. Registro de cambios al plan

| Fecha | Cambio | Motivo |
|---|---|---|
| 2026-11-25 | Versión inicial | Aprobación del plan |
| 2026-11-25 | Calendario por días hasta el 4-dic; pre-entrenamiento con datasets públicos CC BY 4.0/CC0; repo `MusselApp-ML` y carpeta de Drive creados | Fecha de entrega confirmada por el equipo |
| 2026-11-25 | Modelo: RF-DETR Nano en vez de MediaPipe Model Maker; entrada float32 normalizada ImageNet; reglas de `ml` livianas | Model Maker no instala en el Python 3.12 de Colab; Firestore limita cada petición a 1.000 expresiones |
