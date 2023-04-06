package nl.beunbv.npos.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import nl.beunbv.npos.MainActivity
import nl.beunbv.npos.data.Store
import nl.beunbv.npos.ui.components.StoreItem
import org.osmdroid.bonuspack.routing.OSRMRoadManager
import org.osmdroid.bonuspack.routing.RoadManager
import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint
import java.util.*

lateinit var fullList: MutableState<List<Store>>
lateinit var searchBarValue: MutableState<TextFieldValue>


@Composable
fun SearchScreen(
    storeID: Int?,
    navController: NavController,
) {
    val context = LocalContext.current
    roadManager = OSRMRoadManager(context, Configuration.getInstance().userAgentValue)
    (roadManager as OSRMRoadManager).setMean(OSRMRoadManager.MEAN_BY_FOOT)

    fullList = remember {
        mutableStateOf(value = MainActivity.jsonHandler.stores)
    }
    searchBarValue = remember { mutableStateOf(TextFieldValue("")) }

    var openedStore: Store? by remember { mutableStateOf(null) }
    var preOpenedStoreID by remember { mutableStateOf(storeID) }

    val arrayList = arrayListOf<Store>()
    arrayList.addAll(elements = fullList.value)

    val searchList = reformatList(
        list = arrayList,
        searchValue = searchBarValue.value.text,
        roadManager = roadManager,
        userLocation = MainActivity.userLocation
    )

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        TextField(
            modifier = Modifier.fillMaxWidth(),
            value = searchBarValue.value,
            onValueChange = { newValue ->
                searchBarValue.value = newValue
            },
            placeholder = {
                Text(
                    text = "Type hier om te zoeken...",
                    style = MaterialTheme.typography.h1,
                    fontSize = 20.sp
                )
            },
            textStyle = LocalTextStyle.current.copy(
                fontSize = 20.sp
            ),
        )

        LazyColumn(
            modifier = Modifier
                .background(color = Color(
                    red = 240,
                    green = 240,
                    blue = 240))
                .fillMaxHeight()
        ) {
            items(count = searchList.size) { index ->
                val store = searchList[index]

                var foldout = false
                if (openedStore != null && openedStore == store) foldout = true
                if (preOpenedStoreID != null && preOpenedStoreID == store.id) foldout = true

                StoreItem(
                    store = store,
                    onFoldClick = {
                        openedStore = if (foldout) null else store
                        preOpenedStoreID = null
                    },
                    isFoldedOut = foldout,
                    navController = navController,
                )
            }
        }
    }
}

lateinit var roadManager: RoadManager
fun reformatList(
    list: ArrayList<Store>,
    searchValue: String,
    roadManager: RoadManager,
    userLocation: GeoPoint
): List<Store> {
    val map = TreeMap<Double, Store>()
    val loaders = arrayListOf<Thread>()
    list.forEach { store ->
        if (searchValue.isNotBlank()) {
            var containsFilter = false
            for (product in store.products) {
                if (product.name.contains(
                        other = searchValue,
                        ignoreCase = true)) {
                    containsFilter = true
                }

                continue
            }
            if (!containsFilter) return@forEach
        }

        val loader = Thread {
            val road = roadManager.getRoad(
                arrayListOf(userLocation, store.location)
            )

            map[road.mLength] = store
        }

        loader.start()
        loaders.add(element = loader)
    }
    loaders.forEach { thread -> thread.join() }

    list.clear()
    map.forEach { entry -> list.add(element = entry.value) }

    return list
}
