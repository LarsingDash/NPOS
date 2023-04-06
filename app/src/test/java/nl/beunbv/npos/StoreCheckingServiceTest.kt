package nl.beunbv.npos

import nl.beunbv.npos.data.Store
import nl.beunbv.npos.notification.StoreCheckingService
import nl.beunbv.npos.notification.StoreCheckingService.Companion.compareTimes
import org.junit.Assert.assertEquals
import org.junit.Test
import org.osmdroid.util.GeoPoint

class StoreCheckingServiceTest {
    @Test
    fun compareTimeTest() {
        //Same hour positive
        assertEquals(
            true, Pair(first = 10, second = 30).compareTimes(
                now = Pair(first = 10, second = 20)
            )
        )

        //Same hour negative
        assertEquals(
            false, Pair(first = 10, second = 30).compareTimes(
                now = Pair(first = 10, second = 21)
            )
        )

        //Different hour positive
        assertEquals(
            true, Pair(first = 11, second = 0).compareTimes(
                now = Pair(first = 10, second = 50)
            )
        )

        //Different hour negative - 1
        assertEquals(
            false, Pair(first = 11, second = 0).compareTimes(
                now = Pair(first = 10, second = 51)
            )
        )

        //Different hour negative - 2
        assertEquals(
            false, Pair(first = 11, second = 0).compareTimes(
                now = Pair(first = 10, second = 0)
            )
        )
    }

    @Test
    fun checkTimeTest() {
        val testStore = Store(
            id = 0,
            name = "",
            location = GeoPoint(0.0, 0.0),
            openTime = Pair(first = 12, second = 34),
            closeTime = Pair(first = 21, second = 0),
            products = listOf()
        )

        //Open positive
        assertEquals(
            true, StoreCheckingService.checkTime(
                store = testStore,
                currentTime = Pair(first = 12, second = 24),
                context = null
            )
        )

        //Open negative
        assertEquals(
            false, StoreCheckingService.checkTime(
                store = testStore,
                currentTime = Pair(first = 11, second = 24),
                context = null
            )
        )

        //Close positive
        assertEquals(
            true, StoreCheckingService.checkTime(
                store = testStore,
                currentTime = Pair(first = 20, second = 50),
                context = null
            )
        )

        //Close positive
        assertEquals(
            false, StoreCheckingService.checkTime(
                store = testStore,
                currentTime = Pair(first = 21, second = 50),
                context = null
            )
        )
    }
}