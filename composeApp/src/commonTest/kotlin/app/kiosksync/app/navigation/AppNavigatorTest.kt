package app.kiosksync.app.navigation

import androidx.navigation3.runtime.NavKey
import app.kiosksync.app.navigation.emitter.EmitterHomeNavigator
import app.kiosksync.app.navigation.receiver.ReceiverHomeNavigator
import app.kiosksync.app.navigation.role.RoleNavigator
import app.kiosksync.app.navigation.routes.AppRoute
import app.kiosksync.app.navigation.routes.activeRole
import app.kiosksync.app.navigation.session.SessionNavigator
import app.kiosksync.core.common.app.AppRole
import kotlin.test.Test
import kotlin.test.assertEquals

class AppNavigatorTest {

    @Test
    fun startupRouteFollowsTheSavedOrForcedRole() {
        val role = navigators().role
        assertEquals(AppRoute.RoleSelection, role.start(null))
        assertEquals(AppRoute.EmitterHome, role.start(AppRole.EMITTER))
        assertEquals(AppRoute.ReceiverHome, role.start(AppRole.RECEIVER))
    }

    @Test
    fun confirmingARoleReplacesTheRoot() {
        val stack = mutableListOf<NavKey>(AppRoute.RoleSelection)
        val graphs = navigators(stack)

        graphs.role.confirm(AppRole.EMITTER)

        assertEquals(1, stack.size)
        assertEquals(AppRoute.EmitterHome, graphs.session.current)
        assertEquals(AppRole.EMITTER, activeRole(graphs.session.current))
    }

    @Test
    fun resettingTheRoleReplacesTheRootWithSelection() {
        val stack = mutableListOf<NavKey>(AppRoute.ReceiverHome)
        val graphs = navigators(stack)

        graphs.role.show()

        assertEquals(1, stack.size)
        assertEquals(AppRoute.RoleSelection, graphs.session.current)
        assertEquals(null, activeRole(graphs.session.current))
    }

    @Test
    fun replacingTheCurrentRootDoesNothing() {
        val stack = object : MutableList<NavKey> by mutableListOf<NavKey>(AppRoute.EmitterHome) {
            override fun clear() = error("replaced")
        }
        val graphs = navigators(stack)

        graphs.emitter.show()

        assertEquals(1, stack.size)
        assertEquals(AppRoute.EmitterHome, graphs.session.current)
    }

    @Test
    fun backKeepsTheOnlyRoot() {
        val stack = mutableListOf<NavKey>(AppRoute.ReceiverHome)
        val graphs = navigators(stack)

        graphs.session.back()

        assertEquals(1, stack.size)
        assertEquals(AppRoute.ReceiverHome, graphs.session.current)
    }

    @Test
    fun backDropsEntriesAboveTheRoot() {
        val stack = mutableListOf<NavKey>(AppRoute.RoleSelection, AppRoute.EmitterHome)
        val graphs = navigators(stack)

        graphs.session.back()
        graphs.session.back()

        assertEquals(1, stack.size)
        assertEquals(AppRoute.RoleSelection, graphs.session.current)
    }

    private fun navigators(stack: MutableList<NavKey> = mutableListOf()) = Navigators(stack)

    private class Navigators(stack: MutableList<NavKey>) {
        val session = SessionNavigator(stack)
        val emitter = EmitterHomeNavigator(session)
        val receiver = ReceiverHomeNavigator(session)
        val role = RoleNavigator(session, emitter, receiver)
    }
}
