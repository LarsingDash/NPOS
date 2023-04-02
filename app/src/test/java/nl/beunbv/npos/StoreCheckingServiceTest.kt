package nl.beunbv.npos

import nl.beunbv.npos.data.Store
import nl.beunbv.npos.notification.StoreCheckingService
import org.junit.Assert.assertEquals
import org.junit.Test
import org.osmdroid.util.GeoPoint

class StoreCheckingServiceTest {
    @Test
    fun compareTimeTest() {
        //Same hour positive
        assertEquals(
            true, StoreCheckingService.compareTimes(
                Pair(10, 30),
                Pair(10, 20)
            )
        )

        //Same hour negative
        assertEquals(
            false, StoreCheckingService.compareTimes(
                Pair(10, 30),
                Pair(10, 21)
            )
        )

        //Different hour positive
        assertEquals(
            true, StoreCheckingService.compareTimes(
                Pair(11, 0),
                Pair(10, 50)
            )
        )

        //Different hour negative - 1
        assertEquals(
            false, StoreCheckingService.compareTimes(
                Pair(11, 0),
                Pair(10, 51)
            )
        )

        //Different hour negative - 2
        assertEquals(
            false, StoreCheckingService.compareTimes(
                Pair(11, 0),
                Pair(10, 0)
            )
        )
    }

    @Test
    fun checkTimeTest() {
        val testStore = Store(
            0,
            "",
            GeoPoint(0.0, 0.0),
            Pair(12, 34),
            Pair(21, 0),
            listOf()
        )

        //Open positive
        assertEquals(
            true, StoreCheckingService.checkTime(
                testStore,
                Pair(12, 24),
                null
            )
        )

        //Open negative
        assertEquals(
            false, StoreCheckingService.checkTime(
                testStore,
                Pair(11, 24),
                null
            )
        )

        //Close positive
        assertEquals(
            true, StoreCheckingService.checkTime(
                testStore,
                Pair(20, 50),
                null
            )
        )

        //Close positive
        assertEquals(
            false, StoreCheckingService.checkTime(
                testStore,
                Pair(21, 50),
                null
            )
        )
    }
}