# Sistema de diseño

El sistema visual compartido vive en `app.kiosksync.core.ui` (`commonMain`), con
temas en `core/ui/theme` y componentes globales en `core/ui/components`. Los
componentes de una sola feature se quedan junto a esa pantalla. Ninguna pantalla
debe duplicar tokens, colores, formas o animaciones globales.

## Base técnica actual y objetivo

- Compose Multiplatform **1.12.2** (target Desktop JVM).
- Material Kolor **6.0.0**, módulo `material-kolor-material3`.
- Recursos de Compose (`composeResources`) para tipografías e íconos.

### Componentes compartidos

Los controles de la app (`AppButton`, `AppDialog`, `AppTextField`, estados vacíos,
indicadores y demás) son componentes compartidos. Se reutilizan desde las
pantallas y no dependen de APIs experimentales para conseguir consistencia.

## Tema

```kotlin
KioskTheme(theme = AppTheme.DARK, reduceMotion = false) {
    // pantallas
}
```

- `AppTheme.DARK` es el valor por defecto (salas con poca luz).
- `AppTheme.LIGHT` está completo y se puede alternar.
- La preferencia se guarda en DataStore Preferences junto al rol y sobrevive al
  reinicio. `config.json` se conserva en `legacy/` como respaldo tras migrarlo.
- `reduceMotion = true` desactiva springs y fades desde un solo punto
  (`LocalReduceMotion`).

## Color

El tema genera la paleta tonal desde el azul semilla **`#3D7AF5`** con Material
Kolor Material 3 y `ColorSpec.SpecVersion.SPEC_2025`. La galería de UI muestra los
roles generados en claro y oscuro; los tokens de éxito, advertencia e información
siguen definidos por la app.

### Roles principales

| Rol | Uso |
| --- | --- |
| `primary` | Acción principal y foco visual de marca |
| `secondary` | Acciones y superficies tonales secundarias |
| `tertiary` | Acentos generados a partir de la semilla |
| `surface` / `surfaceContainer*` | Fondo de pantalla y jerarquía de superficies |
| `error` | Errores destructivos y de seguridad |
| `success` / `warning` / `infoContainer` | Estados semánticos adicionales de la app |

Además hay roles que Material 3 no modela (`KioskExtraColors`): `success`,
`warning`, `infoContainer`, `disabledContainer` y `disabledContent`.

Revisar los pares de texto normal con una meta de contraste WCAG AA de 4.5:1 y
los componentes gráficos con una meta de 3:1. El contorno se usa para bordes e
íconos, nunca como color de texto pequeño. Repetir la revisión si cambia la
semilla, el estilo de paleta o la versión de Material Kolor.

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
| `AppButton` | `core/ui/components/AppButton.kt` | 4 variantes × 3 tamaños, ícono, carga, motivo de deshabilitado |
| `AppIconButton` | `core/ui/components/AppButton.kt` | acción de solo ícono, 3 variantes |
| `AppButtonGroup` | `core/ui/components/AppButton.kt` | control segmentado tipo píldora |
| `AppTextField` | `core/ui/components/AppTextField.kt` | etiqueta, ayuda y error inline |
| `AppStepper` | `core/ui/components/AppStepper.kt` | número con −/+ y unidad |
| `AppSelectableCard` | `core/ui/components/AppSelectableCard.kt` | tarjeta con rol radio/check, foco y “morph” |
| `AppStatusChip` | `core/ui/components/AppStatusChip.kt` | estados con tono semántico |
| `AppListItem` | `core/ui/components/AppListItem.kt` | fila con miniatura y acciones |
| `AppDialog` | `core/ui/components/AppDialog.kt` | diálogo 480 dp (600 dp en el de seguridad) con ícono y acciones |
| `AppDropdownMenu` | `core/ui/components/AppDropdownMenu.kt` | menú de desbordamiento |
| `AppSnackbarHost` | `core/ui/components/AppSnackbar.kt` | aviso con “Deshacer” |
| `AppLoadingIndicator` | `core/ui/components/AppLoadingIndicator.kt` | puntos ondulados |
| `AppLinearProgress` | `core/ui/components/AppLoadingIndicator.kt` | progreso con etiqueta |
| `AppCircularProgress` | `core/ui/components/AppLoadingIndicator.kt` | espera circular |
| `AppEmptyState` | `core/ui/components/AppEmptyState.kt` | ícono + frase + acción |
| `AppTooltip` | `core/ui/components/AppTooltip.kt` | atajos y motivos |
| `AppIcon` | `core/ui/components/AppIcon.kt` | único punto de dibujo de íconos |

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
