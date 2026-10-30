# Arquitectura

> **Estado:** arquitectura aplicada en la rama `codex/architecture-improvements`.
> Las fases 7–17 consolidan MVI ligero, paquetes por feature, `core.ui`,
> Material Kolor, DataStore para preferencias, Room para datos estructurados y
> archivos para medios y paquetes. En la fase 18, Linux pasó la build y el
> smoke check de `/info` y descubrimiento mDNS. El workflow de Windows pasó
> `clean build` y smoke checks en el runner hospedado `windows-2025-vs2026`,
> incluidos DPAPI, migración, reproducción local, hooks nativos y ambos modos
> de ventana. Falta validar la experiencia visible en un equipo Windows 11 real.

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
| `commonMain` | Modelos y DTOs, lógica pura, contratos por feature, reducers/state holders, pantallas Compose y `core.ui`. |
| `desktopMain` | Adaptadores DataStore/Room/archivos, Ktor, JmDNS, DPAPI, ventana, argumentos y arranque. |

```
composeApp/src/
├── commonMain/kotlin/app/kiosksync/
│   ├── core/            # APP_NAME, constantes, Strings.kt, modelos, DTOs
│   ├── core/ui/         # tema M3, tipografía, iconos y componentes compartidos
│   ├── feature/
│   │   ├── role/{domain,data,presentation}/
│   │   ├── pairing/{domain,data,presentation}/
│   │   ├── emitter/{domain,data,presentation/{components,coordinators}}/
│   │   ├── media/{domain,data}/
│   │   ├── playlist/{domain,data,presentation}/
│   │   ├── sync/domain/
│   │   ├── playback/{domain,presentation}/
│   │   ├── kiosk/{domain,data,presentation}/
│   │   ├── receiver/{domain,data}/
│   │   ├── projection/domain/
│   │   ├── guide/{domain,presentation/{components}}/
│   │   └── update/{domain,presentation}/
│   ├── local/           # KioskDatabase: une los DAO de cada feature
│   ├── migration/       # importación transaccional de JSON heredado
│   ├── ui/              # App.kt, navegación y pantallas por rol
│   └── AppContainer.kt  # inyección manual
└── desktopMain/kotlin/app/kiosksync/
    ├── feature/*/data/  # Room, DataStore, red y adaptadores propios de cada feature
    ├── local/           # builder SQLite y composición de repositorios
    ├── migration/       # importación de JSON heredado a Room
    ├── core/security/   # hash y protección compartida de secretos
    ├── platform/        # logger y servicios compartidos del host
    ├── Desktop*.kt      # fábrica del contenedor y ciclo de ventanas
    └── Main.kt          # argumentos, logger y punto de entrada
```

Las dependencias apuntan hacia contratos y modelos del dominio por feature. Las
implementaciones de repositorio, Room, DataStore, red y operaciones de datos viven
en `data` junto a la feature que las posee. Cada feature declara sus entidades y
su DAO. `app.kiosksync.local.KioskDatabase` solo los reúne en una base; el
builder y `RoomRepositories` viven en el mismo paquete, en `desktopMain`. La
importación del JSON heredado vive en `app.kiosksync.migration` porque escribe
varias features en una transacción. Hash y protección de secretos viven en
`app.kiosksync.core.security`. Los DAOs y las entidades son detalles de
persistencia:
presentación y dominio no los consumen. La app mantiene un módulo Gradle, un
`AppContainer` manual y la navegación existente.

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

`DesktopContainerFactory` inicializa de forma asíncrona una sola instancia de
DataStore Preferences y Room para el `dataDir` efectivo del proceso, migra los JSON
heredados antes de abrir la UI y entrega contratos pequeños a `AppContainer`.
`AppContainer.shutdown()` cierra ambos recursos. La composición crea una sola vez:

- repositorios de preferencias, playlists, medios y credenciales; `FileMediaStore`,
  protector de secretos y `FileAppLogger`.
- `JmDnsDiscovery`.
- `KtorEmitterClient` (Controlador) o `KtorReceiverServer` (Pantalla).
- `PairingManager`, `KioskLockManager`, `ReceiverStatusTracker`,
  `ReceiverPlaylistBridge`, `ReceiverPlaybackSyncBridge`, `ProjectionController`
  (estado compartido de proyección en `feature/projection/domain`).
- `LocalUpdatePackageProvider` (Controlador) y `FileReceiverUpdateManager`
  (Pantalla) para las actualizaciones; ver [UPDATES.md](UPDATES.md).
- `LocalNetwork.detectLocalIpv4Address()` para mostrar la IP de la Pantalla en la
  pantalla de vinculación.

`Main.kt` lee los argumentos, configura el logger y abre `DesktopApplicationRoot`.
La raíz crea el contenedor fuera del hilo de UI y muestra carga o reintento.
`DesktopWindowHost` posee las ventanas y el cierre de recursos, y sigue los cambios
de rol en tiempo de ejecución. Desde 1.1.3 todos los roles abren fullscreen y sin
bordes; la Pantalla además queda siempre encima. `--windowed` conserva una ventana
normal para depuración y desactiva los hooks nativos de kiosco.

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

`EmitterSyncRepositoryImpl` en `desktopMain/feature/sync/data` ejecuta el plan
para cada pantalla seleccionada. El contrato y los tipos de progreso/resumen viven en
`feature/sync/domain`; el plan de comparación sigue siendo lógica pura allí:

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

La Pantalla se bloquea cuando está vinculada y recibió una contraseña del
Controlador; el código de vinculación y la espera de esa contraseña siguen
usables. En Windows se instalan hooks de bajo nivel para teclado
(`WH_KEYBOARD_LL`) y mouse/touchpad (`WH_MOUSE_LL`); en Linux o con `--windowed` se
usa el manejo de teclas de Compose y el mouse no se bloquea. Al desbloquear se
libera el puntero para la card de opciones y se vuelve a capturar al cerrarla. La
secuencia de desbloqueo (solo Shift/Ctrl/Alt) se configura desde el Controlador y
se envía con la playlist o con `PUT /lock`. Si hay contraseña, después de la
secuencia se pide. Hay 5 intentos y 30 segundos de bloqueo. La recuperación usa
una frase universal compilada en la app; ver
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
| `app-config.preferences_pb` | Rol, `deviceId`, nombre, puerto, tema y movimiento reducido. |
| `kiosk.db` | Playlists, ítems ordenados, catálogo de medios, emparejamientos y configuración de kiosco. |
| `legacy/config.json` | Respaldo de la configuración previa que DataStore importó. |
| `legacy/*.room-v1*` | Respaldos de pairing, playlists y bloqueo importados en Room; los respaldos de secretos están protegidos. |
| `credentialCiphertext` en Room | Secretos cifrados con DPAPI en Windows; nunca valores en claro. |
| `updates\` | Paquete propio del Controlador y paquetes recibidos por la Pantalla. |
| `skipped-update.txt` | Versión opcional que la Pantalla decidió posponer. |
| `update-restart.marker` | Marca temporal para conservar los datos tras actualizar. |
| `media\` | Imágenes nombradas por `mediaId` + extensión. |
| `logs\app.log` | Log con rotación (5 archivos de 1 MB). |
| `logs\update.log` | Detalle del ayudante de PowerShell que aplica la actualización. |

DataStore expone configuración mediante `Flow` y operaciones `suspend`; su
implementación vive en `feature/role/data`. Room conserva esquemas versionados y
los repositorios feature-specific implementan contratos de `domain`. Las imágenes
se reciben a archivo temporal, se verifican y se renombran antes de confirmar la
playlist; Room aplica el manifiesto en transacción y la limpieza de archivos ocurre
después del commit. Los errores de almacenamiento se registran y se muestran
cuando afectan a una acción del usuario.

## UI y estado

- El Controlador, la selección de rol y el editor de playlist usan acciones
  tipadas y estado inmutable. Cada ViewModel o state holder aplica las
  transiciones y coordina efectos; un reducer puro se añade solo cuando separa
  lógica no trivial. Los efectos de una sola vez usan un canal consumible
  (confirmar rol o abrir el selector de imágenes); las operaciones de red y
  almacenamiento mantienen su progreso y error en el estado. No hay ViewModels
  de AndroidX ni framework MVI.
- Rol, guía y editor de playlist mantienen `Action`, `UiState` y `ViewModel` en
  la raíz de su paquete `presentation`, sin subcarpetas MVI ni reducers
  mecánicos. Las pantallas reciben una instantánea de estado y callbacks; los
  managers de pairing, playback y kiosco conservan su estado especializado
  porque coordinan temporizadores, recursos o eventos externos.
- `feature/emitter/presentation` contiene en el paquete raíz `EmitterUiState`,
  `EmitterAction`, `EmitterViewModel` y `EmitterScreen`. El ViewModel aplica las
  transiciones locales y delega efectos en coordinadores. `App.kt` crea el
  ViewModel desde `AppContainer`, gestiona `start/stop` con el ciclo de vida del
  workspace y conecta directamente el Screen; un `EmitterRoute` duplicaría esa
  composición sin aportar lógica. `coordinators` agrupa los flujos que adaptan
  operaciones y resultados al estado de UI; `components` agrupa los composables
  exclusivos del Controlador. La pantalla recibe estado y un callback de
  acciones, y no conserva ni invoca el ViewModel. Hay un solo `EmitterViewModel`;
  los coordinadores actualizan estado y progreso, no son ViewModels
  independientes. Emparejamiento, dirección manual y configuración de seguridad
  usan casos de uso en `domain`. `EmitterClient`,
  `EmitterScreensRepository` y `EmitterSyncRepository` son contratos de dominio;
  `EmitterServiceImpl`, `EmitterUpdateRunner`, Ktor, Room y la transferencia de
  medios viven en `data`. Botones, diálogos e iconos reutilizados por otras features
  viven en `core/ui`.
- `ProjectionController` vive en `feature/projection/domain` porque comparte el
  estado de proyección entre la UI y el host de ventanas sin depender de Compose.
- `EmitterSyncRepositoryImpl` es una implementación Desktop en `desktopMain`; el
  contrato y `SyncPlanner` puro permanecen en `feature/sync/domain`.
- Navegación con `sealed interface Screen` y un `when` en `App.kt`.
- El sistema de diseño compartido está en `core/ui/`: tema claro/oscuro, tipografía
  Sora/Inter, iconos Material Symbols y componentes propios. Ver
  [DESIGN_SYSTEM.md](DESIGN_SYSTEM.md).
- Los componentes compartidos se promueven a `core.ui` cuando tienen consumidores
  reales en varias features; componentes específicos permanecen junto a su feature.
- Todos los textos visibles están en `core/Strings.kt`.
- La guía interactiva vive en `feature/guide/presentation`: `GuideViewModel`
  mantiene la máquina de estados y emite efectos tipados; `GuideStep`, el modelo
  del flujo, vive en `feature/guide/domain`. `GuideScreen` es la entrada de
  presentación y delega el overlay, el diálogo de rol, la tarjeta y el contenido
  de los pasos a `presentation/components`. Las pantallas reportan playlist,
  vinculación, contraseña y contenido para que avance al completar cada paso.
  Ver [GUIDE.md](GUIDE.md).
- El Controlador usa un layout de dos paneles en anchos ≥1100 dp y pestañas
  Pantallas/Playlist en anchos menores.

## Errores y logs

- Las rutas se separan en `feature/receiver/data/server/` por acceso público, vinculación,
  contenido/sincronización, estado y actualización. Los payloads malformados se
  responden como error de cliente y la cancelación de una petición se conserva.
  Los errores de operación se registran en el límite que los conoce.
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
- `CatalogRenderTest` renderiza ambos temas y comprueba contraste ≥4.5:1 en roles
  Material y pares semánticos de éxito, advertencia e información.
- Comando: `.\gradlew.bat build`.

## Decisiones de diseño

Las notas cortas están en [decisions/](decisions/):

- [Topología en estrella](decisions/ADR-001-star-topology.md)
- [Vinculación por código](decisions/ADR-002-pairing-code.md)
- [Caché local en la Pantalla](decisions/ADR-003-local-cache.md)
- [Persistencia local por responsabilidad](decisions/ADR-004-local-persistence.md)
