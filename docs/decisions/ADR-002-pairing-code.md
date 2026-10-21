# ADR-002: Vinculación por código de 6 dígitos

## Contexto

Hay que autorizar a un Controlador para que hable con una Pantalla sin cuentas,
sin internet y sin que el usuario configure certificados ni edite archivos. Las
opciones eran:

- Código numérico que se muestra en la Pantalla y se escribe en el Controlador.
- Botón físico en la Pantalla.
- Aprobar desde una pantalla táctil.
- Intercambio manual de un archivo o token.

## Decisión

Código de 6 dígitos, visible solo en la Pantalla sin vincular, válido 5 minutos,
máximo 5 intentos fallidos y 30 segundos de bloqueo al agotarlos. Al acertar, la
Pantalla genera un token de 32 bytes y lo entrega una única vez; el Controlador lo
guarda asociado al `deviceId`.

## Consecuencias

- El código **nunca viaja por la red** en el flujo normal: lo escribe una persona
  que está viendo la pantalla.
- El token evita repetir el código en cada envío y permite revocar la vinculación.
- El código es débil contra fuerza bruta; la ventana y el límite de intentos son
  parte de la decisión, no un extra. Ver [SECURITY.md](../SECURITY.md).
- Sin pantalla física (por ejemplo, un equipo sin monitor) no se puede vincular;
  es aceptable para el caso de uso.
- La vinculación se guarda por `deviceId`, así que un cambio de IP no la rompe.
