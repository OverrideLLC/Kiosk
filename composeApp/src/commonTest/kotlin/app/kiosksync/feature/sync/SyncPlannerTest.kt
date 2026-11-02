package app.kiosksync.feature.sync

import app.kiosksync.feature.playlist.domain.Playlist
import app.kiosksync.feature.playlist.domain.PlaylistItem
import app.kiosksync.feature.emitter.domain.RemoteMediaState
import app.kiosksync.feature.emitter.domain.RemoteSyncState
import app.kiosksync.feature.sync.domain.SyncPlanner
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SyncPlannerTest {

    private fun item(mediaId: String, hash: String) = PlaylistItem(
        mediaId = mediaId,
        fileName = "$mediaId.jpg",
        sha256 = hash
    )

    private fun playlist(vararg items: PlaylistItem) = Playlist(
        version = 2,
        defaultDurationSec = 10,
        items = items.toList()
    )

    @Test
    fun `no uploads when receiver already has every hash`() {
        val local = playlist(item("a", "hash-a"), item("b", "hash-b"))
        val remote = RemoteSyncState(
            playlistVersion = 2,
            media = listOf(
                RemoteMediaState("a", "hash-a"),
                RemoteMediaState("b", "hash-b")
            )
        )

        val plan = SyncPlanner.plan(local, remote)

        assertTrue(plan.uploads.isEmpty())
        assertTrue(plan.mediaToDelete.isEmpty())
        assertEquals(2, plan.unchangedCount)
        assertFalse(plan.hasChanges)
    }

    @Test
    fun `uploads only missing media`() {
        val local = playlist(item("a", "hash-a"), item("b", "hash-b"))
        val remote = RemoteSyncState(playlistVersion = 0, media = listOf(RemoteMediaState("a", "hash-a")))

        val plan = SyncPlanner.plan(local, remote)

        assertEquals(listOf("b"), plan.uploads.map { it.mediaId })
        assertEquals(1, plan.unchangedCount)
        assertTrue(plan.hasChanges)
    }

    @Test
    fun `uploads only media whose hash changed`() {
        val local = playlist(item("a", "hash-a"), item("b", "new-hash-b"))
        val remote = RemoteSyncState(
            playlistVersion = 0,
            media = listOf(
                RemoteMediaState("a", "hash-a"),
                RemoteMediaState("b", "old-hash-b")
            )
        )

        val plan = SyncPlanner.plan(local, remote)

        assertEquals(listOf("b"), plan.uploads.map { it.mediaId })
        assertTrue(plan.mediaToDelete.isEmpty())
    }

    @Test
    fun `lists remote media that is no longer referenced`() {
        val local = playlist(item("a", "hash-a"))
        val remote = RemoteSyncState(
            playlistVersion = 0,
            media = listOf(
                RemoteMediaState("a", "hash-a"),
                RemoteMediaState("orphan", "hash-o")
            )
        )

        val plan = SyncPlanner.plan(local, remote)

        assertTrue(plan.uploads.isEmpty())
        assertEquals(listOf("orphan"), plan.mediaToDelete)
        assertTrue(plan.hasChanges)
    }

    @Test
    fun `does not upload the same media id twice`() {
        val local = playlist(
            item("a", "hash-a"),
            item("a", "hash-a")
        )
        val remote = RemoteSyncState(playlistVersion = 0, media = emptyList())

        val plan = SyncPlanner.plan(local, remote)

        assertEquals(1, plan.uploads.size)
        assertEquals("a", plan.uploads.first().mediaId)
    }

    @Test
    fun `compares hashes ignoring case`() {
        val local = playlist(item("a", "ABCDEF"))
        val remote = RemoteSyncState(playlistVersion = 0, media = listOf(RemoteMediaState("a", "abcdef")))

        val plan = SyncPlanner.plan(local, remote)

        assertTrue(plan.uploads.isEmpty())
    }
}
