# Límites conocidos

Lista honesta de lo que el MVP **no** hace o hace con limitaciones. Úsala antes de
prometer funciones o de instalar la app en un comercio real.

> **Estado:** DataStore, Room, migración JSON, repositorios asíncronos, MVI
> ligero, rutas separadas y composición Desktop están aplicados en
> `codex/architecture-improvements`.

## Plataforma y empaquetado

- **Windows 10/11 es la plataforma objetivo.** Esta revisión se compiló y ejecutó
  en Linux con JDK 21, incluidos los recorridos de arranque de ambos roles. No se
  volvió a ejecutar esta rama en Windows 11; los hooks nativos de teclado y mouse,
  DPAPI y el empaquetado requieren validación en Windows antes de distribuirla.
- Para generar el instalador `.exe`/`.msi` el plugin de Compose descarga WiX 3.11
  automáticamente la primera vez; no hace falta instalarlo a mano. Sin conexión
  en esa primera compilación solo se genera la carpeta portable.
- `jpackage` **no** produce un único `.exe` autocontenido: la versión portable es
  una carpeta con un `.exe` dentro, comprimible en `.zip`.
- El instalador no tiene modo silencioso.
- No hay versión para Android, iOS, macOS ni Linux empaquetada.

## Actualizaciones

- Solo funcionan entre **versiones empaquetadas**. Una ejecución con
  `.\gradlew.bat run` no tiene un `app\` que enviar ni puede aplicarlo.
- Solo se reemplaza la carpeta `app\`: no cambian el runtime de Java ni el
  `OverrideKiosk.exe`. No hay actualización delta; viaja el payload completo.
- El paquete **no está firmado**. La protección es el token de vinculación, el
  SHA-256 y la validación del ZIP, no una firma criptográfica.
- No hay rollback ni copia de seguridad automática de la versión anterior.
- El reemplazo usa PowerShell con `-ExecutionPolicy Bypass` y `robocopy`. Una
  política de grupo o un antivirus puede bloquearlo; el detalle queda en
  `logs\update.log`.
- La carpeta de instalación debe ser escribible por el usuario. La instalación por
  usuario y la portable normal cumplen; una portable dentro de `C:\Program Files`
  puede fallar.
- Si los `major` son distintos, la actualización es obligatoria y no se puede
  posponer. Si solo cambia `minor`/`patch`, la Pantalla puede elegir **Ahora no** y
  no se le vuelve a insistir con esa versión (salvo que se pulse **Actualizar** a
  mano).
- La Pantalla debe estar encendida y en línea para recibir el paquete. No hay
  actualización por USB ni al arrancar.
- El reinicio posterior a una actualización conserva vinculación, playlist, medios
  y bloqueo. El arranque normal también conserva esos datos; solo se eliminan
  mediante un cambio de rol o una acción explícita de reinicio.

## Pantalla y reproducción

- La Pantalla conserva su vinculación, playlist, medios y bloqueo al reiniciar.
  Sigue reproduciendo la copia local aunque no haya red.
- Solo hay **una playlist activa** por Controlador. No hay varias playlists por
  pantalla ni programación por horarios.
- No hay video, audio, PDF ni animaciones. Solo JPG, PNG y WEBP de hasta 25 MB.
- La transición es un fundido de 500 ms. No hay efectos configurionales.
- Las imágenes dañadas o ausentes se omiten con un log; si todas fallan, la
  Pantalla muestra el estado vacío.
- El carrusel no recuerda en qué imagen iba tras reiniciar; comienza desde la
  primera imagen de la playlist activa.
- **La sincronización de reproducción es de mejor esfuerzo.** El Controlador envía
  un retardo común de 2 segundos y cada Pantalla reinicia su carrusel al vencerlo.
  No se sincronizan los relojes ni se corrige la latencia de la red o el tiempo de
  decodificación de la primera imagen, así que puede quedar una diferencia visible
  entre pantallas. La Pantalla debe tener una playlist activa; si está esperando
  contenido, responde `409` y el Controlador lo cuenta como fallo.

## Bloqueo kiosco

- `Ctrl+Alt+Del` no se puede interceptar.
- Los hooks de teclado (`WH_KEYBOARD_LL`) y de mouse (`WH_MOUSE_LL`) solo existen
  en Windows y no están probados en Linux.
- El bloqueo de mouse/touchpad descarta movimiento, clics, rueda y los toques que
  Windows entrega como eventos de mouse. Los gestos de precisión de tres o cuatro
  dedos que maneja el shell directamente pueden escapar; para un kiosco estricto,
  desactiva esos gestos en la configuración del touchpad o usa una cuenta de
  Windows restringida.
- Al desbloquear, el puntero se libera para poder usar la card de opciones; solo
  vuelve a quedar capturado cuando se cierra la card o se elige **Volver a
  bloquear**.
- En `--windowed` o en Linux, el bloqueo usa el manejo de teclas de Compose y no
  bloquea atajos del sistema ni el mouse; es solo para depuración.
- Si Windows minimiza o restaura la ventana por un evento externo, un vigilante
  la devuelve a pantalla completa mientras el bloqueo siga activo. Eso no impide
  `Ctrl+Alt+Del` ni el acceso físico al equipo.
- La frase de recuperación es universal y está compilada en la app; no es un
  secreto.
- La contraseña se guarda con SHA-256 sin sal ni derivación lenta.
- No hay bloqueo por inactividad ni por horario; la Pantalla se bloquea al
  arrancar y al salir del diálogo de opciones.

## Red y descubrimiento

- mDNS puede fallar en redes con aislamiento de clientes, VLANs o firewalls
  estrictos. El plan B es **Agregar por IP**.
- El firewall de Windows debe permitir la app en la **red privada** la primera vez.
- Solo hay un Controlador vinculado por Pantalla a la vez.
- No hay reconexión cifrada, ni TLS, ni autenticación de mDNS.
- El Controlador consulta el estado cada 5 segundos; una pantalla puede tardar
  hasta ~15 segundos en marcarse como desconectada.
- La sincronización es secuencial por pantalla. Con muchas imágenes y varias
  pantallas, el envío puede tardar; no hay reanudación de una subida cortada (la
  siguiente ejecución vuelve a subir solo lo que falte).

## UI y accesibilidad

- El tema claro/oscuro y el movimiento reducido se guardan en DataStore; la
  configuración `config.json` anterior se conserva bajo `legacy/`.
- La Pantalla (pairing y reproducción) es siempre negra por diseño; el tema
  afecta al Controlador y a los diálogos.
- El catálogo de componentes (`--ui-catalog`) es una herramienta de desarrollo, no
  una pantalla de producto.
- La navegación por teclado cubre Tab/Enter/Esc y flechas en las tarjetas de rol,
  pero no se probó con todos los lectores de pantalla de Windows.
- No hay internacionalización completa: los textos están en español y
  centralizados en `Strings.kt` para traducirlos más adelante.
- No hay temas visuales personalizados ni perfiles de usuario.

## Datos y operación

- La configuración pequeña usa DataStore Preferences; playlists, emparejamientos,
  metadatos de medios y bloqueo usan Room 3. Los bytes de imagen siguen en archivos.
  La migración de JSON conserva una copia bajo `legacy/`.
- Sin copia de seguridad automática. Si se borra `%APPDATA%\OverrideKiosk\`, se pierde
  la vinculación y la playlist local.
- Los paquetes de actualización ocupan espacio en `updates\`: el Controlador guarda
  su payload comprimido y la Pantalla el paquete recibido hasta aplicarlo. Se
  limpian al reemplazarse por otro o al aplicarse, pero un paquete pendiente puede
  ocupar cientos de MB.
- Los datos de la versión anterior viven en `%APPDATA%\KioskSync\` y **no se migran**
  automáticamente. Tras actualizar, hay que volver a elegir rol, vincular y enviar
  la playlist. Si se necesita conservar algo, cópialo a mano antes de actualizar.
  La versión anterior también usa el servicio mDNS `_kiosksync._tcp`, así que una
  app vieja y una nueva **no se descubren entre sí**; actualiza ambos equipos.
- El log rota a 5 archivos de 1 MB. No incluye tokens ni códigos, pero sí nombres
  de equipos e ids.
- No hay telemetría, ni informes de error remotos, ni nube.
- La app no se cierra sola por una excepción: la registra y sigue. Si el error es
  fatal para una operación, la UI lo muestra; no hay recuperación automática de
  una playlist corrupta (se ignora y se muestra el estado vacío).

## Fuera del alcance actual

- Jerarquía en árbol de contenido.
- Plantillas de fichas técnicas.
- Varias playlists por pantalla.
- Programación por horarios.
- Arranque automático con Windows (se puede agregar con el Programador de tareas).
- Firma/cifrado de peticiones (diseñado, no implementado).
- Firma de paquetes de actualización y de binarios.
- Rollback automático a la versión anterior.
- Cuentas de usuario.
