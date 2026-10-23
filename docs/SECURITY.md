# Seguridad

Este documento describe el modelo de seguridad real del MVP: qué protege, qué no
protege y qué riesgos se aceptan a conciencia. No es una guía de despliegue
hostil; es una herramienta para decidir dónde usarlo.

## Modelo de confianza

- La red local se considera **parcialmente confiable**. Cualquier equipo conectado
  a la misma LAN puede alcanzar el puerto de la Pantalla.
- El objetivo es evitar que alguien **sin el código de vinculación** use la Pantalla
  o cambie su contenido, y que un Controlador ajeno no pueda secuestrar una
  pantalla ya vinculada.
- No se protege contra acceso físico al equipo ni contra un administrador de la
  red.

## Controles implementados

| Control | Dónde |
| --- | --- |
| Código de vinculación de 6 dígitos, 5 minutos, 5 intentos y 30 s de bloqueo | `PairingManager` |
| Token aleatorio de 32 bytes (hex) entregado una sola vez | `POST /pair` |
| Headers `X-Emitter-Id` + `X-Auth-Token` en todas las rutas sensibles | `KtorReceiverServer` |
| Comparación del token en tiempo constante | `PairingManager.isAuthorized` |
| Verificación SHA-256 antes de guardar una imagen | `PUT /media/{id}` |
| Actualizaciones autenticadas con el mismo token y SHA-256 del paquete | `PUT /update/{version}` |
| Validación del ZIP de actualización (rutas, tamaño, manifiesto y payload) | `FileReceiverUpdateManager` |
| Validación de formato y tamaño (JPG/PNG/WEBP, 25 MB) | Servidor y cliente |
| La playlist solo se aplica si todas sus imágenes existen y coinciden | `PUT /playlist` |
| Un segundo Controlador no puede vincular una Pantalla ya vinculada | `PairRun.AlreadyPaired` |
| El Controlador no abre puertos ni se publica por mDNS | Diseño de roles |
| Bloqueo tipo kiosco con secuencia + contraseña, 5 intentos y 30 s | `KioskLockManager` |
| Bloqueo de teclado y mouse/touchpad con hooks de bajo nivel mientras el kiosco está bloqueado | `WindowsKioskKeyboardGuard`, `WindowsKioskMouseGuard` |
| Bloqueo de nuevas vinculaciones al alcanzar 5 fallos | `PairingManager` |
| Log local con rotación; no registra tokens ni códigos | `FileAppLogger` |
| Ninguna llamada a internet ni telemetría | Todo el código |

## Riesgos aceptados (MVP)

### 1. HTTP en texto plano

El tráfico viaja sin cifrar. Quien pueda capturarlo en la LAN puede leer:

- el token de un Controlador ya vinculado,
- el contenido de las imágenes y la playlist,
- el estado de reproducción.

Con el token, esa persona puede hacerse pasar por el Controlador. **Mitigación
actual:** red local de confianza y separación física del comercio. **Mejora
futura:** firmar cada petición con HMAC-SHA256 derivado del código de vinculación,
o cifrar el cuerpo con AES-GCM, sin cambiar las rutas.

### 2. El código de 6 dígitos es débil

Un código de 6 dígitos tiene 1 000 000 de combinaciones. Las defensas son la
ventana de 5 minutos y el límite de 5 intentos con 30 segundos de bloqueo. Aun así,
un atacante con acceso a la red puede intentar durante horas y, con suerte,
acertar. **No uses la app en una red pública o compartida con desconocidos.**

### 3. Frase de recuperación universal

La app incluye una **frase de recuperación universal compilada en el binario**
(documentada en [KIOSK_UNLOCK.md](KIOSK_UNLOCK.md)). Cualquiera que conozca el
código fuente o el documento puede desbloquear una Pantalla con contraseña. Es una
decisión de soporte para no dejar pantallas inutilizables, no un secreto.

**Consecuencia:** el bloqueo kiosco disuade al usuario casual, pero **no** protege
frente a alguien que leyó el repositorio. Si la Pantalla queda en un lugar público,
el bloqueo debe combinarse con seguridad física y de cuenta de Windows.

**Mejora futura:** frase de recuperación por instalación, guardada solo en el
Controlador (o en un sobre físico), sin valor universal.

### 4. Hash de contraseña rápido y sin sal

Las contraseñas de bloqueo y la recuperación se guardan como SHA-256 hex, sin sal
ni derivación lenta. Si alguien copia `lock.json`, puede probar contraseñas a gran
velocidad. **Mitigación actual:** la contraseña es un PIN corto de bloqueo local,
no una credencial de red. **Mejora futura:** PBKDF2/scrypt/Argon2 con sal aleatoria
y un identificador de algoritmo.

### 5. mDNS es informativo

Los anuncios mDNS no están autenticados. Un equipo de la LAN puede anunciar un
servicio falso con `role=receiver`. El Controlador valida `role` al vincular, pero
no puede demostrar que el anuncio es legítimo. La vinculación por código limita el
daño: un servicio falso necesita que el usuario escriba **su** código, y el token
solo sirve contra ese servicio falso.

### 6. Acceso físico y Windows

- `Ctrl+Alt+Del` no se puede interceptar; el usuario siempre puede llegar a la
  pantalla de seguridad de Windows.
- Los hooks de teclado y mouse son de bajo nivel y **solo existen en Windows**.
  Esta versión se verificó en Windows 11 (los hooks se instalan), pero el bloqueo
  efectivo de atajos y gestos del sistema requiere una prueba manual en el equipo
  destino. En Linux se usa un manejo de teclas de Compose, que
  no bloquea atajos del sistema ni el mouse.
- El hook de mouse descarta movimiento, clics, rueda y los toques del touchpad que
  Windows entrega como eventos de mouse. Los gestos de precisión de tres o cuatro
  dedos que maneja el shell pueden escapar; desactívalos en la configuración del
  touchpad si el equipo queda expuesto al público. Al desbloquear se libera el
  puntero solo para la card de opciones; al cerrarla se vuelve a capturar.
- La app no impide apagar el equipo, desconectar la red ni cerrar sesión.
- El bloqueo no sustituye una cuenta de Windows restringida ni una política de
  grupo.

### 7. Datos en disco

`config.json`, `paired.json` y `lock.json` están en
`%APPDATA%\OverrideKiosk\` con los permisos del usuario de Windows. Cualquier proceso
de ese usuario puede leerlos. El token de la Pantalla y el hash de la contraseña
están ahí; protégelos con los permisos de la cuenta.

### 8. Sin TLS y sin certificados

No hay forma de validar la identidad del servidor más allá del token. Un ataque de
hombre en el medio en la LAN puede leer o modificar el tráfico. La verificación
SHA-256 de las imágenes detecta modificaciones en el cuerpo, pero no en las
respuestas JSON ni en el token.

### 9. Actualizaciones sin firma

El paquete de actualización es un ZIP generado por el Controlador y **no está
firmado**. La Pantalla comprueba el token del Controlador y el SHA-256 que llega en
la misma petición, pero un atacante que pueda interceptar y reescribir el tráfico
—o que haya obtenido el token— puede enviar un paquete y su hash, y la Pantalla lo
aceptará. El paquete se aplica con un script de PowerShell que se ejecuta con
`-ExecutionPolicy Bypass` y reemplaza `app/` con `robocopy`.

**Mitigación actual:** red local de confianza, token, validación estructural del
ZIP, límites de tamaño y confirmación del usuario (salvo actualizaciones
obligatorias). **Mejora futura:** firmar el paquete con una clave pública embebida
en la app y verificar la firma antes de extraer, además de firmar los binarios con
un certificado de código.

## Qué **no** hace la app

- No cifra el tráfico.
- No autentica el anuncio mDNS.
- No firma los paquetes de actualización ni los binarios.
- No usa contraseñas de usuario ni cuentas.
- No registra telemetría ni envía datos fuera de la LAN.
- No impide capturas de pantalla ni grabación del contenido.
- No protege contra un administrador del equipo.

## Recomendaciones de despliegue

1. Usa una red local privada, no la red de invitados.
2. Mantén las pantallas en un lugar con acceso físico controlado.
3. Usa una cuenta de Windows sin privilegios de administrador para la Pantalla.
4. Cambia la secuencia de desbloqueo por defecto desde **Seguridad**.
5. Pon una contraseña de bloqueo si el equipo queda a la vista del público.
6. Revisa `logs\app.log` si una pantalla se comporta raro; no contiene tokens.
7. Si necesitas confidencialidad real, espera la capa de firma/cifrado opcional o
   usa una VPN/TLS de red.
8. Actualiza solo desde el Controlador del comercio y no vincules pantallas a
   Controladores desconocidos: un Controlador vinculado puede enviar código por la
   ruta de actualización.

## Reportar un problema

Este es un proyecto open source. Si encuentras una vulnerabilidad, descríbela con
pasos de reproducción y versión. No publiques tokens reales, contraseñas ni la
frase de recuperación en el reporte.
