package nl.beunbv.npos.ui.components

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
import nl.beunbv.npos.MainActivity
import nl.beunbv.npos.R
import nl.beunbv.npos.data.Store
import nl.beunbv.npos.ui.Pages
import nl.beunbv.npos.ui.currentPage
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
    mapView = remember { MapView(context) }
    mapView.setMultiTouchControls(true)
    mapView.zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)

    roadManager = OSRMRoadManager(context, Configuration.getInstance().userAgentValue)
    (roadManager as OSRMRoadManager).setMean(OSRMRoadManager.MEAN_BY_FOOT)

    storeOverlay = createStoreOverlay(
        context = context,
        navController
    )

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = {
            mapView.apply {
                minZoomLevel = 12.5
                maxZoomLevel = 20.0
                isTilesScaledToDpi = true

                controller.setCenter(MainActivity.userLocation)
                controller.setZoom(17.0)

                mapView.overlays.add(storeOverlay)
            }
        },
    )

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
                    navController.navigate(Pages.Search.title)
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

fun addStoreListToMap(
    StoreList: List<Store>,
    context: Context,
) {
    storeOverlay.removeAllItems()

    for (store in StoreList) {
        storeOverlay.addItem(
            StoreOverlayItem(store, store.name)
        )

        if (store.name.contains("Jumbo", true)) {
            storeOverlay.getItem(storeOverlay.size() - 1)
                .setMarker(context.getDrawable(R.drawable.jumbo))
        } else if (store.name.contains("Coop", true)) {
            storeOverlay.getItem(storeOverlay.size() - 1)
                .setMarker(context.getDrawable(R.drawable.coop))
        }
    }

    mapView.invalidate()
}

fun addRouteToMap(user: GeoPoint, store: GeoPoint, context: Context): Boolean {
    var hasFinished = false
    mapView.overlays.remove(routeOverlay)

    val loader = Thread {
        //Creation
        val road = roadManager.getRoad(arrayListOf<GeoPoint>(user, store))
        routeOverlay = RoadManager.buildRoadOverlay(road)

        //Design
        routeOverlay.outlinePaint.strokeCap = Paint.Cap.ROUND
        routeOverlay.outlinePaint.strokeWidth = 15f
        routeOverlay.outlinePaint.strokeJoin = Paint.Join.ROUND
        routeOverlay.outlinePaint.color = context.getColor(R.color.purple_500)

        if (road.mLength < 0.02) {
            hasFinished = true
        } else {
            //Add the overlay to all overlays
            mapView.overlays.add(0, routeOverlay)
            mapView.invalidate()
        }
    }

    loader.start()
    loader.join()
    return hasFinished
}

fun updateUserLocation(geoPoint: GeoPoint, context: Context) {
    mapView.overlays.remove(locationOverlay)
    locationOverlay = IconOverlay(geoPoint, context.getDrawable(R.drawable.user))
    mapView.overlays.add(locationOverlay)
    mapView.invalidate()
}

fun recenter(geoPoint: GeoPoint, isInstant: Boolean) {
    val speed = if (isInstant) 0L else 1250L
    mapView.controller.animateTo(geoPoint, 18.0, speed)
}

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

private class StoreOverlayItem(
    val store: Store,
    storeName: String
) : OverlayItem(storeName, null, store.location)