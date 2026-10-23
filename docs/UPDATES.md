# Actualizaciones en la red local

El Controlador puede enviar **su propia versión** a las Pantallas vinculadas, sin
internet y sin copiar archivos a mano. La actualización viaja por la misma LAN y
usa el mismo token de vinculación que el resto de las rutas.

## Requisitos

- El Controlador y la Pantalla deben estar **empaquetados**: la carpeta portable o
  una instalación `.msi`/`.exe`. Una ejecución con `.\gradlew.bat run` no tiene un
  `app\` que enviar y la función queda deshabilitada.
- La Pantalla debe estar vinculada y en línea.
- La Pantalla debe poder escribir en su carpeta de instalación. La instalación por
  usuario y la carpeta portable cumplen; una carpeta portable dentro de
  `C:\Program Files` puede requerir permisos de administrador.
- Windows. El reemplazo de archivos usa PowerShell y `robocopy`.

## Qué se envía

El Controlador comprime **solo su carpeta `app\`** (jars, configuración del
lanzador y recursos) junto con un manifiesto `update.json`:

```json
{"version":"1.1.0","format":1}
```

No se envía el runtime de Java ni `OverrideKiosk.exe`; el reemplazo se hace sobre
el payload de la aplicación y el lanzador existente se conserva.

El paquete se genera una vez, se guarda en `<datos>\updates\` y se reutiliza. Los
paquetes viejos se borran automáticamente.

## Cuándo se ofrece

El Controlador lee `appVersion` en `GET /status` cada 5 segundos y aplica esta
política (`AppVersionPolicy`):

| Versión de la Pantalla | Resultado |
| --- | --- |
| Igual a la del Controlador | No se hace nada. |
| Menor, con el **mismo** `major` | Actualización **opcional**. La Pantalla puede posponerla. |
| Mayor, con el **mismo** `major` | No se degrada. La Pantalla es compatible. |
| Distinto `major`, en cualquier dirección | Actualización **obligatoria** hacia la versión del Controlador. |

Si la Pantalla pospone una actualización opcional, guarda la versión en
`skipped-update.txt` y la informa en `/info` y `/status`. El Controlador no vuelve
a insistir con esa versión, pero el botón **Actualizar** de la tarjeta permite
reintentarla a mano. Una actualización obligatoria no se puede posponer.

La comprobación es automática al descubrir la Pantalla y en cada consulta de
estado. No hay un botón de "buscar actualizaciones" separado.

## Qué ve el usuario

1. El Controlador muestra el progreso por pantalla: preparando, enviando,
   esperando confirmación y actualizando.
2. La Pantalla valida el paquete y muestra un aviso con **Actualizar ahora** o
   **Ahora no** (este último solo si la actualización es opcional). Desde la
   **1.1.4** el aviso acepta clics de mouse o pantalla táctil aunque la Pantalla
   esté bloqueada; el teclado sigue bloqueado por seguridad.
3. Al aceptar, la Pantalla cierra la aplicación, un ayudante de PowerShell espera
   a que el proceso termine, reemplaza `app\` y vuelve a abrir `OverrideKiosk.exe`.
4. El reinicio tras una actualización **conserva** la vinculación, la playlist, las
   imágenes y el bloqueo. En un arranque normal (sin actualización) la Pantalla
   sigue limpiándose a propósito; ver [KNOWN_LIMITATIONS.md](KNOWN_LIMITATIONS.md).

## Verificación y seguridad

- La subida exige `X-Emitter-Id` + `X-Auth-Token` y el header
  `X-Content-Sha256`.
- La Pantalla verifica el SHA-256 del cuerpo antes de aceptar el paquete.
- El ZIP se inspecciona antes de extraer: solo se aceptan `update.json` y entradas
  bajo `app/`, sin rutas absolutas, `..`, unidades ni caracteres peligrosos.
- Se limita el paquete a **512 MB** y el contenido extraído a **1 GB**.
- El manifiesto debe declarar la misma versión de la ruta y el formato soportado.
- Al fallar la validación, la Pantalla rechaza el paquete y conserva la versión
  actual.
- El paquete **no está firmado**. La protección es el token de la LAN y el hash;
  quien pueda interceptar y modificar el tráfico, o quien tenga el token, podría
  enviar código. Los riesgos están en [SECURITY.md](SECURITY.md).

## Cómo publicar una versión

1. Sube la versión en **dos lugares** y mantenlos iguales:
   - `composeApp/src/commonMain/kotlin/app/kiosksync/core/Constants.kt`
     (`APP_VERSION`).
   - `composeApp/build.gradle.kts` (`packageVersion`).
2. Ejecuta `.\gradlew.bat build`.
3. Genera la carpeta portable o el instalador con las instrucciones de
   [PACKAGING.md](PACKAGING.md), o empuja el tag `v<versión>` para que el workflow
   de GitHub Actions publique la release en Windows.
4. Instala o descomprime **el Controlador** con la versión nueva.
5. Abre el Controlador con las Pantallas encendidas. La actualización se envía
   sola; si alguna Pantalla estaba apagada, se actualizará cuando vuelva a
   aparecer.
6. Comprueba en la Pantalla que reinició y que sigue mostrando la misma playlist.

No hace falta actualizar primero la Pantalla: el Controlador es el que tiene el
paquete y el que inicia el envío.

## Actualizar una Pantalla que está en 1.1.2

La 1.1.2 tiene un fallo en el modo kiosco: cuando el aviso de actualización
aparece, el hook del mouse sigue descartando los clics. El aviso se ve, pero el
botón **Actualizar ahora** no responde. Para sacar esa Pantalla de 1.1.2:

1. Desbloquea la Pantalla con la secuencia de administración
   (`Shift + Shift + Ctrl + Ctrl + Alt + Shift` por defecto) y su contraseña, si
   tiene. El mouse vuelve a funcionar y el aviso se puede aceptar.
2. Si el aviso ya no está, pulsa **Actualizar** en la tarjeta de esa Pantalla
   desde el Controlador para volver a enviarlo.
3. Si no puedes desbloquearla, instala la versión nueva manualmente con el
   `.exe`, el `.msi` o la carpeta portable sobre la instalación existente. Los
   datos de `%APPDATA%\OverrideKiosk` (vinculación, playlist e imágenes) se
   conservan.

La corrección está en la 1.1.4: a partir de esa versión el aviso se puede aceptar
con mouse o pantalla táctil sin desbloquear la Pantalla.

## Límites

- No se actualiza el runtime de Java ni el lanzador `.exe`; solo el payload.
- No hay actualización delta: se transfiere el `app\` completo.
- No hay rollback automático ni copia de la versión anterior.
- No hay firma de código ni de paquetes.
- Si el equipo tiene una política de grupo que bloquea PowerShell o `robocopy`,
  la instalación puede fallar aunque la descarga sea correcta. El detalle queda en
  `<datos>\logs\update.log`.
- No se puede actualizar una Pantalla que se esté ejecutando desde Gradle ni una
  compilación sin empaquetar.
- La Pantalla debe estar encendida y en línea para recibir el paquete; no hay
  actualización por USB ni al arrancar.
