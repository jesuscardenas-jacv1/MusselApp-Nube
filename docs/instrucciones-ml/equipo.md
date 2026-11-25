# Fase final · Instrucciones para todo el equipo

MusselApp · conteo automático con ML · Para Jesús, Ramón y Lucas · Entrega: **viernes 4 de diciembre de 2026**

> **Qué vamos a construir:** la app fotografía la muestra y un modelo de ML **cuenta los individuos y dice si son chorito o cholga**. Luego mide su calibre en mm, el operario corrige lo que esté mal y la muestra sigue el flujo de siempre (Pendiente → Validado / Corregido). Todo gratis y sin romper la versión presentada (`v1.3.2`).

Plan completo: [`docs/plan-fase-final.md`](../plan-fase-final.md) · Reglas para los asistentes de IA: [`CLAUDE.md`](../../CLAUDE.md)

**Estado actual y bitácora:** [`docs/estado-ml.md`](../estado-ml.md). Léelo antes de empezar y agrega una entrada cuando termines algo.

## 1. Quién hace qué

| Integrante | Su parte | Instrucciones |
|---|---|---|
| **Jesús Cárdenas** · Front-end | Portal web: conteo automático, calibres y recuadros sobre la foto. Más etiquetado. | [`jesus.md`](jesus.md) |
| **Ramón Osorio** · UI/UX & Mobile | App: pantalla de revisión, Room e integración del modelo. | [`ramon.md`](ramon.md) |
| **Lucas Saldivia** · QA & Data | Dataset, entrenamiento, calibre, reglas de seguridad y métricas. | [`lucas.md`](lucas.md) |
| **Los 3** | Fotos, etiquetado y prueba final. | Este documento |

## 2. Calendario

| Fechas | Qué pasa |
|---|---|
| Mié 25 – Vie 27 nov | **Fotos (todos, listas el viernes 27)** · Ramón: modelo de mentira y base de datos · Jesús: portal con datos de prueba · Lucas: respaldo, pre-entrenamiento y notebook |
| Sáb 28 – Lun 30 nov | **Etiquetado (todos, listo el lunes 30)** · Ramón: pantalla de revisión · Jesús: tarjeta y gráfico · Lucas: calibre y reglas |
| Mar 1 – Mié 2 dic | Lucas entrena y decide "seguimos o plan B" (mié 2) · Ramón: modelo real · Jesús: recuadros sobre la foto |
| Jue 3 dic | Prueba final de punta a punta, unir a `main`, versión `v2.0.0` (los 3) |
| Vie 4 dic | **Presentación** |

## 3. Tarea común A · Sacar las fotos

Meta: **150 fotos entre los 3** (~50 cada uno), con al menos 500 choritos y 500 cholgas en total. El modelo solo aprende de nuestras fotos.

**Materiales:**
- Choritos y cholgas reales, con la concha completa.
- Cartulina blanca mate de tamaño A3 o más.
- **Cuadrado de referencia de 40 mm** impreso al 100 %; la línea de control debe medir 100 mm.
- Celular, con la cámara normal.

**Cómo tomar cada foto:**
1. Luz pareja: ventana sin sol directo o luz de techo. **Sin flash.**
2. Entre **20 y 80 individuos sueltos**, mezclando especies y tamaños. En 1 de cada 5 fotos pueden tocarse un poco.
3. La referencia, plana, en una esquina.
4. Celular **paralelo a la mesa**, justo encima, a 30–40 cm.
5. Sin zoom, filtros ni modo retrato. Enfoca tocando la pantalla y revisa que no salga borrosa.
6. 2 o 3 fotos por bandeja; después se cambian los individuos.

**Variedad:** secas y mojadas; luz de día y artificial; bandejas solo de choritos, solo de cholgas y mezcladas; si se puede, alguna sobre superficie metálica o gris.

**Nombres y rangos:**
- Nombre del archivo: `M<bandeja>_<iniciales>_<n>.jpg`, por ejemplo `M012_JC_02.jpg`.
- Bandejas: **Jesús M001–M099**, **Ramón M100–M199**, **Lucas M200–M299**.

**Dónde:** Drive `MusselApp-ML / dataset_v1 / crudas /`. Cada bandeja se anota en la hoja `planilla_fotos`, con el **conteo a mano** de choritos y cholgas.

**Set de calibre** (10 bandejas en total, ~3 cada uno). Sirve para medir si el calibre de la app se equivoca menos de 3 mm:
1. Elige 5 individuos de la bandeja, mide el largo de cada uno con regla o pie de metro (de punta a punta de la concha) y anótalo en la planilla como `largo_1_mm` … `largo_5_mm`, con su especie, y `set_calibre = si`.
2. Saca una **foto extra solo con esos 5**, puestos **en una fila de izquierda a derecha en el mismo orden 1 a 5** de la planilla, separados entre sí, y la referencia en la esquina. Mismas reglas de siempre (cenital, sin flash).
3. Nómbrala terminada en `_cal`: por ejemplo `M002_JC_cal.jpg`, y etiquétala como las demás.

Sin esa fila no se puede saber qué recuadro corresponde a cada medida.

| Chorito (*Mytilus chilensis*) | Cholga (*Aulacomya atra*) |
|---|---|
| Concha **lisa**, con líneas de crecimiento en arco. Negro azulado o café. Más delgada. | **Costillas o estrías** que salen desde la punta. Negro o morado opaco. Más gruesa y suele ser más grande. |

Si no estás seguro de la especie, usa la etiqueta `duda`.

## 4. Tarea común B · Etiquetar (Label Studio)

Cada uno etiqueta sus fotos; Jesús, además, unas 20 de Lucas.

```
pip install label-studio
label-studio start
```

Se abre `http://localhost:8080`.
1. **Create Project** → `MusselApp`.
2. **Data Import**: sube tus fotos.
3. **Labeling Setup** → **Custom template** y pega esto:

```xml
<View>
  <Image name="image" value="$image" zoom="true"/>
  <RectangleLabels name="label" toName="image">
    <Label value="chorito" background="#0F766E"/>
    <Label value="cholga" background="#C76C05"/>
    <Label value="referencia" background="#1E40AF"/>
    <Label value="duda" background="#9CA3AF"/>
  </RectangleLabels>
</View>
```

**Reglas:**
- El recuadro va justo al borde de la concha.
- Un recuadro por individuo, aunque se crucen.
- Si sale cortado en el borde de la foto, se etiqueta solo si se ve más de la mitad.
- La referencia: el recuadro va solo sobre el cuadrado negro.
- No se salta ninguno.
- Atajos: 1-4 para elegir la etiqueta y Ctrl+Enter para enviar.

**Entrega:** Export en formato **COCO** → Drive `dataset_v1/etiquetas/<iniciales>_lote<n>.zip`. Revisión cruzada: cada uno revisa 5 fotos de otro.

## 5. Reglas de Git y Firebase

**No se hace:**
- Trabajar ni hacer push directo a `main`.
- Subir fotos, datasets ni archivos `.tflite` al repo de la app.
- Subir claves (`*firebase-adminsdk*.json`, `keystore.properties`, `.jks`).
- Probar contra la base real de Firestore: se usa el **emulador**.
- Publicar el portal o las reglas en Firebase sin avisar al equipo.
- Poner a la IA como coautora en los commits.

| Rama | Para qué | Quién |
|---|---|---|
| `feature/ml-conteo` | Rama base de la fase (sale de `main`). Los PR van aquí. | Lucas (ya creada) |
| `feature/ml-app` | App | Ramón |
| `feature/ml-portal` | Portal | Jesús |
| `feature/ml-reglas` | Reglas y sincronización | Lucas |

```
git fetch origin
git switch -c feature/ml-app origin/feature/ml-conteo   # o feature/ml-portal
```

Dos veces por semana, trae lo nuevo con `git merge origin/feature/ml-conteo`. Los PR van hacia `feature/ml-conteo`, con el CI en verde y la revisión de otro integrante.

## 6. El contrato (formato común)

- Etiquetas del modelo: `chorito`, `cholga`, `referencia`. La referencia mide **40 mm**.
- Lo que se guarda en cada documento de `muestras`:

```js
ml: { version: "model_v1", total: 64, choritos: 58, cholgas: 6, mmPorPx: 0.21,
      tallaChoritoMm: 54.2, tallaCholgaMm: 71.0, editado: true },
detecciones: [ { e: "C", p: 0.93, x: 120, y: 340, w: 60, h: 28, mm: 55, o: "M" } ]
// e: "C" chorito, "K" cholga · p: confianza 0-1 · x,y,w,h en escala 0-1000 · mm: largo · o: "M" modelo, "O" operario
```

Hasta que el modelo real esté listo, la app usa un detector simulado con este mismo formato.

## 7. Enlaces

- App: https://github.com/jesuscardenas-jacv1/MusselApp-Nube (rama `feature/ml-conteo`)
- Entrenamiento: https://github.com/lucsaldivia-ux/MusselApp-ML (acepten la invitación de GitHub)
- Drive: https://drive.google.com/drive/folders/1DoT9A1pm4GLifuJSojlALrPr8YMIh9eL

## 8. Si algo sale mal

La release `v1.3.2` no se toca. Si el modelo no alcanza la calidad mínima, entregamos el **plan B**: conteo asistido (el operario marca y elige la especie), que usa todo lo que construyen Ramón y Jesús.
