# KioskSync — Nota de continuidad

Fecha: 2026-10-21  
Rama: `master`  
Último commit de código observado: `e5d214b`  
Remoto: `https://github.com/Drinity/OverrideKiosk.git`

## Estado actual

El repositorio está limpio y sincronizado con `origin/master`. En el momento de
escribir esta nota no hay cambios de código pendientes de subir: el historial ya
incluye las fases de playlists con nombre, bloqueo de la Pantalla, pantalla
completa, rediseño visual, pruebas de integración y empaquetado.

Esta nota existe para dejar claro qué falta comprobar antes de considerar el
trabajo cerrado y para poder continuar desde otra computadora.

## Qué falta comprobar

1. **Bloqueo de la Pantalla**
   - Configurar una frase desde las opciones de la Pantalla.
   - Activar el bloqueo y pulsar **Bloquear ahora**.
   - Confirmar que aparece el overlay, que se consumen clics y teclas y que una
     frase incorrecta no desbloquea.
   - Confirmar que la frase correcta desbloquea y que al reiniciar la Pantalla
     con el bloqueo activo vuelve a aparecer bloqueada.
   - Mientras está bloqueada, probar `Esc`, `Ctrl+Shift+P` y `Alt+F4`: no deben
     cerrar ni desbloquear la app.
2. **Pantalla completa**
   - Probar en Windows 11, con escalado de pantalla y en un segundo monitor.
   - Revisar si se acepta que el Controlador conserve la barra de título nativa
     en pantalla completa o si se quiere ocultar por completo.
   - Comprobar que al cambiar de rol se vuelve correctamente a una ventana normal.
3. **Regresión de dos procesos**
   - Emisor y Pantalla en la misma PC con directorios separados.
   - Vincular, enviar la playlist activa y comprobar que la Pantalla reproduce.
   - Crear, renombrar, seleccionar y eliminar playlists en el Controlador.
   - Verificar que un segundo envío sin cambios no vuelve a subir imágenes.
   - Cortar la red a mitad de una sincronización y comprobar que la Pantalla
     conserva la playlist anterior y sigue reproduciendo sin red.
4. **Empaquetado**
   - [x] Build final y carpeta portable generados.
   - [x] Instaladores `.exe` y `.msi` generados; el plugin descarga WiX solo.
   - [ ] Probar la carpeta portable en una PC limpia sin JDK.
   - [x] RAM medida en Windows 11: Controlador ~241 MB, Pantalla ~231 MB
     (working set, sin imágenes cargadas).
5. **Publicación**
   - Todavía no hay una versión publicada en GitHub Releases.
   - El `README.md` explica que, mientras tanto, se debe usar una carpeta
     portable o un instalador generado localmente.

## Cómo continuar en otra computadora

Requisitos: JDK 17 o superior y Git. La primera compilación necesita internet
para descargar Gradle y las dependencias; la app ya compilada funciona offline.

```powershell
git clone https://github.com/Drinity/OverrideKiosk.git
cd OverrideKiosk
git pull origin master
.\gradlew.bat build
```

Para probar los dos roles en la misma PC:

```powershell
# Terminal 1: Pantalla
.\gradlew.bat run --args="--role=receiver --data-dir=C:\tmp\rx --windowed"

# Terminal 2: Controlador
.\gradlew.bat run --args="--role=emitter --data-dir=C:\tmp\tx"
```

Si el descubrimiento automático no encuentra la Pantalla local, usar
**Agregar por IP** con `127.0.0.1:8765`.

## Documentación útil

- `README.md`: qué es, requisitos, compilación y primeros pasos.
- `docs/ARCHITECTURE.md`: arquitectura y responsabilidades.
- `docs/PROTOCOL.md`: rutas y protocolo de red.
- `docs/SECURITY.md`: modelo de seguridad y límites aceptados.
- `docs/KNOWN_LIMITATIONS.md`: limitaciones conocidas.
- `docs/KIOSK_UNLOCK.md`: funcionamiento del desbloqueo.
- `docs/PACKAGING.md`: carpeta portable e instalador.
- `docs/DESIGN_SYSTEM.md`: componentes y tokens visuales.
- `docs/decisions/`: decisiones de arquitectura.

## Notas importantes

- Los datos reales viven en `%APPDATA%\KioskSync\`: `config.json`,
  `paired.json`, `playlist.json`, `playlists.json`, `media\` y `logs\`.
- Los directorios de prueba (`C:\tmp\rx`, `C:\tmp\tx` y los temporales usados
  durante el desarrollo) no se suben al repositorio.
- El token de vinculación y la frase de desbloqueo no deben subirse a Git.
- El bloqueo es a nivel de la aplicación: no puede impedir `Ctrl+Alt+Del`,
  `Alt+Tab`, la tecla Windows ni el Administrador de tareas.
- No se deben agregar dependencias nuevas sin consultarlo.
- No iniciar una fase nueva sin la aprobación correspondiente.
