package nl.beunbv.npos.view.components

import android.content.Context
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
import nl.beunbv.npos.model.StoreModel
import nl.beunbv.npos.view.Pages
import nl.beunbv.npos.view.currentPage
import nl.beunbv.npos.viewModel.mapView
import nl.beunbv.npos.viewModel.storeOverlay
import nl.beunbv.npos.viewModel.LocationViewModel
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.ItemizedIconOverlay
import org.osmdroid.views.overlay.OverlayItem

@Composable
fun OSMMap(
    context: Context,
    navController: NavController
) {
    storeOverlay = createStoreOverlay(
        context = context,
        navController = navController
    )

    //Setup mapView
    mapView = remember { MapView(context) }
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
class StoreOverlayItem(
    val store: StoreModel,
    storeName: String
) : OverlayItem(storeName, null, store.location)