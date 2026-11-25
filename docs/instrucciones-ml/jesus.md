# Instrucciones para Jesús Cárdenas · Front-end

Fase final de MusselApp (conteo con ML). Antes, lee [`equipo.md`](equipo.md). Reglas para tu asistente: [`CLAUDE.md`](../../CLAUDE.md).

> **Tu misión:** que el supervisor vea en el **portal web** lo que encontró el modelo: **cuántos choritos y cholgas**, sus **calibres** y los **recuadros sobre la foto**. Puedes empezar ya con datos inventados.

**Trabajo común:** ~50 fotos (bandejas `M001`–`M099`) y su etiquetado, **más unas 20 fotos de Lucas**.

## Antes de empezar

- [ ] `git fetch origin` y `git switch -c feature/ml-portal origin/feature/ml-conteo`.
- [ ] Todo va en `web-supervisor/index.html`. Los PR van hacia `feature/ml-conteo`, nunca a `main`.
- [ ] **No publicar** en Hosting hasta el final, y solo con el OK del equipo.

## Paso 1 · Emulador (≈1 h) · Mié 25 – Vie 27 nov

Necesitas Java 21 o superior y Node.

```
npx firebase-tools emulators:start --only firestore,auth,hosting --project demo-musselapp
```

En `iniciarFirebase()` (cerca de la línea 1449), después de crear `db`:

```js
if (location.hostname === 'localhost' || location.hostname === '127.0.0.1') {
  db.useEmulator('127.0.0.1', 8080);
  firebase.auth().useEmulator('http://127.0.0.1:9099');
}
```

El portal queda en `http://127.0.0.1:5000`. En la interfaz del emulador crea un supervisor (usuario + `supervisores/{uid}`) y muestras de prueba.

## Paso 2 · Formato de los datos

```js
ml: { version: "model_v1", total: 64, choritos: 58, cholgas: 6, mmPorPx: 0.21,
      tallaChoritoMm: 54.2, tallaCholgaMm: 71.0, editado: true },
detecciones: [
  { e: "C", p: 0.93, x: 120, y: 340, w: 60, h: 28, mm: 55, o: "M" },
  { e: "K", p: 0.81, x: 610, y: 205, w: 75, h: 33, mm: 71, o: "O" }
]
// e: "C" chorito, "K" cholga · p: confianza 0-1 · x,y,w,h en escala 0-1000 (0,0 = arriba a la izquierda)
// mm: largo en mm (puede faltar) · o: "M" modelo, "O" agregado por el operario
```

Crea 3 o 4 muestras de prueba con entre 20 y 60 detecciones inventadas. La foto va en `fotos/{mismo id}`.

## Paso 3 · Leer los campos nuevos (≈1 h)

En `normalizar(id, d)` (cerca de la línea 1199) agrega `ml` (por defecto `null`) y `detecciones` (por defecto `[]`). Una muestra sin `ml` debe verse **igual que hoy**.

## Paso 4 · Tarjeta "Conteo automático" (≈3 h) · Sáb 28 – Lun 30 nov

En `abrirDetalle` / `dibujarDetalle` (cerca de la línea 1851), si la muestra trae `ml`:
- Choritos y cholgas, con su porcentaje del total.
- Calibre promedio, mínimo y máximo por especie, calculado desde `detecciones[].mm` (se ignoran las que no traen `mm`).
- Si `ml.editado` es verdadero: "El operario corrigió el conteo automático", más cuántas detecciones agregó (`o: "O"`).
- `ml.version` en letra chica.

Todo texto de Firestore se muestra con **`esc()`**, como el resto del portal.

## Paso 5 · Gráfico de calibres (≈2 h)

Chart.js (cerca de la línea 1731): histograma por rangos de 5 mm (30–35 … 90–95) con dos series: choritos `#0F766E` y cholgas `#C76C05`.

## Paso 6 · Recuadros sobre la foto (≈2 h) · Mar 1 – Mié 2 dic

- Un `<canvas>` encima de la foto, con el interruptor **"Ver detecciones"**.
- Cada recuadro se dibuja en `x * anchoFoto / 1000`, `y * altoFoto / 1000` (lo mismo con `w` y `h`).
- Chorito en verde y cholga en ámbar; las agregadas por el operario, con línea punteada.
- Se redibuja al cambiar el tamaño de la ventana y funciona en el celular.

**Opcional:** "% de cholgas por centro" en el Dashboard; columnas `choritos`, `cholgas` y calibre promedio en la exportación.

## Lo que no te toca

`app/` es de Ramón · `firestore.rules` y la subida desde el teléfono son de Lucas.

## Checklist antes del PR

- [ ] Una muestra **sin** `ml` se ve igual que hoy.
- [ ] Una muestra **con** `ml` muestra la tarjeta, el gráfico y los recuadros.
- [ ] Probado en el computador y en el celular.
- [ ] Sin errores en la consola del navegador (F12).
- [ ] La conexión al emulador solo se activa en `localhost`.
