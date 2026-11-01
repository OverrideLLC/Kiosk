package app.kiosksync.app.navigation

import androidx.navigation3.runtime.NavKey
import app.kiosksync.core.common.app.AppRole

/** Startup root. A missing role opens selection; a saved or forced role opens that home. */
internal fun startupRoute(role: AppRole?): AppRoute = when (role) {
    null -> AppRoute.RoleSelection
    AppRole.EMITTER -> AppRoute.EmitterHome
    AppRole.RECEIVER -> AppRoute.ReceiverHome
}

/**
 * Replaces the session root. Confirming a role and resetting it both land here.
 * The same route is left untouched so a repeated effect does not rebuild the entry.
 */
internal fun replaceRoute(stack: MutableList<NavKey>, route: AppRoute) {
    if (stack.size == 1 && stack.last() == route) return
    stack.clear()
    stack.add(route)
}

/**
 * Back never removes the session root. Esc on the Screen opens options in the
 * window host; it does not return to role selection.
 */
internal fun onBack(stack: MutableList<NavKey>) {
    if (stack.size > 1) stack.removeAt(stack.lastIndex)
}

internal fun currentRoute(stack: List<NavKey>): AppRoute = stack.last() as AppRoute
