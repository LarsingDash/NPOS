package nl.beunbv.npos.view

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavController
import nl.beunbv.npos.MainActivity
import nl.beunbv.npos.R
import nl.beunbv.npos.model.Messages
import nl.beunbv.npos.model.NotificationModel
import nl.beunbv.npos.model.StoreModel
import nl.beunbv.npos.viewModel.LocationViewModel
import nl.beunbv.npos.viewModel.RoadManagerViewModel
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.IconOverlay
import org.osmdroid.views.overlay.ItemizedIconOverlay
import org.osmdroid.views.overlay.OverlayItem
import org.osmdroid.views.overlay.Polyline

private lateinit var mapView: MapView
private lateinit var storeOverlay: ItemizedIconOverlay<StoreOverlayItem>
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

    //UI init
    storeOverlay = createStoreOverlay(
        context = context,
        navController = navController
    )
    mapView = remember { MapView(context) }

    init(
        context = context,
        store = store
    )

    UI(store = store)
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

@Composable
fun UI(
    store: MutableState<StoreModel?>,
) {
    //OSMMap component
    OSMMap()

    //Page UI
    Row(
        modifier = Modifier.fillMaxSize(),
    ) {
        //If no route is selected: "center me" button fullscreen, otherwise split evenly
        val weight = store.value?.let { 0.5f } ?: run { 1f }

        //"Center me" button
        Box(
            modifier = Modifier
                .weight(weight = weight)
                .padding(
                    start = 10.dp,
                    top = 10.dp,
                    end = 5.dp,
                    bottom = 0.dp
                )
                .clip(shape = RoundedCornerShape(size = 7.5.dp))
                .background(color = Color(color = 0xFF6200EE))
                .clickable {
                    //Center user on mapView
                    recenter(
                        geoPoint = LocationViewModel.getUserLocation(),
                        isInstant = false
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Text(
                modifier = Modifier.padding(
                    horizontal = 10.dp,
                    vertical = 5.dp
                ),
                textAlign = TextAlign.Center,
                text = "Centreer mij",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )
        }

        //If a route is active: display "Center store" button
        store.value?.let {
            Box(
                modifier = Modifier
                    .weight(weight = weight)
                    .padding(
                        start = 5.dp,
                        top = 10.dp,
                        end = 10.dp,
                        bottom = 0.dp
                    )
                    .clip(shape = RoundedCornerShape(size = 7.5.dp))
                    .background(color = Color(color = 0xFF6200EE))
                    .clickable {
                        store.value?.let {
                            //Center selected store on mapView
                            val temp = store.value as StoreModel
                            recenter(
                                geoPoint = temp.location,
                                isInstant = false
                            )
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    modifier = Modifier.padding(
                        horizontal = 10.dp,
                        vertical = 5.dp
                    ),
                    textAlign = TextAlign.Center,
                    text = "Centreer winkel",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
            }
        }
    }
}

@Composable
fun OSMMap() {
    //Setup mapView
    mapView.setMultiTouchControls(true)
    mapView.zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)

    //Setup Map/Android View
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = {
            //Configure mapView settings
            mapView.apply {
                minZoomLevel = 12.5
                maxZoomLevel = 20.0
                isTilesScaledToDpi = true

                controller.setCenter(LocationViewModel.getUserLocation())
                controller.setZoom(17.0)

                //Add storeOverlay
                mapView.overlays.add(element = storeOverlay)
            }
        },
    )

    //Define lifecycle of mapView
    MapLifecycle(mapView = mapView)
}

@Composable
private fun createStoreOverlay(
    context: Context,
    navController: NavController,
): ItemizedIconOverlay<StoreOverlayItem> {
    return remember {
        val listener = object : ItemizedIconOverlay.OnItemGestureListener<StoreOverlayItem> {
            //Single tap
            override fun onItemSingleTapUp(index: Int, item: StoreOverlayItem?): Boolean {
                item?.store?.let {
                    MainActivity.unfoldedStore = it.id
                    navController.navigate(route = Pages.Search.title)
                    currentPage.value = Pages.Search.title
                }
                return true
            }

            //Hold (not in use)
            override fun onItemLongPress(index: Int, item: StoreOverlayItem?): Boolean {
                return false
            }
        }

        //Create and return overlay
        ItemizedIconOverlay(
            mutableListOf<StoreOverlayItem>(),
            context.getDrawable(R.drawable.albert_heijn),
            listener,
            context
        )
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

//Defines the lifecycle of the given mapview
@Composable
private fun MapLifecycle(mapView: MapView) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle

    DisposableEffect(mapView) {
        val mapLifecycleObserver = mapView.lifecycleObserver()

        lifecycle.addObserver(mapLifecycleObserver)

        onDispose {
            lifecycle.removeObserver(mapLifecycleObserver)
        }
    }
}

private fun MapView.lifecycleObserver() = LifecycleEventObserver { _, event ->
    when (event) {
        Lifecycle.Event.ON_RESUME -> this.onResume()
        Lifecycle.Event.ON_PAUSE -> this.onPause()
        else -> {}
    }
}

//Wrapper for OverlayItem, specific to stores
private class StoreOverlayItem(
    val store: StoreModel,
    storeName: String
) : OverlayItem(storeName, null, store.location)