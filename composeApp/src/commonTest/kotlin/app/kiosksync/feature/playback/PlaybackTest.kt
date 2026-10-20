package app.kiosksync.feature.playback

import androidx.compose.ui.graphics.ImageBitmap
import app.kiosksync.FakeLocalStorage
import app.kiosksync.core.AppLogger
import app.kiosksync.core.MAX_DURATION_SEC
import app.kiosksync.core.MIN_DURATION_SEC
import app.kiosksync.feature.playlist.Playlist
import app.kiosksync.feature.playlist.PlaylistItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

class TestLogger : AppLogger {
    val messages = mutableListOf<String>()
    override fun info(message: String) { messages.add("INFO: $message") }
    override fun warn(message: String, throwable: Throwable?) { messages.add("WARN: $message") }
    override fun error(message: String, throwable: Throwable?) { messages.add("ERROR: $message") }
    override fun debug(message: String) { messages.add("DEBUG: $message") }
}

@OptIn(ExperimentalCoroutinesApi::class)
class PlaybackTest {

    @Test
    fun testPlaylistDurationCalculation() {
        val playlist = Playlist(defaultDurationSec = 10)

        // Item with null duration -> uses default 10s
        val itemDefault = PlaylistItem("1", "img1.png", "sha", null)
        assertEquals(10, playlist.getEffectiveDurationSec(itemDefault))

        // Item with custom valid duration -> uses custom 15s
        val itemCustom = PlaylistItem("2", "img2.png", "sha", 15)
        assertEquals(15, playlist.getEffectiveDurationSec(itemCustom))

        // Item with duration < MIN_DURATION_SEC (e.g. 1s) -> clamped to 3s
        val itemTooShort = PlaylistItem("3", "img3.png", "sha", 1)
        assertEquals(MIN_DURATION_SEC, playlist.getEffectiveDurationSec(itemTooShort))

        // Item with duration > MAX_DURATION_SEC (e.g. 1000s) -> clamped to 600s
        val itemTooLong = PlaylistItem("4", "img4.png", "sha", 1000)
        assertEquals(MAX_DURATION_SEC, playlist.getEffectiveDurationSec(itemTooLong))
    }

    @Test
    fun testPlaylistSerialization() {
        val playlist = Playlist(
            version = 3,
            defaultDurationSec = 12,
            items = listOf(
                PlaylistItem("id-1", "promo1.jpg", "hash1", null),
                PlaylistItem("id-2", "promo2.png", "hash2", 20)
            )
        )
        val json = Json { prettyPrint = true }
        val encoded = json.encodeToString(Playlist.serializer(), playlist)
        val decoded = json.decodeFromString<Playlist>(encoded)

        assertEquals(playlist.version, decoded.version)
        assertEquals(playlist.defaultDurationSec, decoded.defaultDurationSec)
        assertEquals(2, decoded.items.size)
        assertEquals("promo1.jpg", decoded.items[0].fileName)
        assertEquals(20, decoded.items[1].durationSec)
    }

    @Test
    fun testPlaybackLoopSkipsDamagedImage() = runTest {
        val dummyBitmap = androidx.compose.ui.graphics.ImageBitmap(1, 1)
        val storage = FakeLocalStorage(
            playlist = Playlist(
                version = 1,
                items = listOf(
                    PlaylistItem("damaged", "bad.png", "sha-bad", 5),
                    PlaylistItem("healthy", "good.png", "sha-good", 5)
                )
            ),
            availableBitmaps = mutableMapOf("healthy" to dummyBitmap)
        )
        val logger = TestLogger()

        val stateHolder = PlaybackStateHolder(
            storage,
            logger,
            this,
            backgroundDispatcher = Dispatchers.Unconfined
        )
        stateHolder.start()
        runCurrent()

        // It should skip "damaged" and display "healthy"
        val state = stateHolder.uiState.value
        assertTrue(state.hasItems)
        assertEquals("healthy", state.currentItem?.mediaId)
        assertEquals(dummyBitmap, state.currentBitmap)

        stateHolder.stop()
    }

    @Test
    fun testPlaybackLoopReusesBitmapForRepeatedContent() = runTest {
        val dummyBitmap = ImageBitmap(1, 1)
        val storage = FakeLocalStorage(
            playlist = Playlist(
                version = 1,
                items = listOf(
                    PlaylistItem("first", "first.png", "same-sha", 3),
                    PlaylistItem("second", "second.png", "same-sha", 3)
                )
            ),
            availableBitmaps = mutableMapOf(
                "first" to dummyBitmap,
                "second" to dummyBitmap
            )
        )

        val stateHolder = PlaybackStateHolder(
            storage,
            TestLogger(),
            this,
            backgroundDispatcher = Dispatchers.Unconfined
        )
        stateHolder.start()
        runCurrent()

        val firstBitmap = stateHolder.uiState.value.currentBitmap
        assertEquals(1, storage.loadMediaBitmapCount)

        advanceTimeBy(3_000)
        runCurrent()

        assertEquals("second", stateHolder.uiState.value.currentItem?.mediaId)
        assertSame(firstBitmap, stateHolder.uiState.value.currentBitmap)
        assertEquals(1, storage.loadMediaBitmapCount)

        stateHolder.stop()
    }
}
