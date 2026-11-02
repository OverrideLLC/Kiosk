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

La playlist y sus metadatos se guardan en Room. Al importar instalaciones
anteriores, la app conserva una copia de los JSON en `legacy/` y después elimina
los archivos de origen. El arranque normal no limpia los datos de la Pantalla.

El envío es incremental: el Controlador compara hashes con `GET /sync/state` y
solo sube lo que falta o cambió.

## Consecuencias

- La reproducción no depende de la red ni del Controlador encendido y puede
  continuar después de reiniciar la Pantalla.
- El envío es rápido en actualizaciones pequeñas y no reenvía todo.
- Hay que reservar espacio en disco para las imágenes (hasta 25 MB por imagen).
- La copia local puede quedar desactualizada si el Controlador no vuelve a enviar;
  el estado se muestra en el Controlador.
- La Pantalla reproduce la copia local después de un reinicio y sin red. Solo una
  acción explícita de reinicio o cambio de rol la borra.
