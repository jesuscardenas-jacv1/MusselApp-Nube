# Instrucciones para Jesús Cárdenas · Front-end

Fase final de MusselApp (conteo con ML). Antes, lee [`equipo.md`](equipo.md) y el estado actual en [`estado-ml.md`](../estado-ml.md). Reglas para tu asistente: [`CLAUDE.md`](../../CLAUDE.md).

> **Tu misión:** que el supervisor vea en el **portal web** lo que encontró el modelo: **cuántos choritos y cholgas**, sus **calibres** y los **recuadros sobre la foto**. Puedes empezar ya con datos inventados.

**Trabajo común:** ~50 fotos (bandejas `M001`–`M099`) y su etiquetado, **más unas 20 fotos de Lucas**.

## Antes de empezar

- [ ] `git fetch origin` y `git switch -c feature/ml-portal origin/feature/ml-conteo`.
- [ ] Todo va en `web-supervisor/index.html`. Los PR van hacia `feature/ml-conteo`, nunca a `main`.
- [ ] **No publicar** en Hosting hasta el final, y solo con el OK del equipo.

## Paso 1 · Emulador (≈1 h) · Mié 25 – Vie 27 nov

Necesitas Java 21 o superior y Node.

```
npx firebase-tools emulators:start --only firestore,auth,hosting --project musselapp-demo
```

Usa `musselapp-demo`, el mismo `projectId` del portal: el emulador separa los datos por proyecto. `firebase.json` ya
trae los puertos de Auth (9099) y Hosting (5000).

En `iniciarFirebase()` (cerca de la línea 1449), justo después de `db = firebase.firestore();`:

```js
if (location.hostname === 'localhost' || location.hostname === '127.0.0.1') {
  db.useEmulator('127.0.0.1', 8080);
  firebase.auth().useEmulator('http://127.0.0.1:9099');
}
```

El portal queda en `http://127.0.0.1:5000`.

**Datos de prueba ya listos:** en otra terminal, `cd scripts/datos-prueba-emulador`, `npm install` y `npm run cargar`.
Carga 6 muestras con `ml` y `detecciones` (con y sin correcciones, sin referencia, una grande de 156 y una sin `ml`),
sus fotos dibujadas con los recuadros calzando, y un supervisor: **RUT `22.222.222-2`, PIN `1234`** (solo existe en el
emulador). Detalle en [`scripts/datos-prueba-emulador/LEEME.md`](../../scripts/datos-prueba-emulador/LEEME.md).
Probado el 2026-11-25: el portal entra y muestra las 6.

## Paso 2 · Formato de los datos

```js
ml: { version: "model_v1", total: 64, choritos: 58, cholgas: 6, mmPorPx: 0.21,
      tallaChoritoMm: 54.2, tallaCholgaMm: 71.0, editado: true },
detecciones: [
  { e: "C", p: 0.93, x: 120, y: 340, w: 60, h: 28, mm: 55, o: "M" },
  { e: "K", x: 610, y: 205, w: 75, h: 33, mm: 71, o: "O" }   // agregada por el operario: sin p
]
// e: "C" chorito, "K" cholga · p: confianza 0-1 · x,y,w,h en escala 0-1000 (0,0 = arriba a la izquierda)
// mm: largo en mm (puede faltar) · o: "M" modelo, "O" agregado por el operario
```

Detalles que ya fija el código del teléfono (`ResultadoMlNube.kt`, PR #55 ya fusionado):
- Las detecciones agregadas por el operario (`o: "O"`) **no traen `p`**.
- `mm`, `ml.mmPorPx`, `ml.tallaChoritoMm` y `ml.tallaCholgaMm` faltan si la foto no tenía la referencia.
- Las reglas aceptan hasta **600** detecciones y conteos de hasta 2000. Las eliminadas por el operario no se suben.

No hace falta crear muestras a mano: usa el script de arriba. La foto va en `fotos/{mismo id}`.

Ojo en el paso 6: la foto del detalle usa `object-fit: cover`. Si la proporción de la foto no calza con su recuadro,
calcula el área visible antes de dibujar los recuadros (o dibújalos en la vista "Ampliar").

## Paso 3 · Leer los campos nuevos (≈1 h)

En `normalizar(id, d)` (cerca de la línea 1199) agrega `ml` (por defecto `null`) y `detecciones` (por defecto `[]`). Una muestra sin `ml` debe verse **igual que hoy**.

> **Regla del caso ALDEMAR (no negociable):** el conteo con ML se presenta como **experimental**. Nunca decir ni
> escribir que la app "cuenta automáticamente" de forma real o validada: el operario siempre revisa y corrige, y en la
> demo se muestra el error contra conteos a mano. Datos solo ficticios o autorizados; nada interno de ALDEMAR en el
> repo, videos o presentaciones.

## Paso 4 · Tarjeta "Conteo automático" (≈3 h) · Sáb 28 – Lun 30 nov

Título de la tarjeta: **"Conteo automático (experimental)"** (regla del caso ALDEMAR).

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

## Revisión de Lucas a `feature/ml-portal` (25-nov)

Probado con los datos de prueba corregidos en el emulador: la tarjeta, los porcentajes, la tabla de calibres, el
histograma, el aviso de corrección y "Ver detecciones" (también en "Ampliar" y en la bandeja de 159) funcionan, y el
formato calza con el contrato y con `ResultadoMlNube.kt` (`o: "O"` = solo los que **agregó** el operario; un cambio
de especie no cambia `o`, solo pone `ml.editado = true`). Lo que queda:

1. **Plan B (importante si el modelo no pasa el go/no-go):** en el conteo asistido todas las detecciones vienen con
   `o: "O"` y sin `p`. Hoy la tarjeta diría "Conteo automático" y "agregó 57 individuos a mano". Si **todas** son
   `o: "O"`, mejor titular "Conteo asistido" y no mostrar ese aviso.
2. **Opcional (del informe técnico de Lucas):** marcar la talla mínima legal en el histograma (50 mm chorito, 70 mm
   cholga) o mostrar "% bajo talla legal" por especie. Le sirve al supervisor; solo si sobra tiempo.
3. **Cosmético:** los dos porcentajes redondeados pueden sumar 99 % o 101 %. Se arregla calculando uno y restando
   el otro de 100.
4. Trae `feature/ml-conteo` y vuelve a correr `npm run cargar`: tus datos de prueba tenían el error de las
   detecciones (ya corregido) y las conchas ahora miden 45–75 mm, con la referencia arriba a la derecha.

## Lo que no te toca

`app/` es de Ramón · `firestore.rules` y la subida desde el teléfono son de Lucas.

## Checklist antes del PR

- [ ] Una muestra **sin** `ml` se ve igual que hoy.
- [ ] Una muestra **con** `ml` muestra la tarjeta, el gráfico y los recuadros.
- [ ] Probado en el computador y en el celular.
- [ ] Sin errores en la consola del navegador (F12).
- [ ] La conexión al emulador solo se activa en `localhost`.
