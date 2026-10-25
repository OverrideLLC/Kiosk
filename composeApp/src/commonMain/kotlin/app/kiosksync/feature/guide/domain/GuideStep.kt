package app.kiosksync.feature.guide.domain

/** A step in the role-specific interactive guide flow. */
enum class GuideStep {
    ROLE,
    EMITTER_PLAYLIST,
    EMITTER_SCREENS,
    EMITTER_PASSWORD,
    EMITTER_SEND,
    EMITTER_DONE,
    RECEIVER_CODE,
    RECEIVER_PASSWORD,
    RECEIVER_CONTENT,
    RECEIVER_DONE,
}
