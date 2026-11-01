package app.kiosksync.app.navigation

import androidx.navigation3.runtime.NavKey
import app.kiosksync.app.navigation.routes.AppRoute
import app.kiosksync.app.navigation.routes.activeRole
import app.kiosksync.app.navigation.session.SessionNavigator
import app.kiosksync.core.common.app.AppRole
import kotlin.test.Test
import kotlin.test.assertEquals

class AppNavigatorTest {

    @Test
    fun startupRouteFollowsTheSavedOrForcedRole() {
        val navigator = navigator()
        assertEquals(AppRoute.RoleSelection, navigator.start(null))
        assertEquals(AppRoute.EmitterHome, navigator.start(AppRole.EMITTER))
        assertEquals(AppRoute.ReceiverHome, navigator.start(AppRole.RECEIVER))
    }

    @Test
    fun confirmingARoleReplacesTheRoot() {
        val stack = mutableListOf<NavKey>(AppRoute.RoleSelection)
        val navigator = navigator(stack)

        navigator.confirm(AppRole.EMITTER)

        assertEquals(1, stack.size)
        assertEquals(AppRoute.EmitterHome, navigator.current)
        assertEquals(AppRole.EMITTER, activeRole(navigator.current))
    }

    @Test
    fun resettingTheRoleReplacesTheRootWithSelection() {
        val stack = mutableListOf<NavKey>(AppRoute.ReceiverHome)
        val navigator = navigator(stack)

        navigator.showRoleSelection()

        assertEquals(1, stack.size)
        assertEquals(AppRoute.RoleSelection, navigator.current)
        assertEquals(null, activeRole(navigator.current))
    }

    @Test
    fun replacingTheCurrentRootDoesNothing() {
        val stack = object : MutableList<NavKey> by mutableListOf<NavKey>(AppRoute.EmitterHome) {
            override fun clear() = error("replaced")
        }
        val navigator = navigator(stack)

        navigator.showEmitter()

        assertEquals(1, stack.size)
        assertEquals(AppRoute.EmitterHome, navigator.current)
    }

    @Test
    fun backKeepsTheOnlyRoot() {
        val stack = mutableListOf<NavKey>(AppRoute.ReceiverHome)
        val navigator = navigator(stack)

        navigator.back()

        assertEquals(1, stack.size)
        assertEquals(AppRoute.ReceiverHome, navigator.current)
    }

    @Test
    fun backDropsEntriesAboveTheRoot() {
        val stack = mutableListOf<NavKey>(AppRoute.RoleSelection, AppRoute.EmitterHome)
        val navigator = navigator(stack)

        navigator.back()
        navigator.back()

        assertEquals(1, stack.size)
        assertEquals(AppRoute.RoleSelection, navigator.current)
    }

    private fun navigator(stack: MutableList<NavKey> = mutableListOf()): AppNavigator {
        return AppNavigator(SessionNavigator(stack))
    }
}
