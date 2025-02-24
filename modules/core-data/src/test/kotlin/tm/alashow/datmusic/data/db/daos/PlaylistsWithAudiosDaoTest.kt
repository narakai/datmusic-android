/*
 * Copyright (C) 2021, Alashov Berkeli
 * All rights reserved.
 */
package tm.alashow.datmusic.data.db.daos

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import javax.inject.Inject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runBlockingTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import tm.alashow.base.testing.BaseTest
import tm.alashow.datmusic.data.SampleData
import tm.alashow.datmusic.data.db.AppDatabase
import tm.alashow.datmusic.data.db.DatabaseModule

@UninstallModules(DatabaseModule::class)
@HiltAndroidTest
class PlaylistsWithAudiosDaoTest : BaseTest() {

    @Inject lateinit var database: AppDatabase
    @Inject lateinit var playlistsDao: PlaylistsDao
    @Inject lateinit var audiosDao: AudiosDao
    @Inject lateinit var dao: PlaylistsWithAudiosDao

    private val testItems = (1..5).map { SampleData.playlistAudioItems() }

    @Before
    override fun setUp() {
        super.setUp()

        runBlockingTest {
            // pre-insert playlists & audios
            // because in tests we're assuming relations already exist
            playlistsDao.insertAll(testItems.map { it.playlist })
            audiosDao.insertAll(testItems.map { it.audio })
        }
    }

    @After
    override fun tearDown() {
        super.tearDown()
        database.close()
    }

    @Test
    fun insert() = testScope.runBlockingTest {
        val item = testItems.first().playlistAudio
        dao.insert(item)

        assertThat(dao.getById(item.id)).isEqualTo(item)
    }

    @Test
    fun insertAll() = testScope.runBlockingTest {
        val items = testItems.map { it.playlistAudio }
        dao.insertAll(items)

        val insertedItems = dao.getByIds(items.map { it.id })
        assertThat(insertedItems).containsExactlyElementsIn(items)
    }

    @Test
    fun updateAll() = testScope.runBlockingTest {
        var items = testItems.map { it.playlistAudio }
        dao.insertAll(items)

        items = testItems.mapIndexed { index, it -> it.playlistAudio.copy(position = index) }
        dao.updateAll(items)

        val updatedItems = dao.getByIds(items.map { it.id })
        assertThat(updatedItems).containsExactlyElementsIn(items)
    }

    @Test
    fun deletePlaylistItems() = testScope.runBlockingTest {
        val items = testItems.map { it.playlistAudio }
        val ids = items.map { it.id }
        dao.insertAll(items)

        val idsToDelete = ids.shuffled().take(3)
        dao.deletePlaylistItems(idsToDelete)

        assertThat(dao.getByIds(idsToDelete)).isEmpty()
    }

    @Test
    fun deleteAll() = testScope.runBlockingTest {
        val items = testItems.map { it.playlistAudio }
        dao.insertAll(items)
        dao.deleteAll()

        assertThat(dao.getAll()).isEmpty()
    }

    @Test
    fun getAll() = testScope.runBlockingTest {
        val items = testItems.map { it.playlistAudio }
        dao.insertAll(items)

        assertThat(dao.getAll()).containsExactlyElementsIn(items)
    }

    @Test
    fun getByPosition() = testScope.runBlockingTest {
        val item = testItems.first().playlistAudio.copy(position = 1000)
        dao.insert(item)

        assertThat(dao.getByPosition(item.playlistId, item.position))
            .isEqualTo(item)
    }

    @Test
    fun getById() = testScope.runBlockingTest {
        val item = testItems.first().playlistAudio
        dao.insert(item)

        assertThat(dao.getById(item.id))
            .isEqualTo(item)
    }

    @Test
    fun getByIds() = testScope.runBlockingTest {
        val items = testItems.map { it.playlistAudio }
        dao.insertAll(items)

        assertThat(dao.getByIds(items.map { it.id })).containsExactlyElementsIn(items)
    }

    @Test
    fun distinctAudios() = testScope.runBlockingTest {
        val audioIds = testItems.also { audiosDao.insertAll(it.map { it.audio }) }.map { it.audio.id }
        val items = testItems.map { it.playlistAudio.copy(audioId = it.audio.id) }
        dao.insertAll(items)
        // insert playlist items with same audio ids but different playlistAudioId and position
        dao.insertAll(items.mapIndexed { index, it -> it.copy(id = items.size + index.toLong(), position = items.size + index) })

        assertThat(dao.playlistAudios().first().map { it.audioId })
            .containsExactlyElementsIn(audioIds + audioIds)
        assertThat(dao.distinctAudios())
            .containsExactlyElementsIn(audioIds)
    }

    @Test
    fun lastPlaylistAudioPosition() = testScope.runBlockingTest {
        val playlistId = testItems.first().playlist.id
        val items = testItems.mapIndexed { index, it -> it.playlistAudio.copy(playlistId = playlistId, position = index) }.take(2)
        dao.insertAll(items)

        assertThat(dao.lastPlaylistAudioPosition(playlistId))
            .isEqualTo(items.last().position)
    }

    @Test
    fun playlistItems() = testScope.runBlockingTest {
        val playlistId = testItems.first().playlist.id
        val orderedItems = testItems.mapIndexed { index, it -> it.playlistAudio.copy(playlistId = playlistId, position = index) }
        dao.insertAll(orderedItems)

        dao.playlistItems(playlistId).test {
            assertThat(awaitItem().map { it.playlistAudio }).isEqualTo(orderedItems)
        }
    }

    @Test
    fun playlistAudios() = testScope.runBlockingTest {
        val items = testItems.map { it.playlistAudio }
        dao.insertAll(items)

        dao.playlistAudios().test {
            assertThat(awaitItem()).containsExactlyElementsIn(items)
        }
    }

    @Test
    fun updatePlaylistAudio() = testScope.runBlockingTest {
        val item = testItems.first().playlistAudio
        dao.insert(item)

        val newPlaylistId = testItems.last().playlist.id
        val updatedCopy = item.copy(playlistId = newPlaylistId, position = 100)
        dao.updatePlaylistAudio(updatedCopy)

        assertThat(dao.getById(updatedCopy.id)).isEqualTo(updatedCopy)
    }
}
