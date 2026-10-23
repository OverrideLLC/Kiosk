# Override Kiosk — Nota de continuidad

Fecha: 2026-10-23  
Rama: `master`  
Último commit de código: consulta `git log -1 --oneline`.
Remoto: `https://github.com/OverrideLLC/Kiosk.git`

## Estado actual

El repositorio está limpio y sincronizado con `origin/master`. La versión de
trabajo es **1.1.3** e incluye lo de 1.1.0/1.1.1/1.1.2 (playlists con nombre,
bloqueo de la Pantalla, guía interactiva, rediseño visual, actualizaciones
automáticas por LAN y pantalla completa de la Pantalla) más el arranque
**siempre fullscreen y sin bordes** para la selección de rol, el Controlador y la
Pantalla, y la página de licencia en el instalador.

Los documentos de actualizaciones, empaquetado, protocolo, límites y la
[guía](docs/GUIDE.md) describen las funciones; el logo oficial está normalizado,
el `icon.ico` se regeneró y el pie enlaza a `https://override.com.mx`. La licencia
es **MIT** (`LICENSE`) y el workflow `.github/workflows/release.yml` genera los
artefactos de Windows y la release al empujar un tag `v1.1.3`.

Esta nota existe para dejar claro qué falta comprobar antes de considerar el
trabajo cerrado y para poder continuar desde otra computadora.

## Qué falta comprobar

1. **Bloqueo de la Pantalla**
   - Configurar una frase desde las opciones de la Pantalla.
   - Activar el bloqueo y pulsar **Bloquear ahora**.
   - Confirmar que aparece el overlay, que se consumen clics y teclas y que una
     frase incorrecta no desbloquea.
   - Confirmar que la frase correcta desbloquea. Tras un reinicio normal la
     Pantalla se limpia por diseño; si se prueba el reinicio de actualización,
     debe conservar la vinculación y volver bloqueada.
   - Mientras está bloqueada, probar `Esc`, `Ctrl+Shift+P` y `Alt+F4`: no deben
     cerrar ni desbloquear la app.
2. **Pantalla completa siempre**
   - Probar en Windows 11, con escalado de pantalla y en un segundo monitor.
   - Confirmar que la selección de rol, el Controlador y la Pantalla abren sin
     barra de título, sin borde y cubriendo la barra de tareas.
   - Comprobar que al cambiar de rol la app sigue a pantalla completa y sin
     bordes, y que los diálogos modales (selector de imágenes) aparecen al frente.
   - Probar que al minimizar con un atajo de Windows la ventana vuelve sola a
     pantalla completa, salvo mientras el Controlador está proyectando.
3. **Regresión de dos procesos**
   - Emisor y Pantalla en la misma PC con directorios separados.
   - Vincular, enviar la playlist activa y comprobar que la Pantalla reproduce.
   - Crear, renombrar, seleccionar y eliminar playlists en el Controlador.
   - Verificar que un segundo envío sin cambios no vuelve a subir imágenes.
   - Cortar la red a mitad de una sincronización y comprobar que la Pantalla
     conserva la playlist anterior y sigue reproduciendo sin red.
4. **Actualización automática**
   - Instalar o descomprimir una versión anterior en la Pantalla y la 1.1.3 en
     el Controlador.
   - Vincular ambas y comprobar que el Controlador detecta la versión vieja,
     envía el paquete y la Pantalla muestra el aviso.
   - Aceptar la actualización y confirmar que la Pantalla se reinicia, conserva la
     vinculación y la playlist y reporta `appVersion` 1.1.3.
   - Probar **Ahora no** con una actualización opcional y el botón **Actualizar**
     de la tarjeta.
   - Si se puede, probar una diferencia de `major` para verificar la actualización
     obligatoria.
   - Revisar `logs\update.log` si el reemplazo falla.
5. **Empaquetado**
   - [x] Build final y carpeta portable generados.
   - [x] Instaladores `.exe` y `.msi` generados; el plugin descarga WiX solo.
   - [ ] Probar la carpeta portable en una PC limpia sin JDK.
   - [x] RAM medida en Windows 11: Controlador ~241 MB, Pantalla ~231 MB
     (working set, sin imágenes cargadas).
   - [ ] Artefactos de la versión 1.1.3 generados por GitHub Actions.
   - [ ] Probar una actualización desde 1.1.2.
6. **Publicación**
   - Licencia MIT añadida en `LICENSE` y en el `README.md`.
   - [x] Release `v1.1.2` publicada:
     https://github.com/OverrideLLC/Kiosk/releases/tag/v1.1.2
   - [x] Release anterior `v1.1.1` (2026-10-23):
     https://github.com/OverrideLLC/Kiosk/releases/tag/v1.1.1
   - [x] Release anterior `v1.1.0` (2026-10-22):
     https://github.com/OverrideLLC/Kiosk/releases/tag/v1.1.0
   - [ ] Release `v1.1.3` pendiente de etiquetar y publicar.
   - El `README.md` apunta a GitHub Releases para la descarga.

## Cómo continuar en otra computadora

Requisitos: JDK 17 o superior y Git. La primera compilación necesita internet
para descargar Gradle y las dependencias; la app ya compilada funciona offline.

```powershell
git clone https://github.com/OverrideLLC/Kiosk.git
cd OverrideKiosk
git pull origin master
.\gradlew.bat build
```

Para probar los dos roles en la misma PC:

```powershell
# Terminal 1: Pantalla
.\gradlew.bat run --args="--role=receiver --data-dir=C:\tmp\rx"

# Terminal 2: Controlador
.\gradlew.bat run --args="--role=emitter --data-dir=C:\tmp\tx"
```

Si el descubrimiento automático no encuentra la Pantalla local, usar
**Agregar por IP** con `127.0.0.1:8765`.

> Las ejecuciones con `gradlew run` no pueden enviar ni recibir actualizaciones.
> Para probar esa función hacen falta las versiones empaquetadas.

## Documentación útil

- `README.md`: qué es, requisitos, compilación y primeros pasos.
- `docs/ARCHITECTURE.md`: arquitectura y responsabilidades.
- `docs/PROTOCOL.md`: rutas y protocolo de red.
- `docs/SECURITY.md`: modelo de seguridad y límites aceptados.
- `docs/KNOWN_LIMITATIONS.md`: limitaciones conocidas.
- `docs/KIOSK_UNLOCK.md`: funcionamiento del desbloqueo.
- `docs/UPDATES.md`: actualizaciones por LAN y cómo publicar una versión.
- `docs/PACKAGING.md`: carpeta portable, instalador y versionado.
- `docs/DESIGN_SYSTEM.md`: componentes y tokens visuales.
- `docs/icons.md`: íconos de Material y logo oficial.
- `docs/decisions/`: decisiones de arquitectura.

## Notas importantes

- Los datos reales viven en `%APPDATA%\OverrideKiosk\`: `config.json`,
  `paired.json`, `playlist.json`, `playlists.json`, `lock.json`, `media\`,
  `updates\` y `logs\`.
- Los directorios de prueba (`C:\tmp\rx`, `C:\tmp\tx` y los temporales usados
  durante el desarrollo) no se suben al repositorio.
- El token de vinculación y la frase de desbloqueo no deben subirse a Git.
- El bloqueo es a nivel de la aplicación: no puede impedir `Ctrl+Alt+Del`,
  `Alt+Tab`, la tecla Windows ni el Administrador de tareas.
- No se deben agregar dependencias nuevas sin consultarlo.
- No iniciar una fase nueva sin la aprobación correspondiente.
