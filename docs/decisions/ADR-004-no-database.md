# ADR-004: Sin base de datos

## Contexto

El estado que hay que persistir es pequeño y muy estructurado: configuración,
vinculación, una playlist, una biblioteca de playlists del Controlador y una
carpeta de imágenes. Las alternativas eran SQLite, un ORM o archivos JSON.

## Decisión

Archivos JSON legibles y una carpeta de imágenes, sin base de datos ni migraciones
complejas. Las escrituras críticas usan archivo temporal + rename.

## Consecuencias

- El usuario puede inspeccionar, copiar o borrar el estado con el Explorador de
  Windows.
- No hay que instalar ni versionar un motor de base de datos.
- No hay consultas ni índices; para este volumen (una playlist, decenas de
  imágenes) no hacen falta.
- La escritura concurrente no está resuelta con transacciones: se evita con un
  solo proceso por rol y escrituras atómicas a nivel de archivo.
- Si un JSON se corrompe, se ignora y se registra el error; no hay recuperación
  automática. Es un riesgo aceptado para el MVP.
- Una futura función que necesite historial, varias playlists por pantalla o
  búsquedas probablemente justifique SQLite; hoy sería complejidad sin retorno.
