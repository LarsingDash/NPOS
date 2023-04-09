package nl.beunbv.npos.viewModel

import android.content.Context
import nl.beunbv.npos.model.LocationModel
import org.osmdroid.util.GeoPoint

class LocationViewModel {
    companion object {
        private val callbacks: ArrayList<(GeoPoint) -> Unit> = ArrayList()

        //Sets up the location model, including permissions
        fun setupUserLocation(context: Context) {
            //Request permissions, if they were already granted, init. Otherwise, wait for callback
            if (LocationModel.requestPermissions(context = context))
                init(
                    context = context,
                    isFromCallback = false
                )
        }

        //Adds a new callback to the list that is executed upon updating the location
        fun addCallback(
            callback: (GeoPoint) -> Unit
        ) {
            //Add new callback
            callbacks.add(element = callback)

            //Invoke callback in case the location doesn't get updated for some time
            callback.invoke(getUserLocation())
        }

        fun getUserLocation(): GeoPoint = LocationModel.userLocation

        fun init(
            context: Context,
            isFromCallback: Boolean
        ) {
            if (isFromCallback) LocationModel.setPermissions()
            LocationModel.init(
                context = context
            ) { newPosition -> callbacks.forEach { it.invoke(newPosition) } }
        }
    }
}