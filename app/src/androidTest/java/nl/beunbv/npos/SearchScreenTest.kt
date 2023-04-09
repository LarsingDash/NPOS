package nl.beunbv.npos

import android.content.Context
import androidx.test.platform.app.InstrumentationRegistry
import nl.beunbv.npos.viewModel.DataViewModel
import nl.beunbv.npos.model.StoreModel
import nl.beunbv.npos.viewModel.RoadManagerViewModel
import nl.beunbv.npos.viewModel.SearchScreenViewModel.Companion.reformatList
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.osmdroid.bonuspack.routing.OSRMRoadManager
import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint

class SearchScreenTest {
    private lateinit var testContext: Context

    @Before
    fun setup() {
        testContext = InstrumentationRegistry.getInstrumentation().targetContext
    }

    @Test
    fun reformatListTest() {
        val dataViewModel = DataViewModel(
            testContext.resources.openRawResource(R.raw.test_products),
            testContext.resources.openRawResource(R.raw.test_stores),
        )

        RoadManagerViewModel.init(testContext)

        val stores = dataViewModel.stores

        //Distance test
        val arrayStores1 = arrayListOf<StoreModel>()
        arrayStores1.addAll(stores)

        val expectedList1 = listOf(stores[1], stores[2], stores[0])
        val reformattedList1 = reformatList(
            arrayStores1,
            "",
            GeoPoint(51.59852, 4.79586)
        )

        assertEquals(expectedList1, reformattedList1)

        //Filter and distance Test
        val arrayStores2 = arrayListOf<StoreModel>()
        arrayStores2.addAll(stores)

        val expectedList2 = listOf(stores[2], stores[0])
        val reformattedList2 = reformatList(
            arrayStores2,
            "Product - 2",
            GeoPoint(51.59852, 4.79586)
        )

        assertEquals(expectedList2, reformattedList2)
    }
}