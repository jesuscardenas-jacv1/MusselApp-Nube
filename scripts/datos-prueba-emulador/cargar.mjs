// Carga muestras de prueba con conteo automático (ml + detecciones) en el EMULADOR de Firebase.
// Nunca toca la base real: fija las variables del emulador antes de cargar firebase-admin y se
// detiene si el emulador no responde. Uso: ver LEEME.md.

const HOST_FIRESTORE = '127.0.0.1:8080';
const HOST_AUTH = '127.0.0.1:9099';
// Mismo projectId que firebaseConfig en web-supervisor/index.html: el emulador separa los datos por proyecto
const PROYECTO = 'musselapp-demo';

process.env.FIRESTORE_EMULATOR_HOST = HOST_FIRESTORE;
process.env.FIREBASE_AUTH_EMULATOR_HOST = HOST_AUTH;
process.env.GOOGLE_CLOUD_PROJECT = PROYECTO;
process.env.METADATA_SERVER_DETECTION = 'none';   // no buscar credenciales de Google Cloud: no se necesitan

async function emuladorResponde(host) {
  try { await fetch(`http://${host}/`); return true; } catch { return false; }
}
if (!(await emuladorResponde(HOST_FIRESTORE)) || !(await emuladorResponde(HOST_AUTH))) {
  console.error(`El emulador no responde en ${HOST_FIRESTORE} (Firestore) y ${HOST_AUTH} (Auth).`);
  console.error(`Inícialo primero: npx firebase-tools emulators:start --only firestore,auth,hosting --project ${PROYECTO}`);
  process.exit(1);
}

const { initializeApp } = await import('firebase-admin/app');
const { getFirestore } = await import('firebase-admin/firestore');
const { getAuth } = await import('firebase-admin/auth');
initializeApp({ projectId: PROYECTO });
const db = getFirestore();
const auth = getAuth();

// ── Cuentas de prueba (solo existen en el emulador) ──
// El portal entra con RUT y PIN: el RUT pasa a correo ficticio y el PIN a "pin-" + PIN.
const SUPERVISOR = { rut: '22.222.222-2', correo: '22222222-2@musselapp.cl', pin: '1234', nombre: 'Supervisor de prueba' };
const OPERARIO = { correo: 'operario-prueba@musselapp.cl', nombre: 'Operario de prueba', rut: '11.111.111-1' };

async function cuenta(correo, clave) {
  try { return (await auth.getUserByEmail(correo)).uid; } catch { /* no existe todavía */ }
  return (await auth.createUser({ email: correo, password: clave })).uid;
}
const uidSupervisor = await cuenta(SUPERVISOR.correo, 'pin-' + SUPERVISOR.pin);
const uidOperario = await cuenta(OPERARIO.correo, 'pin-0000');
await db.collection('supervisores').doc(uidSupervisor).set({ nombre: SUPERVISOR.nombre });

// ── Generador con semilla: siempre produce los mismos datos ──
function azar(semilla) {
  let s = semilla >>> 0;
  return () => { s = (s * 1664525 + 1013904223) >>> 0; return s / 4294967296; };
}

const ANCHO = 1600, ALTO = 1200;          // tamaño de la "foto"
const LADO_REF_PX = 160;                  // la referencia de 40 mm mide 160 px → 0,25 mm por píxel
const MM_POR_PX = 40 / LADO_REF_PX;

/**
 * Dibuja una bandeja (SVG) y devuelve sus detecciones con el formato del contrato.
 * Los recuadros calzan con las conchas dibujadas, para probar "Ver detecciones".
 */
function bandeja({ semilla, choritos, cholgas, conReferencia = true, agregadas = 0, eliminadas = 0 }) {
  const r = azar(semilla);
  const especies = [...Array(choritos).fill('C'), ...Array(cholgas).fill('K')].sort(() => r() - 0.5);
  const cols = Math.ceil(Math.sqrt(especies.length * ANCHO / ALTO));
  const filas = Math.ceil(especies.length / cols);
  const celdaX = (ANCHO - 120) / cols, celdaY = (ALTO - 120) / filas;
  const conchas = [];
  const dets = [];
  especies.forEach((e, i) => {
    const largoMm = e === 'C' ? 38 + r() * 32 : 55 + r() * 35;
    // Achica si la celda es chica, para que no se encimen
    const largo = Math.min(largoMm / MM_POR_PX, Math.min(celdaX, celdaY) * 1.1);
    const ancho = largo * (e === 'C' ? 0.45 : 0.55);
    const cx = 60 + celdaX * (i % cols + 0.5) + (r() - 0.5) * celdaX * 0.2;
    const cy = 60 + celdaY * (Math.floor(i / cols) + 0.5) + (r() - 0.5) * celdaY * 0.2;
    // La referencia va en la esquina superior izquierda: deja libre ese espacio
    if (conReferencia && cx < 60 + LADO_REF_PX + 40 && cy < 60 + LADO_REF_PX + 40) return;
    const ang = r() * Math.PI;
    const ex = Math.sqrt((largo / 2 * Math.cos(ang)) ** 2 + (ancho / 2 * Math.sin(ang)) ** 2);
    const ey = Math.sqrt((largo / 2 * Math.sin(ang)) ** 2 + (ancho / 2 * Math.cos(ang)) ** 2);
    const grados = ang * 180 / Math.PI;
    let forma = `<ellipse cx="${cx.toFixed(1)}" cy="${cy.toFixed(1)}" rx="${(largo / 2).toFixed(1)}" ry="${(ancho / 2).toFixed(1)}" ` +
      `fill="${e === 'C' ? '#1f2a44' : '#3b2236'}" transform="rotate(${grados.toFixed(1)} ${cx.toFixed(1)} ${cy.toFixed(1)})"/>`;
    if (e === 'K') {   // la cholga lleva costillas
      for (let k = -2; k <= 2; k++) {
        forma += `<line x1="${(cx - largo / 2 * 0.9).toFixed(1)}" y1="${cy.toFixed(1)}" x2="${(cx + largo / 2 * 0.85).toFixed(1)}" ` +
          `y2="${(cy + k * ancho * 0.16).toFixed(1)}" stroke="#7a5a72" stroke-width="2" transform="rotate(${grados.toFixed(1)} ${cx.toFixed(1)} ${cy.toFixed(1)})"/>`;
      }
    }
    conchas.push(forma);
    const det = {
      e, p: Math.round((0.55 + r() * 0.44) * 100) / 100,
      x: Math.round((cx - ex) / ANCHO * 1000), y: Math.round((cy - ey) / ALTO * 1000),
      w: Math.round(2 * ex / ANCHO * 1000), h: Math.round(2 * ey / ALTO * 1000),
      o: 'M',
    };
    if (conReferencia) det.mm = Math.round(largo * MM_POR_PX);
    dets.push(det);
  });
  // El operario quitó algunas (no se suben) y agregó otras que el modelo no vio (sin "p")
  const quitadas = dets.splice(0, eliminadas).length;
  for (let k = 0; k < agregadas && k < dets.length; k++) {
    const d = dets[dets.length - 1 - k];
    delete d.p; d.o = 'O';
  }
  const ref = conReferencia
    ? `<rect x="60" y="60" width="${LADO_REF_PX}" height="${LADO_REF_PX}" fill="#000"/>` +
      `<text x="60" y="${60 + LADO_REF_PX + 28}" font-size="22" font-family="sans-serif" fill="#333">40 mm</text>`
    : '';
  const svg = `<svg xmlns="http://www.w3.org/2000/svg" width="${ANCHO}" height="${ALTO}" viewBox="0 0 ${ANCHO} ${ALTO}">` +
    `<rect width="100%" height="100%" fill="#f4f1ea"/>${ref}${conchas.join('')}</svg>`;
  return { dets, quitadas, foto: 'data:image/svg+xml;base64,' + Buffer.from(svg).toString('base64') };
}

function promedio(lista) {
  return lista.length ? Math.round(lista.reduce((a, b) => a + b, 0) / lista.length * 10) / 10 : null;
}

/** `ml` con el mismo formato y redondeos que ResultadoMlNube.aCampos en la app. */
function resumenMl(dets, { version = 'model_v1', conReferencia = true, editado = false }) {
  const ch = dets.filter(d => d.e === 'C'), ko = dets.filter(d => d.e === 'K');
  const ml = { version, total: dets.length, choritos: ch.length, cholgas: ko.length, editado };
  if (conReferencia) {
    ml.mmPorPx = Math.round(MM_POR_PX * 10000) / 10000;
    const tc = promedio(ch.map(d => d.mm).filter(Boolean)), tk = promedio(ko.map(d => d.mm).filter(Boolean));
    if (tc != null) ml.tallaChoritoMm = tc;
    if (tk != null) ml.tallaCholgaMm = tk;
  }
  return ml;
}

// ── Muestras ──
const HOY = Date.now(), DIA = 86400000;
const CASOS = [
  { id: 'prueba-01', centro: 'dalcahue', estado: 'PENDIENTE', dias: 0, choritos: 52, cholgas: 6, obs: 'Conteo automático sin correcciones' },
  { id: 'prueba-02', centro: 'calbuco', estado: 'PENDIENTE', dias: 1, choritos: 34, cholgas: 14, agregadas: 4, eliminadas: 2, obs: 'El operario corrigió el conteo' },
  { id: 'prueba-03', centro: 'quellon', estado: 'VALIDADO', dias: 2, choritos: 25, cholgas: 25, obs: 'Mitad y mitad, ya validada' },
  { id: 'prueba-04', centro: 'melinka', estado: 'PENDIENTE', dias: 3, choritos: 30, cholgas: 3, sinReferencia: true, obs: 'Foto sin la referencia: sin calibre' },
  { id: 'prueba-05', centro: 'dalcahue', estado: 'CORREGIDO', dias: 4, choritos: 140, cholgas: 20, agregadas: 6, obs: 'Bandeja grande (160 individuos)' },
  { id: 'prueba-06', centro: 'calbuco', estado: 'PENDIENTE', dias: 5, sinMl: true, obs: 'Muestra sin conteo automático (debe verse igual que hoy)' },
];

const TRENES = ['A', 'B', 'C'];
let n = 0;
for (const c of CASOS) {
  n++;
  const tramoCm = 100;
  const base = {
    codigo: `MST-P${String(n).padStart(3, '0')}`, centroId: c.centro, tren: TRENES[n % 3], linea: n, profundidadM: 3 + n,
    tramoCm, observacion: c.obs, estado: c.estado, registradaEn: HOY - c.dias * DIA,
    operador: OPERARIO.nombre, operadorRut: OPERARIO.rut, ownerUid: uidOperario, tieneFoto: true,
  };
  if (c.estado !== 'PENDIENTE') {
    Object.assign(base, { revisadaPor: SUPERVISOR.nombre, revisadaEn: base.registradaEn + 3600000, comentarioSupervisor: 'Revisada en el emulador' });
  }
  let foto;
  if (c.sinMl) {
    Object.assign(base, { conteoIndM: 45, cholgasIndM: 5, tallaMm: 55 });
    foto = bandeja({ semilla: n * 97, choritos: 45, cholgas: 5 }).foto;
  } else {
    const b = bandeja({ semilla: n * 97, choritos: c.choritos, cholgas: c.cholgas, conReferencia: !c.sinReferencia,
      agregadas: c.agregadas || 0, eliminadas: c.eliminadas || 0 });
    const editado = (c.agregadas || 0) + b.quitadas > 0;
    const ml = resumenMl(b.dets, { conReferencia: !c.sinReferencia, editado });
    foto = b.foto;
    // Los campos de siempre salen del conteo, por metro (tramo de 100 cm)
    Object.assign(base, {
      conteoIndM: Math.round(ml.choritos * 100 / tramoCm), cholgasIndM: Math.round(ml.cholgas * 100 / tramoCm),
      tallaMm: Math.round(ml.tallaChoritoMm || 52), ml,   // sin referencia, la talla la midió el operario detecciones: b.dets,
    });
  }
  await db.collection('muestras').doc(c.id).set(base);
  await db.collection('fotos').doc(c.id).set({ fotoBase64: foto, ownerUid: uidOperario });
  const resumen = base.ml ? `${base.ml.total} detecciones (${base.ml.choritos} choritos, ${base.ml.cholgas} cholgas)` : 'sin ml';
  console.log(`${c.id} · ${c.estado.padEnd(9)} · ${resumen}`);
}

console.log(`\nListo: ${CASOS.length} muestras en el emulador (proyecto ${PROYECTO}).`);
console.log(`Entra al portal con RUT ${SUPERVISOR.rut} y PIN ${SUPERVISOR.pin} (cuenta que solo existe en el emulador).`);
process.exit(0);
