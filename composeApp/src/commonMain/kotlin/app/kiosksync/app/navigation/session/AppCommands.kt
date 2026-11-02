package app.kiosksync.app.navigation.session

/** Window callbacks read by ViewModels. Set during composition, invoked later. */
internal class AppCommands {
    var onExit: () -> Unit = {}
    var onOpenGuide: () -> Unit = {}
    var onOpenReceiverGuide: () -> Unit = {}
    var onRoleReset: () -> Unit = {}
}
