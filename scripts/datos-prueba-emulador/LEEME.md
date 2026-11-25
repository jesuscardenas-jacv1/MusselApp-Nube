# Datos de prueba para el emulador

Carga en el **emulador** de Firebase 6 muestras con conteo automático (`ml` + `detecciones`), con sus fotos y una
cuenta de supervisor. Sirve para trabajar el portal (paso 11) sin la app ni el modelo. **Nunca toca la base real:**
fija las variables del emulador antes de conectarse y se detiene si el emulador no está corriendo.

## Uso

Necesitas Node y Java 21 o superior. Desde la raíz del repositorio, en una terminal:

```
npx firebase-tools emulators:start --only firestore,auth,hosting --project musselapp-demo
```

En otra terminal:

```
cd scripts/datos-prueba-emulador
npm install
npm run cargar
```

Se puede repetir las veces que quieras: vuelve a dejar las mismas 6 muestras. El emulador borra todo al cerrarse.

Para que el portal use el emulador, en `iniciarFirebase()` de `web-supervisor/index.html`, justo después de
`db = firebase.firestore();`, va:

```js
if (location.hostname === 'localhost' || location.hostname === '127.0.0.1') {
  db.useEmulator('127.0.0.1', 8080);
  firebase.auth().useEmulator('http://127.0.0.1:9099');
}
```

Abre `http://127.0.0.1:5000` y entra con **RUT `22.222.222-2` y PIN `1234`**. Esa cuenta existe solo en el emulador.

> Se usa `--project musselapp-demo` porque es el `projectId` del portal: el emulador guarda los datos por proyecto y,
> con otro nombre, el portal no los vería. Igual es todo local: con `--only` no se conecta a la base real.

## Qué muestras trae

| Id | Estado | Caso |
|---|---|---|
| `prueba-01` | Pendiente | Conteo automático sin correcciones (57 detecciones) |
| `prueba-02` | Pendiente | El operario corrigió: quitó 2 y agregó 4 (`o: "O"`, sin `p`), `ml.editado = true` |
| `prueba-03` | Validado | Mitad choritos y mitad cholgas |
| `prueba-04` | Pendiente | Foto **sin referencia**: sin `mm`, sin `mmPorPx` ni tallas en `ml` |
| `prueba-05` | Corregido | Bandeja grande (156 detecciones) |
| `prueba-06` | Pendiente | **Sin `ml`**: debe verse igual que hoy |

Las fotos son dibujos (SVG de 1600 × 1200) y los recuadros de `detecciones` calzan con las conchas dibujadas, así
que sirven para probar "Ver detecciones". La referencia de 40 mm mide 160 px (0,25 mm por píxel).

Ojo: la foto del detalle usa `object-fit: cover`. Si la foto tiene otra proporción que su recuadro, hay que
calcular el área que realmente se ve antes de dibujar las detecciones (o dibujarlas en la vista "Ampliar").
