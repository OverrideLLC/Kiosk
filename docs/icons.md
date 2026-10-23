# Íconos

Override Kiosk usa **Material Symbols Rounded con `FILL = 1`** (peso 400, tamaño
óptico 24). No hay emojis ni glifos de texto (`↑ ↓ × ✎ +`) como botones.

## Dónde viven

- Los SVG están en `composeApp/src/commonMain/composeResources/drawable/`
  con el prefijo `ic_` y nombre en minúsculas (`ic_add_photo_alternate.svg`).
- El enum `AppIcon` (`designsystem/AppIcon.kt`) expone cada uno.
- El único composable que dibuja íconos de Material es
  `AppIcon(icon, contentDescription, …)`. El logo de marca usa `OverrideLogo(…)`.

Compose Multiplatform convierte el SVG en un `Painter` en tiempo de recursos
(Skia `SVGDOM`) y `Icon` le aplica el tinte del tema. Funciona en el target
desktop del proyecto; no hay targets Android/iOS/web que validar.

## Cómo agregar un ícono

1. Descarga el SVG oficial desde Google Fonts (variante rellena, 24 px):

   ```bash
   curl -o composeApp/src/commonMain/composeResources/drawable/ic_<nombre>.svg \
     "https://fonts.gstatic.com/s/i/short-term/release/materialsymbolsrounded/<nombre>/fill1/24px.svg"
   ```

2. Agrega la entrada al enum en `AppIcon.kt`:

   ```kotlin
   MY_ICON(Res.drawable.ic_my_icon),
   ```

3. Recompila. El acceso `Res.drawable.ic_my_icon` se genera solo y la entrada
   del enum debe importarse igual que las demás:

   ```kotlin
   import app.kiosksync.resources.ic_my_icon
   ```

4. Abre el catálogo (`--ui-catalog`) o corre `CatalogRenderTest` para verlo.

## Logo de Override

El logo de marca no es un ícono de Material Symbols. Vive en
`composeApp/src/commonMain/composeResources/drawable/override_logo.svg` y se dibuja
con `OverrideLogo` (`designsystem/OverrideBrand.kt`), que aplica el tinte del tema.
El pie de las pantallas de gestión usa `OverrideFooter`; las pantallas de
reproducción a pantalla completa no lo muestran.

El SVG actual es el **logo oficial de Override** (marca de tres lóbulos), con
`viewBox` cuadrado `-14.75 -33.75 300 300`, relleno blanco y sin metadatos de
editor. `OverrideLogo` siempre lo dibuja en un cuadrado, así que **no cambies el
`viewBox` por uno rectangular**: el trazo se deformaría. Si reemplazas el archivo,
mantén el nombre y deja el dibujo centrado en un `viewBox` cuadrado.

El ícono de la aplicación y del instalador está en
`composeApp/src/desktopMain/resources/icon.ico` (16, 24, 32, 48, 64, 128 y 256 px).
Se genera con la misma marca blanca sobre un cuadrado redondeado del azul de marca
`#3D7AF5`; `OverrideKiosk.exe` y los accesos directos lo toman de ahí. Si cambias
el logo, vuelve a generar el `.ico` con las mismas resoluciones y conserva el
nombre del archivo.

## Reglas

- Un ícono decorativo lleva `contentDescription = null`.
- Un ícono que es el único contenido de un botón lleva una descripción de la
  acción (`Strings.*`), nunca el nombre técnico del ícono.
- No se agregan íconos “por si acaso”: solo los que usa una pantalla.
- No se cambia el estilo (outlined/sharp) ni se mezclan familias.

## Licencia

Material Symbols forma parte de Material Design Icons y se distribuye bajo
Apache License 2.0. La copia completa está en
`docs/licenses/MATERIAL-SYMBOLS-APACHE-2.0.txt`.

## Catálogo actual

51 íconos: `add`, `add_link`, `add_photo_alternate`, `arrow_downward`,
`arrow_upward`, `cast`, `check`, `check_circle`, `close`, `cloud_off`,
`delete`, `drag_indicator`, `edit`, `error`, `expand_more`, `favorite`,
`folder_open`, `help`, `hourglass_empty`, `image`, `image_not_supported`,
`info`, `link`, `lock`, `lock_open`, `logout`, `more_vert`, `pause`,
`photo_library`, `play_arrow`, `playlist_add`, `playlist_play`,
`present_to_all`, `refresh`, `remove`, `select_all`, `send`, `settings`,
`stop_circle`, `swap_horiz`, `sync`, `sync_problem`, `timer`, `tv`, `undo`,
`upload`, `visibility`, `visibility_off`, `warning`, `wifi`, `wifi_off`.
