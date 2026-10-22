# Override Kiosk

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
   de 6 dígitos que aparece en negro. La misma pantalla muestra su **IP:puerto**
   para vincularla sin depender del descubrimiento automático.
3. **Vincular y enviar**: en el Controlador elige la pantalla, pulsa *Vincular*,
   escribe el código y por último *Enviar a pantallas seleccionadas*.
4. **Sincronizar (opcional)**: pulsa *Sincronizar* para reiniciar el carrusel en
   todas las pantallas vinculadas y que empiecen casi al mismo tiempo.

Las próximas veces basta con abrir la app: la pantalla se anuncia sola y el
Controlador la vuelve a encontrar aunque cambie de IP. Si la red no permite mDNS,
usa **Agregar por IP** (`192.168.1.40:8765`).

## Actualizaciones

El Controlador empaquetado puede enviar su propia versión a las Pantallas
vinculadas, por la LAN y sin internet. La Pantalla muestra el aviso; al aceptar se
reinicia con la versión nueva y conserva su vinculación y su playlist.

- Misma versión `major` y Pantalla más vieja: actualización opcional.
- Versión `major` distinta: actualización obligatoria.
- Una ejecución con `.\gradlew.bat run` no puede enviar ni recibir actualizaciones:
  hacen falta las versiones empaquetadas.

Los detalles, límites y el procedimiento para publicar una versión están en
[Actualizaciones](docs/UPDATES.md).

## Requisitos

- Windows 10 u 11 (incluida la edición Home) para el uso normal.
- Para compilar: JDK 17 o superior y conexión a internet la primera vez (solo para
  descargar Gradle y las dependencias).
- La app compilada **no** necesita JDK ni internet.

## Descarga

La versión **1.1.0** se publica en
[GitHub Releases](https://github.com/Drinity/OverrideKiosk/releases) con tres
variantes para Windows 10/11:

- `OverrideKiosk-1.1.0.exe`: instalador único.
- `OverrideKiosk-1.1.0.msi`: instalador MSI para despliegue.
- `OverrideKiosk-1.1.0-portable.zip`: carpeta portable; descomprime y ejecuta
  `OverrideKiosk.exe`.

No hace falta JDK ni internet para usar ninguna de las tres. La carpeta portable
es la opción más simple y no requiere permisos de administrador; el instalador
solo agrega accesos directos y desinstalación. Si compilas desde el código, sigue
la sección siguiente.

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

Los datos viven en `%APPDATA%\OverrideKiosk\`:

```
config.json      rol, id del equipo, nombre, puerto y tema
paired.json      vinculación: token de la Pantalla o pantallas del Controlador
playlists.json   biblioteca de playlists del Controlador
playlist.json    playlist vigente de la Pantalla
lock.json        contraseña (hash) y secuencia de desbloqueo
updates\         paquetes de actualización
media\           imágenes
logs\app.log     registro con rotación
logs\update.log  detalle del instalador de actualizaciones
```

La Pantalla borra su contenido, su bloqueo y su vinculación al arrancar, para no
reproducir material viejo. La excepción es el primer arranque después de una
actualización, que conserva todo. Consulta
[Límites conocidos](docs/KNOWN_LIMITATIONS.md).

## Documentación

- [Arquitectura](docs/ARCHITECTURE.md)
- [Protocolo de red](docs/PROTOCOL.md)
- [Seguridad y riesgos aceptados](docs/SECURITY.md)
- [Límites conocidos](docs/KNOWN_LIMITATIONS.md)
- [Empaquetado para Windows](docs/PACKAGING.md)
- [Sistema de diseño](docs/DESIGN_SYSTEM.md)
- [Bloqueo y desbloqueo](docs/KIOSK_UNLOCK.md)
- [Actualizaciones](docs/UPDATES.md)
- [Decisiones](docs/decisions/)

## Estado

MVP funcional: vinculación por código, sincronización incremental, reproducción
local, bloqueo tipo kiosco, tema claro/oscuro, empaquetado para Windows y
actualizaciones automáticas por LAN. Consulta los límites conocidos antes de
usarlo en producción.

## Licencia

Override Kiosk se distribuye bajo la licencia **MIT**. Consulta el archivo
[LICENSE](LICENSE) para el texto completo y el aviso de copyright.
