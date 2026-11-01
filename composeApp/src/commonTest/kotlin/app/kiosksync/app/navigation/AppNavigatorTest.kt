package app.kiosksync.app.navigation

import androidx.navigation3.runtime.NavKey
import app.kiosksync.core.common.app.AppRole
import kotlin.test.Test
import kotlin.test.assertEquals

class AppNavigatorTest {

    @Test
    fun startupRouteFollowsTheSavedOrForcedRole() {
        assertEquals(AppRoute.RoleSelection, startupRoute(null))
        assertEquals(AppRoute.EmitterHome, startupRoute(AppRole.EMITTER))
        assertEquals(AppRoute.ReceiverHome, startupRoute(AppRole.RECEIVER))
    }

    @Test
    fun confirmingARoleReplacesTheRoot() {
        val stack = mutableListOf<NavKey>(AppRoute.RoleSelection)

        replaceRoute(stack, AppRoute.EmitterHome)

        assertEquals(1, stack.size)
        assertEquals(AppRoute.EmitterHome, currentRoute(stack))
        assertEquals(AppRole.EMITTER, activeRole(currentRoute(stack)))
    }

    @Test
    fun resettingTheRoleReplacesTheRootWithSelection() {
        val stack = mutableListOf<NavKey>(AppRoute.ReceiverHome)

        replaceRoute(stack, AppRoute.RoleSelection)

        assertEquals(1, stack.size)
        assertEquals(AppRoute.RoleSelection, currentRoute(stack))
        assertEquals(null, activeRole(currentRoute(stack)))
    }

    @Test
    fun replacingTheCurrentRootDoesNothing() {
        val stack = object : MutableList<NavKey> by mutableListOf<NavKey>(AppRoute.EmitterHome) {
            override fun clear() = error("replaced")
        }

        replaceRoute(stack, AppRoute.EmitterHome)

        assertEquals(1, stack.size)
        assertEquals(AppRoute.EmitterHome, currentRoute(stack))
    }

    @Test
    fun backKeepsTheOnlyRoot() {
        val stack = mutableListOf<NavKey>(AppRoute.ReceiverHome)

        onBack(stack)

        assertEquals(1, stack.size)
        assertEquals(AppRoute.ReceiverHome, currentRoute(stack))
    }

    @Test
    fun backDropsEntriesAboveTheRoot() {
        val stack = mutableListOf<NavKey>(AppRoute.RoleSelection, AppRoute.EmitterHome)

        onBack(stack)
        onBack(stack)

        assertEquals(1, stack.size)
        assertEquals(AppRoute.RoleSelection, currentRoute(stack))
    }
}
