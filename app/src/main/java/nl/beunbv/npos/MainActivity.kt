package nl.beunbv.npos

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Looper
import android.os.StrictMode
import android.preference.PreferenceManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.*
import nl.beunbv.npos.data.JSONHandler
import nl.beunbv.npos.notification.StoreCheckingService.Companion.initStoreCheckingService
import nl.beunbv.npos.ui.NPOS
import nl.beunbv.npos.ui.screens.fullList
import nl.beunbv.npos.ui.theme.NPOSTheme
import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint

class MainActivity : ComponentActivity() {
    @SuppressLint("SourceLockedOrientationActivity")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //Thread policies for internet access
        val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)
        Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this))

        //Orientation lock
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        //Initialize JSON
        jsonHandler = JSONHandler(
            productsStream = resources.openRawResource(R.raw.products),
            storesStream = resources.openRawResource(R.raw.stores),
        )

        //Start location service
        setupUserLocation(context = this)

        //Start background service
        initStoreCheckingService(this)

        //Setup ui
        setContent {
            NPOSTheme {
                NPOS()
            }
        }
    }

    companion object {
        //Global variables
        lateinit var jsonHandler: JSONHandler
        var unfoldedStore: Int = -1

        //Location
        private var hasPermissions: Boolean = false
        var userLocation = GeoPoint(51.5892100, 4.7805200)
        var lastUserLocation = GeoPoint(0.0, 0.0)
        var locationUpdateCallback: (GeoPoint) -> Unit = {}

        //Sets up the location service
        fun setupUserLocation(context: Context) {
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
                            fullList.value = jsonHandler.stores

                            locationUpdateCallback.invoke(userLocation)
                        }
                    }
                }
            }

            //Check permissions before assigning request
            val hasFineLocation = ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

            val hasCourseLocation = ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

            if (hasFineLocation && hasCourseLocation) {
                hasPermissions = true

                //Assign request
                provider.requestLocationUpdates(
                    request,
                    callback,
                    Looper.getMainLooper()
                )
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
            }
        }
    }
}