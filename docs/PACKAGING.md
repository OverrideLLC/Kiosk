# Empaquetado para Windows

La app se empaqueta con el plugin de Compose Desktop, que usa `jpackage` del JDK.
Hay dos entregables: **carpeta portable** (no requiere WiX) e **instalador**
(requiere WiX Toolset).

## Requisitos en la máquina que compila

- Windows 10 u 11.
- JDK 17 o superior (probado con JDK 21). Comprueba con `java -version`.
- Para generar el instalador **no hace falta instalar WiX a mano**: el plugin de
  Compose descarga WiX 3.11.2 automáticamente la primera vez que ejecutas
  `packageExe` o `packageMsi`. Esa primera ejecución sí necesita internet; las
  siguientes reutilizan la descarga.
- La primera compilación necesita internet para descargar Gradle y dependencias.
  La app empaquetada **no**.

## 1. Carpeta portable

```powershell
.\gradlew.bat createDistributable
```

Salida:

```
composeApp\build\compose\binaries\main\app\OverrideKiosk\
├── OverrideKiosk.exe
├── app\
└── runtime\
```

Para entregarla:

1. Comprime la carpeta `OverrideKiosk` en un `.zip`.
2. Cópiala a la PC destino (Windows 10/11, incluida Home).
3. Descomprime y ejecuta `OverrideKiosk.exe`.

No hace falta JDK ni permisos de administrador. El `.exe` es el lanzador; la app
vive en `app\` y `runtime\`. **No muevas el `.exe` fuera de la carpeta.**

> `jpackage` no genera un único `.exe` autocontenido para la opción portable: es
> una carpeta con un `.exe` dentro. Es el comportamiento esperado, no un error.
> El instalador `.exe`/`.msi` sí es un archivo único.

## 2. Instalador

El plugin descarga WiX automáticamente; solo necesitas internet la primera vez:

```powershell
.\gradlew.bat packageMsi   # instalador .msi
.\gradlew.bat packageExe   # instalador .exe
```

Salida:

```
composeApp\build\compose\binaries\main\msi\
composeApp\build\compose\binaries\main\exe\
```

El instalador está configurado como **por usuario** (`perUserInstall = true`), así
que no pide administrador y funciona en Windows Home. Crea acceso directo en el
menú Inicio y permite elegir la carpeta.

`upgradeUuid` es fijo (`c8945362-54e5-4a20-a7e8-0d58f5030508`). **No lo cambies**
entre versiones o Windows instalará copias en paralelo en lugar de actualizar.
Si algún día cambias de nombre de paquete o de vendor, genera uno nuevo.

## Ícono

El ícono está en:

```
composeApp\src\desktopMain\resources\icon.ico
```

Incluye 16, 24, 32, 48, 64, 128 y 256 px. Para reemplazarlo, sobrescribe ese
archivo con otro `.ico` multi-resolución y vuelve a empaquetar.

## Probar en una PC limpia

1. Copia la carpeta portable (o instala el `.msi`/`.exe`) en una PC **sin JDK**.
2. Abre `OverrideKiosk.exe`. Debe aparecer la pantalla de selección de rol.
3. Elige **Pantalla**. Windows mostrará el aviso del Firewall: permite el acceso
   en **redes privadas** (no públicas).
4. En otra PC, abre la app, elige **Controlador** y busca la pantalla. Si no
   aparece, usa **Agregar por IP** con `IP:8765`.
5. Vincula con el código y envía una playlist de prueba.
6. Cierra y vuelve a abrir la Pantalla: debe arrancar en negro con un código
   nuevo (la Pantalla se limpia al iniciar; ver
   [KNOWN_LIMITATIONS.md](KNOWN_LIMITATIONS.md)).

## Artefactos de empaquetado

Con el nombre actual, las tareas de empaquetado generan estos archivos:

| Archivo en `dist\` | Tamaño | Para qué sirve |
| --- | --- | --- |
| `OverrideKiosk-1.1.1.exe` | ~72 MB | Instalador único. Instala por usuario, sin administrador, con acceso directo. |
| `OverrideKiosk-1.1.1.msi` | ~71 MB | Instalador MSI equivalente, útil para despliegue con `msiexec`. |
| `OverrideKiosk-1.1.1-portable.zip` | ~70 MB | Carpeta portable comprimida: descomprimir y ejecutar `OverrideKiosk.exe`. |

Los originales quedan en `composeApp\build\compose\binaries\main\`; la carpeta
`dist\` es solo una copia cómoda para distribuir y está ignorada por Git. Incluye
`SHA256SUMS.txt` con los hashes de los tres archivos.

El instalador `.exe` y el `.msi` **no están firmados**: Windows SmartScreen pedirá
confirmación. La instalación es por usuario, en `%LOCALAPPDATA%\OverrideKiosk`, y
no requiere administrador. Vuelve a probar instalación y desinstalación después
de cambiar el nombre del paquete.

## Versión y actualizaciones

La versión se declara en dos lugares y **debe coincidir**:

- `composeApp/src/commonMain/kotlin/app/kiosksync/core/Constants.kt`
  (`APP_VERSION`), que usa la app y la política de actualizaciones.
- `composeApp/build.gradle.kts` (`packageVersion`), que usa `jpackage` para el
  nombre de los artefactos.

Para publicar una versión nueva, sube ambas y vuelve a generar los artefactos. Un
Controlador empaquetado con la versión nueva puede enviar su carpeta `app/` a las
Pantallas vinculadas sin internet; el flujo completo, la política de versiones y
sus límites están en [UPDATES.md](UPDATES.md).

La actualización reemplaza solo `app/`: no cambia el runtime ni el `.exe`. La
carpeta portable debe estar en una ubicación escribible por el usuario; si está en
`C:\Program Files` o protegida por una política, la Pantalla no podrá aplicarla.

## Release automática en GitHub

El workflow `.github/workflows/release.yml` compila en `windows-latest` y publica
la release cuando se empuja un tag con la versión:

```powershell
git tag v1.1.1
git push origin v1.1.1
```

El workflow:

1. Lee `APP_VERSION` y falla si el tag no coincide con `v<versión>`.
2. Ejecuta `build`, `createDistributable`, `packageMsi` y `packageExe`.
3. Publica en GitHub Releases el `.exe`, el `.msi`, la carpeta portable en `.zip`
   y `SHA256SUMS.txt`.

También se puede lanzar a mano desde **Actions → Release Windows → Run
workflow**. La primera ejecución tarda más porque descarga Gradle, dependencias y
WiX. Los artefactos también quedan adjuntos al run del workflow.

## Consumo de memoria

Medición con la carpeta portable en Windows 11 (JDK 21, ventana normal, sin
imágenes cargadas):

| Rol | Working set | Memoria privada |
| --- | --- | --- |
| Controlador | ~241 MB | ~275 MB |
| Pantalla | ~231 MB | ~267 MB |

El lanzador de `jpackage` agrega un proceso auxiliar de unos 8 MB. El consumo
puede variar por la versión de la JVM y por la cantidad y el tamaño de las
imágenes. Mide con el Administrador de tareas en la PC real antes de fijar
requisitos de hardware. Para un equipo de 4 GB de RAM es suficiente; 8 GB deja
margen para Windows y el resto de la operación.

## Firewall y red

- La Pantalla abre el puerto 8765 (o el siguiente libre). La primera vez Windows
  pide permiso; marca **Red privada**.
- Si el negocio tiene varias subredes o VLANs, el mDNS puede no cruzar. Usa
  **Agregar por IP** o pide a quien administre la red que permita el tráfico.
- La app no necesita internet. Si el Firewall bloquea, la Pantalla igual
  reproduce su copia local.

## Firma de código (opcional)

El instalador no está firmado. Windows SmartScreen mostrará "Windows protegió su
PC" hasta que el usuario elija **Más información → Ejecutar de todos modos**. Para
evitarlo hace falta un certificado de firma de código y firmar el `.exe`/`.msi`
con `signtool`. No es parte del MVP.

## Desinstalar

- Instalador: **Configuración → Aplicaciones → Override Kiosk → Desinstalar**.
- Portable: borra la carpeta.
- Los datos de `%APPDATA%\OverrideKiosk\` **no se borran** automáticamente. Bórralos a
  mano si quieres dejar el equipo limpio (incluye la vinculación y las imágenes).

## Notas para desarrollo

- En Linux, `createDistributable` genera una imagen para Linux, no para Windows.
  Los `.msi`/`.exe` solo se pueden generar en Windows con WiX.
- `.\gradlew.bat run` sigue siendo la forma rápida de probar; el empaquetado es
  solo para entregar. Las ejecuciones desde Gradle **no pueden enviar ni recibir
  actualizaciones** porque no tienen una carpeta `app/` empaquetada.
- Si cambias `packageVersion`, usa `mayor.menor.parche` (por ejemplo `1.1.1`) y
  actualiza también `APP_VERSION`.
- Para probar una actualización: instala la versión anterior en el Controlador y la
  Pantalla, genera la versión nueva, sustituye solo la instalación del Controlador
  y comprueba que la Pantalla recibe el aviso, se reinicia y conserva la playlist.
