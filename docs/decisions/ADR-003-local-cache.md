# ADR-003: Caché local en la Pantalla

## Contexto

La Pantalla debe seguir mostrando contenido si el Controlador se apaga, si se cae
el Wi-Fi o si la red se satura. Las alternativas eran:

- Reproducir directamente desde el Controlador (streaming).
- Guardar una copia local de las imágenes y la playlist.
- Usar un servicio de almacenamiento externo.

## Decisión

La Pantalla guarda en disco las imágenes recibidas (`media/`) y en Room la
playlist vigente, su orden y los metadatos de medios. La reproducción lee siempre
esa copia local y no consulta la red. El arranque normal conserva la copia; solo
una acción explícita de reinicio o cambio de rol puede borrarla.

Durante la migración de esta rama, el código previo todavía usa `playlist.json` y
limpia datos al arrancar. La fase 12 debe retirar ese comportamiento y actualizar
`KNOWN_LIMITATIONS.md`.

El envío es incremental: el Controlador compara hashes con `GET /sync/state` y
solo sube lo que falta o cambió.

## Consecuencias

- La reproducción no depende de la red ni del Controlador encendido y puede
  continuar después de reiniciar la Pantalla.
- El envío es rápido en actualizaciones pequeñas y no reenvía todo.
- Hay que reservar espacio en disco para las imágenes (hasta 25 MB por imagen).
- La copia local puede quedar desactualizada si el Controlador no vuelve a enviar;
  el estado se muestra en el Controlador.
- La Pantalla se limpia al arrancar por decisión de producto (no reproducir
  contenido viejo); la caché protege la sesión en curso, no el reinicio. Ver
  [KNOWN_LIMITATIONS.md](../KNOWN_LIMITATIONS.md).
