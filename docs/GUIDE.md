# Guía interactiva

La guía interactiva recorre las pantallas reales de Override Kiosk hasta dejar una
demostración funcionando. No es un manual ni una presentación aparte: la persona
usa los mismos botones, listas y diálogos que usará después, y la guía avanza
cuando el estado real de la app confirma que el paso terminó.

## Cuándo aparece

- Se abre sola en el **primer arranque normal**, cuando todavía no existe
  una configuración de rol en DataStore.
- No se abre en los arranques con `--role=emitter` o `--role=receiver`, para no
  interferir con pruebas automatizadas o con los dos procesos de desarrollo.
- Se puede volver a abrir cuando se quiera:
  - desde la pantalla de selección de rol;
  - en el **Controlador**, desde **Ajustes → Guía interactiva**;
  - en la **Pantalla**, desde **Opciones → Ver guía**, con la Pantalla
    desbloqueada.

La guía no crea archivos nuevos ni usa la red: solo observa el estado que ya
producen el descubrimiento, la vinculación, el bloqueo y la sincronización.

## Recorrido del Controlador

| Paso | Qué muestra | Cuándo avanza |
| --- | --- | --- |
| 1. Agrega imágenes | La playlist real, con el botón *Agregar imágenes*. | Cuando la playlist activa tiene al menos una imagen. |
| 2. Vincula una Pantalla | La lista de pantallas descubiertas y el campo de IP manual. | Cuando hay al menos una Pantalla vinculada. |
| 3. Crea la contraseña | El diálogo de contraseña de bloqueo que abre la vinculación. | Cuando el Controlador ya tiene una contraseña configurada. |
| 4. Envía la playlist | La pantalla seleccionada y el botón de envío. | Cuando el envío a las pantallas seleccionadas termina bien. |
| Demo lista | El resultado final. | — |

La guía incluye atajos para operar la pantalla real:

- **Agregar imágenes**: abre el selector de archivos del sistema.
- **Buscar pantallas**: vuelve a lanzar el descubrimiento mDNS.
- **Enviar ahora**: ejecuta el mismo envío de *Enviar a pantallas seleccionadas*
  cuando hay playlist y al menos una pantalla seleccionada.

En ventanas estrechas, la guía cambia entre las pestañas **Playlist** y
**Pantallas** para mostrar el panel que corresponde al paso. En ventanas anchas
ambos paneles ya están visibles y no hace falta cambiar de pestaña.

## Recorrido de la Pantalla

| Paso | Qué muestra | Cuándo avanza |
| --- | --- | --- |
| 1. Comparte el código | El código grande de 6 dígitos y la IP:puerto. | Cuando la Pantalla queda vinculada. |
| 2. Espera la contraseña | El estado de espera mientras el Controlador configura el bloqueo. | Cuando la Pantalla recibe la contraseña. |
| 3. Recibe el contenido | El carrusel o el estado de espera de contenido. | Cuando la playlist tiene imágenes. |
| Demo lista | El resultado final. | — |

Cuando la Pantalla recibe la contraseña, el bloqueo kiosco se activa y la guía se
cierra sola: un carrusel bloqueado no debe tener una tarjeta de ayuda encima. Para
volver a verla hay que desbloquear la Pantalla y abrir **Opciones → Ver guía**.

## Controles de la tarjeta

- **Anterior / Siguiente**: recorren los pasos manualmente si se quiere leer con
  calma. El avance automático sigue funcionando cuando cambia el estado real.
- **Saltar**: cierra la guía sin modificar nada.
- **Repetir**: vuelve al primer paso del rol actual. Es útil para demostrar el
  recorrido a otra persona.
- **Finalizar**: cierra la guía cuando la demo ya está lista.

La tarjeta se coloca en la parte inferior central, por encima de la barra de envío
del Controlador y de las acciones de la Pantalla, para no tapar los controles que
la persona necesita.

## Límites

- La guía está en español, como el resto de la interfaz.
- No reemplaza la configuración de red ni el aviso del Firewall de Windows.
- Si mDNS no encuentra la Pantalla, el paso de vinculación sigue disponible con
  **Agregar por IP**.
- La guía no introduce funciones nuevas en el protocolo: usa las rutas y el estado
  que ya existen.
- Como toda la app, no hace llamadas a internet ni envía telemetría.
