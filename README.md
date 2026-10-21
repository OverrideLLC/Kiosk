# KioskSync

Carrusel de imágenes para el comercio, **sin internet, sin cuentas y sin suscripción**.
Una computadora con Windows administra el contenido; una o varias pantallas de la
misma red local lo reciben y lo reproducen a pantalla completa.

- **Controlador**: descubre pantallas, las vincula, edita la playlist y envía el
  contenido. No abre puertos.
- **Pantalla**: se anuncia en la red, recibe el contenido y lo reproduce en bucle.
  Funciona sin red usando su copia local.

Todo ocurre en la LAN. La app no hace ninguna llamada a internet y no envía
telemetría.

## Los tres pasos

1. **Controlador**: abre la app, elige *Controlador*, agrega imágenes a la playlist
   y ajusta la duración de cada una.
2. **Pantalla**: abre la app en el otro equipo, elige *Pantalla* y anota el código
   de 6 dígitos que aparece en negro.
3. **Vincular y enviar**: en el Controlador elige la pantalla, pulsa *Vincular*,
   escribe el código y por último *Enviar a pantallas seleccionadas*.

Las próximas veces basta con abrir la app: la pantalla se anuncia sola y el
Controlador la vuelve a encontrar aunque cambie de IP. Si la red no permite mDNS,
usa **Agregar por IP** (`192.168.1.40:8765`).

## Requisitos

- Windows 10 u 11 (incluida la edición Home) para el uso normal.
- Para compilar: JDK 17 o superior y conexión a internet la primera vez (solo para
  descargar Gradle y las dependencias).
- La app compilada **no** necesita JDK ni internet.

## Descarga

Todavía no hay una versión publicada en GitHub Releases. Mientras tanto:

- **Usuarios finales**: pide la carpeta portable `KioskSync` (o el instalador
  `.msi`/`.exe`) generada con las instrucciones de
  [Empaquetado para Windows](docs/PACKAGING.md). No hace falta JDK ni internet.
- **Desarrollo**: clona este repositorio y compílalo con Gradle (siguiente
  sección).

La carpeta portable es la opción más simple y no requiere permisos de
administrador. El instalador solo agrega accesos directos y desinstalación.

## Compilar y ejecutar

En PowerShell, desde la raíz del repositorio:

```powershell
.\gradlew.bat run
```

Para probar los dos roles en la misma PC, con dos procesos:

```powershell
.\gradlew.bat run --args="--role=receiver --data-dir=C:\tmp\rx --windowed"
.\gradlew.bat run --args="--role=emitter --data-dir=C:\tmp\tx"
```

En el Controlador usa **Agregar por IP** con `127.0.0.1:8765` si el descubrimiento
automático no aparece en la misma máquina.

### Argumentos

| Argumento | Efecto |
| --- | --- |
| `--data-dir=<ruta>` | Carpeta de datos alternativa. |
| `--role=emitter\|receiver` | Fuerza el rol y omite la selección. |
| `--port=<n>` | Puerto inicial de la Pantalla (por defecto 8765). |
| `--windowed` | Pantalla en ventana, para depurar. |
| `--ui-catalog` | Catálogo interno de componentes (desarrollo). |

Los datos viven en `%APPDATA%\KioskSync\`:

```
config.json      rol, id del equipo, nombre, puerto, tema
paired.json      vinculación (token) o lista de pantallas del Controlador
playlist.json    playlist vigente
media\           imágenes
logs\app.log     registro con rotación
```

## Documentación

- [Arquitectura](docs/ARCHITECTURE.md)
- [Protocolo de red](docs/PROTOCOL.md)
- [Seguridad y riesgos aceptados](docs/SECURITY.md)
- [Límites conocidos](docs/KNOWN_LIMITATIONS.md)
- [Empaquetado para Windows](docs/PACKAGING.md)
- [Sistema de diseño](docs/DESIGN_SYSTEM.md)
- [Bloqueo y desbloqueo](docs/KIOSK_UNLOCK.md)
- [Decisiones](docs/decisions/)

## Estado

MVP funcional: vinculación por código, sincronización incremental, reproducción
local, bloqueo tipo kiosco, tema claro/oscuro y empaquetado para Windows. Consulta
los límites conocidos antes de usarlo en producción.

## Licencia

El proyecto se distribuye como software libre. La licencia definitiva (MIT o
Apache-2.0) se confirmará antes de publicar la primera versión; hasta entonces no
hay un archivo `LICENSE` en el repositorio.
