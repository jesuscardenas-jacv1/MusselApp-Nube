// Respaldo local de Firestore a archivos JSON. Gratis: usa el Admin SDK, no la exportación oficial (que exige Blaze).
//
// Uso (PowerShell), desde esta carpeta:
//   npm install
//   $env:CLAVE_ADMIN = "C:\Users\<tu usuario>\Documents\claves-musselapp\musselapp-demo-firebase-adminsdk-xxxx.json"
//   npm run respaldo
//
// La clave de la cuenta de servicio NUNCA va al repositorio (.gitignore la bloquea). El respaldo queda en
// ../../respaldo-AAAA-MM-DD/ (también ignorado por git). Solo LEE: no modifica ni borra nada en Firestore.
import { initializeApp, cert } from 'firebase-admin/app';
import { getFirestore, Timestamp } from 'firebase-admin/firestore';
import { readFileSync, mkdirSync, writeFileSync } from 'node:fs';
import { resolve } from 'node:path';

const COLECCIONES = ['muestras', 'fotos', 'supervisores'];

const rutaClave = process.env.CLAVE_ADMIN;
if (!rutaClave) {
  console.error('Falta la variable CLAVE_ADMIN con la ruta de la clave de la cuenta de servicio.');
  process.exit(1);
}
const clave = JSON.parse(readFileSync(rutaClave, 'utf8'));
initializeApp({ credential: cert(clave) });
const db = getFirestore();

// Las fechas de Firestore se guardan como texto ISO para que el JSON se pueda leer y restaurar
const aJson = (valor) => {
  if (valor instanceof Timestamp) return { __fecha: valor.toDate().toISOString() };
  if (Array.isArray(valor)) return valor.map(aJson);
  if (valor && typeof valor === 'object') return Object.fromEntries(Object.entries(valor).map(([k, v]) => [k, aJson(v)]));
  return valor;
};

const hoy = new Date().toISOString().slice(0, 10);
const carpeta = resolve(import.meta.dirname, '..', '..', `respaldo-${hoy}`);
mkdirSync(carpeta, { recursive: true });

let total = 0;
for (const nombre of COLECCIONES) {
  const snap = await db.collection(nombre).get();
  const docs = snap.docs.map((d) => ({ id: d.id, datos: aJson(d.data()) }));
  writeFileSync(resolve(carpeta, `${nombre}.json`), JSON.stringify(docs));
  console.log(`${nombre}: ${docs.length} documentos`);
  total += docs.length;
}
console.log(`Listo: ${total} documentos en ${carpeta} (proyecto ${clave.project_id})`);
