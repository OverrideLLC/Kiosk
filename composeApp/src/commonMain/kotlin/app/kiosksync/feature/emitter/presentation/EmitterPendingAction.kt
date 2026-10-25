package app.kiosksync.feature.emitter.presentation

/** The operation awaiting confirmation in the kiosk password dialog. */
enum class EmitterPendingAction {
    SEND,
    PROJECT,
    CONFIGURE,
    PAIR_LOCK,
}
