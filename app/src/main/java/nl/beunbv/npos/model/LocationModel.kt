package nl.beunbv.npos.model

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Looper
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.*
import org.osmdroid.util.GeoPoint

class LocationModel {
    companion object {
        //Location
        private var hasPermissions: Boolean = false
        var userLocation = GeoPoint(51.5892100, 4.7805200)
        var lastUserLocation = GeoPoint(0.0, 0.0)

        //Sets up the location service
        //Suppression because Android Studio doesn't realise that permissions are being checked in another function
        @SuppressLint("MissingPermission")
        fun init(
            context: Context,
            viewModelCallback : (GeoPoint) -> Unit = {}
        ) {
            //Setup provider and request
            val provider = LocationServices.getFusedLocationProviderClient(context)
            val request = LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY,
                500L
            ).build()

            //Setup callback
            val callback = object : LocationCallback() {
                override fun onLocationResult(result: LocationResult) {
                    super.onLocationResult(result)

                    //Update locations
                    val location = result.lastLocation
                    location?.let {
                        val geoLocation = GeoPoint(location.latitude, location.longitude)
                        userLocation = geoLocation

                        if (userLocation.distanceToAsDouble(lastUserLocation) > 0.5) {
                            lastUserLocation = userLocation
//                        fullList.value = MainActivity.dataViewModel.stores
                            //Todo remove if list updating still works without this line

                            viewModelCallback.invoke(userLocation)
                        }
                    }
                }
            }

            if (hasPermissions) {
                //Assign request
                provider.requestLocationUpdates(
                    request,
                    callback,
                    Looper.getMainLooper()
                )
            }
        }

        fun requestPermissions(
            context: Context
        ) : Boolean {
            //Check permissions before assigning request
            val hasFineLocation = ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

            val hasCourseLocation = ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

            return if (hasFineLocation && hasCourseLocation) {
                hasPermissions = true

                true
            }
            //If permissions were not granted: request them
            else {
                ActivityCompat.requestPermissions(
                    context as Activity,
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                    ),
                    99
                )

                false
            }
        }

        fun setPermissions() {
            hasPermissions = true
        }
    }
}