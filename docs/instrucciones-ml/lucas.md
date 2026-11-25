# Instrucciones para Lucas Saldivia · QA & Data Engineer

Fase final de MusselApp (conteo con ML). Antes, lee [`equipo.md`](equipo.md) y el estado actual en [`estado-ml.md`](../estado-ml.md).

## Preparación

- [x] Rama `feature/ml-conteo` con el plan, las instrucciones y `CLAUDE.md`.
- [x] Repositorio de entrenamiento: https://github.com/lucsaldivia-ux/MusselApp-ML (Jesús y Ramón invitados).
- [x] Carpeta de Drive `MusselApp-ML` compartida con Jesús y Ramón como editores.
- [x] Script de respaldo en `scripts/respaldo-firestore/`.
- [ ] Imprimir la referencia de 40 mm al 100 % y comprobar que la línea de control mide 100 mm.
- [ ] Ejecutar el respaldo de Firestore (ver [`scripts/respaldo-firestore/LEEME.md`](../../scripts/respaldo-firestore/LEEME.md)).

## Mis pasos

| Paso | Qué | Fecha |
|---|---|---|
| 0 | Respaldo de Firestore | Mié 25 |
| 3a | Pre-entrenar con datasets públicos (solo CC BY 4.0 / CC0, registrados en `DATOS_EXTERNOS.md`) | Mié 25 – Vie 27 |
| 1–2 | Mis 50 fotos (`M200`–`M299`) y su etiquetado | Mié 25 – Lun 30 |
| 3b | ✅ Notebook de entrenamiento (`notebooks/entrenar_rfdetr.ipynb`), split por bandeja y checkpoints en Drive; probado con datos sintéticos | Jue 26 – Vie 27 |
| 7 | ✅ `Calibre.kt`: mm por píxel con el cuadrado de 40 mm y largo de cada concha por PCA (PR #55) | Sáb 28 – Dom 29 |
| 8 | ✅ Reglas para `ml` y `detecciones` + 27 pruebas + `ResultadoMlNube` (PR #55). **Falta:** la subida en `NubeSync` cuando esté la base v8 de Ramón | Dom 29 – Lun 30 |
| 9 | Entrenamiento final, métricas y **go/no-go**; entregar `model_v1.tflite`, `labels.txt` y `modelo.json` a Ramón | Mar 1 – Mié 2 |
| 12 | Prueba final, PR a `main`, publicar reglas y `v2.0.0` | Jue 3 |

**Metas del paso 9:**
- error del conteo ≤ 10 %;
- recall de chorito ≥ 85 %;
- confusión de especie ≤ 10 %;
- calibre con error ≤ 3 mm;
- ≤ 1,5 s en gama media.

Si no se cumplen: plan B (conteo asistido).
