# Arquitectura

> **Estado:** este documento describe la arquitectura objetivo de la rama
> `codex/architecture-improvements`. Las fases 7–12 migran el código por partes;
> hasta que terminen, algunas rutas y adaptadores todavía corresponden al diseño
> anterior basado en archivos JSON.

## Vista general

Override Kiosk es **un solo ejecutable** con dos roles que se eligen la primera vez:

- **Controlador (Emitter)**: cliente HTTP. Descubre pantallas, las vincula, edita la
  playlist y envía el contenido. **No abre puertos.**
- **Pantalla (Receiver)**: servidor HTTP. Se anuncia por mDNS, recibe imágenes y
  reproduce la playlist desde su copia local.

La topología es en estrella: solo existen conexiones Controlador → Pantalla. Dos
Controladores nunca se ven entre sí porque solo la Pantalla publica un servicio
mDNS.

```
┌──────────────────┐       mDNS (_overridekiosk._tcp)     ┌──────────────────┐
│   Controlador    │ ───────────────────────────────────► │     Pantalla     │
│  Ktor Client     │                                      │  Ktor Server     │
│  JmDNS browser   │ ──── HTTP + token (LAN) ───────────► │  JmDNS publish   │
└──────────────────┘                                      └──────────────────┘
                                                                   │
                                                          kiosk.db + media/
                                                                   │
                                                          reproducción local
```

Todo el tráfico es HTTP en texto plano dentro de la LAN. La seguridad se apoya en
el código de vinculación, un token y la validación de headers; los riesgos
aceptados están en [SECURITY.md](SECURITY.md).

## Módulo y capas

Hay **un único módulo Gradle** (`composeApp`) con dos source sets:

| Source set | Contenido |
| --- | --- |
| `commonMain` | Modelos y DTOs, lógica pura, contratos por feature, reducers/state holders, repositorios DataStore/Room, pantallas Compose y `core.ui`. |
| `desktopMain` | Servidor y cliente Ktor, JmDNS, builders DataStore/Room, DPAPI, archivos, ventana, argumentos y arranque. |

```
composeApp/src/
├── commonMain/kotlin/app/kiosksync/
│   ├── core/            # APP_NAME, constantes, Strings.kt, modelos, DTOs
│   ├── core/ui/         # tema M3, tipografía, iconos y componentes compartidos
│   ├── feature/
│   │   ├── pairing/     # domain/, presentation/, data/ cuando aplique
│   │   ├── sync/        # domain/, presentation/, data/ cuando aplique
│   │   ├── playlist/    # domain/, presentation/, data/ cuando aplique
│   │   ├── playback/    # domain/, presentation/, data/ cuando aplique
│   │   ├── projection/  # proyección del Controlador a sus propias pantallas
│   │   ├── update/      # aviso de actualización en la Pantalla
│   │   ├── kiosk/       # bloqueo, secuencia y recuperación
│   │   └── role/        # selección y persistencia del rol
│   ├── platform/        # puertos de red, protección de secretos y archivos
│   ├── data/            # database, migración y ensamblado de persistencia
│   ├── ui/              # App.kt, navegación y pantallas por rol
│   └── AppContainer.kt  # inyección manual
└── desktopMain/kotlin/app/kiosksync/
    ├── platform/        # JmDNS, Ktor, builders de almacenamiento, media y DPAPI
    └── Main.kt          # argumentos, logger, composición y ventana
```

Las dependencias apuntan hacia contratos de dominio por feature. Los repositorios
implementan esos contratos y ocultan DataStore, Room y el sistema de archivos. La
UI no conoce DAOs ni entidades de persistencia. La app mantiene un módulo Gradle,
un `AppContainer` manual y la navegación existente.

## Dependencias principales

| Aspecto | Elección |
| --- | --- |
| UI | Compose Multiplatform Desktop + Material 3 |
| Preferencias | DataStore Preferences |
| Datos estructurados | Room 3 + SQLite bundled; KSP genera el código y los esquemas |
| Tema | Material Kolor Material 3 con semilla de marca `#3D7AF5` |
| Red | Ktor Server/Client con motor CIO |
| Serialización | kotlinx.serialization JSON |
| Asincronía | Coroutines + `StateFlow` |
| Descubrimiento | JmDNS (mDNS/Zeroconf) |
| Imágenes | Skia (`org.jetbrains.skia.Image`) → `ImageBitmap` |
| Selector de archivos | `java.awt.FileDialog` |
| Pruebas | `kotlin.test` + render offscreen con `ImageComposeScene` |

## Composición de la aplicación

`AppContainer` construye una sola instancia de DataStore y Room y las entrega a
los repositorios; también construye una sola vez:

- repositorios de preferencias, playlists, medios y credenciales; `FileMediaStore`,
  protector de secretos y `FileAppLogger`.
- `JmDnsDiscovery`.
- `KtorEmitterClient` (Controlador) o `KtorReceiverServer` (Pantalla).
- `PairingManager`, `KioskLockManager`, `ReceiverStatusTracker`,
  `ReceiverPlaylistBridge`, `ReceiverPlaybackSyncBridge`, `ProjectionController`.
- `LocalUpdatePackageProvider` (Controlador) y `FileReceiverUpdateManager`
  (Pantalla) para las actualizaciones; ver [UPDATES.md](UPDATES.md).
- `LocalNetwork.detectLocalIpv4Address()` para mostrar la IP de la Pantalla en la
  pantalla de vinculación.

`Main.kt` lee los argumentos, crea el contenedor y abre la ventana. Si el rol es
Pantalla y no se pasó `--windowed`, la ventana es fullscreen, sin bordes y siempre
encima.

## Flujos

### 1. Descubrimiento y vinculación

1. La Pantalla arranca el servidor en el primer puerto libre desde `8765` y publica
   `_overridekiosk._tcp.local.` con `deviceId`, `deviceName`, `role=receiver` y
   `paired`.
2. El Controlador escucha mDNS y muestra las pantallas. El plan B es **Agregar por
   IP**, que llama a `GET /info` y comprueba `role == "receiver"`.
3. La Pantalla sin vincular muestra un código de 6 dígitos (5 minutos, 5 intentos,
   30 segundos de bloqueo). El Controlador envía `POST /pair`.
4. Si el código es correcto, la Pantalla genera un token de 32 bytes (hex). El
   token y los hashes de desbloqueo se protegen con DPAPI en Windows antes de
   persistirse en Room; el Controlador lo asocia al `deviceId`.
5. A partir de ahí, todas las rutas salvo `/info` y `/pair` exigen
   `X-Emitter-Id` + `X-Auth-Token`.

La vinculación se guarda por `deviceId`, no por IP. Si la Pantalla cambia de IP,
mDNS la vuelve a ubicar.

### 2. Sincronización

`EmitterSyncService` ejecuta el mismo plan para cada pantalla seleccionada:

1. `GET /sync/state` → versión actual y lista de `mediaId` + SHA-256.
2. `SyncPlanner.plan()` compara la playlist local con ese estado y devuelve:
   - imágenes a subir (no existen o cambió el hash),
   - imágenes huérfanas a borrar,
   - cuántas ya coinciden.
3. `PUT /media/{mediaId}` por cada imagen faltante, con el SHA-256 en
   `X-Content-Sha256`. La Pantalla verifica el hash antes de guardar.
4. `PUT /playlist` con el manifiesto completo. La Pantalla comprueba que **todas**
   las imágenes existan y tengan el hash correcto; si falta alguna responde
   `409` y conserva la playlist anterior.
5. Si todo está completo, actualiza playlist e ítems en una transacción Room,
   avisa al reproductor y limpia medios huérfanos después del commit. El carrusel
   cambia de versión sin cortar la imagen actual.

La playlist local del Controlador es la fuente de verdad. Un segundo envío sin
cambios no sube ninguna imagen.

### 3. Sincronización de reproducción

El Controlador envía `POST /playback/sync` con un retardo común de 2 segundos a
todas las pantallas vinculadas, en paralelo. Cada Pantalla espera ese retardo y
reinicia su carrusel desde la primera imagen. Al medirse desde la recepción de
cada petición, no depende de que los relojes de los equipos estén sincronizados.
Es una sincronización de mejor esfuerzo: la latencia de la LAN y la decodificación
de la primera imagen pueden dejar una diferencia visible. Si una Pantalla no
tiene una playlist activa, responde `409` y el Controlador la reporta como fallo.

### 4. Reproducción

`PlaybackStateHolder` observa la playlist local en Room, resuelve cada `mediaId` a
un archivo de `media/` y mantiene un bucle:

- muestra la imagen con `ContentScale.Fit` sobre negro,
- espera la duración efectiva (por imagen o la de por defecto),
- avanza al siguiente índice y vuelve al inicio al terminar,
- omite con un log las imágenes dañadas o ausentes.

La reproducción **no usa la red**. Si la Pantalla se queda sin conexión o se
reinicia, sigue con su playlist y media locales. El borrado solo ocurre por una
acción explícita de reinicio/cambio de rol.

### 5. Estado en línea

El Controlador consulta `GET /status` cada 5 segundos a cada pantalla vinculada.
Tras 3 fallos consecutivos la marca como desconectada y sigue intentando. La
Pantalla no cambia su comportamiento por estar desconectada.

### 6. Bloqueo tipo kiosco

La Pantalla arranca bloqueada. En Windows se instalan hooks de bajo nivel para
teclado (`WH_KEYBOARD_LL`) y mouse/touchpad (`WH_MOUSE_LL`); en Linux o con
`--windowed` se usa el manejo de teclas de Compose y el mouse no se bloquea. Al
desbloquear se libera el puntero para la card de opciones y se vuelve a capturar
al cerrarla. La secuencia de desbloqueo (solo Shift/Ctrl/Alt) se configura desde
el Controlador y se envía con la playlist o con `PUT /lock`. Si hay contraseña,
después de la secuencia se pide. Hay 5 intentos y 30 segundos de bloqueo. La
recuperación usa una frase universal compilada en la app; ver
[KIOSK_UNLOCK.md](KIOSK_UNLOCK.md) y los riesgos en [SECURITY.md](SECURITY.md).

### 7. Actualización de la aplicación

El Controlador empaquetado puede enviar su propio payload a las Pantallas:

1. `LocalUpdatePackageProvider` comprime la carpeta `app/` del Controlador una sola
   vez y la cachea en `<datos>\updates\`.
2. En cada consulta de estado, `AppVersionPolicy` compara `APP_VERSION` con el
   `appVersion` que la Pantalla reporta. Mismo `major` y Pantalla más vieja =
   actualización opcional; `major` distinto = forzada.
3. El Controlador sube el ZIP con `PUT /update/{version}` y `X-Content-Sha256`.
4. `FileReceiverUpdateManager` valida el hash, el manifiesto, el tamaño y las rutas
   del ZIP, y extrae el payload en `<datos>\updates\staged-<versión>`.
5. `ReceiverUpdateOverlay` pide confirmación (salvo que sea forzada). Al aceptar,
   un script de PowerShell espera el cierre de la app, reemplaza `app/` con
   `robocopy`, escribe `update-restart.marker` y vuelve a abrir el `.exe`.
6. En el siguiente arranque, el marcador hace que la Pantalla **no** borre su
   vinculación ni su contenido. Después se elimina.

La actualización no toca el runtime de Java, el lanzador ni `%APPDATA%` salvo la
carpeta `updates\`. Los límites y el procedimiento de publicación están en
[UPDATES.md](UPDATES.md).

## Persistencia

DataStore, Room y una carpeta de imágenes en `%APPDATA%\OverrideKiosk\` (o
`--data-dir`):

| Archivo | Contenido |
| --- | --- |
| `preferences.preferences_pb` | Rol, `deviceId`, nombre, puerto, tema y movimiento reducido. |
| `kiosk.db` | Playlists, ítems ordenados, catálogo de medios, emparejamientos y configuración de kiosco. |
| `legacy/` | Copia de JSON previos a la migración; no se carga después de migrar. |
| `credentialCiphertext` en Room | Secretos cifrados con DPAPI en Windows; nunca valores en claro. |
| `updates\` | Paquete propio del Controlador y paquetes recibidos por la Pantalla. |
| `skipped-update.txt` | Versión opcional que la Pantalla decidió posponer. |
| `update-restart.marker` | Marca temporal para conservar los datos tras actualizar. |
| `media\` | Imágenes nombradas por `mediaId` + extensión. |
| `logs\app.log` | Log con rotación (5 archivos de 1 MB). |
| `logs\update.log` | Detalle del ayudante de PowerShell que aplica la actualización. |

DataStore escribe preferencias transaccionalmente. Room conserva sus esquemas
versionados y migra sin borrar datos. Las imágenes se reciben a archivo temporal,
se verifican y se renombrarán antes de confirmar la playlist; la transacción de
Room aplica el manifiesto y la limpieza de archivos se hace después del commit.
Los errores de almacenamiento se registran y se muestran cuando afectan a una
acción del usuario.

## UI y estado

- Las features interactivas definen `State`, `Action`, reducer puro y `Effect`;
  `StateHolder` recibe acciones, expone `StateFlow` y procesa trabajo asíncrono.
  No hay ViewModels de AndroidX ni framework MVI.
- Navegación con `sealed interface Screen` y un `when` en `App.kt`.
- El sistema de diseño compartido está en `core/ui/`: tema claro/oscuro, tipografía
  Sora/Inter, iconos Material Symbols y componentes propios. Ver
  [DESIGN_SYSTEM.md](DESIGN_SYSTEM.md).
- Todos los textos visibles están en `core/Strings.kt`.
- El Controlador usa un layout de dos paneles en anchos ≥1100 dp y pestañas
  Pantallas/Playlist en anchos menores.

## Errores y logs

- Las rutas del servidor capturan excepciones y responden JSON con un código de
  error; nunca tumban el proceso.
- Las tareas en segundo plano capturan y registran sus fallos.
- `Main.kt` instala un `Thread.setDefaultUncaughtExceptionHandler` como última
  red: cualquier excepción no capturada se escribe en `logs/app.log`.
- La UI muestra el error junto al elemento que lo produjo, con una causa y una
  acción cuando se conoce.
- El log nunca incluye tokens ni códigos de vinculación.

## Pruebas

- `commonTest`: reducers, lógica pura, mapeos de protocolo y reglas de migración.
- `desktopTest`: DataStore/Room con datos temporales, migración JSON idempotente,
  DPAPI en Windows, operaciones atómicas de medios y recorridos UI existentes.
- Comando: `.\gradlew.bat build`.

## Decisiones de diseño

Las notas cortas están en [decisions/](decisions/):

- [Topología en estrella](decisions/ADR-001-star-topology.md)
- [Vinculación por código](decisions/ADR-002-pairing-code.md)
- [Caché local en la Pantalla](decisions/ADR-003-local-cache.md)
- [Persistencia local por responsabilidad](decisions/ADR-004-local-persistence.md)
