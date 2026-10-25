package app.kiosksync.feature.update.presentation.actions

sealed interface ReceiverUpdateAction {
    data object Accept : ReceiverUpdateAction
    data object Decline : ReceiverUpdateAction
    data object DismissError : ReceiverUpdateAction
}

sealed interface ReceiverUpdateEffect {
    data object ExitRequested : ReceiverUpdateEffect
}
