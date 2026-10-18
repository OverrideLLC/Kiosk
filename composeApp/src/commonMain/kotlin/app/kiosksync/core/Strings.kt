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
    const val ENTER_CODE_IN_CONTROLLER = "Escribe este código en el Controlador"
    const val PAIRING_CODE_LABEL = "Código de vinculación"
    const val STATUS_ONLINE = "En línea"
    const val STATUS_OFFLINE = "Desconectada"
    const val STATUS_UNPAIRED = "Sin vincular"

    // Skeleton indicator for Phase 0
    const val SKELETON_READY = "KioskSync - Fase 0 inicializada correctamente"
}
