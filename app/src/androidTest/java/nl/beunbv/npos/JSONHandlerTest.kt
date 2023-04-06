package nl.beunbv.npos

import android.content.Context
import androidx.test.platform.app.InstrumentationRegistry
import nl.beunbv.npos.data.JSONHandler
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class JSONHandlerTest {
    private lateinit var testContext: Context

    @Before
    fun setup() {
        testContext = InstrumentationRegistry.getInstrumentation().targetContext
    }

    @Test
    fun readStoresTest() {
        val jsonHandler = JSONHandler(
            productsStream = testContext.resources.openRawResource(R.raw.test_products),
            storesStream = testContext.resources.openRawResource(R.raw.test_stores),
        )

        val stores = jsonHandler.stores
        val expectedList = listOf(stores[0], stores[1], stores[2])
        val expectedNames = arrayListOf<String>()
        for (store in expectedList) {
            expectedNames.add(element = store.name)
        }

        val readList =
            jsonHandler.readStores(stream = testContext.resources.openRawResource(R.raw.test_stores))
        val readNames = arrayListOf<String>()
        for (store in readList) {
            readNames.add(element = store.name)
        }

        assertEquals(expectedNames, readNames)
    }

    @Test
    fun readProductsTest() {
        val jsonHandler = JSONHandler(
            productsStream = testContext.resources.openRawResource(R.raw.test_products),
            storesStream = testContext.resources.openRawResource(R.raw.test_stores),
        )

        val expectedNames = arrayListOf("Product - 0", "Product - 1", "Product - 2")
        val readList =
            jsonHandler.readProducts(stream = testContext.resources.openRawResource(R.raw.test_products))
        val readNames = arrayListOf<String>()
        for (product in readList) {
            readNames.add(element = product.name)
        }

        assertEquals(expectedNames, readNames)
    }
}