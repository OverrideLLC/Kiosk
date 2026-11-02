package app.kiosksync.feature.emitter.domain.models

/** A synchronization status as presented for one receiver. */
enum class SyncUiStatus {
    PENDING,
    UPLOADING,
    APPLYING,
    SUCCESS,
    FAILED,
}
