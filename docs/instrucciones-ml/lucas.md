# Instrucciones para Lucas Saldivia · QA & Data Engineer

Fase final de MusselApp (conteo con ML). Antes, lee [`equipo.md`](equipo.md) y el estado actual en [`estado-ml.md`](../estado-ml.md).

## Preparación

- [x] Rama `feature/ml-conteo` con el plan, las instrucciones y `CLAUDE.md`.
- [x] Repositorio de entrenamiento: https://github.com/lucsaldivia-ux/MusselApp-ML (Jesús y Ramón invitados).
- [x] Carpeta de Drive `MusselApp-ML` compartida con Jesús y Ramón como editores.
- [x] Script de respaldo en `scripts/respaldo-firestore/`.
- [ ] Imprimir la referencia de 40 mm al 100 % y comprobar que la línea de control mide 100 mm.
- [ ] Ejecutar el respaldo de Firestore (ver [`scripts/respaldo-firestore/LEEME.md`](../../scripts/respaldo-firestore/LEEME.md)).

## Al llegar a casa (25-nov, en este orden)

Cada punto es corto; el 3 queda corriendo solo en Colab mientras haces los demás.

1. **Respaldo de Firestore (≈10 min).** Consola de Firebase → ⚙️ → Cuentas de servicio → *Generar nueva clave
   privada*. Guárdala **fuera del repo** (por ejemplo `Documentos\claves-musselapp\`) y sigue
   [`scripts/respaldo-firestore/LEEME.md`](../../scripts/respaldo-firestore/LEEME.md). Copia la carpeta
   `respaldo-AAAA-MM-DD/` a un lugar seguro.
2. **Dataset público (≈10 min).** Crea la cuenta gratis en Roboflow, abre
   https://universe.roboflow.com/stephen-7b2qu/mussel-l4ogs → *Download Dataset* → formato **COCO** → *Download zip
   to computer*. Sube el `.zip` a Drive `MusselApp-ML/externos/` (crea la carpeta).
3. **Pre-entrenamiento (queda corriendo).** Abre `notebooks/entrenar_rfdetr.ipynb` de MusselApp-ML en Colab con GPU
   T4, corre la **sección 1** y luego la **sección 3** (la 2 no hace falta todavía). Deja la pestaña abierta; si se
   corta, vuelve a correr la 1 y la 3 y sigue desde donde quedó.
4. **Referencia de 40 mm.** Imprímela al 100 % (sin "ajustar a la página") y mide la línea de control: 100 mm.
5. **Tus fotos (M200–M299)** con el protocolo nuevo de [`equipo.md`](equipo.md) §3: luz de los dos costados, fondo
   blanco o blanco hueso mate, nada cortado en el borde. Anota cada bandeja en `planilla_fotos`.

## Mis pasos

| Paso | Qué | Fecha |
|---|---|---|
| 0 | Respaldo de Firestore | Mié 25 |
| 3a | Pre-entrenar con datasets públicos (solo CC BY 4.0 / CC0, registrados en `DATOS_EXTERNOS.md`). Notebook listo: sección 3 corre sola, con `src/preparar_externo.py` | Mié 25 – Vie 27 |
| 1–2 | Mis 50 fotos (`M200`–`M299`) y su etiquetado | Mié 25 – Lun 30 |
| 3b | ✅ Notebook de entrenamiento (`notebooks/entrenar_rfdetr.ipynb`), split por bandeja y checkpoints en Drive; probado con datos sintéticos | Jue 26 – Vie 27 |
| 7 | ✅ `Calibre.kt`: mm por píxel con el cuadrado de 40 mm y largo de cada concha por PCA (PR #55) | Sáb 28 – Dom 29 |
| 8 | ✅ Reglas para `ml` y `detecciones` + 27 pruebas + `ResultadoMlNube` (PR #55). **Falta:** la subida en `NubeSync` cuando esté la base v8 de Ramón | Dom 29 – Lun 30 |
| 7b | ✅ `src/validar_calibre.py` en MusselApp-ML: mide el error del calibre con las fotos `_cal` (mismo cálculo que `Calibre.kt`) | Mié 25 |
| 9b | ✅ `src/evaluar_tflite.py` en MusselApp-ML: mide el `.tflite` INT8 (lo que corre en el teléfono) contra las metas | Mié 25 |
| 9 | Entrenamiento final, métricas (notebook + `evaluar_tflite.py` + `validar_calibre.py`) y **go/no-go**; entregar `model_v1.tflite`, `labels.txt` y `modelo.json` a Ramón | Mar 1 – Mié 2 |
| 12 | Prueba final, PR a `main`, publicar reglas y `v2.0.0` | Jue 3 |

**Metas del paso 9:**
- error del conteo ≤ 10 %;
- recall de chorito ≥ 85 %;
- confusión de especie ≤ 10 %;
- calibre con error ≤ 3 mm;
- ≤ 1,5 s en gama media.

Si no se cumplen: plan B (conteo asistido).
