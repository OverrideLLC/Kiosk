# Empaquetado para Windows

La app se empaqueta con el plugin de Compose Desktop, que usa `jpackage` del JDK.
Hay dos entregables: **carpeta portable** (no requiere WiX) e **instalador**
(requiere WiX Toolset).

## Requisitos en la máquina que compila

- Windows 10 u 11.
- JDK 17 o superior (probado con JDK 21). Comprueba con `java -version`.
- WiX Toolset **3.x** instalado y en el `PATH` solo si vas a generar el instalador.
  `jpackage` de JDK 17/21 no soporta WiX 4.
- La primera compilación necesita internet para descargar Gradle y dependencias.
  La app empaquetada **no**.

## 1. Carpeta portable

```powershell
.\gradlew.bat createDistributable
```

Salida:

```
composeApp\build\compose\binaries\main\app\KioskSync\
├── KioskSync.exe
├── app\
└── runtime\
```

Para entregarla:

1. Comprime la carpeta `KioskSync` en un `.zip`.
2. Cópiala a la PC destino (Windows 10/11, incluida Home).
3. Descomprime y ejecuta `KioskSync.exe`.

No hace falta JDK ni permisos de administrador. El `.exe` es el lanzador; la app
vive en `app\` y `runtime\`. **No muevas el `.exe` fuera de la carpeta.**

> `jpackage` no genera un único `.exe` autocontenido. La opción portable es una
> carpeta con un `.exe` dentro. Es el comportamiento esperado, no un error.

## 2. Instalador

Con WiX 3.x instalado:

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
2. Abre `KioskSync.exe`. Debe aparecer la pantalla de selección de rol.
3. Elige **Pantalla**. Windows mostrará el aviso del Firewall: permite el acceso
   en **redes privadas** (no públicas).
4. En otra PC, abre la app, elige **Controlador** y busca la pantalla. Si no
   aparece, usa **Agregar por IP** con `IP:8765`.
5. Vincula con el código y envía una playlist de prueba.
6. Cierra y vuelve a abrir la Pantalla: debe arrancar en negro con un código
   nuevo (la Pantalla se limpia al iniciar; ver
   [KNOWN_LIMITATIONS.md](KNOWN_LIMITATIONS.md)).

## Consumo de memoria

Medición de referencia con la imagen `createDistributable` en Linux (JVM + Compose
+ Skia, sin imágenes cargadas):

| Rol | RSS aproximado |
| --- | --- |
| Controlador (ventana normal) | ~230 MB |
| Pantalla (ventana normal) | ~225 MB |

En Windows puede variar por la versión de la JVM y por la cantidad y el tamaño de
las imágenes. Mide con el Administrador de tareas en la PC real antes de fijar
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

- Instalador: **Configuración → Aplicaciones → KioskSync → Desinstalar**.
- Portable: borra la carpeta.
- Los datos de `%APPDATA%\KioskSync\` **no se borran** automáticamente. Bórralos a
  mano si quieres dejar el equipo limpio (incluye la vinculación y las imágenes).

## Notas para desarrollo

- En Linux, `createDistributable` genera una imagen para Linux, no para Windows.
  Los `.msi`/`.exe` solo se pueden generar en Windows con WiX.
- `.\gradlew.bat run` sigue siendo la forma rápida de probar; el empaquetado es
  solo para entregar.
- Si cambias `packageVersion`, usa `mayor.menor.parche` (por ejemplo `1.0.1`).
