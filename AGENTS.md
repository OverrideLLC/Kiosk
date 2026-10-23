# Override Kiosk: Plan de trabajo para el agente de IA

> Nombre de trabajo del proyecto: **Override Kiosk** (usa una sola constante `APP_NAME` para que el cambio sea trivial).
> Pega este documento completo al inicio de la sesión con el agente. Todo lo que sigue está escrito como instrucciones para él.

---

## 1. Tu rol y cómo debes trabajar

Eres un ingeniero de software senior especializado en Kotlin Multiplatform y Compose Desktop. Vas a construir, junto con una persona, una aplicación de escritorio open source para Windows 11. Reglas de comportamiento:

1. **Responde siempre en español.** El código, los nombres de clases y los commits van en inglés.
2. **Trabaja por fases, en orden.** Este documento define las fases (sección 12). Requiere la frase de aprobación antes de avanzar, salvo cuando la persona haya autorizado expresamente ejecutar consecutivamente un conjunto de fases.
3. **Al terminar una fase, reporta el resultado** con el formato de la sección 14. Si no hay autorización expresa para una secuencia, detente y espera la aprobación exacta antes de escribir o preparar la siguiente fase.
4. **Si necesitas que la persona haga algo a mano, dilo con la ruta exacta** (carpeta y nombre de archivo, relativos a la raíz del proyecto) y el contenido que debe poner. Nunca asumas que lo hizo; pídele confirmación.
5. **No inventes versiones de dependencias ni APIs.** Antes de fijar una versión de Kotlin, Compose Multiplatform, Ktor, JmDNS o kotlinx.serialization, consulta la documentación oficial o el catálogo de versiones del proyecto generado y usa la última versión estable compatible entre sí. Si no puedes verificar algo, dilo y pregunta.
6. **No agregues funciones fuera del alcance** (sección 13), aunque parezcan útiles. Si crees que algo falta, propónlo al final del reporte de la fase, sin implementarlo.
7. **Prefiere lo simple.** Si dudas entre una solución con menos código y una más "elegante", elige la de menos código. Nada de frameworks de inyección de dependencias, nada de capas vacías.
8. **Cada fase debe terminar compilando y ejecutándose.** Nunca entregues código que no hayas razonado de principio a fin ni dejes `TODO` que rompan la compilación.
9. **Comandos para Windows (PowerShell)**: usa `.\gradlew.bat`, rutas con `\` en instrucciones para la persona.
10. **Al cerrar cada fase, haz tú mismo el commit y el push a GitHub** siguiendo las reglas de Git de la sección 1.1, y recién después presenta el reporte.

### 1.1 Reglas de Git (obligatorias)

- El repositorio y su remoto `origin` ya los configura la persona (ver Fase 0). **Nunca cambies el remoto, las credenciales ni la configuración global de Git.**
- Trabaja en la rama `main` (o la que la persona indique) y usa `git status` antes de cada commit para revisar qué entra.
- **Haz commit y push solo cuando la fase compile, se ejecute y sus tests pasen** (`.\gradlew.bat build`). Si algo falla, corrígelo antes; no subas código roto.
- Pasos al cerrar una fase: `git add -A`, `git commit -m "<mensaje>"`, `git push origin <rama>`.
- Mensaje de commit en inglés, formato `feat(faseN): <resumen corto>` (usa `chore`, `fix` o `docs` cuando corresponda). Un commit por fase como mínimo; si la persona pide cambios después del reporte, haz un commit nuevo por esos cambios y vuelve a hacer push.
- **Prohibido:** `git push --force`, reescribir historial, borrar ramas, subir contraseñas, tokens o archivos de datos locales (`paired.json`, `config.json`, carpetas `media`, logs).
- Mantén un `.gitignore` que excluya como mínimo: `build/`, `.gradle/`, `.idea/`, `local.properties`, `*.log`, `*.iml`.
- **Si el push falla** (credenciales, remoto, conflicto), detente: no intentes arreglarlo cambiando configuración. Reporta el error exacto a la persona y pídele que lo resuelva; luego reintenta solo el push.
- En el reporte incluye el hash corto del commit y confirma que el push terminó bien.

---

## 2. Qué es el proyecto

Una aplicación de escritorio **open source, offline y sin suscripción** para mostrar un carrusel de imágenes (por ejemplo, fichas técnicas y promociones) en una o varias computadoras Windows de un mismo comercio. Una computadora administra y envía el contenido; las demás lo reciben y lo reproducen a pantalla completa. Todo ocurre dentro de la red local (LAN). No hay nube, cuentas ni servidores externos.

Es **un solo ejecutable con dos roles**. La primera vez que se abre, la app pregunta qué rol tendrá ese equipo:

| Rol | Nombre en la UI | Qué hace |
| --- | --- | --- |
| **Emisor** | "Controlador" | Descubre pantallas, las vincula, administra la playlist y envía contenido. Es **cliente**: no abre ningún puerto. |
| **Receptor** | "Pantalla" | Se anuncia en la red, se vincula con un código, recibe contenido y lo reproduce a pantalla completa. Es **servidor**. |

Regla central: **un Emisor nunca se conecta con otro Emisor.** Solo hay conexiones Emisor → Receptor. Un Emisor puede vincular varios Receptores; un Receptor solo puede estar vinculado a **un** Emisor a la vez.

---

## 3. Las tres funciones básicas (alcance del MVP)

1. **Conexión y vinculación.** Selección de rol, descubrimiento automático en la LAN, vinculación mediante código numérico de 6 dígitos y reconexión automática posterior.
2. **Envío y sincronización de contenido.** El Emisor envía la playlist y las imágenes al Receptor y solo transfiere lo que al Receptor le falta.
3. **Reproducción del carrusel.** El Receptor muestra las imágenes a pantalla completa, en bucle, con el tiempo configurado, y sigue funcionando sin red usando su copia local.

Todo lo demás está fuera del MVP (sección 13).

---

## 4. Reglas y restricciones obligatorias

- Plataforma objetivo: **Windows 10/11, incluida la edición Home.** No dependas de funciones exclusivas de Windows Pro.
- Entorno de desarrollo de la persona: **Windows 11**, IntelliJ IDEA (o Android Studio) con el plugin de Kotlin Multiplatform.
- **Solo target Desktop (JVM)** en el MVP. No configures Android, iOS ni web.
- **100 % offline.** Prohibido cualquier llamada a internet, telemetría o analítica.
- **Solo LAN.** El Receptor escucha en la red local; el Emisor solo hace conexiones salientes.
- Persistencia local separada por tipo: DataStore Preferences para configuración pequeña, Room 3 para datos estructurados y archivos para imágenes, logs y paquetes. Los JSON existentes se migran de forma segura y se conservan como respaldo.
- **Sin frameworks de DI, sin librerías de navegación.** DI manual con un `AppContainer`; navegación con una `sealed interface Screen` y un `when`.
- Una sola dependencia de red en el Receptor (Ktor Server) y una en el Emisor (Ktor Client). Evita agregar librerías que no estén listadas en la sección 5 sin preguntar.
- La app **nunca debe cerrarse sola** por una excepción: captura errores en los límites (rutas del servidor, tareas en segundo plano) y regístralos en un archivo de log.
- Todos los textos de UI en **español**, centralizados en un único archivo (`Strings.kt`) para facilitar traducirlos después.
- Formatos de imagen soportados: **JPG, PNG, WEBP**. Tamaño máximo por imagen: **25 MB**. Rechaza con mensaje claro lo demás.
- Código bajo licencia permisiva (propón MIT o Apache-2.0 en la fase final; no la elijas sin aprobación).

---

## 5. Requisitos técnicos

| Aspecto | Decisión |
| --- | --- |
| Lenguaje y UI | Kotlin + Compose Multiplatform (Desktop) |
| JDK | 17 o superior (verifica compatibilidad con la versión del plugin de Compose) |
| Build | Gradle (Kotlin DSL) con catálogo de versiones `libs.versions.toml` |
| Preferencias locales | AndroidX DataStore Preferences KMP, con una instancia estable por proceso |
| Datos estructurados | AndroidX Room 3 + SQLite bundled, con esquemas versionados generados por KSP |
| Tema | Material 3 con Material Kolor; mantener tokens semánticos de la app |
| Red, Receptor | Ktor Server (motor CIO), solo en `desktopMain` |
| Red, Emisor | Ktor Client (motor CIO) |
| Serialización | kotlinx.serialization (JSON) |
| Asincronía | Kotlin Coroutines + `StateFlow` |
| Descubrimiento | JmDNS (mDNS/Zeroconf), solo en `desktopMain`, más campo manual de IP como plan B |
| Imágenes | Decodificación con Skia (`org.jetbrains.skia.Image`) → `ImageBitmap`, sin librerías extra |
| Selector de archivos | `java.awt.FileDialog` (sin dependencias extra) |
| Tests | `kotlin.test` para lógica pura, reducers, repositorios, migración JSON y persistencia local con DataStore/Room en Desktop |
| Empaquetado | Plugin de Compose (`jpackage`): `createDistributable` (carpeta portable) y `packageExe` (instalador; requiere WiX Toolset en la máquina de build) |

### Pasos manuales que la persona deberá hacer (avísale en la fase que corresponda)

- Instalar JDK 17+ y verificar con `java -version`.
- Generar el proyecto base en el asistente oficial de KMP (https://kmp.jetbrains.com) seleccionando **solo Desktop**, y descomprimirlo en la carpeta del repositorio.
- Permitir la app en el **Firewall de Windows** (red privada) la primera vez que el Receptor abra el puerto.
- Instalar **WiX Toolset** si se quiere generar el instalador `.exe` o `.msi` (no hace falta para la carpeta portable).
- Colocar el ícono de la app en `composeApp\src\desktopMain\resources\icon.ico` antes del empaquetado.

---

## 6. Arquitectura: "feature-first" con capas ligeras

Usa una **Clean Architecture pragmática**: separa por **funcionalidad** y, dentro de cada una, solo las capas que hagan falta. El objetivo es empezar a escribir lógica de negocio en minutos, no configurar capas.

Reglas:

1. **Un solo módulo Gradle** (`composeApp`) con los source sets `commonMain` y `desktopMain`. Nada de multimódulo en el MVP.
2. **`commonMain`** contiene: modelos, DTOs del protocolo, lógica pura (generación y validación del código, diff de sincronización, estado del carrusel), estado de UI y pantallas Compose.
3. **`desktopMain`** contiene lo que depende de JVM/Windows: servidor Ktor, JmDNS, acceso a archivos, ventana a pantalla completa, arranque de la aplicación.
4. **Las dependencias apuntan hacia `commonMain`.** UI y dominio dependen de contratos por feature (repositorios y puertos de plataforma); `desktopMain` implementa acceso a archivos, DPAPI, red y creación de bases. No expongas DAOs a UI/dominio ni crees interfaces para lógica pura.
5. **Sin "use cases" por defecto.** Si una operación es una sola línea, vive en el `StateHolder`. Crea una clase de dominio solo cuando tenga reglas propias (por ejemplo `PairingManager`, `SyncPlanner`).
6. **Estado de UI:** usa MVI ligero (estado inmutable, acciones tipadas, reducer puro, `StateFlow` y efectos de una sola vez como `Flow`) en flujos con transiciones de UI claras, como rol, Controlador y editor de playlist. Pairing, reproducción y bloqueo kiosco pueden conservar managers/state holders especializados cuando coordinan temporizadores, recursos o eventos externos; no añadas un reducer mecánico. No uses ViewModel de AndroidX ni un framework MVI.
7. **Resultados:** usa `Result` de Kotlin o una `sealed interface` simple para errores esperados; excepciones solo para lo inesperado.

### Estructura de carpetas

```
composeApp/src/
├── commonMain/kotlin/app/kiosksync/
│   ├── core/            # APP_NAME, constantes, Strings.kt, modelos compartidos, DTOs del protocolo
│   ├── core/ui/         # tema, tokens y componentes realmente compartidos
│   ├── data/local/room/ # entidades, DAO y esquema Room compartido
│   ├── feature/
│   │   ├── role/{domain,presentation}/
│   │   ├── pairing/{domain,presentation}/
│   │   ├── emitter/{domain,presentation}/
│   │   ├── playlist/{domain,presentation}/
│   │   ├── sync/domain/
│   │   ├── playback/{domain,presentation}/
│   │   ├── kiosk/{domain,presentation}/
│   │   ├── projection/presentation/
│   │   ├── guide/{domain,presentation}/
│   │   └── update/presentation/
│   ├── platform/        # contratos de repositorios y servicios de plataforma
│   ├── ui/              # App.kt, navegación y pantallas por rol
│   └── AppContainer.kt  # DI manual
└── desktopMain/kotlin/app/kiosksync/
    ├── feature/*/data/  # adaptadores Room por feature y DataStore de role
    ├── data/local/      # composición Room y adaptadores de medios/reset compartidos
    ├── data/local/migration/ # importación transaccional de JSON heredado a Room
    ├── data/local/room/ # builder SQLite Desktop
    ├── platform/        # JmDnsDiscovery, Ktor, acceso a medios y DPAPI
    ├── platform/server/ # rutas HTTP agrupadas por responsabilidad
    ├── Desktop*.kt      # fábrica del contenedor y ciclo de ventanas
    └── Main.kt          # argumentos de línea de comandos y punto de entrada
```

Dentro de cada `feature/<nombre>/` crea `domain`, `presentation` y `data` solo cuando contengan responsabilidades reales. La capa `data` implementa contratos del dominio; las entidades Room nunca son modelos de protocolo. Comparte un componente `core.ui` cuando tenga consumidores reales en más de una feature; los componentes exclusivos permanecen en su feature. Mantén un solo módulo Gradle y el `AppContainer` manual. DataStore Preferences almacena preferencias pequeñas; Room 3 datos estructurados; archivos, imágenes, logs y paquetes.

---

## 7. Roles, vinculación y protocolo

### 7.1 Primer arranque

1. Si DataStore no tiene rol guardado (o aún no se ha migrado `config.json`), mostrar la pantalla **"¿Qué rol tendrá este equipo?"** con dos opciones: **Controlador (envía contenido)** y **Pantalla (recibe y reproduce)**.
2. Guardar la elección en DataStore Preferences. Arranques posteriores van directo a su rol. Debe existir una opción para **cambiar de rol** (borra la vinculación y reinicia la configuración, con confirmación).

### 7.2 Descubrimiento

- El **Receptor** publica un servicio mDNS de tipo `_overridekiosk._tcp.local.` con: nombre del equipo, puerto real, `deviceId` (UUID persistente), `role=receiver` y si está vinculado o no (`paired=true|false`).
- El **Emisor** busca ese tipo de servicio y muestra los Receptores encontrados. **Los Emisores no se publican**, por lo que es imposible que dos Emisores se vean entre sí.
- Plan B: campo para ingresar **IP:puerto** manualmente (para redes donde mDNS falla). El Emisor llama a `GET /info` y valida que `role == "receiver"`.
- Puerto por defecto del Receptor: **8765**. Si está ocupado, usar el siguiente libre y anunciar el real por mDNS.
- El Emisor guarda los Receptores vinculados por `deviceId`, **no por IP**, y los vuelve a ubicar por mDNS cuando cambie la IP.

### 7.3 Vinculación por código

1. El Receptor sin vincular muestra en pantalla un **código de 6 dígitos** y entra en **modo de vinculación**. Solo en ese modo acepta `POST /pair`.
2. El código es válido **5 minutos** y se renueva automáticamente al vencer.
3. Máximo **5 intentos fallidos**; al alcanzarlos, el Receptor genera un código nuevo y rechaza solicitudes durante 30 segundos.
4. El Emisor selecciona un Receptor, la persona escribe el código y el Emisor envía `POST /pair` con `emitterId`, `emitterName` y `code`.
5. Si el código es correcto, el Receptor genera un **token aleatorio de 32 bytes (hex)**, lo protege con DPAPI en Windows y lo guarda junto al `emitterId` en Room; el token en claro se devuelve al Emisor, que lo almacena protegido junto al `deviceId` del Receptor.
6. A partir de ahí el Receptor **deja de mostrar el código**, sale del modo de vinculación y rechaza vinculaciones nuevas hasta que se desvincule.
7. Desvincular: desde el Emisor (`DELETE /pair`) o, localmente en el Receptor, con un atajo oculto (`Ctrl+Shift+P`) que pide confirmación. Ambos casos eliminan el registro de emparejamiento en Room y vuelven a mostrar un código nuevo.

### 7.4 Reglas de acceso (el "header" del Emisor)

- Toda ruta del Receptor **excepto** `GET /info` y `POST /pair` exige los headers:
  - `X-Emitter-Id`: id del Emisor vinculado.
  - `X-Auth-Token`: token entregado al vincular.
- Si faltan o no coinciden con el registro de emparejamiento → **`401 Unauthorized`**. Lee el secreto mediante el repositorio, desprotegiéndolo con DPAPI en Windows; usa comparación en tiempo constante para el token.
- `POST /pair` solo funciona si el Receptor está sin vincular y en modo de vinculación; si no, `409 Conflict`.

### 7.5 Rutas del Receptor

| Método y ruta | Auth | Función |
| --- | --- | --- |
| `GET /info` | No | `deviceId`, `deviceName`, `role`, `paired`, `appVersion` |
| `POST /pair` | Código | Vincula y devuelve el token |
| `DELETE /pair` | Sí | Desvincula |
| `GET /sync/state` | Sí | Lista de `mediaId` y hash que el Receptor ya tiene y versión de playlist actual |
| `PUT /media/{mediaId}` | Sí | Sube una imagen (cuerpo binario, header `X-Content-Sha256`); el Receptor verifica el hash antes de guardar |
| `PUT /playlist` | Sí | Confirma la playlist nueva (JSON con versión); solo se aplica si todas sus imágenes están ya guardadas |
| `GET /status` | Sí | Estado del Receptor: versión de playlist en reproducción, imagen actual |

### 7.6 Sincronización (diff, no reenvío total)

1. El Emisor pide `GET /sync/state`.
2. `SyncPlanner` (lógica pura, con tests) compara los `mediaId`+hash de la playlist local contra los del Receptor y devuelve: imágenes a subir, imágenes a borrar.
3. El Emisor sube solo las faltantes con `PUT /media/{id}`.
4. El Emisor envía `PUT /playlist` con el manifiesto completo y un número de versión incremental.
5. El Receptor guarda el manifiesto, borra las imágenes huérfanas y el carrusel pasa a la versión nueva **sin cortar la imagen actual**.
6. Si algo falla a mitad de camino, el Receptor conserva la playlist anterior intacta (la nueva solo se aplica en el paso 5, cuando todo está completo).

### 7.7 Estado en línea / desconectada

- El Emisor consulta `GET /status` cada **5 segundos** a cada Receptor vinculado. Tras **3 fallos seguidos** lo marca como "desconectada" y sigue intentando.
- El Receptor **nunca depende de la red para reproducir**: siempre usa su copia local.

---

## 8. Seguridad (decisión para el MVP y mejora opcional)

Estado deseado del MVP: **tráfico HTTP en texto plano dentro de la LAN**, protegido por el código de vinculación, el token y la validación de headers. Esto es una decisión consciente y debe quedar documentada en `docs/SECURITY.md` con sus límites:

- Quien pueda capturar tráfico en la red local podría leer el token. Es un riesgo aceptado para el MVP.
- El código de 6 dígitos es débil contra fuerza bruta, por eso existen la ventana de 5 minutos y el límite de 5 intentos.

**Fase opcional posterior al MVP (no la implementes sin aprobación explícita):** cifrar o firmar las peticiones usando una clave derivada del código de vinculación (por ejemplo, firma HMAC-SHA256 por petición o cifrado AES-GCM del cuerpo). El código nunca viaja por la red, lo escribe la persona en ambos equipos, así que la derivación es segura. Si se implementa, debe ser una capa que envuelva el cliente y el servidor sin cambiar las rutas.

---

## 9. Datos en disco

Carpeta base: `%APPDATA%\OverrideKiosk\` (configurable con `--data-dir`, ver sección 10).

```
OverrideKiosk\
├── app-config.preferences_pb   # configuración pequeña vía DataStore
├── kiosk.db                    # playlists, ítems, catálogo y preferencias estructuradas
├── legacy\                     # JSON previos conservados tras migrar
├── media\                      # imágenes (nombradas por mediaId + extensión)
├── updates\                    # paquetes/estado de actualización
└── logs\app.log                # log con rotación simple (máx. 5 archivos de 1 MB)
```

Los tokens y hashes de desbloqueo se cifran con Windows DPAPI antes de persistirse en Room. En desarrollo fuera de Windows se permite únicamente un protector local de desarrollo; en Windows empaquetado se usa DPAPI. Una instancia compartida de DataStore y una de Room se crean por proceso usando el directorio efectivo, incluido `--data-dir`. El arranque normal nunca borra la playlist local ni su media.

Modelo mínimo de playlist para el protocolo LAN y para leer los JSON previos durante la migración (la persistencia vigente de playlists vive en Room):

```json
{
  "version": 3,
  "defaultDurationSec": 10,
  "items": [
    { "mediaId": "uuid", "fileName": "ficha1.png", "sha256": "…", "durationSec": null }
  ]
}
```

`durationSec = null` significa "usar la duración por defecto". Duración mínima permitida: 3 s; máxima: 600 s.

---

## 10. Facilidad de desarrollo y pruebas en una sola computadora

Soporta estos **argumentos de línea de comandos** desde `Main.kt`, para poder probar Emisor y Receptor a la vez en la misma PC:

- `--data-dir=<ruta>`: carpeta de datos alternativa.
- `--role=emitter|receiver`: fuerza el rol (omite la pantalla de selección).
- `--port=<n>`: puerto del Receptor.
- `--windowed`: abre en ventana normal para depuración y desactiva los hooks nativos de kiosco.

Por defecto, todos los roles abren a pantalla completa y sin bordes desde la 1.1.3.
`--windowed` se conserva para depuración y recorridos automatizados.

Ejemplo de prueba local con dos procesos:

```powershell
.\gradlew.bat run --args="--role=receiver --data-dir=C:\tmp\rx --windowed"
.\gradlew.bat run --args="--role=emitter --data-dir=C:\tmp\tx --windowed"
```

Si el mDNS no encuentra el proceso en la misma PC, el campo manual de IP con `127.0.0.1:8765` debe funcionar.

---

## 11. Interfaz de usuario (mínima)

**Emisor (Controlador):** una sola ventana con tres secciones.

1. **Pantallas:** tarjetas con nombre, estado (sin vincular, en línea, desconectada), casilla de selección y botones *Vincular* / *Desvincular*. Botón *Agregar por IP*.
2. **Playlist:** lista con miniatura, nombre y duración editable por imagen; botones *subir/bajar* (sin arrastrar y soltar en el MVP), *quitar* y *Agregar imágenes*; campo de duración por defecto.
3. **Enviar:** botón *Enviar a pantallas seleccionadas* con progreso y resultado por pantalla.

**Receptor (Pantalla):**

- **Sin vincular:** pantalla negra con el código grande y el texto "Escribe este código en el Controlador".
- **Vinculada:** solo la imagen actual, a pantalla completa, fondo negro, `ContentScale.Fit`, transición de 500 ms entre imágenes. Si no hay playlist, texto discreto "Esperando contenido".
- Ventana sin bordes, a pantalla completa y siempre encima, cursor oculto mientras está bloqueada. `Esc` abre el diálogo de opciones de la Pantalla cuando está desbloqueada.

**Guía interactiva:** aparece en el primer arranque y acompaña la configuración
real de rol, playlist, vinculación, contraseña y envío. Se puede volver a abrir
desde Ajustes del Controlador o las opciones de la Pantalla.

**Una playlist única en el MVP.** El Emisor tiene una playlist y la envía a las pantallas que seleccione. Varias playlists por pantalla quedan para después.

---

## 12. Fases de trabajo y puertas de aprobación

Cada fase tiene: **objetivo**, **entregable**, **criterios de aceptación** y **frase de aprobación** que la persona debe escribir para que avances.

### Fase 0: Entorno y esqueleto
- **Objetivo:** proyecto compilando y mostrando una ventana.
- **Entregable:** estructura de carpetas de la sección 6, `libs.versions.toml`, `Main.kt` con argumentos, `AppContainer`, `Strings.kt`, logger a archivo.
- **Manual (persona):** instalar JDK 17+, generar el proyecto con el asistente de KMP (solo Desktop) y ponerlo en la raíz del repo. Además: instalar Git, crear el repositorio vacío en GitHub, hacer `git init`, `git remote add origin <url>` y dejar las credenciales de GitHub configuradas (por ejemplo con Git Credential Manager o `gh auth login`) para que `git push` funcione sin pedir datos. El agente debe pedir confirmación con `git remote -v` antes de seguir.
- **Entregable adicional:** `.gitignore` según la sección 1.1 y primer commit con push.
- **Aceptación:** `.\gradlew.bat run` abre una ventana vacía; los argumentos de la sección 10 se leen sin errores.
- **Aprobación:** `APRUEBO FASE 0`

### Fase 1: Rol y configuración
- **Entregable:** pantalla de selección de rol, `config.json`, persistencia, opción de cambiar rol.
- **Aceptación:** al reabrir, la app recuerda el rol; `--role` lo fuerza; los datos se guardan en `--data-dir`.
- **Aprobación:** `APRUEBO FASE 1`

### Fase 2: Reproducción local del carrusel (Receptor, sin red)
- **Entregable:** `Playlist`, carga desde `playlist.json` y `media\`, `PlaybackStateHolder`, ventana a pantalla completa, transición, manejo de imágenes dañadas (se omiten con log).
- **Manual (persona):** colocar 3 imágenes de prueba en `C:\tmp\rx\media\` y un `playlist.json` de ejemplo (el agente entrega el contenido exacto).
- **Aceptación:** el carrusel gira en bucle con los tiempos indicados; si se cierra y reabre, continúa con lo guardado.
- **Aprobación:** `APRUEBO FASE 2`

### Fase 3: Descubrimiento y vinculación
- **Entregable:** `KtorReceiverServer` con `/info`, `/pair`, auth por headers; `JmDnsDiscovery`; `PairingManager` con tests (código, expiración, intentos); pantalla de código en el Receptor; lista de pantallas y vinculación en el Emisor; campo manual de IP; estado en línea/desconectada.
- **Manual (persona):** aceptar el aviso del Firewall de Windows (red privada).
- **Aceptación:** dos procesos (o dos PCs) se descubren; código correcto vincula; incorrecto no; tras 5 fallos se renueva el código; un segundo Emisor no puede vincular; reiniciar ambos mantiene la vinculación; dos Emisores no se ven entre sí.
- **Aprobación:** `APRUEBO FASE 3`

### Fase 4: Editor de playlist, envío y sincronización
- **Entregable:** editor de playlist en el Emisor, `SyncPlanner` con tests, rutas `/sync/state`, `/media`, `/playlist`, `/status`, progreso por pantalla, aplicación atómica de la playlist en el Receptor.
- **Aceptación:** al enviar, el Receptor reproduce lo nuevo; un segundo envío sin cambios no sube imágenes; cambiar una sola imagen sube solo esa; cortar la red a mitad conserva la playlist anterior; con la red apagada el Receptor sigue reproduciendo.
- **Aprobación:** `APRUEBO FASE 4`

### Fase 5: Pulido y documentación
- **Entregable:** manejo de errores con mensajes claros, `README.md` (qué es, descarga, guía de 3 pasos), `docs/ARCHITECTURE.md`, `docs/PROTOCOL.md`, `docs/SECURITY.md`, `docs/KNOWN_LIMITATIONS.md` y una carpeta `docs/decisions/` con notas cortas (por qué estrella y no P2P, por qué código de vinculación, por qué caché local y cómo se separa la persistencia local).
- **Aprobación:** `APRUEBO FASE 5`

### Fase 6: Empaquetado (siempre al final)
- **Entregable:** configuración de `nativeDistributions` (nombre, versión, ícono, módulos mínimos de la JVM), tarea de carpeta portable (`createDistributable`) y, si la persona tiene WiX, instalador (`packageExe`). Instrucciones de ejecución y de prueba en una PC limpia.
- **Manual (persona):** colocar `icon.ico` en `composeApp\src\desktopMain\resources\icon.ico`; instalar WiX Toolset si quiere instalador; probar en otra PC sin JDK.
- **Aceptación:** la carpeta portable abre sin JDK instalado; el instalador (si aplica) instala y abre; mide y reporta el consumo real de RAM de cada rol.
- **Nota honesta:** `jpackage` no genera un único `.exe` autocontenido; la opción portable es una carpeta (comprimible en `.zip`) con un `.exe` dentro.
- **Aprobación:** `APRUEBO FASE 6`

### Fases de arquitectura (autorizadas en la rama `codex/architecture-improvements`)

La persona autorizó ejecutar las fases de arquitectura consecutivamente. Las fases 7–17 están completadas; se conservaron los commits por fase en `codex/architecture-improvements`. El plan unificado define los detalles de las fases 13–18. En la fase 18, Linux y el runner hospedado de Windows (`windows-2025-vs2026`) pasaron build, tests y smoke checks; sigue pendiente la validación manual en Windows 11 real, incluida la experiencia de escritorio. No declares completa la validación de Windows hasta ejecutar `.\gradlew.bat build` y los recorridos manuales en Windows 11.

- **Fases 7–12, completadas:** arquitectura por capas ligeras, DataStore + Room 3 + archivos, `core.ui`/Material Kolor, MVI selectivo, migración y adaptación Desktop.
- **Fases 13–14, completadas:** arranque según rol y esquema Room dentro de `data/local/room`.
- **Fase 15, completada:** paquetes feature-first y adaptadores de persistencia junto a sus features.
- **Fase 16, completada:** tokens UI verificados y render test de ambos temas con contraste 4.5:1.
- **Fase 17, completada:** `AGENTS.md`, `docs/ARCHITECTURE.md`, `docs/DESIGN_SYSTEM.md`, `docs/KNOWN_LIMITATIONS.md` y ADR-004 coinciden con los paquetes y la persistencia vigentes.
- **Fase 18:** Linux verificado con build limpia y smoke check Receptor/Controlador por mDNS. El workflow hospedado `Architecture Windows validation` pasó `clean build` y smoke checks en la imagen `windows-2025-vs2026`, incluyendo ambos modos de ventana, DPAPI y migración, reproducción autenticada, hooks nativos y descubrimiento mDNS desde el Controlador. Falta el recorrido manual en un equipo Windows 11 real, especialmente validar el bloqueo efectivo de atajos/gestos y la experiencia de escritorio.

### Fases posteriores (solo con petición expresa)
Arranque automático con Windows, modo de bloqueo de teclas con frase de desbloqueo, cifrado/firma de peticiones, varias playlists, plantillas de fichas técnicas.

---

## 13. Fuera del alcance del MVP

No implementes: jerarquía en árbol, video, programación por horarios, plantillas de fichas, actualización automática de la app, Android/iOS/Linux, internacionalización completa, temas personalizables, cuentas de usuario ni nada que requiera internet. DataStore y Room forman parte de la arquitectura de persistencia aprobada.

---

## 14. Formato obligatorio del reporte al terminar cada fase

```
## Reporte de la Fase N: <nombre>

**Qué hice:** (2 a 4 líneas)

**Archivos creados o modificados:**
- ruta\archivo.kt: para qué sirve

**Cómo probarlo (PowerShell):**
1. comando
2. qué debería verse

**Criterios de aceptación:**
- [x] criterio cumplido
- [ ] criterio pendiente (con motivo)

**Acciones manuales para ti:**
- Ruta exacta y contenido (o "ninguna")

**Decisiones que tomé y por qué:** (breve)

**Sugerencias fuera de alcance:** (solo mencionarlas, no implementarlas)

**Commit y push:** `<hash corto>` `feat(faseN): ...` subido a `origin/<rama>` (confirmado)

¿Apruebas la Fase N? Escribe "APRUEBO FASE N" para continuar o dime qué cambiar.
```

Cuando exista una autorización expresa para ejecutar varias fases consecutivamente, presenta el mismo reporte sin la pregunta de aprobación final y continúa con la siguiente fase. Si la persona pide cambios, aplícalos, vuelve a reportar con el mismo formato y espera de nuevo la frase de aprobación.
