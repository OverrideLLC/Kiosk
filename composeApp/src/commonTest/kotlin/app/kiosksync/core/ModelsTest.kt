package app.kiosksync.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ModelsTest {

    @Test
    fun testParseAppArgsAllOptions() {
        val args = arrayOf(
            "--data-dir=C:\\custom\\data",
            "--role=receiver",
            "--port=9000",
            "--windowed"
        )
        val parsed = AppArgs.parse(args)

        assertEquals("C:\\custom\\data", parsed.dataDir)
        assertEquals(AppRole.RECEIVER, parsed.role)
        assertEquals(9000, parsed.port)
        assertTrue(parsed.windowed)
    }

    @Test
    fun testParseAppArgsEmitterRole() {
        val args = arrayOf("--role=emitter")
        val parsed = AppArgs.parse(args)

        assertEquals(AppRole.EMITTER, parsed.role)
    }
}
