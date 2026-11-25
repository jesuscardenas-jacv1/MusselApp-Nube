# Prueba final de la fase ML · Jueves 3 de diciembre (paso 12)

> **Para los asistentes de IA:** esta es la lista que se recorre el día 3 antes de unir a `main`. No se marca nada
> como hecho sin haberlo probado. Si algo falla, se anota en la columna "Resultado" y en la bitácora
> ([`estado-ml.md`](estado-ml.md)), y se decide con los tres si se arregla o se va al plan B.

**Cuándo:** jueves 3 de diciembre, con los tres conectados. **Duración estimada:** 3 a 4 horas.
**Dónde:** rama `feature/ml-conteo` con todos los PR fusionados. Emulador para todo lo que toque Firestore, salvo
el bloque F (publicación), que solo se hace al final y con el OK de los tres.

**Qué se necesita:** un teléfono de prueba (**no** el personal de nadie si se corren pruebas instrumentadas), la
referencia de 40 mm impresa, una bandeja con choritos y cholgas reales (o 3 fotos impresas del dataset de prueba),
y el portal en `localhost` contra el emulador.

## A. La app sigue sana (antes de probar lo nuevo)

| # | Qué se prueba | Cómo | Resultado esperado | Quién | Resultado |
|---|---|---|---|---|---|
| A1 | Compila y pasan las pruebas | `./gradlew assembleDebug testDebugUnitTest` | En verde; **más** de 225 pruebas (las nuevas de Ramón se suman) | Ramón | |
| A2 | Reglas de Firestore | `cd firestore-tests && npm test` | Todas en verde (27 o más) | Lucas | |
| A3 | CI de la rama | Pestaña Actions de GitHub | "Android CI", "Backend" y "Reglas de Firestore" en verde en el último commit | Lucas | |
| A4 | **`CONTEO_ML = false` se comporta como la v1.3.2** | Compilar con el flag apagado; registrar una muestra a mano, con foto, y sincronizar | Igual que la v1.3.2: sin pantalla de revisión ML, sin `ml` ni `detecciones` en Firestore | Ramón | |
| A5 | Migración de Room 7 → 8 | Instalar la v1.3.2 en el teléfono de prueba, crear 2 muestras, instalar el APK nuevo encima | Las 2 muestras siguen ahí, con sus datos y su estado. Prueba `MigrationTestHelper` en verde | Ramón | |

## B. Conteo automático en la app (`CONTEO_ML = true`)

| # | Qué se prueba | Cómo | Resultado esperado | Quién | Resultado |
|---|---|---|---|---|---|
| B1 | Detecta y clasifica | Foto de una bandeja con referencia (protocolo de `equipo.md`) | Recuadros sobre cada individuo, chorito/cholga con su color; el total se parece al conteo a mano (±10 %) | Ramón | |
| B2 | Tiempo | Medir 5 fotos seguidas, sin contar la primera | Mediana ≤ 1,5 s. Anotar el modelo del teléfono | Ramón | |
| B3 | Calibre | Misma foto | Cada concha con su largo en mm; promedio razonable (choritos 45–80 mm) | Lucas | |
| B4 | Foto sin referencia | Foto sin el cuadrado de 40 mm | Cuenta igual, pero **sin** mm ni tallas, y la app lo avisa | Ramón | |
| B5 | Foto girada (vertical) | Tomar la foto con el celular vertical | Los recuadros calzan con las conchas (EXIF bien aplicado) | Ramón | |
| B6 | Corregir especie | Tocar un recuadro y cambiar chorito ↔ cholga | Cambia el color y el conteo por especie | Ramón | |
| B7 | Agregar y quitar | Agregar uno que faltó y quitar uno de más | El total cambia; el agregado queda como "operario" | Ramón | |
| B8 | Guardar | Guardar la muestra | Queda en Pendiente, con `conteoIndM`, `cholgasIndM` y `tallaMm` calculados como siempre (por metro) | Ramón | |

## C. Sincronización (emulador)

| # | Qué se prueba | Cómo | Resultado esperado | Quién | Resultado |
|---|---|---|---|---|---|
| C1 | Sube `ml` y `detecciones` | Guardar con internet | El documento en el emulador trae `ml` y `detecciones` con el formato del contrato (`plan-fase-final.md` §5) | Lucas | |
| C2 | Sin internet | Modo avión → guardar → volver a conectar | La muestra se sube sola al reconectar, completa | Lucas | |
| C3 | Corregida por el operario | Muestra de B6–B7 | `ml.editado = true`; los agregados con `o: "O"` y sin `p` | Lucas | |
| C4 | Protección del dictamen | Validar la muestra en el portal y luego editarla en el teléfono | El teléfono **no** reescribe `ml` ni `detecciones` de una muestra ya revisada | Lucas | |
| C5 | Reglas rechazan datos malos | Prueba de reglas (A2) | Más de 600 detecciones o conteos fuera de 0–2000 se rechazan | Lucas | |

## D. Portal del supervisor (emulador)

| # | Qué se prueba | Cómo | Resultado esperado | Quién | Resultado |
|---|---|---|---|---|---|
| D1 | Datos de prueba | `scripts/datos-prueba-emulador` → `npm run cargar` | Las 6 muestras de prueba se ven bien (ver su `LEEME.md`) | Jesús | |
| D2 | Muestra real de la app | La muestra de C1 | Tarjeta de conteo automático, porcentajes, histograma y "Ver detecciones" con recuadros alineados | Jesús | |
| D3 | Sin `ml` | Muestra de A4 | Se ve **igual que hoy**, sin tarjeta ni errores en la consola | Jesús | |
| D4 | Sin referencia | Muestra de B4 | "Sin calibre", sin histograma roto | Jesús | |
| D5 | Validar y corregir | Validar una y corregir otra | El flujo Pendiente → Validado / Corregido no cambió | Jesús | |
| D6 | Teléfono angosto | Portal en el navegador con ancho de teléfono | Se lee bien, sin barra horizontal | Jesús | |

## E. Demo (ensayo completo, en orden)

Es el checklist de `plan-fase-final.md` §9, cronometrado: foto preparada → conteo automático → corregir una especie →
guardar sin internet y reconectar → el portal muestra recuadros e histograma → validar.

- [ ] Sale en menos de 5 minutos.
- [ ] Si algo falla en vivo, se apaga el flag y se muestra el conteo manual (probarlo una vez).
- [ ] Métricas del modelo listas para mostrar (salida de `evaluar_tflite.py` y `validar_calibre.py`).

## F. Unir a `main` y publicar (solo si A–E están bien y con el OK de los tres)

1. PR `feature/ml-conteo` → `main`, con el CI en verde y la revisión de otro integrante.
2. Subir `versionCode` (7) y `versionName` ("2.0.0") en `app/build.gradle.kts` **antes** del tag.
3. Respaldo de Firestore recién hecho (`scripts/respaldo-firestore/`) antes de publicar reglas.
4. Publicar las reglas: `npx firebase-tools deploy --only firestore:rules` (las prueba antes el emulador; A2).
5. Publicar el portal en Hosting, si cambió.
6. Tag `v2.0.0` desde `main`: `release.yml` publica el APK solo.
7. Instalar el APK publicado en el teléfono de prueba y repetir B1 y D2 contra la base real, con una muestra de prueba
   que luego se borra.

## Si no se llega

- **El modelo no cumple el go/no-go (mié 2):** plan B, conteo asistido (`plan-fase-final.md` §9). Las secciones A, C,
  D y F siguen igual; B se prueba con el operario marcando las conchas.
- **Falla algo de la app el día 3:** se publica igual la v2.0.0 con `CONTEO_ML = false` (queda como la v1.3.2 más
  el portal y las reglas nuevas) y el ML se muestra desde la rama en la demo.
