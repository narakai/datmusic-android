/*
 * Copyright (C) 2021, Alashov Berkeli
 * All rights reserved.
 */
package tm.alashow.datmusic.data.repos.playlist

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import javax.inject.Inject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Test
import tm.alashow.base.testing.BaseTest
import tm.alashow.base.util.extensions.swap
import tm.alashow.datmusic.data.SampleData
import tm.alashow.datmusic.data.db.AppDatabase
import tm.alashow.datmusic.data.db.DatabaseModule
import tm.alashow.datmusic.data.db.daos.AudiosDao
import tm.alashow.datmusic.domain.entities.PLAYLIST_NAME_MAX_LENGTH
import tm.alashow.datmusic.domain.entities.Playlist
import tm.alashow.i18n.DatabaseNotFoundError
import tm.alashow.i18n.ValidationErrorBlank
import tm.alashow.i18n.ValidationErrorTooLong

@HiltAndroidTest
@UninstallModules(DatabaseModule::class)
class PlaylistsRepoTest : BaseTest() {

    @Inject lateinit var database: AppDatabase
    @Inject lateinit var repo: PlaylistsRepo
    @Inject lateinit var audiosDao: AudiosDao

    private val testItems = (1..5).map { SampleData.playlist() }
    private val entriesComparator = compareByDescending(Playlist::id)

    override fun tearDown() {
        super.tearDown()
        database.close()
    }

    @Test
    fun getByName() = testScope.runBlockingTest {
        val item = testItems.first()
        repo.insert(item)

        assertThat(repo.getByName(item.name)).isEqualTo(item)
    }

    @Test
    fun playlistItems() = testScope.runBlockingTest {
        val item = testItems.first()
        val audioIds = (1..5).map { SampleData.audio() }.also { audiosDao.insertAll(it) }.map { it.id }
        val id = repo.createPlaylist(item, audioIds)

        repo.playlistItems(id).test {
            assertThat(awaitItem().map { it.audio.id })
                .isEqualTo(audioIds)
        }
    }

    @Test
    fun playlistWithAudios() = testScope.runBlockingTest {
        val item = testItems.first()
        val audioIds = (1..5).map { SampleData.audio() }.also { audiosDao.insertAll(it) }.map { it.id }
        val id = repo.createPlaylist(item, audioIds)

        repo.playlistWithItems(id).test {
            assertThat(awaitItem().items.map { it.audio.id })
                .isEqualTo(audioIds)
        }
    }

    @Test
    fun playlists() = testScope.runBlockingTest {
        repo.insertAll(testItems)

        repo.playlists().test {
            assertThat(awaitItem())
                .isEqualTo(testItems.sortedWith(entriesComparator))
        }
    }

    @Test
    fun validatePlaylistId() = testScope.runBlockingTest {
        val item = testItems.first()
        val id = repo.createPlaylist(item)

        repo.validatePlaylistId(id)
    }

    @Test(expected = DatabaseNotFoundError::class)
    fun validatePlaylistId_notFound() = testScope.runBlockingTest {
        val item = testItems.first()

        repo.validatePlaylistId(item.id)
    }

    @Test
    fun createPlaylist() = testScope.runBlockingTest {
        val item = testItems.first()
        val id = repo.createPlaylist(item)

        assertThat(repo.getByName(item.name)?.id)
            .isEqualTo(id)
    }

    @Test(expected = ValidationErrorBlank::class)
    fun `createPlaylist fails with empty playlist name`() = testScope.runBlockingTest {
        val item = testItems.first().copy(name = "")
        repo.createPlaylist(item)
    }

    @Test(expected = ValidationErrorTooLong::class)
    fun `createPlaylist fails with too long playlist name`() = testScope.runBlockingTest {
        val item = testItems.first().copy(name = "a".repeat(PLAYLIST_NAME_MAX_LENGTH + 1))
        repo.createPlaylist(item)
    }

    @Test
    fun getOrCreatePlaylist() = testScope.runBlockingTest {
        val item = testItems.first()
        repo.createPlaylist(item)
        repo.getOrCreatePlaylist(item.name)

        assertThat(repo.getByName(item.name))
            .isEqualTo(item)
    }

    @Test
    fun getOrCreatePlaylist_inexisting() = testScope.runBlockingTest {
        val item = testItems.first()
        val id = repo.getOrCreatePlaylist(item.name)

        assertThat(repo.getByName(item.name)?.id)
            .isEqualTo(id)
    }

    @Test
    fun updatePlaylist() = testScope.runBlockingTest {
        val item = testItems.first()
        repo.createPlaylist(item)

        val updatedItem = item.copy(name = "Updated name")
        repo.updatePlaylist(updatedItem)

        val updated = repo.getByName(updatedItem.name)
        assertThat(updated)
            .isEqualTo(updatedItem.copy(updatedAt = (updated ?: updatedItem).updatedAt))
    }

    @Test(expected = DatabaseNotFoundError::class)
    fun `updatePlaylist fails with inexisting playlist id`() = testScope.runBlockingTest {
        val item = testItems.first()
        repo.updatePlaylist(item)
    }

    @Test(expected = ValidationErrorBlank::class)
    fun `updatePlaylist fails with empty playlist name`() = testScope.runBlockingTest {
        val item = testItems.first()
        repo.createPlaylist(item)
        repo.updatePlaylist(item.copy(name = ""))
    }

    @Test(expected = ValidationErrorTooLong::class)
    fun `updatePlaylist fails with too long playlist name`() = testScope.runBlockingTest {
        val item = testItems.first()
        repo.createPlaylist(item)
        repo.updatePlaylist(item.copy(name = "a".repeat(PLAYLIST_NAME_MAX_LENGTH + 1)))
    }

    @Test
    fun updatePlaylistById() = testScope.runBlockingTest {
        val item = testItems.first()
        val id = repo.createPlaylist(item)

        val updatedItem = repo.updatePlaylist(id) { it.copy(name = "Updated name") }

        val updated = repo.getByName(updatedItem.name)
        assertThat(updated)
            .isEqualTo(updatedItem.copy(updatedAt = (updated ?: updatedItem).updatedAt))
    }

    @Test(expected = DatabaseNotFoundError::class)
    fun `updatePlaylistById fails with inexisting playlist id`() = testScope.runBlockingTest {
        val item = testItems.first()

        repo.updatePlaylist(item.id) { it.copy(name = "") }
    }

    @Test(expected = ValidationErrorBlank::class)
    fun `updatePlaylistById with empty playlist name`() = testScope.runBlockingTest {
        val item = testItems.first()
        val id = repo.createPlaylist(item)

        repo.updatePlaylist(id) { it.copy(name = "") }
    }

    @Test(expected = ValidationErrorTooLong::class)
    fun `updatePlaylistById with too long playlist name`() = testScope.runBlockingTest {
        val item = testItems.first()
        val id = repo.createPlaylist(item)

        repo.updatePlaylist(id) { it.copy(name = "a".repeat(PLAYLIST_NAME_MAX_LENGTH + 1)) }
    }

    @Test
    fun addAudiosToPlaylist() = testScope.runBlockingTest {
        val item = testItems.first()
        val id = repo.createPlaylist(item)

        val audioIds = (1..5).map { SampleData.audio() }.also { audiosDao.insertAll(it) }.map { it.id }
        repo.addAudiosToPlaylist(id, audioIds)

        // Check playlist audios are added have the correct positions
        repo.playlistItems(id).test {
            val playlistItems = awaitItem()
            assertThat(playlistItems.map { it.audio.id })
                .isEqualTo(audioIds)
            assertThat(playlistItems.map { it.playlistAudio.position })
                .isEqualTo(audioIds.indices.toList())
        }

        // Check playlist items are not duplicated when ignoringExisting
        repo.addAudiosToPlaylist(id, audioIds, ignoreExisting = true)
        repo.playlistItems(id).test {
            assertThat(awaitItem().map { it.audio.id })
                .isEqualTo(audioIds)
        }

        // Check more ids are added and have correct positions
        val moreAudioIds = (1..5).map { SampleData.audio() }.also { audiosDao.insertAll(it) }.map { it.id }
        repo.addAudiosToPlaylist(id, moreAudioIds)
        repo.playlistItems(id).test {
            val playlistItems = awaitItem()
            assertThat(playlistItems.map { it.audio.id })
                .isEqualTo(audioIds + moreAudioIds)
            assertThat(playlistItems.map { it.playlistAudio.position })
                .isEqualTo((audioIds + moreAudioIds).indices.toList())
        }
    }

    @Test
    fun swapPositions() = testScope.runBlockingTest {
        val item = testItems.first()
        val audioIds = (1..5).map { SampleData.audio() }.also { audiosDao.insertAll(it) }.map { it.id }
        val repositionedAudioIds = audioIds.swap(0, audioIds.size - 1)
        val id = repo.createPlaylist(item, audioIds)

        repo.swapPositions(id, 0, audioIds.size - 1)
        repo.playlistItems(id).test {
            assertThat(awaitItem().map { it.audio.id })
                .isEqualTo(repositionedAudioIds)
        }
    }

    @Test
    fun updatePlaylistItems() = testScope.runBlockingTest {
        val item = testItems.first()
        val audioIds = (1..5).map { SampleData.audio() }.also { audiosDao.insertAll(it) }.map { it.id }
        val id = repo.createPlaylist(item, audioIds)
        val shuffledPlaylistItems = repo.playlistItems(id).first()
            .shuffled()
            .mapIndexed { index, playlistItem ->
                playlistItem.copy(playlistAudio = playlistItem.playlistAudio.copy(position = index))
            }

        repo.updatePlaylistItems(shuffledPlaylistItems)

        repo.playlistItems(id).test {
            assertThat(awaitItem())
                .isEqualTo(shuffledPlaylistItems)
        }
    }

    @Test
    fun removePlaylistItems() = testScope.runBlockingTest {
        val item = testItems.first()
        val audioIds = (1..5).map { SampleData.audio() }.also { audiosDao.insertAll(it) }.map { it.id }
        val id = repo.createPlaylist(item, audioIds)

        val playlistItems = repo.playlistItems(id).first().shuffled()
        val playlistItemsToRemove = playlistItems.take(2)
        val removedItemsCount = repo.removePlaylistItems(playlistItemsToRemove.map { it.playlistAudio.id })

        assertThat(removedItemsCount)
            .isEqualTo(playlistItemsToRemove.size)

        repo.playlistItems(id).test {
            assertThat(awaitItem())
                .containsExactlyElementsIn(playlistItems.subtract(playlistItemsToRemove))
        }
    }

    @Test
    fun clearPlaylistArtwork() = testScope.runBlockingTest {
        val item = testItems.first().copy(artworkPath = "some/path")
        val id = repo.createPlaylist(item)

        repo.clearPlaylistArtwork(id)

        repo.playlist(id).test {
            assertThat(awaitItem().artworkPath).isNull()
        }
    }

    // region RoomRepo tests

    @Test
    fun entry() = testScope.runBlockingTest {
        val item = testItems.first()
        repo.insert(item)

        repo.entry(item.id).test {
            assertThat(awaitItem()).isEqualTo(item)
        }
    }

    @Test
    fun entries() = testScope.runBlockingTest {
        repo.insertAll(testItems)

        repo.entries().test {
            assertThat(awaitItem())
                .isEqualTo(testItems.sortedWith(entriesComparator))
        }
    }

    @Test
    fun entries_byId() = testScope.runBlockingTest {
        repo.insertAll(testItems)

        repo.entries(testItems.map { it.id }).test {
            assertThat(awaitItem())
                .containsExactlyElementsIn(testItems)
        }
    }

    @Test
    fun update() = testScope.runBlockingTest {
        val item = testItems.first()
        repo.insert(item)

        val updated = item.copy(
            name = "Updated Name",
            artworkSource = "Updated Source",
            artworkPath = "Updated Path",
            params = "Updated Params",
        ).updatedCopy()
        repo.update(updated)

        repo.entry(item.id).test {
            assertThat(awaitItem()).isEqualTo(updated)
        }
    }

    @Test
    fun isEmpty() = testScope.runBlockingTest {
        repo.isEmpty().test {
            assertThat(awaitItem()).isTrue()
        }

        repo.insertAll(testItems)

        repo.isEmpty().test {
            assertThat(awaitItem()).isFalse()
        }
    }

    @Test
    fun count() = testScope.runBlockingTest {
        repo.count().test {
            assertThat(awaitItem()).isEqualTo(0)
        }

        repo.insertAll(testItems)

        repo.count().test {
            assertThat(awaitItem()).isEqualTo(testItems.size)
        }
    }

    @Test
    fun has() = testScope.runBlockingTest {
        val item = testItems.first()

        repo.has(item.id).test {
            assertThat(awaitItem()).isFalse()
            repo.insert(item)
            assertThat(awaitItem()).isTrue()
        }
    }

    @Test
    fun exists() = testScope.runBlockingTest {
        val item = testItems.first()

        assertThat(repo.exists(item.id)).isFalse()
        repo.insert(item)
        assertThat(repo.exists(item.id)).isTrue()
    }

    @Test
    fun delete() = testScope.runBlockingTest {
        val item = testItems.first()
        val id = repo.createPlaylist(item)

        repo.delete(id)

        assertThat(repo.exists(id)).isFalse()
    }

    @Test
    fun deleteAll() = testScope.runBlockingTest {
        repo.insertAll(testItems)

        repo.deleteAll()

        repo.isEmpty().test {
            assertThat(awaitItem()).isTrue()
        }
    }

    // endregion
}
