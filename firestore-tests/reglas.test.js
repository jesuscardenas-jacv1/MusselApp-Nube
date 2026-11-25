// Pruebas de ../firestore.rules. Se corren con `npm test`, que levanta el emulador de Firestore.
import { after, before, beforeEach, test } from 'node:test';
import { readFileSync } from 'node:fs';
import { assertFails, assertSucceeds, initializeTestEnvironment } from '@firebase/rules-unit-testing';
import { collection, deleteDoc, deleteField, doc, getDoc, getDocs, query, setDoc, updateDoc, where } from 'firebase/firestore';

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

// ── Validación de datos (lo que escribe el teléfono) ─────────────────────

// Subida completa, con los mismos campos y tipos que manda NubeSync.subir
const subidaCompleta = (ownerUid, extra = {}) => ({
  codigo: 'Muestra 020', centroId: 'quellon', tren: 'B', linea: 4, profundidadM: 6, tallaMm: 72,
  conteoIndM: 450, cholgasIndM: null, tramoCm: 25, observacion: '', estado: 'PENDIENTE',
  registradaEn: 1732400000000, operador: 'Carla Vera', operadorRut: '11.111.111-1',
  lat: -42.401, lon: -73.622, precisionM: 12.5, distanciaCentroM: 140,
  clima: { tempAireC: 12.1, vientoKn: 22.0, rachaKn: 30.0, vientoDir: 200, codigo: 63, descripcion: 'Lluvia',
           olaM: 1.2, tempAguaC: 13.3, mareaM: 0.4, obtenidoEn: 1732400000000 },
  alertas: ['VIENTO_FUERTE'], ownerUid, instalacion: 'aaaa1111', ...extra,
});

test('validación: pasan las subidas reales de la app (pendiente, sin datos opcionales, con foto y validada sin conexión)', async () => {
  const a = bd(TELEFONO_A);
  await assertSucceeds(setDoc(doc(a, 'muestras', 'aaaa1111_20'), subidaCompleta(TELEFONO_A), { merge: true }));
  // Sin GPS, sin clima y sin tramo: la app sube null
  await assertSucceeds(setDoc(doc(a, 'muestras', 'aaaa1111_21'),
    subidaCompleta(TELEFONO_A, { codigo: 'Muestra 021', lat: null, lon: null, precisionM: null, distanciaCentroM: null,
      clima: null, tallaMm: null, tramoCm: null, alertas: [] }), { merge: true }));
  // Resubida con foto: marca tieneFoto y borra la foto antigua dentro de la muestra
  await assertSucceeds(setDoc(doc(a, 'muestras', 'aaaa1111_20'),
    subidaCompleta(TELEFONO_A, { tieneFoto: true, fotoBase64: deleteField() }), { merge: true }));
  // Validada en el teléfono antes de tener conexión
  await assertSucceeds(setDoc(doc(a, 'muestras', 'aaaa1111_22'),
    subidaCompleta(TELEFONO_A, { codigo: 'Muestra 022', estado: 'VALIDADO', revisadaPor: 'Diego Muñoz', revisadaEn: 1732400100000,
      comentarioSupervisor: null, valoresOperario: { conteoIndM: 410, tallaMm: 65, cholgasIndM: null } }), { merge: true }));
});

test('validación: una muestra antigua con campos que ya no se usan se sigue pudiendo actualizar', async () => {
  await entorno.withSecurityRulesDisabled(async (ctx) => {
    await setDoc(doc(ctx.firestore(), 'muestras', 'aaaa1111_30'), muestra(TELEFONO_A, { campoAntiguo: 'x', fotoBase64: 'data:...' }));
  });
  const a = bd(TELEFONO_A);
  await assertSucceeds(setDoc(doc(a, 'muestras', 'aaaa1111_30'),
    { observacion: 'Con clima', tieneFoto: true, fotoBase64: deleteField(), ownerUid: TELEFONO_A }, { merge: true }));
});

test('ataque: el teléfono no puede agregar campos que la app no usa', async () => {
  const a = bd(TELEFONO_A);
  await assertFails(setDoc(doc(a, 'muestras', 'aaaa1111_40'), subidaCompleta(TELEFONO_A, { esAdmin: true })));
  await assertFails(setDoc(doc(a, 'muestras', 'aaaa1111_1'), { rol: 'SUPERVISOR', ownerUid: TELEFONO_A }, { merge: true }));
  await assertFails(setDoc(doc(a, 'muestras', 'aaaa1111_1'), { demo: true, ownerUid: TELEFONO_A }, { merge: true }));
});

test('ataque: el teléfono no puede guardar valores absurdos ni de otro tipo', async () => {
  const a = bd(TELEFONO_A);
  const malos = [
    { conteoIndM: -5 }, { conteoIndM: 9999999 }, { conteoIndM: '410' }, { tallaMm: 65.5 }, { profundidadM: 500 },
    { estado: 'APROBADO_POR_MI' }, { observacion: 'x'.repeat(5000) }, { codigo: 'y'.repeat(500) },
    { lat: 'aquí' }, { registradaEn: 'ayer' }, { alertas: 'todas' }, { alertas: Array(50).fill('A') },
    { clima: 'soleado' }, { tieneFoto: 'sí' },
  ];
  for (const [i, malo] of malos.entries()) {
    await assertFails(setDoc(doc(a, 'muestras', `aaaa1111_5${i}`), subidaCompleta(TELEFONO_A, malo)));
  }
  await assertFails(setDoc(doc(a, 'muestras', 'aaaa1111_1'), { conteoIndM: -1, ownerUid: TELEFONO_A }, { merge: true }));
});

test('ataque: el teléfono no puede cambiar la fecha de registro ni el código de una muestra subida', async () => {
  const a = bd(TELEFONO_A);
  await assertSucceeds(setDoc(doc(a, 'muestras', 'aaaa1111_20'), subidaCompleta(TELEFONO_A)));
  await assertFails(setDoc(doc(a, 'muestras', 'aaaa1111_20'), { registradaEn: 1, ownerUid: TELEFONO_A }, { merge: true }));
  await assertFails(setDoc(doc(a, 'muestras', 'aaaa1111_20'), { codigo: 'Muestra 999', ownerUid: TELEFONO_A }, { merge: true }));
});

test('ataque: en fotos solo van la foto y el dueño, con tamaño limitado', async () => {
  const a = bd(TELEFONO_A);
  await assertFails(setDoc(doc(a, 'fotos', 'aaaa1111_60'), { fotoBase64: 'data:image/jpeg;base64,BBBB', ownerUid: TELEFONO_A, extra: 1 }));
  await assertFails(setDoc(doc(a, 'fotos', 'aaaa1111_61'), { fotoBase64: 123, ownerUid: TELEFONO_A }));
  await assertFails(setDoc(doc(a, 'fotos', 'aaaa1111_62'), { fotoBase64: 'z'.repeat(1000001), ownerUid: TELEFONO_A }));
  await assertFails(setDoc(doc(a, 'fotos', 'aaaa1111_1'), { nota: 'hola', ownerUid: TELEFONO_A }, { merge: true }));
});

test('el supervisor no queda limitado por la validación del teléfono', async () => {
  const s = bd(SUPERVISOR);
  await assertSucceeds(setDoc(doc(s, 'muestras', 'demo-2'), { codigo: 'Demo', estado: 'PENDIENTE', demo: true, conteoIndM: 300 }));
  await assertSucceeds(updateDoc(doc(s, 'muestras', 'aaaa1111_2'), { estado: 'CORREGIDO', conteoIndM: 380 }));
});

// ── Conteo automático (fase final): campos ml y detecciones ──────────────

const resultadoMl = {
  ml: { version: 'model_v1', total: 3, choritos: 2, cholgas: 1, mmPorPx: 0.21,
        tallaChoritoMm: 54.2, tallaCholgaMm: 71.0, editado: true },
  detecciones: [
    { e: 'C', p: 0.93, x: 120, y: 340, w: 60, h: 28, mm: 55, o: 'M' },
    { e: 'C', p: 0.88, x: 300, y: 410, w: 58, h: 27, mm: 53, o: 'M' },
    { e: 'K', p: 0.81, x: 610, y: 205, w: 75, h: 33, mm: 71, o: 'O' },
  ],
};

test('conteo ML: el teléfono sube el resultado del modelo con la muestra', async () => {
  const a = bd(TELEFONO_A);
  await assertSucceeds(setDoc(doc(a, 'muestras', 'aaaa1111_70'), subidaCompleta(TELEFONO_A, resultadoMl), { merge: true }));
  // Sin conteo automático (flag apagado) sigue funcionando igual
  await assertSucceeds(setDoc(doc(a, 'muestras', 'aaaa1111_71'), subidaCompleta(TELEFONO_A, { codigo: 'Muestra 071' }), { merge: true }));
  // Validada sin conexión, con resultado del modelo, en la misma subida
  await assertSucceeds(setDoc(doc(a, 'muestras', 'aaaa1111_72'),
    subidaCompleta(TELEFONO_A, { ...resultadoMl, codigo: 'Muestra 072', estado: 'VALIDADO', revisadaPor: 'Diego Muñoz', revisadaEn: 1732400100000 }),
    { merge: true }));
});

test('conteo ML: una resubida con el mismo resultado pasa aunque la muestra ya tenga dictamen', async () => {
  await entorno.withSecurityRulesDisabled(async (ctx) => {
    await setDoc(doc(ctx.firestore(), 'muestras', 'aaaa1111_73'),
      subidaCompleta(TELEFONO_A, { ...resultadoMl, codigo: 'Muestra 073', estado: 'VALIDADO', revisadaPor: 'Diego Muñoz' }));
  });
  await assertSucceeds(setDoc(doc(bd(TELEFONO_A), 'muestras', 'aaaa1111_73'),
    { ...resultadoMl, observacion: 'Con clima', ownerUid: TELEFONO_A }, { merge: true }));
});

test('ataque: el teléfono no puede cambiar el resultado del modelo de una muestra con dictamen', async () => {
  await entorno.withSecurityRulesDisabled(async (ctx) => {
    await setDoc(doc(ctx.firestore(), 'muestras', 'aaaa1111_74'),
      subidaCompleta(TELEFONO_A, { ...resultadoMl, codigo: 'Muestra 074', estado: 'VALIDADO', revisadaPor: 'Diego Muñoz' }));
  });
  const a = bd(TELEFONO_A);
  await assertFails(setDoc(doc(a, 'muestras', 'aaaa1111_74'),
    { ml: { ...resultadoMl.ml, choritos: 99, total: 100 }, ownerUid: TELEFONO_A }, { merge: true }));
  await assertFails(setDoc(doc(a, 'muestras', 'aaaa1111_74'), { detecciones: [], ownerUid: TELEFONO_A }, { merge: true }));
});

test('ataque: resultado del modelo con campos extra, valores absurdos o demasiadas detecciones', async () => {
  const a = bd(TELEFONO_A);
  const malos = [
    { ml: { ...resultadoMl.ml, esAdmin: true } },
    { ml: { ...resultadoMl.ml, total: -1 } },
    { ml: { ...resultadoMl.ml, choritos: 5000 } },
    { ml: { ...resultadoMl.ml, version: 'v'.repeat(100) } },
    { ml: { ...resultadoMl.ml, editado: 'sí' } },
    { ml: 'muchos' },
    { detecciones: 'todas' },
    { detecciones: Array(601).fill({ e: 'C', p: 0.9, x: 1, y: 1, w: 1, h: 1, o: 'M' }) },
  ];
  for (const [i, malo] of malos.entries()) {
    await assertFails(setDoc(doc(a, 'muestras', `aaaa1111_8${i}`), subidaCompleta(TELEFONO_A, { ...resultadoMl, ...malo })));
  }
});

test('conteo ML: el supervisor puede corregir el resultado del modelo', async () => {
  await entorno.withSecurityRulesDisabled(async (ctx) => {
    await setDoc(doc(ctx.firestore(), 'muestras', 'aaaa1111_75'), subidaCompleta(TELEFONO_A, { ...resultadoMl, codigo: 'Muestra 075' }));
  });
  await assertSucceeds(updateDoc(doc(bd(SUPERVISOR), 'muestras', 'aaaa1111_75'), { 'ml.cholgas': 2, 'ml.choritos': 1 }));
});
