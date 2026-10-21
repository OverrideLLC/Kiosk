package app.kiosksync.core

object Strings {
    const val APP_TITLE = APP_NAME

    // Role selection
    const val ROLE_SELECTION_TITLE = "¿Qué rol tendrá este equipo?"
    const val ROLE_SELECTION_SUBTITLE = "Selecciona cómo funcionará este equipo en tu red."
    const val ROLE_EMITTER_TITLE = "Controlador"
    const val ROLE_EMITTER_DESC = "Administra la playlist y envía el contenido a las pantallas."
    const val ROLE_RECEIVER_TITLE = "Pantalla"
    const val ROLE_RECEIVER_DESC = "Recibe el contenido y lo reproduce a pantalla completa."
    const val ROLE_CONFIRM_BUTTON = "Continuar con este rol"
    const val CHANGE_ROLE = "Cambiar de rol"
    const val CHANGE_ROLE_CONFIRM = "¿Seguro que deseas cambiar de rol? Se borrará la vinculación actual."

    // Status & messages
    const val WAITING_CONTENT = "Esperando contenido"
    const val WAITING_PLAYLIST_TITLE = "Esperando Playlist"
    const val WAITING_PLAYLIST_MESSAGE =
        "El Controlador enviará el contenido automáticamente cuando esté listo."
    const val ENTER_CODE_IN_CONTROLLER = "Escribe este código en el Controlador"
    const val PAIRING_CODE_LABEL = "Código de vinculación"
    const val STATUS_ONLINE = "En línea"
    const val STATUS_OFFLINE = "Desconectada"
    const val STATUS_UNPAIRED = "Sin vincular"
    const val EXIT_APP = "Salir del programa"

    // Receiver pairing
    const val RECEIVER_PAIRING_TITLE = "Vincula esta pantalla"
    const val RECEIVER_CODE_RENEWS_IN = "El código se renueva en"
    const val RECEIVER_LOCKED_TITLE = "Demasiados intentos fallidos"
    const val RECEIVER_LOCKED_MESSAGE = "Espera unos segundos antes de intentarlo de nuevo."
    const val RECEIVER_WAITING_FOR_EMITTER = "Esperando al Controlador"
    const val RECEIVER_WAITING_PASSWORD_TITLE = "Esperando contraseña del Controlador"
    const val RECEIVER_WAITING_PASSWORD_MESSAGE =
        "La pantalla se bloqueará en cuanto el Controlador envíe la contraseña de bloqueo."
    const val UNPAIR_RECEIVER_TITLE = "Desvincular esta pantalla"
    const val UNPAIR_RECEIVER_CONFIRM = "¿Seguro que deseas desvincular esta pantalla de su Controlador? Se mostrará un código nuevo."
    const val UNPAIR_RECEIVER_DONE = "Pantalla desvinculada"
    const val SERVER_START_ERROR = "No se pudo iniciar el servicio de red. Revisa el registro."

    // Emitter screens
    const val EMITTER_SCREENS_TITLE = "Pantallas"
    const val EMITTER_SCREENS_SUBTITLE = "Selecciona las pantallas a las que enviarás contenido."
    const val ADD_BY_IP = "Agregar por IP"
    const val DISCOVERY_REFRESH = "Actualizar"
    const val DISCOVERY_REFRESHING = "Buscando…"
    const val DISCOVERY_SEARCHING = "Buscando pantallas en la red local…"
    const val DISCOVERY_EMPTY = "Todavía no hay pantallas. Enciende una Pantalla en la misma red o agrégala por IP."
    const val PAIRED_OTHER_CONTROLLER = "Vinculada a otro Controlador"
    const val PAIR_BUTTON = "Vincular"
    const val UNPAIR_BUTTON = "Desvincular"
    const val PAIR_DIALOG_TITLE = "Vincular pantalla"
    const val PAIR_DIALOG_INSTRUCTION = "Escribe el código de 6 dígitos que aparece en la pantalla."
    const val PAIR_CODE_FIELD = "Código"
    const val PAIR_DIALOG_CONFIRM = "Vincular"
    const val PAIR_DIALOG_CANCEL = "Cancelar"
    const val PAIRING_IN_PROGRESS = "Vinculando…"
    const val PAIR_ERROR_INVALID_CODE = "Código incorrecto."
    const val PAIR_ERROR_REMAINING_ATTEMPTS = "Intentos restantes:"
    const val PAIR_ERROR_LOCKED = "Demasiados intentos fallidos. Espera antes de volver a intentarlo."
    const val PAIR_ERROR_ALREADY_PAIRED = "Esa pantalla ya está vinculada a otro Controlador."
    const val PAIR_ERROR_NETWORK = "No se pudo conectar con la pantalla."
    const val PAIR_SUCCESS = "Pantalla vinculada correctamente."
    const val UNPAIR_SUCCESS = "Pantalla desvinculada correctamente."
    const val UNPAIR_ERROR = "No se pudo desvincular la pantalla. Inténtalo cuando esté en línea."
    const val UNPAIR_DIALOG_TITLE = "Desvincular pantalla"
    const val UNPAIR_DIALOG_CONFIRM = "¿Seguro que deseas desvincular esta pantalla?"
    const val MANUAL_IP_DIALOG_TITLE = "Agregar pantalla por IP"
    const val MANUAL_IP_DIALOG_INSTRUCTION = "Escribe la dirección IP y el puerto de la Pantalla."
    const val MANUAL_IP_FIELD = "IP:puerto"
    const val MANUAL_IP_HINT = "Ejemplo: 192.168.1.20:8765"
    const val MANUAL_IP_INVALID = "Formato no válido. Usa IP:puerto, por ejemplo 192.168.1.20:8765."
    const val MANUAL_IP_NOT_RECEIVER = "Ese equipo no es una Pantalla de KioskSync."
    const val MANUAL_IP_UNREACHABLE = "No se pudo conectar con esa dirección."
    const val ADD_BUTTON = "Agregar"
    const val CANCEL_BUTTON = "Cancelar"
    const val CLOSE_BUTTON = "Cerrar"
    const val SAVE_BUTTON = "Guardar"
    const val OPTIONS_TITLE = "Opciones de Pantalla"
    const val OPTIONS_MESSAGE = "¿Deseas cambiar de rol o salir del programa?"
    const val RECEIVER_UNLOCKED_MESSAGE =
        "Equipo desbloqueado temporalmente. Puedes cambiar de rol, salir del programa o volver a bloquear la pantalla."
    const val RELOCK_BUTTON = "Volver a bloquear"
    const val CONTINUE_BUTTON = "Continuar"
    const val EXIT_APP_CONFIRM = "Salir de la aplicación"

    // Playlist editor
    const val PLAYLIST_TITLE = "Playlist"
    const val PLAYLIST_SUBTITLE = "Ordena las imágenes y ajusta cuánto tiempo se muestra cada una."
    const val PLAYLIST_SELECTOR_LABEL = "Playlist activa"
    const val PLAYLIST_DEFAULT_NAME = "Mi playlist"
    const val PLAYLIST_NEW = "Nueva"
    const val PLAYLIST_RENAME = "Renombrar"
    const val PLAYLIST_DELETE = "Eliminar"
    const val PLAYLIST_NEW_TITLE = "Nueva playlist"
    const val PLAYLIST_RENAME_TITLE = "Renombrar playlist"
    const val PLAYLIST_DELETE_TITLE = "Eliminar playlist"
    const val PLAYLIST_NAME_FIELD = "Nombre"
    const val PLAYLIST_NAME_HINT = "Por ejemplo: Anuncios, Especificaciones"
    const val PLAYLIST_NAME_REQUIRED = "Escribe un nombre para la playlist."
    const val PLAYLIST_NAME_EXISTS = "Ya existe una playlist con ese nombre."
    const val PLAYLIST_DELETE_CONFIRM = "¿Seguro que deseas eliminar esta playlist? Las imágenes compartidas con otras playlists no se borran."
    const val PLAYLIST_DELETE_LAST_ERROR = "Debe quedar al menos una playlist."
    const val ADD_IMAGES = "Agregar imágenes"
    const val ADDING_IMAGES = "Agregando…"
    const val PLAYLIST_EMPTY = "Todavía no hay imágenes. Agrega archivos JPG, PNG o WEBP."
    const val DEFAULT_DURATION = "Duración por defecto"
    const val DURATION_HINT = "Entre 3 y 600 segundos. Vacío en una imagen = usar la duración por defecto."
    const val SECONDS_SUFFIX = "s"
    const val DURATION_FIELD_HINT = "auto"
    const val MOVE_UP = "Subir"
    const val MOVE_DOWN = "Bajar"
    const val REMOVE_ITEM = "Quitar"
    const val ITEM_DEFAULT_DURATION = "Por defecto"
    const val PLAYLIST_IMPORTED = "Imágenes agregadas"
    const val PLAYLIST_REJECTED = "Archivos rechazados"
    const val PLAYLIST_IMPORT_NONE = "No se agregó ninguna imagen. Solo se admiten JPG, PNG o WEBP de hasta 25 MB."
    const val PLAYLIST_IMPORT_ERROR = "Algunas imágenes no se pudieron agregar. Revisa el formato y el tamaño."

    // Kiosk lock
    const val KIOSK_LOCKED_TITLE = "Equipo bloqueado"
    const val KIOSK_UNLOCK_HINT = "Shift + Shift + Ctrl y escribe la contraseña"
    const val KIOSK_RECOVERY_HINT = "Si olvidaste la contraseña, usa la frase de recuperación universal."
    const val KIOSK_WRONG_PASSWORD = "Contraseña incorrecta."
    const val KIOSK_ATTEMPTS_LEFT = "Intentos restantes:"
    const val KIOSK_LOCKED_OUT = "Demasiados intentos fallidos. Espera antes de volver a intentarlo."
    const val KIOSK_CHORD_LABEL = "Secuencia de desbloqueo:"
    const val KIOSK_TYPING_PASSWORD = "Escribe la contraseña"
    const val KIOSK_KEYS_ACTIVE = "Las teclas se registran aquí"
    const val KIOSK_KEY_CHAR = "•"
    const val KIOSK_KEY_BACKSPACE = "Retroceso"

    fun kioskLastKey(key: String): String = "Última tecla: $key"
    const val KIOSK_PASSWORD_TITLE = "Contraseña de bloqueo"
    const val KIOSK_PASSWORD_INSTRUCTION =
        "Esta contraseña bloqueará el teclado de esta PC y de las pantallas que reciban contenido. " +
            "Para desbloquear: Shift + Shift + Ctrl y luego la contraseña."
    const val KIOSK_PASSWORD_FIELD = "Contraseña"
    const val KIOSK_PASSWORD_CONFIRM_FIELD = "Repite la contraseña"
    const val KIOSK_PASSWORD_RULE = "Entre 4 y 64 caracteres. Solo letras y números."
    const val KIOSK_PASSWORD_MISMATCH = "Las contraseñas no coinciden."
    const val KIOSK_PASSWORD_INVALID = "Usa entre 4 y 64 letras o números."
    const val KIOSK_PASSWORD_SAVED = "Contraseña de bloqueo guardada."
    const val KIOSK_PASSWORD_APPLIES_NOW =
        "Si la cambias, se enviará ahora mismo a todas las pantallas vinculadas."
    const val KIOSK_PASSWORD_PAIR_TITLE = "Bloquea la pantalla recién vinculada"
    const val KIOSK_PASSWORD_PAIR_HINT =
        "La pantalla está esperando esta contraseña para bloquearse."
    const val KIOSK_SECURITY_BUTTON = "Seguridad"
    const val KIOSK_PASSWORD_CHANGE_TITLE = "Cambiar contraseña de bloqueo"
    const val KIOSK_UNIVERSAL_HINT =
        "La frase de recuperación universal está documentada en el manual del proyecto."
    const val KIOSK_LOCK_ACTIVE_HINT = "El equipo quedará bloqueado hasta que se introduzca la contraseña."
    const val KIOSK_RECOVERY_USED = "Bloqueo liberado con la frase de recuperación universal."

    // Projection (controller as a display)
    const val PROJECT_BUTTON = "Proyectar"
    const val PROJECT_STOP = "Dejar de proyectar"
    const val PROJECT_HINT = "Muestra la playlist activa a pantalla completa en este equipo."
    const val PROJECT_NO_PLAYLIST = "Agrega al menos una imagen antes de proyectar."
    const val PROJECT_SHORTCUT_HINT = "Atajo: Win (Super) + V"

    // Sending content
    const val SEND_TITLE = "Enviar"
    const val SEND_SUBTITLE = "Envía la playlist activa a las pantallas seleccionadas."
    const val SEND_BUTTON = "Enviar a pantallas seleccionadas"
    const val SENDING = "Enviando…"
    const val SEND_NO_SCREENS = "Selecciona al menos una pantalla vinculada."
    const val SEND_EMPTY_PLAYLIST = "Agrega al menos una imagen a la playlist antes de enviar."
    const val SEND_DEVICE_PENDING = "En espera…"
    const val SEND_DEVICE_UPLOADING = "Subiendo imágenes"
    const val SEND_DEVICE_APPLYING = "Aplicando playlist…"
    const val SEND_DEVICE_SUCCESS = "Enviado correctamente."
    const val SEND_DEVICE_UP_TO_DATE = "Ya estaba actualizada."
    const val SEND_DEVICE_FAILED = "No se pudo enviar."
    const val SEND_ERROR_MISSING = "Faltan imágenes en la Pantalla."
    const val SEND_ERROR_LOCAL_MEDIA = "Falta un archivo de imagen en este equipo."
    const val SEND_ERROR_NETWORK = "No se pudo conectar con la Pantalla."

    fun sendCompleted(success: Int, failure: Int): String = when {
        success == 0 -> "No se pudo enviar a ninguna pantalla."
        failure == 0 && success == 1 -> "Contenido enviado a 1 pantalla."
        failure == 0 -> "Contenido enviado a $success pantallas."
        else -> "Envío terminado: $success pantalla(s) correcta(s) y $failure con error."
    }

    fun syncUploading(uploaded: Int, total: Int): String = "Subiendo $uploaded de $total…"

    fun playlistImportSummary(added: Int, rejected: Int): String = when {
        added > 0 && rejected == 0 -> "$PLAYLIST_IMPORTED: $added."
        added > 0 && rejected > 0 -> "$PLAYLIST_IMPORTED: $added. $PLAYLIST_REJECTED: $rejected."
        else -> PLAYLIST_IMPORT_NONE
    }

    fun selectedScreens(count: Int): String = when (count) {
        0 -> SEND_NO_SCREENS
        1 -> "1 pantalla seleccionada."
        else -> "$count pantallas seleccionadas."
    }

    fun kioskLockoutSeconds(seconds: Int): String =
        "Demasiados intentos fallidos. Espera $seconds s."

    // Skeleton indicator for Phase 0
    const val SKELETON_READY = "KioskSync - Fase 0 inicializada correctamente"
}
