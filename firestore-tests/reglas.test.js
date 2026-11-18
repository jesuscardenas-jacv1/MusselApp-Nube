// Pruebas de ../firestore.rules. Se corren con `npm test`, que levanta el emulador de Firestore.
import { after, before, beforeEach, test } from 'node:test';
import { readFileSync } from 'node:fs';
import { assertFails, assertSucceeds, initializeTestEnvironment } from '@firebase/rules-unit-testing';
import { collection, deleteDoc, doc, getDoc, getDocs, query, setDoc, updateDoc, where } from 'firebase/firestore';

const TELEFONO_A = 'uid-telefono-a';
const TELEFONO_B = 'uid-telefono-b';
const SUPERVISOR = 'uid-supervisor';
const OTRA_CUENTA = 'uid-cuenta-sin-permiso';

let entorno;

// Muestra como la sube la app (solo los campos que importan para las reglas)
const muestra = (ownerUid, extra = {}) => ({
  codigo: 'Muestra 012', centroId: 'quellon', estado: 'PENDIENTE', conteoIndM: 410, tallaMm: 65,
  observacion: 'Muestreo preliminar', instalacion: 'aaaa1111', ownerUid, ...extra,
});

const bd = (uid) => (uid ? entorno.authenticatedContext(uid) : entorno.unauthenticatedContext()).firestore();

before(async () => {
  entorno = await initializeTestEnvironment({
    projectId: 'demo-musselapp',
    firestore: { rules: readFileSync(new URL('../firestore.rules', import.meta.url), 'utf8') },
  });
});

after(async () => { await entorno.cleanup(); });

beforeEach(async () => {
  await entorno.clearFirestore();
  // Datos de partida, escritos sin pasar por las reglas
  await entorno.withSecurityRulesDisabled(async (ctx) => {
    const admin = ctx.firestore();
    await setDoc(doc(admin, 'supervisores', SUPERVISOR), { nombre: 'Diego Muñoz' });
    await setDoc(doc(admin, 'muestras', 'aaaa1111_1'), muestra(TELEFONO_A));
    await setDoc(doc(admin, 'muestras', 'aaaa1111_2'), muestra(TELEFONO_A, { estado: 'VALIDADO', revisadaPor: 'Diego Muñoz', comentarioSupervisor: 'Conforme' }));
    await setDoc(doc(admin, 'muestras', 'bbbb2222_1'), muestra(TELEFONO_B, { instalacion: 'bbbb2222' }));
    // Subida antes de que existieran las sesiones: sin ownerUid ni instalacion
    await setDoc(doc(admin, 'muestras', 'aaaa1111_9'), { codigo: 'Muestra 009', estado: 'PENDIENTE', conteoIndM: 300 });
    await setDoc(doc(admin, 'fotos', 'aaaa1111_1'), { fotoBase64: 'data:image/jpeg;base64,AAAA', ownerUid: TELEFONO_A });
  });
});

// ── Sin sesión ───────────────────────────────────────────────────────────

test('sin sesión no se lee ni se escribe nada', async () => {
  const anonimo = bd(null);
  await assertFails(getDoc(doc(anonimo, 'muestras', 'aaaa1111_1')));
  await assertFails(getDocs(collection(anonimo, 'muestras')));
  await assertFails(setDoc(doc(anonimo, 'muestras', 'cccc3333_1'), muestra('quien-sea')));
  await assertFails(getDoc(doc(anonimo, 'fotos', 'aaaa1111_1')));
  await assertFails(getDoc(doc(anonimo, 'supervisores', SUPERVISOR)));
});

// ── Teléfono ─────────────────────────────────────────────────────────────

test('el teléfono crea muestras a su nombre, no a nombre de otro', async () => {
  const a = bd(TELEFONO_A);
  await assertSucceeds(setDoc(doc(a, 'muestras', 'aaaa1111_3'), muestra(TELEFONO_A)));
  await assertFails(setDoc(doc(a, 'muestras', 'aaaa1111_4'), muestra(TELEFONO_B)));
  await assertFails(setDoc(doc(a, 'muestras', 'aaaa1111_5'), { codigo: 'Sin dueño', estado: 'PENDIENTE' }));
});

test('el teléfono lee sus muestras y no las de otro teléfono', async () => {
  const a = bd(TELEFONO_A);
  await assertSucceeds(getDoc(doc(a, 'muestras', 'aaaa1111_1')));
  await assertFails(getDoc(doc(a, 'muestras', 'bbbb2222_1')));
});

test('la subida puede consultar por id una muestra que todavía no existe', async () => {
  await assertSucceeds(getDoc(doc(bd(TELEFONO_A), 'muestras', 'aaaa1111_77')));
});

test('la escucha del teléfono debe filtrar por su ownerUid', async () => {
  const a = bd(TELEFONO_A);
  const propias = await assertSucceeds(getDocs(query(collection(a, 'muestras'), where('ownerUid', '==', TELEFONO_A))));
  if (propias.size !== 2) throw new Error(`se esperaban 2 muestras propias y llegaron ${propias.size}`);
  await assertFails(getDocs(collection(a, 'muestras')));
  await assertFails(getDocs(query(collection(a, 'muestras'), where('ownerUid', '==', TELEFONO_B))));
});

test('el teléfono actualiza su muestra pendiente, incluido el dictamen hecho en la app', async () => {
  const a = bd(TELEFONO_A);
  await assertSucceeds(setDoc(doc(a, 'muestras', 'aaaa1111_1'), { observacion: 'Con clima', ownerUid: TELEFONO_A }, { merge: true }));
  await assertSucceeds(setDoc(doc(a, 'muestras', 'aaaa1111_1'),
    { estado: 'CORREGIDO', revisadaPor: 'Diego Muñoz', revisadaEn: 1000, comentarioSupervisor: 'Repetir conteo', ownerUid: TELEFONO_A },
    { merge: true }));
});

test('gana el primer dictamen: el teléfono no puede cambiar uno que ya está en la nube', async () => {
  const a = bd(TELEFONO_A);
  await assertFails(setDoc(doc(a, 'muestras', 'aaaa1111_2'), { estado: 'CORREGIDO', ownerUid: TELEFONO_A }, { merge: true }));
  await assertFails(setDoc(doc(a, 'muestras', 'aaaa1111_2'), { estado: 'PENDIENTE', ownerUid: TELEFONO_A }, { merge: true }));
  await assertFails(setDoc(doc(a, 'muestras', 'aaaa1111_2'), { conteoIndM: 999, ownerUid: TELEFONO_A }, { merge: true }));
  // Lo demás sí se puede seguir actualizando (posición, clima, alertas)
  await assertSucceeds(setDoc(doc(a, 'muestras', 'aaaa1111_2'), { observacion: 'Con clima', alertas: [], ownerUid: TELEFONO_A }, { merge: true }));
});

test('el teléfono no puede tocar ni quedarse con la muestra de otro teléfono', async () => {
  const a = bd(TELEFONO_A);
  await assertFails(setDoc(doc(a, 'muestras', 'bbbb2222_1'), { observacion: 'Ajena' }, { merge: true }));
  await assertFails(setDoc(doc(a, 'muestras', 'bbbb2222_1'), { ownerUid: TELEFONO_A }, { merge: true }));
  // Tampoco puede regalar la suya
  await assertFails(setDoc(doc(a, 'muestras', 'aaaa1111_1'), { ownerUid: TELEFONO_B }, { merge: true }));
});

test('el teléfono no puede eliminar muestras', async () => {
  await assertFails(deleteDoc(doc(bd(TELEFONO_A), 'muestras', 'aaaa1111_1')));
});

test('una muestra antigua sin dueño se puede reclamar', async () => {
  const a = bd(TELEFONO_A);
  await assertSucceeds(getDoc(doc(a, 'muestras', 'aaaa1111_9')));
  await assertSucceeds(setDoc(doc(a, 'muestras', 'aaaa1111_9'), { ownerUid: TELEFONO_A, instalacion: 'aaaa1111' }, { merge: true }));
  // Ya tiene dueño: otro teléfono no la ve ni se la puede quedar
  const b = bd(TELEFONO_B);
  await assertFails(getDoc(doc(b, 'muestras', 'aaaa1111_9')));
  await assertFails(setDoc(doc(b, 'muestras', 'aaaa1111_9'), { ownerUid: TELEFONO_B }, { merge: true }));
});

test('fotos: el teléfono sube y lee las suyas, no las de otro', async () => {
  const a = bd(TELEFONO_A);
  await assertSucceeds(setDoc(doc(a, 'fotos', 'aaaa1111_3'), { fotoBase64: 'data:image/jpeg;base64,BBBB', ownerUid: TELEFONO_A }));
  await assertSucceeds(setDoc(doc(a, 'fotos', 'aaaa1111_1'), { fotoBase64: 'data:image/jpeg;base64,CCCC', ownerUid: TELEFONO_A }));
  await assertSucceeds(getDoc(doc(a, 'fotos', 'aaaa1111_1')));
  const b = bd(TELEFONO_B);
  await assertFails(getDoc(doc(b, 'fotos', 'aaaa1111_1')));
  await assertFails(setDoc(doc(b, 'fotos', 'aaaa1111_1'), { fotoBase64: 'x', ownerUid: TELEFONO_B }));
  await assertFails(deleteDoc(doc(a, 'fotos', 'aaaa1111_1')));
});

// ── Supervisor (portal) ──────────────────────────────────────────────────

test('el supervisor lee todas las muestras y fotos', async () => {
  const s = bd(SUPERVISOR);
  const todas = await assertSucceeds(getDocs(collection(s, 'muestras')));
  if (todas.size !== 4) throw new Error(`se esperaban 4 muestras y llegaron ${todas.size}`);
  await assertSucceeds(getDoc(doc(s, 'fotos', 'aaaa1111_1')));
});

test('el supervisor dictamina, corrige valores y elimina', async () => {
  const s = bd(SUPERVISOR);
  await assertSucceeds(updateDoc(doc(s, 'muestras', 'aaaa1111_1'), {
    estado: 'CORREGIDO', revisadaPor: 'Diego Muñoz', revisadaEn: 2000, comentarioSupervisor: 'Repetir conteo',
    conteoIndM: 380, valoresOperario: { conteoIndM: 410, tallaMm: 65, cholgasIndM: null },
  }));
  await assertSucceeds(deleteDoc(doc(s, 'muestras', 'bbbb2222_1')));
  await assertSucceeds(deleteDoc(doc(s, 'fotos', 'aaaa1111_1')));
  // Datos de demostración creados desde el portal
  await assertSucceeds(setDoc(doc(s, 'muestras', 'demo-1'), { codigo: 'Demo', estado: 'PENDIENTE', demo: true }));
});

test('una cuenta con sesión que no está en supervisores no tiene permisos de supervisor', async () => {
  const otra = bd(OTRA_CUENTA);
  await assertFails(getDocs(collection(otra, 'muestras')));
  await assertFails(updateDoc(doc(otra, 'muestras', 'aaaa1111_1'), { estado: 'VALIDADO' }));
  await assertFails(deleteDoc(doc(otra, 'muestras', 'aaaa1111_1')));
});

// ── Lista de supervisores ────────────────────────────────────────────────

test('cada cuenta solo puede comprobar si ella es supervisor, y nadie edita la lista', async () => {
  await assertSucceeds(getDoc(doc(bd(SUPERVISOR), 'supervisores', SUPERVISOR)));
  await assertSucceeds(getDoc(doc(bd(OTRA_CUENTA), 'supervisores', OTRA_CUENTA)));
  await assertFails(getDoc(doc(bd(OTRA_CUENTA), 'supervisores', SUPERVISOR)));
  await assertFails(getDocs(collection(bd(SUPERVISOR), 'supervisores')));
  await assertFails(setDoc(doc(bd(OTRA_CUENTA), 'supervisores', OTRA_CUENTA), { nombre: 'Intruso' }));
  await assertFails(setDoc(doc(bd(SUPERVISOR), 'supervisores', OTRA_CUENTA), { nombre: 'Amigo' }));
  await assertFails(deleteDoc(doc(bd(SUPERVISOR), 'supervisores', SUPERVISOR)));
});
