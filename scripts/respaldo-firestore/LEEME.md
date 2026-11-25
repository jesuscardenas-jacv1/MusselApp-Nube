# Respaldo de Firestore

Guarda `muestras`, `fotos` y `supervisores` en archivos JSON, en tu computador. Es gratis (no necesita el plan Blaze)
y **solo lee**: no cambia nada en la base.

1. Consola de Firebase → ⚙️ Configuración del proyecto → **Cuentas de servicio** → **Generar nueva clave privada**.
   Guarda el archivo **fuera del repositorio** (por ejemplo `Documentos\claves-musselapp\`). No lo compartas por chat.
2. En PowerShell, desde esta carpeta:
   ```
   npm install
   $env:CLAVE_ADMIN = "C:\ruta\a\la\clave.json"
   npm run respaldo
   ```
3. El respaldo queda en `respaldo-AAAA-MM-DD/` en la raíz del repositorio (git lo ignora). Cópialo a un lugar seguro.

Un respaldo completo lee unos cientos o miles de documentos: está dentro de la cuota gratuita diaria (50.000 lecturas).
