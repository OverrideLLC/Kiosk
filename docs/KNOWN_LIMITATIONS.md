# Límites conocidos

Lista honesta de lo que el MVP **no** hace o hace con limitaciones. Úsala antes de
prometer funciones o de instalar la app en un comercio real.

## Plataforma y empaquetado

- **Windows 10/11 es la plataforma objetivo.** Esta versión se compiló y se
  ejecutó en Windows 11 con JDK 21; los hooks nativos de teclado y mouse se
  instalan correctamente, pero el bloqueo efectivo de atajos y gestos del sistema
  requiere una prueba manual en el equipo destino.
- Para generar el instalador `.exe`/`.msi` el plugin de Compose descarga WiX 3.11
  automáticamente la primera vez; no hace falta instalarlo a mano. Sin conexión
  en esa primera compilación solo se genera la carpeta portable.
- `jpackage` **no** produce un único `.exe` autocontenido: la versión portable es
  una carpeta con un `.exe` dentro, comprimible en `.zip`.
- No hay actualización automática ni instalador silencioso.
- No hay versión para Android, iOS, macOS ni Linux empaquetada.

## Pantalla y reproducción

- **La Pantalla se limpia al arrancar.** Por diseño, cada inicio borra contenido,
  bloqueo y vinculación para no reproducir material viejo. Consecuencia: tras un
  corte de luz o un reinicio, la pantalla queda en negro esperando que el
  Controlador la vuelva a vincular y enviar. La reproducción sin red funciona
  **mientras el proceso sigue vivo**, no después de reiniciar.
- Solo hay **una playlist activa** por Controlador. No hay varias playlists por
  pantalla ni programación por horarios.
- No hay video, audio, PDF ni animaciones. Solo JPG, PNG y WEBP de hasta 25 MB.
- La transición es un fundido de 500 ms. No hay efectos configurionales.
- Las imágenes dañadas o ausentes se omiten con un log; si todas fallan, la
  Pantalla muestra el estado vacío.
- El carrusel no recuerda en qué imagen iba tras reiniciar; empieza de nuevo (y,
  de hecho, arranca en blanco por la limpieza anterior).
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

- El tema claro/oscuro y el movimiento reducido se guardan en `config.json`.
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

- Sin base de datos: archivos JSON y una carpeta de imágenes.
- Sin copia de seguridad automática. Si se borra `%APPDATA%\OverrideKiosk\`, se pierde
  la vinculación y la playlist local.
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
- Actualización automática.
- Cuentas de usuario.
