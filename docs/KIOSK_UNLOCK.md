# Desbloqueo de una Pantalla (modo kiosco)

Una Pantalla se bloquea sola al arrancar y no responde a nada del teclado ni del
mouse/touchpad salvo la secuencia de desbloqueo. El Controlador, cuando proyecta,
también se bloquea con la misma contraseña.

## Secuencia de desbloqueo

La secuencia se compone con tres teclas: **Shift**, **Ctrl** y **Alt**, en el
orden que se quiera, entre 2 y 12 teclas.

Secuencia por defecto:

```
Shift + Shift + Ctrl + Ctrl + Alt + Shift
```

- **Nunca se muestra en la pantalla bloqueada.** Solo aparece en este documento
  y en el botón **Seguridad** del Controlador.
- Se puede cambiar desde el Controlador, en **Seguridad → Secuencia de
  desbloqueo**: se agregan las teclas con los botones Shift / Ctrl / Alt y se
  guarda. El cambio se envía a todas las pantallas vinculadas.
- La misma secuencia se usa **con y sin contraseña**.
- Si la pantalla **no tiene contraseña** (por ejemplo, recién instalada, en
  blanco), completar la secuencia la desbloquea directamente.
- Si la pantalla **tiene contraseña**, primero se escribe la secuencia y en
  seguida la contraseña.

## Contraseña propia

El Controlador define una contraseña con el botón **Seguridad** y la envía a las
pantallas vinculadas.

- Entre 4 y 64 caracteres, solo letras (A-Z, a-z) y números (0-9).
- Solo se guarda y se envía su hash SHA-256 junto con su longitud; la
  contraseña en claro nunca sale del Controlador.

## Frase de recuperación

Se escribe tras la secuencia de desbloqueo y sirve para recuperar una pantalla
si se olvida la contraseña.

Por defecto está compilada dentro de la aplicación:

```
0137GenericPassword
```

### Cambiar la frase de recuperación

En **Seguridad → Frase de recuperación** el Controlador puede definir una frase
propia (entre 8 y 64 letras o números).

- Antes de cambiarla aparece un aviso con las consecuencias y hay que marcar una
  casilla de aceptación.
- Al guardar una frase propia, **la frase universal deja de funcionar**.
- Si se olvidan la contraseña **y** la frase personalizada, no hay forma de
  desbloquear esa PC ni las pantallas vinculadas: habría que borrar sus datos
  locales (playlist, imágenes y vinculación) y volver a configurarlas.
- La opción **Volver a la frase universal** restaura el comportamiento por
  defecto sin necesidad del aviso.

## Límite de intentos

Tras 5 intentos fallidos, el desbloqueo se rechaza durante 30 segundos.

## Mouse y touchpad

- En modo kiosco (Pantalla a pantalla completa o Controlador proyectando) se
  instala un hook `WH_MOUSE_LL`: se descartan movimiento, clics, rueda y los
  toques del touchpad. El puntero no puede alcanzar la barra de tareas, los bordes
  de la ventana ni otra aplicación. Mientras está bloqueado, el cursor se fija
  además en un píxel para que el primer movimiento no alcance a escapar.
- Al desbloquear con la secuencia, el hook libera el puntero para que la card
  **Opciones de Pantalla** se pueda usar con mouse o touchpad. Al pulsar
  **Volver a bloquear** o cerrar la card, el puntero se captura de nuevo.
- Cuando aparece un aviso de **actualización** en una Pantalla bloqueada, el
  mouse se libera temporalmente para poder pulsar **Actualizar ahora** o
  **Ahora no**; el teclado sigue bloqueado. Al cerrar el aviso, el mouse se
  captura otra vez. Esta corrección está en la 1.1.4.
- La ventana de la Pantalla no es redimensionable y, si Windows la minimiza o la
  restaura por un evento externo, vuelve a pantalla completa mientras el bloqueo
  siga activo.
- En Linux (sin hooks nativos) el mouse no se bloquea, para poder usar la
  interfaz y cerrar la app.

## Límites conocidos

- `Ctrl + Alt + Supr` no se puede interceptar (limitación del sistema).
- Los gestos de precisión de tres o cuatro dedos que maneja Windows directamente
  pueden escapar del hook de mouse. Si el equipo queda expuesto al público,
  desactívalos en la configuración del touchpad.
- En Linux (sin hooks nativos) el bloqueo es "blando": algunos atajos del sistema
  (Alt+Tab, tecla Windows) pueden escapar y el mouse sigue activo. En Windows, los
  enganches nativos bloquean teclado y mouse.
