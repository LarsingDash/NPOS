package nl.beunbv.npos.data

import org.json.JSONArray
import org.json.JSONObject
import org.osmdroid.util.GeoPoint
import java.io.InputStream
import java.io.InputStreamReader

class JSONHandler(
    productsStream: InputStream,
    storesStream: InputStream
) {
    private val products: List<Product> = readProducts(productsStream)
    val stores: List<Store> = readStores(storesStream)

    //Reads products from the given InputStream (raw JSON)
    fun readProducts(stream: InputStream): List<Product> {
        val reader = InputStreamReader(stream)
        val array = JSONArray(JSONObject(reader.readText())["products"].toString())

        val products = arrayListOf<Product>()

        for (i in 0 until array.length()) {
            val currentProduct = array[i] as JSONObject

            products.add(
                element = Product(name = currentProduct["name"] as String)
            )
        }

        return products
    }

    //Reads stores from the given InputStream (raw JSON)
    fun readStores(stream: InputStream): List<Store> {
        val reader = InputStreamReader(stream)
        val array = JSONArray(JSONObject(reader.readText())["stores"].toString())

        val stores = arrayListOf<Store>()

        for (i in 0 until array.length()) {
            val currentStore = array[i] as JSONObject

            val storeProducts = (currentStore["products"] as String).split(',')
            val productsList = arrayListOf<Product>()
            for (id in storeProducts) {
                productsList.add(products[id.trim().toInt()])
            }

            val openList = (currentStore["open"] as String).split(':')
            val closeList = (currentStore["close"] as String).split(':')

            stores.add(
                element = Store(
                    id = currentStore["id"] as Int,
                    name = currentStore["name"] as String,
                    location = GeoPoint(
                        currentStore["lat"] as Double,
                        currentStore["lon"] as Double
                    ),
                    openTime = Pair(openList[0].toInt(), openList[1].toInt()),
                    closeTime = Pair(closeList[0].toInt(), closeList[1].toInt()),
                    products = productsList
                )
            )
        }

        return stores
    }
}