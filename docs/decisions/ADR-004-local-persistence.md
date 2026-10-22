# ADR-004: Persistencia local según el tipo de dato

## Estado

Aceptada e implementada para la persistencia local en
`codex/architecture-improvements`.

## Contexto

La app guarda preferencias simples, playlists ordenadas, catálogo de medios,
credenciales de vinculación, configuración de kiosco y archivos binarios. El
antiguo adaptador monolítico `LocalStorage` y varios JSON mezclaban esas
responsabilidades; `LocalStorage` ya no tiene consumidores de producción. Además,
una playlist puede quedar inconsistente si una actualización se interrumpe. El
Receptor debe conservar una copia local reproducible entre reinicios y sin red.

## Decisión

- **DataStore Preferences** guarda preferencias pequeñas: rol, identidad y nombre
  del dispositivo, puerto, tema, movimiento reducido y ajustes simples. El contrato
  `AppConfigStore` y `AppConfig` viven en `feature/role/domain`; su implementación
  Desktop está en `feature/role/data/DataStoreAppConfigStore.kt`. Usa el archivo
  `app-config.preferences_pb` bajo el `dataDir` resuelto y expone `Flow`/`suspend`.
  El arranque crea una instancia por proceso y `AppContainer.shutdown()` cancela
  su scope.
- **Room 3 sobre SQLite** guarda playlists y sus ítems ordenados, metadatos/hash
  de medios, emparejamientos y datos estructurados de kiosco. Las actualizaciones
  de playlist y catálogo usan transacciones. El esquema compartido (base, entidades
  y DAO) vive en `commonMain/data/local/room`; las implementaciones de repositorio
  están en `desktopMain/feature/<nombre>/data` cuando pertenecen a una feature. Los
  adaptadores compartidos de medios y limpieza viven en `desktopMain/data/local`.
  Los DAOs no salen de la capa `data`.
- **Archivos** guardan imágenes, logs, paquetes de actualización y copias JSON
  heredadas. Los bytes de imagen no se guardan como BLOB en Room; se validan en un
  archivo temporal y se renombran antes de aplicar sus referencias en la base.
- Los tokens de vinculación y hashes sensibles de kiosco se protegen con Windows
  DPAPI antes de escribirse en Room. Las pruebas usan un protector falso; un
  adaptador local de desarrollo fuera de Windows no se incluye en paquetes de
  producción.
- `config.json` se migra por separado a DataStore con una copia en
  `legacy/config.json`. `paired.json`, `playlist.json`, `playlists.json` y
  `lock.json` se importan en una transacción Room; sus copias se conservan bajo
  `legacy/` y las de archivos con secretos están protegidas. La migración Room solo
  se marca completa después de importar y retirar las fuentes. Un fallo conserva
  las fuentes y los datos ya importados para reintentar.
- Los DTO y payloads JSON del protocolo LAN no cambian. La persistencia los mapea
  a modelos de dominio y entidades Room.
- El arranque normal nunca limpia la playlist ni los medios del Receptor. El
  borrado se limita a una acción explícita de reinicio o cambio de rol.

## Consecuencias

- Hay que incluir DataStore, Room 3, KSP y el driver SQLite bundled; los esquemas
  versionados se guardan en el repositorio en
  `composeApp/schemas/app.kiosksync.data.local.room.KioskDatabase/`.
- `DesktopContainerFactory` es el único punto de creación de DataStore y Room por
  proceso/directorio. `AppContainer.shutdown()` cierra la persistencia; ningún
  consumidor de UI, state holder o ruta HTTP accede directamente a un DAO ni usa
  una fachada síncrona común.
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
