package nl.beunbv.npos.viewModel

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import nl.beunbv.npos.model.StoreModel
import org.osmdroid.util.GeoPoint
import java.util.*

class SearchScreenViewModel {
    companion object {
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
    }
}