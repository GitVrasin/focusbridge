package com.focusbridge.data.db

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.focusbridge.data.db.dao.UsageRecordDao
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class UsageRecordDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: UsageRecordDao

    private val today = 19900 // arbitrary epoch day for test

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = db.usageRecordDao()
    }

    @After
    fun tearDown() { db.close() }

    @Test
    fun upsert_insertsNewRecord() = runTest {
        dao.upsert("com.instagram.android", today, 500_000L, System.currentTimeMillis())
        val record = dao.getRecord("com.instagram.android", today)
        assertNotNull(record)
        assertEquals(500_000L, record!!.totalTimeMs)
    }

    @Test
    fun upsert_updatesExistingRecord() = runTest {
        dao.upsert("com.instagram.android", today, 100_000L, System.currentTimeMillis())
        dao.upsert("com.instagram.android", today, 800_000L, System.currentTimeMillis())
        val record = dao.getRecord("com.instagram.android", today)
        assertEquals(800_000L, record!!.totalTimeMs)
    }

    @Test
    fun getRecordsForDay_returnsOnlySameDayRecords() = runTest {
        val yesterday = today - 1
        dao.upsert("com.instagram.android", today, 100L, System.currentTimeMillis())
        dao.upsert("com.tiktok.android", yesterday, 200L, System.currentTimeMillis())

        val records = dao.getRecordsForDay(today)
        assertEquals(1, records.size)
        assertEquals("com.instagram.android", records.first().packageName)
    }

    @Test
    fun deleteOlderThan_removesOldRecords() = runTest {
        dao.upsert("com.instagram.android", today - 5, 100L, System.currentTimeMillis())
        dao.upsert("com.tiktok.android", today, 200L, System.currentTimeMillis())

        dao.deleteOlderThan(today)

        assertNull(dao.getRecord("com.instagram.android", today - 5))
        assertNotNull(dao.getRecord("com.tiktok.android", today))
    }
}
