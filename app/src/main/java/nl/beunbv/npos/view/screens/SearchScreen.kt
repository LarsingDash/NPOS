package nl.beunbv.npos.view.screens

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
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import nl.beunbv.npos.MainActivity
import nl.beunbv.npos.model.StoreModel
import nl.beunbv.npos.view.components.StoreItem
import nl.beunbv.npos.viewModel.LocationViewModel
import nl.beunbv.npos.viewModel.RoadManagerViewModel
import org.osmdroid.util.GeoPoint
import java.util.*

lateinit var fullList: MutableState<List<StoreModel>>
lateinit var searchBarValue: MutableState<TextFieldValue>


@Composable
fun SearchScreen(
    storeID: Int?,
    navController: NavController,
) {
    //Get full list of stores
    fullList = remember {
        mutableStateOf(value = MainActivity.dataViewModel.stores)
    }

    //Remember text written in the searchbar
    searchBarValue = remember { mutableStateOf(TextFieldValue("")) }

    //OpenedStore is from past compositions
    var openedStore: StoreModel? by remember { mutableStateOf(null) }
    //preOpenedStoreID comes from a navigational parameter
    var preOpenedStoreID by remember { mutableStateOf(storeID) }

    //Throwaway clone of fullList, this one is used to filter and sort
    val arrayList = arrayListOf<StoreModel>()
    arrayList.addAll(elements = fullList.value)

    //Filter (on searchBarValue) and sort (on distance to user) cloned list
    val searchList = reformatList(
        list = arrayList,
        searchValue = searchBarValue.value.text,
        userLocation = LocationViewModel.getUserLocation()
    )

    //Main ui
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        //Search TextField
        TextField(
            modifier = Modifier.fillMaxWidth(),
            value = searchBarValue.value,
            onValueChange = { newValue ->
                //Triggers recomposition
                searchBarValue.value = newValue
            },
            //Prompt text
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

        //Stores list
        LazyColumn(
            modifier = Modifier
                .background(
                    color = Color(
                        red = 240,
                        green = 240,
                        blue = 240
                    )
                )
                .fillMaxHeight()
        ) {
            //For every store item:
            items(count = searchList.size) { index ->
                //Get current store
                val store = searchList[index]

                //Default foldout option is folded in -> check if it needs to be unfolded
                var foldout = false
                openedStore?.let { if (openedStore == store) foldout = true }
                preOpenedStoreID?.let { if (preOpenedStoreID == store.id) foldout = true }

                //ui per store card
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

//Filters (on given searchValue) and sorts (by distance to given location) given arraylist
fun reformatList(
    list: ArrayList<StoreModel>,
    searchValue: String,
    userLocation: GeoPoint
): List<StoreModel> {
    //Treemap for automatic sorting on distance (Double)
    val map = TreeMap<Double, StoreModel>()

    //List of internet RoadManager loaders
    val loaders = ArrayList<Job>()

    //Run internet / API calls on coroutine
    runBlocking {
        //Start a loader for each store that passes the filter
        list.forEach { store ->
            //Filter if the searchValue is contained in one of the names of the products of the current store
            if (searchValue.isNotBlank()) {
                var containsFilter = false

                //Run in separate scope to gain the ability to break out of the loop
                run breaking@{
                    //Check each of the store's products' names to see if it contains the searchValue
                    store.products.forEach { product ->
                        if (product.name.contains(
                                other = searchValue,
                                ignoreCase = true
                            )
                        ) {
                            //Stop checking (it only needs one product to pass the filter)
                            containsFilter = true
                            return@breaking
                        }
                    }
                }

                //Skip the loading segment below if filter was not passed
                if (!containsFilter) return@forEach
            }

            //Load road from the OSRMRoadManager API asynchronously
            val loader = launch(Dispatchers.IO) {
                val road = RoadManagerViewModel.getRoad(
                    arrayListOf(userLocation, store.location)
                )

                //Store the length of the retrieved road in the automatically sorting map
                map[road.mLength] = store
            }
            //Add itself to the list of loaders to wait for
            loaders.add(loader)
        }

        //Wait for all loaders to finish
        loaders.forEach { job -> job.join() }
    }

    //Clear given list (just to be sure)
    list.clear()
    //Fill given list with the automatically sorted results from the filter
    map.forEach { entry -> list.add(element = entry.value) }

    //Return filtered (on given searchValue) and sorted (by distance to given location) list
    return list
}
