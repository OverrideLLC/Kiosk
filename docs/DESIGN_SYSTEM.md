# Sistema de diseño

El paquete `app.kiosksync.designsystem` (en `commonMain`) concentra **todos** los
tokens y componentes visuales. Ninguna pantalla debe declarar colores, tamaños,
formas ni animaciones propias.

## Base técnica

- Compose Multiplatform **1.7.3** (solo target desktop JVM).
- Material 3 **1.3.1** (estable), la versión que resuelve `compose.material3`.
- Recursos de Compose (`composeResources`) para tipografías e íconos.

### Sobre Material 3 Expressive

La versión estable 1.3.1 **no** expone las APIs Expressive:

| API | Estado real |
| --- | --- |
| `MaterialExpressiveTheme` | Existe, pero es `internal` |
| `ButtonGroup` | No existe |
| `LoadingIndicator` ondulado | No existe |
| `MotionScheme` | No existe |
| `MaterialShapes` | No existe |

Por eso el sistema de diseño **emula** el lenguaje Expressive con tokens
propios (formas de 28 dp, botones píldora, springs cortos) y componentes
propios (`AppButtonGroup`, `AppLoadingIndicator`, `AppEmptyState`). No se
forzaron versiones alfa.

## Tema

```kotlin
KioskTheme(theme = AppTheme.DARK, reduceMotion = false) {
    // pantallas
}
```

- `AppTheme.DARK` es el valor por defecto (salas con poca luz).
- `AppTheme.LIGHT` está completo y se puede alternar.
- La preferencia se guarda en `config.json` (`AppConfig.theme`) y sobrevive al
  reinicio.
- `reduceMotion = true` desactiva springs y fades desde un solo punto
  (`LocalReduceMotion`).

## Color

Paleta tonal generada a partir del azul semilla **`#3D7AF5`** (tonos en
CIE L\*a\*b\*, con reducción de croma para no salir del gamut sRGB).

### Roles principales

| Rol | Oscuro | Claro |
| --- | --- | --- |
| `primary` | `#BBC3FF` | `#295AB9` |
| `onPrimary` | `#002E6B` | `#FFFFFF` |
| `primaryContainer` | `#21438B` | `#DEE0FF` |
| `onPrimaryContainer` | `#DEE0FF` | `#001A43` |
| `secondary` | `#C2C5E3` | `#5A5D77` |
| `secondaryContainer` | `#42455E` | `#E2E2E7` |
| `tertiary` | `#F3B5CD` | `#844E64` |
| `surface` | `#131317` | `#F9F9FE` |
| `onSurface` | `#E2E2E7` | `#1B1B1F` |
| `surfaceContainerHigh` | `#292A2D` | `#E7E8ED` |
| `surfaceContainerHighest` | `#343438` | `#E2E2E7` |
| `onSurfaceVariant` | `#C4C5D8` | `#444655` |
| `outline` | `#8E8FA1` | `#747687` |
| `error` | `#FFB4AB` | `#9F3F3B` |
| `errorContainer` | `#822525` | `#FFDAD6` |

Además hay roles que Material 3 no modela (`KioskExtraColors`): `success`,
`warning`, `infoContainer`, `disabledContainer`, `disabledContent` y
`hoverOverlay`.

### Contraste (WCAG)

| Par | Oscuro | Claro |
| --- | --- | --- |
| Texto normal / superficie | 14.35:1 | 16.36:1 |
| Texto secundario / superficie | 10.88:1 | 8.88:1 |
| Texto secundario / superficie alta | 8.42:1 | 7.62:1 |
| Contorno / superficie | 5.82:1 | 4.27:1 |
| Primario / superficie | 10.91:1 | 6.14:1 |
| Sobre primario / primario | 7.71:1 | 6.45:1 |
| Error / superficie | 10.91:1 | 6.16:1 |
| Deshabilitado / contenedor | 7.27:1 | 7.22:1 |

Todos los pares de texto superan AA (4.5:1). El contorno se usa para bordes e
íconos (mínimo 3:1), nunca como color de texto.

Reglas:

- **Primario** = acción principal.
- **Tonal** = acciones secundarias.
- **Error** solo para eliminar, fallos y bloqueos de seguridad. “Cambiar de
  rol” y “Salir” no usan error.
- Sin degradados radiales ni manchas de fondo.

## Tipografía

| Familia | Uso | Licencia |
| --- | --- | --- |
| **Sora** 600/700/800 | Titulares, títulos, botones | SIL OFL 1.1 |
| **Inter** 400/500/600 | Cuerpo, etiquetas, campos | SIL OFL 1.1 |

Las licencias están en `docs/licenses/SORA-OFL.txt` y
`docs/licenses/INTER-OFL.txt`.

### Escala del Controlador

| Rol | Tamaño | Peso |
| --- | --- | --- |
| `displayLarge` | 57 sp | 800 |
| `headlineLarge` | 32 sp | 800 |
| `headlineMedium` | 28 sp | 700 |
| `headlineSmall` | 24 sp | 700 |
| `titleLarge` | 20 sp | 600 |
| `titleMedium` | 17 sp | 600 |
| `bodyLarge` | 16 sp | 400 |
| `bodyMedium` | 15 sp | 400 |
| `bodySmall` | 14 sp | 400 |
| `labelLarge` | 15 sp | 600 |
| `labelMedium` | 13 sp | 500 |
| `labelSmall` | 13 sp | 500 |

Nada por debajo de **13 sp** en el Controlador.

### Escala de distancia (Pantalla)

`KioskDisplayType` expone:

- `pairingCode()`: 104 sp monoespaciado, `letterSpacing` 8 sp (código ≥ 96 sp).
- `screenTitle()`: 40 sp display.
- `screenBody()`: 28 sp.
- `screenCaption()`: 24 sp.

## Formas y espacio

- Tarjetas: 28 dp (32 dp cuando están seleccionadas, con transición).
- Botones y chips: píldora.
- Campos: 16 dp.
- Diálogos: 28 dp.
- Menús: 20 dp.
- Escala de espacio: 4, 8, 12, 16, 24, 32, 40 y 56 dp (`Space`).
- Ancho máximo de contenido: 960 dp (`Sizes.contentMaxWidth`) en selección de
  rol, pantalla de vinculación y diálogos. El Controlador es un tablero de dos
  columnas y usa el ancho disponible con márgenes de 24–32 dp; sus filas y
  tarjetas mantienen la lectura acotada.
- Área de clic mínima: 40 dp.

## Movimiento

Todo vive en `Motion`:

- `fast()`: spring sin rebote, stiffness 900 (selección, hover, foco).
- `medium()`: spring 0.82 / 600 (tarjetas, filas, menús).
- `slow()`: spring 0.78 / 320 (transiciones de pantalla).
- `fade()`: tween 220 ms con `FastOutSlowInEasing`.

Con `reduceMotion` activo, todas devuelven `snap()`.

La Pantalla en reproducción no tiene movimiento decorativo.

## Componentes

| Componente | Archivo | Uso |
| --- | --- | --- |
| `AppButton` | `AppButton.kt` | 4 variantes × 3 tamaños, ícono, carga, motivo de deshabilitado |
| `AppIconButton` | `AppButton.kt` | acción de solo ícono, 3 variantes |
| `AppButtonGroup` | `AppButton.kt` | control segmentado tipo píldora |
| `AppTextField` | `AppTextField.kt` | etiqueta, ayuda y error inline |
| `AppStepper` | `AppStepper.kt` | número con −/+ y unidad |
| `AppSelectableCard` | `AppSelectableCard.kt` | tarjeta con rol radio/check, foco y “morph” |
| `AppStatusChip` | `AppStatusChip.kt` | estados con tono semántico |
| `AppListItem` | `AppListItem.kt` | fila con miniatura y acciones |
| `AppDialog` | `AppDialog.kt` | diálogo 480 dp (600 dp en el de seguridad) con ícono y acciones |
| `AppDropdownMenu` | `AppDropdownMenu.kt` | menú de desbordamiento |
| `AppSnackbarHost` | `AppSnackbar.kt` | aviso con “Deshacer” |
| `AppLoadingIndicator` | `AppLoadingIndicator.kt` | puntos ondulados |
| `AppLinearProgress` | `AppLoadingIndicator.kt` | progreso con etiqueta |
| `AppCircularProgress` | `AppLoadingIndicator.kt` | espera circular |
| `AppEmptyState` | `AppEmptyState.kt` | ícono + frase + acción |
| `AppTooltip` | `AppTooltip.kt` | atajos y motivos |
| `AppIcon` | `AppIcon.kt` | único punto de dibujo de íconos |

## Pantallas

| Pantalla | Composición | Notas |
| --- | --- | --- |
| Selección de rol | Marca + titular 32 sp + dos tarjetas grandes + “Salir” | Flechas mueven la selección; Enter confirma. |
| Pantalla sin vincular | Fondo negro, código agrupado (“685 853”) a 104 sp, texto de apoyo ≥ 24 sp | Sin menús visibles; acciones de administración discretas. |
| Pantalla bloqueada | Tarjeta tonal, ícono de candado, puntos de contraseña, pista de recuperación 24 sp | No revela la secuencia. |
| Controlador | Cabecera con marca, rol, estado de bloqueo y menú; dos paneles (≥ 1100 dp) o pestañas | Pantallas, playlist, barra de envío fija. |
| Reproducción | Imagen `ContentScale.Fit` sobre negro, fundido de 500 ms | Sin movimiento decorativo; estado vacío con marca. |

## Accesibilidad

- Los controles interactivos usan `Role` y `contentDescription`.
- Las tarjetas y grupos exponen selección (`selectable` / `selectableGroup`).
- El foco se dibuja con un anillo de 3 dp en las tarjetas.
- Los botones deshabilitados mantienen texto legible y exponen el motivo como
  `stateDescription` + tooltip.

## Catálogo interno

Para revisar todos los tokens y componentes juntos:

```powershell
.\gradlew.bat run --args="--ui-catalog"
```

Abre una ventana de desarrollo (no forma parte del flujo de usuario) con:

- selector de tema oscuro/claro;
- interruptor de movimiento reducido;
- muestras de color, tipografía, formas, botones, íconos, campos, stepper,
  chips, tarjetas, filas, diálogo, menú, snackbar, tooltips, carga y estados
  vacíos.

El test `CatalogRenderTest` renderiza el catálogo fuera de pantalla en ambos
temas y deja capturas en `composeApp/build/reports/ui/` para revisión visual.
`ControllerRenderTest` y `ScreenRenderTest` hacen lo mismo con el Controlador
(oscuro, claro, compacto y diálogos) y con la selección de rol y la reproducción.
