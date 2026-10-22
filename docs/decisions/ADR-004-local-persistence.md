# ADR-004: Persistencia local según el tipo de dato

## Estado

Aceptada para la migración de arquitectura en `codex/architecture-improvements`.
La implementación se aplica por fases; los archivos JSON anteriores se mantienen
como fuentes de migración y respaldo hasta validar cada importación.

## Contexto

La app guarda preferencias simples, playlists ordenadas, catálogo de medios,
credenciales de vinculación, configuración de kiosco y archivos binarios. El
adaptador monolítico `LocalStorage` y varios JSON mezclan esas responsabilidades;
además, una playlist puede quedar inconsistente si una actualización se interrumpe.
El Receptor debe conservar una copia local reproducible entre reinicios y sin red.

## Decisión

- **DataStore Preferences** guarda preferencias pequeñas: rol, identidad y nombre
  del dispositivo, puerto, tema, movimiento reducido y ajustes simples.
- **Room 3 sobre SQLite** guarda playlists y sus ítems ordenados, metadatos/hash
  de medios, emparejamientos y datos estructurados de kiosco. Las actualizaciones
  de playlist y catálogo usan transacciones y DAOs quedan ocultos detrás de
  repositorios por feature.
- **Archivos** guardan imágenes, logs, paquetes de actualización y copias JSON
  heredadas. Los bytes de imagen no se guardan como BLOB en Room; se validan en un
  archivo temporal y se renombran antes de aplicar sus referencias en la base.
- Los tokens de vinculación y hashes sensibles de kiosco se protegen con Windows
  DPAPI antes de escribirse en Room. Las pruebas usan un protector falso; un
  adaptador local de desarrollo fuera de Windows no se incluye en paquetes de
  producción.
- La migración de `config.json`, `paired.json`, `playlist.json`,
  `playlists.json` y `lock.json` es repetible. Se validan datos, se guardan copias
  en `legacy/` y solo se marca la migración completa cuando todo el conjunto queda
  importado. Un fallo conserva tanto las fuentes como los datos ya válidos.
- Los DTO y payloads JSON del protocolo LAN no cambian. La persistencia los mapea
  a modelos de dominio y entidades Room.
- El arranque normal nunca limpia la playlist ni los medios del Receptor. El
  borrado se limita a una acción explícita de reinicio o cambio de rol.

## Consecuencias

- Hay que incluir DataStore, Room 3, KSP y el driver SQLite bundled; los esquemas
  versionados se guardan en el repositorio.
- La base no se inspecciona como JSON plano. Para recuperar datos, hay respaldo
  durante la migración y el modelo puede exportarse con herramientas internas si
  hiciera falta en el futuro.
- DPAPI protege secretos en reposo contra la copia a otro perfil de Windows, pero
  los procesos del mismo usuario aún pueden solicitar el descifrado. No cifra el
  tráfico LAN ni sustituye la seguridad de la cuenta.
- Los medios y actualizaciones siguen siendo archivos y usan temporal + rename;
  una falla de sincronización no publica una playlist nueva incompleta.
- Los cambios se pueden desplegar gradualmente por `--data-dir`, sin cambiar el
  número de puerto, las rutas HTTP ni los DTO de red.
