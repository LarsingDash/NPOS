package nl.beunbv.npos.view.components

import android.content.Context
import android.graphics.Paint
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavController
import kotlinx.coroutines.runBlocking
import nl.beunbv.npos.MainActivity
import nl.beunbv.npos.R
import nl.beunbv.npos.model.StoreModel
import nl.beunbv.npos.view.Pages
import nl.beunbv.npos.view.currentPage
import nl.beunbv.npos.viewModel.LocationViewModel
import org.osmdroid.bonuspack.routing.OSRMRoadManager
import org.osmdroid.bonuspack.routing.RoadManager
import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.IconOverlay
import org.osmdroid.views.overlay.ItemizedIconOverlay
import org.osmdroid.views.overlay.OverlayItem
import org.osmdroid.views.overlay.Polyline

private lateinit var mapView: MapView
private lateinit var roadManager: RoadManager

private lateinit var storeOverlay: ItemizedIconOverlay<StoreOverlayItem>
private var locationOverlay: IconOverlay = IconOverlay()
private var routeOverlay = Polyline()

@Composable
fun OSMMap(
    navController: NavController,
    context: Context,
) {
    //Setup mapView
    mapView = remember { MapView(context) }
    mapView.setMultiTouchControls(true)
    mapView.zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)

    //Setup roadManager
    roadManager = OSRMRoadManager(context, Configuration.getInstance().userAgentValue)
    (roadManager as OSRMRoadManager).setMean(OSRMRoadManager.MEAN_BY_FOOT)

    //Setup storeOverlay
    storeOverlay = createStoreOverlay(
        context = context,
        navController = navController
    )

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
    var hasFinished = false

    //Run internet / API call on a blocking coroutine
    runBlocking {
        //Create path / road
        val road = roadManager.getRoad(arrayListOf<GeoPoint>(user, store))
        routeOverlay = RoadManager.buildRoadOverlay(road)

        //Check if the user has reached the given store, if not -> design and add the path
        if (road.mLength < 0.02) {
            hasFinished = true
        } else {
            //Give road overlay design
            routeOverlay.outlinePaint.strokeCap = Paint.Cap.ROUND
            routeOverlay.outlinePaint.strokeWidth = 15f
            routeOverlay.outlinePaint.strokeJoin = Paint.Join.ROUND
            routeOverlay.outlinePaint.color = context.getColor(R.color.purple_500)

            //Add the overlay
            mapView.overlays.add(
                index = 0,
                element = routeOverlay
            )

            //Refresh
            mapView.invalidate()
        }
    }

    //Returns true if the user is close enough to the given store
    return hasFinished
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