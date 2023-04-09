package nl.beunbv.npos.viewModel

import android.content.Context
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import nl.beunbv.npos.MainActivity
import nl.beunbv.npos.R
import nl.beunbv.npos.model.Messages
import nl.beunbv.npos.model.NotificationModel
import nl.beunbv.npos.model.StoreModel
import nl.beunbv.npos.view.components.MapScreenOverlay
import nl.beunbv.npos.view.components.OSMMap
import nl.beunbv.npos.view.components.StoreOverlayItem
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.IconOverlay
import org.osmdroid.views.overlay.ItemizedIconOverlay
import org.osmdroid.views.overlay.Polyline

lateinit var mapView: MapView
lateinit var storeOverlay: ItemizedIconOverlay<StoreOverlayItem>
private var locationOverlay: IconOverlay = IconOverlay()
private var routeOverlay = Polyline()

@Composable
fun MapScreenView(
    storeID: Int,
    navController: NavController,
) {
    val context = LocalContext.current

    //Remember selected store? that could have been passed through as a navigational argument
    //Null means no route is active
    val store = remember {
        mutableStateOf(if (storeID == -1) null else MainActivity.dataViewModel.stores[storeID])
    }

    OSMMap(
        context = context,
        navController = navController
    )
    MapScreenOverlay(store = store)

    init(
        context = context,
        store = store
    )
}

fun init(
    context: Context,
    store: MutableState<StoreModel?>
) {
    //Add all stores to map
    addStoreListToMap(
        StoreList = MainActivity.dataViewModel.stores,
        context = context
    )

    //Add / update user on the map
    updateUserLocation(
        geoPoint = LocationViewModel.getUserLocation(),
        context = context
    )

    //If a store was passed through, that means a route should be started
    store.value?.let {
        //Create route
        addRouteToMap(
            user = LocationViewModel.getUserLocation(),
            store = store.value!!.location,
            context = context
        )

        //Center map to destination (store)
        recenter(
            geoPoint = store.value!!.location,
            isInstant = true
        )
    }

    //Subscribe to callback from locationProvider
    LocationViewModel.addCallback { newLocation ->
        //Update user location when a new location is received
        updateUserLocation(
            geoPoint = newLocation,
            context = context
        )

        //If a route was active: add / refresh it on the map
        store.value?.let {
            //Returns true when route has been finished
            if (addRouteToMap(
                    user = newLocation,
                    store = store.value!!.location,
                    context = context
                )
            ) {

                //Push notification of the route being finished
                store.value?.let {
                    NotificationModel.postMessage(
                        storeName = it.name,
                        storeID = it.id,
                        message = Messages.ARRIVE,
                        context = context
                    )
                }

                //Clear selected store
                store.value = null
            }
        }
    }
}

//Adds the given StoreList to the storeOverlay and refreshes the map
fun addStoreListToMap(
    StoreList: List<StoreModel>,
    context: Context,
) {
    //Clear overlay
    storeOverlay.removeAllItems()

    //Create and add new overlay item for each store
    for (store in StoreList) {
        //Add new overlay item
        storeOverlay.addItem(StoreOverlayItem(store, store.name))

        //Change icon / marker if not default (albert_heijn)
        if (store.name.contains(other = "Jumbo", ignoreCase = true)) {
            storeOverlay.getItem(storeOverlay.size() - 1)
                .setMarker(context.getDrawable(R.drawable.jumbo))
        } else if (store.name.contains(other = "Coop", ignoreCase = true)) {
            storeOverlay.getItem(storeOverlay.size() - 1)
                .setMarker(context.getDrawable(R.drawable.coop))
        }
    }

    //Refresh mapView
    mapView.invalidate()
}

//Creates a route from the users position to the given stores position and adds it
fun addRouteToMap(user: GeoPoint, store: GeoPoint, context: Context): Boolean {
    //Clear overlay
    mapView.overlays.remove(element = routeOverlay)

    //RoadManager will create overlay if route wasn't finished, otherwise overlay == null
    val overlay = RoadManagerViewModel.createRoadOverlay(
        user = user,
        store = store,
        context = context
    )

    //Returns true if the user is close enough to the given store
    overlay?.let {
        //Add the overlay
        mapView.overlays.add(
            index = 0,
            element = routeOverlay
        )

        //Refresh
        mapView.invalidate()

        return false
    } ?: return true
}

//Refreshes the users location on the map
fun updateUserLocation(geoPoint: GeoPoint, context: Context) {
    //Remove current overlay
    mapView.overlays.remove(element = locationOverlay)
    //Create new overlay
    locationOverlay = IconOverlay(geoPoint, context.getDrawable(R.drawable.user))
    //Add new overlay
    mapView.overlays.add(element = locationOverlay)
    //Refresh
    mapView.invalidate()
}

//Recenter to given position (instant is only used on page transitions)
fun recenter(geoPoint: GeoPoint, isInstant: Boolean) {
    val speed = if (isInstant) 0L else 1250L
    mapView.controller.animateTo(geoPoint, 18.0, speed)
}