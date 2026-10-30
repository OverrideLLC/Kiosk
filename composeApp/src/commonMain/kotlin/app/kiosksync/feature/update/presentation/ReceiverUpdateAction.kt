package app.kiosksync.feature.update.presentation

sealed interface ReceiverUpdateAction {
    data object Accept : ReceiverUpdateAction
    data object Decline : ReceiverUpdateAction
    data object DismissError : ReceiverUpdateAction
}
