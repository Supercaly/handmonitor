package com.handmonitor.wear.repository

import com.handmonitor.wear.data.HandEvent
import com.handmonitor.wear.data.HandEventType
import com.handmonitor.wear.database.AppDatabase
import com.handmonitor.wear.database.dao.HandEventDao
import io.mockk.*
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class HandEventsRepositoryTest {
    @MockK
    private lateinit var mDb: AppDatabase

    @MockK
    private lateinit var mDao: HandEventDao
    private lateinit var mRepository: HandEventsRepository

    @BeforeEach
    fun setup() {
        mRepository = HandEventsRepository(mDb)
        every { mDb.handEventDao() } returns mDao
        coEvery { mDao.insertEvent(any()) } just Runs
    }

    @Test
    fun `addNewEvent insert new data to the database`() {
        val event = HandEvent(0, HandEventType.WASHING, 5, 1L, 2L)
        runBlocking { mRepository.addNewEvent(event) }

        coVerify {
            mDao.insertEvent(event)
        }
    }
}