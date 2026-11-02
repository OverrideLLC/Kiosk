package app.kiosksync.app.navigation.session

import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import app.kiosksync.app.navigation.routes.AppRoute

/**
 * Owns the single session stack. Confirming a role and resetting it replace
 * the root. Back never removes that root.
 */
internal class SessionNavigator(
    stack: MutableList<NavKey>? = null,
) {
    private var stack: MutableList<NavKey>? = stack

    fun attach(stack: NavBackStack<NavKey>) {
        this.stack = stack
    }

    fun backStack(): NavBackStack<NavKey> = stack() as NavBackStack<NavKey>

    val current: AppRoute
        get() = stack().last() as AppRoute

    fun replace(route: AppRoute) {
        val stack = stack()
        if (stack.size == 1 && stack.last() == route) return
        stack.clear()
        stack.add(route)
    }

    fun back() {
        val stack = stack()
        if (stack.size > 1) stack.removeAt(stack.lastIndex)
    }

    private fun stack(): MutableList<NavKey> = checkNotNull(stack) { "Session stack is not attached" }
}
