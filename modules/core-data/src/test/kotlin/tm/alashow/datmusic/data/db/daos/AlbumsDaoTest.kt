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
import kotlinx.coroutines.test.runBlockingTest
import org.junit.After
import org.junit.Test
import tm.alashow.base.testing.BaseTest
import tm.alashow.datmusic.data.DatmusicSearchParams
import tm.alashow.datmusic.data.SampleData
import tm.alashow.datmusic.data.db.AppDatabase
import tm.alashow.datmusic.data.db.DatabaseModule
import tm.alashow.datmusic.domain.entities.Album

@UninstallModules(DatabaseModule::class)
@HiltAndroidTest
class AlbumsDaoTest : BaseTest() {

    @Inject
    lateinit var database: AppDatabase

    @Inject
    lateinit var dao: AlbumsDao

    private val testItems = (1..5).map { SampleData.album() }
    private val testParams = DatmusicSearchParams("test")
    private val entriesComparator = compareBy(Album::page, Album::searchIndex)

    @After
    override fun tearDown() {
        super.tearDown()
        database.close()
    }

    @Test
    fun entries() = testScope.runBlockingTest {
        val items = testItems.sortedWith(entriesComparator)
        dao.insertAll(items)

        dao.entries().test {
            assertThat(awaitItem()).isEqualTo(items)
        }
    }

    @Test
    fun entries_onSamePage() = testScope.runBlockingTest {
        val items = testItems.map { it.copy(page = 0) }.sortedWith(entriesComparator)
        dao.insertAll(items)

        dao.entries().test {
            assertThat(awaitItem()).isEqualTo(items)
        }
    }

    @Test
    fun entries_withParams() = testScope.runBlockingTest {
        val items = testItems.map { it.copy(params = testParams.toString()) }
            .sortedWith(entriesComparator)
        dao.insertAll(items)

        dao.entries(testParams).test {
            assertThat(awaitItem()).isEqualTo(items)
        }
    }

    @Test
    fun entries_withParams_onSamePage() = testScope.runBlockingTest {
        val items = testItems.map { it.copy(params = testParams.toString(), page = 0) }
            .sortedWith(entriesComparator)
        dao.insertAll(items)

        dao.entries(testParams).test {
            assertThat(awaitItem()).isEqualTo(items)
        }
    }

    @Test
    fun entries_withParamsAndPage() = testScope.runBlockingTest {
        val page = 2
        val items = testItems.map { it.copy(params = testParams.toString(), page = page) }
            .sortedWith(entriesComparator)
        dao.insertAll(items)

        dao.entries(testParams, page).test {
            assertThat(awaitItem()).isEqualTo(items)
        }
        dao.entries(testParams, page + 1).test {
            assertThat(awaitItem()).isEmpty()
        }
    }

    @Test
    fun entries_withCountAndOffset() = testScope.runBlockingTest {
        val items = testItems.map { it.copy(params = testParams.toString()) }
            .sortedWith(entriesComparator)
        dao.insertAll(items)

        val count = 1
        val offset = 2
        dao.entries(count = count, offset = offset).test {
            assertThat(awaitItem()).isEqualTo(items.drop(offset).take(count))
        }
    }

    @Test
    fun entry() = testScope.runBlockingTest {
        val item = testItems.first()
        dao.insert(item)

        dao.entry(item.id).test {
            assertThat(awaitItem()).isEqualTo(item)
        }
    }

    @Test
    fun entryNullable() = testScope.runBlockingTest {
        val item = testItems.first()
        dao.entryNullable(item.id).test {
            assertThat(awaitItem()).isNull()
        }
    }

    @Test
    fun entriesById() = testScope.runBlockingTest {
        dao.insertAll(testItems)

        dao.entriesById(testItems.map { it.id }).test {
            assertThat(awaitItem()).containsExactlyElementsIn(testItems)
        }
    }

    @Test
    fun delete() = testScope.runBlockingTest {
        val item = testItems.first()
        dao.insert(item)
        dao.delete(item.id)

        assertThat(dao.exists(item.id)).isEqualTo(0)
    }

    @Test
    fun delete_withParams() = testScope.runBlockingTest {
        val item = testItems.first().copy(params = testParams.toString())
        dao.insert(item)
        dao.delete(testParams)

        assertThat(dao.exists(item.id)).isEqualTo(0)
    }

    @Test
    fun delete_withParamsAndPage() = testScope.runBlockingTest {
        val page = 2
        val items = testItems.map { it.copy(params = testParams.toString(), page = page) }
        dao.insertAll(items)
        dao.delete(testParams, page)

        dao.entriesById(items.map { it.id }).test {
            assertThat(awaitItem()).isEmpty()
        }
    }

    @Test
    fun deleteAll() = testScope.runBlockingTest {
        dao.insertAll(testItems)
        dao.deleteAll()

        assertThat(dao.count()).isEqualTo(0)
    }

    @Test
    fun count() = testScope.runBlockingTest {
        dao.insertAll(testItems)

        assertThat(dao.count()).isEqualTo(testItems.size)
    }

    @Test
    fun observeCount() = testScope.runBlockingTest {
        dao.insertAll(testItems)

        dao.observeCount().test {
            assertThat(awaitItem()).isEqualTo(testItems.size)
            dao.insert(SampleData.album())
            assertThat(awaitItem()).isEqualTo(testItems.size + 1)
        }
    }

    @Test
    fun count_withParams() = testScope.runBlockingTest {
        val paramlessItems = (1..5).map { SampleData.album() }
        val items = testItems.map { it.copy(params = testParams.toString()) }

        dao.insertAll(paramlessItems)
        dao.insertAll(items)

        assertThat(dao.count()).isEqualTo(paramlessItems.size + items.size)
        assertThat(dao.count(testParams)).isEqualTo(items.size)
    }

    @Test
    fun exists() = testScope.runBlockingTest {
        val item = testItems.first()
        dao.insert(item)

        assertThat(dao.exists(item.id)).isEqualTo(1)
    }

    @Test
    fun has() = testScope.runBlockingTest {
        val item = testItems.first()
        dao.insert(item)

        dao.has(item.id).test {
            assertThat(awaitItem()).isEqualTo(1)
            dao.delete(item.id)
            assertThat(awaitItem()).isEqualTo(0)
        }
    }
}
