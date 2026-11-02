package app.kiosksync.feature.emitter.domain.models

/** The operation awaiting confirmation in the kiosk password dialog. */
enum class EmitterPendingAction {
    SEND,
    PROJECT,
    CONFIGURE,
    PAIR_LOCK,
}
